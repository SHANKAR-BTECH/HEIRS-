package com.heirs.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.heirs.TestRecords;
import com.heirs.dto.RecordSearchRequestDto;
import com.heirs.entity.*;
import com.heirs.entity.Record;
import com.heirs.exception.*;
import com.heirs.mapper.RecordMapper;
import com.heirs.repository.RecordRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class RecordServiceTest {
  @Mock RecordRepository repository;
  @Mock DocumentService documents;

  RecordService service() {
    return new RecordService(repository, new RecordMapper(), documents);
  }

  @Test
  void getsAllWithPagination() {
    when(repository.findAll(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(TestRecords.entity("REF/1"))));
    assertThat(service().getAllRecords(0, 20).content())
        .extracting("referenceNumber")
        .containsExactly("REF/1");
  }

  @Test
  void getsById() {
    when(repository.findById(1L)).thenReturn(Optional.of(TestRecords.entity("REF/1")));
    assertThat(service().getRecordById(1L).referenceNumber()).isEqualTo("REF/1");
  }

  @Test
  void missingRecordThrowsDomainException() {
    assertThatThrownBy(() -> service().getRecordById(42L))
        .isInstanceOf(RecordNotFoundException.class)
        .hasMessageContaining("42");
  }

  @Test
  void searchesThroughRepository() {
    when(repository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(Page.empty());
    assertThat(
            service()
                .searchRecords(
                    new RecordSearchRequestDto(
                        "digital", Category.POLICY, 2026, RecordStatus.ACTIVE, null),
                    0,
                    20)
                .content())
        .isEmpty();
    verify(repository).findAll(any(Specification.class), eq(PageRequest.of(0, 20, Sort.by("id"))));
  }

  @Test
  void createsAndMapsRecord() {
    when(repository.saveAndFlush(any(Record.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    assertThat(service().createRecord(TestRecords.create(" REF/1 ")).referenceNumber())
        .isEqualTo("REF/1");
  }

  @Test
  void rejectsDuplicateOnCreate() {
    when(repository.existsByReferenceNumber("REF/1")).thenReturn(true);
    assertThatThrownBy(() -> service().createRecord(TestRecords.create("REF/1")))
        .isInstanceOf(DuplicateReferenceNumberException.class);
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void rejectsDuplicateOnUpdate() {
    when(repository.findById(1L)).thenReturn(Optional.of(TestRecords.entity("REF/1")));
    when(repository.existsByReferenceNumberAndIdNot("REF/2", 1L)).thenReturn(true);
    assertThatThrownBy(() -> service().updateRecord(1L, TestRecords.update("REF/2")))
        .isInstanceOf(DuplicateReferenceNumberException.class);
  }

  @Test
  void updatesExistingReferenceAndClearsOptionalFields() {
    when(repository.findById(1L)).thenReturn(Optional.of(TestRecords.entity("REF/1")));
    when(repository.saveAndFlush(any(Record.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    var result = service().updateRecord(1L, TestRecords.update("REF/1"));
    assertThat(result.title()).isEqualTo("Revised Policy");
    assertThat(result.description()).isNull();
  }

  @Test
  void missingUpdateAndDeleteThrowNotFound() {
    assertThatThrownBy(() -> service().updateRecord(42L, TestRecords.update("REF/1")))
        .isInstanceOf(RecordNotFoundException.class);
    assertThatThrownBy(() -> service().deleteRecord(42L))
        .isInstanceOf(RecordNotFoundException.class);
  }

  @Test
  void deletesExistingRecord() {
    var record = TestRecords.entity("REF/1");
    when(repository.lockById(1L)).thenReturn(Optional.of(record));
    service().deleteRecord(1L);
    verify(repository).delete(record);
    verify(documents).deleteForRecord(1L);
  }

  @Test
  void returnsAllCategoryLabels() {
    assertThat(service().getCategories())
        .extracting("name")
        .containsExactly("Policy", "Scheme", "Regulation", "Project", "Rules");
  }
}
