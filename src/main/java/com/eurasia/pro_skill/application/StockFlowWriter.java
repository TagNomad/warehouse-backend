package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.entity.StockFlowEntity;
import com.eurasia.pro_skill.mapper.StockFlowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 用自己的事务写流水。flow-after-commit 是 off 时 InboundService 调这里。
// 外层入库后面回滚时，这一行已经提交，撤不掉。
@Service // 必须是另一个 Bean。同一个类里自己调自己，REQUIRES_NEW 不会生效
@RequiredArgsConstructor
public class StockFlowWriter {

    private final StockFlowMapper stockFlowMapper; // 写 stock_flow

    @Transactional(propagation = Propagation.REQUIRES_NEW) // 暂停外层事务，另开一个。方法结束就提交
    public void write(String bizNo, Long skuId, Long locationId, int quantity) { // 四个值来自入库行
        StockFlowEntity row = new StockFlowEntity(); // 准备 stock_flow 的一行
        row.setBizNo(bizNo); // 写进 biz_no：入库单号，例如 IN-3
        row.setSkuId(skuId); // 写进 sku_id
        row.setLocationId(locationId); // 写进 location_id
        row.setQuantity(quantity); // 写进 quantity：这一行入库的数量
        row.setCreatedAt(LocalDateTime.now()); // 写进 created_at
        stockFlowMapper.insert(row); // 写下 1 行。方法返回时这一行已经提交
    }
}
