package com.heirs.desktop;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import com.heirs.desktop.api.DocumentsApi;
import com.heirs.desktop.api.RecordsApi;
import com.heirs.desktop.dto.DocumentResponse;
import com.heirs.desktop.model.DocumentInfo;
import com.heirs.desktop.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static com.heirs.desktop.J4LiveTest.*;
import static com.heirs.desktop.util.JsonUtil.*;
import static org.junit.jupiter.api.Assertions.*;

/** Run in a second Maven/JVM invocation after J4LiveTest and a backend restart. */
@EnabledIfSystemProperty(named = "heirs.restart", matches = "true")
class J4RestartLiveTest {
    @Test
    void persistedDocumentsSurviveClientAndBackendRestartThenCleanup() throws Exception {
        var state = getMapper().readTree(Files.readString(VERIFY.resolve("j4-state.json")));
        long recordId = state.get("recordId").asLong();
        String reference = state.get("reference").asText();
        String docName = state.get("docName").asText();
        String sha256 = state.get("sha256").asText();
        long baseline = state.get("baseline").asLong();
        assertTrue(reference.startsWith("TEST/JFXDOC/2026/"));

        // Fresh JVM + restarted backend: metadata and file stream must have survived.
        assertEquals(reference, getMapper().readTree(http("GET", "records/" + recordId).body())
                .get("referenceNumber").asText());
        HttpResponse<byte[]> live = httpBytes("documents/" + state.get("docId").asLong() + "/download");
        assertEquals(200, live.statusCode());
        assertEquals("%PDF-", new String(live.body(), 0, 5, StandardCharsets.US_ASCII));
        assertEquals(sha256, hex(live.body()));
        List<DocumentResponse> docs = new DocumentsApi().getDocumentsForRecord(recordId);
        assertEquals(1, docs.size());
        assertEquals(docName, docs.get(0).originalFileName());
        DocumentInfo kept = new DocumentService().listDocuments(recordId, reference).get(0);
        assertEquals(docName, kept.getFileName());
        assertArrayEquals(live.body(), Files.readAllBytes(new DocumentService().preparePreviewFile(kept)));
        System.out.println("J4 PASS: fresh client JVM + backend restart persisted metadata and exact bytes");

        // Cleanup: document delete, then record delete cascades; totals restored.
        new DocumentService().deleteDocument(kept);
        assertEquals(404, httpBytes("documents/" + state.get("docId").asLong() + "/download").statusCode());
        assertEquals(0, new DocumentsApi().getDocumentsForRecord(recordId).size());
        new RecordsApi().deleteRecord(recordId);
        assertEquals(404, http("GET", "records/" + recordId).statusCode());
        assertEquals(404, http("GET", "records/" + recordId + "/documents").statusCode());
        assertEquals(baseline, getMapper().readTree(http("GET", "records?size=1").body()).get("totalElements").asLong());
        Files.deleteIfExists(VERIFY.resolve("j4-state.json"));
        System.out.println("J4 PASS: document delete; record-delete cascade; independent 404; original count restored");
    }
}