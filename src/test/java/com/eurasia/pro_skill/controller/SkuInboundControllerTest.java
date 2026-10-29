package com.eurasia.pro_skill.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("货品和入库：编码查重、按库位记数量")
class SkuInboundControllerTest extends SkuInboundPathSupport {

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
    @DisplayName("两个格子各入 10 和 8：inventory 两行")
    void twoLocationsTwoRows() throws Exception {
        createSku("SKU-T2").andExpect(status().isOk());

        Long first = inbound("SKU-T2", "T2-01", 10);
        Long second = inbound("SKU-T2", "T2-02", 8);

        assertEquals(10, inventory("SKU-T2", "T2-01"));
        assertEquals(8, inventory("SKU-T2", "T2-02"));
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
