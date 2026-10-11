package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.application.AttachmentService;
import com.eurasia.pro_skill.entity.InboundAttachmentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// 入库附件。Controller 把文件字节交给 Service，不在这里决定存在哪。
@RestController
@RequestMapping("/inbound-orders/{orderId}/attachments")
@RequiredArgsConstructor
public class InboundAttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping
    public InboundAttachmentEntity upload(@PathVariable Long orderId, @RequestParam("file") MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() == null ? "file.bin" : file.getOriginalFilename(); // 页面传来的文件名
        return attachmentService.save(orderId, filename, file.getBytes()); // 字节原样交下去
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<byte[]> download(@PathVariable Long orderId, @PathVariable Long attachmentId) {
        byte[] body = attachmentService.load(attachmentId); // 按附件主键读内容
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .body(body);
    }
}
