package com.eurasia.pro_skill.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class Sku {

    private Long id;
    private String code;
    private String name;
    private String unit;
    private LocalDateTime createdAt;
}
