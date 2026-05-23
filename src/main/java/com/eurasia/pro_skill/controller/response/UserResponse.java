package com.eurasia.pro_skill.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应（不含 password），供列表、详情、更新等接口复用。
 *
 * <p>由 {@link UserResponseMapper} 从 Domain {@link com.eurasia.pro_skill.domain.User} 转换而来。
 */
@Data
@Schema(description = "用户信息（API 响应）")
public class UserResponse {

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "显示名称")
    private String displayName;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "状态：1-启用 0-禁用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
