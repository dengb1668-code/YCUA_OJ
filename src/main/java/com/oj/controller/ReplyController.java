package com.oj.controller;

import com.oj.common.Result;
import com.oj.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 回复接口(楼层回复的删除)
 */
@RestController
@RequestMapping("/api/reply")
@RequiredArgsConstructor
public class ReplyController {

    private final PostService postService;

    /**
     * 删除回复(作者本人或管理端)
     * DELETE /api/reply/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postService.deleteReply(id);
        return Result.ok(null);
    }
}
