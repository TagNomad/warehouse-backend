package com.eurasia.pro_skill.application;

// 附件内容放进对象存储。运行时是 MinioObjectStore，测试时是 MemoryObjectStore。
public interface ObjectStore {

    // 放一个对象。key 例如 inbound/IN-1/3f2a….txt；同一个 key 再放一次会覆盖
    void put(String key, byte[] bytes, String contentType);

    // 按 key 取回内容。没有这个对象返回 null
    byte[] get(String key);
}
