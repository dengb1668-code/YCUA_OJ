package com.oj.enums;

import lombok.Getter;

/**
 * 用户对某道题的做题状态
 * 仅用于接口返回(由提交记录聚合计算), 不落库
 */
@Getter
public enum ProblemStatus {

    NOT_ATTEMPTED(0, "未尝试"),
    ATTEMPTED(1, "尝试未通过"),
    SOLVED(2, "已通过");

    private final int code;

    private final String label;

    ProblemStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
