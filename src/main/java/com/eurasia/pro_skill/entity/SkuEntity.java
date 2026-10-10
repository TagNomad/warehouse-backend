package com.eurasia.pro_skill.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SkuEntity {

    private Long id;
    private String code;
    private String name;
    private String unit;
    private LocalDateTime createdAt;
}
