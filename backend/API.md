# HEIRS REST contract ? phase 2

Base URL: `http://127.0.0.1:8080`. JSON requests use `Content-Type: application/json`. This contract applies equally to React and JavaFX. No authentication or cookies are required in this local foundation.

## Record response

GET `/api/records/{id}` returns HTTP 200 with exactly these fields:

```json
{
  "id": 1,
  "title": "National Digital Learning Policy for Higher Education",
  "description": "Framework for improving digital infrastructure, blended learning and institutional digital readiness across universities and colleges.",
  "category": "Policy",
  "department": "Higher Education Department",
  "referenceNumber": "HEIRS/POL/2026/014",
  "publicationYear": 2026,
  "publishedDate": "2026-03-12",
  "status": "Active",
  "source": "Higher Education Department",
  "keywords": "Digital Learning,Universities,Infrastructure,Blended Learning"
}
```

`id` is a positive generated integer, not the mock catalogue's string ID. `publishedDate` is an ISO `YYYY-MM-DD` date without a timezone. `keywords` is a comma-separated string, not an array. Optional fields are returned as `null` when absent. There is no JPA metadata, audit timestamps, or document upload information in this response.

## List and search

GET `/api/records` supports `page` and `size`. GET `/api/records/search` supports all parameters below. Query parameters have no request body.

| Parameter | Type/default | Rules |
|---|---|---|
| `q` | optional string | Max 500 characters; trimmed, case-insensitive substring across title, description, keywords, department, referenceNumber |
| `category` | optional enum | Policy, Scheme, Regulation, Project, Rules |
| `year` | optional integer | 1900?2100 inclusive; filters `publicationYear` |
| `status` | optional enum | Active, Draft, Completed, Archived |
| `department` | optional string | Max 150 characters; trimmed case-insensitive exact match |
| `page` | integer, `0` | Zero-based, minimum 0 |
| `size` | integer, `20` | 1?100 inclusive |

All search parameters are optional and combine with AND; `q` matches any of the five fields. Blank `q` and department values are ignored. Omit unused enum and year parameters. Enum inputs accept display labels or uppercase codes, ignoring case; responses always use the display labels. `%` and `_` in keywords are literal, not wildcard syntax. URL-encode parameter values. Unknown filter names do not define additional filters.

Example: `/api/records/search?q=digital&category=Policy&year=2026&status=Active&page=0&size=20`.

Both endpoints return HTTP 200 with the same stable envelope. This complete one-item example corresponds to `/api/records?page=0&size=1` immediately after seeding:

```json
{
  "content": [
    {
      "id": 1,
      "title": "National Digital Learning Policy for Higher Education",
      "description": "Framework for improving digital infrastructure, blended learning and institutional digital readiness across universities and colleges.",
      "category": "Policy",
      "department": "Higher Education Department",
      "referenceNumber": "HEIRS/POL/2026/014",
      "publicationYear": 2026,
      "publishedDate": "2026-03-12",
      "status": "Active",
      "source": "Higher Education Department",
      "keywords": "Digital Learning,Universities,Infrastructure,Blended Learning"
    }
  ],
  "page": 0,
  "size": 1,
  "totalElements": 60,
  "totalPages": 60,
  "first": true,
  "last": false
}
```

Results are sorted by ascending `id`. `totalElements` counts matches before pagination. No matches return `content: []`, `totalElements: 0`, and `totalPages: 0`. A page past the end returns an empty content array with the actual totals. Clients read `response.content`, not a top-level record array. Pages can shift if records change between requests.

## Create and update

POST `/api/records` creates a record. PUT `/api/records/{id}` fully replaces an existing record. Both accept the same body shape (separate request DTOs):

```json
{
  "title": "New Student Research Scheme",
  "description": "Research support for eligible students.",
  "category": "Scheme",
  "department": "Student Welfare",
  "referenceNumber": "SW/SCH/2026/NEW-001",
  "publicationYear": 2026,
  "publishedDate": "2026-09-13",
  "status": "Draft",
  "source": null,
  "keywords": "research,students"
}
```

| Field | Required | Validation |
|---|---|---|
| title | Yes | Nonblank, max 255 characters |
| description | No | Max 20,000 characters, nullable |
| category | Yes | Known category enum |
| department | Yes | Nonblank, max 150 characters |
| referenceNumber | Yes | Nonblank, max 100 characters, database unique |
| publicationYear | No | Integer 1900?2100, nullable |
| publishedDate | No | Valid ISO date, nullable |
| status | Yes | Known status enum |
| source | No | Max 500 characters, nullable; free text, not forced to be a URL |
| keywords | No | Max 2,000 characters, nullable string |

Title, department, and reference number are trimmed before persistence. Required fields must be supplied on PUT. Omitted optional fields become null; PUT is not a partial update. Clients must not send `id`, timestamps, or unknown JSON fields. Numeric enum values are rejected. No automatic category classification, reference generation, or relationship between publication year and date is imposed.

POST returns HTTP **201**, a `Location` header pointing to `/api/records/{newId}`, and the full record response. PUT returns HTTP **200** and the full updated record. Explicit reference corrections are allowed if unique; the backend never regenerates references. A missing PUT target is **404**; duplicate references on either operation are **409**, including database-detected concurrent conflicts.

## Delete

DELETE `/api/records/{id}` has no body. Returns **204 No Content** on success, or **404** if the record does not exist (including repeated deletion).

## Categories

GET `/api/categories` returns HTTP 200:

```json
[
  {"code":"POLICY","name":"Policy"},
  {"code":"SCHEME","name":"Scheme"},
  {"code":"REGULATION","name":"Regulation"},
  {"code":"PROJECT","name":"Project"},
  {"code":"RULES","name":"Rules"}
]
```

