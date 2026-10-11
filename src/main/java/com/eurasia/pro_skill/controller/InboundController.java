package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.application.InboundService;
import com.eurasia.pro_skill.controller.request.InboundLineRequest;
import com.eurasia.pro_skill.domain.InboundOrder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 入库入口。和 UserController 一样：不写 SQL，把路径和请求体里的字段交给 Service。
@Tag(name = "入库", description = "入库单草稿、加行、完成")
@RestController // 方法的返回值直接变成 JSON
@RequestMapping("/inbound-orders") // 下面三个方法的路径都从这里开头
@RequiredArgsConstructor // 为 inboundService 生成构造方法，Spring 用来注入
public class InboundController {

    private final InboundService inboundService; // 业务在 Service

    @Operation(summary = "新建入库草稿")
    @PostMapping // POST /inbound-orders，这个方法没有请求体
    public InboundOrder create() {
        return inboundService.create(); // 不传字段。Service 自己建一张 DRAFT
    }

    @Operation(summary = "给草稿加一行")
    @PostMapping("/{id}/lines") // {id} 是路径上的入库单主键，例如 /inbound-orders/1/lines
    public InboundOrder addLine(@PathVariable Long id, @Valid @RequestBody InboundLineRequest request) {
        // id 来自路径。skuCode、locationCode、quantity 来自 JSON，三个值原样传下去
        return inboundService.addLine(id, request.getSkuCode(), request.getLocationCode(), request.getQuantity());
    }

    @Operation(summary = "完成入库")
    @PostMapping("/{id}/complete") // POST /inbound-orders/1/complete，没有请求体
    public InboundOrder complete(@PathVariable Long id) {
        return inboundService.complete(id); // 只传主键。加库存和改状态都在 Service
    }
}
