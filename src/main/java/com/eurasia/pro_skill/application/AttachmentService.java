package com.eurasia.pro_skill.application;

import com.eurasia.pro_skill.common.BusinessException;
import com.eurasia.pro_skill.domain.AttachmentFile;
import com.eurasia.pro_skill.domain.InboundAttachment;
import com.eurasia.pro_skill.entity.InboundAttachmentEntity;
import com.eurasia.pro_skill.entity.InboundOrderEntity;
import com.eurasia.pro_skill.mapper.InboundAttachmentMapper;
import com.eurasia.pro_skill.mapper.InboundOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

// 入库附件。内容放在哪由 file-store 决定，叫什么名字由 file-key 决定。表里只记原名和放的位置。
@Service
public class AttachmentService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);

    private final InboundOrderMapper orderMapper; // 查附件挂在哪张入库单上
    private final InboundAttachmentMapper attachmentMapper; // 读写 inbound_attachment
    private final ObjectStore objectStore; // MinIO
    private final LessonSwitches switches;
    private final Path localDir; // file-store 是 local 时的根目录

    public AttachmentService(InboundOrderMapper orderMapper, InboundAttachmentMapper attachmentMapper,
                             ObjectStore objectStore, LessonSwitches switches,
                             @Value("${app.lesson.local-file-dir:data/attachments}") String localDir) {
        this.orderMapper = orderMapper;
        this.attachmentMapper = attachmentMapper;
        this.objectStore = objectStore;
        this.switches = switches;
        // 相对路径从进程的工作目录算起：本机是项目目录，容器里是 /app
        this.localDir = Path.of(localDir).toAbsolutePath().normalize();
    }

    public InboundAttachment upload(Long orderId, String fileName, String contentType, byte[] bytes) {
        // orderId 来自路径 /inbound-orders/{orderId}/attachments，另外三个值来自表单字段 file
        InboundOrderEntity order = orderMapper.getById(orderId); // 附件挂在哪张入库单上
        if (order == null) {
            throw new BusinessException("INBOUND_NOT_FOUND", "入库单不存在：" + orderId);
        }
        if (bytes.length == 0) { // 表单里没选文件，或者选了空文件
            throw new BusinessException("FILE_EMPTY", "FILE_EMPTY：文件是空的");
        }
        String originalName = fileName == null || fileName.isBlank() ? "file.bin" : fileName; // 页面上的原名
        String storedName = storedName(originalName); // 真正存放时用的名字
        String location = switches.fileOnMinio()
                ? putObject(order.getOrderNo(), storedName, contentType, bytes) // minio：返回对象 key
                : putLocal(order.getOrderNo(), storedName, bytes); // local：返回本机绝对路径
        InboundAttachmentEntity row = new InboundAttachmentEntity(); // 准备 inbound_attachment 的一行
        row.setOrderId(order.getId()); // 写进 order_id：入库单主键
        row.setFileName(originalName); // 写进 file_name：原名，下载时还给页面
        row.setFilePath(location); // 写进 file_path：下载时按它去找内容
        attachmentMapper.insert(row); // 插入后主键写回 row.getId()。返回的 int 是 1
        log.info("{} 附件 {} 存到 {}", order.getOrderNo(), originalName, location);
        return toDomain(row);
    }

    public AttachmentFile download(Long orderId, Long attachmentId) {
        // 两个主键都来自路径 /inbound-orders/{orderId}/attachments/{attachmentId}
        InboundAttachmentEntity row = attachmentMapper.getById(attachmentId); // 先从表里找到 file_path
        if (row == null || !row.getOrderId().equals(orderId)) { // 附件不是这张单的，也当作没有
            throw new BusinessException("FILE_NOT_FOUND", "FILE_NOT_FOUND：这张入库单没有附件 " + attachmentId);
        }
        String location = row.getFilePath();
        // 本机绝对路径到磁盘上读；其余是对象 key，到 MinIO 读。看的是写入时记下的值，不看现在的开关
        byte[] bytes = Path.of(location).isAbsolute() ? readLocal(location) : objectStore.get(location);
        if (bytes == null) { // 表里有这一行，内容却找不到了
            throw new BusinessException("FILE_NOT_FOUND", "FILE_NOT_FOUND：找不到 " + location);
        }
        return new AttachmentFile(row.getFileName(), bytes);
    }

    private String storedName(String originalName) {
        if (!switches.uniqueFileKey()) { // file-key 是 filename：原样用页面传来的名字
            return originalName; // 同名会互相覆盖；名字里带 ../ 会跳出目录
        }
        int dot = originalName.lastIndexOf('.');
        String ext = dot < 0 ? "" : originalName.substring(dot).replaceAll("[^A-Za-z0-9.]", ""); // 只留 .txt 这样的扩展名
        return UUID.randomUUID() + (ext.length() > 10 ? "" : ext); // 例如 3f2a…e1.txt，每次上传都不同
    }

    private String putLocal(String orderNo, String storedName, byte[] bytes) {
        Path dir = localDir.resolve(orderNo); // 例如 /app/data/attachments/IN-1
        Path file = dir.resolve(storedName).normalize(); // storedName 是 ../../x 时，normalize 后已经不在 dir 里
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, bytes); // 写在运行这个进程的机器上。同名文件直接覆盖
        } catch (IOException e) {
            throw new BusinessException("FILE_WRITE_FAILED", "FILE_WRITE_FAILED：写不了 " + file);
        }
        return file.toString(); // 表里记下这台机器上的绝对路径
    }

    private String putObject(String orderNo, String storedName, String contentType, byte[] bytes) {
        String key = "inbound/" + orderNo + "/" + storedName; // 对象 key，例如 inbound/IN-1/3f2a…e1.txt
        objectStore.put(key, bytes, contentType); // 放进桶 wms。同一个 key 再放一次会覆盖
        return key; // 表里只记 key，不带任何机器上的目录
    }

    private byte[] readLocal(String location) {
        Path path = Path.of(location);
        if (!Files.exists(path)) { // 换了机器、换了容器，这个路径就不在了
            return null;
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            return null;
        }
    }

    private InboundAttachment toDomain(InboundAttachmentEntity row) { // 表的一行变成返回给页面的 JSON
        InboundAttachment attachment = new InboundAttachment();
        attachment.setId(row.getId());
        attachment.setOrderId(row.getOrderId());
        attachment.setFileName(row.getFileName());
        attachment.setFilePath(row.getFilePath());
        return attachment;
    }
}
