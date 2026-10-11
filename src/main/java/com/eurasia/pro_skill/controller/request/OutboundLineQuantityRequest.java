package com.eurasia.pro_skill.controller.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

// 修改一行数量时，页面只传新的数量。
@Data
public class OutboundLineQuantityRequest {

    @NotNull(message = "数量不能为空")
    @Positive(message = "数量要大于 0")
    private Integer quantity; // 要把这一行改成的数量
}
