package com.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 图片验证码响应: id 用于登录时回传校验, image 为 base64 data URL
 */
@Data
@AllArgsConstructor
public class CaptchaVO {

    private String captchaId;

    private String image;
}
