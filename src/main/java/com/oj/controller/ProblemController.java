package com.oj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.common.Result;
import com.oj.common.UserContext;
import com.oj.dto.ProblemCreateRequest;
import com.oj.entity.Problem;
import com.oj.service.ProblemService;
import com.oj.vo.ProblemListVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题目相关接口
 */
@RestController
@RequestMapping("/api/problem")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    /**
     * 分页获取题目列表(含当前登录用户做题状态, keyword 模糊匹配标题)
     * GET /api/problem/page?pageNum=1&pageSize=10&keyword=
     */
    @GetMapping("/page")
    public Result<Page<ProblemListVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword) {
        // 当前用户由 AuthInterceptor 从 JWT 解析后放入 UserContext
        return Result.ok(problemService.pageProblems(pageNum, pageSize, keyword, UserContext.getUserId()));
    }

    /**
     * 获取单道题目详情
     * GET /api/problem/{id}
     */
    @GetMapping("/{id}")
    public Result<Problem> detail(@PathVariable Long id) {
        return Result.ok(problemService.getProblemDetail(id));
    }

    /**
     * 创建题目
     * POST /api/problem
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProblemCreateRequest request) {
        return Result.ok(problemService.createProblem(request));
    }

    /**
     * 编辑题目题面(仅创建者/管理员)
     * PUT /api/problem/{id}
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProblemCreateRequ