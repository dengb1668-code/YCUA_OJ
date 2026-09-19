package com.oj.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.oj.dto.PostCreateRequest;
import com.oj.dto.PostUpdateRequest;
import com.oj.entity.Post;
import com.oj.enums.PostType;
import com.oj.vo.PostDetailVO;
import com.oj.vo.PostListVO;

public interface PostService extends IService<Post> {

    /**
     * 帖子分页(讨论/题解通用, problemId 为空时查全局讨论)
     */
    Page<PostListVO> pagePosts(PostType type, Long problemId, long pageNum, long pageSize);

    /**
     * 帖子详情(含全部回复与当前用户权限标志)
     */
    PostDetailVO detail(Long id);

    /**
     * 发帖(题解必须关联题目)
     */
    Long create(PostCreateRequest request);

    /**
     * 编辑帖子(作者本人或管理端)
     */
    void update(Long id, PostUpdateRequest request);

    /**
     * 删除帖子(作者本人或管理端, 级联删除回复)
     */
    void delete(Long id);

    /**
     * 回复帖子
     */
    Long addReply(Long postId, String content);

    /**
     * 删除回复(作者本人或管理端)
     */
    void deleteReply(Long replyId);
}
