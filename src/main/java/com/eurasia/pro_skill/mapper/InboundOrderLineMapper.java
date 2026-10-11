package com.eurasia.pro_skill.mapper;

import com.eurasia.pro_skill.entity.InboundOrderLineEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InboundOrderLineMapper {

    @Insert("INSERT INTO inbound_order_line (order_id, sku_id, location_id, quantity) " +
            "VALUES (#{orderId}, #{skuId}, #{locationId}, #{quantity})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InboundOrderLineEntity entity);

    @Select("SELECT * FROM inbound_order_line WHERE order_id = #{orderId} ORDER BY id")
    List<InboundOrderLineEntity> listByOrderId(Long orderId);
}
