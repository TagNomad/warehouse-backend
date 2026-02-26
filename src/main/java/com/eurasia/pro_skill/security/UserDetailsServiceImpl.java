package com.eurasia.pro_skill.security;

import com.eurasia.pro_skill.entity.UserEntity;
import com.eurasia.pro_skill.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 从数据库加载用户信息，供 Spring Security 认证使用。
 * 直接操作 UserEntity（需要 password 字段），不经过 Domain 转换。
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity entity = userMapper.findByUsername(username);
        if (entity == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }
        if (entity.getStatus() == null || entity.getStatus() == 0) {
            throw new UsernameNotFoundException("用户已被禁用：" + username);
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(entity.getUsername())
                .password(entity.getPassword())
                // Spring Security roles() 会自动加 "ROLE_" 前缀
                .roles(entity.getRole())
                .build();
    }
}
