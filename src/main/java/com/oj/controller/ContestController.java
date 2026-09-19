package com.oj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.common.Result;
import com.oj.dto.ContestCreateRequest;
import com.oj.dto.ContestJoinRequest;
import com.oj.dto.ContestProblemUpdateRequest;
import com.oj.dto.ContestUpdateRequest;
import com.oj.service.ContestService;
import com.oj.vo.ContestDetailVO;
import com.oj.vo.ContestJoinVO;
import com.oj.vo.ContestListVO;
import com.oj.vo.ContestProblemVO;
import com.oj.vo.StandingsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 比赛接口(均在 /api/** 下自动要求登录; 题目/榜单/提交用 X-Contest-Token 校验比赛访问权限)
 */
@RestController
@RequestMapping("/api/contest")
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;

    /**
     * 创建比赛(任何登录用户)
     * POST /api/contest
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ContestCreateRequest request) {
        return Result.ok(contestService.create(request));
    }

    /**
     * 比赛分页列表
     * GET /api/contest/page?pageNum=&pageSize=
     */
    @GetMapping("/page")
    public Result<Page<ContestListVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(contestService.page(pageNum, pageSize));
    }

    /**
     * 比赛详情(基本信息无需 token; 已持有 token 可带 X-Contest-Token 头)
     * GET /api/contest/{id}
     */
    @GetMapping("/{id}")
    public Result<ContestDetailVO> detail(@PathVariable Long id,
                                          @RequestHeader(value = "X-Contest-Token", required = false) String contestToken) {
        return Result.ok(contestService.detail(id, contestToken));
    }

    /**
     * 加入比赛(密码制需密码), 返回访问 token
     * POST /api/contest/{id}/join
     */
    @PostMapping("/{id}/join")
    public Result<ContestJoinVO> join(@PathVariable Long id,
                                      @RequestBody(required = false) ContestJoinRequest request) {
        return Result.ok(contestService.join(id, request));
    }

    /**
     * 编辑比赛(创建者或管理端)
     * PUT /api/contest/{id}
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ContestUpdateRequest request) {
        contestService.update(id, request);
        return Result.ok(null);
    }

    /**
     * 更新比赛题目列表(创建者或管理端, 仅未开赛)
     * PUT /api/contest/{id}/problems
     */
    @PutMapping("/{id}/problems")
    public Result<Void> updateProblems(@PathVariable Long id,
                                       @Valid @RequestBody ContestProblemUpdateRequest request) {
        contestService.updateProblems(id, request);
        return Result.ok(null);
    }

    /**
     * 删除比赛(创建者或管理端)
     * DELETE /api/contest/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        contestService.delete(id);
        return Result.ok(null);
    }

    /**
     * 比赛题目列表(需比赛访问 token)
     * GET /api/contest/{id}/problems
     */
    @GetMapping("/{id}/problems")
    public Result<List<ContestProblemVO>> problems(
            @PathVariable Long id,
            @RequestHeader(value = "X-Contest-Token", required = false) String contestToken) {
        return Result.ok(contestService.problems(id, contestToken));
    }

    /**
     * 榜单(需比赛访问 token; OI 赛制赛期非创建者/管理端隐藏)
     * GET /api/contest/{id}/standings
     */
    @GetMapping("/{id}/standings")
    public Result<StandingsVO> standings(
            @PathVariable Long id,
            @RequestHeader(value = "X-Contest-Token", required = false) String contestToken) {
        return Result.ok(contestService.standings(id, contestToken));
    }
}
