package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.PageResponse;
import com.eurasia.pro_skill.converter.UserConverter;
import com.eurasia.pro_skill.domain.User;
import com.eurasia.pro_skill.entity.UserEntity;
import com.eurasia.pro_skill.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;

    public PageResponse<User> listPage(int current, int pageSize) {
        long total = userMapper.countAll();
        int offset = (current - 1) * pageSize;
        List<User> records = userMapper.findPage(pageSize, offset)
                .stream()
                .map(userConverter::toDomain)
                .toList();
        return PageResponse.of(current, pageSize, total, records);
    }

    /**
     * 创建新用户（管理员使用，可指定角色）
     *
     * @param username    用户名
     * @param password    明文密码
     * @param displayName 显示名称
     * @param role        角色
     * @param email       邮箱
     * @param phone       手机号
     * @return 新创建用户的 ID
     * @throws IllegalArgumentException 用户名已存在
     */
    public Long createUser(String username, String password, String displayName, String role,
                           String email, String phone) {
        // 检查用户名是否已存在
        UserEntity existingUser = userMapper.findByUsername(username);
        if (existingUser != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 构建新用户实体
        UserEntity newUser = new UserEntity();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setDisplayName(displayName != null ? displayName : username);
        newUser.setRole(role);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setStatus(1);
        LocalDateTime now = LocalDateTime.now();
        newUser.setCreatedAt(now);
        newUser.setUpdatedAt(now);

        userMapper.insert(newUser);
        return newUser.getId();
    }
}
