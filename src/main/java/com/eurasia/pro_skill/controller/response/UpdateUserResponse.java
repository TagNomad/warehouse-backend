package com.eurasia.pro_skill.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 更新用户响应（PUT /users/{id}）。
 */
@Data
@AllArgsConstructor
@Schema(description = "更新用户响应")
public class UpdateUserResponse {

    @Schema(description = "更新后的用户信息")
    private UserResponse user;

    @Schema(description = "提示消息")
    private String message;
}
