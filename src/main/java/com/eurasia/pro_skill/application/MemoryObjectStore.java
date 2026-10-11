package com.eurasia.pro_skill.application;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 测试不连 MinIO。对象按 key 放在内存里，同一个 key 再放一次会覆盖，和 MinIO 一样。
@Component
@Profile("test")
public class MemoryObjectStore implements ObjectStore {

    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override
    public void put(String key, byte[] bytes, String contentType) {
        objects.put(key, bytes.clone());
    }

    @Override
    public byte[] get(String key) {
        byte[] bytes = objects.get(key);
        return bytes == null ? null : bytes.clone();
    }

    public void clear() {
        objects.clear();
    }
}
