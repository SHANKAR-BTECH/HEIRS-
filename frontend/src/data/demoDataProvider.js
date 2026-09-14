// Demo data provider: a fully self-contained frontend store used when
// VITE_DATA_MODE=demo. No backend, no MySQL, no cloud service is contacted.
//
// Records and document metadata are seeded from bundled data on first visit
// and persist per-browser in versioned localStorage keys. Uploaded PDFs are
// intentionally NOT stored in localStorage (binary blobs are unsuitable for
// it): metadata persists, and files are only previewable for the current
// session via object URLs. No false persistence claims are made.

import { cloneDemoRecords } from './demoRecords';
import { cloneDemoDocuments } from './demoDocuments';
import { loadStored, storeValue, STORAGE_KEYS } from './localDemoStore';
import { CATEGORY_KEYS } from './mockCategories';
import { searchRecords as clientSearch } from '../lib/searchUtils';

let records = null;
let documents = null;

// Blob-object URLs created for files picked in the current session. They stop
// working after a page refresh, which is why they are session-only.
const sessionUrls = new Map();

// Load demo documents with a v1 → v2 migration.  Browsers that visited the
// deployed site before this fix may already have a valid but incomplete
// `heirs_demo_documents_v1` key.  We preserve any user-uploaded document
// metadata from that legacy key (identified by ID not matching the bundled
// set) and merge it with the current bundled defaults into v2.  The v1 key
// is removed, and the merged set is persisted to v2, so this only runs once.
function loadDemoDocuments() {
  const bundled = cloneDemoDocuments();
  const bundledIds = new Set(bundled.map((d) => d.id));

  // Normal v2 load (may return null if key is missing or invalid).
  const stored = loadStored(STORAGE_KEYS.documents, null, isValidDemoDocument);

  let userDocs = [];
  let shouldPersist = false;
  if (Array.isArray(stored)) {
    // Preserve user-created documents that are not part of the bundled set.
    userDocs = stored.filter((d) => !bundledIds.has(d.id));
  } else {
    // One-time migration from v1 (only if v2 had no stored data).
    shouldPersist = true;
    const legacy = loadStored(STORAGE_KEYS.documentsLegacy, null, isValidDemoDocument);
    if (Array.isArray(legacy)) {
      userDocs = legacy.filter((d) => !bundledIds.has(d.id));
    }
    // Clean up the legacy key.
    try { window.localStorage.removeItem(STORAGE_KEYS.documentsLegacy); } catch { /* ignore */ }
  }

  const merged = [...bundled, ...userDocs];
  if (shouldPersist) {
    storeValue(STORAGE_KEYS.documents, merged);
  }
  return merged.map(cloneDemoDocument);
}

function ensureLoaded() {
  if (records !== null) return;
  records = loadStored(STORAGE_KEYS.records, cloneDemoRecords(), isValidDemoRecord).map(cloneDemoRecord);
  documents = loadDemoDocuments();
}

function cloneDemoRecord(record) {
  return {
    ...record,
    keywords: Array.isArray(record.keywords) ? [...record.keywords] : [],
  };
}

function isValidDemoRecord(record) {
  return Boolean(
    record &&
      typeof record === 'object' &&
      typeof record.id === 'string' &&
      typeof record.title === 'string' &&
      typeof record.category === 'string'
  );
}

function isValidDemoDocument(document) {
  return Boolean(
    document &&
      typeof document === 'object' &&
      typeof document.id === 'string' &&
      typeof document.recordId === 'string' &&
      typeof document.originalFileName === 'string'
  );
}

function cloneDemoDocument(document) {
  return { ...document };
}

function persistRecords() {
  storeValue(STORAGE_KEYS.records, records);
}

function persistDocuments() {
  storeValue(STORAGE_KEYS.documents, documents);
}

function newRecordId() {
  return `demo-${crypto.randomUUID()}`;
}

function newDocumentId() {
  return `demo-doc-${crypto.randomUUID()}`;
}

function nowUtc() {
  return new Date().toISOString().split('.')[0];
}

function keywordsToArray(keywords) {
  if (!keywords) return [];
  if (Array.isArray(keywords)) return keywords.map((k) => String(k).trim()).filter(Boolean);
  return String(keywords)
    .split(',')
    .map((keyword) => keyword.trim())
    .filter(Boolean);
}

function revokeSessionUrl(docId) {
  const url = sessionUrls.get(docId);
  if (url?.preview) {
    try {
      URL.revokeObjectURL(url.preview);
    } catch {
      // Safe to ignore during revoke.
    }
  }
  sessionUrls.delete(docId);
}

const notFound = (message = 'Record not found.') => {
  const error = new Error(message);
  error.status = 404;
  return error;
};

