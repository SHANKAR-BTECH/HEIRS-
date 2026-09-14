"""Generate synthetic supporting PDFs for EVERY bundled HEIRS demo record.

Pipeline:

  1. Load canonical records (JSON snapshot extracted from the frontend source).
  2. Assign 2/3 documents per record deterministically (sha256, no built-in hash()).
  3. Generate every PDF into a STAGING directory.
  4. Generate a candidate frontend/src/data/demoDocuments.js.
  5. Run the critical validation gate against staging.
  6. Only if every check passes: back up current assets and promote.

The existing deployed demo data is untouched unless and until every check in
the validation gate passes.

Usage:
    python scripts/generate_demo_documents.py            # full run
    python scripts/generate_demo_documents.py --continue # skip env re-check
"""

import argparse
import json
import os
import shutil
import subprocess
import sys
from difflib import SequenceMatcher
from pathlib import Path

from content_engine import (
    CATEGORY_TERM,
    build_document_spec,
    doc_count,
    stable_index,
    uploaded_at,
)
from pdf_renderer import DISCLAIMER_TEXT, render

ROOT = Path(__file__).resolve().parent.parent
FRONTEND = ROOT / "frontend"
PUBLIC_DOC_DIR = FRONTEND / "public" / "demo-documents"
SRC_DOCS = FRONTEND / "src" / "data" / "demoDocuments.js"
TEMP = Path(__file__).resolve().parent / ".temp"
STAGING = TEMP / "generated-demo-documents"
CANDIDATE = TEMP / "demoDocuments.generated.js"
BACKUP_DOC_DIR = TEMP / "backup-demo-documents"
BACKUP_SRC_DOCS = TEMP / "backup-demoDocuments.js"

# ---------------------------------------------------------------------------
# Document type definitions per category
# ---------------------------------------------------------------------------

CATEGORY_DOC_TYPES = {
    "Policy": [
        {"suffix": "main", "name": "Main Policy", "rank": "primary",
         "originalName": "Main Policy.pdf", "offset": 1},
        {"suffix": "guidelines", "name": "Implementation Guidelines", "rank": "secondary",
         "originalName": "Implementation Guidelines.pdf", "offset": 2},
        {"suffix": "annexure", "name": "Monitoring Annexure", "rank": "tertiary",
         "originalName": "Monitoring Annexure.pdf", "offset": 3},
    ],
    "Scheme": [
        {"suffix": "scheme-guidelines", "name": "Scheme Guidelines", "rank": "primary",
         "originalName": "Scheme Guidelines.pdf", "offset": 1},
        {"suffix": "application-guide", "name": "Eligibility and Application Guide", "rank": "secondary",
         "originalName": "Eligibility and Application Guide.pdf", "offset": 2},
        {"suffix": "monitoring", "name": "Renewal and Monitoring Annexure", "rank": "tertiary",
         "originalName": "Renewal and Monitoring Annexure.pdf", "offset": 3},
    ],
    "Regulation": [
        {"suffix": "regulation", "name": "Regulation Document", "rank": "primary",
         "originalName": "Regulation Document.pdf", "offset": 1},
        {"suffix": "compliance", "name": "Compliance Guidelines", "rank": "secondary",
         "originalName": "Compliance Guidelines.pdf", "offset": 2},
        {"suffix": "assessment", "name": "Reporting and Assessment Annexure", "rank": "tertiary",
         "originalName": "Reporting and Assessment Annexure.pdf", "offset": 3},
    ],
    "Project": [
        {"suffix": "framework", "name": "Project Framework", "rank": "primary",
         "originalName": "Project Framework.pdf", "offset": 1},
        {"suffix": "implementation", "name": "Implementation Plan", "rank": "secondary",
         "originalName": "Implementation Plan.pdf", "offset": 2},
        {"suffix": "monitoring", "name": "Progress Monitoring Annexure", "rank": "tertiary",
         "originalName": "Progress Monitoring Annexure.pdf", "offset": 3},
    ],
    "Rules": [
        {"suffix": "rules", "name": "Rules Notification", "rank": "primary",
         "originalName": "Rules Notification.pdf", "offset": 1},
        {"suffix": "procedures", "name": "Operational Procedures", "rank": "secondary",
         "originalName": "Operational Procedures.pdf", "offset": 2},
        {"suffix": "checklist", "name": "Compliance Checklist", "rank": "tertiary",
         "originalName": "Compliance Checklist.pdf", "offset": 3},
    ],
}


