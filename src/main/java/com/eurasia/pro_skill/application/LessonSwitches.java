package com.eurasia.pro_skill.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 课堂开关。值写在 application.yaml 的 app.lesson 下，容器里由 Compose 的 APP_LESSON_* 盖过。
// 第三部分默认走有漏洞的那一侧，第三部分改良版把前四项改成修复后的值。
@Component // 注册成 Bean，Service 构造时注入
public class LessonSwitches {

    private final boolean outboundCheck; // on：出库前看剩余够不够
    private final boolean optimisticLock; // on：更新库存时带上读到的 version
    private final boolean orderStateEnforced; // enforced：不是草稿就不能改行
    private final boolean flowAfterCommit; // on：流水和入库完成在同一个事务里
    private final boolean inboundFailBeforeStatus; // true：改状态前让主事务失败，用来看流水会不会留下来
    private final long outboundReadPauseMs; // 大于 0：出库读到数量后停这么多毫秒，让两次请求读到同一个旧数

    public LessonSwitches(
            @Value("${app.lesson.outbound-check:off}") String outboundCheck, // 没写时按 off
            @Value("${app.lesson.optimistic-lock:off}") String optimisticLock,
            @Value("${app.lesson.order-state:open}") String orderState, // open 或 enforced
            @Value("${app.lesson.flow-after-commit:off}") String flowAfterCommit,
            @Value("${app.lesson.inbound-fail-before-status:false}") String inboundFailBeforeStatus,
            @Value("${app.lesson.outbound-read-pause-ms:0}") long outboundReadPauseMs) { // 毫秒，0 是不停
        this.outboundCheck = enabled(outboundCheck);
        this.optimisticLock = enabled(optimisticLock);
        this.orderStateEnforced = "enforced".equalsIgnoreCase(orderState);
        this.flowAfterCommit = enabled(flowAfterCommit);
        this.inboundFailBeforeStatus = enabled(inboundFailBeforeStatus);
        this.outboundReadPauseMs = outboundReadPauseMs;
    }

    // yaml 里不加引号的 on 会被读成字符串 "true"，所以 on、true、yes 都算打开。
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

    public long outboundReadPauseMs() {
        return outboundReadPauseMs;
    }
}
