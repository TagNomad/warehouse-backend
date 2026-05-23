package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.UserEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户表数据访问层（Mapper / DAO）。
 *
 * <p>MyBatis 会在运行时为此接口生成实现类，把 {@code @Select/@Insert/@Update} 上的 SQL 发到 MySQL。
 *
 * <p>约定：
 * <ul>
 *   <li>方法名表达业务含义（findById、insert）</li>
 *   <li>参数用 {@code #{属性名}}，对应 {@link UserEntity} 的 Java 属性（如 displayName → display_name 由配置自动映射）</li>
 *   <li>不写 if/else 业务判断，复杂条件可用 {@code <script>} 动态 SQL</li>
 * </ul>
 *
 * <p>教学文档：{@code docs/用户管理CRUD全流程.md}
 */
@Mapper
public interface UserMapper {

    /** 按主键查一行，没有则返回 null */
    @Select("SELECT * FROM tb_user WHERE id = #{id}")
    UserEntity findById(Long id);

    /** 按登录名查一行，用于注册/创建前查重 */
    @Select("SELECT * FROM tb_user WHERE username = #{username} LIMIT 1")
    UserEntity findByUsername(String username);

    /**
     * 统计符合条件的总条数（分页用）。
     *
     * <p>{@code <script>}：动态 SQL，只有传入的筛选条件非空时才拼接到 WHERE。
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM tb_user " +
            "<where>" +
            "<if test='username != null and username != \"\"'>AND username LIKE CONCAT('%', #{username}, '%')</if>" +
            "<if test='role != null and role != \"\"'>AND role = #{role}</if>" +
            "<if test='status != null'>AND status = #{status}</if>" +
            "</where>" +
            "</script>")
    long count(@Param("username") String username, @Param("role") String role, @Param("status") Integer status);

    /**
     * 分页查询当前页数据。
     *
     * @param pageSize 每页条数（LIMIT）
     * @param offset   跳过前多少条（OFFSET），由 Service 计算
     */
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

    /**
     * 插入新用户（Create 的核心 SQL）。
     *
     * <p>{@code useGeneratedKeys}：插入后把数据库自增主键写回 entity.id。
     * <p>返回值 int：受影响行数，正常应为 1。
     */
    @Insert("INSERT INTO tb_user (username, password, display_name, role, email, phone, status, created_at, updated_at) " +
            "VALUES (#{username}, #{password}, #{displayName}, #{role}, #{email}, #{phone}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserEntity entity);

    /** 按主键更新一行（Update / 软删除都会走此方法） */
    @Update("UPDATE tb_user SET display_name=#{displayName}, role=#{role}, email=#{email}, phone=#{phone}, " +
            "status=#{status}, password=#{password}, updated_at=#{updatedAt} WHERE id=#{id}")
    int update(UserEntity entity);
}
