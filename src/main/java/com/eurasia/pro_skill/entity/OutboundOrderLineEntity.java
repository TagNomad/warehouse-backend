package com.eurasia.pro_skill.entity;

import lombok.Data;

// 对应 outbound_order_line 表的一行。
@Data
public class OutboundOrderLineEntity {

    private Long id; // 列 id
    private Long orderId; // 列 order_id，属于哪张出库单
    private Long skuId; // 列 sku_id，货品主键，不是编码
    private Long locationId; // 列 location_id，库位主键，不是编码
    private Integer quantity; // 列 quantity，这一行要出的数量
}
