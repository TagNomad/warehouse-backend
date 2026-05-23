package com.eurasia.pro_skill.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 用户分页列表响应（GET /users）。
 *
 * <p>字段与 {@link com.eurasia.pro_skill.common.PageResponse} 对齐，便于前端分页组件绑定。
 */
@Data
@AllArgsConstructor
@Schema(description = "用户分页列表响应")
public class UserPageResponse {

    @Schema(description = "当前页码（1-based）")
    private int current;

    @Schema(description = "每页条数")
    private int pageSize;

    @Schema(description = "总记录数")
    private long total;

    @Schema(description = "当前页用户列表")
    private List<UserResponse> records;
}
