package com.northland.drools.common.response;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 通用响应对象
 */
@Data
@Accessors(chain = true)
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<T>()
                .setCode(200)
                .setMessage("Success")
                .setData(data);
    }

    public static <T> Result<T> error(String message) {
        return new Result<T>()
                .setCode(500)
                .setMessage(message);
    }
}
