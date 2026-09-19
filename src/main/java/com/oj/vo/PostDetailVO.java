package com.oj.vo;

import com.oj.enums.PostType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子详情: 列表字段 + 正文 + 权限标志 + 全部回复
 */
@Data
public class PostDetailVO {

    private Long id;

    private PostType type;

    private String title;

    private Long problemId;

    private String problemTitle;

    private Long authorId;

    private String authorName;

    private String content;

    /** 当前用户是否可编辑/删除该帖(作者本人或管理端) */
    private Boolean canEdit;

    private Boolean canDelete;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<ReplyVO> replies;
}
