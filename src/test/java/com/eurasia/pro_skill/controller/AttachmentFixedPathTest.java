package com.eurasia.pro_skill.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 改良版那一侧。两个分支上都跑这一份，开关在这里写死。
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.lesson.file-store=minio",
        "app.lesson.file-key=unique",
        "app.lesson.local-file-dir=target/test-attachments/data"
})
@DisplayName("改良版：附件放进对象存储、换成随机名")
class AttachmentFixedPathTest extends AttachmentPathSupport {

    @Test
    @DisplayName("表里是对象 key，本机目录没了也照样下载")
    void objectKeySurvivesAnotherMachine() throws Exception {
        long id = upload("note.txt", "第一张收货单").get("id").asLong();
        String key = filePathInTable(id);
        assertTrue(key.matches("inbound/IN-\\d+/[0-9a-f-]{36}\\.txt"), key);
        assertFalse(Path.of(key).isAbsolute());

        wipeLocalDisk();

        assertEquals("第一张收货单", downloadText(id));
    }

    @Test
    @DisplayName("同一张单传两个 note.txt，各自下载到各自的内容，下载名还是 note.txt")
    void sameNameKeepsBoth() throws Exception {
        long first = upload("note.txt", "第一张收货单").get("id").asLong();
        long second = upload("note.txt", "第二张收货单").get("id").asLong();

        assertNotEquals(filePathInTable(first), filePathInTable(second));
        assertEquals("第一张收货单", downloadText(first));
        assertEquals("第二张收货单", downloadText(second));
        download(first).andExpect(header().string("Content-Disposition", containsString("filename*=UTF-8''note.txt")));
    }

    @Test
    @DisplayName("文件名带 ../ 也留在 inbound/单号/ 下面")
    void dotDotStaysInside() throws Exception {
        long id = upload("../../escape.txt", "不该在这里").get("id").asLong();
        String key = filePathInTable(id);

        assertTrue(key.startsWith("inbound/IN-"), key);
        assertFalse(key.contains(".."), key);
    }

    @Test
    @DisplayName("附件不属于路径上的入库单时找不到")
    void attachmentOfAnotherOrder() throws Exception {
        long id = upload("note.txt", "第一张收货单").get("id").asLong();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/inbound-orders/" + (orderId + 1000) + "/attachments/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
