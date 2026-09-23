package com.oj.dto;

import com.oj.entity.Problem;
import com.oj.enums.JudgeMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建题目请求
 */
@Data
public class ProblemCreateRequest {

    @NotBlank(message = "题目标题不能为空")
    private String title;

    /** 题目来源(可选, 如: 洛谷 P1001) */
    private String source;

    @NotBlank(message = "题目描述不能为空")
    private String description;

    private String inputDescription;

    private String outputDescription;

    /** 样例列表, 可为空 */
    private List<Problem.Sample> samples;

    /** 时间限制(毫秒) */
    @NotNull(message = "时间限制不能为空")
    private Integer timeLimit;

    /** 内存限制(MB) */
    @NotNull(message = "内存限制不能为空")
    private Integer memoryLimit;

    /** 难度(Codeforces Rating, 800-3500) */
    @NotNull(message = "难度不能为空")
    @Min(value = 800, message = "难度最低为 800")
    @Max(value = 3500, message = "难度最高为 3500")
    private Integer difficulty;

    /** 判题模式(可空, 默认 ICPC) */
    private JudgeMode judgeMode;

    /** 题目标签(可空, 从固定中文标签集多选, 服务端白名单过滤) */
    @Size(max = 5, message = "标签最多选 5 个")
    private List<String> tags;
}
