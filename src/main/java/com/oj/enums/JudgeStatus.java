package com.oj.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 判题状态
 * 数据库存储 TINYINT, 通过 @EnumValue 自动映射
 */
@Getter
public enum JudgeStatus {

    PENDING(0, "Pending"),
    JUDGING(1, "Judging"),
    ACCEPTED(2, "Accepted"),
    WRONG_ANSWER(3, "Wrong Answer"),
    TIME_LIMIT_EXCEEDED(4, "Time Limit Exceeded"),
    MEMORY_LIMIT_EXCEEDED(5, "Memory Limit Exceeded"),
    RUNTIME_ERROR(6, "Runtime Error"),
    COMPILE_ERROR(7, "Compile Error"),
    SYSTEM_ERROR(8, "System Error");

    /** 存储到数据库的整数值 */
    @EnumValue
    private final int code;

    /** 展示用名称 */
    private final String label;

    JudgeStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
