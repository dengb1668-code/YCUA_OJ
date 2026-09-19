package com.oj.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 比赛赛制
 * 数据库存储 TINYINT, 通过 @EnumValue 自动映射
 */
@Getter
public enum ContestType {

    /** ACM 式: 按解题数排名, 同解题数比罚时(错一次 +20 分钟), 实时榜单 */
    ICPC(0, "ICPC"),
    /** 洛谷式: 按总分排名(每题取最高分), 比赛期间隐藏榜单 */
    OI(1, "OI"),
    /** 部分分实时制: 按总分排名, 实时榜单 */
    IOI(2, "IOI");

    /** 存储到数据库的整数值 */
    @EnumValue
    private final int code;

    /** 展示用名称 */
    private final String label;

    ContestType(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
