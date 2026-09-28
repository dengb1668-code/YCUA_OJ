package com.oj.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 学生认证状态。普通用户(USER)认证通过后才能提交代码。
 * 负责人/站长无需认证。
 */
@Getter
public enum CertStatus {

    NONE(0, "未认证"),
    PENDING(1, "待审核"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已驳回");

    @EnumValue
    private final int code;

    private final String label;

    CertStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
