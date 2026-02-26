package com.eurasia.pro_skill.exception;

import com.eurasia.pro_skill.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器（Controller 层及以内）
 *
 * <p>Filter 层的 401/403 由 {@link com.eurasia.pro_skill.config.SecurityConfiguration}
 * 中的 AuthenticationEntryPoint / AccessDeniedHandler 负责，此处无法捕获。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 400 参数校验失败 ──────────────────────────────────────────────────────

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiError handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return new ApiError(HttpStatus.BAD_REQUEST.value(), message, request.getRequestURI());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiError handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return new ApiError(HttpStatus.BAD_REQUEST.value(), "请求体格式错误", request.getRequestURI());
    }

    // ── 401 登录认证失败（来自 /auth/login 业务层）────────────────────────────

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ApiError handleBadCredentials(RuntimeException ex, HttpServletRequest request) {
        return new ApiError(HttpStatus.UNAUTHORIZED.value(), "用户名或密码错误", request.getRequestURI());
    }

    // ── 404 资源不存在 ────────────────────────────────────────────────────────

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException.class)
    public ApiError handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        return new ApiError(HttpStatus.NOT_FOUND.value(), "资源不存在: " + request.getRequestURI(), request.getRequestURI());
    }

    // ── 403 方法级权限不足（@PreAuthorize 抛出，在 Controller 层被捕获）────────

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ApiError handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return new ApiError(HttpStatus.FORBIDDEN.value(), "无权访问此资源", request.getRequestURI());
    }

    // ── 500 兜底 ──────────────────────────────────────────────────────────────

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiError handleGeneral(Exception ex, HttpServletRequest request) {
        return new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误", request.getRequestURI());
    }
}
