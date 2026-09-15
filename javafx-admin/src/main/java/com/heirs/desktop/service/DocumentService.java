package com.heirs.desktop.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.heirs.desktop.api.BinaryResponse;
import com.heirs.desktop.api.DocumentsApi;
import com.heirs.desktop.dto.DocumentResponse;
import com.heirs.desktop.model.DocumentInfo;
import com.heirs.desktop.util.FileNames;

/**
 * High-level service coordinating JavaFX document workflows with
 * {@link DocumentsApi} and converting backend DTOs into UI-safe models.
 *
 * <p>All methods are blocking and intended to be invoked off the JavaFX
 * Application Thread through {@link com.heirs.desktop.util.FxAsync}.</p>
 */
public class DocumentService {

    private final DocumentsApi documentsApi;

    public DocumentService() {
        this(new DocumentsApi());
    }

    public DocumentService(DocumentsApi documentsApi) {
        this.documentsApi = documentsApi;
    }

    public List<DocumentInfo> listDocuments(Long recordId, String relatedReference) {
        List<DocumentResponse> docs = documentsApi.getDocumentsForRecord(recordId);
        return docs.stream()
                .map(dto -> DocumentInfo.from(dto, relatedReference))
                .collect(Collectors.toList());
    }

    public List<DocumentInfo> uploadDocuments(Long recordId, String relatedReference, List<Path> files) {
        List<DocumentResponse> uploaded = documentsApi.uploadDocuments(recordId, files);
        return uploaded.stream()
                .map(dto -> DocumentInfo.from(dto, relatedReference))
                .collect(Collectors.toList());
    }

    public DocumentInfo replaceDocument(DocumentInfo existing, String relatedReference, Path file) {
        DocumentResponse replaced = documentsApi.replaceDocument(existing.getId(), file);
        return DocumentInfo.from(replaced, relatedReference);
    }

    public void deleteDocument(DocumentInfo document) {
        documentsApi.deleteDocument(document.getId());
    }

    /**
     * Fetches preview bytes from the backend and writes them to a safe
     * system-temp {@code .pdf} file. The file is registered for deletion on
     * JVM exit so no backend-storage path is ever touched directly.
     */
    public Path preparePreviewFile(DocumentInfo document) {
        BinaryResponse preview = documentsApi.previewDocument(document.getId(), document.getFileName());
        validatePdf(preview);
        try {
            Path temp = Files.createTempFile("heirs-preview-", ".pdf");
            temp.toFile().deleteOnExit();
            Files.write(temp, preview.body());
            return temp;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write the temporary preview file: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches download bytes and writes them to the user-chosen destination.
     * Callers guard against blind overwrites before calling this method.
     */
    public Path downloadTo(DocumentInfo document, Path destination) {
        BinaryResponse download = documentsApi.downloadDocument(document.getId(), document.getFileName());
        validatePdf(download);
        try {
            Files.write(destination, download.body());
            return destination;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write the downloaded file: " + e.getMessage(), e);
        }
    }

    /** Best-effort safe default name for save dialogs and temp isolation. */
    public String defaultDownloadName(DocumentInfo document) {
        return FileNames.safeFileName(document.getFileName());
    }

    private void validatePdf(BinaryResponse response) {
        if (response == null || response.isEmpty()) {
            throw new IllegalStateException("The backend returned an empty document.");
        }
        byte[] body = response.body();
        if (body.length < 5
                || body[0] != '%' || body[1] != 'P' || body[2] != 'D' || body[3] != 'F' || body[4] != '-') {
            throw new IllegalStateException("The downloaded content is not a valid PDF document.");
        }
        if (response.contentType() != null
                && !response.contentType().toLowerCase(Locale.ROOT).contains("pdf")
                && !response.contentType().toLowerCase(Locale.ROOT).startsWith("application/octet-stream")) {
            throw new IllegalStateException("Unexpected document content type: " + response.contentType());
        }
    }
}