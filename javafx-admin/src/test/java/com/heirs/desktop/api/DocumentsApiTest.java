package com.heirs.desktop.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.heirs.desktop.config.ApiConfig;
import com.heirs.desktop.dto.DocumentResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentsApiTest {

    private static final byte[] PDF_BYTES = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF".getBytes(StandardCharsets.UTF_8);

    private HttpServer server;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final Map<String, Object> lastRequest = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ApiConfig.setOverrideBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
        ApiConfig.setOverrideBaseUrl(null);
    }

    private void registerApi() {
        server.createContext("/api", exchange -> {
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            try {
                handle(exchange, method, path);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    private void handle(com.sun.net.httpserver.HttpExchange exchange, String method, String path) throws IOException {
        if (path.matches("/api/records/\\d+/documents") && "GET".equals(method)) {
            respond(exchange, 200, """
                    [
                      {"id":8,"recordId":12,"originalFileName":"Main_Policy.pdf","contentType":"application/pdf",
                       "fileSize":1845932,"uploadedAt":"2026-09-13T12:00:00","updatedAt":"2026-09-13T12:00:00"}
                    ]
                    """);
        } else if (path.matches("/api/records/\\d+/documents") && "POST".equals(method)) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            lastRequest.put("contentType", exchange.getRequestHeaders().getFirst("Content-Type"));
            lastRequest.put("body", body);
            respond(exchange, 201, """
                    [
                      {"id":8,"recordId":12,"originalFileName":"Main_Policy.pdf","contentType":"application/pdf",
                       "fileSize":1845932,"uploadedAt":"2026-09-13T12:00:00","updatedAt":"2026-09-13T12:00:00"},
                      {"id":9,"recordId":12,"originalFileName":"Annex.pdf","contentType":"application/pdf",
                       "fileSize":1024,"uploadedAt":"2026-09-13T12:00:00","updatedAt":"2026-09-13T12:00:00"}
                    ]
                    """);
        } else if (path.matches("/api/documents/\\d+/replace") && "PUT".equals(method)) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            lastRequest.put("body", body);
            respond(exchange, 200, """
                    {"id":8,"recordId":12,"originalFileName":"Replacement.pdf","contentType":"application/pdf",
                     "fileSize":2048,"uploadedAt":"2026-09-13T12:00:00","updatedAt":"2026-09-14T12:00:00"}
                    """);
        } else if (path.matches("/api/documents/\\d+") && "DELETE".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
        } else if (path.matches("/api/documents/\\d+/preview")) {
            exchange.getResponseHeaders().add("Content-Type", "application/pdf");
            exchange.getResponseHeaders().add("Content-Disposition",
                    "inline; filename=\"Annex.pdf\"; filename*=UTF-8''Annex.pdf");
            exchange.sendResponseHeaders(200, PDF_BYTES.length);
            exchange.getResponseBody().write(PDF_BYTES);
        } else if (path.matches("/api/documents/\\d+/download")) {
            exchange.getResponseHeaders().add("Content-Type", "application/pdf");
            exchange.getResponseHeaders().add("Content-Disposition",
                    "attachment; filename=\"Main_Policy.pdf\"; filename*=UTF-8''Main_Policy.pdf");
            exchange.sendResponseHeaders(200, PDF_BYTES.length);
            exchange.getResponseBody().write(PDF_BYTES);
        } else if (path.matches("/api/documents/\\d+") && "GET".equals(method)) {
            respond(exchange, 200, """
                    {"id":8,"recordId":12,"originalFileName":"Main_Policy.pdf","contentType":"application/pdf",
                     "fileSize":1845932,"uploadedAt":"2026-09-13T12:00:00","updatedAt":"2026-09-13T12:00:00"}
                    """);
        } else {
            respond(exchange, 404, "{\"message\":\"Not found\"}");
        }
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    @Test
    void documentResponseDtoMatchesBackendShape() throws Exception {
        String json = """
                {"id":8,"recordId":12,"originalFileName":"Main_Policy.pdf","contentType":"application/pdf",
                 "fileSize":1845932,"uploadedAt":"2026-09-13T12:00:00","updatedAt":"2026-09-13T12:00:00"}
                """;
        DocumentResponse dto = com.heirs.desktop.util.JsonUtil.fromJson(json, DocumentResponse.class);
        assertEquals(8L, dto.id());
        assertEquals(12L, dto.recordId());
        assertEquals("Main_Policy.pdf", dto.originalFileName());
        assertEquals("application/pdf", dto.contentType());
        assertEquals(1845932L, dto.fileSize());
        assertNotNull(dto.uploadedAt());
        assertNotNull(dto.updatedAt());
    }

    @Test
    void listDocumentsParsesJsonArray() {
        registerApi();
        List<DocumentResponse> docs = new DocumentsApi().getDocumentsForRecord(12L);
        assertEquals(1, docs.size());
        assertEquals("Main_Policy.pdf", docs.get(0).originalFileName());
        assertEquals(12L, docs.get(0).recordId());
    }

    @Test
    void uploadSendsRepeatedFilesMultipartAndParses201() throws IOException {
        Path tmpA = Files.createTempFile("j4-test-a", ".pdf");
        Path tmpB = Files.createTempFile("j4-test-b", ".pdf");
        Files.write(tmpA, PDF_BYTES);
        Files.write(tmpB, PDF_BYTES);
        try {
            registerApi();
            List<DocumentResponse> uploaded = new DocumentsApi().uploadDocuments(12L, List.of(tmpA, tmpB));
            assertEquals(2, uploaded.size());
            assertTrue(requests.contains("POST /api/records/12/documents"));

            String contentType = (String) lastRequest.get("contentType");
            assertNotNull(contentType);
            assertTrue(contentType.startsWith("multipart/form-data; boundary="));
            String body = (String) lastRequest.get("body");
            assertEquals(2, countOccurrences(body, "name=\"files\""));
            assertEquals(1, countOccurrences(body, "filename=\"" + tmpA.getFileName() + "\""));
            assertEquals(1, countOccurrences(body, "filename=\"" + tmpB.getFileName() + "\""));
            assertTrue(body.contains("Content-Type: application/pdf"));
        } finally {
            Files.deleteIfExists(tmpA);
            Files.deleteIfExists(tmpB);
        }
    }

    @Test
    void replaceSendsSingularFileMultipartAndParses200() throws IOException {
        Path tmp = Files.createTempFile("j4-test-replacement", ".pdf");
        Files.write(tmp, PDF_BYTES);
        try {
            registerApi();
            DocumentResponse replaced = new DocumentsApi().replaceDocument(8L, tmp);
            assertEquals("Replacement.pdf", replaced.originalFileName());
            assertTrue(requests.contains("PUT /api/documents/8/replace"));
            String body = (String) lastRequest.get("body");
            assertEquals(1, countOccurrences(body, "name=\"file\""));
            assertEquals(0, countOccurrences(body, "name=\"files\""));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void deleteTreatsEmpty204AsSuccess() {
        registerApi();
        var api = new DocumentsApi();
        assertDoesNotThrow(() -> api.deleteDocument(8L));
        assertTrue(requests.contains("DELETE /api/documents/8"));
    }

    @Test
    void previewReturnsPdfBytesAndParsedFilename() {
        registerApi();
        BinaryResponse preview = new DocumentsApi().previewDocument(8L, "fallback.pdf");
        assertArrayEquals(PDF_BYTES, preview.body());
        assertEquals("application/pdf", preview.contentType());
        assertEquals(PDF_BYTES.length, preview.contentLength());
        assertEquals("Annex.pdf", preview.fileName());
        assertTrue(requests.contains("GET /api/documents/8/preview"));
    }

    @Test
    void downloadReturnsPdfBytesAndParsedFilename() {
        registerApi();
        BinaryResponse download = new DocumentsApi().downloadDocument(8L, "fallback.pdf");
        assertArrayEquals(PDF_BYTES, download.body());
        assertEquals("application/pdf", download.contentType());
        assertEquals(PDF_BYTES.length, download.contentLength());
        assertEquals("Main_Policy.pdf", download.fileName());
    }

    @Test
    void missingDocumentSurfaceAs404NotOffline() {
        registerApi();
        server.removeContext("/api");
        server.createContext("/api", exchange -> {
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("{\"message\":\"Document 999 was not found\"}".getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        ApiException error = assertThrows(ApiException.class, () -> new DocumentsApi().deleteDocument(999L));
        assertEquals(404, error.getStatusCode());
        assertFalse(error.isOffline());
    }

    @Test
    void stoppedBackendReportsOffline() {
        server.stop(0);
        ApiException error = assertThrows(ApiException.class, () -> new DocumentsApi().getDocumentsForRecord(12L));
        assertTrue(error.isOffline());
    }

    @Test
    void invalidRecordListFallsBackToEmptyOn404() throws Exception {
        HttpServer s404 = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s404.createContext("/", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        s404.start();
        try {
            ApiConfig.setOverrideBaseUrl("http://127.0.0.1:" + s404.getAddress().getPort());
            List<DocumentResponse> docs = new DocumentsApi().getDocumentsForRecord(999L);
            assertEquals(0, docs.size());
        } finally {
            s404.stop(0);
        }
    }

    private static int countOccurrences(String haystack, String needle) {
        if (haystack == null) return 0;
        int count = 0;
        int index = 0;
        while ((index = haystack.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}