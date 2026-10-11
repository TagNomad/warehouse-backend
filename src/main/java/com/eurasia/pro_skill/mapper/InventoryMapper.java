package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.InventoryEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

// 按货品和库位读写库存。SQL 只写在这一层。
@Mapper
public interface InventoryMapper {

    /**
     * 找这一种货在这一个库位上的那一行。
     * #{skuId}、#{locationId} 是调用时传入的两个主键。
     * 还没有这一行时返回 null。
     */
    @Select("SELECT * FROM inventory WHERE sku_id = #{skuId} AND location_id = #{locationId}")
    InventoryEntity getBySkuAndLocation(@Param("skuId") Long skuId, @Param("locationId") Long locationId);

    /**
     * 第一次入库到这个格子，插入一行。
     * id 不在列清单里，由 MySQL 自增。
     * useGeneratedKeys 把生成的主键写回 entity.id。
     * 返回值是写下了几行，成功是 1。
     */
    @Insert("INSERT INTO inventory (sku_id, location_id, quantity, version) VALUES (#{skuId}, #{locationId}, #{quantity}, #{version})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InventoryEntity entity);

    /**
     * 这个格子已经有一行时，把数量加上，version 加一。
     * #{delta} 是这次要加的数量。
     * 这里还不拿 version 当更新条件，乐观锁以后再加。
     */
    @Update("UPDATE inventory SET quantity = quantity + #{delta}, version = version + 1 WHERE sku_id = #{skuId} AND location_id = #{locationId}")
    int addQuantity(@Param("skuId") Long skuId, @Param("locationId") Long locationId, @Param("delta") int delta);

    // #{id} 是 inventory 这一行的主键。出库前先把整行读出来。
    @Select("SELECT * FROM inventory WHERE id = #{id}")
    InventoryEntity getById(Long id);

    // 直接把数量写成 #{quantity}。条件只有 id，不看 version，也不看剩余够不够。
    // 返回改了几行。找到这一行就是 1。
    @Update("UPDATE inventory SET quantity = #{quantity} WHERE id = #{id}")
    int updateQuantity(@Param("id") Long id, @Param("quantity") int quantity);

    // #{version} 必须等于读到的那一版，才把数量写成 #{quantity}，并把 version 加一。
    // 别人已经改过这一行时，影响行数是 0。
    @Update("UPDATE inventory SET quantity = #{quantity}, version = version + 1 WHERE id = #{id} AND version = #{version}")
    int updateQuantityIfVersion(@Param("id") Long id, @Param("quantity") int quantity, @Param("version") int version);
}
