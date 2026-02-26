package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.dto.LoginRequest;
import com.eurasia.pro_skill.dto.LoginResponse;
import com.eurasia.pro_skill.entity.User;
import com.eurasia.pro_skill.mapper.UserMapper;
import com.eurasia.pro_skill.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
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

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Operation(summary = "登录", description = "用户名 + 密码登录，成功后返回 JWT token")
    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        // 委托 Spring Security 完成认证（密码校验、用户状态校验）
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userMapper.findByUsername(userDetails.getUsername());

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getUsername(), user.getRole());
    }
}
