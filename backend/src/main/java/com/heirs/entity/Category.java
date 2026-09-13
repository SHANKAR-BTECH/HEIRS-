package com.heirs.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum Category {
  POLICY("Policy"),
  SCHEME("Scheme"),
  REGULATION("Regulation"),
  PROJECT("Project"),
  RULES("Rules");
  private final String label;

  Category(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  @JsonCreator
  public static Category fromValue(String value) {
    if (value == null) return null;
    return Arrays.stream(values())
        .filter(
            v -> v.label.equalsIgnoreCase(value.trim()) || v.name().equalsIgnoreCase(value.trim()))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Invalid Category. Allowed values: Policy, Scheme, Regulation, Project,"
                        + " Rules"));
  }
}
