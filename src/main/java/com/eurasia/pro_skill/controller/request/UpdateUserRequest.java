package com.eurasia.pro_skill.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户请求体（管理员使用）
 */
@Data
@Schema(description = "更新用户请求")
public class UpdateUserRequest {

    @NotBlank(message = "显示名称不能为空")
    @Size(max = 64, message = "显示名称长度不能超过 64 个字符")
    @Schema(description = "显示名称", example = "张三")
    private String displayName;

    @NotBlank(message = "角色不能为空")
    @Schema(description = "角色", example = "OPERATOR",
            allowableValues = {"ADMIN", "WAREHOUSE_MANAGER", "OPERATOR", "BUSINESS"})
    private String role;

    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "状态：1-启用 0-禁用", example = "1", allowableValues = {"0", "1"})
    private Integer status;

    @Size(min = 6, max = 100, message = "密码长度需在 6-100 个字符之间")
    @Schema(description = "新密码（不传则不修改）", example = "Password@123")
    private String password;
}
