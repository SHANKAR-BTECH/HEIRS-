package com.heirs.dto;

import com.heirs.entity.Category;
import com.heirs.entity.RecordStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CreateRecordRequestDto(
    @NotBlank(message = "Title is required") @Size(max = 255) String title,
    @Size(max = 20000) String description,
    @NotNull(message = "Category is required") Category category,
    @NotBlank(message = "Department is required") @Size(max = 150) String department,
    @NotBlank(message = "Reference number is required") @Size(max = 100) String referenceNumber,
    @Min(1900) @Max(2100) Integer publicationYear,
    LocalDate publishedDate,
    @NotNull(message = "Status is required") RecordStatus status,
    @Size(max = 500) String source,
    @Size(max = 2000) String keywords) {}
