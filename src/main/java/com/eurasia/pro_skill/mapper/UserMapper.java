package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.UserEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM tb_user WHERE id = #{id}")
    UserEntity findById(Long id);

    @Select("SELECT * FROM tb_user WHERE username = #{username} LIMIT 1")
    UserEntity findByUsername(String username);

    @Select("<script>" +
            "SELECT COUNT(*) FROM tb_user " +
            "<where>" +
            "<if test='username != null and username != \"\"'>AND username LIKE CONCAT('%', #{username}, '%')</if>" +
            "<if test='role != null and role != \"\"'>AND role = #{role}</if>" +
            "<if test='status != null'>AND status = #{status}</if>" +
            "</where>" +
            "</script>")
    long count(@Param("username") String username, @Param("role") String role, @Param("status") Integer status);

    @Select("<script>" +
            "SELECT * FROM tb_user " +
            "<where>" +
            "<if test='username != null and username != \"\"'>AND username LIKE CONCAT('%', #{username}, '%')</if>" +
            "<if test='role != null and role != \"\"'>AND role = #{role}</if>" +
            "<if test='status != null'>AND status = #{status}</if>" +
            "</where>" +
            "ORDER BY id LIMIT #{pageSize} OFFSET #{offset}" +
            "</script>")
    List<UserEntity> findPage(@Param("pageSize") int pageSize, @Param("offset") int offset,
                              @Param("username") String username, @Param("role") String role,
                              @Param("status") Integer status);

    @Insert("INSERT INTO tb_user (username, password, display_name, role, email, phone, status, created_at, updated_at) " +
            "VALUES (#{username}, #{password}, #{displayName}, #{role}, #{email}, #{phone}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserEntity entity);

    @Update("UPDATE tb_user SET display_name=#{displayName}, role=#{role}, email=#{email}, phone=#{phone}, " +
            "status=#{status}, password=#{password}, updated_at=#{updatedAt} WHERE id=#{id}")
    int update(UserEntity entity);
}
