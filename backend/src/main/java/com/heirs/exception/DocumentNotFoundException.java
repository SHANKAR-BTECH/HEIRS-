package com.heirs.exception;

public class DocumentNotFoundException extends RuntimeException {
  public DocumentNotFoundException(Long id) {
    super("Document " + id + " was not found");
  }
}
