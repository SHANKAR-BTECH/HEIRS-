package com.heirs.mapper;

import com.heirs.dto.*;
import com.heirs.entity.Record;
import org.springframework.stereotype.Component;

@Component
public class RecordMapper {
  public RecordResponseDto toDto(Record record) {
    return new RecordResponseDto(
        record.getId(),
        record.getTitle(),
        record.getDescription(),
        record.getCategory(),
        record.getDepartment(),
        record.getReferenceNumber(),
        record.getPublicationYear(),
        record.getPublishedDate(),
        record.getStatus(),
        record.getSource(),
        record.getKeywords());
  }

  public Record toEntity(CreateRecordRequestDto dto) {
    Record record = new Record();
    record.setTitle(dto.title().trim());
    record.setDescription(dto.description());
    record.setCategory(dto.category());
    record.setDepartment(dto.department().trim());
    record.setReferenceNumber(dto.referenceNumber().trim());
    record.setPublicationYear(dto.publicationYear());
    record.setPublishedDate(dto.publishedDate());
    record.setStatus(dto.status());
    record.setSource(dto.source());
    record.setKeywords(dto.keywords());
    return record;
  }

  public void update(Record record, UpdateRecordRequestDto dto) {
    record.setTitle(dto.title().trim());
    record.setDescription(dto.description());
    record.setCategory(dto.category());
    record.setDepartment(dto.department().trim());
    record.setReferenceNumber(dto.referenceNumber().trim());
    record.setPublicationYear(dto.publicationYear());
    record.setPublishedDate(dto.publishedDate());
    record.setStatus(dto.status());
    record.setSource(dto.source());
    record.setKeywords(dto.keywords());
  }
}
