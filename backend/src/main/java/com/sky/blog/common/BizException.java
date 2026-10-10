package com.sky.blog.common;

/** 带业务码和 HTTP 状态的业务异常，由 {@link GlobalExceptionHandler} 统一转成响应体。 */
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 保留错误码，但换掉对外文案（例如"用户名已存在"复用 DUPLICATE）。 */
    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /** 归属校验失败这类场景直接用它抛。 */
    public static BizException notOwner() {
        return new BizException(ErrorCode.NOT_OWNER);
    }

    public static BizException notFound() {
        return new BizException(ErrorCode.NOT_FOUND);
    }

    public static BizException conflict(String message) {
        return new BizException(ErrorCode.CONFLICT, message);
    }

    public static BizException unprocessable(String message) {
        return new BizException(ErrorCode.UNPROCESSABLE, message);
    }
}
