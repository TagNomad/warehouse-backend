package com.eurasia.pro_skill.entity;

import lombok.Data;

import java.time.LocalDateTime;

// 对应 inbound_order 表的一行。
@Data
public class InboundOrderEntity {

    private Long id; // 列 id
    private String orderNo; // 列 order_no
    private String status; // 列 status
    private LocalDateTime createdAt; // 列 created_at
}
