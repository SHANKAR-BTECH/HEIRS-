package com.heirs.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
    name = "heirs_documents",
    indexes = @Index(name = "idx_documents_record", columnList = "record_id"))
public class Document {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "record_id", nullable = false)
  private Record record;

  @Column(nullable = false, length = 180)
  private String originalFileName;

  @Column(nullable = false, unique = true, length = 80)
  private String storageKey;

  @Column(nullable = false)
  private String contentType;

  @Column(nullable = false)
  private Long fileSize;

  @Column(nullable = false, updatable = false)
  private LocalDateTime uploadedAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  public Document() {}

  public Document(Record record) {
    this.record = record;
  }

  @PrePersist
  void create() {
    uploadedAt = now();
    updatedAt = uploadedAt;
  }

  @PreUpdate
  void update() {
    updatedAt = now();
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
  }

  public void setFile(String name, String key, long size) {
    originalFileName = name;
    storageKey = key;
    fileSize = size;
    contentType = "application/pdf";
  }

  public Long getId() {
    return id;
  }

  public Record getRecord() {
    return record;
  }

  public String getOriginalFileName() {
    return originalFileName;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public String getContentType() {
    return contentType;
  }

  public Long getFileSize() {
    return fileSize;
  }

  public LocalDateTime getUploadedAt() {
    return uploadedAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
