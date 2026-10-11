package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.entity.InboundAttachmentEntity;
import com.eurasia.pro_skill.entity.InboundOrderEntity;
import com.eurasia.pro_skill.mapper.InboundAttachmentMapper;
import com.eurasia.pro_skill.mapper.InboundOrderMapper;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

// 入库附件。local 写本机磁盘，minio 写对象存储，表里只留定位用的字符串。
@Service
@RequiredArgsConstructor
public class AttachmentService {

    public static final String BUCKET = "wms";

    private final InboundOrderMapper orderMapper;
    private final InboundAttachmentMapper attachmentMapper;
    private final LessonSwitches switches;

    @Value("${app.lesson.minio-endpoint:http://minio:9000}")
    private String minioEndpoint;
    @Value("${app.lesson.minio-access:minioadmin}")
    private String minioAccess;
    @Value("${app.lesson.minio-secret:minioadmin}")
    private String minioSecret;

    public InboundAttachmentEntity save(Long orderId, String filename, byte[] bytes) {
        InboundOrderEntity order = orderMapper.getById(orderId); // 路径上的入库单主键
        if (order == null) {
            throw new BusinessException("INBOUND_NOT_FOUND", "入库单不存在：" + orderId);
        }
        String stored = switches.fileOnMinio()
                ? putMinio(order.getOrderNo(), filename, bytes) // 对象 key
                : putLocal(order.getOrderNo(), filename, bytes); // 本机绝对路径
        InboundAttachmentEntity row = new InboundAttachmentEntity();
        row.setOrderId(orderId);
        row.setFileName(filename);
        row.setFilePath(stored);
        attachmentMapper.insert(row); // 插入后 row.getId() 有值
        return row;
    }

    public byte[] load(Long attachmentId) {
        InboundAttachmentEntity row = attachmentMapper.getById(attachmentId);
        if (row == null) {
            throw new BusinessException("FILE_NOT_FOUND", "FILE_NOT_FOUND：附件不存在");
        }
        try {
            if (switches.fileOnMinio()) {
                return readMinio(row.getFilePath()); // file_path 这时是对象 key
            }
            Path path = Path.of(row.getFilePath()); // file_path 这时是本机绝对路径
            if (!Files.exists(path)) {
                throw new BusinessException("FILE_NOT_FOUND", "FILE_NOT_FOUND：本机没有这个文件：" + path);
            }
            return Files.readAllBytes(path);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("FILE_NOT_FOUND", "FILE_NOT_FOUND：读不到附件");
        }
    }

    public static String objectKey(String orderNo, String filename) {
        return "inbound/" + orderNo + "/" + filename; // 不带机器路径
    }

    private String putLocal(String orderNo, String filename, byte[] bytes) {
        try {
            Path dir = Path.of("data", "attachments", orderNo).toAbsolutePath(); // 写在运行这个进程的机器上
            Files.createDirectories(dir);
            Path file = dir.resolve(filename);
            Files.write(file, bytes);
            return file.toString(); // 表里记下绝对路径
        } catch (Exception exception) {
            throw new BusinessException("FILE_NOT_FOUND", "FILE_NOT_FOUND：写不了本地文件");
        }
    }

    private String putMinio(String orderNo, String filename, byte[] bytes) {
        String key = objectKey(orderNo, filename);
        try {
            MinioClient client = client();
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
            }
            client.putObject(PutObjectArgs.builder().bucket(BUCKET).object(key)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1).build());
            return key; // 表里只留 key
        } catch (Exception exception) {
            throw new BusinessException("FILE_NOT_FOUND", "FILE_NOT_FOUND：写不进 MinIO");
        }
    }

    private byte[] readMinio(String key) throws Exception {
        try (var stream = client().getObject(GetObjectArgs.builder().bucket(BUCKET).object(key).build())) {
            return stream.readAllBytes();
        }
    }

    private MinioClient client() {
        return MinioClient.builder().endpoint(minioEndpoint)
                .credentials(minioAccess, minioSecret).build();
    }
}
