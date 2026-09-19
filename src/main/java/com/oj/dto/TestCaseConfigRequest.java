package com.oj.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 行内更新测试点计分配置(洛谷数据点配置风格)
 */
@Data
public class TestCaseConfigRequest {

    /** 该点分值; null 保留原分值 */
    @Min(value = 0, message = "分值不能为负")
    private Integer score;

    /** 独立时间限制(毫秒), null 表示清除(用题目默认) */
    @Min(value = 1, message = "时间限制必须为正数")
    private Integer timeLimit;

    /** 独立内存限制(MB), null 表示清除(用题目默认) */
    @Min(value = 1, message = "内存限制必须为正数")
    private Integer memoryLimit;
}
