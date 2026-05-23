package com.eurasia.pro_skill.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建用户时，前端 POST 的 JSON 请求体结构。
 *
 * <p>只出现在 Controller 层：Swagger 展示、{@code @Valid} 校验。
 * Controller 会把字段拆开传给 {@link com.eurasia.pro_skill.application.UserService#createUser}。
 *
 * <p>{@code @Data}：Lombok 自动生成 getter/setter，Spring 才能把 JSON 填进对象。
 */
@Data
@Schema(description = "创建用户请求")
public class CreateUserRequest {

    /** 登录账号，全局唯一，对应表字段 username */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度需在 3-50 个字符之间")
    @Schema(description = "用户名", example = "newuser")
    private String username;

    /** 明文密码，仅用于传输；入库前会在 Service 里 BCrypt 加密 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度需在 6-100 个字符之间")
    @Schema(description = "密码", example = "Password@123")
    private String password;

    /** 界面显示名，可不填，Service 会默认用 username */
    @Schema(description = "显示名称", example = "新用户")
    private String displayName;

    /** 角色，需与库中约定一致，见 Flyway 种子数据 */
    @NotBlank(message = "角色不能为空")
    @Schema(description = "角色", example = "OPERATOR",
            allowableValues = {"ADMIN", "WAREHOUSE_MANAGER", "OPERATOR", "BUSINESS"})
    private String role;

    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;
}
