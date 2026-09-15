package com.heirs.desktop.dto;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Generic pagination envelope matching the HEIRS Spring Boot backend PageResponseDto.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public List<T> safeContent() {
        return content != null ? content : Collections.emptyList();
    }

    public static <T> PagedResponse<T> empty() {
        return new PagedResponse<>(Collections.emptyList(), 0, 20, 0L, 0, true, true);
    }
}
