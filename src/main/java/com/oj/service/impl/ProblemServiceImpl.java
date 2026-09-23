package com.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.oj.common.ProblemTags;
import com.oj.common.UserContext;
import com.oj.dto.ProblemCreateRequest;
import com.oj.entity.ContestProblem;
import com.oj.entity.Post;
import com.oj.entity.Problem;
import com.oj.entity.ProblemTag;
import com.oj.entity.Reply;
import com.oj.entity.Submission;
import com.oj.enums.JudgeMode;
import com.oj.enums.JudgeStatus;
import com.oj.enums.ProblemStatus;
import com.oj.judge.TestDataStore;
import com.oj.mapper.ContestProblemMapper;
import com.oj.mapper.PostMapper;
import com.oj.mapper.ProblemMapper;
import com.oj.mapper.ProblemTagMapper;
import com.oj.mapper.ReplyMapper;
import com.oj.mapper.SubmissionMapper;
import com.oj.service.ProblemService;
import com.oj.service.TestDataService;
import com.oj.vo.ProblemListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemServiceImpl extends ServiceImpl<ProblemMapper, Problem> implements ProblemService {

    private final SubmissionMapper submissionMapper;
    private final TestDataService testDataService;
    private final TestDataStore testDataStore;
    private final ContestProblemMapper contestProblemMapper;
    private final PostMapper postMapper;
    private final ReplyMapper replyMapper;
    private final ProblemTagMapper problemTagMapper;

    @Override
    public Page<ProblemListVO> pageProblems(long pageNum, long pageSize, String keyword, List<String> tags, Long userId) {
        // 标签 AND 筛选两段式: 先查同时含全部标签的题目ID, 空集合直接返回空页(MP 对空集合 in 会生成非法 IN ())
        List<String> validTags = ProblemTags.filterValid(tags);
        boolean tagFilterActive = !validTags.isEmpty();
        List<Long> tagProblemIds = List.of();
        if (tagFilterActive) {
            tagProblemIds = problemTagMapper.selectList(new LambdaQueryWrapper<ProblemTag>()
                            .in(ProblemTag::getTag, validTags)
                            .select(ProblemTag::getProblemId)
                            .groupBy(ProblemTag::getProblemId)
                            .having("COUNT(DISTINCT tag) = {0}", validTags.size()))
                    .stream().map(ProblemTag::getProblemId).collect(Collectors.toList());
            if (tagProblemIds.isEmpty()) {
                Page<ProblemListVO> empty = new Page<>(pageNum, pageSize, 0);
                empty.setRecords(List.of());
                return empty;
            }
        }

        Page<Problem> page = lambdaQuery()
                .in(tagFilterActive, Problem::getId, tagProblemIds)
                .like(org.springframework.util.StringUtils.hasText(keyword), Problem::getTitle, keyword)
                .orderByAsc(Problem::getId)
                .page(new Page<>(pageNum, pageSize));
        // 3.5.17 中 Page.convert 返回 IPage 而非 Page, 手动构造以保持返回类型
        Page<ProblemListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(ProblemListVO::from)
                .collect(Collectors.toList()));

        List<Long> problemIds = voPage.getRecords().stream()
                .map(ProblemListVO::getId)
                .collect(Collectors.toList());
        // 批量填充标签(一次查询, 避免 N+1)
        Map<Long, List<String>> tagMap = queryTagMap(problemIds);
        voPage.getRecords().forEach(vo -> vo.setTags(tagMap.getOrDefault(vo.getId(), List.of())));

        // 聚合当前用户对本题的做题状态
        Map<Long, ProblemStatus> statusMap = queryUserStatusMap(userId, problemIds);
        voPage.getRecords().forEach(vo -> vo.setStatus(statusMap.getOrDefault(vo.getId(), ProblemStatus.NOT_ATTEMPTED)));
        return voPage;
    }

    /** 批量查询题目标签, 按题目ID分组 */
    private Map<Long, List<String>> queryTagMap(List<Long> problemIds) {
        if (problemIds.isEmpty()) {
            return new HashMap<>();
        }
        return problemTagMapper.selectList(new LambdaQueryWrapper<ProblemTag>()
                        .in(ProblemTag::getProblemId, problemIds))
                .stream()
                .collect(Collectors.groupingBy(ProblemTag::getProblemId,
                        Collectors.mapping(ProblemTag::getTag, Collectors.toList())));
    }

    /**
     * 查询用户在各题目上的做题状态(按最新提交判定, CF 配色语义):
     * 有 Accepted 提交 -> 已通过(永不被更早提交降级);
     * 最新一次提交为编译错误 -> 编译错误(前端黄色);
     * 最新一次提交为其他错误 -> 尝试未通过(前端红色);
     * 无提交 -> 未尝试(调用方兜底); 判题中的提交不参与判定
     */
    private Map<Long, ProblemStatus> queryUserStatusMap(Long userId, List<Long> problemIds) {
        if (problemIds.isEmpty()) {
            return new HashMap<>();
        }
        List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getUserId, userId)
                        .in(Submission::getProblemId, problemIds)
                        .select(Submission::getProblemId, Submission::getStatus)
                        .orderByDesc(Submission::getId)); // 最新提交在前
        Map<Long, ProblemStatus> statusMap = new HashMap<>();
        for (Submission s : submissions) {
            // 判题中的提交不决定最终状态
            if (s.getStatus() == JudgeStatus.PENDING || s.getStatus() == JudgeStatus.JUDGING) {
                continue;
            }
            if (s.getStatus() == JudgeStatus.ACCEPTED) {
                // AC 优先级最高, 覆盖并锁定
                statusMap.put(s.getProblemId(), ProblemStatus.SOLVED);
            } else if (!statusMap.containsKey(s.getProblemId())) {
                // 按 id 倒序遍历, 首次遇到即该题最新提交(且题目尚无状态)
                statusMap.put(s.getProblemId(), s.getStatus() == JudgeStatus.COMPILE_ERROR
                        ? ProblemStatus.COMPILE_ERROR
                        : ProblemStatus.ATTEMPTED);
            }
        }
        return statusMap;
    }

    @Override
    public Problem getProblemDetail(Long id) {
        Problem problem = getById(id);
        if (problem == null) {
            throw new IllegalArgumentException("题目不存在: id=" + id);
        }
        // 填充测试点管理相关的瞬态字段(前端据此显示管理入口)
        problem.setCanManage(testDataService.canManage(problem, UserContext.getUserId()));
        problem.setTestCaseCount(testDataService.count(id));
        // 填充标签(编辑回填与详情展示用)
        problem.setTags(queryTagMap(List.of(id)).getOrDefault(id, List.of()));
        return problem;
    }

    @Override
    @Transactional
    public Long createProblem(ProblemCreateRequest request) {
        Problem problem = new Problem();
        problem.setTitle(request.getTitle());
        problem.setSource(request.getSource());
        problem.setDescription(request.getDescription());
        problem.setInputDescription(request.getInputDescription());
        problem.setOutputDescription(request.getOutputDescription());
        problem.setSamples(request.getSamples());
        problem.setTimeLimit(request.getTimeLimit());
        problem.setMemoryLimit(request.getMemoryLimit());
        problem.setDifficulty(request.getDifficulty());
        problem.setJudgeMode(request.getJudgeMode() != null ? request.getJudgeMode() : JudgeMode.ICPC);
        problem.setAuthorId(UserContext.getUserId());
        save(problem);
        saveTags(problem.getId(), request.getTags());
        return problem.getId();
    }

    @Override
    @Transactional
    public void updateProblem(Long id, ProblemCreateRequest request) {
        Problem problem = getById(id);
        if (problem == null) {
            throw new IllegalArgumentException("题目不存在: id=" + id);
        }
        // 权限与测试点管理一致: 创建者 ∨ 管理员 ∨ 旧题(无创建者)宽松
        if (!testDataService.canManage(problem, UserContext.getUserId())) {
            throw new IllegalArgumentException("无权限编辑该题目");
        }
        problem.setTitle(request.getTitle());
        problem.setSource(request.getSource());
        problem.setDescription(request.getDescription());
        problem.setInputDescription(request.getInputDescription());
        problem.setOutputDescription(request.getOutputDescription());
        problem.setSamples(request.getSamples());
        problem.setTimeLimit(request.getTimeLimit());
        problem.setMemoryLimit(request.getMemoryLimit());
        problem.setDifficulty(request.getDifficulty());
        if (request.getJudgeMode() != null) {
            problem.setJudgeMode(request.getJudgeMode());
        }
        updateById(problem);
        saveTags(id, request.getTags());
    }

    /** 保存题目标签: 白名单过滤后全量替换(先删后插) */
    private void saveTags(Long problemId, List<String> tags) {
        problemTagMapper.delete(new LambdaQueryWrapper<ProblemTag>().eq(ProblemTag::getProblemId, problemId));
        for (String tag : ProblemTags.filterValid(tags)) {
            ProblemTag pt = new ProblemTag();
            pt.setProblemId(problemId);
            pt.setTag(tag);
            problemTagMapper.insert(pt);
        }
    }

    @Override
    @Transactional
    public void deleteProblem(Long id) {
        Problem problem = getById(id);
        if (problem == null) {
            throw new IllegalArgumentException("题目不存在: id=" + id);
        }
        // 权限与测试点管理一致: 创建者 ∨ 管理端
        if (!testDataService.canManage(problem, UserContext.getUserId())) {
            throw new IllegalArgumentException("无权限删除该题目");
        }
        // 被比赛引用的题目不可删(保护比赛完整性, 无论比赛是否开赛)
        Long contestRefs = contestProblemMapper.selectCount(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getProblemId, id));
        if (contestRefs != null && contestRefs > 0) {
            throw new IllegalArgumentException("该题目已被比赛引用, 请先从比赛中移除后再删除");
        }
        // 级联: 帖子(含回复) -> 提交记录 -> 标签 -> 测试点文件 -> 题目
        List<Long> postIds = postMapper.selectList(new LambdaQueryWrapper<Post>()
                        .eq(Post::getProblemId, id)
                        .select(Post::getId))
                .stream().map(Post::getId).collect(Collectors.toList());
        if (!postIds.isEmpty()) {
            replyMapper.delete(new LambdaQueryWrapper<Reply>().in(Reply::getPostId, postIds));
            postMapper.deleteByIds(postIds);
        }
        submissionMapper.delete(new LambdaQueryWrapper<Submission>().eq(Submission::getProblemId, id));
        problemTagMapper.delete(new LambdaQueryWrapper<ProblemTag>().eq(ProblemTag::getProblemId, id));
        try {
            testDataStore.deleteAll(id);
        } catch (Exception e) {
            log.warn("删除题目测试点文件失败: problemId={}", id, e);
        }
        removeById(id);
    }
}
