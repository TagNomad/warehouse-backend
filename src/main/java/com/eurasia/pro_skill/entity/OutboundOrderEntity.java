package com.eurasia.pro_skill.entity;

import lombok.Data;

import java.time.LocalDateTime;

// 对应 outbound_order 表的一行。
@Data
public class OutboundOrderEntity {

    private Long id; // 列 id，插入后由数据库写入
    private String orderNo; // 列 order_no
    private String status; // 列 status
    private LocalDateTime createdAt; // 列 created_at
}
