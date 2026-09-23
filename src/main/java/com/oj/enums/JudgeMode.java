package com.oj.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 题目级判题模式(出题人创建/编辑题目时选择)
 * 数据库存储 TINYINT, 通过 @EnumValue 自动映射
 */
@Getter
public enum JudgeMode {

    /** ICPC/CF 式: 无部分分, 第一个失败测试点即停, 结果展示 "Wrong answer on test N" */
    ICPC(0, "ICPC"),
    /** IOI 式: 跑完全部测试点, 逐点按分值计分(洛谷式部分分) */
    IOI(1, "IOI");

    /** 存储到数据库的整数值 */
    @EnumValue
    private final int code;

    /** 展示用名称 */
    private final String label;

    JudgeMode(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
