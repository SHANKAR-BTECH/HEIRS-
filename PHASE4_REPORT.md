# HEIRS Phase 4 implementation report

Implemented locally from clean checkpoint `62ee6c4`. No commit, push, cloud deployment, or JavaFX work was performed. Implementation and automated/API verification are complete; real browser interaction and visual verification remain pending because the Browser runtime reported no available browsers and discovery returned an empty list.

## 1. Backend files created

Under `backend/src/main/java/com/heirs/`:

- `entity/Document.java`, `entity/FileCleanup.java`
- `dto/DocumentResponseDto.java`
- `repository/DocumentRepository.java`, `repository/FileCleanupRepository.java`
- `storage/FileStorageService.java`, `storage/LocalFileStorageService.java`
- `service/DocumentService.java`, `service/FileCleanupService.java`
- `controller/DocumentController.java`
- `exception/DocumentNotFoundException.java`, `InvalidDocumentException.java`, `DocumentTooLargeException.java`, `StorageException.java`

New tests: `backend/src/test/java/com/heirs/controller/DocumentApiTest.java` and `backend/src/test/java/com/heirs/storage/LocalFileStorageServiceTest.java`.

## 2. Backend files modified

- `entity/Record.java`: inverse document relationship.
- `repository/RecordRepository.java`: parent-row locking for attachment mutations and parent deletion.
- `service/RecordService.java`: explicit document cleanup before permanent record deletion.
- `exception/GlobalExceptionHandler.java`: consistent document/multipart errors.
- `src/main/resources/application.properties`: storage and multipart limits.
- `src/test/java/com/heirs/service/RecordServiceTest.java`: updated constructor wiring and deletion expectations; all original test cases retained.
- `backend/mvnw.cmd`: fixed null `Target` handling for an ordinary Maven cache directory on Windows. This was necessary to run the requested wrapper command.
- `backend/README.md`, `backend/API.md`: storage architecture, configuration, endpoints, examples, validation, cleanup semantics, limitations, future adapter guidance.
- Root `.gitignore`: excludes default upload directories.

## 3. Document entity

`id`, `record`, `originalFileName`, `storageKey`, `contentType`, `fileSize`, `uploadedAt`, `updatedAt`. The unique UUID storage key also serves as the physical stored filename, avoiding a redundant field. Timestamps are UTC with microsecond precision matching database persistence. No file bytes are stored in MySQL.

## 4. JPA relationship

One `Record` has zero to many `Document` rows. Each document has one non-null lazy `@ManyToOne` parent with a foreign key and `record_id` index. `Record` declares lazy `@OneToMany(mappedBy="record")`. Explicit service deletion handles file cleanup; no automatic cascade can silently bypass it. Controllers expose DTOs, not entities.

## 5. Storage abstraction

`FileStorageService` exposes `store(InputStream)`, `load(key)`, `exists(key)`, and idempotent `delete(key)`. Replacement composes a new immutable stored object and deferred deletion of the old key. A future cloud implementation can replace the storage bean without changing controllers, document service, record logic, or clients.

## 6. Local storage configuration

Default `local-storage`, relative to backend working directory: normally `backend/local-storage/`. Configure an absolute location with `HEIRS_STORAGE_PATH` or `heirs.storage.path`. Files survive application restarts. Default directories are Git-ignored. Files are served only through document-ID APIs.

## 7. Document endpoints

| Method | Path | Result |
|---|---|---|
| POST | `/api/records/{recordId}/documents` | 201, uploaded document DTO array |
| GET | `/api/records/{recordId}/documents` | 200, ordered list or `[]` |
| GET | `/api/documents/{documentId}/preview` | PDF bytes, inline disposition |
| GET | `/api/documents/{documentId}/download` | PDF bytes, original filename attachment |
| PUT | `/api/documents/{documentId}/replace` | Updated document DTO, same ID |
| DELETE | `/api/documents/{documentId}` | 204, specific document removal |

## 8. Multi-upload

One multipart POST accepts repeated `files` fields. The entire batch is validated before storage. All metadata is persisted in one transaction; normal storage/database failures roll back the batch and remove new files. Later POSTs append. Duplicate original filenames are allowed and receive different storage keys.

## 9. Validation

Reject empty files, unsupported extension/MIME type, non-PDF signature, oversized files, blank or overlong names, path separators, `..`, control/format characters, and unsafe Windows filename punctuation. Require `%PDF-` at the start. Return consistent 400/404/413/415/500 JSON errors without local paths. Signature validation is not full PDF parsing or malware scanning.

## 10. Size limits

Default **20 MiB (20,971,520 bytes) per file**, configurable with `HEIRS_MAX_FILE_BYTES`. Total multipart request default **100 MiB (104,857,600 bytes), including overhead**, configurable with `HEIRS_MAX_REQUEST_BYTES`. The real servlet upload limit was verified over HTTP with a file one byte over 20 MiB: 413, no attachments added.

## 11. Supported types

PDF only: `.pdf` and `application/pdf`. Office formats and arbitrary executable uploads are not supported.

## 12. Replacement

Preserves document ID and original upload time; updates filename, key, byte size, and update time. New-storage or metadata failure preserves the original. Successful replacement commits an old-file cleanup job and attempts removal immediately. Sibling documents remain unchanged.

## 13. Document deletion

Removes only the selected document's metadata; preserves parent and siblings. Physical removal is attempted immediately after commit. If it fails, a durable `heirs_file_cleanup` job remains, the failure is logged, and cleanup retries every minute. The removed attachment is no longer available through APIs.

## 14. Parent record deletion

