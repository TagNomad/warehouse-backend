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

// 货品的 HTTP 入口。只接收请求、交给 Service，不在这里写 SQL。
@Tag(name = "货品", description = "货品：编码、名称、单位") // Swagger 里这一组的标题
@RestController // 方法的返回值直接变成 JSON
@RequestMapping("/skus") // 下面两个方法的路径都从 /skus 开始
@RequiredArgsConstructor // 为 skuService 生成构造方法，完成注入
public class SkuController {

    private final SkuService skuService; // 真正创建和查询的业务

    @Operation(summary = "创建货品") // Swagger 上这个接口的名称
    @PostMapping // POST /skus
    public Sku create(@Valid @RequestBody SkuCreateRequest request) {
        // @RequestBody：把 JSON 装进 SkuCreateRequest
        // @Valid：触发编码、名称、单位上的非空和长度校验
        // 和 UserController 创建用户一样：不在这里组装对象，把字段交给 Service
        return skuService.create(request.getCode(), request.getName(), request.getUnit());
    }

    @Operation(summary = "查询全部货品")
    @GetMapping // GET /skus
    public List<Sku> list() {
        return skuService.list(); // 返回全部货品，包括启动时写入的两行
    }
}
