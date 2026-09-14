// Versioned, safe browser-local storage used by demo mode only.
//
// Versioned keys guard against future schema changes corrupting older demo
// state. Every read is wrapped in try/catch: if a stored key contains invalid
// JSON or fails the structural validator, only that broken key is cleared and
// the caller's fallback (bundled defaults) is used so the app never crashes.

export const STORAGE_KEYS = {
  records: 'heirs_demo_records_v1',
  documents: 'heirs_demo_documents_v3',
  documentsLegacyV2: 'heirs_demo_documents_v2',
  documentsLegacyV1: 'heirs_demo_documents_v1',
};

export function loadStored(key, fallback, validate = () => true) {
  try {
    const raw = window.localStorage.getItem(key);
    if (raw === null || raw === undefined) return fallback;
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed) && parsed.every(validate)) return parsed;
  } catch {
    // fall through to recovery
  }
  try {
    window.localStorage.removeItem(key);
  } catch {
    // Storage may be unavailable entirely; bundled defaults still win.
  }
  return fallback;
}

export function storeValue(key, value) {
  try {
    window.localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Quota / privacy mode: demo mode keeps working for this session even if
    // changes cannot be persisted between reloads.
  }
}

export function clearStored(key) {
  try {
    window.localStorage.removeItem(key);
  } catch {
    // Ignored; nothing else to recover.
  }
}