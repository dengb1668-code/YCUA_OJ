package com.oj.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 用户角色
 * 数据库存储 TINYINT(编码保持不变, 仅展示名调整), 通过 @EnumValue 自动映射
 */
@Getter
public enum UserRole {

    USER(0, "认证学生"),
    ADMIN(1, "集训队负责人"),
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
