package com.eurasia.pro_skill.application;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

// 定时调用消费者。和接口请求不在同一个线程，入库接口返回时通知可能还没写。
@Configuration
@EnableScheduling // 打开 @Scheduled
@Profile("!test") // 测试里直接调用 consumer.poll()，不让定时任务插进来
@RequiredArgsConstructor
public class InboundNoticePoller {

    private final InboundNoticeConsumer consumer;

    @Scheduled(fixedDelayString = "${app.lesson.notice-poll-ms:1000}") // 上一次结束后再等这么久
    public void poll() {
        consumer.poll();
    }
}
