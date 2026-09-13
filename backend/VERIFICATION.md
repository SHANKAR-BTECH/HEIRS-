# Phase 2 implementation and verification report

Verified on **2026-09-13**, on the supplied Windows workspace.

| # | Requested report item | Result |
|---|---|---|
| 1 | Java | Oracle JDK 21.0.10 LTS; target Java 21, no preview features |
| 2 | Spring Boot | 3.5.16 |
| 3 | Dependencies | Spring Web, Data JPA, Validation, MySQL Connector/J; Spring Boot Starter Test and test-only H2; no Lombok |
| 4 | File structure | New standalone `backend/`; controller, service, repository, entity, dto, mapper, specification, exception, config packages; resources and tests; official Maven Wrapper; see README tree |
| 5 | Entities | `Record`, table `heirs_records`, generated Long ID, full domain fields, internal UTC createdAt/updatedAt |
| 6 | Enums | `Category`: POLICY/SCHEME/REGULATION/PROJECT/RULES; `RecordStatus`: ACTIVE/DRAFT/COMPLETED/ARCHIVED; title-case JSON labels |
| 7 | DTOs | RecordResponseDto, CreateRecordRequestDto, UpdateRecordRequestDto, RecordSearchRequestDto, CategoryResponseDto, PageResponseDto, ApiErrorDto |
| 8 | Repositories | RecordRepository: JpaRepository + JpaSpecificationExecutor, reference lookup and duplicate checks |
| 9 | Services | Transactional RecordService with getAllRecords, getRecordById, searchRecords, createRecord, updateRecord, deleteRecord, getCategories |
| 10 | Controllers | RecordController, CategoryController, HealthController |
| 11 | Endpoints | GET health, records, record by ID, search, categories; POST records, PUT record by ID, DELETE record by ID |
| 12 | Validation | Required nonblank title/department/reference, required category/status, bounded strings, optional year 1900?2100, valid ISO date parsing, positive IDs, bounded pagination, enum checks |
| 13 | Exceptions | RecordNotFoundException, DuplicateReferenceNumberException, GlobalExceptionHandler; consistent application errors for validation, malformed inputs, conflicts, missing resources, methods/media types, unexpected failures |
| 14 | Search | JPA Specifications: keyword OR across five fields, AND optional category/year/status/department filters; case-insensitive, escaped LIKE input; stable ascending-ID pagination |
| 15 | MySQL connection | SUCCESS: isolated MySQL Community Server 8.4.9 at 127.0.0.1:3307; heirs_db and separate disposable heirs_test; JDBC and real SQL verified |
| 16 | Seeds | 60 records: 19 Policy, 12 Scheme, 9 Regulation, 10 Project, 10 Rules; original references retained; repeated seed inserts zero and preserves edits |
| 17 | Tests added | 11 service tests, 30 full-stack MockMvc API cases, 16 repository/search cases, 1 seed repeatability test; 58 total |
| 18 | Maven result | `clean test`: BUILD SUCCESS, 58 tests, 0 failures, 0 errors, 0 skipped; full suite also passed against MySQL with the same totals |
| 19 | Runtime | `spring-boot:run` started successfully on 127.0.0.1:8080; all nine requested GET requests passed; live create/update/delete and error requests passed |
| 20 | Frontend | Untouched by this work; three pre-existing frontend modifications preserved byte-for-byte in the Git diff; only backend/ added; no commit or push |
| 21 | Next phase | Integrate the React data-access layer using API.md; then document upload/download with a separate Document model/service; JavaFX remains future work |

## Build evidence

Maven was not initially on PATH. An official Maven 3.9.11 distribution was downloaded inside ignored `backend/.tools/`, its SHA-512 checked against Maven Central, and the official Wrapper generated with a pinned distribution SHA-256.

The initial sandboxed build hit Java `AccessDeniedException` while resolving workspace paths. The approved build outside that restriction succeeded. An initial MySQL test launch could not load a PowerShell environment script because of the machine's execution policy; loading the generated JSON settings into process environment variables resolved it without changing the execution policy.

Successful runs:

- Maven `clean test` with the real MySQL profile: **58 / 0 / 0 / 0**, BUILD SUCCESS.
- Final Maven Wrapper `clean test` with default H2: **58 / 0 / 0 / 0**, BUILD SUCCESS.
- `spring-boot:run`: Spring Boot 3.5.16 started; Hikari connected to MySQL 8.4.9; Tomcat listened on 8080; 60 seeds inserted.

Local logs are ignored: `.local/mysql-test.log`, `.local/final-test.log`, `.local/runtime.log`.

## Requested live GET checks

| Request | HTTP | Observed result |
|---|---|---|
| `/api/health` | 200 | status UP, database UP |
| `/api/records` | 200 | 60 total records, default page size 20 |
| `/api/records/1` | 200 | HEIRS/POL/2026/014 |
| `/api/categories` | 200 | 5 category objects |
| `/api/records/search?q=policy` | 200 | 19 matches |
| `/api/records/search?category=Policy` | 200 | 19 matches |
| `/api/records/search?status=Active` | 200 | 42 matches |
| `/api/records/search?year=2026` | 200 | 8 matches |
| `/api/records/search?q=digital&category=Policy&status=Active` | 200 | 5 matches |

Live CRUD verification created a disposable record (201 with Location), repeated its reference (409), updated it to Completed (200), deleted it (204), and fetched the deleted ID (404). Empty POST returned 400 with field errors; an invalid category returned 400. Cleanup left **60 records** in heirs_db. The auto-increment sequence may have a gap from this verification, which is expected.

## Database evidence

`SELECT VERSION()` returned **8.4.9**. `SELECT COUNT(*) FROM heirs_records` returned **60**. `SHOW CREATE TABLE heirs_records` confirmed:

- Primary key and generated bigint ID.
- Non-null title, department, reference number, category, status, and audit timestamps.
- Unique index `uk_records_reference` on `reference_number`.
- Indexes on category, status, publication_year, department.
- Check constraint limiting a non-null publication_year to 1900?2100.
- TEXT description, optional source/keywords/date/year, utf8mb4 charset.

The MySQL repository test attempted a duplicate directly, bypassing the service precheck. MySQL returned **error 1062 / SQLState 23000** for `heirs_records.uk_records_reference`, and the assertion passed.

## Git evidence

Before work and after implementation, these existing frontend paths were modified:

- frontend/src/App.css
- frontend/src/pages/ReportsPage.jsx
- frontend/src/styles/globals.css

`git diff --binary -- frontend | git hash-object --stdin` returned the same hash both times:

`fefa26fb1e45ea27b611e1e5147bd6414f08ed74`

`git status --short` added only `?? backend/`. `git diff --stat` still showed only the existing frontend changes (374 insertions, 169 deletions), since backend files remain untracked for user review. `git diff --check` passed. Git ignore checks confirmed local credentials, downloaded tooling, database files, and build outputs are excluded.

## Scope and limitations

The application remains local development software: no authentication, document storage, frontend wiring, or JavaFX. CORS is restricted to the two requested Vite origins. Production requires access control and controlled migrations. Search uses SQL substring matching and is an appropriate foundation; it is not a full-text search engine. Seeding is opt-in, intended for one development instance at a time.
