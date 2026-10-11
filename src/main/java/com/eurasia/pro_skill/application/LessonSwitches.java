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

    public LessonSwitches(
            @Value("${app.lesson.outbound-check:off}") String outboundCheck,
            @Value("${app.lesson.optimistic-lock:off}") String optimisticLock,
            @Value("${app.lesson.order-state:open}") String orderState,
            @Value("${app.lesson.flow-after-commit:off}") String flowAfterCommit,
            @Value("${app.lesson.inbound-fail-before-status:false}") String inboundFailBeforeStatus) {
        this.outboundCheck = enabled(outboundCheck);
        this.optimisticLock = enabled(optimisticLock);
        this.orderStateEnforced = "enforced".equalsIgnoreCase(orderState);
        this.flowAfterCommit = enabled(flowAfterCommit);
        this.inboundFailBeforeStatus = enabled(inboundFailBeforeStatus);
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
}
