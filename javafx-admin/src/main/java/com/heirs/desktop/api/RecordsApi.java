package com.heirs.desktop.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.heirs.desktop.dto.CategoryResponse;
import com.heirs.desktop.dto.HealthResponse;
import com.heirs.desktop.dto.PagedResponse;
import com.heirs.desktop.dto.RecordResponse;

/**
 * Client for HEIRS Record, Category, and Health REST endpoints.
 */
public class RecordsApi {

    private final ApiClient apiClient;

    public RecordsApi() {
        this(new ApiClient());
    }

    public RecordsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public boolean checkHealth() {
        try {
            HealthResponse health = apiClient.get("/api/health", HealthResponse.class);
            return health != null && health.isHealthy();
        } catch (Exception e) {
            return false;
        }
    }

    public RecordResponse createRecord(com.heirs.desktop.dto.RecordRequest request) {
        return apiClient.post("/api/records", request, RecordResponse.class);
    }

    public RecordResponse updateRecord(Long id, com.heirs.desktop.dto.RecordRequest request) {
        requireId(id);
        return apiClient.put("/api/records/" + id, request, RecordResponse.class);
    }

    public void deleteRecord(Long id) {
        requireId(id);
        apiClient.delete("/api/records/" + id);
    }

    private static void requireId(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("A positive record ID is required");
    }

    public PagedResponse<RecordResponse> getAllRecords(int page, int size) {
        String path = "/api/records?page=" + page + "&size=" + size;
        return apiClient.get(path, new TypeReference<PagedResponse<RecordResponse>>() {});
    }

    public RecordResponse getRecordById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Record id must not be null");
        }
        return apiClient.get("/api/records/" + id, RecordResponse.class);
    }

    public PagedResponse<RecordResponse> searchRecords(String query,
                                                       String category,
                                                       Integer year,
                                                       String status,
                                                       String department,
                                                       int page,
                                                       int size,
                                                       String sortBy,
                                                       String sortDirection) {
        List<String> queryParams = new ArrayList<>();
        if (query != null && !query.isBlank()) {
            queryParams.add("q=" + encode(query.trim()));
        }
        if (category != null && !category.isBlank() && !"All Categories".equalsIgnoreCase(category)) {
            queryParams.add("category=" + encode(category.trim()));
        }
        if (year != null && year > 0) {
            queryParams.add("year=" + year);
        }
        if (status != null && !status.isBlank() && !"All Statuses".equalsIgnoreCase(status)) {
            queryParams.add("status=" + encode(status.trim()));
        }
        if (department != null && !department.isBlank() && !"All Departments".equalsIgnoreCase(department)) {
            queryParams.add("department=" + encode(department.trim()));
        }
        if (sortBy != null && !sortBy.isBlank()) {
            queryParams.add("sortBy=" + encode(sortBy.trim()));
        }
        if (sortDirection != null && !sortDirection.isBlank()) {
            queryParams.add("sortDirection=" + encode(sortDirection.trim()));
        }
        queryParams.add("page=" + page);
        queryParams.add("size=" + size);

        String path = "/api/records/search?" + String.join("&", queryParams);
        return apiClient.get(path, new TypeReference<PagedResponse<RecordResponse>>() {});
    }

    public List<CategoryResponse> getCategories() {
        return apiClient.get("/api/categories", new TypeReference<List<CategoryResponse>>() {});
    }

    private String encode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}
