package com.eurasia.pro_skill.entity;

import lombok.Data;

import java.time.LocalDateTime;

// 对应 stock_flow 的一行。
@Data
public class StockFlowEntity {

    private Long id; // 自增主键，插入时不传
    private String bizNo; // 入库单号，例如 IN-1
    private Long skuId; // 货品主键
    private Long locationId; // 库位主键
    private Integer quantity; // 这一行入库的数量
    private LocalDateTime createdAt; // 写入时间
}
