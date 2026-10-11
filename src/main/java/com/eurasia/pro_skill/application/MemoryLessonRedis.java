package com.eurasia.pro_skill.application;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// 测试不用真的 Redis。键和消息都放在内存里，语义和 Redis 这一课用到的命令一样。
@Component
@Profile("test")
public class MemoryLessonRedis implements LessonRedis {

    private final ConcurrentHashMap<String, String> values = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> streams = new ConcurrentHashMap<>();

    @Override
    public String get(String key) {
        return values.get(key); // 没有键就是 null
    }

    @Override
    public void set(String key, String value) {
        values.put(key, value); // 后来的值盖住前面的
    }

    @Override
    public boolean setIfAbsent(String key, String value) {
        return values.putIfAbsent(key, value) == null; // 只有第一次放进去才成功
    }

    @Override
    public void delete(String key) {
        values.remove(key);
    }

    @Override
    public void addStream(String stream, String body) {
        streams.computeIfAbsent(stream, name -> new CopyOnWriteArrayList<>()).add(body);
    }

    @Override
    public List<String> readStream(String stream) {
        return new ArrayList<>(streams.getOrDefault(stream, List.of()));
    }
}
