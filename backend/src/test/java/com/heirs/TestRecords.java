package com.heirs;

import com.heirs.dto.*;
import com.heirs.entity.*;
import com.heirs.entity.Record;
import com.heirs.mapper.RecordMapper;
import java.time.LocalDate;

public final class TestRecords {
  private TestRecords() {}

  public static CreateRecordRequestDto create(String reference) {
    return new CreateRecordRequestDto(
        "Digital Learning Policy",
        "Scholarship access",
        Category.POLICY,
        "Higher Education Department",
        reference,
        2026,
        LocalDate.of(2026, 3, 12),
        RecordStatus.ACTIVE,
        null,
        "digital,learning");
  }

  public static Record entity(String reference) {
    return new RecordMapper().toEntity(create(reference));
  }

  public static UpdateRecordRequestDto update(String reference) {
    return new UpdateRecordRequestDto(
        "Revised Policy",
        null,
        Category.POLICY,
        "Higher Education Department",
        reference,
        2025,
        null,
        RecordStatus.ARCHIVED,
        null,
        null);
  }
}
