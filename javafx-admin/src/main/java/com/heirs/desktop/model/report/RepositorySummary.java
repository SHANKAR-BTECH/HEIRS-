package com.heirs.desktop.model.report;

import java.util.Map;

/**
 * Immutable repository-wide summary derived from real backend records.
 */
public record RepositorySummary(
        long totalRecords,
        Map<String, Long> categoryCounts,
        Map<String, Long> statusCounts,
        long departmentsRepresented,
        long yearsRepresented,
        long totalDocuments,
        long recordsWithDocuments,
        long recordsWithoutDocuments
) {

    public long categoryCount(String category) {
        return categoryCounts.getOrDefault(category, 0L);
    }

    public long statusCount(String status) {
        return statusCounts.getOrDefault(status, 0L);
    }
}