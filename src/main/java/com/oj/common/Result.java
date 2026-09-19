package com.oj.common;

import lombok.Data;

/**
 * 统一响应包装
 */
@Data
public class Result<T> {

    /** 0 表示成功, 非 0 表示失败 */
    private Integer code;

    private String message;

    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setCode(0);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
