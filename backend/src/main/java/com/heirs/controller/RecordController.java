package com.heirs.controller;

import com.heirs.dto.*;
import com.heirs.service.RecordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/records")
public class RecordController {
  private final RecordService service;

  public RecordController(RecordService service) {
    this.service = service;
  }

  @GetMapping
  public PageResponseDto<RecordResponseDto> getAll(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.getAllRecords(page, size);
  }

  @GetMapping("/{id}")
  public RecordResponseDto getById(@PathVariable @Positive Long id) {
    return service.getRecordById(id);
  }

  @GetMapping("/search")
  public PageResponseDto<RecordResponseDto> search(
      @Valid @ModelAttribute RecordSearchRequestDto filter,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.searchRecords(filter, page, size);
  }

  @PostMapping
  public ResponseEntity<RecordResponseDto> create(
      @Valid @RequestBody CreateRecordRequestDto request) {
    RecordResponseDto result = service.createRecord(request);
    return ResponseEntity.created(
            ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.id())
                .toUri())
        .body(result);
  }

  @PutMapping("/{id}")
  public RecordResponseDto update(
      @PathVariable @Positive Long id, @Valid @RequestBody UpdateRecordRequestDto request) {
    return service.updateRecord(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
    service.deleteRecord(id);
    return ResponseEntity.noContent().build();
  }
}
