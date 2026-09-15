package com.heirs.desktop.model.report;

/**
 * One row of the supporting-document report: a single record and its attached
 * document metadata aggregated from the backend document API.
 */
public record DocumentReportRow(
        String reference,
        String title,
        long documentCount,
        long totalSizeBytes
) {
}