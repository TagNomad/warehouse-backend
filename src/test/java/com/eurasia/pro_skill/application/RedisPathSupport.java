package com.eurasia.pro_skill.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

// 第三部分两条路径的测试共用。数据真正提交，Redis 换成内存实现，每个测试前后都清空。
abstract class RedisPathSupport extends OutboundRaceSupport {

    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected OutboundService outboundService;
    @Autowired
    protected InboundService inboundService;
    @Autowired
    protected InventoryQueryService queryService;
    @Autowired
    protected InboundNoticeConsumer consumer;
    @Autowired
    protected MemoryLessonRedis redis;

    protected int cachedQuery() {
        return queryService.quantity(skuIdOf(), locationIdOf());
    }

    protected int lineCount(Long orderId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbound_order_line WHERE order_id=?", Integer.class, orderId);
    }

    protected Integer noticeCount(String orderNo) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inbound_notice WHERE biz_no=?", Integer.class, orderNo);
    }

    protected void publishDirectly(String orderNo) {
        redis.addStream(InboundNoticeConsumer.STREAM, Map.of(
                "orderNo", orderNo,
                "skuId", skuIdOf().toString(),
                "locationId", locationIdOf().toString(),
                "quantity", "4"));
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
