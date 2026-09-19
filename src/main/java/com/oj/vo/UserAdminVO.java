package com.oj.vo;

import com.oj.entity.User;
import com.oj.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户管理列表项(仅站长可见的用户信息)
 */
@Data
public class UserAdminVO {

    private Long id;

    private String username;

    private String nickname;

    private String phone;

    private UserRole role;

    private LocalDateTime createTime;

    public static UserAdminVO from(User user) {
        UserAdminVO vo = new UserAdminVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setRole(user.getRole());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
