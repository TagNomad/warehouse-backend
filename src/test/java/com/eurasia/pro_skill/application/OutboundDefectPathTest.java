package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.domain.InboundOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// 第二部分有漏洞的一侧。两个分支上都跑这一份，开关在这里写死。
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.lesson.optimistic-lock=off",
        "app.lesson.inbound-fail-before-status=true",
        "app.lesson.outbound-read-pause-ms=500",
        "app.lesson.stock-lock=after-check"
})
@DisplayName("第二部分：不带 version、流水另开事务")
class OutboundDefectPathTest extends OutboundRaceSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private OutboundService outboundService;
    @Autowired
    private InboundService inboundService;

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    @DisplayName("剩余 1，两张单各出 1 同时完成：两张都完成，库存是 0")
    void bothCompleteWithoutVersion() throws Exception {
        prepareStock(1);
        Long first = outboundOf(1);
        Long second = outboundOf(1);

        List<String> results = completeTogether(first, second);

        assertEquals(List.of("COMPLETED", "COMPLETED"), results);
        assertEquals(0, quantity());
        assertEquals("COMPLETED", outboundStatus(first));
        assertEquals("COMPLETED", outboundStatus(second));
    }


    @Override
    protected JdbcTemplate jdbc() {
        return jdbcTemplate;
    }

    @Override
    protected OutboundService outboundService() {
        return outboundService;
    }

    @Override
    protected InboundService inboundService() {
        return inboundService;
    }
}
