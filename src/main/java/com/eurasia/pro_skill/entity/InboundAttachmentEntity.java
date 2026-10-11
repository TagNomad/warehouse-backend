package com.eurasia.pro_skill.entity;

import lombok.Data;

// 对应 inbound_attachment 的一行。
@Data
public class InboundAttachmentEntity {

    private Long id; // 自增主键
    private Long orderId; // 属于哪张入库单
    private String fileName; // 页面上传时的文件名
    private String filePath; // 本机绝对路径，或 MinIO 的对象 key
}
