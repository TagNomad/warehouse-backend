package com.eurasia.pro_skill.security;

import com.eurasia.pro_skill.entity.User;
import com.eurasia.pro_skill.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 从数据库加载用户信息，供 Spring Security 认证使用
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new UsernameNotFoundException("用户已被禁用：" + username);
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                // Spring Security roles() 会自动加 "ROLE_" 前缀
                .roles(user.getRole())
                .build();
    }
}
