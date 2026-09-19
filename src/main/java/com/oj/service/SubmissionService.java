package com.oj.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.oj.dto.SubmitRequest;
import com.oj.entity.Submission;
import com.oj.enums.JudgeStatus;
import com.oj.vo.SubmissionListVO;
import com.oj.vo.UserStatsVO;

public interface SubmissionService extends IService<Submission> {

    /**
     * 接收代码提交: 校验题目 -> (比赛内提交校验时间窗/访问 token) -> 落库(状态 Pending) -> 触发异步判题
     *
     * @param contestToken 比赛访问 token(比赛内提交必传, 普通提交为 null)
     * @return 提交记录ID
     */
    Long submit(SubmitRequest request, String contestToken);

    /**
     * 分页查询当前登录用户的提交记录(含题目标题)
     */
    Page<SubmissionListVO> pageMySubmissions(long pageNum, long pageSize);

    /**
     * 分页查询提交记录: scope=mine 查当前用户; scope=all 查全站(必须指定 problemId 或 contestId);
     * problemId/contestId/status 可选过滤
     */
    Page<SubmissionListVO> pageSubmissions(String scope, Long problemId, Long contestId, JudgeStatus status,
                                           long pageNum, long pageSize);

    /**
     * 提交详情(带代码查看权限): 本人/管理端/对该题已 AC 者可见代码,
     * 否则 code 与 errorMessage 置 null
     */
    Submission detailWithPermission(Long id);

    /**
     * 当前登录用户的做题统计(总提交/通过情况/近30天每日提交数)
     */
    UserStatsVO getMyStats();
}
