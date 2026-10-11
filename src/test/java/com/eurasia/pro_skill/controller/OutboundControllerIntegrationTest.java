package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.support.TestAuthHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("出库完成")
class OutboundControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = TestAuthHelper.obtainAdminToken(mockMvc);
        jdbcTemplate.update("INSERT INTO warehouse (code, name, type) VALUES ('WH-OUT', '测试仓', 'RAW_MATERIAL')");
        Long warehouseId = jdbcTemplate.queryForObject("SELECT id FROM warehouse WHERE code='WH-OUT'", Long.class);
        jdbcTemplate.update("INSERT INTO warehouse_zone (warehouse_id, code, name, type) VALUES (?, 'Z-OUT', '区', 'STORAGE')", warehouseId);
        Long zoneId = jdbcTemplate.queryForObject("SELECT id FROM warehouse_zone WHERE code='Z-OUT'", Long.class);
        jdbcTemplate.update("INSERT INTO warehouse_location (zone_id, code, name) VALUES (?, 'A-01-01', '位')", zoneId);
        mockMvc.perform(post("/skus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SKU-OUT-001\",\"name\":\"轴承\",\"unit\":\"个\"}"))
                .andExpect(status().isOk());
        String inboundId = createId(post("/inbound-orders").header("Authorization", "Bearer " + token));
        mockMvc.perform(post("/inbound-orders/" + inboundId + "/lines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuCode\":\"SKU-OUT-001\",\"locationCode\":\"A-01-01\",\"quantity\":10}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/inbound-orders/" + inboundId + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("不看剩余时，出 15 会把库存写成负数")
    void completeAllowsNegativeQuantity() throws Exception {
        String id = outboundOf(15);
        mockMvc.perform(post("/outbound-orders/" + id + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        Integer quantity = quantityOf();
        assertEquals(-5, quantity);
    }

    @Test
    @DisplayName("已完成的出库单仍能改行上的数量")
    void completedOrderLineCanChange() throws Exception {
        String id = outboundOf(1);
        mockMvc.perform(post("/outbound-orders/" + id + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        Long lineId = jdbcTemplate.queryForObject(
                "SELECT id FROM outbound_order_line WHERE order_id=?", Long.class, Long.valueOf(id));

        mockMvc.perform(patch("/outbound-orders/" + id + "/lines/" + lineId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":5}"))
                .andExpect(status().isOk());

        Integer lineQuantity = jdbcTemplate.queryForObject(
                "SELECT quantity FROM outbound_order_line WHERE id=?", Integer.class, lineId);
        Integer stock = quantityOf();
        assertEquals(5, lineQuantity);
        assertEquals(9, stock);
    }

    private String outboundOf(int quantity) throws Exception {
        String id = createId(post("/outbound-orders").header("Authorization", "Bearer " + token));
        mockMvc.perform(post("/outbound-orders/" + id + "/lines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuCode\":\"SKU-OUT-001\",\"locationCode\":\"A-01-01\",\"quantity\":" + quantity + "}"))
                .andExpect(status().isOk());
        return id;
    }

    private Integer quantityOf() {
        return jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory WHERE sku_id=(SELECT id FROM sku WHERE code='SKU-OUT-001')",
                Integer.class);
    }

    private String createId(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        MvcResult created = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        return created.getResponse().getContentAsString().replaceAll(".*\"id\":(\\d+).*", "$1");
    }
}
