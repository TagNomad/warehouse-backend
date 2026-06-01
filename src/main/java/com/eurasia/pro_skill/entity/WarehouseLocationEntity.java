package com.eurasia.pro_skill.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WarehouseLocationEntity {

    private Long id;
    private Long zoneId;
    private String code;
    private String name;
    private String spec;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
