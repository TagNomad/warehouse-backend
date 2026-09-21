package com.eurasia.pro_skill.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WarehouseZone {

    public static final String STATUS_ENABLED = "ENABLED";
    public static final String STATUS_DISABLED = "DISABLED";

    /** 存储区 */
    public static final String TYPE_STORAGE = "STORAGE";
    /** 拣货区 */
    public static final String TYPE_PICKING = "PICKING";

    private Long id;
    private Long warehouseId;
    private String code;
    private String name;
    private String type;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isAvailable() {
        return STATUS_ENABLED.equals(status);
    }
}
