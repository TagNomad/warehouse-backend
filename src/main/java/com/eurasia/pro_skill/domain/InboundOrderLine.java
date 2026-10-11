package com.eurasia.pro_skill.domain;

import lombok.Data;

// 入库单上的一行：哪种货，进哪个格子，多少数量。
@Data
public class InboundOrderLine {

    private Long id; // 行主键
    private Long orderId; // 属于哪张入库单
    private Long skuId; // 货品主键
    private String skuCode; // 货品编码，返回给页面看
    private Long locationId; // 库位主键
    private String locationCode; // 库位编码，返回给页面看
    private Integer quantity; // 这一行的数量
}
