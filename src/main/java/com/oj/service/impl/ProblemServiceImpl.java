package com.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.oj.common.UserContext;
import com.oj.dto.ProblemCreateRequest;
import com.oj.entity.Problem;
import com.oj.entity.Submission;
import com.oj.enums.JudgeStatus;
import com.oj.enums.ProblemStatus;
import com.oj.mapper.ProblemMapper;
import com.oj.mapper.SubmissionMapper;
import com.oj.service.ProblemService;
import com.oj.service.TestDataService;
import com.oj.vo.ProblemListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemServiceImpl extends ServiceImpl<ProblemMapper, Problem> implements ProblemService {

    private final SubmissionMapper submissionMapper;
    private final TestDataService testDataService;

    @Override
    public Page<ProblemListVO> pageProblems(long pageNum, long pageSize, String keyword, Long userId) {
        Page<Problem> page = lambdaQuery()
                .like(org.springframework.util.StringUtils.hasText(keyword), Problem::getTitle, keyword)
                .orderByAsc(Problem::getId)
                .page(new Page<>(pageNum, pageSize));
        // 3.5.17 中 Page.convert 返回 IPage 而非 Page, 手动构造以保持返回类型
        Page<ProblemListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(ProblemListVO::from)
                .collect(Collectors.toList()));

        // 聚合当前用户对本题的做题状态
        List<Long> problemIds = voPage.getRecords().stream()
                .map(ProblemListVO::getId)
                .collect(Collectors.toList());
        Map<Long, ProblemStatus> statusMap = queryUserStatusMap(userId, problemIds);
        voPage.getRecords().forEach(vo -> vo.setStatus(statusMap.getOrDefault(vo.getId(), ProblemStatus.NOT_ATTEMPTED)));
        return voPage;
    }

    /**
     * 查询用户在各题目上的做题状态:
     * 有 Accepted 提交 -> 已通过; 有提交但无 Accepted -> 尝试未通过; 无提交 -> 未尝试(调用方兜底)
     */
    private Map<Long, ProblemStatus> queryUserStatusMap(Long userId, List<Long> problemIds) {
        if (problemIds.isEmpty()) {
            return new HashMap<>();
        }
        List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getUserId, userId)
                        .in(Submission::getProblemId, problemIds)
                        .select(Submission::getProblemId, Submission::getStatus));
        Map<Long, ProblemStatus> statusMap = new HashMap<>();
        for (Submission s : submissions) {
            ProblemStatus status = s.getStatus() == JudgeStatus.ACCEPTED
                    ? ProblemStatus.SOLVED
                    : ProblemStatus.ATTEMPTED;
            // 已通过优先级最高, 不被后续"尝试未通过"覆盖
            statusMap.merge(s.getProblemId(), status,
                    (oldVal, newVal) -> oldVal == ProblemStatus.SOLVED ? oldVal : newVal);
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
        return problem;
    }

    @Override
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
        problem.setAuthorId(UserContext.getUserId());
        save(problem);
        return problem.getId();
    }

    @Override
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
        probl