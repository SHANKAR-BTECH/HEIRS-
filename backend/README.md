# HEIRS backend

Independent REST/JSON API for the integrated React web client and a future JavaFX client. Includes local supporting-document management. Authentication, JavaFX, and cloud storage are outside this phase.

## Toolchain

- Java **21** (verified with Oracle JDK **21.0.10**; no preview features).
- Spring Boot **3.5.16**, Maven **3.9.11** via the included official Maven Wrapper.
- MySQL **8.4 LTS** (verified with **8.4.9**).
- Runtime dependencies: Spring Web, Spring Data JPA, Bean Validation, MySQL Connector/J.
- Test dependencies: Spring Boot Starter Test (JUnit, Mockito, MockMvc, AssertJ) and **test-only H2** for a standalone default test suite. The same integration tests can run against real MySQL.

Spring Boot 3.5 supports Java 21: [official requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html). Dependency versions are managed by the Spring Boot parent. No Lombok, MapStruct, Docker, security framework, or additional infrastructure.

## MySQL setup

Install MySQL Community Server and start it. From an administrator's MySQL session:

```sql
CREATE DATABASE heirs_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'heirs'@'localhost' IDENTIFIED BY 'replace-with-your-local-password';
GRANT ALL PRIVILEGES ON heirs_db.* TO 'heirs'@'localhost';
```

The password above is a placeholder. Supply your actual password through the local environment; do not commit it.

From PowerShell, in `backend/`:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/heirs_db'
$env:DB_USERNAME = 'heirs'
$env:DB_PASSWORD = '<your-local-password>'
$env:SEED_ENABLED = 'true'
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

If Maven is already installed, `mvn clean test` and `mvn spring-boot:run` are equivalent. On macOS/Linux use `sh ./mvnw`. First use requires internet access to download Maven and dependencies. Set `JAVA_HOME` to your JDK 21 installation if needed.

Open <http://127.0.0.1:8080/api/health>. Expected response: `{"status":"UP","database":"UP"}` (JSON key order is irrelevant). Ctrl+C stops the application. The default address is loopback because this phase has no authentication.

### Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/heirs_db` | JDBC connection URL |
| `DB_USERNAME` | `root` | Database login; prefer a dedicated user |
| `DB_PASSWORD` | empty | Database password |
| `SERVER_ADDRESS` | `127.0.0.1` | HTTP bind address |
| `SERVER_PORT` | `8080` | HTTP port |
| `DDL_AUTO` | `update` | Development schema initialization |
| `SEED_ENABLED` | `false` | Set `true` to insert missing sample records |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | Comma-separated allowed browser origins |
| `TEST_DB_URL` | `jdbc:mysql://127.0.0.1:3307/heirs_test` | Dedicated schema for opt-in MySQL tests |

Spring Boot reads process environment variables; it does not automatically load `.env` files. `ddl-auto=update` is for development. Adopt controlled database migrations and `validate` before production.

### Isolated instance prepared in this workspace

Verification used portable MySQL at `backend/.tools/mysql-8.4.9-winx64`, bound only to `127.0.0.1:3307`, with data under `backend/.local/mysql-data`. It does not install a Windows service or change an existing database. Generated credentials are in ignored `.local/environment.json` and `.local/client.cnf`; they are not part of the source distribution. `.tools/`, `.local/`, and `target/` are ignored by Git.

To restart this existing isolated MySQL installation, from `backend/` in one terminal:

```powershell
.\.tools\mysql-8.4.9-winx64\bin\mysqld.exe --defaults-file="$((Resolve-Path .local/my.ini).Path)" --console
```

In another terminal, load the generated environment and run the backend:

```powershell
$heirsLocalEnv = Get-Content .local/environment.json | ConvertFrom-Json
$heirsLocalEnv.psobject.Properties | ForEach-Object {
    [Environment]::SetEnvironmentVariable($_.Name, $_.Value, 'Process')
}
.\.tools\apache-maven-3.9.11\bin\mvn.cmd '-Dmaven.repo.local=.tools/repository' spring-boot:run
```

Do not start a second copy while these ports are in use. Ctrl+C stops each foreground process. For a fresh clone use the normal MySQL setup above; portable binaries, data, and credentials are intentionally not committed.

## Architecture and files

