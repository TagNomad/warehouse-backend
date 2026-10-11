package com.eurasia.pro_skill.domain;

// 下载时交给 Controller 的两样东西：原来的文件名和文件内容。
public record AttachmentFile(String fileName, byte[] bytes) {
}
