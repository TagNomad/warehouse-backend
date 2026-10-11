package com.eurasia.pro_skill.entity;

import lombok.Data;

import java.time.LocalDateTime;

// 对应 inbound_notice 的一行。
@Data
public class InboundNoticeEntity {

    private Long id; // 自增主键，插入时不传
    private String msgId; // Stream 消息编号，例如 1760250000000-0
    private String bizNo; // 入库单号，例如 IN-3
    private Long skuId; // 货品主键
    private Long locationId; // 库位主键
    private Integer quantity; // 入库数量
    private LocalDateTime createdAt; // 写入时间
}
