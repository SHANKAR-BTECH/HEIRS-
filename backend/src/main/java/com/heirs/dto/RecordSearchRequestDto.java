package com.heirs.dto;

import com.heirs.entity.Category;
import com.heirs.entity.RecordStatus;
import jakarta.validation.constraints.*;

public record RecordSearchRequestDto(
    @Size(max = 500) String q,
    Category category,
    @Min(1900) @Max(2100) Integer year,
    RecordStatus status,
    @Size(max = 150) String department) {}
