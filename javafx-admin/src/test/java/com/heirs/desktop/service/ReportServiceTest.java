package com.heirs.desktop.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.heirs.desktop.dto.DocumentResponse;
import com.heirs.desktop.model.Record;
import com.heirs.desktop.model.report.ReportCountRow;
import com.heirs.desktop.model.report.ReportData;
import com.heirs.desktop.model.report.YearReportRow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {

    private static Record rec(long id, String category, String status, String dept, Integer year) {
        return new Record(id, "REF-" + id, "Title " + id, category, dept, year,
                null, status, "", "", "");
    }

    @Test
    void emptyRecordsProducesEmptyReport() {
        ReportData data = ReportService.assemble(List.of(), Map.of(), true);
        assertEquals(0, data.totalRecords());
        assertTrue(data.categories().isEmpty());
        assertTrue(data.statuses().isEmpty());
        assertTrue(data.years().isEmpty());
        assertTrue(data.departments().isEmpty());
        assertTrue(data.documents().isEmpty());
        assertTrue(data.documentsLoaded());
        assertEquals(0, data.supportedReportTotalDocuments());
        assertEquals(0, data.supportedReportRecordsWithDocs());
        assertEquals(0, data.supportedReportRecordsWithoutDocs());
        assertEquals(0.0, data.averageDocumentsPerRecord());
        assertEquals(0, data.largestDocumentCount());
        assertEquals(0, data.totalStoredFileBytes());
        assertNotNull(data.generatedAt());
    }

    @Test
    void singleRecordProducesOneForEach() {
        ReportData data = ReportService.assemble(
                List.of(rec(1, "Policy", "Active", "Dept A", 2024)),
                Map.of(), true);
        assertEquals(1, data.totalRecords());
        assertEquals(1, data.categories().size());
        assertEquals("Policy", data.categories().get(0).name());
        assertEquals(1, data.categories().get(0).count());
        assertEquals("100.0%", data.categories().get(0).percentageText());
        assertEquals(1, data.statuses().size());
        assertEquals("Active", data.statuses().get(0).name());
        assertEquals(1, data.years().size());
        assertEquals("2024", data.years().get(0).yearText());
        assertEquals(1, data.departments().size());
        assertEquals("Dept A", data.departments().get(0).name());
    }

    @Test
    void multipleRecordsCountAndSortCorrectly() {
        ReportData data = ReportService.assemble(
                List.of(
                        rec(1, "Policy", "Active", "Dept A", 2024),
                        rec(2, "Policy", "Active", "Dept A", 2024),
                        rec(3, "Policy", "Active", "Dept B", 2023),
                        rec(4, "Scheme", "Draft", "Dept B", 2023)
                ),
                Map.of(), true);
        assertEquals(4, data.totalRecords());
        // Categories sorted count desc then name: Policy(3) then Scheme(1)
        assertEquals(2, data.categories().size());
        assertEquals("Policy", data.categories().get(0).name());
        assertEquals(3, data.categories().get(0).count());
        assertEquals("Scheme", data.categories().get(1).name());
        assertEquals(1, data.categories().get(1).count());
        // Statuses: Active(3) then Draft(1)
        assertEquals(2, data.statuses().size());
        assertEquals("Active", data.statuses().get(0).name());
        assertEquals(3, data.statuses().get(0).count());
        // Years sorted newest first: 2024(2) then 2023(2)
        assertEquals(2, data.years().size());
        assertEquals(2024, data.years().get(0).year());
        assertEquals(2023, data.years().get(1).year());
        // Departments: Dept A(2), Dept B(2) → sorted name since same count
        assertEquals(2, data.departments().size());
        assertEquals("Dept A", data.departments().get(0).name());
        assertEquals(2, data.departments().get(0).count());
    }

    @Test
    void blankCategoryNormalizedToUncategorized() {
        ReportData data = ReportService.assemble(
                List.of(rec(1, "", "Active", "Dept", 2024),
                        rec(2, "  ", "Active", "Dept", 2024)),
                Map.of(), true);
        assertEquals(1, data.categories().size());
        assertEquals("Uncategorized", data.categories().get(0).name());
        assertEquals(2, data.categories().get(0).count());
    }

    @Test
    void blankStatusNormalizedToUnknown() {
        ReportData data = ReportService.assemble(
                List.of(rec(1, "Policy", "", "Dept", 2024),
                        rec(2, "Policy", null, "Dept", 2024)),
                Map.of(), true);
        assertEquals(1, data.statuses().size());
        assertEquals("Unknown", data.statuses().get(0).name());
    }

    @Test
    void blankDepartmentRowStillAppears() {
        ReportData data = ReportService.assemble(
                List.of(rec(1, "Policy", "Active", "", 2024),
                        rec(2, "Policy", "Active", null, 2024)),
                Map.of(), true);
        // buildDepartmentRows groups blank as "" — 1 row in the table
        assertEquals(1, data.departments().size());
        assertEquals("", data.departments().get(0).name());
        assertEquals(2, data.departments().get(0).count());
        // But summary excludes blank from distinct count
        assertEquals(0, data.summary().departmentsRepresented());
    }

    @Test
    void zeroYearGroupedAsUnknown() {
        ReportData data = ReportService.assemble(
                List.of(rec(1, "Policy", "Active", "Dept", 0),
                        rec(2, "Policy", "Active", "Dept", null)),
                Map.of(), true);
        assertEquals(1, data.years().size());
        assertEquals("Unknown", data.years().get(0).yearText());
        assertEquals(2, data.years().get(0).count());
        assertEquals(0, data.summary().yearsRepresented());
    }

    @Test
    void zeroRecordsGivesZeroPercentages() {
        ReportData data = ReportService.assemble(List.of(), Map.of(), true);
        // No rows → no percentages to check; just verify no division by zero crash
        assertTrue(data.categories().isEmpty());
        assertTrue(data.statuses().isEmpty());
    }

    @Test
    void documentStatsAggregatedCorrectly() {
        DocumentResponse doc1 = new DocumentResponse(1L, 1L, "a.pdf", "application/pdf", 1000L, null, null);
        DocumentResponse doc2 = new DocumentResponse(2L, 1L, "b.pdf", "application/pdf", 500L, null, null);
        DocumentResponse doc3 = new DocumentResponse(3L, 3L, "c.pdf", "application/pdf", 2000L, null, null);
        Map<Long, List<DocumentResponse>> docs = Map.of(
                1L, List.of(doc1, doc2),
                3L, List.of(doc3));
        ReportData data = ReportService.assemble(
                List.of(
                        rec(1, "Policy", "Active", "Dept", 2024),
                        rec(2, "Scheme", "Draft", "Dept", 2024),
                        rec(3, "Policy", "Active", "Dept", 2023)),
                docs, true);
        assertEquals(3, data.supportedReportTotalDocuments());
        assertEquals(2, data.supportedReportRecordsWithDocs());
        assertEquals(1, data.supportedReportRecordsWithoutDocs());
        assertEquals(3500, data.totalStoredFileBytes());
        assertEquals(2, data.largestDocumentCount());
        assertEquals(1.0, data.averageDocumentsPerRecord(), 0.001);
        assertTrue(data.documentsLoaded());
    }

    @Test
    void documentPartialFailureSetsLoadedFalse() {
        ReportData data = ReportService.assemble(
                List.of(rec(1, "Policy", "Active", "Dept", 2024)),
                Map.of(), false);
        assertFalse(data.documentsLoaded());
    }

    @Test
    void documentRowSortedByCountDescThenReference() {
        DocumentResponse doc1 = new DocumentResponse(1L, 1L, "a.pdf", "application/pdf", 1000L, null, null);
        DocumentResponse doc2 = new DocumentResponse(2L, 1L, "b.pdf", "application/pdf", 1000L, null, null);
        DocumentResponse doc3 = new DocumentResponse(3L, 3L, "c.pdf", "application/pdf", 500L, null, null);
        Map<Long, List<DocumentResponse>> docs = Map.of(
                1L, List.of(doc1, doc2),
                3L, List.of(doc3));
        ReportData data = ReportService.assemble(
                List.of(
                        rec(3, "Policy", "Active", "Dept", 2023),
                        rec(1, "Policy", "Active", "Dept", 2024)),
                docs, true);
        assertEquals("REF-1", data.documents().get(0).reference());
        assertEquals(2, data.documents().get(0).documentCount());
        assertEquals("REF-3", data.documents().get(1).reference());
        assertEquals(1, data.documents().get(1).documentCount());
    }

    @Test
    void summaryCategoryAndStatusCountsMatch() {
        ReportData data = ReportService.assemble(
                List.of(
                        rec(1, "Policy", "Active", "Dept A", 2024),
                        rec(2, "Policy", "Draft", "Dept A", 2024),
                        rec(3, "Scheme", "Active", "Dept B", 2023)),
                Map.of(), true);
        assertEquals(2, data.summary().categoryCount("Policy"));
        assertEquals(1, data.summary().categoryCount("Scheme"));
        assertEquals(0, data.summary().categoryCount("Missing"));
        assertEquals(2, data.summary().statusCount("Active"));
        assertEquals(1, data.summary().statusCount("Draft"));
    }
}
