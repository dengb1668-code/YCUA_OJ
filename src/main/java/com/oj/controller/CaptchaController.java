package com.oj.controller;

import com.oj.common.CaptchaService;
import com.oj.common.Result;
import com.oj.vo.CaptchaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图片验证码接口(无需登录态, 在拦截器白名单中)
 */
@RestController
@RequestMapping("/api/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    /**
     * 获取登录验证码
     * GET /api/captcha
     */
    @GetMapping
    public Result<CaptchaVO> get() {
        return Result.ok(captchaService.generate());
    }
}
