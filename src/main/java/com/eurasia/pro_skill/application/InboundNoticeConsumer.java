package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.application.LessonRedis.LessonMessage;
import com.eurasia.pro_skill.entity.InboundNoticeEntity;
import com.eurasia.pro_skill.mapper.InboundNoticeMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 读 Stream 里的入库完成消息，每条写一行 inbound_notice。由 InboundNoticePoller 每秒调一次。
@Service
@RequiredArgsConstructor
public class InboundNoticeConsumer {

    private static final Logger log = LoggerFactory.getLogger(InboundNoticeConsumer.class);
    public static final String STREAM = "wms:inbound:completed"; // InboundService 往这里 XADD
    public static final String GROUP = "notice"; // 消费组。组记住投到了哪一条、哪些还没确认
    static final String CONSUMER = "app-1"; // 组里的消费者名字
    private static final long CONSUMED_TTL_MS = 7L * 24 * 60 * 60 * 1000; // 去重记录留 7 天

    private final LessonRedis redis;
    private final LessonSwitches switches;
    private final InboundNoticeMapper noticeMapper;
    private final Set<String> failedOnce = ConcurrentHashMap.newKeySet(); // 演示失败用：每条只失败一次

    public synchronized int poll() { // 返回这一次写了几行通知
        redis.createGroup(STREAM, GROUP); // XGROUP CREATE。组已存在就跳过
        int written = 0;
        // 先处理上次领过、还没 XACK 的。进程在确认前出错，这些消息会在这里再来一次
        for (LessonMessage message : redis.readGroup(STREAM, GROUP, CONSUMER, true)) {
            written += handle(message);
        }
        for (LessonMessage message : redis.readGroup(STREAM, GROUP, CONSUMER, false)) { // 再读新消息
            written += handle(message);
        }
        return written;
    }

    private int handle(LessonMessage message) {
        String msgId = message.id(); // Redis 生成的消息编号，重投时编号不变
        Map<String, String> fields = message.fields(); // InboundService 放进去的四个字段
        // consume-once 是 on：SET inbound:consumed:编号 1 NX。第二次来是 false，说明这条已经写过
        if (switches.consumeOnce() && !redis.setIfAbsent("inbound:consumed:" + msgId, "1", CONSUMED_TTL_MS)) {
            log.info("消息 {} 已经处理过，只确认不再写", msgId);
            redis.ack(STREAM, GROUP, msgId); // XACK，从待确认列表里拿掉
            return 0;
        }
        InboundNoticeEntity row = new InboundNoticeEntity(); // 准备 inbound_notice 的一行
        row.setMsgId(msgId); // 写进 msg_id
        row.setBizNo(fields.get("orderNo")); // 写进 biz_no：入库单号
        row.setSkuId(Long.valueOf(fields.get("skuId"))); // 写进 sku_id
        row.setLocationId(Long.valueOf(fields.get("locationId"))); // 写进 location_id
        row.setQuantity(Integer.valueOf(fields.get("quantity"))); // 写进 quantity
        row.setCreatedAt(LocalDateTime.now()); // 写进 created_at
        try {
            noticeMapper.insert(row); // 返回 1。这条语句自己提交
        } catch (RuntimeException e) {
            redis.delete("inbound:consumed:" + msgId); // 没写进去，去掉去重记录，重投时还能再写
            throw e;
        }
        log.info("消息 {} 写入通知 {}", msgId, row.getBizNo());
        if (switches.consumeFailBeforeAck() && failedOnce.add(msgId)) { // 演示用：写完了，还没确认就出错
            log.warn("消息 {} 写完通知后、XACK 之前失败，留在待确认列表", msgId);
            return 1; // 没有 XACK。下一次 poll 会从待确认列表里再拿到它
        }
        redis.ack(STREAM, GROUP, msgId); // XACK：这条处理完了，不会再投给这个组
        return 1;
    }
}
