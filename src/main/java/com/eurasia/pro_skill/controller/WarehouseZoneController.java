package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.common.PageResponse;
import com.eurasia.pro_skill.controller.request.WarehouseZoneCreateRequest;
import com.eurasia.pro_skill.controller.request.WarehouseZoneUpdateRequest;
import com.eurasia.pro_skill.domain.WarehouseZone;
import com.eurasia.pro_skill.application.WarehouseZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "FR-01 库区管理", description = "库区 CRUD，隶属于仓库")
@RestController
@RequiredArgsConstructor
public class WarehouseZoneController {

    private final WarehouseZoneService warehouseZoneService;

    @Operation(summary = "在仓库下创建库区")
    @PostMapping("/warehouses/{warehouseId}/zones")
    public WarehouseZone create(
            @Parameter(description = "仓库 ID") @PathVariable Long warehouseId,
            @Valid @RequestBody WarehouseZoneCreateRequest request) {
        return warehouseZoneService.create(warehouseId, toCreateDomain(request));
    }

    @Operation(summary = "分页查询库区列表", description = "可按仓库 ID 筛选")
    @GetMapping("/warehouses/{warehouseId}/zones/list")
    public PageResponse<WarehouseZone> listByWarehouse(
            @Parameter(description = "仓库 ID") @PathVariable Long warehouseId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status
    ) {
        return warehouseZoneService.listPage(current, pageSize, warehouseId, code, name, type, status);
    }

    @Operation(summary = "全局分页查询库区")
    @GetMapping("/warehouse-zones/list")
    public PageResponse<WarehouseZone> listPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status
    ) {
        return warehouseZoneService.listPage(current, pageSize, warehouseId, code, name, type, status);
    }

    @Operation(summary = "查询库区详情")
    @GetMapping("/warehouse-zones/{id}")
    public WarehouseZone getById(@PathVariable Long id) {
        return warehouseZoneService.getById(id);
    }

    @Operation(summary = "更新库区")
    @PutMapping("/warehouse-zones/{id}")
    public WarehouseZone update(
            @PathVariable Long id,
            @Valid @RequestBody WarehouseZoneUpdateRequest request) {
        return warehouseZoneService.update(id, toUpdateDomain(request));
    }

    @Operation(summary = "删除库区", description = "若库区下存在库位则拒绝")
    @DeleteMapping("/warehouse-zones/{id}")
    public void delete(@PathVariable Long id) {
        warehouseZoneService.delete(id);
    }

    private WarehouseZone toCreateDomain(WarehouseZoneCreateRequest request) {
        return WarehouseZone.builder()
                .code(request.getCode())
                .name(request.getName())
                .type(request.getType())
                .build();
    }

    private WarehouseZone toUpdateDomain(WarehouseZoneUpdateRequest request) {
        return WarehouseZone.builder()
                .name(request.getName())
                .type(request.getType())
                .status(request.getStatus())
                .build();
    }
}
