package com.oj.judge;

import com.oj.entity.Problem;
import com.oj.entity.Submission;
import com.oj.enums.JudgeStatus;
import com.oj.enums.Language;
import com.oj.mapper.ProblemMapper;
import com.oj.mapper.SubmissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 真实判题服务: 编译用户代码 -> 逐样例用"样例输入"运行 -> 与"样例输出"比对
 * <p>
 * 判定规则:
 *  - 编译失败 -> CE(错误信息写入 errorMessage)
 *  - 运行超时(超过题目时间限制) -> TLE
 *  - 非零退出码 -> RE
 *  - 输出与样例输出不一致(忽略首尾空白) -> WA
 *  - 全部一致 -> AC
 * <p>
 * 整体状态取所有样例中最差的; 逐样例结果写入 judgeDetail。
 * 注意: 本类必须是独立 Bean, @Async 方法跨 Bean 调用才会经过代理生效。
 * 本地开发直接执行用户代码, 部署前必须替换为沙箱判题。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JudgeService {

    private final SubmissionMapper submissionMapper;
    private final ProblemMapper problemMapper;
    private final CodeRunner codeRunner;
    private final TestDataStore testDataStore;

    @Async("judgeExecutor")
    public void judge(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        Problem problem = problemMapper.selectById(submission.getProblemId());
        List<Problem.Sample> samples = (problem != null && problem.getSamples() != null)
                ? problem.getSamples() : List.of();
        long timeLimit = (problem != null && problem.getTimeLimit() != null) ? problem.getTimeLimit() : 1000L;

        Path dir = null;
        try {
            dir = codeRunner.createTempDir();
            codeRunner.writeSource(dir, submission.getLanguage(), submission.getCode());

            // 编译
            String compileError = codeRunner.compile(dir, submission.getLanguage());
            if (compileError != null) {
                finish(submissionId, JudgeStatus.COMPILE_ERROR, null, null, 0, compileError);
                return;
            }

            // 编译成功后一次性把测试点读入内存(避免判题期间 Web 端修改文件);
            // 题目无测试点时回退用公开样例判题
            List<TestDataStore.CaseData> testCases = testDataStore.loadAll(submission.getProblemId());
            if (testCases.isEmpty()) {
                log.info("题目 {} 无测试点, 回退样例判题: submissionId={}", submission.getProblemId(), submissionId);
            }

            List<Submission.JudgeCase> cases = new ArrayList<>();
            JudgeStatus overall = JudgeStatus.ACCEPTED;
            int maxTime = 0;
            int earnedScore = 0;
            int totalScore = 0;

            if (!testCases.isEmpty()) {
                // 计分制: 每点 AC 得该点分值, 独立时限优先(runCase 内统一加 500ms 启动余量)
                totalScore = testCases.stream().mapToInt(TestDataStore.CaseData::score).sum();
                for (TestDataStore.CaseData tc : testCases) {
                    long pointTimeLimit = tc.timeLimit() != null ? tc.timeLimit() : timeLimit;
                    Submission.JudgeCase c = runCase(dir, submission.getLanguage(), "测试点" + tc.index(),
                            tc.input(), tc.output(), pointTimeLimit);
                    int pointScore = c.getStatus() == JudgeStatus.ACCEPTED ? tc.score() : 0;
                    c.setScore(pointScore);
                    c.setFullScore(tc.score());
                    earnedScore += pointScore;
                    cases.add(c);
                }
            } else {
                // 样例回退: 保持旧行为——全过=AC(100 分), 任一失败=0 分, 用例不显示分值
                for (int i = 0; i < samples.size(); i++) {
                    cases.add(runCase(dir, submission.getLanguage(), "样例" + (i + 1),
                            samples.get(i).getInput(), samples.get(i).getOutput(), timeLimit));
                }
                totalScore = 100;
                earnedScore = cases.stream().allMatch(c -> c.getStatus() == JudgeStatus.ACCEPTED) ? 100 : 0;
            }

            for (Submission.JudgeCase c : cases) {
                maxTime = Math.max(maxTime, c.getTimeUsed() == null ? 0 : c.getTimeUsed());
                if (severity(c.getStatus()) > severity(overall)) {
                    overall = c.getStatus();
                }
            }

            // 计分制汇总: 满分=AC; 部分分=WA(洛谷式); 0 分保持 severity 最重的状态
            if (!testCases.isEmpty()) {
                if (earnedScore >= totalScore) {
                    overall = JudgeStatus.ACCEPTED;
                } else if (earnedScore > 0) {
                    overall = JudgeStatus.WRONG_ANSWER;
                }
            }

            finish(submissionId, overall, cases, maxTime, earnedScore, null);
        } catch (Exception e) {
            log.error("判题异常, submissionId={}", submissionId, e);
            finish(submissionId, JudgeStatus.SYSTEM_ERROR, null, null, 0, "判题系统错误: " + e.getMessage());
        } finally {
            codeRunner.deleteQuietly(dir);
        }
    }

    /** 运行单个用例并判定 */
    private Submission.JudgeCase runCase(Path dir, Language language,
                                         String caseName, String input, String expectedOutput, long timeLimit)
            throws IOException, InterruptedException {
        // 时间限制 + 500ms 启动余量
        CodeRunner.RunResult result = codeRunner.run(dir, language, input, timeLimit + 500);

        JudgeStatus caseStatus;
        if (result.timedOut()) {
            caseStatus = JudgeStatus.TIME_LIMIT_EXCEEDED;
        } else if (result.exitCode() != 0) {
            caseStatus = JudgeStatus.RUNTIME_ERROR;
        } else if (normalize(result.stdout()).equals(normalize(expectedOutput))) {
            caseStatus = JudgeStatus.ACCEPTED;
        } else {
            caseStatus = JudgeStatus.WRONG_ANSWER;
        }

        Submission.JudgeCase c = new Submission.JudgeCase();
        c.setCaseName(caseName);
        c.setStatus(caseStatus);
        c.setTimeUsed((int) result.elapsedMillis());
        // Windows 下不便精确测量内存, 暂不写入
        return c;
    }

    private void finish(Long submissionId, JudgeStatus status, List<Submission.JudgeCase> cases,
                        Integer timeUsed, Integer score, String errorMessage) {
        Submission update = new Submission();
        update.setId(submissionId);
        update.setStatus(status);
        update.setJudgeDetail(cases);
        update.setTimeUsed(timeUsed);
        update.setScore(score);
        update.setErrorMessage(errorMessage);
        submissionMapper.updateById(update);
        log.info("判题完成: submissionId={}, status={}, score={}", submissionId, status.getLabel(), score);
    }

    /** 状态严重程度, 用于取整体最差结果 */
    private int severity(JudgeStatus status) {
        return switch (status) {
            case ACCEPTED -> 0;
            case WRONG_ANSWER -> 1;
            case TIME_LIMIT_EXCEEDED, MEMORY_LIMIT_EXCEEDED -> 2;
            default -> 3; // RE / SE
        };
    }

    /**
     * 输出比对归一化: Windows 下程序输出的换行是 \r\n,
     * 而样例/测试点文件可能用 \n, 统一转换后再忽略首尾空白
     */
    private String normalize(String s) {
        return s == null ? "" : s.replace("\r\n", "\n").trim();
    }
}
