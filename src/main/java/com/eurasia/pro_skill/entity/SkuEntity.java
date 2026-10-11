package com.eurasia.pro_skill.entity;

import lombok.Data;

import java.time.LocalDateTime;

// 和 sku 表的一行对齐。只在 Service 和 Mapper 之间使用，不返回给页面。
@Data // 生成 get/set，MyBatis 用它们读写列
public class SkuEntity {

    private Long id; // 对应列 id，自增主键
    private String code; // 对应列 code
    private String name; // 对应列 name
    private String unit; // 对应列 unit
    private LocalDateTime createdAt; // 对应列 created_at，Java 用驼峰，表用下划线
    private Integer onHand; // 对应列 on_hand，这张货品上的数量合计
}
