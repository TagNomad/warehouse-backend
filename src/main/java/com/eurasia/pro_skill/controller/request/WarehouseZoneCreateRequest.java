package com.eurasia.pro_skill.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarehouseZoneCreateRequest {

    @NotBlank(message = "库区编码不能为空")
    @Size(max = 50, message = "库区编码长度不能超过50")
    private String code;

    @NotBlank(message = "库区名称不能为空")
    @Size(max = 100, message = "库区名称长度不能超过100")
    private String name;

    @NotBlank(message = "库区类型不能为空")
    @Size(max = 50, message = "库区类型长度不能超过50")
    private String type;
}
