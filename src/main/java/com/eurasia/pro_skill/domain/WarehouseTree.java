package com.eurasia.pro_skill.domain;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 库位视图：仓库 → 库区 → 库位 树形结构（FR-01 输出） */
@Data
@Builder
public class WarehouseTree {

    private Warehouse warehouse;
    private List<WarehouseZoneNode> zones;

    @Data
    @Builder
    public static class WarehouseZoneNode {
        private WarehouseZone zone;
        private List<WarehouseLocation> locations;
    }
}
