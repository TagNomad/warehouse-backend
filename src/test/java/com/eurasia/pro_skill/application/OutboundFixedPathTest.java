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
import static org.junit.jupiter.api.Assertions.assertTrue;

// 第二部分：乐观锁打开时两张单同时完成的结果。开关在这里写死，不读 yaml。
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.lesson.optimistic-lock=on",
        "app.lesson.outbound-read-pause-ms=500"
})
@DisplayName("第二部分：乐观锁打开")
class OutboundFixedPathTest extends OutboundRaceSupport {

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
    @DisplayName("剩余 1，两张单各出 1 同时完成：一张完成，另一张 STOCK_CONFLICT")
    void secondGetsConflict() throws Exception {
        prepareStock(1);
        Long first = outboundOf(1);
        Long second = outboundOf(1);

        List<String> results = completeTogether(first, second);

        assertTrue(results.contains("COMPLETED"), results.toString());
        assertTrue(results.contains("STOCK_CONFLICT"), results.toString());
        assertEquals(0, quantity());
        assertEquals(1, List.of(outboundStatus(first), outboundStatus(second)).stream()
                .filter("COMPLETED"::equals).count());
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
