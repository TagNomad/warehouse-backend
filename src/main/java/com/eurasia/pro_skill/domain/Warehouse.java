package com.eurasia.pro_skill.domain;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class Warehouse {

    // 仓库状态常量
    public static final String STATUS_ENABLED = "ENABLED";
    public static final String STATUS_DISABLED = "DISABLED";

    // 仓库类型常量
    public static final String TYPE_RAW_MATERIAL = "RAW_MATERIAL";
    public static final String TYPE_FINISHED_GOODS = "FINISHED_GOODS";
    public static final String TYPE_TRANSFER = "TRANSFER";

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

    /**
     * 业务方法：校验仓库是否可用
     */
    public boolean isAvailable() {
        return STATUS_ENABLED.equals(this.status);
    }

    /**
     * 业务方法：校验仓库类型
     */
    public boolean isType(String targetType) {
        return this.type != null && this.type.equals(targetType);
    }
}
