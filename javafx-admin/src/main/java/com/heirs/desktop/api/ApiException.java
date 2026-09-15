package com.heirs.desktop.api;

import java.util.Collections;
import java.util.Map;

import com.heirs.desktop.dto.ApiErrorResponse;

/**
 * Structured exception thrown by ApiClient and API operations.
 */
public class ApiException extends RuntimeException {

    private final int statusCode;
    private final String userMessage;
    private final String backendMessage;
    private final Map<String, String> fieldErrors;
    private final boolean offline;

    public ApiException(String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.statusCode = -1;
        this.userMessage = userMessage;
        this.backendMessage = cause != null ? cause.getMessage() : null;
        this.fieldErrors = Collections.emptyMap();
        this.offline = true;
    }

    public ApiException(int statusCode, String userMessage, String backendMessage, Map<String, String> fieldErrors) {
        super(userMessage);
        this.statusCode = statusCode;
        this.userMessage = userMessage;
        this.backendMessage = backendMessage;
        this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.emptyMap();
        this.offline = false;
    }

    public static ApiException offline(String userMessage, Throwable cause) {
        return new ApiException(userMessage, cause);
    }

    public static ApiException clientError(String userMessage, Throwable cause) {
        ApiException error = new ApiException(0, userMessage, null, Collections.emptyMap());
        error.initCause(cause);
        return error;
    }

    public static ApiException fromErrorResponse(int statusCode, ApiErrorResponse error) {
        String msg;
        if (error != null && error.message() != null && !error.message().isBlank()) {
            msg = error.message();
        } else {
            msg = "HTTP " + statusCode + " error from backend service.";
        }
        Map<String, String> fields = error != null ? error.safeFieldErrors() : Collections.emptyMap();
        return new ApiException(statusCode, msg, error != null ? error.error() : null, fields);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getBackendMessage() {
        return backendMessage;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public boolean isOffline() {
        return offline;
    }
}
