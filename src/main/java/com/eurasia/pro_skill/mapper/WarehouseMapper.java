package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.WarehouseEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WarehouseMapper {

    @Insert("INSERT INTO warehouse (code, name, type, address, manager, phone, status, capacity, created_at, updated_at) " +
            "VALUES (#{code}, #{name}, #{type}, #{address}, #{manager}, #{phone}, #{status}, #{capacity}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WarehouseEntity entity);

    @Update("UPDATE warehouse SET name=#{name}, type=#{type}, address=#{address}, manager=#{manager}, " +
            "phone=#{phone}, status=#{status}, capacity=#{capacity}, updated_at=#{updatedAt} WHERE id=#{id}")
    int update(WarehouseEntity entity);

    @Delete("DELETE FROM warehouse WHERE id=#{id}")
    int delete(Long id);

    @Select("SELECT * FROM warehouse WHERE id=#{id}")
    WarehouseEntity getById(Long id);

    @Select("SELECT * FROM warehouse WHERE code=#{code}")
    WarehouseEntity getByCode(String code);

    @Select("<script>" +
            "SELECT * FROM warehouse " +
            "<where>" +
            "<if test='code != null and code != \"\"'>AND code LIKE CONCAT('%', #{code}, '%')</if>" +
            "<if test='name != null and name != \"\"'>AND name LIKE CONCAT('%', #{name}, '%')</if>" +
            "<if test='type != null and type != \"\"'>AND type = #{type}</if>" +
            "<if test='status != null and status != \"\"'>AND status = #{status}</if>" +
            "</where>" +
            "ORDER BY id DESC LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<WarehouseEntity> listPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
                                   @Param("code") String code, @Param("name") String name,
                                   @Param("type") String type, @Param("status") String status);

    @Select("<script>" +
            "SELECT COUNT(*) FROM warehouse " +
            "<where>" +
            "<if test='code != null and code != \"\"'>AND code LIKE CONCAT('%', #{code}, '%')</if>" +
            "<if test='name != null and name != \"\"'>AND name LIKE CONCAT('%', #{name}, '%')</if>" +
            "<if test='type != null and type != \"\"'>AND type = #{type}</if>" +
            "<if test='status != null and status != \"\"'>AND status = #{status}</if>" +
            "</where>" +
            "</script>")
    long count(@Param("code") String code, @Param("name") String name,
               @Param("type") String type, @Param("status") String status);
}
