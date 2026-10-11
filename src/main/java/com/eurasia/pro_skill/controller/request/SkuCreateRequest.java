package com.eurasia.pro_skill.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// 创建货品时，页面传进来的三个字段。这一层还没有 id，也没有创建时间。
@Data // 生成 get/set，Controller 才能 request.getCode()
public class SkuCreateRequest {

    @NotBlank(message = "货品编码不能为空") // 缺省或空字符串时，请求在进 Service 之前就被拦住
    @Size(max = 50, message = "货品编码长度不能超过50") // 和表里 code VARCHAR(50) 对齐
    private String code; // 货品编码，例如 SKU-003

    @NotBlank(message = "货品名称不能为空")
    @Size(max = 100, message = "货品名称长度不能超过100") // 和表里 name VARCHAR(100) 对齐
    private String name; // 货品名称，例如十字螺丝

    @NotBlank(message = "单位不能为空")
    @Size(max = 20, message = "单位长度不能超过20") // 和表里 unit VARCHAR(20) 对齐
    private String unit; // 计量单位，例如个、盒
}
