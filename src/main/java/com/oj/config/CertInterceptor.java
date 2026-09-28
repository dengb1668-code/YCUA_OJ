package com.oj.config;

import com.oj.common.UserContext;
import com.oj.entity.User;
import com.oj.enums.CertStatus;
import com.oj.enums.UserRole;
import com.oj.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 学生认证拦截器(在 AuthInterceptor 之后执行):
 * 普通用户(USER)未通过学生认证时, 只允许浏览题目与比赛、查看/提交自己的认证申请,
 * 其余接口(提交代码/自测/讨论/博客/加入比赛等)一律 403, 提示先完成学生认证。
 * 负责人/站长不受限。
 */
@Component
@RequiredArgsConstructor
public class CertInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Long userId = UserContext.getUserId();
        // 未登录请求由 AuthInterceptor 处理
        if (userId == null) {
            return true;
        }
        User user = userMapper.selectById(userId);
        if (user == null || user.getRole() != UserRole.USER
                || user.getCertStatus() == CertStatus.APPROVED) {
            return true;
        }
        if (isCertAllowed(request)) {
            return true;
        }
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"请先完成学生认证\"}");
        return false;
    }

    /** 未认证学生白名单: 题目/比赛浏览 + 认证申请/查询 */
    private boolean isCertAllowed(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("POST".equalsIgnoreCase(method) && "/api/user/cert/apply".equals(path)) {
            return true;
        }
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }
        if ("/api/user/cert".equals(path)
                || "/api/problem/page".equals(path)
                || "/api/problem/tags".equals(path)
                || "/api/contest/page".equals(path)
                || path.matches("/api/problem/\\d+")
                || path.matches("/api/contest/\\d+/problems")
                || path.matches("/api/contest/\\d+/standings")
                || path.matches("/api/contest/\\d+")) {
            return true;
        }
        return false;
    }
}
