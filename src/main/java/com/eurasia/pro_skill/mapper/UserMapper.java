package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT COUNT(*) FROM tb_user")
    long countAll();

    @Select("SELECT * FROM tb_user ORDER BY id LIMIT #{pageSize} OFFSET #{offset}")
    List<UserEntity> findPage(@Param("pageSize") int pageSize, @Param("offset") int offset);

    @Select("SELECT * FROM tb_user WHERE username = #{username} LIMIT 1")
    UserEntity findByUsername(String username);
}
