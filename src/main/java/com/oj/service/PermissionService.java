package com.oj.service;

import com.oj.common.UserContext;
import com.oj.entity.User;
import com.oj.enums.UserRole;
import com.oj.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 权限工具: 角色判定与操作人校验的公共逻辑。
 * <p>
 * 角色体系: 站长(OWNER) > 管理员(ADMIN) > 普通用户(USER)。
 * 管理员与站长统称"管理端"(manager), 可管理全站题目与内容;
 * 仅站长可分配/撤销管理员。
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserMapper userMapper;

    /** 查库判定某用户是否为管理端(管理员或站长), 用户不存在返回 false */
    public boolean isManager(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && isManagerRole(user.getRole());
    }

    /** 当前登录用户是否为管理端 */
    public boolean isCurrentManager() {
        return isManager(UserContext.getUserId());
    }

    /** 当前登录用户必须为管理端, 否则抛异常 */
    public void requireManager() {
        if (!isCurrentManager()) {
            throw new IllegalArgumentException("无权限执行该操作");
        }
    }

    /** 当前登录用户必须为站长, 否则抛异常 */
    public void requireOwner() {
        User user = userMapper.selectById(UserContext.getUserId());
        if (user == null || user.getRole() != UserRole.OWNER) {
            throw new IllegalArgumentException("仅站长可执行该操作");
        }
    }

    /** 操作人校验: 内容作者本人或管理端可操作 */
    public void requireAuthorOrManager(Long authorId) {
        if (authorId.equals(UserContext.getUserId()) || isCurrentManager()) {
            return;
        }
        throw new IllegalArgumentException("无权限操作他人内容");
    }

    private boolean isManagerRole(UserRole role) {
        return role == UserRole.ADMIN || role == UserRole.OWNER;
    }
}
