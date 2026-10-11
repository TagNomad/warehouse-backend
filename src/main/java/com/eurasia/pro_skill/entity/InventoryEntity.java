package com.eurasia.pro_skill.entity;

import lombok.Data;

// 对应 inventory 表的一行：一种货在一个库位上的数量。
@Data
public class InventoryEntity {

    private Long id; // 列 id
    private Long skuId; // 列 sku_id
    private Long locationId; // 列 location_id
    private Integer quantity; // 列 quantity
    private Integer version; // 列 version
}
