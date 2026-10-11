package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.application.InboundStreamConsumer;
import com.eurasia.pro_skill.application.MemoryLessonRedis;
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
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Redis 缓存和消息")
class RedisLessonIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MemoryLessonRedis redis;
    @Autowired
    private InboundStreamConsumer consumer;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = TestAuthHelper.obtainAdminToken(mockMvc);
        jdbcTemplate.update("INSERT INTO warehouse (code, name, type) VALUES ('WH-R', '仓', 'RAW_MATERIAL')");
        Long warehouseId = jdbcTemplate.queryForObject("SELECT id FROM warehouse WHERE code='WH-R'", Long.class);
        jdbcTemplate.update("INSERT INTO warehouse_zone (warehouse_id, code, name, type) VALUES (?, 'Z-R', '区', 'STORAGE')", warehouseId);
        Long zoneId = jdbcTemplate.queryForObject("SELECT id FROM warehouse_zone WHERE code='Z-R'", Long.class);
        jdbcTemplate.update("INSERT INTO warehouse_location (zone_id, code, name) VALUES (?, 'R-01-01', '位')", zoneId);
        mockMvc.perform(post("/skus").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SKU-R\",\"name\":\"轴承\",\"unit\":\"个\"}"))
                .andExpect(status().isOk());
        String inboundId = idOf(post("/inbound-orders").header("Authorization", "Bearer " + token));
        mockMvc.perform(post("/inbound-orders/" + inboundId + "/lines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuCode\":\"SKU-R\",\"locationCode\":\"R-01-01\",\"quantity\":10}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/inbound-orders/" + inboundId + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("出库后缓存还是旧数量")
    void cacheKeepsOldQuantity() throws Exception {
        Long skuId = jdbcTemplate.queryForObject("SELECT id FROM sku WHERE code='SKU-R'", Long.class);
        Long locationId = jdbcTemplate.queryForObject("SELECT id FROM warehouse_location WHERE code='R-01-01'", Long.class);
        mockMvc.perform(get("/inventories").header("Authorization", "Bearer " + token)
                        .param("skuId", skuId.toString()).param("locationId", locationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(10));

        String outboundId = idOf(post("/outbound-orders").header("Authorization", "Bearer " + token));
        mockMvc.perform(post("/outbound-orders/" + outboundId + "/lines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuCode\":\"SKU-R\",\"locationCode\":\"R-01-01\",\"quantity\":3}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/outbound-orders/" + outboundId + "/complete")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/inventories").header("Authorization", "Bearer " + token)
                        .param("skuId", skuId.toString()).param("locationId", locationId.toString()))
                .andExpect(jsonPath("$.quantity").value(10));
        Integer inDb = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory WHERE sku_id=? AND location_id=?", Integer.class, skuId, locationId);
        assertEquals(7, inDb);
    }

    @Test
    @DisplayName("同一张出库单完成两次会扣两次")
    void completeTwiceDeductsTwice() throws Exception {
        String outboundId = idOf(post("/outbound-orders").header("Authorization", "Bearer " + token));
        mockMvc.perform(post("/outbound-orders/" + outboundId + "/lines")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuCode\":\"SKU-R\",\"locationCode\":\"R-01-01\",\"quantity\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/outbound-orders/" + outboundId + "/complete")
                        .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(post("/outbound-orders/" + outboundId + "/complete")
                        .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        Integer left = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventory WHERE sku_id=(SELECT id FROM sku WHERE code='SKU-R')", Integer.class);
        assertEquals(8, left);
    }

    @Test
    @DisplayName("同一条入库消息消费两次会写出两笔流水")
    void consumeTwiceWritesTwoFlows() {
        redis.addStream(InboundStreamConsumer.STREAM, "IN-9|1|1|4");
        consumer.consume();
        consumer.consume();
        Integer rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stock_flow WHERE biz_no='IN-9'", Integer.class);
        assertEquals(2, rows);
    }

    private String idOf(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        MvcResult created = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return created.getResponse().getContentAsString().replaceAll(".*\"id\":(\\d+).*", "$1");
    }
}
