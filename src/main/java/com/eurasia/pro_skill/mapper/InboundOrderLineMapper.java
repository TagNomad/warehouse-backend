package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.InboundOrderLineEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

// 入库行的 SQL。一行里记下的是货品主键和库位主键，不是页面传来的编码。
@Mapper
public interface InboundOrderLineMapper {

    // #{orderId}、#{skuId}、#{locationId}、#{quantity} 都取自参数 entity 的同名属性。
    // 列清单里没有 id，主键由 MySQL 自增。
    @Insert("INSERT INTO inbound_order_line (order_id, sku_id, location_id, quantity) " +
            "VALUES (#{orderId}, #{skuId}, #{locationId}, #{quantity})")
    @Options(useGeneratedKeys = true, keyProperty = "id") // 把生成的行主键写回 entity.id
    int insert(InboundOrderLineEntity entity); // 成功插入一行就是 1，不是主键

    // #{orderId} 是这张入库单的主键。按行主键从小到大，返回这一张单上的全部行。
    @Select("SELECT * FROM inbound_order_line WHERE order_id = #{orderId} ORDER BY id")
    List<InboundOrderLineEntity> listByOrderId(Long orderId);
}
