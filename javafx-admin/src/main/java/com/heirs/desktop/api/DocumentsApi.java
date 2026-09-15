package com.heirs.desktop.api;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.heirs.desktop.dto.DocumentResponse;

/**
 * Client for the HEIRS Supporting Documents REST API.
 *
 * <p>Actual backend contract (Spring Boot):
 * <ul>
 *   <li>GET  {@code /api/records/{recordId}/documents}          → list</li>
 *   <li>POST {@code /api/records/{recordId}/documents}          → upload (multipart field {@code files})</li>
 *   <li>PUT  {@code /api/documents/{id}/replace}                → replace (multipart field {@code file})</li>
 *   <li>DELETE {@code /api/documents/{id}}                       → delete (204)</li>
 *   <li>GET  {@code /api/documents/{id}/preview}                 → inline PDF bytes</li>
 *   <li>GET  {@code /api/documents/{id}/download}                → attachment PDF bytes</li>
 * </ul>
 */
public class DocumentsApi {

    private final ApiClient apiClient;

    public DocumentsApi() {
        this(new ApiClient());
    }

    public DocumentsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public List<DocumentResponse> getDocumentsForRecord(Long recordId) {
        if (recordId == null) {
            return Collections.emptyList();
        }
        try {
            return apiClient.get("/api/records/" + recordId + "/documents",
                    new TypeReference<List<DocumentResponse>>() {});
        } catch (ApiException e) {
            if (e.getStatusCode() == 404) {
                return Collections.emptyList();
            }
            throw e;
        }
    }

    public List<DocumentResponse> uploadDocuments(Long recordId, List<Path> files) {
        requireId(recordId);
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Select at least one PDF document to upload");
        }
        try {
            MultipartRequestBuilder builder = MultipartRequestBuilder.create();
            for (Path file : files) {
                builder.addFile("files", file);
            }
            MultipartBody body = builder.build();
            return apiClient.postMultipart("/api/records/" + recordId + "/documents", body,
                    new TypeReference<List<DocumentResponse>>() {});
        } catch (IOException e) {
            throw ApiException.clientError("Unable to read the selected file: " + e.getMessage(), e);
        }
    }

    public DocumentResponse replaceDocument(Long documentId, Path file) {
        requireId(documentId);
        if (file == null) {
            throw new IllegalArgumentException("A replacement PDF file is required");
        }
        try {
            MultipartBody body = MultipartRequestBuilder.create().addFile("file", file).build();
            return apiClient.putMultipart("/api/documents/" + documentId + "/replace", body,
                    new TypeReference<DocumentResponse>() {});
        } catch (IOException e) {
            throw ApiException.clientError("Unable to read the replacement file: " + e.getMessage(), e);
        }
    }

    public void deleteDocument(Long documentId) {
        requireId(documentId);
        apiClient.delete("/api/documents/" + documentId);
    }

    public BinaryResponse previewDocument(Long documentId, String fallbackFileName) {
        requireId(documentId);
        return apiClient.getBinary("/api/documents/" + documentId + "/preview", fallbackFileName);
    }

    public BinaryResponse downloadDocument(Long documentId, String fallbackFileName) {
        requireId(documentId);
        return apiClient.getBinary("/api/documents/" + documentId + "/download", fallbackFileName);
    }

    private static void requireId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("A positive document ID is required");
        }
    }
}