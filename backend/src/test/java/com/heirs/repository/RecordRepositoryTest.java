package com.heirs.repository;

import static org.assertj.core.api.Assertions.*;

import com.heirs.TestRecords;
import com.heirs.dto.RecordSearchRequestDto;
import com.heirs.entity.*;
import com.heirs.entity.Record;
import com.heirs.specification.RecordSpecifications;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles(resolver = com.heirs.TestDatabaseProfileResolver.class)
class RecordRepositoryTest {
  @Autowired RecordRepository repository;

  @BeforeEach
  void prepare() {
    var policy = TestRecords.entity("HEIRS/POL/2026/014");
    policy.setDescription("Remote campus access");
    policy.setKeywords("infrastructure");
    var scheme = TestRecords.entity("SCH/2024/001");
    scheme.setTitle("Student Aid");
    scheme.setCategory(Category.SCHEME);
    scheme.setStatus(RecordStatus.DRAFT);
    scheme.setPublicationYear(2024);
    scheme.setDepartment("Student Welfare");
    scheme.setKeywords(null);
    scheme.setDescription(null);
    var rule = TestRecords.entity("RULE/2025/001");
    rule.setTitle("100% access_rule!");
    rule.setCategory(Category.RULES);
    rule.setStatus(RecordStatus.ARCHIVED);
    rule.setPublicationYear(2025);
    rule.setDepartment("Administration");
    repository.saveAllAndFlush(List.of(policy, scheme, rule));
  }

  List<Record> search(String q, Category c, Integer year, RecordStatus status, String dept) {
    return repository.findAll(
        RecordSpecifications.matching(new RecordSearchRequestDto(q, c, year, status, dept)));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"DIGITAL", "remote campus", "infrastructure", "higher education", "POL/2026/014"})
  void keywordSearchesEachFieldIgnoringCase(String query) {
    assertThat(search(query, Category.POLICY, null, null, null))
        .extracting(Record::getReferenceNumber)
        .containsExactly("HEIRS/POL/2026/014");
  }

  @Test
  void categoryFilter() {
    assertThat(search(null, Category.SCHEME, null, null, null))
        .extracting(Record::getTitle)
        .containsExactly("Student Aid");
  }

  @Test
  void statusFilter() {
    assertThat(search(null, null, null, RecordStatus.DRAFT, null)).hasSize(1);
  }

  @Test
  void yearFilter() {
    assertThat(search(null, null, 2024, null, null)).hasSize(1);
  }

  @Test
  void departmentFilter() {
    assertThat(search(null, null, null, null, " student welfare ")).hasSize(1);
  }

  @Test
  void combinedFiltersUseAnd() {
    assertThat(
            search(
                "digital",
                Category.POLICY,
                2026,
                RecordStatus.ACTIVE,
                "Higher Education Department"))
        .hasSize(1);
    assertThat(search("digital", Category.POLICY, 2024, RecordStatus.ACTIVE, null)).isEmpty();
  }

  @Test
  void emptyFiltersReturnAll() {
    assertThat(search("  ", null, null, null, " ")).hasSize(3);
  }

  @ParameterizedTest
  @ValueSource(strings = {"%", "_", "!"})
  void wildcardsAreLiteral(String query) {
    assertThat(search(query, null, null, null, null))
        .extracting(Record::getTitle)
        .containsExactly("100% access_rule!");
  }

  @Test
  void databaseEnforcesUniqueReference() {
    assertThatThrownBy(() -> repository.saveAndFlush(TestRecords.entity("HEIRS/POL/2026/014")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void lookupAndAuditTimestamps() {
    var record = repository.findByReferenceNumber("HEIRS/POL/2026/014").orElseThrow();
    assertThat(record.getCreatedAt()).isNotNull();
    assertThat(record.getUpdatedAt()).isNotNull();
    var created = record.getCreatedAt();
    record.setTitle("Updated");
    repository.flush();
    assertThat(record.getCreatedAt()).isEqualTo(created);
    assertThat(record.getUpdatedAt()).isAfterOrEqualTo(created);
  }
}
