package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.controller.request.LoginRequest;
import com.eurasia.pro_skill.controller.request.RegisterRequest;
import com.eurasia.pro_skill.controller.response.LoginResponse;
import com.eurasia.pro_skill.controller.response.RegisterResponse;
import com.eurasia.pro_skill.application.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
@Tag(name = "鉴权", description = "登录 / 注册 / token 签发")
@SecurityRequirements
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录", description = "用户名 + 密码登录，成功后返回 JWT token")
    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        var authToken = authService.login(request.getUsername(), request.getPassword());
        return new LoginResponse(authToken.token(), authToken.username(), authToken.role());
    }

    @Operation(summary = "注册", description = "注册新用户，默认角色为 USER")
    @PostMapping("/register")
    public RegisterResponse register(@RequestBody @Valid RegisterRequest request) {
        Long userId = authService.register(
                request.getUsername(),
                request.getPassword(),
                request.getDisplayName(),
                request.getEmail(),
                request.getPhone()
        );
        return new RegisterResponse(userId, request.getUsername(), "注册成功");
    }
}
