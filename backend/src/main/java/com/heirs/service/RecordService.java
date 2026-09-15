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
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class RecordService {
  private static final Map<String, String> SORT_FIELDS =
      Map.of(
          "id", "id",
          "title", "title",
          "reference", "referenceNumber",
          "referenceNumber", "referenceNumber",
          "category", "category",
          "department", "department",
          "year", "publicationYear",
          "publicationYear", "publicationYear",
          "status", "status");

  private static final Map<String, Sort.Direction> SORT_DIRECTIONS =
      Map.of("asc", Sort.Direction.ASC, "desc", Sort.Direction.DESC);

  private final RecordRepository repository;
  private final RecordMapper mapper;
  private final DocumentService documents;

  public RecordService(
      RecordRepository repository, RecordMapper mapper, DocumentService documents) {
    this.repository = repository;
    this.mapper = mapper;
    this.documents = documents;
  }

  public PageResponseDto<RecordResponseDto> getAllRecords(
      @Min(0) int page, @Min(1) @Max(100) int size) {
    return getAllRecords(page, size, "id", "asc");
  }

  public PageResponseDto<RecordResponseDto> getAllRecords(
      @Min(0) int page, @Min(1) @Max(100) int size,
      @Size(max = 40) String sortBy, @Size(max = 10) String sortDirection) {
    return PageResponseDto.from(
        repository.findAll(pageRequest(page, size, sortBy, sortDirection)).map(mapper::toDto));
  }

  public RecordResponseDto getRecordById(@Positive Long id) {
    return mapper.toDto(requireRecord(id));
  }

  public PageResponseDto<RecordResponseDto> searchRecords(
      @NotNull @Valid RecordSearchRequestDto filter, @Min(0) int page, @Min(1) @Max(100) int size) {
    return searchRecords(filter, page, size, "id", "asc");
  }

  public PageResponseDto<RecordResponseDto> searchRecords(
      @NotNull @Valid RecordSearchRequestDto filter,
      @Min(0) int page,
      @Min(1) @Max(100) int size,
      @Size(max = 40) String sortBy,
      @Size(max = 10) String sortDirection) {
    return PageResponseDto.from(
        repository
            .findAll(
                RecordSpecifications.matching(filter), pageRequest(page, size, sortBy, sortDirection))
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
    Record record = repository
        .lockById(id)
        .orElseThrow(() -> new RecordNotFoundException(id));
    documents.deleteForRecord(id);
    repository.delete(record);
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

  private PageRequest pageRequest(
      int page, int size, String sortBy, String sortDirection) {
    String field = SORT_FIELDS.get(sortBy);
    if (field == null) throw new IllegalArgumentException("Unknown sort field: " + sortBy);
    Sort.Direction direction = SORT_DIRECTIONS.get(sortDirection.toLowerCase(Locale.ROOT));
    if (direction == null) throw new IllegalArgumentException("Unknown sort direction: " + sortDirection);
    return PageRequest.of(page, size, Sort.by(direction, field));
  }
}
