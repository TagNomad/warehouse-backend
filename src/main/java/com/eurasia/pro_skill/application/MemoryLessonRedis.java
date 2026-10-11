package com.eurasia.pro_skill.application;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 测试不连 Redis。键、过期时间、Stream 和消费组都放在内存里，行为和这一课用到的命令一致。
@Component
@Profile("test")
public class MemoryLessonRedis implements LessonRedis {

    private record Entry(String value, long expireAt) { // expireAt 是 0 表示不过期
        boolean alive() {
            return expireAt == 0 || System.currentTimeMillis() < expireAt;
        }
    }

    private static final class Group {
        int delivered; // 已经投给这个组的条数
        final Set<String> pending = new LinkedHashSet<>(); // 投出去、还没确认的编号
    }

    private final Map<String, Entry> values = new ConcurrentHashMap<>();
    private final Map<String, List<LessonMessage>> streams = new LinkedHashMap<>();
    private final Map<String, Group> groups = new LinkedHashMap<>();
    private long sequence;

    @Override
    public String get(String key) {
        Entry entry = values.get(key);
        return entry != null && entry.alive() ? entry.value() : null;
    }

    @Override
    public void set(String key, String value) {
        values.put(key, new Entry(value, 0));
    }

    @Override
    public synchronized boolean setIfAbsent(String key, String value, long ttlMs) {
        if (get(key) != null) {
            return false;
        }
        values.put(key, new Entry(value, System.currentTimeMillis() + ttlMs));
        return true;
    }

    @Override
    public void delete(String key) {
        values.remove(key);
    }

    @Override
    public synchronized boolean deleteIfValue(String key, String value) {
        if (!value.equals(get(key))) {
            return false;
        }
        values.remove(key);
        return true;
    }

    @Override
    public synchronized String addStream(String stream, Map<String, String> fields) {
        String id = System.currentTimeMillis() + "-" + (sequence++);
        streams.computeIfAbsent(stream, name -> new ArrayList<>()).add(new LessonMessage(id, Map.copyOf(fields)));
        return id;
    }

    @Override
    public synchronized void createGroup(String stream, String group) {
        streams.computeIfAbsent(stream, name -> new ArrayList<>());
        groups.computeIfAbsent(stream + "/" + group, name -> new Group());
    }

    @Override
    public synchronized List<LessonMessage> readGroup(String stream, String group, String consumer, boolean pending) {
        Group state = groups.get(stream + "/" + group);
        List<LessonMessage> all = streams.getOrDefault(stream, List.of());
        List<LessonMessage> result = new ArrayList<>();
        if (pending) {
            for (LessonMessage message : all) {
                if (state.pending.contains(message.id())) {
                    result.add(message);
                }
            }
            return result;
        }
        while (state.delivered < all.size()) {
            LessonMessage message = all.get(state.delivered++);
            state.pending.add(message.id());
            result.add(message);
        }
        return result;
    }

    @Override
    public synchronized void ack(String stream, String group, String id) {
        groups.get(stream + "/" + group).pending.remove(id);
    }

    public synchronized void clear() { // 测试之间清空
        values.clear();
        streams.clear();
        groups.clear();
    }
}
