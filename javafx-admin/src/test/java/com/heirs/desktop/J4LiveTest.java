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
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.heirs.desktop.api.DocumentsApi;
import com.heirs.desktop.api.RecordsApi;
import com.heirs.desktop.dto.DocumentResponse;
import com.heirs.desktop.dto.RecordRequest;
import com.heirs.desktop.dto.RecordResponse;
import com.heirs.desktop.model.DocumentInfo;
import com.heirs.desktop.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static com.heirs.desktop.util.JsonUtil.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Opt-in real MySQL + Spring Boot test (backend must run at 127.0.0.1:8080). All
 * mutations are limited to a freshly generated {@code TEST/JFXDOC/2026/...}
 * record. On success the record and one uploaded document are intentionally
 * retained for J4RestartLiveTest (fresh JVM, after backend stop/start); a failed
 * run removes everything it created.
 */
@EnabledIfSystemProperty(named = "heirs.live", matches = "true")
class J4LiveTest {

    static final Path VERIFY = Path.of("verification");
    static final Path FILES = VERIFY.resolve("j4-files");
    static final HttpClient HTTP = HttpClient.newHttpClient();

    static HttpResponse<String> http(String method, String path) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:8080/api/" + path))
                .method(method, HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    static HttpResponse<byte[]> httpBytes(String path) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:8080/api/" + path)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
    }

    static String hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Deterministic minimal PDF whose body differs per marker. */
    static byte[] pdf(String marker) {
        String body = IntStream.range(0, 100)
                .mapToObj(i -> "j4-virtual-%03d %s payload data data data\n".formatted(i, marker))
                .collect(Collectors.joining());
        return ("%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n2 0 obj\n<< /Length "
                + body.length() + " >>\nstream\n" + body + "endstream\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    static Path write(String name, byte[] bytes) throws Exception {
        Files.createDirectories(FILES);
        Path p = FILES.resolve(name);
        Files.write(p, bytes);
        return p;
    }

    static DocumentInfo byName(List<DocumentInfo> docs, String name) {
        return docs.stream().filter(d -> d.getFileName().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void realDocumentLifecycleThroughApiClient() throws Exception {
        boolean complete = false;
        long recordId = -1;
        try {
            Files.createDirectories(VERIFY);
            String reference = "TEST/JFXDOC/2026/" + System.currentTimeMillis() + "/A";
            long baseline = getMapper().readTree(http("GET", "records?size=1").body()).get("totalElements").asLong();

            byte[] contentA = pdf("j4-a");
            byte[] contentB = pdf("j4-b");
            byte[] replacement = pdf("j4-replacement");
            Path fileA = write("j4-test-a.pdf", contentA);
            Path fileB = write("j4-test-b.pdf", contentB);
            Path fileR = write("j4-test-replacement.pdf", replacement);
            String shaB = hex(contentB);

            RecordResponse created = new RecordsApi().createRecord(new RecordRequest(
                    "JavaFX Document Verification Record", "Disposable J4 document lifecycle record.",
                    "Policy", "JavaFX Verification Department", reference, 2026,
                    LocalDate.of(2026, 9, 15), "Active", "Disposable J4 live verification", "Digital Learning"));
            recordId = created.id();
            assertNotNull(recordId);
            assertEquals(0, new DocumentsApi().getDocumentsForRecord(recordId).size());

            var service = new DocumentService();
            List<DocumentInfo> uploaded = service.uploadDocuments(recordId, reference, List.of(fileA, fileB));
            assertEquals(2, uploaded.size());
            assertEquals(200, httpBytes("records/" + recordId + "/documents").statusCode());
            assertEquals(2, new DocumentsApi().getDocumentsForRecord(recordId).size());
            System.out.println("J4 PASS: real multi-file upload; independent list consistency");

            // Byte-integrity cross-check through preview, service download and independent HTTP.
            DocumentInfo docA = byName(uploaded, "j4-test-a.pdf");
            Path previewA = service.preparePreviewFile(docA);
            assertArrayEquals(contentA, Files.readAllBytes(previewA));
            Path downloaded = FILES.resolve("downloaded-" + System.currentTimeMillis() + ".pdf");
            service.downloadTo(docA, downloaded);
            assertArrayEquals(contentA, Files.readAllBytes(downloaded));
            assertEquals("j4-test-a.pdf", service.defaultDownloadName(docA));
            byte[] raw = httpBytes("documents/" + docA.getId() + "/download").body();
            assertArrayEquals(contentA, raw);
            assertEquals("%PDF-", new String(raw, 0, 5, StandardCharsets.US_ASCII));
            System.out.println("J4 PASS: preview/download bytes identical to uploaded source (%PDF- signature intact)");

            // Replace only the first document; the sibling stream must stay byte-identical.
            DocumentInfo replaced = service.replaceDocument(docA, reference, fileR);
            assertEquals("j4-test-replacement.pdf", replaced.getFileName());
            assertEquals(2, new DocumentsApi().getDocumentsForRecord(recordId).size());
            assertArrayEquals(replacement, httpBytes("documents/" + replaced.getId() + "/preview").body());
            DocumentInfo docB = byName(uploaded, "j4-test-b.pdf");
            assertArrayEquals(contentB, httpBytes("documents/" + docB.getId() + "/download").body());
            assertEquals(shaB, hex(httpBytes("documents/" + docB.getId() + "/download").body()));
            System.out.println("J4 PASS: replace persists new bytes and leaves sibling untouched");

            // Delete the replaced document only; sibling remains; deleted preview goes 404.
            service.deleteDocument(replaced);
            assertEquals(200, httpBytes("documents/" + docB.getId() + "/download").statusCode());
            assertEquals(404, httpBytes("documents/" + replaced.getId() + "/preview").statusCode());
            List<DocumentResponse> remaining = new DocumentsApi().getDocumentsForRecord(recordId);
            assertEquals(1, remaining.size());
            assertEquals("j4-test-b.pdf", remaining.get(0).originalFileName());
            DocumentInfo kept = service.listDocuments(recordId, reference).get(0);
            assertEquals(reference, kept.getRelatedReference());
            System.out.println("J4 PASS: single delete; sibling preserved; deleted document 404");

            Files.deleteIfExists(downloaded);
            Files.deleteIfExists(previewA);

            getMapper().writeValue(VERIFY.resolve("j4-state.json").toFile(), Map.of(
                    "recordId", recordId,
                    "reference", reference,
                    "docId", kept.getId(),
                    "docName", "j4-test-b.pdf",
                    "sha256", shaB,
                    "baseline", baseline));
            complete = true;
            System.out.println("J4 PASS: record + one document retained for restart verification");
        } finally {
            if (!complete) {
                try { new RecordsApi().deleteRecord(recordId); } catch (Exception ignored) { }
            }
        }
    }
}