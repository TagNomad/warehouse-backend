package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.application.AttachmentService;
import com.eurasia.pro_skill.support.TestAuthHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("入库附件")
class AttachmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String token;
    private String orderId;

    @BeforeEach
    void setUp() throws Exception {
        token = TestAuthHelper.obtainAdminToken(mockMvc);
        orderId = mockMvc.perform(post("/inbound-orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString().replaceAll(".*\"id\":(\\d+).*", "$1");
    }

    @Test
    @DisplayName("删掉本机文件后再下载就找不到")
    void localFileDisappears() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes());
        MvcResult uploaded = mockMvc.perform(multipart("/inbound-orders/" + orderId + "/attachments")
                        .file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String body = uploaded.getResponse().getContentAsString();
        String attachmentId = body.replaceAll(".*\"id\":(\\d+).*", "$1");
        String path = jdbcTemplate.queryForObject("SELECT file_path FROM inbound_attachment WHERE id=?", String.class, Long.valueOf(attachmentId));
        assertTrue(Path.of(path).isAbsolute());
        Files.deleteIfExists(Path.of(path));
        mockMvc.perform(get("/inbound-orders/" + orderId + "/attachments/" + attachmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("对象 key 不带本机目录")
    void objectKeyHasNoMachinePath() {
        String key = AttachmentService.objectKey("IN-1", "note.txt");
        assertEquals("inbound/IN-1/note.txt", key);
        assertFalse(key.startsWith("/"));
    }
}
