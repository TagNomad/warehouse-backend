package com.eurasia.pro_skill.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarehouseUpdateRequest {

    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 100, message = "仓库名称长度不能超过100")
    private String name;

    @Size(max = 50, message = "仓库类型长度不能超过50")
    private String type;

    @Size(max = 255, message = "仓库地址长度不能超过255")
    private String address;

    @Size(max = 50, message = "负责人长度不能超过50")
    private String manager;

    @Size(max = 20, message = "联系电话长度不能超过20")
    private String phone;

    private String status;

    private Integer capacity;
}
