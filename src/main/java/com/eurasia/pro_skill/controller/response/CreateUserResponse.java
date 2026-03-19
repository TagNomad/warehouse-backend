package com.eurasia.pro_skill.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 创建用户响应体
 */
@Data
@AllArgsConstructor
@Schema(description = "创建用户响应")
public class CreateUserResponse {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "创建成功消息")
    private String message;
}