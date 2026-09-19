package com.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 编辑比赛请求(创建者或管理端; 已开赛/已有提交时赛制与开始时间锁定, 结束时间只可延长)
 */
@Data
public class ContestUpdateRequest {

    @NotBlank(message = "比赛标题不能为空")
    @Size(max = 100, message = "标题最长 100 字")
    private String title;

    private String description;

    /** 新开始时间(null=不改; 仅未开赛且无提交时可改) */
    private LocalDateTime startTime;

    /** 新密码(空串=取消密码; null=不改) */
    private String password;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
}
