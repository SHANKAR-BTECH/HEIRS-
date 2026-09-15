package com.heirs.desktop.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.heirs.desktop.api.DocumentsApi;
import com.heirs.desktop.dto.DocumentResponse;
import com.heirs.desktop.model.Record;
import com.heirs.desktop.model.report.DocumentReportRow;
import com.heirs.desktop.model.report.ReportCountRow;
import com.heirs.desktop.model.report.ReportData;
import com.heirs.desktop.model.report.RepositorySummary;
import com.heirs.desktop.model.report.YearReportRow;

/**
 * Generates the administrative report dataset from the real backend repository.
 *
 * <p>Record history is loaded by paging through {@link RecordService#fetchCatalog()}
 * (never a single page), so reports always cover the full repository. Supporting
 * document metadata is fetched per record with a bounded, small parallel pool and
 * failure tolerance: a partial load is flagged via {@link ReportData#documentsLoaded()}.
 */
public class ReportService {

    /** Bounded document fetch concurrency (avoids N+1 storm against the backend). */
    private static final int DOC_FETCH_CONCURRENCY = 4;
    private static final long DOC_FETCH_TIMEOUT_SECONDS = 60;

    private final RecordService recordService;
    private final DocumentsApi documentsApi;

    public ReportService() {
        this(new RecordService(), new DocumentsApi());
    }

    public ReportService(RecordService recordService, DocumentsApi documentsApi) {
        this.recordService = recordService;
        this.documentsApi = documentsApi;
    }

    /** Loads the full repository and returns all report data for it. */
    public ReportData generateReports() {
        List<Record> records = recordService.fetchCatalog().safeContent();
        DocumentLoad docs = loadDocumentStats(records);
        return assemble(records, docs.byRecord, docs.allLoaded);
    }

    /* ------------------------------------------------------------------ */
    /* Document statistics (network, bounded concurrency)                 */
    /* ------------------------------------------------------------------ */

    private static final class DocumentLoad {
        final Map<Long, List<DocumentResponse>> byRecord = new ConcurrentHashMap<>();
        volatile boolean allLoaded;
        DocumentLoad(boolean allLoaded) {
            this.allLoaded = allLoaded;
        }
    }

    private DocumentLoad loadDocumentStats(List<Record> records) {
        ExecutorService pool = Executors.newFixedThreadPool(DOC_FETCH_CONCURRENCY, new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "heirs-report-docs-" + counter.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });

        DocumentLoad load = new DocumentLoad(false);
        try {
            List<CompletableFuture<Void>> tasks = new ArrayList<>();
            for (Record record : records) {
                tasks.add(CompletableFuture.runAsync(() -> {
                    try {
                        List<DocumentResponse> docs = documentsApi.getDocumentsForRecord(record.getId());
                        load.byRecord.put(record.getId(), docs != null ? docs : List.of());
                    } catch (RuntimeException ignored) {
                        // Individual document fetch failure tolerated; load flagged below.
                    }
                }, pool));
            }
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                    .get(DOC_FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            load.allLoaded = true;
        } catch (Exception e) {
            load.allLoaded = false;
        } finally {
            pool.shutdown();
        }
        return load;
    }

    /* ------------------------------------------------------------------ */
    /* Pure aggregation (deterministic; also used by unit tests)          */
    /* ------------------------------------------------------------------ */

    /**
     * Assembles the full immutable report dataset. Package-visible so tests can
     * exercise aggregation without any network access.
     */
    static ReportData assemble(List<Record> records, Map<Long, List<DocumentResponse>> docsByRecord,
                               boolean documentsFullyLoaded) {
        RepositorySummary summary = buildSummary(records, docsByRecord);
        List<ReportCountRow> categories = buildCategoryRows(records);
        List<ReportCountRow> statuses = buildStatusRows(records);
        List<YearReportRow> years = buildYearRows(records);
        List<ReportCountRow> departments = buildDepartmentRows(records);

        long totalDocs = 0;
        long recordsWithDocs = 0;
        long largestCount = 0;
        long totalStoredBytes = 0;
        for (Map.Entry<Long, List<DocumentResponse>> e : docsByRecord.entrySet()) {
            List<DocumentResponse> docs = e.getValue();
            long count = docs == null ? 0 : docs.size();
            totalDocs += count;
            if (count > 0) recordsWithDocs++;
            if (count > largestCount) largestCount = count;
            if (docs != null) {
                for (DocumentResponse d : docs) {
                    totalStoredBytes += d.fileSize() != null ? d.fileSize() : 0L;
                }
            }
        }

        double averagePerRecord = records.isEmpty() ? 0.0
                : (double) totalDocs / summary.totalRecords();
        long recordsWithoutDocs = summary.totalRecords() - recordsWithDocs;

        List<DocumentReportRow> documentRows = records.stream()
                .map(r -> new DocumentReportRow(
                        safe(r.getReference()),
                        safe(r.getTitle()),
                        docCount(docsByRecord, r.getId()),
                        docBytes(docsByRecord, r.getId())))
                .sorted(Comparator.comparingLong(DocumentReportRow::documentCount).reversed()
                        .thenComparing(DocumentReportRow::reference))
                .collect(Collectors.toList());

        return new ReportData(
                summary,
                categories,
                statuses,
                years,
                departments,
                documentRows,
                documentsFullyLoaded,
                totalDocs,
                recordsWithDocs,
                recordsWithoutDocs,
                averagePerRecord,
                largestCount,
                totalStoredBytes,
                LocalDateTime.now()
        );
    }

