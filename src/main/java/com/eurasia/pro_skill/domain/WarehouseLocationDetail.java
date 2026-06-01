package com.eurasia.pro_skill.domain;

import lombok.Builder;
import lombok.Data;

/** 库位详情：含所属库区与仓库信息（FR-01 输出） */
@Data
@Builder
public class WarehouseLocationDetail {

    private WarehouseLocation location;
    private WarehouseZone zone;
    private Warehouse warehouse;
}
