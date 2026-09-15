package com.heirs.desktop.model.report;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Complete, immutable snapshot of every report generated from one load of the
 * real backend repository. A single object is handed to the controller so all
 * tabs share one consistent dataset and generation timestamp.
 */
public record ReportData(
        RepositorySummary summary,
        List<ReportCountRow> categories,
        List<ReportCountRow> statuses,
        List<YearReportRow> years,
        List<ReportCountRow> departments,
        List<DocumentReportRow> documents,
        boolean documentsLoaded,
        long supportedReportTotalDocuments,
        long supportedReportRecordsWithDocs,
        long supportedReportRecordsWithoutDocs,
        double averageDocumentsPerRecord,
        long largestDocumentCount,
        long totalStoredFileBytes,
        LocalDateTime generatedAt
) {
    public long totalRecords() {
        return summary.totalRecords();
    }
}