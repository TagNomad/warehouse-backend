package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.domain.InboundOrder;
import com.eurasia.pro_skill.domain.OutboundOrder;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// 两条路径的测试共用的准备和清理。数据会真正提交，所以每个测试结束都要删干净。
abstract class OutboundRaceSupport {

    protected static final String SKU = "SKU-RACE";
    protected static final String LOCATION = "RACE-01-01";

    protected abstract JdbcTemplate jdbc();

    protected abstract OutboundService outboundService();

    protected abstract InboundService inboundService();

    protected void prepareStock(int quantity) {
        cleanUp();
        jdbc().update("INSERT INTO warehouse (code, name, type) VALUES ('WH-RACE', '并发仓', 'RAW_MATERIAL')");
        Long warehouseId = jdbc().queryForObject("SELECT id FROM warehouse WHERE code='WH-RACE'", Long.class);
        jdbc().update("INSERT INTO warehouse_zone (warehouse_id, code, name, type) VALUES (?, 'Z-RACE', '区', 'STORAGE')", warehouseId);
        Long zoneId = jdbc().queryForObject("SELECT id FROM warehouse_zone WHERE code='Z-RACE'", Long.class);
        jdbc().update("INSERT INTO warehouse_location (zone_id, code, name) VALUES (?, ?, '位')", zoneId, LOCATION);
        jdbc().update("INSERT INTO sku (code, name, unit) VALUES (?, '轴承', '个')", SKU);
        if (quantity > 0) {
            jdbc().update("INSERT INTO inventory (sku_id, location_id, quantity, version) VALUES (?, ?, ?, 0)",
                    skuId(), locationId(), quantity);
        }
    }

    protected Long outboundOf(int quantity) {
        OutboundOrder order = outboundService().create();
        outboundService().addLine(order.getId(), null, SKU, LOCATION, quantity);
        return order.getId();
    }

    protected InboundOrder inboundOf(int quantity) {
        InboundOrder order = inboundService().create();
        return inboundService().addLine(order.getId(), SKU, LOCATION, quantity);
    }

    // 两张单同时完成。返回每一次的结果：成功是 COMPLETED，失败是错误码。
    protected List<String> completeTogether(Long first, Long second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<String>> tasks = List.of(() -> completeOne(first), () -> completeOne(second));
            List<String> results = new ArrayList<>();
            for (Future<String> future : pool.invokeAll(tasks)) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private String completeOne(Long id) {
        try {
            return outboundService().complete(id).getStatus();
        } catch (BusinessException e) {
            return e.getCode();
        }
    }

    protected Integer quantity() {
        return jdbc().queryForObject(
                "SELECT quantity FROM inventory WHERE sku_id=? AND location_id=?", Integer.class, skuId(), locationId());
    }

    protected String outboundStatus(Long id) {
        return jdbc().queryForObject("SELECT status FROM outbound_order WHERE id=?", String.class, id);
    }

    protected String inboundStatus(Long id) {
        return jdbc().queryForObject("SELECT status FROM inbound_order WHERE id=?", String.class, id);
    }

    protected Integer flowCount(String orderNo) {
        return jdbc().queryForObject("SELECT COUNT(*) FROM stock_flow WHERE biz_no=?", Integer.class, orderNo);
    }

    protected Integer inventoryRows() {
        return jdbc().queryForObject(
                "SELECT COUNT(*) FROM inventory WHERE sku_id=? AND location_id=?", Integer.class, skuId(), locationId());
    }

    private Long skuId() {
        return jdbc().queryForObject("SELECT id FROM sku WHERE code=?", Long.class, SKU);
    }

    private Long locationId() {
        return jdbc().queryForObject("SELECT id FROM warehouse_location WHERE code=?", Long.class, LOCATION);
    }

    protected Long skuIdOf() {
        return skuId();
    }

    protected Long locationIdOf() {
        return locationId();
    }

    protected void cleanUp() {
        jdbc().update("DELETE FROM stock_flow WHERE sku_id IN (SELECT id FROM sku WHERE code=?)", SKU);
        jdbc().update("DELETE FROM inbound_notice WHERE sku_id IN (SELECT id FROM sku WHERE code=?)", SKU);
        jdbc().update("DELETE FROM outbound_order WHERE id IN (SELECT order_id FROM outbound_order_line "
                + "WHERE sku_id IN (SELECT id FROM sku WHERE code=?))", SKU);
        jdbc().update("DELETE FROM outbound_order_line WHERE sku_id IN (SELECT id FROM sku WHERE code=?)", SKU);
        jdbc().update("DELETE FROM inbound_order WHERE id IN (SELECT order_id FROM inbound_order_line "
                + "WHERE sku_id IN (SELECT id FROM sku WHERE code=?))", SKU);
        jdbc().update("DELETE FROM inbound_order_line WHERE sku_id IN (SELECT id FROM sku WHERE code=?)", SKU);
        jdbc().update("DELETE FROM inventory WHERE sku_id IN (SELECT id FROM sku WHERE code=?)", SKU);
        jdbc().update("DELETE FROM sku WHERE code=?", SKU);
        jdbc().update("DELETE FROM warehouse_location WHERE code=?", LOCATION);
        jdbc().update("DELETE FROM warehouse_zone WHERE code='Z-RACE'");
        jdbc().update("DELETE FROM warehouse WHERE code='WH-RACE'");
    }
}
