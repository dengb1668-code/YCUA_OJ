package com.oj.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 用户角色
 * 数据库存储 TINYINT, 通过 @EnumValue 自动映射
 */
@Getter
public enum UserRole {

    USER(0, "普通用户"),
    ADMIN(1, "管理员"),
    OWNER(2, "站长");

    /** 存储到数据库的整数值 */
    @EnumValue
    private final int code;

    /** 展示用名称 */
    private final String label;

    UserRole(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
