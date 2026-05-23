package com.eurasia.pro_skill.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户持久化对象（Entity / PO），与表 {@code tb_user} 字段一一对应。
 *
 * <p>用途：MyBatis 查询/插入时，一行表记录对应一个 UserEntity 实例。
 *
 * <p>注意：包含 {@code password}（BCrypt 哈希），不要作为 Controller 的返回值。
 *
 * <p>Java 属性名用驼峰（displayName），表列名用下划线（display_name），
 * 由 {@code application.yaml} 中 {@code map-underscore-to-camel-case: true} 自动映射。
 */
@Data
public class UserEntity {

    /** 主键，插入后由数据库自增 */
    private Long id;
    private String username;
    /** 存 BCrypt 哈希，不是明文 */
    private String password;
    private String displayName;
    private String role;
    private String email;
    private String phone;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
