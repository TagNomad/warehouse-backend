package com.eurasia.pro_skill.application;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

// 连 Compose 里的 Redis。主机名用服务名 redis，不用 localhost。
@Component
@Profile("!test")
public class RedisLessonRedis implements LessonRedis {

    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;

    public RedisLessonRedis(@Value("${app.lesson.redis-host:redis}") String host) {
        this.client = RedisClient.create("redis://" + host + ":6379"); // 容器里 host 是服务名
        this.connection = client.connect();
    }

    private RedisCommands<String, String> commands() {
        return connection.sync();
    }

    @Override
    public String get(String key) {
        return commands().get(key); // GET
    }

    @Override
    public void set(String key, String value) {
        commands().set(key, value); // SET
    }

    @Override
    public boolean setIfAbsent(String key, String value) {
        return commands().setnx(key, value); // SETNX，键已在就 false
    }

    @Override
    public void delete(String key) {
        commands().del(key); // DEL
    }

    @Override
    public void addStream(String stream, String body) {
        commands().rpush(stream, body); // 列表当这一课的消息队列
    }

    @Override
    public List<String> readStream(String stream) {
        return commands().lrange(stream, 0, -1); // 从头读到尾
    }

    @PreDestroy
    public void close() {
        connection.close();
        client.shutdown();
    }
}
