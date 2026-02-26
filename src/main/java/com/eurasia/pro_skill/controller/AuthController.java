package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.controller.request.LoginRequest;
import com.eurasia.pro_skill.controller.response.LoginResponse;
import com.eurasia.pro_skill.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 鉴权控制器：提供登录与 token 签发能力
 */
@Tag(name = "鉴权", description = "登录 / token 签发")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录", description = "用户名 + 密码登录，成功后返回 JWT token")
    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request.getUsername(), request.getPassword());
    }
}
