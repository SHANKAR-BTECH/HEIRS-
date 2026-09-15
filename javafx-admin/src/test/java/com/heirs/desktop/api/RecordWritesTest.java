package com.heirs.desktop.api;

import com.heirs.desktop.config.ApiConfig;
import com.heirs.desktop.dto.RecordRequest;
import com.heirs.desktop.model.RecordValidation;
import com.heirs.desktop.util.JsonUtil;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RecordWritesTest {
    private HttpServer server;
    @AfterEach void cleanup() { if (server != null) server.stop(0); ApiConfig.setOverrideBaseUrl(null); }
    private RecordRequest request() {
        return new RecordRequest("Title", "Description", "Policy", "Free department", "TEST/1", 2026,
                LocalDate.of(2026, 9, 15), "Draft", "Source", "Digital Learning, Infrastructure");
    }
    @Test void exactPayloadAndIsoDate() throws Exception {
        var json = JsonUtil.getMapper().readTree(JsonUtil.toJson(request()));
        assertEquals(10, json.size());
        assertFalse(json.has("id"));
        assertEquals("2026-09-15", json.get("publishedDate").asText());
        assertTrue(json.get("keywords").isTextual());
        assertEquals("Free department", json.get("department").asText());
    }
    @Test void optionalYearDescriptionAndRequiredFields() {
        assertTrue(RecordValidation.validate(request()).isEmpty());
        var invalid = new RecordRequest(" ", null, null, "", "", 2101, null, null, null, null);
        var fields = RecordValidation.validate(invalid);
        assertEquals(6, fields.size());
        assertFalse(fields.containsKey("description"));
        assertTrue(RecordValidation.validate(new RecordRequest("t", null, "Policy", "d", "r", null, null, "Draft", null, null)).isEmpty());
    }
    @Test void realHttpMethodsAndEmpty204() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var seen = new java.util.concurrent.CopyOnWriteArrayList<String>();
        server.createContext("/api/records", exchange -> {
            seen.add(exchange.getRequestMethod() + " " + exchange.getRequestURI());
            if (exchange.getRequestMethod().equals("DELETE")) {
                exchange.sendResponseHeaders(204, -1);
            } else {
                var body = JsonUtil.getMapper().readTree(exchange.getRequestBody());
                assertEquals("2026-09-15", body.get("publishedDate").asText());
                assertEquals("application/json", exchange.getRequestHeaders().getFirst("Content-Type"));
                byte[] response = "{\"id\":17,\"title\":\"Title\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(exchange.getRequestMethod().equals("POST") ? 201 : 200, response.length);
                exchange.getResponseBody().write(response);
            }
            exchange.close();
        });
        server.start();
        ApiConfig.setOverrideBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        var api = new RecordsApi();
        assertEquals(17L, api.createRecord(request()).id());
        assertEquals(17L, api.updateRecord(17L, request()).id());
        assertDoesNotThrow(() -> api.deleteRecord(17L));
        assertEquals(java.util.List.of("POST /api/records", "PUT /api/records/17", "DELETE /api/records/17"), seen);
    }
    @Test void backendValidationAndDuplicateMapping() throws Exception {
        var error = JsonUtil.fromJson("{\"message\":\"Request validation failed\",\"fieldErrors\":{\"source\":\"size must be between 0 and 500\"}}", com.heirs.desktop.dto.ApiErrorResponse.class);
        assertEquals("size must be between 0 and 500", RecordValidation.backendFields(ApiException.fromErrorResponse(400, error)).get("source"));
        var duplicate = new ApiException(409, "Reference number already exists", null, Map.of());
        assertEquals("Reference number already exists", RecordValidation.backendFields(duplicate).get("referenceNumber"));
        assertTrue(RecordValidation.backendFields(new ApiException(409, "Other constraint", null, Map.of())).isEmpty());
    }
    @Test void invalidJsonIsNotReportedAsNetworkOffline() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/records", exchange -> {
            byte[] body = "invalid json".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(201, body.length);
            exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
        ApiConfig.setOverrideBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        assertFalse(assertThrows(ApiException.class, () -> new RecordsApi().createRecord(request())).isOffline());
    }
    @Test void httpErrorsPreserveStatusAndNetworkFailure() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/records", exchange -> {
            int code = Integer.parseInt(exchange.getRequestURI().getPath().substring("/api/records/".length()));
            byte[] body = "{\"message\":\"Backend rejected operation\",\"fieldErrors\":{\"title\":\"Invalid title\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        ApiConfig.setOverrideBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        var api = new RecordsApi();
        for (long code : new long[]{400, 404, 409, 500}) {
            var error = assertThrows(ApiException.class, () -> api.updateRecord(code, request()));
            assertEquals(code, error.getStatusCode());
            assertFalse(error.isOffline());
            assertEquals("Invalid title", error.getFieldErrors().get("title"));
        }
        server.stop(0);
        assertTrue(assertThrows(ApiException.class, () -> api.createRecord(request())).isOffline());
    }
}
