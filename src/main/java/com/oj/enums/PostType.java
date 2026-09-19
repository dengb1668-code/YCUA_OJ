package com.oj.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 帖子类型: 讨论帖 / 题解
 * 数据库存储 TINYINT, 通过 @EnumValue 自动映射
 */
@Getter
public enum PostType {

    DISCUSSION(0, "讨论"),
    SOLUTION(1, "题解");

    /** 存储到数据库的整数值 */
    @EnumValue
    private final int code;

    /** 展示用名称 */
    private final String label;

    PostType(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
