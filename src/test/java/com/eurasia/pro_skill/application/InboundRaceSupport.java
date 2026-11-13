package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.domain.InboundOrder;
import com.eurasia.pro_skill.entity.InboundOrderEntity;
import com.eurasia.pro_skill.mapper.InboundOrderMapper;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.mockingDetails;

/** 作业一：真实事务和数据库参与并发；只在 Mapper 读返回后加测试屏障，不模拟库存写入。 */
abstract class InboundRaceSupport {
    protected static final String SKU = "SKU-2119";
    protected static final String LOCATION = "A1-2119";
    protected static final int QUANTITY = 19;

    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected InboundService service;
    @MockitoSpyBean protected InboundOrderMapper orderMapper;

    protected void prepareStock(int initial) {
        cleanUp();
        jdbc.update("INSERT INTO warehouse(code,name,type) VALUES ('WH-A1','作业一测试仓','RAW_MATERIAL')");
        Long warehouse = jdbc.queryForObject("SELECT id FROM warehouse WHERE code='WH-A1'", Long.class);
        jdbc.update("INSERT INTO warehouse_zone(warehouse_id,code,name,type) VALUES (?,'Z-A1','测试区','STORAGE')", warehouse);
        Long zone = jdbc.queryForObject("SELECT id FROM warehouse_zone WHERE code='Z-A1'", Long.class);
        jdbc.update("INSERT INTO warehouse_location(zone_id,code,name) VALUES (?,?,'作业一库位')", zone, LOCATION);
        jdbc.update("INSERT INTO sku(code,name,unit) VALUES (?,'作业一货品','个')", SKU);
        if (initial > 0) {
            jdbc.update("INSERT INTO inventory(sku_id,location_id,quantity,version) VALUES (?,?,?,0)", skuId(), locationId(), initial);
        }
    }

    protected InboundOrder orderOf(int quantity) {
        InboundOrder order = service.create();
        return service.addLine(order.getId(), SKU, LOCATION, quantity);
    }

    protected List<String> completeTogether(Long orderId) throws Exception {
        CyclicBarrier bothReadDraft = new CyclicBarrier(2);
        // MyBatis Mapper 是 JDK 动态代理；保留 Spy 的原始委托 Answer，不能对接口抽象方法 callRealMethod。
        var originalAnswer = mockingDetails(orderMapper).getMockCreationSettings().getDefaultAnswer();
        // 两次真实 SELECT 都完成后才放行，确定性制造“两个请求都已读到 DRAFT”的时序，不用 sleep 碰运气。
        doAnswer(invocation -> {
            InboundOrderEntity order = (InboundOrderEntity) originalAnswer.answer(invocation);
            if (order != null && InboundOrder.DRAFT.equals(order.getStatus())) {
                bothReadDraft.await(10, TimeUnit.SECONDS);
            }
            return order;
        }).when(orderMapper).getById(orderId);
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var task = (java.util.concurrent.Callable<String>) () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("线程未同时启动");
                try {
                    return service.complete(orderId).getStatus();
                } catch (BusinessException ex) {
                    return ex.getCode();
                }
            };
            var first = pool.submit(task);
            var second = pool.submit(task);
            assertTrue(ready.await(10, TimeUnit.SECONDS), "两个线程必须都已就绪");
            start.countDown();
            return List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            pool.shutdownNow();
            reset(orderMapper);
        }
    }

    protected int quantity() {
        return jdbc.queryForObject("SELECT quantity FROM inventory WHERE sku_id=? AND location_id=?", Integer.class, skuId(), locationId());
    }

    protected int flowCount(String bizNo) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM stock_flow WHERE biz_no=?", Integer.class, bizNo);
    }

    protected String status(Long id) {
        return jdbc.queryForObject("SELECT status FROM inbound_order WHERE id=?", String.class, id);
    }

    protected Long skuId() { return jdbc.queryForObject("SELECT id FROM sku WHERE code=?", Long.class, SKU); }
    protected Long locationId() { return jdbc.queryForObject("SELECT id FROM warehouse_location WHERE code=?", Long.class, LOCATION); }

    @AfterEach
    void cleanUp() {
        reset(orderMapper);
        jdbc.update("DELETE FROM stock_flow WHERE sku_id IN (SELECT id FROM sku WHERE code=?)", SKU);
        jdbc.update("DELETE FROM inbound_order WHERE id IN (SELECT order_id FROM inbound_order_line WHERE sku_id IN (SELECT id FROM sku WHERE code=?))", SKU);
        jdbc.update("DELETE FROM inbound_order_line WHERE sku_id IN (SELECT id FROM sku WHERE code=?)", SKU);
        jdbc.update("DELETE FROM inventory WHERE sku_id IN (SELECT id FROM sku WHERE code=?)", SKU);
        jdbc.update("DELETE FROM sku WHERE code=?", SKU);
        jdbc.update("DELETE FROM warehouse_location WHERE code=?", LOCATION);
        jdbc.update("DELETE FROM warehouse_zone WHERE code='Z-A1'");
        jdbc.update("DELETE FROM warehouse WHERE code='WH-A1'");
    }
}
