package com.heirs.exception;

public class RecordNotFoundException extends RuntimeException {
  public RecordNotFoundException(Long id) {
    super("Record with id " + id + " was not found");
  }
}
