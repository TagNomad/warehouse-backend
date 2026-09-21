package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.common.PageResponse;
import com.eurasia.pro_skill.controller.request.WarehouseLocationCreateRequest;
import com.eurasia.pro_skill.controller.request.WarehouseLocationUpdateRequest;
import com.eurasia.pro_skill.domain.WarehouseLocation;
import com.eurasia.pro_skill.domain.WarehouseLocationDetail;
import com.eurasia.pro_skill.application.WarehouseLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "FR-01 库位管理", description = "库位 CRUD 与详情，隶属于库区")
@RestController
@RequiredArgsConstructor
public class WarehouseLocationController {

    private final WarehouseLocationService warehouseLocationService;

    @Operation(summary = "在库区下创建库位")
    @PostMapping("/warehouse-zones/{zoneId}/locations")
    public WarehouseLocation create(
            @Parameter(description = "库区 ID") @PathVariable Long zoneId,
            @Valid @RequestBody WarehouseLocationCreateRequest request) {
        return warehouseLocationService.create(zoneId, toCreateDomain(request));
    }

    @Operation(summary = "分页查询库区下库位")
    @GetMapping("/warehouse-zones/{zoneId}/locations/list")
    public PageResponse<WarehouseLocation> listByZone(
            @PathVariable Long zoneId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status
    ) {
        return warehouseLocationService.listPage(current, pageSize, null, zoneId, code, name, status);
    }

    @Operation(summary = "全局分页查询库位", description = "可按仓库 ID、库区 ID 筛选")
    @GetMapping("/warehouse-locations/list")
    public PageResponse<WarehouseLocation> listPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status
    ) {
        return warehouseLocationService.listPage(current, pageSize, warehouseId, zoneId, code, name, status);
    }

    @Operation(summary = "查询库位详情", description = "含所属库区与仓库，对应 FR-01 输出「库位详情」")
    @GetMapping("/warehouse-locations/{id}")
    public WarehouseLocation getById(@PathVariable Long id) {
        return warehouseLocationService.getById(id);
    }

    @Operation(summary = "查询库位完整详情", description = "库位 + 库区 + 仓库 聚合信息")
    @GetMapping("/warehouse-locations/{id}/detail")
    public WarehouseLocationDetail getDetail(@PathVariable Long id) {
        return warehouseLocationService.getDetail(id);
    }

    @Operation(summary = "更新库位")
    @PutMapping("/warehouse-locations/{id}")
    public WarehouseLocation update(
            @PathVariable Long id,
            @Valid @RequestBody WarehouseLocationUpdateRequest request) {
        return warehouseLocationService.update(id, toUpdateDomain(request));
    }

    @Operation(summary = "删除库位", description = "占用状态库位不可删除")
    @DeleteMapping("/warehouse-locations/{id}")
    public void delete(@PathVariable Long id) {
        warehouseLocationService.delete(id);
    }

    private WarehouseLocation toCreateDomain(WarehouseLocationCreateRequest request) {
        return WarehouseLocation.builder()
                .code(request.getCode())
                .name(request.getName())
                .spec(request.getSpec())
                .build();
    }

    private WarehouseLocation toUpdateDomain(WarehouseLocationUpdateRequest request) {
        return WarehouseLocation.builder()
                .name(request.getName())
                .spec(request.getSpec())
                .status(request.getStatus())
                .build();
    }
}
