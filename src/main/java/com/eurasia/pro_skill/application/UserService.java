package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.PageResponse;
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

    public PageResponse<User> listPage(int current, int pageSize) {
        long total = userMapper.countAll();
        int offset = (current - 1) * pageSize;
        List<User> records = userMapper.findPage(pageSize, offset)
                .stream()
                .map(userConverter::toDomain)
                .toList();
        return PageResponse.of(current, pageSize, total, records);
    }
}
