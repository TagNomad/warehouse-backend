package com.eurasia.pro_skill.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建用户请求体（管理员使用）
 */
@Data
@Schema(description = "创建用户请求")
public class CreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度需在 3-50 个字符之间")
    @Schema(description = "用户名", example = "newuser")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度需在 6-100 个字符之间")
    @Schema(description = "密码", example = "Password@123")
    private String password;

    @Schema(description = "显示名称", example = "新用户")
    private String displayName;

    @NotBlank(message = "角色不能为空")
    @Schema(description = "角色", example = "OPERATOR",
            allowableValues = {"ADMIN", "WAREHOUSE_MANAGER", "OPERATOR", "BUSINESS"})
    private String role;

    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;
}