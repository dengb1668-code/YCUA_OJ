package com.oj.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.oj.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体, 对应表 user
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名(唯一) */
    private String username;

    /**
     * 密码(BCrypt哈希)
     * select = false: 普通查询不会返回该字段, 避免泄露;
     * 登录校验时需用 QueryWrapper.select("password") 显式查询
     */
    @TableField(select = false)
    private String password;

    /** 昵称 */
    private String nickname;

    /** 手机号(唯一, 找回密码依据) */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像URL */
    private String avatar;

    /** 角色: 普通用户 / 管理员 */
    private UserRole role;

    /** 创建时间(插入时自动填充) */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间(插入/更新时自动填充) */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
