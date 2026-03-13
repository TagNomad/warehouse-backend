package com.eurasia.pro_skill.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WarehouseEntity {

    private Long id;
    private String code;
    private String name;
    private String type;
    private String address;
    private String manager;
    private String phone;
    private String status;
    private Integer capacity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
