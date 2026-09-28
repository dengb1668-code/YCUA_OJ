package com.oj.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 学生认证审核(负责人/站长): 通过或驳回
 */
@Data
public class CertReviewRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "审核结论不能为空")
    private Boolean approve;

    /** 驳回原因(驳回时建议填写) */
    private String reason;
}
