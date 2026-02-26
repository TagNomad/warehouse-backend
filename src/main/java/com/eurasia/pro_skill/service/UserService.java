package com.eurasia.pro_skill.service;

import com.eurasia.pro_skill.mapper.UserMapper;
import com.eurasia.pro_skill.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public List<UserVO> listAll() {
        return userMapper.findAll()
                .stream()
                .map(UserVO::from)
                .toList();
    }
}
