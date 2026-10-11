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

// 第三部分改良版那一侧。两个分支上都跑这一份，开关在这里写死。
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.lesson.outbound-check=on",
        "app.lesson.optimistic-lock=on",
        "app.lesson.flow-after-commit=on",
        "app.lesson.inbound-fail-before-status=true",
        "app.lesson.outbound-read-pause-ms=500",
        "app.lesson.stock-lock=after-check"
})
@DisplayName("第三部分改良版：带 version、流水同一次提交")
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

    @Test
    @DisplayName("入库在改状态前失败：单据仍是草稿，流水也没有留下")
    void flowRollsBackWithOrder() {
        prepareStock(0);
        InboundOrder order = inboundOf(4);

        BusinessException error = assertThrows(BusinessException.class, () -> inboundService.complete(order.getId()));

        assertEquals("INBOUND_MAIN_ROLLED_BACK", error.getCode());
        assertEquals("DRAFT", inboundStatus(order.getId()));
        assertEquals(0, inventoryRows());
        assertEquals(0, flowCount(order.getOrderNo()));
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
