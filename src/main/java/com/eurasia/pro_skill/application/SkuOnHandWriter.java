package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.mapper.SkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 单独提交「把数量加到货品总数上」。和改单据状态不在同一个事务里。
@Service // 必须是另一个 Bean。写在 InboundService 自己里面再调用，REQUIRES_NEW 不会生效
@RequiredArgsConstructor
public class SkuOnHandWriter {

    private final SkuMapper skuMapper; // SQL 在 SkuMapper.addOnHand

    @Transactional(propagation = Propagation.REQUIRES_NEW) // 这个方法一结束就提交，外层抛错也撤不掉
    public void add(Long skuId, int quantity) { // skuId 是货品主键。没有库位参数
        skuMapper.addOnHand(skuId, quantity); // on_hand = on_hand + quantity
    }
}
