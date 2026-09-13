package com.heirs.service;

import com.heirs.dto.*;
import com.heirs.entity.Category;
import com.heirs.entity.Record;
import com.heirs.exception.*;
import com.heirs.mapper.RecordMapper;
import com.heirs.repository.RecordRepository;
import com.heirs.specification.RecordSpecifications;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class RecordService {
  private final RecordRepository repository;
  private final RecordMapper mapper;

  public RecordService(RecordRepository repository, RecordMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  public PageResponseDto<RecordResponseDto> getAllRecords(
      @Min(0) int page, @Min(1) @Max(100) int size) {
    return PageResponseDto.from(repository.findAll(pageRequest(page, size)).map(mapper::toDto));
  }

  public RecordResponseDto getRecordById(@Positive Long id) {
    return mapper.toDto(requireRecord(id));
  }

  public PageResponseDto<RecordResponseDto> searchRecords(
      @NotNull @Valid RecordSearchRequestDto filter, @Min(0) int page, @Min(1) @Max(100) int size) {
    return PageResponseDto.from(
        repository
            .findAll(RecordSpecifications.matching(filter), pageRequest(page, size))
            .map(mapper::toDto));
  }

  @Transactional
  public RecordResponseDto createRecord(@NotNull @Valid CreateRecordRequestDto request) {
    String reference = request.referenceNumber().trim();
    if (repository.existsByReferenceNumber(reference))
      throw new DuplicateReferenceNumberException(reference);
    return mapper.toDto(repository.saveAndFlush(mapper.toEntity(request)));
  }

  @Transactional
  public RecordResponseDto updateRecord(
      @Positive Long id, @NotNull @Valid UpdateRecordRequestDto request) {
    Record record = requireRecord(id);
    String reference = request.referenceNumber().trim();
    if (repository.existsByReferenceNumberAndIdNot(reference, id))
      throw new DuplicateReferenceNumberException(reference);
    mapper.update(record, request);
    return mapper.toDto(repository.saveAndFlush(record));
  }

  @Transactional
  public void deleteRecord(@Positive Long id) {
    repository.delete(requireRecord(id));
    repository.flush();
  }

  public List<CategoryResponseDto> getCategories() {
    return Arrays.stream(Category.values())
        .map(c -> new CategoryResponseDto(c.name(), c.getLabel()))
        .toList();
  }

  private Record requireRecord(Long id) {
    return repository.findById(id).orElseThrow(() -> new RecordNotFoundException(id));
  }

  private PageRequest pageRequest(int page, int size) {
    return PageRequest.of(page, size, Sort.by("id").ascending());
  }
}