def load_records():
    path = TEMP / "records.json"
    if not path.exists():
        print(f"[extract] records.json not found at {path}; run extract_records.mjs first.")
        sys.exit(2)
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def plan_documents(records):
    """Return list of (record, doc_type)."""
    plan = []
    for record in records:
        category = record.get("category") or "Policy"
        types = CATEGORY_DOC_TYPES.get(category)
        if types is None:
            # Fall back to a generic set for unexpected categories.
            types = CATEGORY_DOC_TYPES["Policy"]
        n = doc_count(record["id"])
        plan.append((record, types[:n]))
    return plan


def build_meta(record, doc_type, size):
    return {
        "id": f"demo-doc-{record['id']}-{doc_type['suffix']}",
        "recordId": record["id"],
        "originalFileName": doc_type["originalName"],
        "contentType": "application/pdf",
        "fileSize": size,
        "demo": True,
        "srcFile": f"/demo-documents/{record['id']}_{doc_type['suffix']}.pdf",
        "uploadedAt": uploaded_at(record, doc_type["offset"]),
        "updatedAt": uploaded_at(record, doc_type["offset"]),
    }


def js_repr(value):
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, int) or isinstance(value, float):
        return str(value)
    return json.dumps(value)


def render_documents_js(metadata):
    lines = [
        "// Auto-generated demo-mode supporting document metadata.",
        "//",
        "// Generated by scripts/generate_demo_documents.py from the canonical",
        "// bundled record set (mockRecords.js/demoRecords.js). Do not edit by hand;",
        "// re-run the generator after changing the record set.",
        "//",
        "// Timestamps use naive UTC strings, matching what SupportingDocuments expects",
        "// (it appends a trailing \"Z\" before formatting).",
        "",
        "export const demoDocuments = [",
    ]
    for m in metadata:
        lines.append("  {")
        keys = list(m.keys())
        for i, k in enumerate(keys):
            comma = "," if i < len(keys) - 1 else ","
            lines.append(f"    {k}: {js_repr(m[k])}{comma}")
        lines.append("  },")
    lines.append("];")
    lines.append("")
    lines.append("export function cloneDemoDocuments() {")
    lines.append("  return demoDocuments.map((document) => ({ ...document }));")
    lines.append("}")
    lines.append("")
    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Validation gate
# ---------------------------------------------------------------------------

def validate_pdf(path):
    """Return (ok, reason, pages, words)."""
    try:
        import pymupdf
    except Exception as exc:  # pragma: no cover
        return False, f"pymupdf unavailable: {exc}", 0, 0
    try:
        doc = pymupdf.open(path)
        pages = len(doc)
        if pages < 1:
            doc.close()
            return False, "zero pages", 0, 0
        text = "".join(page.get_text() for page in doc)
        words = len(text.split())
        if words < 120:
            doc.close()
            return False, f"too little text ({words} words)", pages, words
        if "Demo / Synthetic Document" not in text:
            doc.close()
            return False, "missing disclaimer", pages, words
        if "HEIRS" not in text:
            doc.close()
            return False, "missing HEIRS label", pages, words
        doc.close()
        return True, "ok", pages, words
    except Exception as exc:
        return False, f"open failed: {exc}", 0, 0


def normalize_for_similarity(text):
    t = text
    t = t.replace(DISCLAIMER_TEXT, " ")
    lines = [ln for ln in t.splitlines()
             if "DEMO DOCUMENT" not in ln and "HEIRS" not in ln
             and not ln.strip().startswith("Page") and ln.strip()]
    t = " ".join(lines)
    words = t.split()
    skip = {"the", "a", "an", "of", "and", "or", "in", "on", "for", "with",
            "to", "by", "this", "that", "is", "are", "be", "will", "may"}
    filtered = [w for w in words if w.lower() not in skip and len(w) > 3]
    return " ".join(filtered)


