package com.heirs.exception;

public class DuplicateReferenceNumberException extends RuntimeException {
  public DuplicateReferenceNumberException(String referenceNumber) {
    super("Reference number already exists: " + referenceNumber);
  }
}
