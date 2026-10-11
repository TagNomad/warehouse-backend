package com.eurasia.pro_skill.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

// 给出库单加一行时，页面 JSON 里的三个字段。
@Data
public class OutboundLineRequest {

    @NotBlank(message = "货品编码不能为空")
    private String skuCode; // 例如 SKU-001，是编码，不是主键

    @NotBlank(message = "库位编码不能为空")
    private String locationCode; // 例如 A-01-01，是库位编码，不是主键

    @NotNull(message = "数量不能为空")
    @Positive(message = "数量要大于 0")
    private Integer quantity; // 这一行要从这个格子出的数量
}
