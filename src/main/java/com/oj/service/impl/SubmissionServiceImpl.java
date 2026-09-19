package com.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.oj.common.NicknameUtil;
import com.oj.common.UserContext;
import com.oj.dto.SubmitRequest;
import com.oj.entity.Problem;
import com.oj.entity.Submission;
import com.oj.entity.User;
import com.oj.enums.JudgeStatus;
import com.oj.judge.JudgeQueue;
import com.oj.mapper.ProblemMapper;
import com.oj.mapper.SubmissionMapper;
import com.oj.mapper.UserMapper;
import com.oj.service.ContestService;
import com.oj.service.PermissionService;
import com.oj.service.ProblemService;
import com.oj.service.SubmissionService;
import com.oj.vo.SubmissionListVO;
import com.oj.vo.UserStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl extends ServiceImpl<SubmissionMapper, Submission> implements SubmissionService {

    private final ProblemService problemService;
    private final JudgeQueue judgeQueue;
    private final ProblemMapper problemMapper;
    private final UserMapper userMapper;
    private final PermissionService permissionService;
    private final ContestService contestService;

    @Override
    public Long submit(SubmitRequest request, String contestToken) {
        // 校验题目是否存在
        if (problemService.getById(request.getProblemId()) == null) {
            throw new IllegalArgumentException("题目不存在: id=" + request.getProblemId());
        }
        // 比赛内提交: 校验比赛存在/时间窗/访问 token/题目在比赛题目集中
        if (request.getContestId() != null) {
            contestService.checkSubmitAccess(request.getContestId(), request.getProblemId(), contestToken);
        }

        // 落库, 初始状态 Pending
        Submission submission = new Submission();
        submission.setUserId(UserContext.getUserId());
        submission.setProblemId(request.getProblemId());
        submission.setContestId(request.getContestId());
        submission.setLanguage(request.getLanguage());
        submission.setCode(request.getCode());
        submission.setStatus(JudgeStatus.PENDING);
        save(submission);

        // 提交入队, 由独立评测机进程消费判题(编译 + 逐样例比对输出)
        judgeQueue.push(submission.getId());

        return submission.getId();
    }

    @Override
    public Page<SubmissionListVO> pageMySubmissions(long pageNum, long pageSize) {
        return pageSubmissions("mine", null, null, pageNum, pageSize);
    }

    @Override
    public Page<SubmissionListVO> pageSubmissions(String scope, Long problemId, JudgeStatus status,
                                                  long pageNum, long pageSize) {
        boolean mine = !"all".equals(scope);
        if (!mine && problemId == null) {
            throw new IllegalArgumentException("全站提交查询必须指定题目");
        }
        Long currentUserId = UserContext.getUserId();
        Page<Submission> page = lambdaQuery()
                .eq(mine, Submission::getUserId, currentUserId)
                .eq(problemId != null, Submission::getProblemId, problemId)
                .eq(status != null, Submission::getStatus, status)
                .orderByDesc(Submission::getId)
                .page(new Page<>(pageNum, pageSize));

        // 批量查题目标题(只取需要的两列)
        List<Long> problemIds = page.getRecords().stream()
                .map(Submission::getProblemId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> titleMap = problemIds.isEmpty() ? Map.of()
                : problemMapper.selectList(new LambdaQueryWrapper<Problem>()
                        .in(Problem::getId, problemIds)
                        .select(Problem::getId, Problem::getTitle))
                .stream()
                .collect(Collectors.toMap(Problem::getId, Problem::getTitle));

        // 批量查提交者昵称
        List<Long> userIds = page.getRecords().stream()
                .map(Submission::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> nameMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, NicknameUtil::nicknameOf));

        // 代码查看权限: 管理端全放行; scope=mine 全是本人; scope=all 看是否对该题已 AC
        boolean manager = permissionService.isCurrentManager();
        boolean solvedThisProblem = false;
        if (!mine && !manager) {
            solvedThisProblem = lambdaQuery()
                    .eq(Submission::getUserId, currentUserId)
                    .eq(Submission::getProblemId, problemId)
                    .eq(Submission::getStatus, JudgeStatus.ACCEPTED)
                    .count() > 0;
        }

        Page<SubmissionListVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(s -> {
            SubmissionListVO vo = new SubmissionListVO();
            vo.setId(s.getId());
            vo.setProblemId(s.getProblemId());
            vo.setProblemTitle(titleMap.get(s.getProblemId()));
            vo.setContestId(s.getContestId());
            vo.setUserId(s.getUserId());
            vo.setUsername(nameMap.get(s.getUserId()));
            boolean own = s.getUserId().equals(currentUserId);
            vo.setCanViewCode(own || manager || solvedThisProblem);
            vo.setLanguage(s.getLanguage());
            vo.setStatus(s.getStatus());
            vo.setScore(s.getScore());
            vo.setTimeUsed(s.getTimeUsed());
            vo.setMemoryUsed(s.getMemoryUsed());
            vo.setCreateTime(s.getCreateTime());
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public Submission detailWithPermission(Long id) {
        Submission submission = getById(id);
        if (submission == null) {
            throw new IllegalArgumentException("提交记录不存在: id=" + id);
        }
        Long currentUserId = UserContext.getUserId();
        if (submission.getUserId().equals(currentUserId) || permissionService.isCurrentManager()) {
            return submission;
        }
        // 非本人非管理端: 对该题有 AC 记录才能看代码; 否则隐藏代码与错误信息(CE 报文可能回显源码)
        boolean solved = lambdaQuery()
                .eq(Submission::getUserId, currentUserId)
                .eq(Submission::getProblemId, submission.getProblemId())
                .eq(Submission::getStatus, JudgeStatus.ACCEPTED)
                .select(Submission::getId)
                .count() > 0;
        if (!solved) {
            submission.setCode(null);
            submission.setErrorMessage(null);
        }
        return submission;
    }

    @Override
    public UserStatsVO getMyStats() {
        Long userId = UserContext.getUserId();
        int acceptedCode = JudgeStatus.ACCEPTED.getCode();

        long total = baseMapper.countTotalByUser(userId);
        long accepted = baseMapper.countAcceptedByUser(userId, acceptedCode);

        UserStatsVO vo = new UserStatsVO();
        vo.setTotalSubmissions(total);
        vo.setAcceptedSubmissions(accepted);
        vo.setSolvedProblems(baseMapper.countSolvedProblems(userId, acceptedCode));
        vo.setAttemptedProblems(baseMapper.countUnsolvedAttempted(userId, acceptedCode));
        vo.setAcRate(total == 0 ? "0%" : String.format("%.1f%%", accepted * 100.0 / total));
        vo.setDailyAccepted(baseMapper.dailyAcceptedCounts(userId, acceptedCode).stream().map(m -> {
            UserStatsVO.DailyCount dc = new UserStatsVO.DailyCount();
            dc.setDate(String.valueOf(m.get("date")));
            dc.setCount(Long.valueOf(String.valueOf(m.get("cnt"))));
            return dc;
        }).collect(Collectors.toList()));
        vo.setRatingDistribution(baseMapper.ratingDistribution(userId, acceptedCode).stream().map(m -> {
            UserStatsVO.RatingCount rc = new UserStatsVO.RatingCount();
            rc.setRating(Integer.valueOf(String.valueOf(m.get("rating"))));
            rc.setCount(Long.valueOf(String.valueOf(m.get("cnt"))));
            return rc;
        }).collect(Collectors.toList()));
        return vo;
    }
}
