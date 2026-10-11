package com.eurasia.pro_skill.domain;

import lombok.Data;

// 入库单上的一个附件，上传后返回给页面。
@Data
public class InboundAttachment {

    private Long id; // 附件主键，下载时放在路径上
    private Long orderId; // 属于哪张入库单
    private String fileName; // 页面上传时的原名，下载时还用这个名字
    private String filePath; // 内容放在哪：本机绝对路径，或 MinIO 里的对象 key
}
