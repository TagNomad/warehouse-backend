package com.eurasia.pro_skill.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 课堂开关。值写在 application.yaml 的 app.lesson 下，容器里由 Compose 的 APP_LESSON_* 盖过。
// 第二部分默认走有漏洞的那一侧，第二部分改良版把这两项改成修复后的值。
@Component // 注册成 Bean，Service 构造时注入
public class LessonSwitches {

    private final boolean skuCodeCheck; // on：创建货品前先按编码查一次，已有就拒绝
    private final boolean stockByLocation; // location：入库数量按货品 + 库位记进 inventory

    public LessonSwitches(
            @Value("${app.lesson.sku-code-check:off}") String skuCodeCheck, // 没写时按 off
            @Value("${app.lesson.inbound-stock:sku}") String inboundStock) { // sku 或 location
        this.skuCodeCheck = enabled(skuCodeCheck);
        this.stockByLocation = "location".equalsIgnoreCase(inboundStock);
    }

    // yaml 里不加引号的 on 会被读成字符串 "true"，所以 on、true、yes 都算打开。
    private static boolean enabled(String value) {
        return "on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }

    public boolean skuCodeCheck() {
        return skuCodeCheck;
    }

    public boolean stockByLocation() {
        return stockByLocation;
    }
}
