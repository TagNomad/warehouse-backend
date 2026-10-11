package com.eurasia.pro_skill.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// 库存缓存。一个格子一个键 inv:货品主键:库位主键，值是数量的字符串。数量以 inventory 表为准。
@Component // 查询、入库、出库三处共用
@RequiredArgsConstructor
public class InventoryCache {

    private final LessonRedis redis;

    public static String key(Long skuId, Long locationId) {
        return "inv:" + skuId + ":" + locationId; // 例如 inv:1:1
    }

    public Integer get(Long skuId, Long locationId) {
        String cached = redis.get(key(skuId, locationId)); // GET inv:1:1。没有这个键是 null
        return cached == null ? null : Integer.valueOf(cached);
    }

    public void put(Long skuId, Long locationId, int quantity) {
        redis.set(key(skuId, locationId), Integer.toString(quantity)); // SET inv:1:1 10
    }

    // 事务提交之后再删。提交前删掉，别的请求会把还没提交的旧数重新读进缓存
    public void evictAfterCommit(Long skuId, Long locationId) {
        String key = key(skuId, locationId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() { // 回滚时不会执行，缓存里本来就是对的
                redis.delete(key); // DEL inv:1:1。下一次查询从库里读新数
            }
        });
    }
}
