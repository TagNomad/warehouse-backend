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

// 第四部分改良版那一侧。两个分支上都跑这一份，开关在这里写死。
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.lesson.outbound-check=on",
        "app.lesson.optimistic-lock=on",
        "app.lesson.outbound-read-pause-ms=500",
        "app.lesson.inbound-fail-before-status=true",
        "app.lesson.cache-evict=on",
        "app.lesson.stock-lock=sku-location",
        "app.lesson.submit-once=on",
        "app.lesson.publish-after-commit=on",
        "app.lesson.consume-once=on",
        "app.lesson.consume-fail-before-ack=true"
})
@DisplayName("第四部分改良版：提交后删缓存、读之前加锁、认重发、提交后发消息、按编号去重")
class RedisFixedPathTest extends RedisPathSupport {

    @BeforeEach
    void clearRedis() {
        redis.clear();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    @DisplayName("出库 3 之后，查询和库里都是 7")
    void cacheFollowsDatabase() {
        prepareStock(10);
        assertEquals(10, cachedQuery());

        outboundService.complete(outboundOf(3));

        assertEquals(7, quantity());
        assertEquals(7, cachedQuery());
    }

    @Test
    @DisplayName("剩余 2，两张单各出 1 同时完成：第二张等锁，两张都完成，剩 0")
    void lockBeforeReadBothComplete() throws Exception {
        prepareStock(2);
        Long first = outboundOf(1);
        Long second = outboundOf(1);

        List<String> results = completeTogether(first, second);

        assertEquals(List.of("COMPLETED", "COMPLETED"), results);
        assertEquals(0, quantity());
    }

    @Test
    @DisplayName("出库失败回滚后锁也放了，下一张单不用等")
    void lockReleasedAfterRollback() {
        prepareStock(1);
        Long tooMany = outboundOf(5);
        Long one = outboundOf(1);

        BusinessException error = assertThrows(BusinessException.class, () -> outboundService.complete(tooMany));
        long start = System.currentTimeMillis();
        outboundService.complete(one);

        assertEquals("INSUFFICIENT_STOCK", error.getCode());
        assertTrue(System.currentTimeMillis() - start < StockLock.WAIT_MS);
        assertEquals(0, quantity());
    }

    @Test
    @DisplayName("同一个 Idempotency-Key 发两次只有一行，换一个键再加一行")
    void sameKeyAddsOneLine() {
        prepareStock(10);
        OutboundOrder order = outboundService.create();

        outboundService.addLine(order.getId(), "line-001", SKU, LOCATION, 2);
        outboundService.addLine(order.getId(), "line-001", SKU, LOCATION, 2);
        assertEquals(1, lineCount(order.getId()));

        outboundService.addLine(order.getId(), "line-002", SKU, LOCATION, 2);
        assertEquals(2, lineCount(order.getId()));
    }

    @Test
    @DisplayName("入库回滚，消息没有发出，通知表里没有这张单")
    void rolledBackInboundNotNotified() {
        prepareStock(0);
        InboundOrder order = inboundOf(4);

        assertThrows(BusinessException.class, () -> inboundService.complete(order.getId()));
        consumer.poll();

        assertEquals("DRAFT", inboundStatus(order.getId()));
        assertEquals(0, noticeCount(order.getOrderNo()));
    }

    @Test
    @DisplayName("写完通知、确认之前失败，重投时认出编号，只有一行")
    void redeliveryWritesOnce() {
        prepareStock(0);
        publishDirectly("IN-R1");

        assertEquals(1, consumer.poll());
        assertEquals(0, consumer.poll());
        assertEquals(0, consumer.poll());

        assertEquals(1, noticeCount("IN-R1"));
    }
}
