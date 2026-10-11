package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.application.OutboundService;
import com.eurasia.pro_skill.controller.request.OutboundLineQuantityRequest;
import com.eurasia.pro_skill.controller.request.OutboundLineRequest;
import com.eurasia.pro_skill.domain.OutboundOrder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 出库入口。和 UserController 一样，不写 SQL，把路径和字段交给 Service。
@Tag(name = "出库", description = "出库单草稿、加行、完成")
@RestController
@RequestMapping("/outbound-orders") // 下面方法的路径都从这里开头
@RequiredArgsConstructor
public class OutboundController {

    private final OutboundService outboundService;

    @Operation(summary = "新建出库草稿")
    @PostMapping // POST /outbound-orders，没有请求体
    public OutboundOrder create() {
        return outboundService.create(); // 不传字段。Service 自己建一张 DRAFT
    }

    @Operation(summary = "给草稿加一行")
    @PostMapping("/{id}/lines") // {id} 是出库单主键
    public OutboundOrder addLine(@PathVariable Long id, @Valid @RequestBody OutboundLineRequest request) {
        // id 来自路径。三个字段来自 JSON，原样传下去
        return outboundService.addLine(id, request.getSkuCode(), request.getLocationCode(), request.getQuantity());
    }

    @Operation(summary = "完成出库")
    @PostMapping("/{id}/complete") // 没有请求体。数量用行上已经记下的
    public OutboundOrder complete(@PathVariable Long id) {
        return outboundService.complete(id); // 只传主键。减库存在 Service
    }

    @Operation(summary = "修改一行的数量")
    @PatchMapping("/{id}/lines/{lineId}") // 已完成的单能不能改，看 order-state 开关
    public OutboundOrder updateLine(@PathVariable Long id,
                                    @PathVariable Long lineId,
                                    @Valid @RequestBody OutboundLineQuantityRequest request) {
        // 路径上是单主键和行主键，请求体里只有新数量
        return outboundService.updateLine(id, lineId, request.getQuantity());
    }
}
