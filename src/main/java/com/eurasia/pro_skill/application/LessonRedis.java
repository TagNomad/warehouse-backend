package com.eurasia.pro_skill.application;

import java.util.List;
import java.util.Map;

// 这一课用到的 Redis 命令。运行时连 Redis（RedisLessonRedis），测试里放在内存（MemoryLessonRedis）。
public interface LessonRedis {

    String get(String key); // GET。没有这个键时返回 null

    void set(String key, String value); // SET。覆盖写成这个值

    boolean setIfAbsent(String key, String value, long ttlMs); // SET key value NX PX ttl。键已存在就返回 false

    void delete(String key); // DEL

    boolean deleteIfValue(String key, String value); // 值还是 value 才 DEL。锁只能由加锁的那一次释放

    String addStream(String stream, Map<String, String> fields); // XADD stream * k v ...，返回消息编号

    void createGroup(String stream, String group); // XGROUP CREATE stream group 0 MKSTREAM。已有就跳过

    // XREADGROUP。pending 为 true 读自己领过、还没确认的（id 0）；为 false 读新消息（id >）
    List<LessonMessage> readGroup(String stream, String group, String consumer, boolean pending);

    void ack(String stream, String group, String id); // XACK。确认以后这条不会再投给这个组

    record LessonMessage(String id, Map<String, String> fields) { // 一条消息：编号和字段
    }
}
