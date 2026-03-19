package com.eurasia.pro_skill.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 注册响应体
 */
@Data
@AllArgsConstructor
@Schema(description = "注册响应")
public class RegisterResponse {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "注册成功消息")
    private String message;
}