package com.eurasia.pro_skill.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 课堂开关。值写在 application.yaml 的 app.lesson 下，容器里由 Compose 的 APP_LESSON_* 盖过。
// 每个开关默认是正确的写法；老师上课把一个拨到另一侧，演示不这么写会出什么错。
@Component // 注册成 Bean，Service 构造时注入
public class LessonSwitches {

    private final boolean optimisticLock; // on：更新库存时带上读到的 version
    private final boolean inboundFailBeforeStatus; // true：改状态前让主事务失败，用来看流水会不会留下来
    private final long outboundReadPauseMs; // 大于 0：出库读到数量后停这么多毫秒，让两次请求读到同一个旧数
    private final boolean cacheEvict; // on：出库提交后删掉这个格子的库存缓存
    private final boolean lockBeforeRead; // stock-lock 是 sku-location：先加锁再读数量
    private final boolean submitOnce; // on：同一个 Idempotency-Key 只加一行
    private final boolean publishAfterCommit; // on：入库提交之后才发消息
    private final boolean consumeOnce; // on：同一条消息只写一行通知
    private final boolean consumeFailBeforeAck; // true：写完通知、确认之前失败一次，用来看消息重投
    private final boolean fileOnMinio; // minio：附件进对象存储
    private final boolean inventoryPage; // page：库存列表分页
    private final boolean metrics; // on：记出库耗时和拒绝次数

    public LessonSwitches(
            @Value("${app.lesson.optimistic-lock:on}") String optimisticLock,
            @Value("${app.lesson.inbound-fail-before-status:false}") String inboundFailBeforeStatus,
            @Value("${app.lesson.outbound-read-pause-ms:0}") long outboundReadPauseMs, // 毫秒，0 是不停
            @Value("${app.lesson.cache-evict:on}") String cacheEvict,
            @Value("${app.lesson.stock-lock:sku-location}") String stockLock, // after-check 或 sku-location
            @Value("${app.lesson.submit-once:on}") String submitOnce,
            @Value("${app.lesson.publish-after-commit:on}") String publishAfterCommit,
            @Value("${app.lesson.consume-once:on}") String consumeOnce,
            @Value("${app.lesson.consume-fail-before-ack:false}") String consumeFailBeforeAck,
            @Value("${app.lesson.file-store:local}") String fileStore,
            @Value("${app.lesson.inventory-query:full}") String inventoryQuery,
            @Value("${app.lesson.metrics:off}") String metrics) {
        this.optimisticLock = enabled(optimisticLock);
        this.inboundFailBeforeStatus = enabled(inboundFailBeforeStatus);
        this.outboundReadPauseMs = outboundReadPauseMs;
        this.cacheEvict = enabled(cacheEvict);
        this.lockBeforeRead = "sku-location".equalsIgnoreCase(stockLock);
        this.submitOnce = enabled(submitOnce);
        this.publishAfterCommit = enabled(publishAfterCommit);
        this.consumeOnce = enabled(consumeOnce);
        this.consumeFailBeforeAck = enabled(consumeFailBeforeAck);
        this.fileOnMinio = "minio".equalsIgnoreCase(fileStore);
        this.inventoryPage = "page".equalsIgnoreCase(inventoryQuery);
        this.metrics = enabled(metrics);
    }

    // yaml 里不加引号的 on 会被读成字符串 "true"，所以 on、true、yes 都算打开。
    private static boolean enabled(String value) {
        return "on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }


    public boolean optimisticLock() {
        return optimisticLock;
    }



    public boolean inboundFailBeforeStatus() {
        return inboundFailBeforeStatus;
    }

    public long outboundReadPauseMs() {
        return outboundReadPauseMs;
    }

    public boolean cacheEvict() {
        return cacheEvict;
    }

    public boolean lockBeforeRead() {
        return lockBeforeRead;
    }

    public boolean submitOnce() {
        return submitOnce;
    }

    public boolean publishAfterCommit() {
        return publishAfterCommit;
    }

    public boolean consumeOnce() {
        return consumeOnce;
    }

    public boolean consumeFailBeforeAck() {
        return consumeFailBeforeAck;
    }

    public boolean fileOnMinio() {
        return fileOnMinio;
    }

    public boolean inventoryPage() {
        return inventoryPage;
    }

    public boolean metrics() {
        return metrics;
    }
}
