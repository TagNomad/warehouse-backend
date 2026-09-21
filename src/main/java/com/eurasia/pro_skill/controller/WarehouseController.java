package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.common.PageResponse;
import com.eurasia.pro_skill.controller.request.WarehouseCreateRequest;
import com.eurasia.pro_skill.controller.request.WarehouseUpdateRequest;
import com.eurasia.pro_skill.domain.Warehouse;
import com.eurasia.pro_skill.domain.WarehouseTree;
import com.eurasia.pro_skill.application.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "FR-01 仓库管理", description = "仓库主数据 CRUD 与库位树形视图")
@RestController
@RequestMapping("/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @Operation(summary = "创建仓库", description = "新建物理仓库主数据，编码全局唯一")
    @PostMapping
    public Warehouse create(@Valid @RequestBody WarehouseCreateRequest request) {
        return warehouseService.create(toCreateDomain(request));
    }

    @Operation(summary = "更新仓库", description = "修改仓库信息；编码不可变更")
    @PutMapping("/{id}")
    public Warehouse update(
            @Parameter(description = "仓库 ID") @PathVariable Long id,
            @Valid @RequestBody WarehouseUpdateRequest request) {
        return warehouseService.update(id, toUpdateDomain(request));
    }

    @Operation(summary = "删除仓库", description = "物理删除；若存在库区则拒绝")
    @DeleteMapping("/{id}")
    public void delete(@Parameter(description = "仓库 ID") @PathVariable Long id) {
        warehouseService.delete(id);
    }

    @Operation(summary = "查询仓库详情")
    @GetMapping("/{id}")
    public Warehouse getById(@Parameter(description = "仓库 ID") @PathVariable Long id) {
        return warehouseService.getById(id);
    }

    @Operation(summary = "分页查询仓库列表", description = "对应需求书输出「仓库列表」")
    @GetMapping("/list")
    public PageResponse<Warehouse> listPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "编码模糊") @RequestParam(required = false) String code,
            @Parameter(description = "名称模糊") @RequestParam(required = false) String name,
            @Parameter(description = "类型") @RequestParam(required = false) String type,
            @Parameter(description = "状态") @RequestParam(required = false) String status
    ) {
        return warehouseService.listPage(current, pageSize, code, name, type, status);
    }

    @Operation(summary = "库位视图（树形）", description = "返回 仓库→库区→库位 树形结构，对应 FR-01 输出「库位视图」")
    @GetMapping("/{id}/tree")
    public WarehouseTree getTree(@Parameter(description = "仓库 ID") @PathVariable Long id) {
        return warehouseService.getTree(id);
    }

    private Warehouse toCreateDomain(WarehouseCreateRequest request) {
        return Warehouse.builder()
                .code(request.getCode())
                .name(request.getName())
                .type(request.getType())
                .address(request.getAddress())
                .manager(request.getManager())
                .phone(request.getPhone())
                .capacity(request.getCapacity())
                .build();
    }

    private Warehouse toUpdateDomain(WarehouseUpdateRequest request) {
        return Warehouse.builder()
                .name(request.getName())
                .type(request.getType())
                .address(request.getAddress())
                .manager(request.getManager())
                .phone(request.getPhone())
                .status(request.getStatus())
                .capacity(request.getCapacity())
                .build();
    }
}
