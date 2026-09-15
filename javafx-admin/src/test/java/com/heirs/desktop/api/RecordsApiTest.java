package com.heirs.desktop.api;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
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

    @Test
    public void testSearchRecordsSendsSortParams() {
        CapturingApiClient client = new CapturingApiClient();
        RecordsApi recordsApi = new RecordsApi(client);

        recordsApi.searchRecords("digital", "Policy", 2026, "Active", "Higher Education Department", 0, 20, "year", "desc");

        assertNotNull(client.lastPath, "A search request should have been issued");
        String path = client.lastPath;
        assertTrue(path.contains("sortBy=year"), "Expected sortBy=year but got " + path);
        assertTrue(path.contains("sortDirection=desc"), "Expected sortDirection=desc but got " + path);
        assertTrue(path.contains("q=digital"), "Expected q=digital but got " + path);
        assertTrue(path.contains("category=Policy"), "Expected category=Policy but got " + path);
        assertTrue(path.contains("page=0"), "Expected page=0 but got " + path);
        assertTrue(path.contains("size=20"), "Expected size=20 but got " + path);
        assertTrue(path.startsWith("/api/records/search?"), "Search endpoint should be used but got " + path);
    }

    @Test
    public void testSearchRecordsOmitsEmptySortParams() {
        CapturingApiClient client = new CapturingApiClient();
        RecordsApi recordsApi = new RecordsApi(client);

        recordsApi.searchRecords("digital", null, null, null, null, 1, 50, null, null);

        assertNotNull(client.lastPath, "A search request should have been issued");
        assertFalse(client.lastPath.contains("sortBy="), "Null sortBy should be omitted but got " + client.lastPath);
        assertFalse(client.lastPath.contains("sortDirection="), "Null sortDirection should be omitted but got " + client.lastPath);
        assertTrue(client.lastPath.contains("page=1"), "Expected page=1 but got " + client.lastPath);
        assertTrue(client.lastPath.contains("size=50"), "Expected size=50 but got " + client.lastPath);
    }

    /**
     * Captures the endpoint path passed to ApiClient.get without opening a real
     * network connection. ApiClient.get(String, TypeReference) is overridden only;
     * the canned response is an empty paged envelope sufficient for the caller.
     */
    private static final class CapturingApiClient extends ApiClient {
        volatile String lastPath;

        @Override
        public <T> T get(String endpointPath, TypeReference<T> responseType) {
            lastPath = endpointPath;
            String json = """
                          {
                            "content": [],
                            "page": 0,
                            "size": 20,
                            "totalElements": 0,
                            "totalPages": 0,
                            "first": true,
                            "last": true
                          }
                          """;
            try {
                return JsonUtil.fromJson(json, responseType);
            } catch (Exception e) {
                throw new RuntimeException("Unable to parse canned paged response in test", e);
            }
        }
    }
}
