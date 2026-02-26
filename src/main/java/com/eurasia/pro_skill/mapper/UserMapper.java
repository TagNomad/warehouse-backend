package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM tb_user ORDER BY id")
    List<User> findAll();

    @Select("SELECT * FROM tb_user WHERE username = #{username} LIMIT 1")
    User findByUsername(String username);
}
