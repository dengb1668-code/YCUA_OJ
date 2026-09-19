package com.oj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.common.Result;
import com.oj.dto.PostCreateRequest;
import com.oj.dto.PostUpdateRequest;
import com.oj.dto.ReplyRequest;
import com.oj.enums.PostType;
import com.oj.service.PostService;
import com.oj.vo.PostDetailVO;
import com.oj.vo.PostListVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 帖子接口(讨论帖/题解统一, type 区分; 均在 /api/** 下自动要求登录)
 */
@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 发帖(讨论帖/题解)
     * POST /api/post
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody PostCreateRequest request) {
        return Result.ok(postService.create(request));
    }

    /**
     * 帖子分页(problemId 为空查全局讨论, type 必传)
     * GET /api/post/page?type=&problemId=&pageNum=&pageSize=
     */
    @GetMapping("/page")
    public Result<Page<PostListVO>> page(@RequestParam PostType type,
                                         @RequestParam(required = false) Long problemId,
                                         @RequestParam(defaultValue = "1") long pageNum,
                                         @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(postService.pagePosts(type, problemId, pageNum, pageSize));
    }

    /**
     * 帖子详情(含全部回复)
     * GET /api/post/{id}
     */
    @GetMapping("/{id}")
    public Result<PostDetailVO> detail(@PathVariable Long id) {
        return Result.ok(postService.detail(id));
    }

    /**
     * 编辑帖子(作者本人或管理端)
     * PUT /api/post/{id}
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PostUpdateRequest request) {
        postService.update(id, request);
        return Result.ok(null);
    }

    /**
     * 删除帖子(作者本人或管理端, 级联删除回复)
     * DELETE /api/post/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postService.delete(id);
        return Result.ok(null);
    }

    /**
     * 回复帖子
     * POST /api/post/{id}/reply
     */
    @PostMapping("/{id}/reply")
    public Result<Long> reply(@PathVariable Long id, @Valid @RequestBody ReplyRequest request) {
        return Result.ok(postService.addReply(id, request.getContent()));
    }
}