## Health

GET `/api/health` opens and validates a database connection. HTTP **200**:

```json
{"status":"UP","database":"UP"}
```

If the database is unavailable: HTTP **503**, `{"status":"DOWN","database":"DOWN"}`. Health is a readiness contract and uses this status body rather than the application error envelope. Initial startup also requires a reachable database.

## Errors

Application errors use this shape (timestamp is an ISO UTC instant):

```json
{
  "timestamp": "2026-09-13T07:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Record with id 42 was not found",
  "path": "/api/records/42",
  "fieldErrors": {}
}
```

Validation example:

```json
{
  "timestamp": "2026-09-13T07:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/records",
  "fieldErrors": {
    "title": "Title is required",
    "referenceNumber": "Reference number is required"
  }
}
```

`fieldErrors` maps invalid input names to messages and is empty for errors without field details. Treat `status` and field names as structured data; messages are intended for humans.

| Status | Meaning |
|---|---|
| 400 | Bean Validation failure, invalid query types/enums, malformed JSON/date, unknown body fields |
| 404 | Missing record or endpoint |
| 405 | Unsupported HTTP method |
| 409 | Duplicate reference or database constraint conflict |
| 415 | Unsupported request content type |
| 500 | Unexpected error; details logged server-side, generic public message |

CORS preflight rejection is handled by Spring's CORS filter (403); it is not an application JSON error. Both local Vite origins are allowed, with GET/POST/PUT/DELETE/OPTIONS and exposed `Location`. JavaFX uses ordinary HTTP without browser CORS enforcement.
# Supporting document API (Phase 4)

Base URL: `http://127.0.0.1:8080`. PDF uploads use `multipart/form-data`; JSON record endpoints retain their existing contract. Clients must let their multipart library generate the boundary. A record may have zero, one, or many attachments.

| Method | Endpoint | Success |
|---|---|---|
| POST | `/api/records/{recordId}/documents` | 201; array of newly attached document DTOs |
| GET | `/api/records/{recordId}/documents` | 200; all attachments ordered by ID, or `[]` |
| GET | `/api/documents/{documentId}/preview` | 200; PDF bytes, inline disposition |
| GET | `/api/documents/{documentId}/download` | 200; PDF bytes, attachment disposition |
| PUT | `/api/documents/{documentId}/replace` | 200; updated document DTO |
| DELETE | `/api/documents/{documentId}` | 204; metadata removed and file cleanup committed |

Example document DTO (timestamps are UTC):

```json
{
  "id": 8,
  "recordId": 12,
  "originalFileName": "Main_Policy.pdf",
  "contentType": "application/pdf",
  "fileSize": 1845932,
  "uploadedAt": "2026-09-13T12:00:00",
  "updatedAt": "2026-09-13T12:00:00"
}
```

No physical paths or storage keys are returned. Preview/download preserve the safe original filename using UTF-8 Content-Disposition, send `application/pdf`, content length, `X-Content-Type-Options: nosniff`, and `Cache-Control: no-store`. PDFs use the browser's native viewer.

From PowerShell, upload three files in **one request** using repeated `files` fields:

```powershell
curl.exe -f -X POST 'http://127.0.0.1:8080/api/records/12/documents' `
  -F 'files=@Main_Policy.pdf;type=application/pdf' `
  -F 'files=@Amendment_1.pdf;type=application/pdf' `
  -F 'files=@Annexure_A.pdf;type=application/pdf'

curl.exe -f 'http://127.0.0.1:8080/api/records/12/documents'
curl.exe -f 'http://127.0.0.1:8080/api/documents/8/preview' -o preview.pdf
curl.exe -f 'http://127.0.0.1:8080/api/documents/8/download' -o Main_Policy.pdf

# Replacement accepts exactly the single file field, and preserves document ID 8.
curl.exe -f -X PUT 'http://127.0.0.1:8080/api/documents/8/replace' `
  -F 'file=@Revised_Policy.pdf;type=application/pdf'

curl.exe -f -X DELETE 'http://127.0.0.1:8080/api/documents/8'
```

A one-file upload uses the same POST with one `files` part. Additional POSTs append documents; duplicate original filenames are supported. Replacement affects only the selected ID and preserves `uploadedAt`; removal affects only the selected document. `DELETE /api/records/{id}` also removes all attachment metadata and stored files.

Validation: PDFs only; `.pdf` extension, `application/pdf` MIME type, and `%PDF-` signature required. Non-empty, safe filename up to 180 characters. Default maximum: **20 MiB (20,971,520 bytes) per file**; **100 MiB (104,857,600 bytes) per multipart request including overhead**. Configure through `HEIRS_MAX_FILE_BYTES` and `HEIRS_MAX_REQUEST_BYTES`. See [README.md](README.md) for storage configuration.

All files are validated before any batch persistence. Invalid members reject the whole batch; database/storage failures roll the batch back and clean newly written files. Existing siblings remain unchanged. Old-file removal runs after commit with durable retry jobs: if the filesystem is temporarily unavailable, the mutation still succeeds and physical removal is retried every minute and after restart. See README for the process-crash and simultaneous-outage limitations.

Errors use the existing `ApiErrorDto` envelope (`timestamp`, `status`, `error`, `message`, `path`, `fieldErrors`):

| Status | Meaning |
|---|---|
| 400 | Missing multipart field, empty/invalid file, unsafe filename, or invalid identifier |
| 404 | Missing parent record, document metadata, or stored file |
| 413 | File or total multipart request exceeds the configured size limit |
| 415 | Unsupported request Content-Type |
| 500 | Storage/database operation failed; physical paths are not exposed |
