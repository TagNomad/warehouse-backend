package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.application.InventoryQueryService;
import com.eurasia.pro_skill.entity.InventoryEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// 库存查询。和 UserController 一样，不写 SQL，把查询参数交给 Service。
@Tag(name = "库存", description = "按货品和库位查数量")
@RestController
@RequestMapping("/inventories") // 下面方法的路径都从这里开头
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryQueryService inventoryQueryService;

    @Operation(summary = "查一个格子上的数量")
    @GetMapping // GET /inventories?skuId=1&locationId=1，没有请求体
    public Map<String, Object> quantity(@RequestParam Long skuId, @RequestParam Long locationId) {
        // skuId、locationId 来自问号后面的参数，原样传下去。返回的 int 是数量，可能来自缓存
        int quantity = inventoryQueryService.quantity(skuId, locationId);
        return Map.of("skuId", skuId, "locationId", locationId, "quantity", quantity); // 三个字段拼成 JSON
    }

    @Operation(summary = "库存列表")
    @GetMapping("/list")
    public List<InventoryEntity> list(@RequestParam(required = false) Integer page,
                                      @RequestParam(required = false) Integer size) {
        return inventoryQueryService.list(page, size); // full 时页码用不上
    }
}
