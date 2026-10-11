package com.eurasia.pro_skill.application;

import java.util.List;

// 课堂用的 Redis。测试里是内存实现，运行时连 Redis。
public interface LessonRedis {

    String get(String key); // 没有这个键时返回 null

    void set(String key, String value); // 覆盖写成这个值

    boolean setIfAbsent(String key, String value); // 键已存在就返回 false，不覆盖

    void delete(String key); // 删掉这个键

    void addStream(String stream, String body); // 往列表尾部追加一条消息

    List<String> readStream(String stream); // 按写入顺序读出全部消息
}
