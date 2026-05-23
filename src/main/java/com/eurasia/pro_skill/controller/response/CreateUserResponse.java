package com.eurasia.pro_skill.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 创建用户成功后的响应体（仅本接口使用，其它查询接口直接返回 {@link com.eurasia.pro_skill.domain.User}）。
 *
 * <p>教学提示：若 {@code userId} 为 null，说明 insert 未执行或未回填自增 id，应查 Service 与 Mapper。
 */
@Data
@AllArgsConstructor
@Schema(description = "创建用户响应")
public class CreateUserResponse {

    /** 数据库自增主键，insert 成功后由 MyBatis 写回 */
    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "创建成功消息")
    private String message;
}
