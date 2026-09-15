package com.heirs.desktop.dto;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Backend error envelope matching ApiErrorDto.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public Map<String, String> safeFieldErrors() {
        return fieldErrors != null ? fieldErrors : Collections.emptyMap();
    }
}
