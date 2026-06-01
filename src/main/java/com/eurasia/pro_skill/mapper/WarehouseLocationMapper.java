package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.WarehouseLocationEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WarehouseLocationMapper {

    @Insert("INSERT INTO warehouse_location (zone_id, code, name, spec, status, created_at, updated_at) " +
            "VALUES (#{zoneId}, #{code}, #{name}, #{spec}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WarehouseLocationEntity entity);

    @Update("UPDATE warehouse_location SET name=#{name}, spec=#{spec}, status=#{status}, updated_at=#{updatedAt} " +
            "WHERE id=#{id}")
    int update(WarehouseLocationEntity entity);

    @Delete("DELETE FROM warehouse_location WHERE id=#{id}")
    int delete(Long id);

    @Select("SELECT * FROM warehouse_location WHERE id=#{id}")
    WarehouseLocationEntity getById(Long id);

    @Select("SELECT * FROM warehouse_location WHERE zone_id=#{zoneId} AND code=#{code}")
    WarehouseLocationEntity getByZoneIdAndCode(@Param("zoneId") Long zoneId, @Param("code") String code);

    @Select("SELECT COUNT(*) FROM warehouse_location WHERE zone_id=#{zoneId}")
    long countByZoneId(Long zoneId);

    @Select("<script>" +
            "SELECT l.* FROM warehouse_location l " +
            "INNER JOIN warehouse_zone z ON l.zone_id = z.id " +
            "<where>" +
            "<if test='zoneId != null'>AND l.zone_id = #{zoneId}</if>" +
            "<if test='warehouseId != null'>AND z.warehouse_id = #{warehouseId}</if>" +
            "<if test='code != null and code != \"\"'>AND l.code LIKE CONCAT('%', #{code}, '%')</if>" +
            "<if test='name != null and name != \"\"'>AND l.name LIKE CONCAT('%', #{name}, '%')</if>" +
            "<if test='status != null and status != \"\"'>AND l.status = #{status}</if>" +
            "</where>" +
            "ORDER BY l.id DESC LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<WarehouseLocationEntity> listPage(@Param("offset") int offset, @Param("pageSize") int pageSize,
                                           @Param("warehouseId") Long warehouseId, @Param("zoneId") Long zoneId,
                                           @Param("code") String code, @Param("name") String name,
                                           @Param("status") String status);

    @Select("<script>" +
            "SELECT COUNT(*) FROM warehouse_location l " +
            "INNER JOIN warehouse_zone z ON l.zone_id = z.id " +
            "<where>" +
            "<if test='zoneId != null'>AND l.zone_id = #{zoneId}</if>" +
            "<if test='warehouseId != null'>AND z.warehouse_id = #{warehouseId}</if>" +
            "<if test='code != null and code != \"\"'>AND l.code LIKE CONCAT('%', #{code}, '%')</if>" +
            "<if test='name != null and name != \"\"'>AND l.name LIKE CONCAT('%', #{name}, '%')</if>" +
            "<if test='status != null and status != \"\"'>AND l.status = #{status}</if>" +
            "</where>" +
            "</script>")
    long count(@Param("warehouseId") Long warehouseId, @Param("zoneId") Long zoneId,
               @Param("code") String code, @Param("name") String name, @Param("status") String status);

    @Select("SELECT * FROM warehouse_location WHERE zone_id=#{zoneId} ORDER BY id ASC")
    List<WarehouseLocationEntity> listByZoneId(Long zoneId);
}
