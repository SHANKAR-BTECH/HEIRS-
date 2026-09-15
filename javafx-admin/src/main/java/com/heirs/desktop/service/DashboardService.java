package com.heirs.desktop.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.heirs.desktop.dto.PagedResponse;
import com.heirs.desktop.model.Record;

/**
 * Service calculating real Dashboard metrics and recent records from backend data.
 */
public class DashboardService {

    private final RecordService recordService;

    public DashboardService() {
        this(new RecordService());
    }

    public DashboardService(RecordService recordService) {
        this.recordService = recordService;
    }

    public record DashboardData(
            long totalRecords,
            long policies,
            long schemes,
            long regulations,
            long projects,
            long rules,
            List<Record> recentRecords
    ) {}

    public DashboardData loadDashboardData() {
        PagedResponse<Record> page = recordService.fetchCatalog();
        List<Record> all = page.safeContent();

        long total = page.totalElements() > 0 ? page.totalElements() : all.size();

        Map<String, Long> categoryCounts = all.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCategory() != null ? r.getCategory().trim() : "",
                        Collectors.counting()));

        long policies = categoryCounts.getOrDefault("Policy", 0L);
        long schemes = categoryCounts.getOrDefault("Scheme", 0L);
        long regulations = categoryCounts.getOrDefault("Regulation", 0L);
        long projects = categoryCounts.getOrDefault("Project", 0L);
        long rules = categoryCounts.getOrDefault("Rules", 0L);

        // Recent records: sort by publishedDate desc, then id desc, top 8
        List<Record> recent = all.stream()
                .sorted(Comparator.comparing(
                        Record::getPublishedDate,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Record::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .collect(Collectors.toList());

        return new DashboardData(total, policies, schemes, regulations, projects, rules, recent);
    }
}
