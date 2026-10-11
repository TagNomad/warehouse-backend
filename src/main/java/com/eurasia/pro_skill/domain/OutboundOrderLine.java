package com.eurasia.pro_skill.domain;

import lombok.Data;

// 出库单上的一行。编码是查回来给页面看的，表里存的是主键。
@Data
public class OutboundOrderLine {

    private Long id; // 行主键
    private Long orderId; // 属于哪张出库单
    private Long skuId; // 货品主键
    private String skuCode; // 货品编码，例如 SKU-001
    private Long locationId; // 库位主键
    private String locationCode; // 库位编码，例如 A-01-01
    private Integer quantity; // 这一行出多少
}
