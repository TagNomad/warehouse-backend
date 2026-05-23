package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.common.PageResponse;
import com.eurasia.pro_skill.converter.UserConverter;
import com.eurasia.pro_skill.domain.User;
import com.eurasia.pro_skill.entity.UserEntity;
import com.eurasia.pro_skill.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;

    private final UserMapper userMapper;
    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<User> listPage(int current, int pageSize, String username, String role, Integer status) {
        int safeCurrent = Math.max(current, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safeCurrent - 1) * safePageSize;
        long total = userMapper.count(username, role, status);
        List<User> records = userMapper.findPage(safePageSize, offset, username, role, status)
                .stream()
                .map(userConverter::toDomain)
                .toList();
        return PageResponse.of(safeCurrent, safePageSize, total, records);
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        UserEntity entity = userMapper.findById(id);
        if (entity == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在：" + id);
        }
        return userConverter.toDomain(entity);
    }

    /**
     * 创建新用户（管理员使用，可指定角色）
     */
    public Long createUser(String username, String password, String displayName, String role,
                           String email, String phone) {
        if (userMapper.findByUsername(username) != null) {
            throw new BusinessException("USER_USERNAME_EXISTS", "用户名已存在：" + username);
        }

        UserEntity newUser = new UserEntity();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setDisplayName(displayName != null ? displayName : username);
        newUser.setRole(role);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setStatus(STATUS_ENABLED);
        LocalDateTime now = LocalDateTime.now();
        newUser.setCreatedAt(now);
        newUser.setUpdatedAt(now);

        userMapper.insert(newUser);
        return newUser.getId();
    }

    public User update(Long id, String displayName, String role, String email, String phone,
                       Integer status, String password) {
        UserEntity entity = userMapper.findById(id);
        if (entity == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在：" + id);
        }

        entity.setDisplayName(displayName);
        entity.setRole(role);
        entity.setEmail(email);
        entity.setPhone(phone);
        if (status != null) {
            entity.setStatus(status);
        }
        if (StringUtils.hasText(password)) {
            entity.setPassword(passwordEncoder.encode(password));
        }
        entity.setUpdatedAt(LocalDateTime.now());

        userMapper.update(entity);
        return userConverter.toDomain(entity);
    }

    public void delete(Long id) {
        UserEntity entity = userMapper.findById(id);
        if (entity == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在：" + id);
        }
        entity.setStatus(STATUS_DISABLED);
        entity.setUpdatedAt(LocalDateTime.now());
        userMapper.update(entity);
    }
}
