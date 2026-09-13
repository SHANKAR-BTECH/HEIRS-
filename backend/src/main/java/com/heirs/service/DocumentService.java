package com.heirs.service;

import com.heirs.dto.DocumentResponseDto;
import com.heirs.entity.Document;
import com.heirs.entity.Record;
import com.heirs.exception.*;
import com.heirs.repository.*;
import com.heirs.storage.FileStorageService;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class DocumentService {
  private final DocumentRepository documents;
  private final RecordRepository records;
  private final FileStorageService storage;
  private final FileCleanupService cleanup;
  private final EntityManager entityManager;
  private final long maxBytes;

  public DocumentService(
      DocumentRepository documents,
      RecordRepository records,
      FileStorageService storage,
      FileCleanupService cleanup,
      EntityManager entityManager,
      @Value("${heirs.storage.max-file-bytes}") long maxBytes) {
    this.documents = documents;
    this.records = records;
    this.storage = storage;
    this.cleanup = cleanup;
    this.entityManager = entityManager;
    this.maxBytes = maxBytes;
  }

  public List<DocumentResponseDto> getDocumentsForRecord(Long recordId) {
    if (!records.existsById(recordId)) throw new RecordNotFoundException(recordId);
    return documents.findAllByRecordIdOrderByIdAsc(recordId).stream()
        .map(DocumentResponseDto::from)
        .toList();
  }

  private Document require(Long id) {
    return documents.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
  }

  private Record lockRecord(Long id) {
    return records.lockById(id).orElseThrow(() -> new RecordNotFoundException(id));
  }

  private Document lockDocument(Long id) {
    Document document = require(id);
    lockRecord(document.getRecord().getId());
    // Refresh after acquiring the parent lock: a competing replacement may have committed.
    try {
      entityManager.refresh(document);
    } catch (jakarta.persistence.EntityNotFoundException e) {
      throw new DocumentNotFoundException(id);
    }
    return document;
  }

  private String validate(MultipartFile file) {
    if (file == null) throw new InvalidDocumentException("Select at least one PDF document");
    String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").trim();
    if (name.isBlank()
        || name.length() > 180
        || name.contains("..")
        || java.util.regex.Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}\\p{Cf}]").matcher(name).find())
      throw new InvalidDocumentException(
          "Unsafe filename: use a plain filename up to 180 characters");
    if (file.isEmpty()) throw new InvalidDocumentException(name + ": file is empty");
    if (file.getSize() > maxBytes)
      throw new DocumentTooLargeException(
          name + ": exceeds the " + maxBytes + " byte per-file limit");
    if (!name.toLowerCase(Locale.ROOT).endsWith(".pdf")
        || !"application/pdf".equalsIgnoreCase(file.getContentType()))
      throw new InvalidDocumentException(
          name + ": only PDF documents (application/pdf) are supported");
    try (var input = file.getInputStream()) {
      if (!Arrays.equals(input.readNBytes(5), "%PDF-".getBytes(StandardCharsets.US_ASCII)))
        throw new InvalidDocumentException(name + ": content is not a PDF");
    } catch (IOException e) {
      throw new StorageException(e);
    }
    return name;
  }

  private void store(Document document, MultipartFile file, String name) {
    try {
      String key = storage.store(file.getInputStream());
      cleanup.onRollback(key);
      document.setFile(name, key, file.getSize());
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }

  @Transactional
  public List<DocumentResponseDto> uploadDocuments(Long recordId, List<MultipartFile> files) {
    Record record = lockRecord(recordId);
    if (files == null || files.isEmpty())
      throw new InvalidDocumentException("Select at least one PDF document");
    List<String> names = files.stream().map(this::validate).toList();
    List<Document> batch = new ArrayList<>();
    for (int i = 0; i < files.size(); i++) {
      Document document = new Document(record);
      store(document, files.get(i), names.get(i));
      batch.add(document);
    }
    return documents.saveAllAndFlush(batch).stream().map(DocumentResponseDto::from).toList();
  }

  @Transactional
  public DocumentResponseDto replaceDocument(Long id, MultipartFile file) {
    String name = validate(file);
    Document document = lockDocument(id);
    String oldKey = document.getStorageKey();
    store(document, file, name);
    documents.flush();
    cleanup.afterCommit(oldKey);
    return DocumentResponseDto.from(document);
  }

  @Transactional
  public void deleteDocument(Long id) {
    Document document = lockDocument(id);
    cleanup.afterCommit(document.getStorageKey());
    documents.delete(document);
    documents.flush();
  }

  @Transactional
  public void deleteForRecord(Long recordId) {
    lockRecord(recordId);
    var attached = documents.findAllByRecordIdOrderByIdAsc(recordId);
    attached.forEach(d -> cleanup.afterCommit(d.getStorageKey()));
    documents.deleteAll(attached);
    documents.flush();
  }

  public DocumentContent content(Long id) {
    Document document = require(id);
    if (!storage.exists(document.getStorageKey())) throw new DocumentNotFoundException(id);
    return new DocumentContent(
        DocumentResponseDto.from(document), storage.load(document.getStorageKey()));
  }

  public record DocumentContent(DocumentResponseDto metadata, Resource resource) {}
}