def check_similarity(meta, dir_path):
    import pymupdf
    text_cache = {}
    for m in meta:
        path = dir_path / Path(m["srcFile"]).name
        doc = pymupdf.open(path)
        text_cache[m["id"]] = normalize_for_similarity(
            "".join(p.get_text() for p in doc))
        doc.close()
    warnings = []
    by_record = {}
    for m in meta:
        by_record.setdefault(m["recordId"], []).append(m["id"])
    worst = 0.0
    for record_id, ids in by_record.items():
        for i in range(len(ids)):
            for j in range(i + 1, len(ids)):
                a, b = text_cache[ids[i]], text_cache[ids[j]]
                if not a or not b:
                    continue
                ratio = SequenceMatcher(None, a, b).ratio()
                worst = max(worst, ratio)
                if ratio > 0.78:
                    warnings.append(f"{ids[i]} <-> {ids[j]}: {ratio:.2f}")
    return warnings, worst


def validate_js_candidate(candidate_path):
    """Import the candidate ES module under Node and check its shape.

    The module content is written to a temp .mjs file (exact same text) and
    imported via a file URL passed through an environment variable -- inline
    base64 payloads exceed the Windows command-line length limit.
    """
    mjs = TEMP / "validateDocs.mjs"
    mjs.write_text(candidate_path.read_text(encoding="utf-8"), encoding="utf-8")
    script = (
        "import { pathToFileURL } from 'node:url';"
        "import p from 'node:path';"
        "const url = pathToFileURL(process.env.DOC_JS_PATH);"
        "import(url.href).then(m => {"
        "  const a = m.demoDocuments;"
        "  if (!Array.isArray(a)) { console.error('NOT-ARRAY'); process.exit(1); }"
        "  const n = a.length;"
        "  const ids = new Set(a.map(d => d.id));"
        "  const files = new Set(a.map(d => d.srcFile).filter(Boolean));"
        "  const bad = a.filter(d => !d.id || !d.recordId || !d.originalFileName || !d.srcFile || !Number.isInteger(d.fileSize));"
        "  console.log('JS-OK count=' + n + ' uniqueIds=' + ids.size + ' uniqueFiles=' + files.size + ' bad=' + bad.length);"
        "  if (bad.length || ids.size !== n || files.size !== n) process.exit(1);"
        "}).catch(e => { console.error('JS-ERR', e); process.exit(1); });"
    )
    env = dict(os.environ)
    env["DOC_JS_PATH"] = str(mjs)
    res = subprocess.run(["node", "-e", script], capture_output=True, text=True,
                         cwd=str(ROOT), env=env, creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
    try:
        mjs.unlink()
    except OSError:
        pass
    out = (res.stdout + res.stderr).strip()
    return res.returncode == 0, out


def run_validation(records, meta, dir_path):
    print("\n=== VALIDATION GATE ===")
    ok = True
    problems = []

    rec_id_set = {r["id"] for r in records}
    by_record = {}
    for m in meta:
        by_record.setdefault(m["recordId"], []).append(m)

    # A. record coverage
    for r in records:
        n = len(by_record.get(r["id"], []))
        if n < 2 or n > 3:
            ok = False
            problems.append(f"COVERAGE {r['id']}: {n} docs")
    print(f"A. record coverage           records=60  -> "
          f"2-doc={sum(1 for r in records if len(by_record.get(r['id'], [])) == 2)} "
          f"3-doc={sum(1 for r in records if len(by_record.get(r['id'], [])) == 3)}")

    # B. orphans
    orphans = [m["id"] for m in meta if m["recordId"] not in rec_id_set]
    if orphans:
        ok = False
        problems.append(f"ORPHANS {len(orphans)}: {orphans[:5]}")
    print(f"B. orphan metadata           orphans={len(orphans)}")

    # C. file existence
    missing_files = [
        m["id"] for m in meta
        if not (dir_path / Path(m["srcFile"]).name).exists()
    ]
    if missing_files:
        ok = False
        problems.append(f"MISSING {len(missing_files)}: {missing_files[:5]}")
    print(f"C. file existence            missing={len(missing_files)}")

    # D-H. PDF checks
    bad_pdf = []
    total_pages = 0
    total_words = 0
    for m in meta:
        path = dir_path / Path(m["srcFile"]).name
        good, reason, pages, words = validate_pdf(path)
        total_pages += pages
        total_words += words
        if not good:
            ok = False
            bad_pdf.append((m["id"], reason))
    print(f"D-H. pdf opens/pages/text/disclaimer  bad={len(bad_pdf)} "
          f"total_pages={total_pages} total_words={total_words}")
    for bid, reason in bad_pdf[:6]:
        print(f"      ! {bid}: {reason}")

    # I. unique ids
    ids = [m["id"] for m in meta]
    dup_ids = [x for x in set(ids) if ids.count(x) > 1]
    if dup_ids:
        ok = False
        problems.append(f"DUP_IDS {dup_ids}")
    print(f"I. unique ids                 dups={len(dup_ids)}")

    # J. unique filenames
    files = [Path(m["srcFile"]).name for m in meta]
    dup_files = [x for x in set(files) if files.count(x) > 1]
    if dup_files:
        ok = False
        problems.append(f"DUP_FILES {dup_files}")
    print(f"J. unique filenames           dups={len(dup_files)}")

    # K. JS module valid (exact content via data URL)
    js_ok, js_out = validate_js_candidate(CANDIDATE)
    if not js_ok:
        ok = False
        problems.append(f"JS_INVALID: {js_out}")
    print(f"K. js module valid            {js_out}")

    # similarity quality (warning, not critical)
    sim_warnings, worst = check_similarity(meta, dir_path)
    print(f"L. similarity (same record)   worst={worst:.2f} warnings={len(sim_warnings)}")
    for w in sim_warnings[:8]:
        print(f"      ~ {w}")

    print(f"\nOverall: {'PASS' if ok else 'FAIL'}")
    if problems:
        for p in problems:
            print(f"  [{p}]")
    return ok


# ---------------------------------------------------------------------------
# Promotion
# ---------------------------------------------------------------------------

def promote():
    print("\n=== PROMOTION ===")
    if BACKUP_DOC_DIR.exists():
        shutil.rmtree(BACKUP_DOC_DIR)
    if BACKUP_SRC_DOCS.exists():
        BACKUP_SRC_DOCS.unlink()

    if PUBLIC_DOC_DIR.exists():
        shutil.copytree(PUBLIC_DOC_DIR, BACKUP_DOC_DIR)
    if SRC_DOCS.exists():
        shutil.copy2(SRC_DOCS, BACKUP_SRC_DOCS)
    print("   backup created.")

    if PUBLIC_DOC_DIR.exists():
        shutil.rmtree(PUBLIC_DOC_DIR)
    shutil.copytree(STAGING, PUBLIC_DOC_DIR)
    shutil.copy2(CANDIDATE, SRC_DOCS)
    print("   promoted staging -> production.")

    # verify
    staged_files = {p.name for p in STAGING.glob("*.pdf")}
    prod_files = {p.name for p in PUBLIC_DOC_DIR.glob("*.pdf")}
    if staged_files != prod_files:
        print("   WARNING: file set mismatch after promotion.")
        return False
    print(f"   verified: {len(prod_files)} PDFs in place.")
    return True


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--no-promote", action="store_true",
                        help="generate + validate only (leave production untouched)")
    parser.add_argument("--no-validate-js", action="store_true",
                        help="skip Node validation of the candidate module")
    args = parser.parse_args()

    records = load_records()
    print(f"Loaded {len(records)} records from frontend snapshot.")

    if STAGING.exists():
        shutil.rmtree(STAGING)
    STAGING.mkdir(parents=True)

    meta = []
    plan = plan_documents(records)
    print("Generating documents...")
    for record, types in plan:
        for doc_type in types:
            spec = build_document_spec(record, doc_type, record.get("category"))
            out = render(spec, str(STAGING))
            size = os.path.getsize(out)
            meta.append(build_meta(record, doc_type, size))
    print(f"Generated {len(meta)} PDFs.")

    CANDIDATE.write_text(render_documents_js(meta), encoding="utf-8")
    print(f"Wrote candidate metadata: {CANDIDATE}")

    ok = run_validation(records, meta, STAGING)
    if not ok:
        print("\nValidation FAILED. Production assets were NOT modified.")
        print("Staging output kept for inspection.")
        sys.exit(1)

    print("\nValidation PASSED.")
    if args.no_promote:
        print("--no-promote: staging left in place; production untouched.")
        sys.exit(0)

    if not promote():
        sys.exit(1)

    # Post-promotion re-validation of production files
    print("\n=== POST-PROMOTION VALIDATION ===")
    js_ok, js_out = validate_js_candidate(SRC_DOCS)
    print("PROD-JS:", js_out, "status", "OK" if js_ok else "FAIL")
    if not js_ok:
        print("Post-promotion JS validation failed. See scripts/.temp/backup-* to restore.")
        sys.exit(1)
    print("Done.")


if __name__ == "__main__":
    main()