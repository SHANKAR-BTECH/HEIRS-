package com.heirs.desktop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Health check response matching GET /api/health.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HealthResponse(
        String status,
        String database
) {
    public boolean isHealthy() {
        return "UP".equalsIgnoreCase(status) && "UP".equalsIgnoreCase(database);
    }
}
