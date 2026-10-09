package com.eurasia.pro_skill.observability;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TraceIdFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("开关关闭时响应没有 X-Trace-Id")
    void traceIdOffOmitsHeader() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(TraceIdFilter.HEADER));
    }
}

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.lesson.trace-id=on")
@ExtendWith(OutputCaptureExtension.class)
class TraceIdOnTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("没有请求头时生成 X-Trace-Id，并写进日志")
    void generatesTraceId(CapturedOutput output) throws Exception {
        String traceId = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists(TraceIdFilter.HEADER))
                .andExpect(header().string(TraceIdFilter.HEADER, not(emptyString())))
                .andReturn()
                .getResponse()
                .getHeader(TraceIdFilter.HEADER);

        Assertions.assertNotNull(traceId);
        Assertions.assertTrue(output.getAll().contains(traceId), output.getAll());
    }

    @Test
    @DisplayName("沿用调用方传来的 X-Trace-Id")
    void echoesIncomingTraceId() throws Exception {
        mockMvc.perform(get("/actuator/health").header(TraceIdFilter.HEADER, "demo-trace-001"))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceIdFilter.HEADER, "demo-trace-001"));
    }
}
