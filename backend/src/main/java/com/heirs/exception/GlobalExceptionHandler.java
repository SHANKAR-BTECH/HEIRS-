package com.heirs.exception;

import com.heirs.dto.ApiErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(DocumentNotFoundException.class)
  ResponseEntity<ApiErrorDto> documentMissing(
      DocumentNotFoundException ex, HttpServletRequest request) {
    return error(404, ex.getMessage(), request, Map.of());
  }

  @ExceptionHandler({
    InvalidDocumentException.class,
    org.springframework.web.multipart.support.MissingServletRequestPartException.class
  })
  ResponseEntity<ApiErrorDto> documentInvalid(Exception ex, HttpServletRequest request) {
    return error(400, ex.getMessage(), request, Map.of());
  }

  @ExceptionHandler({
    DocumentTooLargeException.class,
    org.springframework.web.multipart.MaxUploadSizeExceededException.class
  })
  ResponseEntity<ApiErrorDto> documentTooLarge(Exception ex, HttpServletRequest request) {
    return error(
        413,
        ex instanceof DocumentTooLargeException
            ? ex.getMessage()
            : "Upload exceeds the configured file or request size limit",
        request,
        Map.of());
  }

  @ExceptionHandler(StorageException.class)
  ResponseEntity<ApiErrorDto> storage(StorageException ex, HttpServletRequest request) {
    log.error("Document storage failure", ex);
    return error(500, ex.getMessage(), request, Map.of());
  }

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(RecordNotFoundException.class)
  ResponseEntity<ApiErrorDto> notFound(RecordNotFoundException ex, HttpServletRequest request) {
    return error(404, ex.getMessage(), request, Map.of());
  }

  @ExceptionHandler(DuplicateReferenceNumberException.class)
  ResponseEntity<ApiErrorDto> duplicate(
      DuplicateReferenceNumberException ex, HttpServletRequest request) {
    return error(409, ex.getMessage(), request, Map.of());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiErrorDto> integrity(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    // The database is the final authority, including concurrent duplicate creates/updates.
    String detail = ex.getMostSpecificCause().getMessage();
    String message =
        detail != null && detail.toLowerCase(java.util.Locale.ROOT).contains("uk_records_reference")
            ? "Reference number already exists"
            : "The record conflicts with a database constraint";
    return error(409, message, request, Map.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiErrorDto> validation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    Map<String, String> fields = new LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(
            e ->
                fields.putIfAbsent(
                    e.getField(), e.isBindingFailure() ? "Invalid value" : e.getDefaultMessage()));
    return error(400, "Request validation failed", request, fields);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiErrorDto> constraint(
      ConstraintViolationException ex, HttpServletRequest request) {
    Map<String, String> fields = new LinkedHashMap<>();
    ex.getConstraintViolations()
        .forEach(v -> fields.put(v.getPropertyPath().toString(), v.getMessage()));
    return error(400, "Request validation failed", request, fields);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  ResponseEntity<ApiErrorDto> parameters(
      HandlerMethodValidationException ex, HttpServletRequest request) {
    Map<String, String> fields = new LinkedHashMap<>();
    ex.getParameterValidationResults()
        .forEach(
            r ->
                r.getResolvableErrors()
                    .forEach(
                        e ->
                            fields.put(
                                String.valueOf(r.getMethodParameter().getParameterName()),
                                e.getDefaultMessage())));
    return error(400, "Request validation failed", request, fields);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<ApiErrorDto> malformed(Exception ex, HttpServletRequest request) {
    return error(
        400,
        "Malformed request: check JSON fields, enum values, numbers and ISO dates",
        request,
        Map.of());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ApiErrorDto> route(NoResourceFoundException ex, HttpServletRequest request) {
    return error(404, "Endpoint was not found", request, Map.of());
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ApiErrorDto> method(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    return error(405, "HTTP method is not supported", request, Map.of());
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ApiErrorDto> media(
      HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
    return error(415, "Unsupported Content-Type for this endpoint", request, Map.of());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiErrorDto> unexpected(Exception ex, HttpServletRequest request) {
    log.error("Unexpected error for {} {}", request.getMethod(), request.getRequestURI(), ex);
    return error(500, "An unexpected server error occurred", request, Map.of());
  }

  private ResponseEntity<ApiErrorDto> error(
      int status, String message, HttpServletRequest request, Map<String, String> fields) {
    return ResponseEntity.status(status)
        .body(
            new ApiErrorDto(
                Instant.now(),
                status,
                HttpStatus.valueOf(status).getReasonPhrase(),
                message,
                request.getRequestURI(),
                fields));
  }
}
