package com.oj.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子楼层回复实体, 对应表 reply
 */
@Data
@TableName("reply")
public class Reply {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属帖子ID */
    private Long postId;

    /** 回复人ID */
    private Long userId;

    /** 回复内容(Markdown) */
    private String content;

    /** 创建时间(插入时自动填充) */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
