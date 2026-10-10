package com.sky.blog.common;

import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：把各类异常统一转成 {@link ApiResult} + 合适的 HTTP 状态码。
 *
 * <p>注意：兜底分支只回"服务器内部错误"，<b>绝不把堆栈或原始异常信息写进响应体</b>，
 * 避免泄漏 SQL 片段、文件路径等实现细节（作业 2.2.1 的"不得泄漏"要求同样适用于错误响应）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：按错误码自带的 HTTP 状态返回 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResult<Void>> handleBiz(BizException e) {
        ErrorCode ec = e.getErrorCode();
        log.debug("业务异常 code={} msg={}", ec.getCode(), e.getMessage());
        return ResponseEntity.status(ec.getHttpStatus()).body(ApiResult.fail(ec, e.getMessage()));
    }

    /** @Valid 在请求体上失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleBodyValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::describe)
                .collect(Collectors.joining("；"));
        return badRequest(detail.isEmpty() ? ErrorCode.VALIDATION_FAILED.getMessage() : detail);
    }

    /** 表单/查询参数绑定失败 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResult<Void>> handleBind(BindException e) {
        String detail = e.getFieldErrors().stream()
                .map(GlobalExceptionHandler::describe)
                .collect(Collectors.joining("；"));
        return badRequest(detail);
    }

    /** 方法参数上的 @NotNull / @Size 等（@Validated 在类上） */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleConstraint(ConstraintViolationException e) {
        String detail = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("；"));
        return badRequest(detail);
    }

    /** Spring 对 controller 方法参数做校验时抛的这个（Spring 7 用 getAllErrors()） */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodValidation(HandlerMethodValidationException e) {
        String detail = e.getAllErrors().stream()
                .map(resolvable -> {
                    String msg = resolvable.getDefaultMessage();
                    if (msg != null && !msg.isBlank()) {
                        return msg;
                    }
                    String[] codes = resolvable.getCodes();
                    return (codes == null || codes.length == 0) ? null : codes[0];
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.joining("；"));
        return badRequest(detail);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResult<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return badRequest("缺少必要参数：" + e.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        return badRequest("请求体格式不正确（需要合法 JSON）");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return badRequest("该接口不支持 " + e.getMethod() + " 方法");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResult<Void>> handleTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.badRequest()
                .body(ApiResult.fail(ErrorCode.BAD_REQUEST, "上传文件超过大小限制"));
    }

    /** 静态资源/未知路径：交回默认处理，避免把 404 伪装成业务错误 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(ErrorCode.NOT_FOUND.getHttpStatus())
                .body(ApiResult.fail(ErrorCode.NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleOther(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(ErrorCode.SERVER_ERROR.getHttpStatus())
                .body(ApiResult.fail(ErrorCode.SERVER_ERROR));
    }

    private static ResponseEntity<ApiResult<Void>> badRequest(String detail) {
        String msg = (detail == null || detail.isBlank())
                ? ErrorCode.VALIDATION_FAILED.getMessage() : detail;
        return ResponseEntity.badRequest().body(ApiResult.fail(ErrorCode.VALIDATION_FAILED, msg));
    }

    private static String describe(FieldError err) {
        return err.getField() + ": " + err.getDefaultMessage();
    }
}
