package com.oj.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 楼层回复
 */
@Data
public class ReplyVO {

    private Long id;

    private Long userId;

    private String authorName;

    private String content;

    /** 当前用户是否可删除该回复(作者本人或管理端) */
    private Boolean canDelete;

    private LocalDateTime createTime;
}
