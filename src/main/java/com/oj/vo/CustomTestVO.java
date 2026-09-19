package com.oj.vo;

import lombok.Data;

/**
 * 自定义测试结果
 */
@Data
public class CustomTestVO {

    /** 程序标准输出 */
    private String output;

    /** 错误信息(编译错误/运行时错误/超时), 无错误时为 null */
    private String error;

    public static CustomTestVO error(String message) {
        CustomTestVO vo = new CustomTestVO();
        vo.setError(message);
        return vo;
    }
}
