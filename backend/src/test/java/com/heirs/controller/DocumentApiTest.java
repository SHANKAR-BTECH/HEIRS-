package com.heirs.controller;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.heirs.TestRecords;
import com.heirs.exception.StorageException;
import com.heirs.repository.*;
import com.heirs.service.*;
import com.heirs.storage.FileStorageService;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {"heirs.storage.max-file-bytes=1024", "heirs.storage.cleanup-interval-ms=3600000"})
@AutoConfigureMockMvc
@ActiveProfiles(resolver = com.heirs.TestDatabaseProfileResolver.class)
class DocumentApiTest {
  static final Path DIRECTORY;

  static {
    try {
      DIRECTORY = Files.createTempDirectory("heirs-document-test-");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @DynamicPropertySource
  static void storagePath(DynamicPropertyRegistry properties) {
    properties.add("heirs.storage.path", DIRECTORY::toString);
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired RecordRepository records;
  @MockitoSpyBean DocumentRepository documents;
  @MockitoSpyBean FileStorageService storage;
  @Autowired RecordService recordService;
  @Autowired FileCleanupService cleanup;
  @Autowired FileCleanupRepository pending;
  Long recordId;
  static final byte[] PDF =
      "%PDF-1.4\nfirst test document\n%%EOF".getBytes(StandardCharsets.US_ASCII);

  @BeforeEach
  void setup() {
    recordId = records.saveAndFlush(TestRecords.entity("DOC/TEST")).getId();
  }

  @AfterEach
  void clear() {
    reset(storage, documents);
    if (records.existsById(recordId)) recordService.deleteRecord(recordId);
    cleanup.retryPending();
    assertThat(documents.count()).isZero();
    assertThat(pending.count()).isZero();
    try (var files = Files.list(DIRECTORY)) {
      assertThat(files.count()).isZero();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @AfterAll
  static void removeDirectory() throws IOException {
    Files.delete(DIRECTORY);
  }

  MockMultipartFile pdf(String field, String name) {
    return new MockMultipartFile(field, name, "application/pdf", PDF);
  }

  JsonNode upload(int count) throws Exception {
    var request = multipart("/api/records/" + recordId + "/documents");
    for (int i = 0; i < count; i++) request.file(pdf("files", "policy.pdf"));
    return json.readTree(
        mvc.perform(request)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$", hasSize(count)))
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  String key(long id) {
    return documents.findById(id).orElseThrow().getStorageKey();
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  void uploadsAndListsMultipleDocuments(int count) throws Exception {
    var result = upload(count);
    assertThat(result.get(0).get("recordId").asLong()).isEqualTo(recordId);
    assertThat(result.get(0).has("storageKey")).isFalse();
    assertThat(result.get(0).get("uploadedAt").asText()).isNotBlank();
    mvc.perform(get("/api/records/" + recordId + "/documents"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(count)));
    for (var doc : result)
      assertThat(Files.readAllBytes(DIRECTORY.resolve(key(doc.get("id").asLong())))).isEqualTo(PDF);
  }

  @Test
  void zeroDocumentsIsValid() throws Exception {
    mvc.perform(get("/api/records/" + recordId + "/documents"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  void addsLaterAndAllowsDuplicateOriginalNames() throws Exception {
    long first = upload(1).get(0).get("id").asLong();
    long second = upload(1).get(0).get("id").asLong();
    assertThat(key(first)).isNotEqualTo(key(second));
    mvc.perform(get("/api/records/" + recordId + "/documents"))
        .andExpect(jsonPath("$", hasSize(2)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"download", "preview"})
  void returnsActualBytesAndCorrectHeaders(String action) throws Exception {
    long id = upload(1).get(0).get("id").asLong();
    mvc.perform(get("/api/documents/" + id + "/" + action))
        .andExpect(status().isOk())
        .andExpect(content().bytes(PDF))
        .andExpect(content().contentType("application/pdf"))
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    org.hamcrest.Matchers.startsWith(
                        action.equals("preview") ? "inline;" : "attachment;")))
        .andExpect(header().string("Content-Disposition", containsString("policy.pdf")))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"));
  }

  @Test
  void replacesOnlySecondAndKeepsItsIdAndUploadTime() throws Exception {
    var original = upload(3);
    long id = original.get(1).get("id").asLong();
    String oldKey = key(id);
    mvc.perform(
            multipart("/api/documents/" + id + "/replace")
                .file(pdf("file", "amendment.pdf"))
                .with(
                    r -> {
                      r.setMethod("PUT");
                      return r;
                    }))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.originalFileName").value("amendment.pdf"))
        .andExpect(jsonPath("$.uploadedAt").value(original.get(1).get("uploadedAt").asText()));
    assertThat(storage.exists(oldKey)).isFalse();
    assertThat(storage.exists(key(id))).isTrue();
    assertThat(documents.count()).isEqualTo(3);
    for (int index : new int[] {0, 2}) {
      long sibling = original.get(index).get("id").asLong();
      assertThat(documents.findById(sibling).orElseThrow().getOriginalFileName())
          .isEqualTo("policy.pdf");
      assertThat(storage.exists(key(sibling))).isTrue();
    }
  }

  @Test
  void deletesOnlyOneAndPreservesParentAndSiblings() throws Exception {
    var uploaded = upload(3);
    long id = uploaded.get(2).get("id").asLong();
    String oldKey = key(id);
    mvc.perform(delete("/api/documents/" + id)).andExpect(status().isNoContent());
    assertThat(documents.existsById(id)).isFalse();
    assertThat(storage.exists(oldKey)).isFalse();
    assertThat(records.existsById(recordId)).isTrue();
    assertThat(documents.count()).isEqualTo(2);
    for (int i = 0; i < 2; i++)
      assertThat(storage.exists(key(uploaded.get(i).get("id").asLong()))).isTrue();
  }

  @Test
  void deletingRecordRemovesAllMetadataAndFiles() throws Exception {
    upload(3);
    mvc.perform(delete("/api/records/" + recordId)).andExpect(status().isNoContent());
    assertThat(documents.count()).isZero();
    try (var files = Files.list(DIRECTORY)) {
      assertThat(files.count()).isZero();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"program.exe", "../policy.pdf", "C:\\policy.pdf", "bad\r\n.pdf"})
  void rejectsUnsafeNamesAndExtensions(String name) throws Exception {
    mvc.perform(multipart("/api/records/" + recordId + "/documents").file(pdf("files", name)))
        .andExpect(status().isBadRequest());
    assertThat(documents.count()).isZero();
  }

  @Test
  void rejectsMimeMismatch() throws Exception {
    mvc.perform(
            multipart("/api/records/" + recordId + "/documents")
                .file(
                    new MockMultipartFile("files", "policy.pdf", "application/octet-stream", PDF)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsSpoofedPdfSignature() throws Exception {
    mvc.perform(
            multipart("/api/records/" + recordId + "/documents")
                .file(
                    new MockMultipartFile(
                        "files", "policy.pdf", "application/pdf", "not pdf".getBytes())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsEmptyFile() throws Exception {
    mvc.perform(
            multipart("/api/records/" + recordId + "/documents")
                .file(new MockMultipartFile("files", "empty.pdf", "application/pdf", new byte[0])))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsMissingFilesPart() throws Exception {
    mvc.perform(multipart("/api/records/" + recordId + "/documents"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsOversizedFile() throws Exception {
    mvc.perform(
            multipart("/api/records/" + recordId + "/documents")
                .file(
                    new MockMultipartFile("files", "large.pdf", "application/pdf", new byte[1025])))
        .andExpect(status().isPayloadTooLarge());
  }

  @Test
  void rejectsEntireBatchBeforeAnyStorage() throws Exception {
    mvc.perform(
            multipart("/api/records/" + recordId + "/documents")
                .file(pdf("files", "valid.pdf"))
                .file(pdf("files", "invalid.exe")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("invalid.exe")));
    verify(storage, never()).store(any());
    assertThat(documents.count()).isZero();
  }

  @Test
  void missingRecord() throws Exception {
    mvc.perform(get("/api/records/999999/documents")).andExpect(status().isNotFound());
    mvc.perform(multipart("/api/records/999999/documents").file(pdf("files", "policy.pdf")))
        .andExpect(status().isNotFound());
  }

  @Test
  void missingDocument() throws Exception {
    mvc.perform(get("/api/documents/999999/download")).andExpect(status().isNotFound());
    mvc.perform(get("/api/documents/999999/preview")).andExpect(status().isNotFound());
    mvc.perform(delete("/api/documents/999999")).andExpect(status().isNotFound());
    mvc.perform(
            multipart("/api/documents/999999/replace")
                .file(pdf("file", "policy.pdf"))
                .with(
                    r -> {
                      r.setMethod("PUT");
                      return r;
                    }))
        .andExpect(status().isNotFound());
  }

  @Test
  void missingPhysicalFileReturns404() throws Exception {
    long id = upload(1).get(0).get("id").asLong();
    storage.delete(key(id));
    mvc.perform(get("/api/documents/" + id + "/download")).andExpect(status().isNotFound());
  }

  @Test
  void secondStorageFailureRollsBackWholeBatch() throws Exception {
    doCallRealMethod()
        .doThrow(new StorageException(new IOException("test storage failure")))
        .when(storage)
        .store(any());
    mvc.perform(
            multipart("/api/records/" + recordId + "/documents")
                .file(pdf("files", "one.pdf"))
                .file(pdf("files", "two.pdf")))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message", not(containsString(DIRECTORY.toString()))));
    assertThat(documents.count()).isZero();
  }

  @Test
  void databaseFailureCleansStoredBatch() throws Exception {
    doThrow(new IllegalStateException("test DB failure")).when(documents).saveAllAndFlush(any());
    mvc.perform(
            multipart("/api/records/" + recordId + "/documents")
                .file(pdf("files", "one.pdf"))
                .file(pdf("files", "two.pdf")))
        .andExpect(status().isInternalServerError());
    assertThat(documents.count()).isZero();
  }

  @Test
  void replacementStorageFailurePreservesOriginal() throws Exception {
    long id = upload(1).get(0).get("id").asLong();
    String oldKey = key(id);
    doThrow(new StorageException(new IOException("test"))).when(storage).store(any());
    mvc.perform(
            multipart("/api/documents/" + id + "/replace")
                .file(pdf("file", "new.pdf"))
                .with(
                    r -> {
                      r.setMethod("PUT");
                      return r;
                    }))
        .andExpect(status().isInternalServerError());
    assertThat(key(id)).isEqualTo(oldKey);
    assertThat(storage.exists(oldKey)).isTrue();
  }

  @Test
  void replacementDatabaseFailurePreservesOriginalAndCleansNewFile() throws Exception {
    long id = upload(1).get(0).get("id").asLong();
    String oldKey = key(id);
    doThrow(new IllegalStateException("test DB failure")).when(documents).flush();
    mvc.perform(
            multipart("/api/documents/" + id + "/replace")
                .file(pdf("file", "new.pdf"))
                .with(
                    r -> {
                      r.setMethod("PUT");
                      return r;
                    }))
        .andExpect(status().isInternalServerError());
    assertThat(key(id)).isEqualTo(oldKey);
    try (var files = Files.list(DIRECTORY)) {
      assertThat(files.count()).isEqualTo(1);
    }
  }

  @Test
  void deleteDatabaseFailurePreservesFileAndMetadata() throws Exception {
    long id = upload(1).get(0).get("id").asLong();
    String oldKey = key(id);
    doThrow(new IllegalStateException("test DB failure")).when(documents).flush();
    mvc.perform(delete("/api/documents/" + id)).andExpect(status().isInternalServerError());
    assertThat(documents.existsById(id)).isTrue();
    assertThat(storage.exists(oldKey)).isTrue();
    assertThat(pending.count()).isZero();
  }

  @Test
  void filesystemDeleteFailureHasDurableRetry() throws Exception {
    long id = upload(1).get(0).get("id").asLong();
    String oldKey = key(id);
    doThrow(new StorageException(new IOException("file locked"))).when(storage).delete(oldKey);
    mvc.perform(delete("/api/documents/" + id)).andExpect(status().isNoContent());
    assertThat(documents.existsById(id)).isFalse();
    assertThat(pending.existsById(oldKey)).isTrue();
    assertThat(storage.exists(oldKey)).isTrue();
    reset(storage);
    cleanup.retryPending();
    assertThat(storage.exists(oldKey)).isFalse();
    assertThat(pending.count()).isZero();
  }
}
