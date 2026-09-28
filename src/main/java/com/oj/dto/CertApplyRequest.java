package com.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 学生认证申请(姓名 + 年级 + 专业)
 */
@Data
public class CertApplyRequest {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名过长")
    private String realName;

    @NotBlank(message = "年级不能为空")
    @Size(max = 20, message = "年级过长")
    private String grade;

    @NotBlank(message = "专业不能为空")
    @Size(max = 50, message = "专业过长")
    private String major;
}
