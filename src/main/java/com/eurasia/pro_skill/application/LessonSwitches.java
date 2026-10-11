package com.eurasia.pro_skill.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 课堂开关。默认走有漏洞的那一侧，改良版把对应项改成修复后的值。
@Component
public class LessonSwitches {

    private final boolean outboundCheck; // on：出库前看剩余够不够
    private final boolean optimisticLock; // on：更新库存时带上 version
    private final boolean orderStateEnforced; // enforced：不是草稿就不能改行
    private final boolean flowAfterCommit; // on：流水和入库完成在同一个事务里
    private final boolean inboundFailBeforeStatus; // true：改状态前让主事务失败，用来看流水会不会留下来
    private final boolean cacheEvict; // on：出库后删掉库存缓存
    private final String stockLock; // after-check 或 sku-location
    private final boolean submitOnce; // on：同一单号只扣一次
    private final boolean publishAfterCommit; // on：事务提交之后才发消息
    private final boolean consumeOnce; // on：同一个入库单号只记一笔流水
    private final boolean fileOnMinio; // minio：附件进对象存储
    private final boolean inventoryPage; // page：库存列表分页
    private final boolean metrics; // on：记出库耗时和拒绝次数

    public LessonSwitches(
            @Value("${app.lesson.outbound-check:off}") String outboundCheck,
            @Value("${app.lesson.optimistic-lock:off}") String optimisticLock,
            @Value("${app.lesson.order-state:open}") String orderState,
            @Value("${app.lesson.flow-after-commit:off}") String flowAfterCommit,
            @Value("${app.lesson.inbound-fail-before-status:false}") String inboundFailBeforeStatus,
            @Value("${app.lesson.cache-evict:off}") String cacheEvict,
            @Value("${app.lesson.stock-lock:after-check}") String stockLock,
            @Value("${app.lesson.submit-once:off}") String submitOnce,
            @Value("${app.lesson.publish-after-commit:off}") String publishAfterCommit,
            @Value("${app.lesson.consume-once:off}") String consumeOnce,
            @Value("${app.lesson.file-store:local}") String fileStore,
            @Value("${app.lesson.inventory-query:full}") String inventoryQuery,
            @Value("${app.lesson.metrics:off}") String metrics) {
        this.outboundCheck = enabled(outboundCheck);
        this.optimisticLock = enabled(optimisticLock);
        this.orderStateEnforced = "enforced".equalsIgnoreCase(orderState);
        this.flowAfterCommit = enabled(flowAfterCommit);
        this.inboundFailBeforeStatus = enabled(inboundFailBeforeStatus);
        this.cacheEvict = enabled(cacheEvict);
        this.stockLock = stockLock == null ? "after-check" : stockLock;
        this.submitOnce = enabled(submitOnce);
        this.publishAfterCommit = enabled(publishAfterCommit);
        this.consumeOnce = enabled(consumeOnce);
        this.fileOnMinio = "minio".equalsIgnoreCase(fileStore);
        this.inventoryPage = "page".equalsIgnoreCase(inventoryQuery);
        this.metrics = enabled(metrics);
    }

    private static boolean enabled(String value) {
        return "on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }

    public boolean outboundCheck() {
        return outboundCheck;
    }

    public boolean optimisticLock() {
        return optimisticLock;
    }

    public boolean orderStateEnforced() {
        return orderStateEnforced;
    }

    public boolean flowAfterCommit() {
        return flowAfterCommit;
    }

    public boolean inboundFailBeforeStatus() {
        return inboundFailBeforeStatus;
    }

    public boolean cacheEvict() {
        return cacheEvict;
    }

    public String stockLock() {
        return stockLock;
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
