package com.oj.dto;

import com.oj.enums.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 代码提交请求
 */
@Data
public class SubmitRequest {

    @NotNull(message = "题目ID不能为空")
    private Long problemId;

    /** 所属比赛ID(比赛内提交时必填; 后端校验时间窗与访问 token) */
    private Long contestId;

    @NotNull(message = "编程语言不能为空")
    private Language language;

    @NotBlank(message = "代码不能为空")
    private String code;
}
