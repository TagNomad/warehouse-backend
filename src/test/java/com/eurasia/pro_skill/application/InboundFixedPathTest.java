package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.mapper.StockFlowMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.lesson.inbound-complete-once=on")
@DisplayName("作业一 on 数据库保证同单只完成一次")
class InboundFixedPathTest extends InboundRaceSupport {
    @MockitoSpyBean private StockFlowMapper flowMapper;

    @AfterEach
    void resetFlowMapper() { reset(flowMapper); }

    @Test
    void sameOrderAddsOnceWhenOn() throws Exception {
        prepareStock(QUANTITY);
        var order = orderOf(QUANTITY);
        var results = completeTogether(order.getId());
        assertEquals(1, results.stream().filter("COMPLETED"::equals).count());
        assertEquals(1, results.stream().filter("INBOUND_NOT_DRAFT"::equals).count());
        assertEquals(QUANTITY * 2, quantity());
        assertEquals(1, flowCount(order.getOrderNo()));
        assertEquals("COMPLETED", status(order.getId()));
    }

    @Test
    void firstStockRowIsNotInsertedTwice() throws Exception {
        prepareStock(0); // 同一单首次入此库位，也必须在插入库存行前排除重复执行者。
        var order = orderOf(QUANTITY);
        var results = completeTogether(order.getId());
        assertEquals(1, results.stream().filter("COMPLETED"::equals).count());
        assertEquals(1, results.stream().filter("INBOUND_NOT_DRAFT"::equals).count());
        assertEquals(QUANTITY, quantity());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM inventory WHERE sku_id=? AND location_id=?", Integer.class, skuId(), locationId()));
        assertEquals(1, flowCount(order.getOrderNo()));
    }

    @Test
    void completedOrderCannotBeCompletedAgain() {
        prepareStock(QUANTITY);
        var order = orderOf(QUANTITY);
        service.complete(order.getId());
        var ex = assertThrows(BusinessException.class, () -> service.complete(order.getId()));
        assertEquals("INBOUND_NOT_DRAFT", ex.getCode());
        assertEquals(QUANTITY * 2, quantity());
        assertEquals(1, flowCount(order.getOrderNo()));
    }

    @Test
    void emptyOrderRemainsDraft() {
        prepareStock(QUANTITY);
        var order = service.create();
        try {
            var ex = assertThrows(BusinessException.class, () -> service.complete(order.getId()));
            assertEquals("INBOUND_LINE_REQUIRED", ex.getCode());
            assertEquals("DRAFT", status(order.getId()));
            assertEquals(QUANTITY, quantity());
            assertEquals(0, flowCount(order.getOrderNo()));
        } finally {
            jdbc.update("DELETE FROM inbound_order WHERE id=?", order.getId());
        }
    }

    @Test
    void failureAfterClaimRollsBackStatusInventoryAndFlow() {
        prepareStock(QUANTITY);
        var order = orderOf(QUANTITY);
        // 真实库存更新后，让流水边界失败，检验事务回滚，不能遗留“已完成但未入库”的单。
        doThrow(new IllegalStateException("模拟流水写入失败")).when(flowMapper).insert(any());
        assertThrows(IllegalStateException.class, () -> service.complete(order.getId()));
        assertEquals("DRAFT", status(order.getId()));
        assertEquals(QUANTITY, quantity());
        assertEquals(0, flowCount(order.getOrderNo()));
        reset(flowMapper);
        assertEquals("COMPLETED", service.complete(order.getId()).getStatus());
        assertEquals(QUANTITY * 2, quantity());
        assertEquals(1, flowCount(order.getOrderNo()));
    }
}