export const demoDataProvider = {
  async getAllRecords() {
    ensureLoaded();
    return { records: records.map(cloneDemoRecord), total: records.length };
  },

  async getRecordById(id) {
    ensureLoaded();
    const record = records.find((item) => item.id === id);
    if (!record) throw notFound();
    return cloneDemoRecord(record);
  },

  async searchRecords({ q, category, year, status, department } = {}) {
    ensureLoaded();
    const matched = clientSearch(records, {
      query: String(q || '').trim(),
      category: category || 'All',
      year: year || 'All Years',
      status: status || 'All',
      department: department || 'All Departments',
    });
    return { records: matched.map(cloneDemoRecord), total: matched.length };
  },

  async getCategories() {
    return CATEGORY_KEYS.map((name) => ({ code: name, name }));
  },

  async createRecord(payload) {
    ensureLoaded();
    const record = {
      id: newRecordId(),
      title: String(payload.title || '').trim(),
      description: String(payload.description || '').trim(),
      category: payload.category,
      department: payload.department,
      referenceNumber: String(payload.referenceNumber || '').trim(),
      publicationYear: Number(payload.publicationYear),
      publishedDate: payload.publishedDate || '',
      status: payload.status,
      source: String(payload.source || payload.department || '').trim(),
      keywords: keywordsToArray(payload.keywords),
    };
    records = [record, ...records];
    persistRecords();
    return cloneDemoRecord(record);
  },

  async updateRecord(id, payload) {
    ensureLoaded();
    const existing = records.find((item) => item.id === id);
    if (!existing) throw notFound('Record not found.');
    const updated = {
      ...existing,
      title: String(payload.title || '').trim(),
      description: String(payload.description || '').trim(),
      category: payload.category,
      department: payload.department,
      referenceNumber: String(payload.referenceNumber || '').trim(),
      publicationYear: Number(payload.publicationYear),
      publishedDate: payload.publishedDate || '',
      status: payload.status,
      source: String(payload.source || payload.department || existing.source || '').trim(),
      keywords: keywordsToArray(payload.keywords),
    };
    records = records.map((item) => (item.id === id ? updated : item));
    persistRecords();
    return cloneDemoRecord(updated);
  },

  async deleteRecord(id) {
    ensureLoaded();
    records = records.filter((item) => item.id !== id);
    documents = documents.filter((document) => document.recordId !== id);
    persistRecords();
    persistDocuments();
  },

  async getDocuments(recordId) {
    ensureLoaded();
    return documents
      .filter((document) => document.recordId === recordId)
      .map(cloneDemoDocument);
  },

  async uploadDocuments(recordId, files) {
    ensureLoaded();
    const added = Array.from(files || []).map((file) => {
      const id = newDocumentId();
      const document = {
        id,
        recordId,
        originalFileName: file.name || 'document.pdf',
        contentType: file.type || 'application/pdf',
        fileSize: file.size || 0,
        demo: true,
        srcFile: null,
        uploadedAt: nowUtc(),
        updatedAt: nowUtc(),
      };
      try {
        sessionUrls.set(id, {
          preview: URL.createObjectURL(file),
          download: URL.createObjectURL(file),
        });
      } catch {
        // Object URL creation failed; metadata still persists.
      }
      return document;
    });
    documents = [...documents, ...added];
    persistDocuments();
    return added.map(cloneDemoDocument);
  },

  async replaceDocument(documentId, file) {
    ensureLoaded();
    const existing = documents.find((document) => document.id === documentId);
    if (!existing) throw notFound('Document not found.');
    revokeSessionUrl(documentId);
    const updated = {
      ...existing,
      originalFileName: file.name || existing.originalFileName,
      contentType: file.type || existing.contentType,
      fileSize: file.size || existing.fileSize,
      srcFile: null,
      updatedAt: nowUtc(),
    };
    try {
      sessionUrls.set(documentId, {
        preview: URL.createObjectURL(file),
        download: URL.createObjectURL(file),
      });
    } catch {
      // Session preview unavailable; metadata updated.
    }
    documents = documents.map((document) => (document.id === documentId ? updated : document));
    persistDocuments();
    return cloneDemoDocument(updated);
  },

  async deleteDocument(documentId) {
    ensureLoaded();
    revokeSessionUrl(documentId);
    documents = documents.filter((document) => document.id !== documentId);
    persistDocuments();
  },

  getDocumentUrls(document) {
    ensureLoaded();
    const session = sessionUrls.get(document?.id);
    const bundled = document?.srcFile ? { preview: document.srcFile, download: document.srcFile } : {};
    return {
      preview: session?.preview || bundled.preview || null,
      download: session?.download || bundled.download || null,
    };
  },

  async resetDemoData() {
    for (const id of [...sessionUrls.keys()]) {
      revokeSessionUrl(id);
    }
    records = cloneDemoRecords();
    documents = cloneDemoDocuments();
    persistRecords();
    persistDocuments();
  },
};