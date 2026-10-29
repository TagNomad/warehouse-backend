package com.eurasia.pro_skill.application;

import io.lettuce.core.Consumer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.SetArgs;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 连真正的 Redis。本机进程 redis-host 是 localhost，容器里是 Compose 服务名 redis。
@Component
@Profile("!test") // 测试换成 MemoryLessonRedis
public class RedisLessonRedis implements LessonRedis {

    // 值还是自己写进去的那个才删。GET 和 DEL 放在一段脚本里，中间不会插进别的命令
    private static final String DELETE_IF_VALUE =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;

    public RedisLessonRedis(@Value("${app.lesson.redis-host:redis}") String host,
                            @Value("${app.lesson.redis-port:6379}") int port) { // 6379 是 Redis 默认端口
        this.client = RedisClient.create("redis://" + host + ":" + port);
        this.connection = client.connect(); // 一条连接，多个线程共用
    }

    private RedisCommands<String, String> commands() {
        return connection.sync(); // 同步命令：调用返回时 Redis 已经执行完
    }

    @Override
    public String get(String key) {
        return commands().get(key);
    }

    @Override
    public void set(String key, String value) {
        commands().set(key, value);
    }

    @Override
    public boolean setIfAbsent(String key, String value, long ttlMs) {
        // SET key value NX PX ttl。成功回 OK，键已存在回 null
        return "OK".equals(commands().set(key, value, SetArgs.Builder.nx().px(ttlMs)));
    }

    @Override
    public void delete(String key) {
        commands().del(key);
    }

    @Override
    public boolean deleteIfValue(String key, String value) {
        Long deleted = commands().eval(DELETE_IF_VALUE, ScriptOutputType.INTEGER, new String[]{key}, value);
        return deleted != null && deleted == 1L; // 1 是删到了，0 是锁已经不是自己的
    }

    @Override
    public String addStream(String stream, Map<String, String> fields) {
        return commands().xadd(stream, fields); // 编号由 Redis 生成，例如 1760250000000-0
    }

    @Override
    public void createGroup(String stream, String group) {
        try {
            // 从 0 开始：建组之前已经在 Stream 里的消息也会投给这个组
            commands().xgroupCreate(XReadArgs.StreamOffset.from(stream, "0"), group, XGroupCreateArgs.Builder.mkstream());
        } catch (RedisCommandExecutionException e) {
            if (!e.getMessage().startsWith("BUSYGROUP")) { // BUSYGROUP 是组已经有了，其他错误照样抛出
                throw e;
            }
        }
    }

    @Override
    public List<LessonMessage> readGroup(String stream, String group, String consumer, boolean pending) {
        XReadArgs.StreamOffset<String> offset = pending
                ? XReadArgs.StreamOffset.from(stream, "0") // 0：投给我、我还没 XACK 的
                : XReadArgs.StreamOffset.lastConsumed(stream); // >：还没投给这个组的新消息
        List<StreamMessage<String, String>> read =
                commands().xreadgroup(Consumer.from(group, consumer), XReadArgs.Builder.count(10), offset);
        List<LessonMessage> messages = new ArrayList<>();
        for (StreamMessage<String, String> message : read) {
            messages.add(new LessonMessage(message.getId(), message.getBody()));
        }
        return messages;
    }

    @Override
    public void ack(String stream, String group, String id) {
        commands().xack(stream, group, id);
    }

    @PreDestroy // 应用停止时关掉连接
    public void close() {
        connection.close();
        client.shutdown();
    }
}
