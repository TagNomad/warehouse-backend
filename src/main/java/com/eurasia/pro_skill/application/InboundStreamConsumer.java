package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.entity.StockFlowEntity;
import com.eurasia.pro_skill.mapper.StockFlowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

// 把入库完成消息写成库存流水。
@Service
@RequiredArgsConstructor
public class InboundStreamConsumer {

    public static final String STREAM = "wms:inbound:completed";

    private final LessonRedis redis;
    private final LessonSwitches switches;
    private final StockFlowMapper stockFlowMapper;

    public int consume() {
        int written = 0;
        for (String body : redis.readStream(STREAM)) { // 每一条是 orderNo|skuId|locationId|quantity
            String[] parts = body.split("\\|");
            String orderNo = parts[0];
            if (switches.consumeOnce() && !redis.setIfAbsent("inbound:consumed:" + orderNo, "1")) {
                continue; // 这个单号已经记过，跳过
            }
            StockFlowEntity row = new StockFlowEntity();
            row.setBizNo(orderNo);
            row.setSkuId(Long.valueOf(parts[1]));
            row.setLocationId(Long.valueOf(parts[2]));
            row.setQuantity(Integer.valueOf(parts[3]));
            row.setCreatedAt(LocalDateTime.now());
            stockFlowMapper.insert(row); // 写下一条流水
            written++;
        }
        return written;
    }
}
