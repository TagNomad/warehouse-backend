package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.application.MemoryObjectStore;
import com.eurasia.pro_skill.support.TestAuthHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.FileSystemUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 两个附件测试共用：建入库单、上传、下载、把本机目录整个删掉（等于换了一台机器或重建了容器）。
abstract class AttachmentPathSupport {

    static final Path ROOT = Path.of("target", "test-attachments").toAbsolutePath();
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    MockMvc mockMvc;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    MemoryObjectStore objectStore;

    String token;
    long orderId;

    @BeforeEach
    void prepare() throws Exception {
        objectStore.clear();
        wipeLocalDisk();
        token = TestAuthHelper.obtainAdminToken(mockMvc);
        String body = mockMvc.perform(post("/inbound-orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        orderId = JSON.readTree(body).get("id").asLong();
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM inbound_attachment");
        jdbcTemplate.update("DELETE FROM inbound_order");
        wipeLocalDisk();
    }

    JsonNode upload(String fileName, String content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", fileName, "text/plain",
                content.getBytes(StandardCharsets.UTF_8));
        String body = mockMvc.perform(multipart("/inbound-orders/" + orderId + "/attachments").file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JSON.readTree(body);
    }

    ResultActions download(long attachmentId) throws Exception {
        return mockMvc.perform(get("/inbound-orders/" + orderId + "/attachments/" + attachmentId)
                .header("Authorization", "Bearer " + token));
    }

    String downloadText(long attachmentId) throws Exception {
        return download(attachmentId).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    String filePathInTable(long attachmentId) {
        return jdbcTemplate.queryForObject("SELECT file_path FROM inbound_attachment WHERE id = ?", String.class, attachmentId);
    }

    void wipeLocalDisk() {
        FileSystemUtils.deleteRecursively(ROOT.toFile());
    }
}
