package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.application.InventoryQueryService;
import com.eurasia.pro_skill.entity.InventoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// 库存查询。Controller 只把查询参数交给 Service。
@RestController
@RequestMapping("/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryQueryService inventoryQueryService;

    @GetMapping
    public Map<String, Object> quantity(@RequestParam Long skuId, @RequestParam Long locationId) {
        int quantity = inventoryQueryService.quantity(skuId, locationId); // 两个主键定位一行
        return Map.of("skuId", skuId, "locationId", locationId, "quantity", quantity);
    }

    @GetMapping("/list")
    public List<InventoryEntity> list(@RequestParam(required = false) Integer page,
                                      @RequestParam(required = false) Integer size) {
        return inventoryQueryService.list(page, size); // full 时页码用不上
    }
}
