package com.eurasia.pro_skill.controller.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarehouseLocationUpdateRequest {

    @Size(max = 100, message = "库位名称长度不能超过100")
    private String name;

    @Size(max = 100, message = "规格长度不能超过100")
    private String spec;

    private String status;
}
