package com.eurasia.pro_skill.controller;

import com.eurasia.pro_skill.application.AttachmentService;
import com.eurasia.pro_skill.domain.AttachmentFile;
import com.eurasia.pro_skill.domain.InboundAttachment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// 入库附件。和 UserController 一样：不决定文件存在哪，把路径和表单里的值交给 Service。
@Tag(name = "入库附件", description = "给入库单传附件、下载附件")
@RestController
@RequestMapping("/inbound-orders/{orderId}/attachments") // {orderId} 是入库单主键
@RequiredArgsConstructor
public class InboundAttachmentController {

    private final AttachmentService attachmentService;

    @Operation(summary = "上传附件")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // 表单上传，不是 JSON
    public InboundAttachment upload(@PathVariable Long orderId,
                                    @RequestParam("file") MultipartFile file) throws IOException { // 表单字段名是 file
        // orderId 来自路径。原名、类型、内容都从 file 里取，原样传下去
        return attachmentService.upload(orderId, file.getOriginalFilename(), file.getContentType(), file.getBytes());
    }

    @Operation(summary = "下载附件")
    @GetMapping("/{attachmentId}") // GET /inbound-orders/1/attachments/1
    public ResponseEntity<byte[]> download(@PathVariable Long orderId, @PathVariable Long attachmentId) {
        AttachmentFile file = attachmentService.download(orderId, attachmentId); // 原名和内容
        MediaType type = MediaTypeFactory.getMediaType(file.fileName())
                .orElse(MediaType.APPLICATION_OCTET_STREAM); // 按扩展名定类型，.txt 是 text/plain
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8).build(); // 浏览器保存时用原名，中文名也不乱码
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.bytes()); // 响应体就是文件内容
    }
}
