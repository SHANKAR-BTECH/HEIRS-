package com.heirs.entity;

import jakarta.persistence.*;

/** Durable work item committed in the same transaction as metadata removal. */
@Entity
@Table(name = "heirs_file_cleanup")
public class FileCleanup {
  @Id
  @Column(length = 80)
  private String storageKey;

  public FileCleanup() {}

  public FileCleanup(String storageKey) {
    this.storageKey = storageKey;
  }

  public String getStorageKey() {
    return storageKey;
  }
}