```text
backend/
  pom.xml, mvnw, mvnw.cmd, .mvn/wrapper/
  README.md, API.md, VERIFICATION.md, api.http
  src/main/java/com/heirs/
    HeirsApplication.java
    controller/       RecordController, CategoryController, HealthController
    service/          RecordService
    repository/       RecordRepository
    entity/           Record, Category, RecordStatus
    dto/              CreateRecordRequestDto, UpdateRecordRequestDto,
                      RecordResponseDto, RecordSearchRequestDto,
                      CategoryResponseDto, PageResponseDto, ApiErrorDto
    mapper/           RecordMapper
    specification/    RecordSpecifications
    exception/        RecordNotFoundException, DuplicateReferenceNumberException,
                      GlobalExceptionHandler
    config/           WebConfig, RecordSeeder
  src/main/resources/
    application.properties
    seed/records.json
  src/test/java/com/heirs/
    service/RecordServiceTest.java
    repository/RecordRepositoryTest.java
    controller/RecordApiTest.java
    SeedDataTest.java, TestRecords.java, TestDatabaseProfileResolver.java
  src/test/resources/
    application-test.properties, application-mysql-test.properties
```

Controllers validate transport inputs and return DTOs. The transactional service applies business rules and mapping. The repository extends `JpaRepository<Record, Long>` and `JpaSpecificationExecutor<Record>`; it includes reference lookup and uniqueness checks. Hibernate maps the entity to `heirs_records`, with an auto-generated numeric primary key, unique reference number, filter indexes, non-null constraints, and a year range constraint. Audit timestamps are maintained in UTC internally and are not exposed in the current DTO.

`RecordSpecifications` ORs case-insensitive keyword matches across title, description, keywords, department, and reference number; all supplied filters are then ANDed. Department filtering is a case-insensitive exact match. Keyword `%`, `_`, and `!` characters are escaped so they are literal input. Results are consistently ordered by ascending ID. Substring searches use SQL `LIKE`; leading wildcards can scan rows, so assess query plans and indexing as the catalogue grows.

Reference numbers are trimmed, explicitly supplied, and never generated. PUT can explicitly correct one, subject to uniqueness. With the documented MySQL collation, reference uniqueness is case-insensitive. DTO checks provide early duplicate feedback; the database unique constraint also protects against concurrent duplicates.

## API

See [API.md](API.md) for the exact client contract and [api.http](api.http) for executable REST-client examples.

| Method | Endpoint | Result |
|---|---|---|
| GET | `/api/health` | Database readiness, 200 or 503 |
| GET | `/api/records?page=0&size=20` | Paginated records |
| GET | `/api/records/{id}` | One record or 404 |
| GET | `/api/records/search` | Paginated optional keyword/filter search |
| GET | `/api/categories` | Five category codes and labels |
| POST | `/api/records` | Create; 201 with `Location` |
| PUT | `/api/records/{id}` | Full replacement; 200 |
| DELETE | `/api/records/{id}` | Delete; 204 |

Example PowerShell requests:

```powershell
Invoke-RestMethod 'http://127.0.0.1:8080/api/records'
Invoke-RestMethod 'http://127.0.0.1:8080/api/records/search?q=digital&category=Policy&status=Active'
```

## Seed records

`SEED_ENABLED=true` runs a transactional JSON seeder with Bean Validation. It imports **60** representative records copied from the existing mock catalogue, preserving titles, reference numbers, categories, and statuses. Original display dates become ISO dates; keyword arrays become comma-separated strings. Numeric IDs are newly generated database keys and are not frontend mock IDs.

Counts: **19 Policies, 12 Schemes, 9 Regulations, 10 Projects, 10 Rules**. Records span multiple years, departments, and all four statuses. Startup inserts only missing references and never overwrites an existing record. Repeated startup adds zero rows if all samples exist. Deleted sample references will be reinserted on a later seeded startup; disable seeding once the initial catalogue is loaded. Development seeding assumes one application instance starts at a time.

## Tests

```powershell
.\mvnw.cmd clean test
```

The default suite uses H2 only in test scope. Service tests use Mockito; repository tests exercise real Specifications, constraints, and timestamps; API tests exercise the complete controller/service/repository stack with MockMvc. Seed tests verify all 60 records and preservation of edits.

To run the same suite on MySQL, create a **disposable database named `heirs_test`** and grant your test user access:

```sql
CREATE DATABASE heirs_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
GRANT ALL PRIVILEGES ON heirs_test.* TO 'heirs'@'localhost';
```

```powershell
$env:TEST_DB_URL = 'jdbc:mysql://127.0.0.1:3307/heirs_test'
# Set DB_USERNAME and DB_PASSWORD for the test user, or load the isolated environment above.
.\mvnw.cmd '-Dheirs.test.mysql=true' clean test
```

This opt-in profile uses **create-drop** and destroys tables in `heirs_test`. A profile resolver rejects `TEST_DB_URL` values naming another schema. It never uses the application `DB_URL`. Do not run multiple test suites simultaneously against the same test schema.

