package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.support.TestAuthHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("FR-01 仓库模块集成测试")
class WarehouseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String token;

    @BeforeEach
    void login() throws Exception {
        token = TestAuthHelper.obtainAdminToken(mockMvc);
    }

    @Test
    @DisplayName("完整主数据链路：仓库 → 库区 → 库位 → 树形视图")
    void fullWarehouseFlow() throws Exception {
        String warehouseJson = """
                {"code":"WH-IT-001","name":"集成测试仓","type":"RAW_MATERIAL","address":"测试路1号","capacity":500}
                """;

        String warehouseResponse = mockMvc.perform(post("/warehouses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(warehouseJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("WH-IT-001"))
                .andExpect(jsonPath("$.status").value("ENABLED"))
                .andReturn().getResponse().getContentAsString();

        Long warehouseId = extractId(warehouseResponse);

        String zoneJson = """
                {"code":"Z-IT-01","name":"集成测试区","type":"STORAGE"}
                """;

        String zoneResponse = mockMvc.perform(post("/warehouses/{id}/zones", warehouseId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(zoneJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("Z-IT-01"))
                .andReturn().getResponse().getContentAsString();

        Long zoneId = extractId(zoneResponse);

        String locationJson = """
                {"code":"IT-A-01","name":"集成库位","spec":"1m×1m"}
                """;

        mockMvc.perform(post("/warehouse-zones/{id}/locations", zoneId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("IT-A-01"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        mockMvc.perform(get("/warehouses/{id}/tree", warehouseId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warehouse.code").value("WH-IT-001"))
                .andExpect(jsonPath("$.zones", hasSize(1)))
                .andExpect(jsonPath("$.zones[0].locations", hasSize(1)));
    }

    @Test
    @DisplayName("创建仓库失败：编码重复")
    void createDuplicateCode() throws Exception {
        String body = """
                {"code":"WH-DUP","name":"重复仓","type":"RAW_MATERIAL","address":"x","capacity":100}
                """;

        mockMvc.perform(post("/warehouses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/warehouses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("仓库编码已存在")));
    }

    @Test
    @DisplayName("未登录访问仓库接口返回 401")
    void unauthorizedAccess() throws Exception {
        mockMvc.perform(get("/warehouses/list"))
                .andExpect(status().isUnauthorized());
    }

    private Long extractId(String json) {
        int idx = json.indexOf("\"id\":");
        if (idx < 0) {
            throw new IllegalStateException("响应中未找到 id 字段");
        }
        int start = idx + 5;
        int end = json.indexOf(',', start);
        if (end < 0) {
            end = json.indexOf('}', start);
        }
        return Long.parseLong(json, start, end, 10);
    }
}
