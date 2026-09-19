package com.oj.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 图片验证码 id(登录前先调 GET /api/captcha 获取) */
    @NotBlank(message = "验证码不能为空")
    private String captchaId;

    /** 图片验证码内容 */
    @NotBlank(message = "验证码不能为空")
    private String captchaCode;
}