See [VERIFICATION.md](VERIFICATION.md) for actual build, MySQL, HTTP, and Git verification results.

## Supporting documents

A record supports zero, one, or many PDF documents. MySQL stores the `heirs_documents` metadata table, with a foreign key and index on `record_id`. Each document has an ID, lazy parent relationship, original filename, unique storage key, MIME type, byte size, and UTC upload/update timestamps. The storage key also serves as the generated stored filename; no redundant filename field or file bytes are stored in MySQL. The inverse `Record.documents` association is lazy and has no automatic cascade: `RecordService` explicitly routes deletion through `DocumentService` so file cleanup cannot be skipped by JPA cascading. REST responses use DTOs and never expose storage keys or filesystem paths.

`DocumentService` depends on `FileStorageService` (`store`, `load`, `exists`, `delete`). `LocalFileStorageService` stores UUID-named files in a directory accessed only through document-ID endpoints. Replacing a file is implemented by storing a new object and scheduling deletion of the old key after the metadata commit. There is no arbitrary static file mount.

From `backend/`, the default location is `backend/local-storage/`, ignored by Git. Relative paths resolve against the server's working directory. For a stable location independent of working directory, configure an absolute path:

```powershell
$env:HEIRS_STORAGE_PATH = 'C:\Users\Shank\Higher-Education-Information-Retrieval-System\backend\local-storage'
.\mvnw.cmd spring-boot:run
```

| Setting | Default | Meaning |
|---|---|---|
| `HEIRS_STORAGE_PATH` / `heirs.storage.path` | `local-storage` | Local file directory; auto-created |
| `HEIRS_MAX_FILE_BYTES` / `heirs.storage.max-file-bytes` | `20971520` | **20 MiB** per document; environment variable also configures the multipart parser |
| `HEIRS_MAX_REQUEST_BYTES` | `104857600` | **100 MiB** total multipart request, including multipart overhead |
| `heirs.storage.cleanup-interval-ms` | `60000` | Retry pending file removals every minute |

When configuring the per-file Spring property directly, also set `spring.servlet.multipart.max-file-size` consistently. Use the environment variable to set both together. Keep a custom storage directory out of source control. Back up the database and storage together; both are necessary to restore attachments. Restarting the application does not clear either.

Only `.pdf` with MIME type `application/pdf` and a `%PDF-` content signature is accepted. Validation also rejects empty files, files exceeding the configured size, and unsafe names (path separators, `..`, control/format characters, Windows-reserved punctuation, blank names, or more than 180 characters). A PDF signature check is not full PDF parsing or malware scanning. Duplicate human-readable filenames are allowed because storage keys are unique. All batch files are validated before persistence; an invalid member rejects the entire batch and names the invalid file where safe. Transport-level size rejection returns 413 before application validation and may not identify the offending member.

Uploads append to existing attachments. Replacement preserves the ID and original upload timestamp, updates filename/size/update timestamp, and leaves siblings intact. Document deletion preserves the parent and siblings. Permanent record deletion removes all its document metadata and schedules all stored files for deletion. Mutations lock the parent row to serialize competing uploads, replacements, and deletion.

Newly stored files are removed on transaction rollback. Old-file deletion is recorded in the durable `heirs_file_cleanup` table in the same transaction as metadata changes, then attempted immediately after commit. If a physical deletion fails (for example a Windows file lock), the API mutation remains committed, the failure is logged, and its cleanup job survives restart and retries every minute. Such files are no longer reachable through document endpoints. Rollback cleanup also records a retry if physical removal fails. A database and filesystem are not one atomic transaction: abrupt process termination during a new upload can leave an unreferenced file before rollback callbacks run; a simultaneous database outage and rollback-cleanup failure requires operator reconciliation. No silent claim of distributed atomicity is made.

The React Edit modal includes document listing, multi-file selection with counts, upload status, preview, download, single-file replacement, and confirmation before removal. Creating record metadata keeps the modal open for document attachment. Document operations are saved independently of metadata; canceling metadata edits does not undo completed attachment changes. Record Details uses the same live document list without management controls.

For a future cloud adapter, implement `FileStorageService` with opaque immutable keys, streamed `Resource` loading, partial-write cleanup, and idempotent deletion. Replace the selected storage bean; controllers, DTOs, document service, record logic, and clients need no changes. No cloud service is configured here.

## Next phase

Verify the local document flows with users, then add authentication and role-based authorization for administrator mutations, controlled database migrations, and backup/recovery procedures. Keep cloud storage and JavaFX as separate later phases.
