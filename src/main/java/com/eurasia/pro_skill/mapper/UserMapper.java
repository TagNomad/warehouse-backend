package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT COUNT(*) FROM tb_user")
    long countAll();

    @Select("SELECT * FROM tb_user ORDER BY id LIMIT #{pageSize} OFFSET #{offset}")
    List<UserEntity> findPage(@Param("pageSize") int pageSize, @Param("offset") int offset);

    @Select("SELECT * FROM tb_user WHERE username = #{username} LIMIT 1")
    UserEntity findByUsername(String username);

    @Insert("INSERT INTO tb_user (username, password, display_name, role, email, phone, status, created_at, updated_at) " +
            "VALUES (#{username}, #{password}, #{displayName}, #{role}, #{email}, #{phone}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserEntity entity);
}
