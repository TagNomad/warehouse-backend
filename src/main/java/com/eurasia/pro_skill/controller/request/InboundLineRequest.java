package com.eurasia.pro_skill.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

// 加一行时，页面 JSON 里的三个字段。Controller 再把它们拆开传给 Service。
@Data
public class InboundLineRequest {

    @NotBlank(message = "货品编码不能为空") // 空字符串进不了 Service
    private String skuCode; // 例如 SKU-001。这里是编码，不是表里的主键

    @NotBlank(message = "库位编码不能为空")
    private String locationCode; // 例如 A-01-01。这里是库位编码，不是库位主键

    @NotNull(message = "数量不能为空")
    @Positive(message = "数量要大于 0") // 0 和负数在进 Service 之前就被拒绝
    private Integer quantity; // 这一行要放进这个格子的数量
}
