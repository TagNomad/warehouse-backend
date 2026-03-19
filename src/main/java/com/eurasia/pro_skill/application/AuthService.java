package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.converter.UserConverter;
import com.eurasia.pro_skill.domain.AuthToken;
import com.eurasia.pro_skill.domain.User;
import com.eurasia.pro_skill.entity.UserEntity;
import com.eurasia.pro_skill.mapper.UserMapper;
import com.eurasia.pro_skill.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
    private final PasswordEncoder passwordEncoder;

    /**
     * 登录并签发 JWT token
     *
     * @param username 登录账号
     * @param password 明文密码
     * @return {@link AuthToken} Domain 级登录结果，由 Controller 负责转换为响应体
     */
    public AuthToken login(String username, String password) {
        // 委托 Spring Security 完成认证（密码校验、用户状态校验）
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        UserEntity entity = userMapper.findByUsername(username);
        User user = userConverter.toDomain(entity);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return new AuthToken(token, user.getUsername(), user.getRole());
    }

    /**
     * 注册新用户
     *
     * @param username    用户名
     * @param password    明文密码
     * @param displayName 显示名称
     * @param email       邮箱
     * @param phone       手机号
     * @return 新注册用户的 ID
     * @throws IllegalArgumentException 用户名已存在
     */
    public Long register(String username, String password, String displayName, String email, String phone) {
        // 检查用户名是否已存在
        UserEntity existingUser = userMapper.findByUsername(username);
        if (existingUser != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 构建新用户实体
        UserEntity newUser = new UserEntity();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password)); // BCrypt 加密
        newUser.setDisplayName(displayName != null ? displayName : username);
        newUser.setRole("USER"); // 默认角色
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setStatus(1); // 正常状态
        LocalDateTime now = LocalDateTime.now();
        newUser.setCreatedAt(now);
        newUser.setUpdatedAt(now);

        userMapper.insert(newUser);
        return newUser.getId();
    }
}
