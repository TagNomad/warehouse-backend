package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.OutboundOrderLineEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

// 出库行的 SQL。行上记下的是货品主键和库位主键。
@Mapper
public interface OutboundOrderLineMapper {

    // #{orderId} 等四个值都取自 entity 的同名属性。id 不在列清单里。
    @Insert("INSERT INTO outbound_order_line (order_id, sku_id, location_id, quantity) " +
            "VALUES (#{orderId}, #{skuId}, #{locationId}, #{quantity})")
    @Options(useGeneratedKeys = true, keyProperty = "id") // 把生成的行主键写回 entity.id
    int insert(OutboundOrderLineEntity entity); // 成功是 1，不是主键

    // #{orderId} 是出库单主键。按行主键从小到大返回这一张单的全部行。
    @Select("SELECT * FROM outbound_order_line WHERE order_id = #{orderId} ORDER BY id")
    List<OutboundOrderLineEntity> listByOrderId(Long orderId);

    // #{id} 是行主键。改数量时用。
    @Select("SELECT * FROM outbound_order_line WHERE id = #{id}")
    OutboundOrderLineEntity getById(Long id);

    // #{quantity} 是新数量，#{id} 是行主键。这里不看单据状态，状态检查在 Service。
    @Update("UPDATE outbound_order_line SET quantity = #{quantity} WHERE id = #{id}")
    int updateQuantity(@Param("id") Long id, @Param("quantity") int quantity);
}
