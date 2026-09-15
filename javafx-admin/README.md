# HEIRS — JavaFX Desktop Administration

Phase **J4 — Real Supporting Document Management**.

A traditional JavaFX 21 desktop client. The MenuBar, ToolBar, TreeView, TableView, grey/white styling, plain category/status text, dialogs and bottom status bar remain the J1 interface.

## Architecture and backend requirement

```text
JavaFX controller → RecordService / DocumentService
                                       ↘
                         DocumentsApi → ApiClient → MultipartRequestBuilder
                                       ↙              REST / multipart / binary
                                   Spring Boot
                                       ↘
                                   document metadata + file storage
```

**Spring Boot must be running for reads and writes to succeed.** JavaFX uses only REST. It contains no JDBC connection, database driver, copied backend repository or SQL implementation. The client can launch offline; start Spring Boot and use Retry/Refresh. See `../backend/README.md` for the existing local backend and MySQL setup.

The API URL resolves from JVM property `heirs.api.baseUrl`, environment variable `HEIRS_API_BASE_URL`, then `http://127.0.0.1:8080`. Connect timeout is 4 seconds; request timeout is 8 seconds.

JavaFX never touches the backend storage folder directly. Every document operation — list, upload, preview/open, download, replace, delete — goes through the Spring Boot document REST API.

## Document workflows (Phase J4)

- **List:** pick a record in the Documents screen; its real metadata loads from `GET /api/records/{id}/documents`.
- **Upload multiple:** select several PDFs; they are validated locally (exists, readable, non-empty, ≤ 20 MiB, `.pdf` extension, `%PDF-` signature) then sent as one `multipart/form-data` request with repeated `files` parts to `POST /api/records/{id}/documents`. Rows appear only after the backend confirms.
- **Preview / Open:** fetch bytes from `GET /api/documents/{id}/preview`, write them to a safe system-temp `.pdf`, and hand them to the OS default viewer. No embedded viewer.
- **Download:** a save dialog proposes the safe backend filename; bytes come from `GET /api/documents/{id}/download`. Existing destinations ask before overwriting.
- **Replace one:** choose a new PDF, confirm Existing/New, then `PUT /api/documents/{id}/replace` (single `file` part). Only the selected document changes; siblings are untouched.
- **Delete one:** confirmation dialog, then `DELETE /api/documents/{id}` (204). The list refreshes and the selection clears.
- Double-clicking a row or the context menu (Open / Download / Replace / Remove) reuses the same centralized action handlers.

All HTTP is asynchronous (background `FxAsync` pool); the JavaFX Application Thread never blocks. Duplicate action buttons are disabled while an operation is in flight. Offline failures show the Backend Offline state, never false table mutations.

## Validation and recovery

- Obvious required-field, year and date errors appear beside the fields.
- Backend named field errors appear beside the matching field. Duplicate-reference `409` messages map to Reference Number.
- Failed saves retain all entries and re-enable Save. During a request, Save/Cancel and form controls are disabled and a small `Saving...` message appears. The FX event loop stays responsive.
- Network failure sets Backend Offline. Restart/reconnect and click Save again using the retained entries. Writes are never automatically retried.
- A failed delete retains the row. A `404` update/delete reports that the record no longer exists and refreshes the list.
- After successful writes, the active Records, Dashboard or Search view reloads. Manage Records follows all API pages; create clears its filters and selects/scrolls to the new row. Search keeps its criteria. Dashboard counts use the full catalog and load fresh on navigation. Older overlapping refresh results are ignored. The global record count also refreshes.
- Record deletion delegates document metadata/file cleanup entirely to the backend.
- Missing documents (404) refresh the list; a missing parent record returns the user to the records view with a clear message.

## Document error specifics

- Upload validation failures list the offending file; the whole batch is rejected like the backend.
- A `413` shows a concise "File is too large" message (per-file limit 20 MiB), never a multipart stack trace.
- `Content-Disposition` filenames are parsed safely (quoted and `filename*=UTF-8''…` forms) and reduced to a safe basename before saving or opening.

## Run and verify

From `javafx-admin/`:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean compile
.\mvnw.cmd javafx:run
```

The default suite runs focused serialization, HTTP, multipart, validation and error tests. It never modifies MySQL. Live tests are skipped unless explicitly enabled.

Read-only J2 offline/Retry verification (backend must be running):

```powershell
.\mvnw.cmd '-Dheirs.live=true' '-Dtest=J2LiveTest' test
```

**Opt-in live tests modify the local backend.** They use uniquely generated `TEST/JFX/2026/...` references and never edit/delete seeded records. Run these in order; the first deliberately retains one record for verification in the second, fresh JVM:

```powershell
.\mvnw.cmd '-Dheirs.live=true' '-Dtest=J3LiveTest' test
.\mvnw.cmd '-Dheirs.restart=true' '-Dtest=J3RestartLiveTest' test
.\mvnw.cmd '-Dheirs.live=true' '-Dtest=J3ActionsLiveTest' test
```

These tests open the actual JavaFX FXML screens and invoke their real controls. Independent `java.net.http.HttpClient` requests verify persistence and deletion. They require a graphical desktop. Screenshots and the temporary restart record identity are written under ignored `verification/`. The default live outage scenario points the client temporarily at an unavailable local port. The executed J3 verification also stopped/restarted the actual Spring Boot process while an Edit dialog remained open; see [J3_VERIFICATION.md](J3_VERIFICATION.md).

For manually coordinated real-outage tests, remove previous `verification/stop-backend*` and `verification/start-backend*` marker files, then add `-Dheirs.coordinatedOutage=true` to J3LiveTest. When `stop-backend` appears, stop Spring Boot and create `stop-backend.continue`. When `start-backend` appears, restart Spring Boot, verify health, and create `start-backend.continue`. Each checkpoint has a 120-second deadline. Do not stop MySQL.

Optional existing screenshot smoke mode:

```powershell
$env:HEIRS_SMOKE='true'
$env:HEIRS_SHOTS_DIR='verification/smoke'
.\mvnw.cmd javafx:run
Remove-Item Env:HEIRS_SMOKE, Env:HEIRS_SHOTS_DIR
```