Locks the parent, removes all attachment metadata, records all cleanup jobs, and deletes the record in one database transaction. Stored files are removed after commit using the same durable retry mechanism. Parent deletion with three attachments and physical cleanup passed automated tests and direct API verification.

## 15. Frontend API additions

`frontend/src/api/documentsApi.js` supplies list, multi-upload, replacement, deletion, and preview/download URL helpers. `apiClient.js` now recognizes `FormData` without setting Content-Type or JSON-encoding it. No new dependency was added.

## 16. Record Details

Replaced fabricated supporting documents and placeholder download alerts with the shared live `SupportingDocuments` component. Every document shows filename, type, size, upload/update time, Preview, and Download. Removed the unused fake-document helper from `searchUtils.js`.

## 17. Edit Record

Added Supporting Documents below metadata fields. Existing documents have Preview, Download, Replace, and Remove. Removal requires an inline confirmation naming the file. Attachment operations preserve metadata edits and update the list without a page reload. Busy operations prevent repeated mutations and modal closure. After Add Record succeeds, the modal remains open with the new ID so documents can be attached safely.

## 18. Multi-file selector

Labeled file input uses `multiple` and PDF acceptance hints. Shows selected names, sizes, count, and `Upload N Documents`. Replacement opens a single-file picker for one document. Document changes save independently of record metadata, as stated beside the selector.

## 19. Empty/loading/error states

Exact empty copy: “No supporting documents are currently attached to this record.” Existing `LoadingState` and `InlineError` components handle loading, retryable list errors, action errors, and upload/replace/remove status. Success is announced via `role="status"`. Buttons and links identify the document in accessible names. The modal focus trap now includes links and excludes hidden/disabled inputs.

## 20. Backend tests added

**32 new test executions**: 29 document API cases and 3 local storage cases. Coverage includes one/two/three-file batches, list/zero state, append/duplicate names, actual download/preview bytes and headers, stable-ID replacement, sibling-preserving deletion, parent deletion, unsafe names, MIME/signature/empty/missing/oversized files, invalid batch rejection before storage, missing records/documents/files, second-write failure, database-save failure, replacement storage/DB failure, deletion DB failure, durable removal retry, storage restart persistence, path traversal rejection, and partial-write cleanup.

## 21. Total backend tests

**90 passed, 0 failures, 0 errors, 0 skipped** using default H2 tests. The same **90 passed on MySQL 8.4.9** using the dedicated `heirs_test` schema. All original 58 tests remain passing. Commands used the Maven wrapper and existing workspace cache:

```powershell
$env:MAVEN_USER_HOME = (Resolve-Path .tools/wrapper-home).Path
.\mvnw.cmd '-Dmaven.repo.local=.tools/repository' test
.\mvnw.cmd '-Dmaven.repo.local=.tools/repository' '-Dheirs.test.mysql=true' test
```

Existing ignored environment settings were loaded for MySQL without printing credentials. Java/esbuild filesystem access required approved execution outside the sandbox.

## 22. Frontend lint

`npm.cmd run lint`: passed. Windows PowerShell blocks the `npm.ps1` shim; `npm.cmd` executes the same npm script without changing execution policy.

## 23. Frontend build

`npm.cmd run build`: passed; Vite compiled 1,488 modules. No dependencies, global CSS, fonts, or application layout files were changed.

## 24. Real multi-document browser test

**Not run / pending.** Browser runtime setup succeeded, but selection reported “No browser is available”; documented discovery returned `[]`. No visual screenshot QA or actual browser file-picker, native PDF viewer, or download interaction is claimed.

Direct HTTP verification against the real local MySQL application passed: create metadata; empty list; upload three distinct valid PDFs in one request; list three; preview/download all three with byte comparisons and disposition/filename checks; replace second; remove third; append a duplicate name; reject invalid batch; enforce 413; edit and search metadata; restart Spring Boot; verify retained IDs and exact bytes; delete parent; verify document 404s and empty storage. Temporary verification data was removed. Existing application record count was **59 before and after**, without reseeding or changing existing records.

The frontend is running at `http://127.0.0.1:5173/`, backend at `http://127.0.0.1:8080/`. User verification should exercise the requested Manage Records → Edit → upload three → refresh → View → preview/download → replace second → remove third → refresh sequence. Verify desktop/mobile layout and keyboard controls in that pass.

## 25. Existing record/search CRUD

Original automated record/search/filter/pagination/validation/CRUD tests pass on both databases. Direct HTTP metadata create/update/search/delete also passed while attachments existed. Existing records were preserved.

## 26. UI preservation and limitations

Preserved global colors, Questrial, typography, sidebar, navigation, glass controls, card styles, shadows, page layouts, and responsive grid. Reused existing document and button classes; new CSS only supports document controls. This is confirmed by the source diff; visual browser confirmation remains pending.

Database/filesystem operations cannot be one atomic transaction. Durable cleanup protects committed deletion/replacement and ordinary rollback paths. Abrupt process termination during a new upload can leave an unreferenced new file before rollback callbacks execute. Simultaneous database and rollback-cleanup failures require operator reconciliation. These limits and eventual cleanup semantics are documented in README/API. No cloud, authentication, or production deployment was added.

## 27. Recommended next phase

Complete user browser verification and create a separate checkpoint after approval. Then add authentication/administrator authorization, controlled schema migrations, and backup/recovery procedures before wider access. Keep cloud adapters and JavaFX as later independent phases.

Git remains uncommitted at `62ee6c4`; uploaded test PDFs and local verification scripts/logs are ignored. `git diff --check` passes.
