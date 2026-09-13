package com.heirs.dto;

import java.time.Instant;
import java.util.Map;

public record ApiErrorDto(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String, String> fieldErrors) {}
