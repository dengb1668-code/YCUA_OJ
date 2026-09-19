package com.oj.dto;

import com.oj.enums.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 自定义测试请求: 用给定输入本地运行代码
 */
@Data
public class CustomTestRequest {

    @NotNull(message = "编程语言不能为空")
    private Language language;

    @NotBlank(message = "代码不能为空")
    private String code;

    /** 测试输入(标准输入), 可为空 */
    private String input;
}
