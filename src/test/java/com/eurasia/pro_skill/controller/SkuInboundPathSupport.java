package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.support.TestAuthHelper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// 货品和入库测试用：登录、建库位、调接口、查表。
abstract class SkuInboundPathSupport {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    private String token;

    @BeforeEach
    void prepare() throws Exception {
        token = TestAuthHelper.obtainAdminToken(mockMvc);
        jdbcTemplate.update("INSERT INTO warehouse (code, name, type) VALUES ('WH-T2', '测试仓', 'RAW_MATERIAL')");
        Long warehouseId = jdbcTemplate.queryForObject("SELECT id FROM warehouse WHERE code='WH-T2'", Long.class);
        jdbcTemplate.update("INSERT INTO warehouse_zone (warehouse_id, code, name, type) VALUES (?, 'Z-T2', '区', 'STORAGE')", warehouseId);
        Long zoneId = jdbcTemplate.queryForObject("SELECT id FROM warehouse_zone WHERE code='Z-T2'", Long.class);
        jdbcTemplate.update("INSERT INTO warehouse_location (zone_id, code, name) VALUES (?, 'T2-01', '一号格')", zoneId);
        jdbcTemplate.update("INSERT INTO warehouse_location (zone_id, code, name) VALUES (?, 'T2-02', '二号格')", zoneId);
    }

    protected ResultActions createSku(String code) throws Exception {
        return mockMvc.perform(post("/skus")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"name\":\"轴承\",\"unit\":\"个\"}"));
    }

    // 新建草稿，加一行，完成。返回入库单主键。
    protected Long inbound(String skuCode, String locationCode, int quantity) throws Exception {
        String body = mockMvc.perform(post("/inbound-orders").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
        mockMvc.perform(post("/inbound-orders/" + id + "/lines")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"skuCode\":\"" + skuCode + "\",\"locationCode\":\"" + locationCode + "\",\"quantity\":" + quantity + "}"));
        mockMvc.perform(post("/inbound-orders/" + id + "/complete").header("Authorization", "Bearer " + token));
        return id;
    }

    protected int skuRows(String code) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sku WHERE code=?", Integer.class, code);
    }

    protected Integer inventory(String skuCode, String locationCode) {
        return jdbcTemplate.query(
                "SELECT i.quantity FROM inventory i JOIN sku s ON s.id=i.sku_id JOIN warehouse_location l ON l.id=i.location_id"
                        + " WHERE s.code=? AND l.code=?",
                rs -> rs.next() ? rs.getInt(1) : null, skuCode, locationCode);
    }

    protected int inventoryRows(String skuCode) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory i JOIN sku s ON s.id=i.sku_id WHERE s.code=?", Integer.class, skuCode);
    }

    protected String orderStatus(Long orderId) {
        return jdbcTemplate.queryForObject("SELECT status FROM inbound_order WHERE id=?", String.class, orderId);
    }
}
