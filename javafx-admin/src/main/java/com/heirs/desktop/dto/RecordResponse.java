package com.heirs.desktop.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Desktop DTO matching the HEIRS Spring Boot backend record JSON representation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RecordResponse(
        Long id,
        String title,
        String description,
        String category,
        String department,
        String referenceNumber,
        Integer publicationYear,
        LocalDate publishedDate,
        String status,
        String source,
        String keywords
) {}
