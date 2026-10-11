package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

// 一个格子一把锁，键是 lock:inv:货品主键:库位主键，值是这一次加锁的标记。
@Component
@RequiredArgsConstructor
public class StockLock {

    private static final Logger log = LoggerFactory.getLogger(StockLock.class);
    static final long TTL_MS = 30_000; // 进程挂了没放锁，30 秒后 Redis 自己删掉
    static final long WAIT_MS = 10_000; // 最多等 10 秒，还拿不到就返回 STOCK_LOCKED
    private static final long RETRY_MS = 50; // 每隔 50 毫秒再试一次

    private final LessonRedis redis;

    public static String key(Long skuId, Long locationId) {
        return "lock:inv:" + skuId + ":" + locationId; // 例如 lock:inv:1:1
    }

    // 拿到锁返回标记，释放时要带回来。标记是 单号:随机串，日志里能看出是哪一张单拿的
    public String acquire(String key, String orderNo) {
        String owner = orderNo + ":" + UUID.randomUUID();
        long deadline = System.currentTimeMillis() + WAIT_MS;
        while (!redis.setIfAbsent(key, owner, TTL_MS)) { // SET key owner NX PX 30000。别人拿着就是 false
            if (System.currentTimeMillis() > deadline) {
                throw new BusinessException("STOCK_LOCKED", "STOCK_LOCKED：这个格子正在出库，请稍后再试");
            }
            sleep();
        }
        log.info("{} 拿到锁 {}", orderNo, key);
        return owner;
    }

    public void release(String key, String owner) {
        redis.deleteIfValue(key, owner); // 值还是自己的标记才删。超时后被别人拿走的锁不会被误删
        log.info("{} 放开锁 {}", owner.substring(0, owner.indexOf(':')), key);
    }

    private void sleep() {
        try {
            Thread.sleep(RETRY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("STOCK_LOCKED", "STOCK_LOCKED：等锁时被中断");
        }
    }
}
