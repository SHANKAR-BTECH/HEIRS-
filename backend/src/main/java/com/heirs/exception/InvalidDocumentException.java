package com.heirs.exception;

public class InvalidDocumentException extends RuntimeException {
  public InvalidDocumentException(String message) {
    super(message);
  }
}
