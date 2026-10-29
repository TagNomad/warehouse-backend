package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.domain.InboundOrder;
import com.eurasia.pro_skill.domain.OutboundOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 第三部分那一侧。两个分支上都跑这一份，开关在这里写死。
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.lesson.outbound-check=on",
        "app.lesson.optimistic-lock=on",
        "app.lesson.outbound-read-pause-ms=500",
        "app.lesson.inbound-fail-before-status=true",
        "app.lesson.cache-evict=off",
        "app.lesson.stock-lock=after-check",
        "app.lesson.submit-once=off",
        "app.lesson.publish-after-commit=off",
        "app.lesson.consume-once=off",
        "app.lesson.consume-fail-before-ack=true"
})
@DisplayName("第三部分：缓存不删、锁加在判断之后、不认重发、事务中发消息、重投再写")
class RedisDefectPathTest extends RedisPathSupport {

    @BeforeEach
    void clearRedis() {
        redis.clear();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    @DisplayName("出库 3 之后，库里是 7，查询还是 10")
    void cacheKeepsOldQuantity() {
        prepareStock(10);
        assertEquals(10, cachedQuery());

        outboundService.complete(outboundOf(3));

        assertEquals(7, quantity());
        assertEquals(10, cachedQuery());
    }

    @Test
    @DisplayName("剩余 2，两张单各出 1 同时完成：库存够，第二张却 STOCK_CONFLICT")
    void lockAfterCheckStillConflicts() throws Exception {
        prepareStock(2);
        Long first = outboundOf(1);
        Long second = outboundOf(1);

        List<String> results = completeTogether(first, second);

        assertTrue(results.contains("COMPLETED"), results.toString());
        assertTrue(results.contains("STOCK_CONFLICT"), results.toString());
        assertEquals(1, quantity());
    }

    @Test
    @DisplayName("同一个 Idempotency-Key 发两次，单上有两行")
    void sameKeyAddsTwoLines() {
        prepareStock(10);
        OutboundOrder order = outboundService.create();

        outboundService.addLine(order.getId(), "line-001", SKU, LOCATION, 2);
        outboundService.addLine(order.getId(), "line-001", SKU, LOCATION, 2);

        assertEquals(2, lineCount(order.getId()));
    }

    @Test
    @DisplayName("入库回滚了，消息已经发出，通知表里有这张草稿单")
    void rolledBackInboundStillNotified() {
        prepareStock(0);
        InboundOrder order = inboundOf(4);

        BusinessException error = assertThrows(BusinessException.class, () -> inboundService.complete(order.getId()));
        consumer.poll();

        assertEquals("INBOUND_MAIN_ROLLED_BACK", error.getCode());
        assertEquals("DRAFT", inboundStatus(order.getId()));
        assertEquals(1, noticeCount(order.getOrderNo()));
    }

    @Test
    @DisplayName("写完通知、确认之前失败，重投后同一条消息写了两行")
    void redeliveryWritesTwice() {
        prepareStock(0);
        publishDirectly("IN-R1");

        assertEquals(1, consumer.poll());
        assertEquals(1, consumer.poll());

        assertEquals(2, noticeCount("IN-R1"));
    }
}
