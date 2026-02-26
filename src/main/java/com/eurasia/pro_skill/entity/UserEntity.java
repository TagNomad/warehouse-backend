package com.eurasia.pro_skill.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户持久化对象（PO），1:1 映射 tb_user 表。
 *
 * <p>含 password 等所有原始字段，仅在 Mapper ↔ Service 之间流通，不出 Service 层。
 */
@Data
public class UserEntity {

    private Long id;
    private String username;
    /** DB 存储的 BCrypt 哈希，不向外暴露 */
    private String password;
    private String displayName;
    private String role;
    private String email;
    private String phone;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
