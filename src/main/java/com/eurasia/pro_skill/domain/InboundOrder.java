package com.eurasia.pro_skill.domain;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 一张入库单。完成前状态是 DRAFT。
@Data
public class InboundOrder {

    public static final String DRAFT = "DRAFT"; // 草稿，数量还没作为完成结果定下来
    public static final String COMPLETED = "COMPLETED"; // 已完成

    private Long id; // 主键，插入后由数据库生成
    private String orderNo; // 单号，插入后写成 IN-主键
    private String status; // DRAFT 或 COMPLETED
    private LocalDateTime createdAt; // 创建时间
    private List<InboundOrderLine> lines = new ArrayList<>(); // 这张单上的每一行
}
