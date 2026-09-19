package com.oj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oj.common.Result;
import com.oj.dto.ForgotPasswordRequest;
import com.oj.dto.LoginRequest;
import com.oj.dto.RegisterRequest;
import com.oj.dto.RoleUpdateRequest;
import com.oj.service.UserService;
import com.oj.vo.LoginVO;
import com.oj.vo.UserAdminVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户相关接口(注册/登录无需登录态, 在拦截器白名单中)
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 注册, 成功后自动登录并返回令牌
     * POST /api/user/register
     */
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(userService.register(request));
    }

    /**
     * 登录
     * POST /api/user/login
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(userService.login(request));
    }

    /**
     * 找回密码: 用户名 + 手机号 + 图形验证码匹配后重置(无需登录态)
     * POST /api/user/forgot-password
     */
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request);
        return Result.ok(null);
    }

    /**
     * 用户管理分页(仅站长, keyword 模糊匹配用户名/昵称)
     * GET /api/user/page
     */
    @GetMapping("/page")
    public Result<Page<UserAdminVO>> page(@RequestParam(defaultValue = "") String keyword,
                                          @RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(userService.pageUsers(keyword, pageNum, pageSize));
    }

    /**
     * 修改用户角色(仅站长, 只允许设为普通用户/管理员)
     * PUT /api/user/{id}/role
     */
    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        userService.updateRole(id, request.getRole());
        return Result.ok(null);
    }
}
