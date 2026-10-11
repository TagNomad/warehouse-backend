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

// 入库单入口。和 UserController 一样，把字段交给 Service，不在这里写 SQL。
@Tag(name = "入库", description = "入库单草稿、加行、完成")
@RestController
@RequestMapping("/inbound-orders")
@RequiredArgsConstructor
public class InboundController {

    private final InboundService inboundService;

    @Operation(summary = "新建入库草稿")
    @PostMapping
    public InboundOrder create() {
        return inboundService.create(); // 没有请求体，新建一张 DRAFT
    }

    @Operation(summary = "给草稿加一行")
    @PostMapping("/{id}/lines")
    public InboundOrder addLine(@PathVariable Long id, @Valid @RequestBody InboundLineRequest request) {
        // 三个字段原样交给 Service
        return inboundService.addLine(id, request.getSkuCode(), request.getLocationCode(), request.getQuantity());
    }

    @Operation(summary = "完成入库")
    @PostMapping("/{id}/complete")
    public InboundOrder complete(@PathVariable Long id) {
        return inboundService.complete(id);
    }
}
