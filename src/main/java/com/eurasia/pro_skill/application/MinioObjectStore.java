package com.eurasia.pro_skill.application;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

// 连真正的 MinIO。本机进程 minio-endpoint 是 http://localhost:9000，容器里是 Compose 服务名 http://minio:9000。
@Component
@Profile("!test") // 测试换成 MemoryObjectStore
public class MinioObjectStore implements ObjectStore {

    static final String BUCKET = "wms"; // 所有附件放在这一个桶里，桶里再按 inbound/单号/ 分目录

    private final MinioClient client;
    private volatile boolean bucketReady; // 桶建过一次就不再查

    public MinioObjectStore(@Value("${app.lesson.minio-endpoint:http://minio:9000}") String endpoint,
                            @Value("${app.lesson.minio-access:minioadmin}") String access,
                            @Value("${app.lesson.minio-secret:minioadmin}") String secret) {
        this.client = MinioClient.builder().endpoint(endpoint).credentials(access, secret).build(); // 一个客户端，多个请求共用
    }

    @Override
    public void put(String key, byte[] bytes, String contentType) {
        try {
            ensureBucket();
            client.putObject(PutObjectArgs.builder().bucket(BUCKET).object(key)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1) // 长度已知，-1 是不分片
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("写不进 MinIO：" + key, e);
        }
    }

    @Override
    public byte[] get(String key) {
        try (InputStream in = client.getObject(GetObjectArgs.builder().bucket(BUCKET).object(key).build())) {
            return in.readAllBytes();
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code()) || "NoSuchBucket".equals(e.errorResponse().code())) {
                return null; // 没有这个对象
            }
            throw new IllegalStateException("读不了 MinIO：" + key, e);
        } catch (Exception e) {
            throw new IllegalStateException("读不了 MinIO：" + key, e);
        }
    }

    private void ensureBucket() throws Exception {
        if (bucketReady) {
            return;
        }
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build()); // 第一次上传时建桶 wms
        }
        bucketReady = true;
    }
}
