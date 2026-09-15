package com.heirs.desktop.api;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.heirs.desktop.config.ApiConfig;
import com.heirs.desktop.dto.CategoryResponse;
import com.heirs.desktop.dto.PagedResponse;
import com.heirs.desktop.dto.RecordResponse;
import com.heirs.desktop.model.Record;
import com.heirs.desktop.util.JsonUtil;

import static org.junit.jupiter.api.Assertions.*;

public class RecordsApiTest {

    @Test
    public void testRecordResponseDeserialization() throws Exception {
        String json = """
                {
                  "id": 14,
                  "title": "National Digital Learning Policy",
                  "description": "Comprehensive digital learning framework.",
                  "category": "Policy",
                  "department": "Higher Education Department",
                  "referenceNumber": "HEIRS/POL/2026/014",
                  "publicationYear": 2026,
                  "publishedDate": "2026-03-12",
                  "status": "Active",
                  "source": "Higher Education",
                  "keywords": "digital,learning,policy"
                }
                """;

        RecordResponse res = JsonUtil.fromJson(json, RecordResponse.class);
        assertNotNull(res);
        assertEquals(14L, res.id());
        assertEquals("National Digital Learning Policy", res.title());
        assertEquals("Policy", res.category());
        assertEquals(2026, res.publicationYear());
        assertEquals(LocalDate.of(2026, 3, 12), res.publishedDate());
        assertEquals("Active", res.status());

        Record model = Record.from(res);
        assertNotNull(model);
        assertEquals(14L, model.getId());
        assertEquals("HEIRS/POL/2026/014", model.getReference());
        assertEquals("National Digital Learning Policy", model.getTitle());
    }

    @Test
    public void testPagedResponseDeserialization() throws Exception {
        String json = """
                {
                  "content": [
                    {
                      "id": 1,
                      "title": "Title 1",
                      "category": "Policy",
                      "department": "Higher Education",
                      "referenceNumber": "REF-1",
                      "publicationYear": 2026,
                      "status": "Active"
                    }
                  ],
                  "page": 0,
                  "size": 20,
                  "totalElements": 60,
                  "totalPages": 3,
                  "first": true,
                  "last": false
                }
                """;

        PagedResponse<RecordResponse> page = JsonUtil.fromJson(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        assertNotNull(page);
        assertEquals(1, page.safeContent().size());
        assertEquals(60L, page.totalElements());
        assertEquals(3, page.totalPages());
        assertEquals(0, page.page());
        assertTrue(page.first());
        assertFalse(page.last());
    }

    @Test
    public void testCategoryListDeserialization() throws Exception {
        String json = """
                [
                  {"code":"POLICY","name":"Policy"},
                  {"code":"SCHEME","name":"Scheme"}
                ]
                """;

        List<CategoryResponse> categories = JsonUtil.fromJson(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        assertNotNull(categories);
        assertEquals(2, categories.size());
        assertEquals("Policy", categories.get(0).name());
        assertEquals("Scheme", categories.get(1).name());
    }

    @Test
    public void testApiConfigResolution() {
        assertNotNull(ApiConfig.getBaseUrl());
        assertTrue(ApiConfig.getBaseUrl().startsWith("http"));

        ApiConfig.setOverrideBaseUrl("http://localhost:9090/");
        assertEquals("http://localhost:9090", ApiConfig.getBaseUrl());
        ApiConfig.setOverrideBaseUrl(null);
    }
}
