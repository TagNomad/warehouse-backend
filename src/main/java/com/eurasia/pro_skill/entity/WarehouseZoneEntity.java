package com.eurasia.pro_skill.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WarehouseZoneEntity {

    private Long id;
    private Long warehouseId;
    private String code;
    private String name;
    private String type;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
