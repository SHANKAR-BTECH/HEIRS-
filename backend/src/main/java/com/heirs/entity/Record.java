package com.heirs.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
    name = "heirs_records",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_records_reference", columnNames = "referenceNumber"),
    indexes = {
      @Index(name = "idx_records_category", columnList = "category"),
      @Index(name = "idx_records_status", columnList = "status"),
      @Index(name = "idx_records_year", columnList = "publicationYear"),
      @Index(name = "idx_records_department", columnList = "department")
    })
public class Record {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Title is required")
  @Size(max = 255)
  @Column(nullable = false, length = 255)
  private String title;

  @Size(max = 20000)
  @Column(columnDefinition = "TEXT")
  private String description;

  @NotNull(message = "Category is required")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Category category;

  @NotBlank(message = "Department is required")
  @Size(max = 150)
  @Column(nullable = false, length = 150)
  private String department;

  @NotBlank(message = "Reference number is required")
  @Size(max = 100)
  @Column(nullable = false, length = 100)
  private String referenceNumber;

  @Min(1900)
  @Max(2100)
  @Column
  private Integer publicationYear;

  @Column private LocalDate publishedDate;

  @NotNull(message = "Status is required")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RecordStatus status;

  @Size(max = 500)
  @Column(length = 500)
  private String source;

  @Size(max = 2000)
  @Column(length = 2000)
  private String keywords;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  public Record() {}

  @PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now(ZoneOffset.UTC);
    updatedAt = createdAt;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now(ZoneOffset.UTC);
  }

  public Long getId() {
    return id;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Category getCategory() {
    return category;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  public String getDepartment() {
    return department;
  }

  public void setDepartment(String department) {
    this.department = department;
  }

  public String getReferenceNumber() {
    return referenceNumber;
  }

  public void setReferenceNumber(String referenceNumber) {
    this.referenceNumber = referenceNumber;
  }

  public Integer getPublicationYear() {
    return publicationYear;
  }

  public void setPublicationYear(Integer publicationYear) {
    this.publicationYear = publicationYear;
  }

  public LocalDate getPublishedDate() {
    return publishedDate;
  }

  public void setPublishedDate(LocalDate publishedDate) {
    this.publishedDate = publishedDate;
  }

  public RecordStatus getStatus() {
    return status;
  }

  public void setStatus(RecordStatus status) {
    this.status = status;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getKeywords() {
    return keywords;
  }

  public void setKeywords(String keywords) {
    this.keywords = keywords;
  }
}
