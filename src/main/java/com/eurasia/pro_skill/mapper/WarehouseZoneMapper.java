package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.WarehouseZoneEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WarehouseZoneMapper {

    @Insert("INSERT INTO warehouse_zone (warehouse_id, code, name, type, status, created_at, updated_at) " +
            "VALUES (#{warehouseId}, #{code}, #{name}, #{type}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WarehouseZoneEntity entity);

    @Update("UPDATE warehouse_zone SET name=#{name}, type=#{type}, status=#{status}, updated_at=#{updatedAt} " +
            "WHERE id=#{id}")
    int update(WarehouseZoneEntity entity);

    @Delete("DELETE FROM warehouse_zone WHERE id=#{id}")
    int delete(Long id);

    @Select("SELECT * FROM warehouse_zone WHERE id=#{id}")
    WarehouseZoneEntity getById(Long id);

    @Select("SELECT * FROM warehouse_zone WHERE warehouse_id=#{warehouseId} AND code=#{code}")
    WarehouseZoneEntity getByWarehouseIdAndCode(@Param("warehouseId") Long warehouseId, @Param("code") String code);

    @Select("SELECT COUNT(*) FROM warehouse_zone WHERE warehouse_id=#{warehouseId}")
    long countByWarehouseId(Long warehouseId);

    @Select("<script>" +
            "SELECT * FROM warehouse_zone " +
            "<where>" +
            "<if test='warehouseId != null'>AND warehouse_id = #{warehouseId}</if>" +
            "<if test='code != null and code != \"\"'>AND code LIKE CONCAT('%', #{code}, '%')</if>" +
            "<if test='name != null and name != \"\"'>AND name LIKE CONCAT('%', #{name}, '%')</if>" +
            "<if test='type != null and type != \"\"'>AND type = #{type}</if>" +
            "<if test='status != null and status != \"\"'>AND status = #{status}</if>" +
            "</where>" +
            "ORDER BY id DESC LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<WarehouseZoneEntity> listPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
                                     @Param("warehouseId") Long warehouseId,
                                     @Param("code") String code, @Param("name") String name,
                                     @Param("type") String type, @Param("status") String status);

    @Select("<script>" +
            "SELECT COUNT(*) FROM warehouse_zone " +
            "<where>" +
            "<if test='warehouseId != null'>AND warehouse_id = #{warehouseId}</if>" +
            "<if test='code != null and code != \"\"'>AND code LIKE CONCAT('%', #{code}, '%')</if>" +
            "<if test='name != null and name != \"\"'>AND name LIKE CONCAT('%', #{name}, '%')</if>" +
            "<if test='type != null and type != \"\"'>AND type = #{type}</if>" +
            "<if test='status != null and status != \"\"'>AND status = #{status}</if>" +
            "</where>" +
            "</script>")
    long count(@Param("warehouseId") Long warehouseId,
               @Param("code") String code, @Param("name") String name,
               @Param("type") String type, @Param("status") String status);

    @Select("SELECT * FROM warehouse_zone WHERE warehouse_id=#{warehouseId} ORDER BY id ASC")
    List<WarehouseZoneEntity> listByWarehouseId(Long warehouseId);
}
