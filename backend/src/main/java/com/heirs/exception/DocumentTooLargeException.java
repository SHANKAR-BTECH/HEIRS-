package com.heirs.exception;

public class DocumentTooLargeException extends RuntimeException {
  public DocumentTooLargeException(String message) {
    super(message);
  }
}
