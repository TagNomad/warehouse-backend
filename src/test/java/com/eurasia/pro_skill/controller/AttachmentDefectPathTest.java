package com.eurasia.pro_skill.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 开关拨到演示问题的一侧。开关写死在这里，不看 application.yaml。
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.lesson.file-store=local",
        "app.lesson.file-key=filename",
        "app.lesson.local-file-dir=target/test-attachments/data"
})
@DisplayName("第四部分：附件写在本机磁盘、用原名存放")
class AttachmentDefectPathTest extends AttachmentPathSupport {

    @Test
    @DisplayName("表里是本机绝对路径，换一台机器就下载不到")
    void localPathIsGoneOnAnotherMachine() throws Exception {
        long id = upload("note.txt", "第一张收货单").get("id").asLong();
        assertTrue(Path.of(filePathInTable(id)).isAbsolute());
        assertEquals("第一张收货单", downloadText(id));

        wipeLocalDisk();

        download(id).andExpect(status().isBadRequest());
        assertTrue(Path.of(filePathInTable(id)).isAbsolute()); // 表里的路径还在，文件没了
    }

    @Test
    @DisplayName("同一张单传两个 note.txt，第一个被第二个盖掉")
    void sameNameOverwrites() throws Exception {
        long first = upload("note.txt", "第一张收货单").get("id").asLong();
        long second = upload("note.txt", "第二张收货单").get("id").asLong();

        assertEquals(filePathInTable(first), filePathInTable(second));
        assertEquals("第二张收货单", downloadText(first));
    }

    @Test
    @DisplayName("文件名带 ../ 时写到了附件目录外面")
    void dotDotEscapesDirectory() throws Exception {
        JsonNode row = upload("../../escape.txt", "不该在这里");
        Path written = Path.of(row.get("filePath").asText());

        assertTrue(Files.exists(written));
        assertFalse(written.startsWith(ROOT.resolve("data")));
    }
}
