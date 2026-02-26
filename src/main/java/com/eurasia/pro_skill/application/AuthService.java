package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.controller.response.LoginResponse;
import com.eurasia.pro_skill.converter.UserConverter;
import com.eurasia.pro_skill.domain.User;
import com.eurasia.pro_skill.entity.UserEntity;
import com.eurasia.pro_skill.mapper.UserMapper;
import com.eurasia.pro_skill.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * 鉴权业务服务：封装登录认证与 token 签发逻辑
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final UserConverter userConverter;

    /**
     * 登录并签发 JWT token
     *
     * @param username 登录账号
     * @param password 明文密码
     * @return 包含 token 及基本用户信息的登录响应
     */
    public LoginResponse login(String username, String password) {
        // 委托 Spring Security 完成认证（密码校验、用户状态校验）
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        UserEntity entity = userMapper.findByUsername(username);
        User user = userConverter.toDomain(entity);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getUsername(), user.getRole());
    }
}
