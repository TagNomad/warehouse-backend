package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.common.PageResponse;
import com.eurasia.pro_skill.controller.request.WarehouseCreateRequest;
import com.eurasia.pro_skill.controller.request.WarehouseUpdateRequest;
import com.eurasia.pro_skill.domain.Warehouse;
import com.eurasia.pro_skill.application.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping
    public Warehouse create(@Valid @RequestBody WarehouseCreateRequest request) {
        return warehouseService.create(toCreateDomain(request));
    }

    @PutMapping("/{id}")
    public Warehouse update(@PathVariable Long id, @Valid @RequestBody WarehouseUpdateRequest request) {
        return warehouseService.update(id, toUpdateDomain(request));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        warehouseService.delete(id);
    }

    @GetMapping("/{id}")
    public Warehouse getById(@PathVariable Long id) {
        return warehouseService.getById(id);
    }

    @GetMapping("/list")
    public PageResponse<Warehouse> listPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status
    ) {
        return warehouseService.listPage(current, pageSize, code, name, type, status);
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
