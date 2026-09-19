package com.oj.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 编程语言
 * 数据库存储 TINYINT, 通过 @EnumValue 自动映射
 */
@Getter
public enum Language {

    JAVA(0, "Java"),
    CPP(1, "C++"),
    C(2, "C"),
    PYTHON3(3, "Python3"),
    GO(4, "Go"),
    JAVASCRIPT(5, "JavaScript");

    /** 存储到数据库的整数值 */
    @EnumValue
    private final int code;

    /** 展示用名称 */
    private final String label;

    Language(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
