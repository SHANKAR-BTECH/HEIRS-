package com.heirs.controller;

import com.heirs.dto.DocumentResponseDto;
import com.heirs.service.DocumentService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class DocumentController {
  private final DocumentService service;

  public DocumentController(DocumentService service) {
    this.service = service;
  }

  @GetMapping("/records/{recordId}/documents")
  public List<DocumentResponseDto> list(@PathVariable Long recordId) {
    return service.getDocumentsForRecord(recordId);
  }

  @PostMapping(
      value = "/records/{recordId}/documents",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public List<DocumentResponseDto> upload(
      @PathVariable Long recordId, @RequestPart("files") List<MultipartFile> files) {
    return service.uploadDocuments(recordId, files);
  }

  @PutMapping(value = "/documents/{id}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public DocumentResponseDto replace(
      @PathVariable Long id, @RequestPart("file") MultipartFile file) {
    return service.replaceDocument(id, file);
  }

  @DeleteMapping("/documents/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.deleteDocument(id);
  }

  @GetMapping("/documents/{id}/download")
  public ResponseEntity<Resource> download(@PathVariable Long id) {
    return content(id, false);
  }

  @GetMapping("/documents/{id}/preview")
  public ResponseEntity<Resource> preview(@PathVariable Long id) {
    return content(id, true);
  }

  private ResponseEntity<Resource> content(Long id, boolean inline) {
    var content = service.content(id);
    var metadata = content.metadata();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(metadata.contentType()))
        .contentLength(metadata.fileSize())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(metadata.originalFileName(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .header("X-Content-Type-Options", "nosniff")
        .cacheControl(CacheControl.noStore())
        .body(content.resource());
  }
}
