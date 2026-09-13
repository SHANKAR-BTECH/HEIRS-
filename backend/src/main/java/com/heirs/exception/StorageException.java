package com.heirs.exception;

public class StorageException extends RuntimeException {
  public StorageException(Throwable cause) {
    super("Document storage is unavailable. Please try again.", cause);
  }
}
