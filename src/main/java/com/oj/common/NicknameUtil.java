package com.oj.common;

import com.oj.entity.User;

/**
 * 展示名工具: 昵称为空时回退用户名
 */
public final class NicknameUtil {

    private NicknameUtil() {
    }

    public static String nicknameOf(User user) {
        if (user == null) {
            return "未知用户";
        }
        return user.getNickname() == null || user.getNickname().isBlank()
                ? user.getUsername() : user.getNickname();
    }
}
