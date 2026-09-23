package com.oj.judge;

import com.oj.entity.Problem;
import com.oj.entity.Submission;
import com.oj.enums.JudgeMode;
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
 * 真实判题服务: 编译用户代码 -> 按测试点运行 -> 与期望输出比对, 按题目的判题模式分支
 * <p>
 * 判定规则(单点优先级 TLE > MLE > RE > WA):
 *  - 编译失败 -> CE(错误信息写入 errorMessage)
 *  - 运行超时(超过题目时间限制) -> TLE
 *  - 内存超限(Linux + GNU time 实测 max RSS, Windows 跳过) -> MLE
 *  - 非零退出码 -> RE
 *  - 输出与期望输出不一致(忽略首尾空白) -> WA
 *  - 全部一致 -> AC
 * <p>
 * 模式(题目创建时由出题人选择):
 *  - ICPC(CF 式): 无部分分(score=100/0), 首个失败测试点即停, failedTestIndex 记录失败点
 *  - IOI(洛谷式): 跑完全部测试点, 逐点按分值计分
 * <p>
 * 整体状态取已跑测试点中最差的; 逐点结果写入 judgeDetail。
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
        if (submission == null) {
            // 提交已被删除(如删题级联)或队列残留旧 ID, 直接丢弃任务
            log.warn("判题任务对应的提交不存在, 跳过: submissionId={}", submissionId);
            return;
        }
        Problem problem = problemMapper.selectById(submission.getProblemId());
        List<Problem.Sample> samples = (problem != null && problem.getSamples() != null)
                ? problem.getSamples() : List.of();
        long timeLimit = (problem != null && problem.getTimeLimit() != null) ? problem.getTimeLimit() : 1000L;
        // 判题模式由题目决定(出题人创建时选择); 存量数据迁移已回填, null 兜底 ICPC
        boolean icpc = problem == null || problem.getJudgeMode() == null || problem.getJudgeMode() == JudgeMode.ICPC;

        Path dir = null;
        try {
            dir = codeRunner.createTempDir();
            codeRunner.writeSource(dir, submission.getLanguage(), submission.getCode());

            // 编译
            String compileError = codeRunner.compile(dir, submission.getLanguage());
            if (compileError != null) {
                finish(submissionId, JudgeStatus.COMPILE_ERROR, null, null, null, 0, null, compileError);
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
            int maxMemory = 0;
            int earnedScore = 0;
            Integer failedTestIndex = null;
            Integer memoryLimitMb = (problem != null && problem.getMemoryLimit() != null)
                    ? problem.getMemoryLimit() : null;

            if (!testCases.isEmpty()) {
                if (icpc) {
                    // ICPC/CF 式: 按序跑测试点, 首个失败即停, 无部分分(score=100/0), 结果带 "on test N"
                    for (TestDataStore.CaseData tc : testCases) {
                        long pointTimeLimit = tc.timeLimit() != null ? tc.timeLimit() : timeLimit;
                        Integer pointMemoryLimit = tc.memoryLimit() != null ? tc.memoryLimit() : memoryLimitMb;
                        Submission.JudgeCase c = runCase(dir, submission.getLanguage(), "test " + tc.index(),
                                tc.input(), tc.output(), pointTimeLimit, pointMemoryLimit, tc.index());
                        cases.add(c);
                        maxTime = Math.max(maxTime, c.getTimeUsed() == null ? 0 : c.getTimeUsed());
                        maxMemory = Math.max(maxMemory, c.getMemoryUsed() == null ? 0 : c.getMemoryUsed());
                        if (c.getStatus() != JudgeStatus.ACCEPTED) {
                            overall = c.getStatus();
                            failedTestIndex = tc.index();
                            break; // CF 式: 首错即停, 不跑剩余测试点
                        }
                    }
                    earnedScore = overall == JudgeStatus.ACCEPTED ? 100 : 0;
                } else {
                    // IOI 式(现状): 跑完全部测试点, 逐点按分值计分, 独立时限优先(runCase 内统一加 500ms 启动余量)
                    int totalScore = testCases.stream().mapToInt(TestDataStore.CaseData::score).sum();
                    for (TestDataStore.CaseData tc : testCases) {
                        long pointTimeLimit = tc.timeLimit() != null ? tc.timeLimit() : timeLimit;
                        Integer pointMemoryLimit = tc.memoryLimit() != null ? tc.memoryLimit() : memoryLimitMb;
                        Submission.JudgeCase c = runCase(dir, submission.getLanguage(), "测试点" + tc.index(),
                                tc.input(), tc.output(), pointTimeLimit, pointMemoryLimit, tc.index());
                        int pointScore = c.getStatus() == JudgeStatus.ACCEPTED ? tc.score() : 0;
                        c.setScore(pointScore);
                        c.setFullScore(tc.score());
                        earnedScore += pointScore;
                        cases.add(c);
                    }
                    for (Submission.JudgeCase c : cases) {
                        maxTime = Math.max(maxTime, c.getTimeUsed() == null ? 0 : c.getTimeUsed());
                        maxMemory = Math.max(maxMemory, c.getMemoryUsed() == null ? 0 : c.getMemoryUsed());
                        if (severity(c.getStatus()) > severity(overall)) {
                            overall = c.getStatus();
                        }
                    }
                    // 计分制汇总: 满分=AC; 部分分=WA(洛谷式); 0 分保持 severity 最重的状态
                    if (earnedScore >= totalScore) {
                        overall = JudgeStatus.ACCEPTED;
                    } else if (earnedScore > 0) {
                        overall = JudgeStatus.WRONG_ANSWER;
                    }
                }
            } else {
                // 样例回退(无测试点): 全过=AC(100 分), 任一失败=0 分, 用例不显示分值;
                // ICPC 模式首错即停, IOI 模式跑完全部样例(整体取 severity 最重的状态)
                for (int i = 0; i < samples.size(); i++) {
                    Submission.JudgeCase c = runCase(dir, submission.getLanguage(), "test " + (i + 1),
                            samples.get(i).getInput(), samples.get(i).getOutput(), timeLimit, memoryLimitMb, i + 1);
                    cases.add(c);
                    maxTime = Math.max(maxTime, c.getTimeUsed() == null ? 0 : c.getTimeUsed());
                    maxMemory = Math.max(maxMemory, c.getMemoryUsed() == null ? 0 : c.getMemoryUsed());
                    if (severity(c.getStatus()) > severity(overall)) {
                        overall = c.getStatus();
                    }
                    if (icpc && c.getStatus() != JudgeStatus.ACCEPTED) {
                        failedTestIndex = i + 1;
                        break;
                    }
                }
                earnedScore = overall == JudgeStatus.ACCEPTED ? 100 : 0;
            }

            finish(submissionId, overall, cases, maxTime, maxMemory == 0 ? null : maxMemory, earnedScore,
                    failedTestIndex, null);
        } catch (Exception e) {
            log.error("判题异常, submissionId={}", submissionId, e);
            finish(submissionId, JudgeStatus.SYSTEM_ERROR, null, null, null, 0, null,
                    "判题系统错误: " + e.getMessage());
        } finally {
            codeRunner.deleteQuietly(dir);
        }
    }

    /** 运行单个用例并判定(判定优先级: TLE > MLE > RE > WA) */
    private Submission.JudgeCase runCase(Path dir, Language language, String caseName, String input,
                                         String expectedOutput, long timeLimit, Integer memoryLimitMb, int testIndex)
            throws IOException, InterruptedException {
        // 时间限制 + 500ms 启动余量
        CodeRunner.RunResult result = codeRunner.run(dir, language, input, timeLimit + 500, memoryLimitMb);

        JudgeStatus caseStatus;
        if (result.timedOut()) {
            caseStatus = JudgeStatus.TIME_LIMIT_EXCEEDED;
        } else if (memoryLimitMb != null && result.memoryKb() != null
                && result.memoryKb() > memoryLimitMb * 1024L) {
            caseStatus = JudgeStatus.MEMORY_LIMIT_EXCEEDED;
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
        c.setTestIndex(testIndex);
        c.setTimeUsed((int) result.elapsedMillis());
        // Linux + GNU time 时为实测 max RSS(KB); Windows 无测量, 为 null
        c.setMemoryUsed(result.memoryKb());
        return c;
    }

    private void finish(Long submissionId, JudgeStatus status, List<Submission.JudgeCase> cases,
                        Integer timeUsed, Integer memoryUsed, Integer score, Integer failedTestIndex,
                        String errorMessage) {
        Submission update = new Submission();
        update.setId(submissionId);
        update.setStatus(status);
        update.setJudgeDetail(cases);
        update.setTimeUsed(timeUsed);
        update.setMemoryUsed(memoryUsed);
        update.setScore(score);
        update.setFailedTestIndex(failedTestIndex);
        update.setErrorMessage(errorMessage);
        submissionMapper.updateById(update);
        log.info("判题完成: submissionId={}, status={}, score={}, failedTest={}",
                submissionId, status.getLabel(), score, failedTestIndex);
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
