package com.eurasia.pro_skill.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 创建用户响应（POST /users）。
 *
 * <p>与 {@link RegisterResponse} 风格一致：含 userId、message，并附带完整 {@link UserResponse}。
 */
@Data
@AllArgsConstructor
@Schema(description = "创建用户响应")
public class CreateUserResponse {

    @Schema(description = "新用户 ID")
    private Long userId;

    @Schema(description = "新用户信息")
    private UserResponse user;

    @Schema(description = "提示消息")
    private String message;
}