    static RepositorySummary buildSummary(List<Record> records, Map<Long, List<DocumentResponse>> docsByRecord) {
        long totalRecords = records.size();

        Map<String, Long> categories = records.stream()
                .collect(Collectors.groupingBy(r -> normalizeCategory(r.getCategory()), Collectors.counting()));
        Map<String, Long> statuses = records.stream()
                .collect(Collectors.groupingBy(r -> normalizeStatus(r.getStatus()), Collectors.counting()));

        long departments = records.stream()
                .map(r -> normalizeDepartment(r.getDepartment()))
                .filter(d -> !d.isBlank())
                .distinct()
                .count();
        long years = records.stream()
                .map(Record::getYear)
                .filter(y -> y != null && y > 0)
                .distinct()
                .count();

        long totalDocs = 0;
        long recordsWithDocs = 0;
        for (List<DocumentResponse> docs : docsByRecord.values()) {
            long count = docs == null ? 0 : docs.size();
            totalDocs += count;
            if (count > 0) recordsWithDocs++;
        }

        return new RepositorySummary(
                totalRecords,
                categories,
                statuses,
                departments,
                years,
                totalDocs,
                recordsWithDocs,
                totalRecords - recordsWithDocs
        );
    }

    static List<ReportCountRow> buildCategoryRows(List<Record> records) {
        long total = records.size();
        return records.stream()
                .collect(Collectors.groupingBy(r -> normalizeCategory(r.getCategory()), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ReportCountRow(e.getKey(), e.getValue(), percent(e.getValue(), total)))
                .sorted(Comparator.comparingLong(ReportCountRow::count).reversed()
                        .thenComparing(ReportCountRow::name))
                .collect(Collectors.toList());
    }

    static List<ReportCountRow> buildStatusRows(List<Record> records) {
        long total = records.size();
        return records.stream()
                .collect(Collectors.groupingBy(r -> normalizeStatus(r.getStatus()), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ReportCountRow(e.getKey(), e.getValue(), percent(e.getValue(), total)))
                .sorted(Comparator.comparingLong(ReportCountRow::count).reversed()
                        .thenComparing(ReportCountRow::name))
                .collect(Collectors.toList());
    }

    static List<YearReportRow> buildYearRows(List<Record> records) {
        long total = records.size();
        Map<Integer, Long> byYear = new LinkedHashMap<>();
        for (Record r : records) {
            Integer y = r.getYear();
            int year = y != null ? y : 0;
            byYear.merge(year, 1L, Long::sum);
        }
        return byYear.entrySet().stream()
                .map(e -> new YearReportRow(e.getKey(), e.getValue(), percent(e.getValue(), total)))
                .sorted(Comparator.comparingInt(YearReportRow::year).reversed())
                .collect(Collectors.toList());
    }

    static List<ReportCountRow> buildDepartmentRows(List<Record> records) {
        long total = records.size();
        return records.stream()
                .collect(Collectors.groupingBy(r -> normalizeDepartment(r.getDepartment()), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ReportCountRow(e.getKey(), e.getValue(), percent(e.getValue(), total)))
                .sorted(Comparator.comparingLong(ReportCountRow::count).reversed()
                        .thenComparing(ReportCountRow::name))
                .collect(Collectors.toList());
    }

    private static double percent(long part, long total) {
        return total == 0 ? 0.0 : (part * 100.0) / total;
    }

    private static long docCount(Map<Long, List<DocumentResponse>> docsByRecord, Long id) {
        List<DocumentResponse> docs = docsByRecord.get(id);
        return docs == null ? 0 : docs.size();
    }

    private static long docBytes(Map<Long, List<DocumentResponse>> docsByRecord, Long id) {
        List<DocumentResponse> docs = docsByRecord.get(id);
        if (docs == null) return 0L;
        long sum = 0;
        for (DocumentResponse d : docs) {
            if (d.fileSize() != null) sum += d.fileSize();
        }
        return sum;
    }

    private static String normalizeCategory(String value) {
        return value == null || value.isBlank() ? "Uncategorized" : value.trim();
    }

    private static String normalizeStatus(String value) {
        return value == null || value.isBlank() ? "Unknown" : value.trim();
    }

    private static String normalizeDepartment(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}