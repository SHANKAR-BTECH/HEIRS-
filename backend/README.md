# HEIRS backend

Independent REST/JSON API for the React web client and future JavaFX desktop client. No frontend integration, JavaFX, authentication, or document storage is included in this phase.

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

## Next phase

Connect the React data-access layer to the documented contract while preserving its approved UI. Add explicit adapters for numeric IDs, ISO dates, keyword strings, and paginated responses. JavaFX can later use the same ordinary HTTP/JSON endpoints without CORS requirements or browser sessions.

Then introduce a `Document` entity linked to `Record` (id, recordId, originalFileName, storedFileName, contentType, fileSize, storagePath, uploadedAt), a separate document service/repository, and upload/download endpoints with validation and storage rules. No placeholder upload endpoints or storage logic exist yet. Add authentication/authorization before exposing writable APIs beyond trusted local development, and controlled migrations before production.
