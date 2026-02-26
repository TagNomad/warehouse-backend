package com.eurasia.pro_skill.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {

    private Long id;
    private String username;
    /** 返回给前端时需脱敏，在 VO 层屏蔽，此处保留完整字段 */
    private String password;
    private String displayName;
    private String role;
    private String email;
    private String phone;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
