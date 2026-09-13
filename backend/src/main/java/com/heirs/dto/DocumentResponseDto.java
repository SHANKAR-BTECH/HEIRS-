package com.heirs.dto;

import com.heirs.entity.Document;
import java.time.LocalDateTime;

public record DocumentResponseDto(
    Long id,
    Long recordId,
    String originalFileName,
    String contentType,
    Long fileSize,
    LocalDateTime uploadedAt,
    LocalDateTime updatedAt) {
  public static DocumentResponseDto from(Document d) {
    return new DocumentResponseDto(
        d.getId(),
        d.getRecord().getId(),
        d.getOriginalFileName(),
        d.getContentType(),
        d.getFileSize(),
        d.getUploadedAt(),
        d.getUpdatedAt());
  }
}
