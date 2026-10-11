package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.mapper.SkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 单独提交「把数量加到货品总数上」。和改单据状态不在同一个事务里。
@Service
@RequiredArgsConstructor
public class SkuOnHandWriter {

    private final SkuMapper skuMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW) // 这个方法自己提交，外层回滚也撤不掉
    public void add(Long skuId, int quantity) {
        skuMapper.addOnHand(skuId, quantity); // on_hand = on_hand + quantity
    }
}
