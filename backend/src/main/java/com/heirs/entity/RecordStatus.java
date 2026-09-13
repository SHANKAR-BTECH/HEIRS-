package com.heirs.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum RecordStatus {
  ACTIVE("Active"),
  DRAFT("Draft"),
  COMPLETED("Completed"),
  ARCHIVED("Archived");
  private final String label;

  RecordStatus(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  @JsonCreator
  public static RecordStatus fromValue(String value) {
    if (value == null) return null;
    return Arrays.stream(values())
        .filter(
            v -> v.label.equalsIgnoreCase(value.trim()) || v.name().equalsIgnoreCase(value.trim()))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Invalid RecordStatus. Allowed values: Active, Draft, Completed, Archived"));
  }
}
