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
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("入库完成")
class InboundControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = TestAuthHelper.obtainAdminToken(mockMvc);
        jdbcTemplate.update("INSERT INTO warehouse (code, name, type) VALUES ('WH-IN', '测试仓', 'RAW_MATERIAL')");
        Long warehouseId = jdbcTemplate.queryForObject("SELECT id FROM warehouse WHERE code='WH-IN'", Long.class);
        jdbcTemplate.update("INSERT INTO warehouse_zone (warehouse_id, code, name, type) VALUES (?, 'Z-IN', '区', 'STORAGE')", warehouseId);
        Long zoneId = jdbcTemplate.queryForObject("SELECT id FROM warehouse_zone WHERE code='Z-IN'", Long.class);
        jdbcTemplate.update("INSERT INTO warehouse_location (zone_id, code, name) VALUES (?, 'A-01-01', '位')", zoneId);
        jdbcTemplate.update("INSERT INTO warehouse_location (zone_id, code, name) VALUES (?, 'A-01-02', '位')", zoneId);
    }

    @Test
    @DisplayName("两个库位各记一行，单据变成已完成")
    void completeWritesInventoryAndCompletesOrder() throws Exception {
        mockMvc.perform(post("/skus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SKU-IN-001\",\"name\":\"轴承\",\"unit\":\"个\"}"))
                .andExpect(status().isOk());

        completeLine("A-01-01", 10);
        completeLine("A-01-02", 8);

        Integer first = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory WHERE sku_id = (SELECT id FROM sku WHERE code='SKU-IN-001') AND location_id = (SELECT id FROM warehouse_location WHERE code='A-01-01')",
                Integer.class);
        Integer second = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory WHERE sku_id = (SELECT id FROM sku WHERE code='SKU-IN-001') AND location_id = (SELECT id FROM warehouse_location WHERE code='A-01-02')",
                Integer.class);
        Integer onHand = jdbcTemplate.queryForObject(
                "SELECT on_hand FROM sku WHERE code='SKU-IN-001'", Integer.class);
        Integer completed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inbound_order WHERE status='COMPLETED'", Integer.class);
        assertEquals(10, first);
        assertEquals(8, second);
        assertEquals(0, onHand);
        assertEquals(2, completed);
    }

    private void completeLine(String locationCode, int quantity) throws Exception {
        MvcResult created = mockMvc.perform(post("/inbound-orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        String id = created.getResponse().getContentAsString().replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(post("/inbound-orders/" + id + "/lines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuCode\":\"SKU-IN-001\",\"locationCode\":\"" + locationCode + "\",\"quantity\":" + quantity + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/inbound-orders/" + id + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
