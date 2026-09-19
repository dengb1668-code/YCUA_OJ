package com.oj.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 单个测试点的数据与计分配置(添加与编辑共用)
 */
@Data
public class TestCaseRequest {

    /** 测试点输入(允许空串) */
    @NotNull(message = "输入不能为空")
    @Size(max = 200000, message = "输入过长(最多 200000 字符)")
    private String input;

    /** 期望输出(允许空串) */
    @NotNull(message = "输出不能为空")
    @Size(max = 200000, message = "输出过长(最多 200000 字符)")
    private String output;

    /** 该点分值; 添加时为空则所有测试点重新均分 100 */
    @Min(value = 0, message = "分值不能为负")
    private Integer score;

    /** 独立时间限制(毫秒), null 用题目默认 */
    @Min(value = 1, message = "时间限制必须为正数")
    private Integer timeLimit;

    /** 独立内存限制(MB), null 用题目默认 */
    @Min(value = 1, message = "内存限制必须为正数")
    private Integer memoryLimit;
}
