package com.heirs.desktop.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Supporting document metadata matching GET /api/records/{recordId}/documents.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DocumentResponse(
        Long id,
        Long recordId,
        String originalFileName,
        String contentType,
        Long fileSize,
        LocalDateTime uploadedAt,
        LocalDateTime updatedAt
) {}
