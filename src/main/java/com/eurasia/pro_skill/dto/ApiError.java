package com.eurasia.pro_skill.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 统一错误响应体
 * <pre>
 * {
 *   "code": 401,
 *   "message": "Unauthorized",
 *   "path": "/api/users",
 *   "timestamp": "2026-02-26T10:15:30.123"
 * }
 * </pre>
 */
@Getter
public class ApiError {

    private final int code;
    private final String message;
    private final String path;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private final LocalDateTime timestamp;

    public ApiError(int code, String message, String path) {
        this.code = code;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }
}
