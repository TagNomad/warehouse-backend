package com.eurasia.pro_skill.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 第二部分修复后的一侧。两个分支上都跑这一份，开关在这里写死。
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "app.lesson.sku-code-check=on",
        "app.lesson.inbound-stock=location"
})
@DisplayName("第二部分改良版：编码查重、按库位记账")
class SkuInboundFixedPathTest extends SkuInboundPathSupport {

    @Test
    @DisplayName("同一个编码创建两次：第二次 400，表里一行")
    void sameCodeRejected() throws Exception {
        createSku("SKU-T2").andExpect(status().isOk());
        createSku("SKU-T2")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("SKU_CODE_DUPLICATED")));

        assertEquals(1, skuRows("SKU-T2"));
    }

    @Test
    @DisplayName("两个格子各入 10 和 8：inventory 两行，on_hand 不动")
    void twoLocationsTwoRows() throws Exception {
        createSku("SKU-T2").andExpect(status().isOk());

        Long first = inbound("SKU-T2", "T2-01", 10);
        Long second = inbound("SKU-T2", "T2-02", 8);

        assertEquals(10, inventory("SKU-T2", "T2-01"));
        assertEquals(8, inventory("SKU-T2", "T2-02"));
        assertEquals(0, onHand("SKU-T2"));
        assertEquals("COMPLETED", orderStatus(first));
        assertEquals("COMPLETED", orderStatus(second));
    }

    @Test
    @DisplayName("同一个格子入两次：还是一行，数量相加，version 是 1")
    void sameLocationAddsToOneRow() throws Exception {
        createSku("SKU-T2").andExpect(status().isOk());

        inbound("SKU-T2", "T2-01", 10);
        inbound("SKU-T2", "T2-01", 5);

        assertEquals(1, inventoryRows("SKU-T2"));
        assertEquals(15, inventory("SKU-T2", "T2-01"));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT i.version FROM inventory i JOIN sku s ON s.id=i.sku_id WHERE s.code='SKU-T2'", Integer.class));
    }
}
