package com.oj.vo;

import com.oj.enums.PostType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子列表项(讨论区/题解列表通用)
 */
@Data
public class PostListVO {

    private Long id;

    private PostType type;

    private String title;

    /** 关联题目ID(全局讨论为 null) */
    private Long problemId;

    /** 关联题目标题(全局讨论为 null) */
    private String problemTitle;

    private Long authorId;

    private String authorName;

    /** 回复数 */
    private Long replyCount;

    private LocalDateTime createTime;
}
