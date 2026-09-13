package com.heirs.dto;

import com.heirs.entity.Category;
import com.heirs.entity.RecordStatus;
import java.time.LocalDate;

public record RecordResponseDto(
    Long id,
    String title,
    String description,
    Category category,
    String department,
    String referenceNumber,
    Integer publicationYear,
    LocalDate publishedDate,
    RecordStatus status,
    String source,
    String keywords) {}
