package com.eurasia.pro_skill.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应体，包含 JWT token 及用户基本信息
 */
@Data
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "JWT token，后续请求放入 Authorization: Bearer <token>")
    private String token;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "角色", example = "ADMIN")
    private String role;
}
