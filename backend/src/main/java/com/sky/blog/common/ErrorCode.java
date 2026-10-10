package com.sky.blog.common;

/**
 * 业务错误码。
 *
 * <p>约定：HTTP 状态码表达"哪一类问题"，业务码表达"具体哪一条规则"，
 * 前端拦截器只看 HTTP 状态就能决定是跳登录还是弹提示。
 */
public enum ErrorCode {

    OK(0, "ok", 200),

    BAD_REQUEST(40000, "请求参数有误", 400),
    VALIDATION_FAILED(40001, "字段校验未通过", 400),

    UNAUTHORIZED(40100, "请先登录", 401),
    LOGIN_FAILED(40101, "用户名或密码错误", 401),
    ACCOUNT_DISABLED(40102, "账号已被禁用，请联系管理员", 401),
    SESSION_EXPIRED(40103, "登录状态已失效，请重新登录", 401),
    PASSWORD_CHANGED(40104, "密码已修改，请重新登录", 401),

    FORBIDDEN(40300, "没有权限执行该操作", 403),
    NOT_OWNER(40301, "只能操作自己的内容", 403),

    NOT_FOUND(40400, "内容不存在或已下架", 404),

    CONFLICT(40900, "状态已变化，请刷新后重试", 409),
    DUPLICATE(40901, "该内容已存在", 409),

    UNPROCESSABLE(42200, "操作被业务规则拒绝", 422),

    SERVER_ERROR(50000, "服务器内部错误", 500);

    private final int code;
    private final String message;
    private final int httpStatus;

    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
