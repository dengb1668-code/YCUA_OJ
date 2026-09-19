package com.oj.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oj.enums.PostType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子实体(讨论帖/题解统一建模), 对应表 post
 */
@Data
@TableName("post")
public class Post {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联题目ID(NULL=全局讨论) */
    private Long problemId;

    /** 类型: 讨论 / 题解 */
    private PostType type;

    /** 发帖人ID */
    private Long userId;

    /** 标题 */
    private String title;

    /** 正文(Markdown) */
    private String content;

    /** 创建时间(插入时自动填充) */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间(插入/更新时自动填充) */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
