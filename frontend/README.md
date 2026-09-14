# HEIRS frontend

React single-page application for the Higher Education Information Retrieval
System. The frontend supports **two clean runtime modes** selected by one
environment variable; no other configuration changes mode.

## Data modes

### Mode 1 — API (full-stack, default)

Used on the developer machine and when deployed with the backend.

```
React
  -> Spring Boot
  -> MySQL
  -> real document storage
```

```env
VITE_DATA_MODE=api
VITE_API_BASE_URL=http://127.0.0.1:8080
```

Required: the Spring Boot backend running with MySQL. All records, CRUD,
backend search, document upload/preview/download/replace/remove are **real**
and shared across users. `VITE_DATA_MODE` defaults to `api` when unset, so local
development works with no extra configuration.

### Mode 2 — Demo (frontend-only, Vercel)

Used when deploying the frontend alone (for example to Vercel) to let someone
open the site with no backend at all.

```
React
  -> bundled demo data / browser-local demo state
```

```env
VITE_DATA_MODE=demo
```

No Java server, no MySQL, no cloud database, and no cloud file storage are
required. Spring Boot may be completely off.

Demo-mode behavior:

- Records are seeded from the bundled dataset on first visit and then persisted
  **per browser** in `localStorage` (`heirs_demo_records_v1`).
- Add / Edit / Delete records update that browser's local copy and survive a
  refresh. They are **local to that browser only** and are not shared with
  other users.
- Search and all filters run client-side over the active demo records and use
  the exact same Search UI and URL query sync as API mode.
- Dashboard, Categories, Record Details, Manage Records and Reports all operate
  on the active provider's records, so no numbers are hard-coded.
- A few demo records ship with bundled sample PDFs under
  `public/demo-documents/` so Preview / Download visibly work. Documents
  without a bundled file show:
  *"Preview is unavailable in frontend demo mode."*
- Document upload in demo mode records **metadata only** (persisted in
  `heirs_demo_documents_v1`); the picked files themselves are only previewable
  for the current session and are intentionally **not** stored in
  `localStorage` (binary blobs are unsuitable for it). No false persistence
  claim is made.
- `Manage Records` shows a demo-only **Reset Demo Data** action that restores
  the bundled defaults.
- The header shows **Demo Ready** instead of **System Ready**; no backend is
  pinged.

`VITE_DATA_MODE` is read once at build time. Rebuild the app after changing it.

## Environment

| Variable | Default | Mode | Purpose |
|---|---|---|---|
| `VITE_DATA_MODE` | `api` | both | `api` (real backend) or `demo` (frontend-only) |
| `VITE_API_BASE_URL` | `http://127.0.0.1:8080` | api only | Spring Boot base URL; unused in demo mode |

See `.env.example`.

## Local development

```bash
npm install
npm run dev        # http://127.0.0.1:5173
```

Create a local `.env` (or copy `.env.example`). For full-stack mode keep the
backend running, then the app talks to it directly. For demo-only testing stop
the backend and set `VITE_DATA_MODE=demo`.

## Build / checks

```bash
npm run lint
npm run build
```

`lint` and `build` must pass before a deployment.

## Vercel deployment (demo)

Deploy the `frontend/` directory with:

- `VITE_DATA_MODE=demo`
- no `VITE_API_BASE_URL` needed

Included `vercel.json` rewrites all routes to `index.html` so SPA deep links
(such as `/records/:id` or `/search`) work after a refresh. The bundled demo
PDFs in `public/demo-documents/` are copied into the build automatically.

## Mode accuracy

- **API mode**: real persistent shared backend data.
- **Demo mode**: per-browser local demo data; nothing is globally persisted.

The components and `RecordsContext` only ever talk to a single provider
interface in `src/data/dataProvider.js` (selected by `VITE_DATA_MODE`), so both
modes render the same approved UI with the same layout, colors, Questrial
typography, sidebar, navigation, cards, glass buttons and responsive behavior.