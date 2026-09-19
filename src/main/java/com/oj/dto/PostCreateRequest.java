package com.oj.dto;

import com.oj.enums.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发帖请求(讨论帖/题解)
 */
@Data
public class PostCreateRequest {

    @NotNull(message = "帖子类型不能为空")
    private PostType type;

    /** 关联题目ID(全局讨论可为空; 题解必须关联题目, 由 Service 校验) */
    private Long problemId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长 100 字")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;
}
