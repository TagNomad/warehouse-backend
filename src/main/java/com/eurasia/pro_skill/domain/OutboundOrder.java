package com.eurasia.pro_skill.domain;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 一张出库单。返回给页面的 JSON 用这个对象，不直接返回表实体。
@Data
public class OutboundOrder {

    public static final String DRAFT = "DRAFT"; // 草稿
    public static final String COMPLETED = "COMPLETED"; // 已完成

    private Long id; // 主键
    private String orderNo; // 单号，插入后写成 OUT-主键
    private String status; // DRAFT 或 COMPLETED
    private LocalDateTime createdAt; // 创建时间
    private List<OutboundOrderLine> lines = new ArrayList<>(); // 这张单上的每一行
}
