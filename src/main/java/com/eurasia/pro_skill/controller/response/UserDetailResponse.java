package com.eurasia.pro_skill.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户详情响应（GET /users/{id}）。
 */
@Data
@AllArgsConstructor
@Schema(description = "用户详情响应")
public class UserDetailResponse {

    @Schema(description = "用户信息")
    private UserResponse user;

    @Schema(description = "提示消息")
    private String message;
}
