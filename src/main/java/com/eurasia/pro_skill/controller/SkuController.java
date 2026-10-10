package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.application.SkuService;
import com.eurasia.pro_skill.controller.request.SkuCreateRequest;
import com.eurasia.pro_skill.domain.Sku;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "货品", description = "货品主数据。编码目前可以重复，这是课堂要学生修的缺陷")
@RestController
@RequestMapping("/skus")
@RequiredArgsConstructor
public class SkuController {

    private final SkuService skuService;

    @Operation(summary = "创建货品", description = "只记编码、名称、单位。当前不拒绝重复编码")
    @PostMapping
    public Sku create(@Valid @RequestBody SkuCreateRequest request) {
        Sku input = Sku.builder()
                .code(request.getCode())
                .name(request.getName())
                .unit(request.getUnit())
                .build();
        return skuService.create(input);
    }

    @Operation(summary = "查询全部货品")
    @GetMapping
    public List<Sku> list() {
        return skuService.list();
    }
}
