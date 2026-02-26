package com.eurasia.pro_skill.vo;

import com.eurasia.pro_skill.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/** 对外暴露的用户视图对象（不含密码） */
@Data
@Schema(description = "用户信息")
public class UserVO {

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "显示名称")
    private String displayName;

    @Schema(description = "角色", example = "ADMIN / WAREHOUSE_MANAGER / OPERATOR / BUSINESS")
    private String role;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "状态：1-启用 0-禁用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    public static UserVO from(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setDisplayName(user.getDisplayName());
        vo.setRole(user.getRole());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
