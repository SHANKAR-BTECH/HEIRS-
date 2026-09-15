package com.heirs.desktop.model;

import java.time.format.DateTimeFormatter;

import com.heirs.desktop.dto.DocumentResponse;

/**
 * UI display model for supporting documents.
 */
public class DocumentInfo {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Long id;
    private final Long recordId;
    private final String fileName;
    private final String fileType;
    private final String fileSize;
    private final String updatedDate;
    private final String relatedReference;
    private final String status;

    public DocumentInfo(Long id,
                        Long recordId,
                        String fileName,
                        String fileType,
                        String fileSize,
                        String updatedDate,
                        String relatedReference,
                        String status) {
        this.id = id;
        this.recordId = recordId;
        this.fileName = fileName != null ? fileName : "";
        this.fileType = fileType != null ? fileType : "";
        this.fileSize = fileSize != null ? fileSize : "";
        this.updatedDate = updatedDate != null ? updatedDate : "";
        this.relatedReference = relatedReference != null ? relatedReference : "";
        this.status = status != null ? status : "Active";
    }

    public static DocumentInfo from(DocumentResponse dto, String relatedReference) {
        if (dto == null) {
            return null;
        }

        String type = "PDF";
        if (dto.contentType() != null && dto.contentType().contains("pdf")) {
            type = "PDF";
        } else if (dto.originalFileName() != null && dto.originalFileName().contains(".")) {
            type = dto.originalFileName().substring(dto.originalFileName().lastIndexOf('.') + 1).toUpperCase();
        }

        String formattedSize = com.heirs.desktop.util.FileNames.formatSize(dto.fileSize() != null ? dto.fileSize() : 0L);
        String updated = dto.updatedAt() != null
                ? dto.updatedAt().format(DATE_FORMATTER)
                : (dto.uploadedAt() != null ? dto.uploadedAt().format(DATE_FORMATTER) : "—");

        return new DocumentInfo(
                dto.id(),
                dto.recordId(),
                dto.originalFileName(),
                type,
                formattedSize,
                updated,
                relatedReference,
                "Attached"
        );
    }

    public Long getId() {
        return id;
    }

    public Long getRecordId() {
        return recordId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public String getFileSize() {
        return fileSize;
    }

    public String getUpdatedDate() {
        return updatedDate;
    }

    public String getRelatedReference() {
        return relatedReference;
    }

    public String getStatus() {
        return status;
    }
}
