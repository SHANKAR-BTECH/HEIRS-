package com.heirs.desktop.model;

import java.util.LinkedHashMap;
import java.util.Map;
import com.heirs.desktop.dto.RecordRequest;
import com.heirs.desktop.api.ApiException;

public final class RecordValidation {
    private RecordValidation() {}
    public static Map<String, String> validate(RecordRequest r) {
        var errors = new LinkedHashMap<String, String>();
        required(errors, "title", r.title(), "Title is required");
        required(errors, "referenceNumber", r.referenceNumber(), "Reference number is required");
        required(errors, "category", r.category(), "Category is required");
        required(errors, "department", r.department(), "Department is required");
        required(errors, "status", r.status(), "Status is required");
        if (r.publicationYear() != null && (r.publicationYear() < 1900 || r.publicationYear() > 2100))
            errors.put("publicationYear", "Use a year from 1900 to 2100, or leave blank");
        return errors;
    }
    private static void required(Map<String, String> errors, String field, String value, String message) {
        if (value == null || value.isBlank()) errors.put(field, message);
    }
    public static Map<String, String> backendFields(ApiException error) {
        var fields = new LinkedHashMap<String, String>();
        error.getFieldErrors().forEach((key, value) -> fields.put(key.substring(key.lastIndexOf('.') + 1), value));
        // The backend's duplicate-reference 409 has a message and an empty fieldErrors map.
        if (error.getStatusCode() == 409 && error.getUserMessage().toLowerCase(java.util.Locale.ROOT).contains("reference"))
            fields.putIfAbsent("referenceNumber", "Reference number already exists");
        return fields;
    }
}
