package com.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 回复帖子请求
 */
@Data
public class ReplyRequest {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 10000, message = "回复内容过长")
    private String content;
}
