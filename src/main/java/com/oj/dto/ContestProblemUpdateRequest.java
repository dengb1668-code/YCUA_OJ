package com.oj.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 更新比赛题目列表请求(顺序即 A/B/C... 题号)
 */
@Data
public class ContestProblemUpdateRequest {

    @NotNull(message = "题目列表不能为空")
    @Size(max = 100, message = "单场比赛最多 100 题")
    private List<Long> problemIds;
}
