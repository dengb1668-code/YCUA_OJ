package com.oj.dto;

import com.oj.enums.ContestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建比赛请求(任何登录用户可创建)
 */
@Data
public class ContestCreateRequest {

    @NotBlank(message = "比赛标题不能为空")
    @Size(max = 100, message = "标题最长 100 字")
    private String title;

    /** 比赛说明(Markdown, 可选) */
    private String description;

    @NotNull(message = "赛制不能为空")
    private ContestType type;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /** 参赛密码(可选, 非空则为密码制) */
    private String password;

    /** 初始题目列表(按顺序生成 A/B/C... 题号, 可选, 创建后未开赛可再改) */
    private List<Long> problemIds;
}
