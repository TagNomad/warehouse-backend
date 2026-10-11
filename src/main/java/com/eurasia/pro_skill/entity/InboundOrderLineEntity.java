package com.eurasia.pro_skill.entity;

import lombok.Data;

// 对应 inbound_order_line 表的一行。
@Data
public class InboundOrderLineEntity {

    private Long id; // 列 id
    private Long orderId; // 列 order_id
    private Long skuId; // 列 sku_id
    private Long locationId; // 列 location_id
    private Integer quantity; // 列 quantity
}
