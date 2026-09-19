package com.oj.vo;

import com.oj.enums.UserRole;
import lombok.Data;

/**
 * 登录/注册成功响应: 令牌 + 用户信息
 */
@Data
public class LoginVO {

    private String token;

    private Long userId;

    private String username;

    private String nickname;

    /** 角色(UserRole: 0-普通用户 1-管理员 2-站长), 前端入口控制用 */
    private UserRole role;
}
