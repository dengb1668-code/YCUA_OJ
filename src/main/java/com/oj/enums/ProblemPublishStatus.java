package com.oj.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 题目生命周期。比赛可见性由 contest_problem 单独控制，不和发布状态混在一起。
 */
@Getter
public enum ProblemPublishStatus {

    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    ARCHIVED(2, "已归档");

    @EnumValue
    private final int code;

    private final String label;

    ProblemPublishStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
