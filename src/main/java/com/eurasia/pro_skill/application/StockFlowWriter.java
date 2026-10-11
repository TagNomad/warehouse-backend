package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.entity.StockFlowEntity;
import com.eurasia.pro_skill.mapper.StockFlowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 用自己的事务写流水。外层入库回滚时，这一行已经提交，撤不掉。
@Service // 必须是另一个 Bean，REQUIRES_NEW 才会单独提交
@RequiredArgsConstructor
public class StockFlowWriter {

    private final StockFlowMapper stockFlowMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW) // 方法结束就提交
    public void write(String bizNo, Long skuId, Long locationId, int quantity) {
        StockFlowEntity row = new StockFlowEntity();
        row.setBizNo(bizNo); // 入库单号
        row.setSkuId(skuId);
        row.setLocationId(locationId);
        row.setQuantity(quantity);
        row.setCreatedAt(LocalDateTime.now());
        stockFlowMapper.insert(row); // 写下 1 行
    }
}
