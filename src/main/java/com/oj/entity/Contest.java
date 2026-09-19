package com.oj.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oj.enums.ContestType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 比赛实体, 对应表 contest
 */
@Data
@TableName("contest")
public class Contest {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 比赛标题 */
    private String title;

    /** 比赛说明(Markdown) */
    private String description;

    /** 赛制 */
    private ContestType type;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 参赛密码(BCrypt 哈希, NULL=公开); 注意: 任何 VO/接口都不得回传此字段 */
    private String password;

    /** 创建者ID */
    private Long creatorId;

    /** 创建时间(插入时自动填充) */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间(插入/更新时自动填充) */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
