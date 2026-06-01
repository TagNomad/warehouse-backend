package com.eurasia.pro_skill.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarehouseLocationCreateRequest {

    @NotBlank(message = "库位编码不能为空")
    @Size(max = 50, message = "库位编码长度不能超过50")
    private String code;

    @Size(max = 100, message = "库位名称长度不能超过100")
    private String name;

    @Size(max = 100, message = "规格长度不能超过100")
    private String spec;
}
