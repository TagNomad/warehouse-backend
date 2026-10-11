package com.eurasia.pro_skill.domain;

import lombok.Data;

import java.time.LocalDateTime;

// 业务里的一件货。没有表注解。查询结果和创建结果都用它返回给页面。
@Data // 生成 get/set
public class Sku {

    private Long id; // 数据库生成的主键；创建请求进来时还没有，插入成功后才有
    private String code; // 货品编码
    private String name; // 货品名称
    private String unit; // 计量单位
    private LocalDateTime createdAt; // 创建时间，由 Service 在插入前写入
    private Integer onHand; // 这张货品上的数量合计，不区分库位
}
