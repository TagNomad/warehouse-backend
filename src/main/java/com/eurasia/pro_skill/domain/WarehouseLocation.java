package com.eurasia.pro_skill.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WarehouseLocation {

    /** 可用 */
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    /** 禁用 */
    public static final String STATUS_DISABLED = "DISABLED";
    /** 占用（已有库存或锁定） */
    public static final String STATUS_OCCUPIED = "OCCUPIED";

    private Long id;
    private Long zoneId;
    private String code;
    private String name;
    private String spec;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isAvailable() {
        return STATUS_AVAILABLE.equals(status);
    }
}
