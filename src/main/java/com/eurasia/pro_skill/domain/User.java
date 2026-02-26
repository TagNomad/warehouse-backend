package com.eurasia.pro_skill.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户业务对象（Domain）
 *
 * <p>不含 password 等敏感/技术字段，天然可安全流通于 XxxxService → Controller。
 * 与 DB 结构解耦，由 {@link com.eurasia.pro_skill.converter.UserConverter} 与 UserEntity 互转。
 */
@Data
public class User {

    private Long id;
    private String username;
    private String displayName;
    private String role;
    private String email;
    private String phone;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
