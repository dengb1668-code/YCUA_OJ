package com.oj.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置: 注册认证拦截器, 提供 BCrypt 密码加密器
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final CertInterceptor certInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                // 注册/登录/找回密码/验证码无需登录态
                .excludePathPatterns("/api/user/login", "/api/user/register",
                        "/api/user/forgot-password", "/api/captcha");
        // 学生认证拦截: 登录态用户中, 未认证的普通用户只放行题目/比赛浏览与认证申请
        registry.addInterceptor(certInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/user/login", "/api/user/register",
                        "/api/user/forgot-password", "/api/captcha");
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
