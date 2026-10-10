package com.sky.blog.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 统一响应体：{@code {"code":0,"message":"ok","data":{...},"timestamp":123}}。
 *
 * <p>约定 {@code code == 0} 表示成功；失败时 HTTP 状态码同时带上语义（401/403/404/409…）。
 */
@Schema(description = "统一响应包装")
public class ApiResult<T> {

    @Schema(description = "业务码，0 表示成功", example = "0")
    private int code;

    @Schema(description = "提示信息，可直接展示给用户", example = "ok")
    private String message;

    @Schema(description = "业务数据，失败时为 null")
    private T data;

    @Schema(description = "服务器时间戳（毫秒）")
    private long timestamp;

    public ApiResult() {
        this.timestamp = System.currentTimeMillis();
    }

    private ApiResult(int code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "ok", data);
    }

    public static ApiResult<Void> ok() {
        return new ApiResult<>(0, "ok", null);
    }

    public static <T> ApiResult<T> ok(T data, String message) {
        return new ApiResult<>(0, message, data);
    }

    public static <T> ApiResult<T> fail(ErrorCode errorCode) {
        return new ApiResult<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> ApiResult<T> fail(ErrorCode errorCode, String message) {
        return new ApiResult<>(errorCode.getCode(), message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
