package com.eurasia.pro_skill.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.lesson.inbound-complete-once=off")
@DisplayName("作业一 off 保留重复入库缺陷")
class InboundDefectPathTest extends InboundRaceSupport {
    @Test
    void sameOrderAddsTwiceWhenOff() throws Exception {
        prepareStock(QUANTITY); // 先有库存，避免首次插入撞唯一键干扰复现。
        var order = orderOf(QUANTITY);
        assertEquals(List.of("COMPLETED", "COMPLETED"), completeTogether(order.getId()));
        assertEquals(QUANTITY * 3, quantity()); // 19 + 19 + 19 = 57，正确结果应是38。
        assertEquals(2, flowCount(order.getOrderNo()));
        assertEquals("COMPLETED", status(order.getId()));
    }
}
