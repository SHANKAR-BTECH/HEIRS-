package com.heirs.desktop;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.heirs.desktop.model.Record;
import com.heirs.desktop.model.report.ReportCountRow;
import com.heirs.desktop.model.report.ReportData;
import com.heirs.desktop.model.report.YearReportRow;
import com.heirs.desktop.service.RecordService;
import com.heirs.desktop.service.ReportService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live reconciliation: ReportService output must match independent
 * RecordService + DocumentsApi scans against the real backend at 127.0.0.1:8080.
 *
 * <p>Skipped automatically unless {@code -Dheirs.live=true}.
 */
@EnabledIfSystemProperty(named = "heirs.live", matches = "true")
class J6LiveTest {

    @Test
    void reportServiceMatchesLiveRepository() throws Exception {
        // 1. Independent fetch
        RecordService recordService = new RecordService();
        ReportService reportService = new ReportService();
        List<Record> records = recordService.fetchCatalog().safeContent();
        ReportData report = reportService.generateReports();

        // 2. Total records must match
        assertEquals(records.size(), report.totalRecords(),
                "Report total records must equal live fetchCatalog size");
        assertTrue(report.totalRecords() > 0, "Live backend should have at least 1 record");

        // 3. Categories: count must match independent group-by
        Map<String, Long> expectedCategories = records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCategory() == null || r.getCategory().isBlank()
                                ? "Uncategorized" : r.getCategory().trim(),
                        Collectors.counting()));
        assertEquals(expectedCategories.size(), report.categories().size(),
                "Number of distinct categories must match");
        for (ReportCountRow row : report.categories()) {
            long expected = expectedCategories.getOrDefault(row.name(), 0L);
            assertEquals(expected, row.count(),
                    "Category '" + row.name() + "' count must match independent scan");
        }

        // 4. Statuses
        Map<String, Long> expectedStatuses = records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getStatus() == null || r.getStatus().isBlank()
                                ? "Unknown" : r.getStatus().trim(),
                        Collectors.counting()));
        assertEquals(expectedStatuses.size(), report.statuses().size());
        for (ReportCountRow row : report.statuses()) {
            long expected = expectedStatuses.getOrDefault(row.name(), 0L);
            assertEquals(expected, row.count(),
                    "Status '" + row.name() + "' count must match independent scan");
        }

        // 5. Years
        Map<Integer, Long> expectedYears = records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getYear() != null && r.getYear() > 0 ? r.getYear() : 0,
                        Collectors.counting()));
        assertEquals(expectedYears.size(), report.years().size());
        for (YearReportRow row : report.years()) {
            long expected = expectedYears.getOrDefault(row.year(), 0L);
            assertEquals(expected, row.count(),
                    "Year '" + row.yearText() + "' count must match independent scan");
        }

        // 6. Departments (distinct non-blank)
        long expectedDeptCount = records.stream()
                .map(r -> r.getDepartment() == null ? "" : r.getDepartment().trim())
                .filter(d -> !d.isBlank())
                .distinct()
                .count();
        assertEquals(expectedDeptCount, report.departments().size(),
                "Department count must match distinct non-blank departments");
        for (ReportCountRow row : report.departments()) {
            long expected = records.stream()
                    .filter(r -> row.name().equals(
                            r.getDepartment() == null ? "" : r.getDepartment().trim()))
                    .count();
            assertEquals(expected, row.count(),
                    "Department '" + row.name() + "' count must match");
        }

        // 7. Documents loaded (should succeed with real backend)
        assertTrue(report.documentsLoaded(),
                "Documents should load successfully against live backend");

        // 8. Document count consistency
        long totalDocsFromRows = report.documents().stream()
                .mapToLong(r -> r.documentCount())
                .sum();
        assertEquals(report.supportedReportTotalDocuments(), totalDocsFromRows,
                "supportedReportTotalDocuments must equal sum of document row counts");

        System.out.println("J6 VERIFIED: " + report.totalRecords() + " records, "
                + report.categories().size() + " categories, "
                + report.statuses().size() + " statuses, "
                + report.years().size() + " year groups, "
                + report.departments().size() + " departments, "
                + report.supportedReportTotalDocuments() + " documents");
    }
}
