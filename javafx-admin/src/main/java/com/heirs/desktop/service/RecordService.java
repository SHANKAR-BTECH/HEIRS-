package com.heirs.desktop.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.heirs.desktop.api.RecordsApi;
import com.heirs.desktop.dto.CategoryResponse;
import com.heirs.desktop.dto.PagedResponse;
import com.heirs.desktop.dto.RecordResponse;
import com.heirs.desktop.model.Record;

/**
 * High-level service mediating between JavaFX controllers and RecordsApi.
 */
public class RecordService {

    private final RecordsApi recordsApi;

    public RecordService() {
        this(new RecordsApi());
    }

    public RecordService(RecordsApi recordsApi) {
        this.recordsApi = recordsApi;
    }

    public boolean checkHealth() {
        return recordsApi.checkHealth();
    }

    public PagedResponse<Record> fetchAll(int page, int size) {
        PagedResponse<RecordResponse> dtoPage = recordsApi.getAllRecords(page, size);
        List<Record> records = dtoPage.safeContent().stream()
                .map(Record::from)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                records,
                dtoPage.page(),
                dtoPage.size(),
                dtoPage.totalElements(),
                dtoPage.totalPages(),
                dtoPage.first(),
                dtoPage.last()
        );
    }

    public Record fetchById(Long id) {
        RecordResponse response = recordsApi.getRecordById(id);
        return Record.from(response);
    }

    public Record create(com.heirs.desktop.dto.RecordRequest request) {
        return Record.from(recordsApi.createRecord(request));
    }

    public Record update(Long id, com.heirs.desktop.dto.RecordRequest request) {
        return Record.from(recordsApi.updateRecord(id, request));
    }

    public void delete(Long id) {
        recordsApi.deleteRecord(id);
    }

    /** Follow backend pagination so newly created records beyond row 100 stay visible. */
    public PagedResponse<Record> fetchCatalog() {
        var all = new ArrayList<Record>();
        PagedResponse<Record> page;
        int index = 0;
        do {
            page = fetchAll(index++, 100);
            all.addAll(page.safeContent());
        } while (!page.last() && index < page.totalPages());
        return new PagedResponse<>(all, 0, all.size(), page.totalElements(), 1, true, true);
    }

    public PagedResponse<Record> search(String query,
                                       String category,
                                       Integer year,
                                       String status,
                                       String department,
                                       int page,
                                       int size,
                                       String sortBy,
                                       String sortDirection) {
        PagedResponse<RecordResponse> dtoPage = recordsApi.searchRecords(
                query, category, year, status, department, page, size, sortBy, sortDirection);

        List<Record> records = dtoPage.safeContent().stream()
                .map(Record::from)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                records,
                dtoPage.page(),
                dtoPage.size(),
                dtoPage.totalElements(),
                dtoPage.totalPages(),
                dtoPage.first(),
                dtoPage.last()
        );
    }

    public List<String> fetchCategoryNames() {
        try {
            List<CategoryResponse> list = recordsApi.getCategories();
            if (list == null || list.isEmpty()) {
                return getDefaultCategories();
            }
            return list.stream().map(CategoryResponse::name).collect(Collectors.toList());
        } catch (Exception e) {
            return getDefaultCategories();
        }
    }

    private List<String> getDefaultCategories() {
        return com.heirs.desktop.model.RecordOptions.CATEGORIES;
    }
}
