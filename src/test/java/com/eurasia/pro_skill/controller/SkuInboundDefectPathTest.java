package com.eurasia.pro_skill.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 第二部分有漏洞的一侧。两个分支上都跑这一份，开关在这里写死。
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "app.lesson.sku-code-check=off",
        "app.lesson.inbound-stock=sku"
})
@DisplayName("第二部分：编码不查重、数量不分库位")
class SkuInboundDefectPathTest extends SkuInboundPathSupport {

    @Test
    @DisplayName("同一个编码创建两次：两次都是 200，表里两行")
    void sameCodeTwice() throws Exception {
        createSku("SKU-T2").andExpect(status().isOk());
        createSku("SKU-T2").andExpect(status().isOk());

        assertEquals(2, skuRows("SKU-T2"));
    }

    @Test
    @DisplayName("两个格子各入 10 和 8：on_hand 是 18，inventory 没有行")
    void twoLocationsAddUp() throws Exception {
        createSku("SKU-T2").andExpect(status().isOk());

        Long first = inbound("SKU-T2", "T2-01", 10);
        Long second = inbound("SKU-T2", "T2-02", 8);

        assertEquals(18, onHand("SKU-T2"));
        assertEquals(0, inventoryRows("SKU-T2"));
        assertEquals("COMPLETED", orderStatus(first));
        assertEquals("COMPLETED", orderStatus(second));
    }
}
