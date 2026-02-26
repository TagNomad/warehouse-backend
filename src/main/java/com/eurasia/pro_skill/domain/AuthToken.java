package com.eurasia.pro_skill.domain;

/**
 * 登录认证结果（Domain），表示一次成功签发的 JWT token 及关联的用户基本信息。
 *
 * <p>使用 record 保证不可变性，无需 Lombok。
 * 由 {@link com.eurasia.pro_skill.application.AuthService} 返回，
 * Controller 负责将其转换为 {@link com.eurasia.pro_skill.controller.response.LoginResponse}。
 */
public record AuthToken(String token, String username, String role) {}
