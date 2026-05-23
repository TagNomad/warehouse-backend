package com.eurasia.pro_skill.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 禁用用户响应（DELETE /users/{id}，软删除）。
 */
@Data
@AllArgsConstructor
@Schema(description = "禁用用户响应")
public class DeleteUserResponse {

    @Schema(description = "被禁用的用户 ID")
    private Long userId;

    @Schema(description = "提示消息")
    private String message;
}
