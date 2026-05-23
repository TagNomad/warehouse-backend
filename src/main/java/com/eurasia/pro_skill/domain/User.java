package com.eurasia.pro_skill.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户领域对象（Domain / 业务对象）。
 *
 * <p>用途：在 Service 与 Controller 之间传递「可安全返回给前端」的用户信息。
 *
 * <p>与 {@link com.eurasia.pro_skill.entity.UserEntity} 的区别：
 * <ul>
 *   <li>Domain <strong>没有 password</strong>，避免密码进入 JSON 响应</li>
 *   <li>Entity 与数据库表字段一一对应，仅在 Service ↔ Mapper 之间使用</li>
 * </ul>
 *
 * <p>转换：{@link com.eurasia.pro_skill.converter.UserConverter#toDomain}
 */
@Data
public class User {

    private Long id;
    private String username;
    private String displayName;
    private String role;
    private String email;
    private String phone;
    /** 1=启用，0=禁用 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
