package com.oj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.common.Result;
import com.oj.dto.CustomTestRequest;
import com.oj.dto.SubmitRequest;
import com.oj.entity.Submission;
import com.oj.enums.JudgeStatus;
import com.oj.judge.CustomTestService;
import com.oj.service.SubmissionService;
import com.oj.vo.CustomTestVO;
import com.oj.vo.SubmissionListVO;
import com.oj.vo.UserStatsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提交相关接口
 */
@RestController
@RequestMapping("/api/submission")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final CustomTestService customTestService;

    /**
     * 接收代码提交, 返回提交记录ID
     * POST /api/submission
     * body: {"problemId":1,"contestId":2,"language":"CPP","code":"..."}
     * 比赛内提交需带 X-Contest-Token 请求头
     */
    @PostMapping
    public Result<Long> submit(@Valid @RequestBody SubmitRequest request,
                               @RequestHeader(value = "X-Contest-Token", required = false) String contestToken) {
        return Result.ok(submissionService.submit(request, contestToken));
    }

    /**
     * 分页获取提交记录(含题目标题/提交者/代码查看权限标志)
     * GET /api/submission/page?scope=mine|all&problemId=&status=&pageNum=&pageSize=
     * scope=all 查全站(必须带 problemId), scope=mine 查当前用户
     */
    @GetMapping("/page")
    public Result<Page<SubmissionListVO>> page(
            @RequestParam(defaultValue = "mine") String scope,
            @RequestParam(required = false) Long problemId,
            @RequestParam(required = false) JudgeStatus status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(submissionService.pageSubmissions(scope, problemId, status, pageNum, pageSize));
    }

    /**
     * 当前用户的做题统计(个人主页: 总提交/通过情况/近30天每日提交数)
     * GET /api/submission/stats
     */
    @GetMapping("/stats")
    public Result<UserStatsVO> stats() {
        return Result.ok(submissionService.getMyStats());
    }

    /**
     * 自定义测试: 用用户提供的输入在本机真实运行代码(不落库、不判题)
     * POST /api/submission/custom-test
     */
    @PostMapping("/custom-test")
    public Result<CustomTestVO> customTest(@Valid @RequestBody CustomTestRequest request) {
        return Result.ok(customTestService.run(request));
    }

    /**
     * 查询提交记录(前端提交后轮询判题结果; 无代码查看权时 code/errorMessage 为空)
     * GET /api/submission/{id}
     */
    @GetMapping("/{id}")
    public Result<Submission> detail(@PathVariable Long id) {
        return Result.ok(submissionService.detailWithPermission(id));
    }
}
