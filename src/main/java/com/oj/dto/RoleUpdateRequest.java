package com.oj.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改用户角色请求(仅站长可用, 只允许设为普通用户/管理员, 站长不可被修改)
 */
@Data
public class RoleUpdateRequest {

    @NotNull(message = "角色不能为空")
    @Min(value = 0, message = "角色值非法")
    @Max(value = 1, message = "角色值非法")
    private Integer role;
}
