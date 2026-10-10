package com.eurasia.pro_skill.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SkuCreateRequest {

    @NotBlank(message = "货品编码不能为空")
    @Size(max = 50, message = "货品编码长度不能超过50")
    private String code;

    @NotBlank(message = "货品名称不能为空")
    @Size(max = 100, message = "货品名称长度不能超过100")
    private String name;

    @NotBlank(message = "单位不能为空")
    @Size(max = 20, message = "单位长度不能超过20")
    private String unit;
}
