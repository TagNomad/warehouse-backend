package com.eurasia.pro_skill.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 课堂开关。值写在 application.yaml 的 app.lesson 下，也可以在启动参数里写 --app.lesson.optimistic-lock=off。
@Component // 注册成 Bean，Service 构造时注入
public class LessonSwitches {

    private final boolean optimisticLock; // on：更新库存时带上读到的 version；off：只按 id 改，演示两次同时出库
    private final long outboundReadPauseMs; // 大于 0：出库读到数量后停这么多毫秒，让两次请求读到同一个旧数

    public LessonSwitches(
            @Value("${app.lesson.optimistic-lock:on}") String optimisticLock, // 没写时按 on
            @Value("${app.lesson.outbound-read-pause-ms:0}") long outboundReadPauseMs) { // 毫秒，0 是不停
        this.optimisticLock = "on".equalsIgnoreCase(optimisticLock) || "true".equalsIgnoreCase(optimisticLock);
        this.outboundReadPauseMs = outboundReadPauseMs;
    }

    public boolean optimisticLock() {
        return optimisticLock;
    }

    public long outboundReadPauseMs() {
        return outboundReadPauseMs;
    }
}
