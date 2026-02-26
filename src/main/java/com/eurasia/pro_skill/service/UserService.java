package com.eurasia.pro_skill.service;

import com.eurasia.pro_skill.converter.UserConverter;
import com.eurasia.pro_skill.domain.User;
import com.eurasia.pro_skill.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserConverter userConverter;

    public List<User> listAll() {
        return userMapper.findAll()
                .stream()
                .map(userConverter::toDomain)
                .toList();
    }
}
