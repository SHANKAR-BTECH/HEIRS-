// Record API functions bound to the HEIRS Spring Boot REST contract.
// All endpoint shapes follow backend/API.md exactly. Records are normalized
// here so components receive the frontend data shape (e.g. keyword arrays).
import { apiFetch } from './apiClient';

export const MAX_PAGE_SIZE = 100;

// Frontend category aliases -> backend enum label. The approved filter UI
// uses "Rule" while the backend enum is "Rules", so this mapping is required.
const CATEGORY_TO_API = {
  Regulation: 'Regulation',
  Policy: 'Policy',
  Project: 'Project',
  Rule: 'Rules',
  Rules: 'Rules',
  Scheme: 'Scheme',
};

export function categoryToApi(value) {
  if (!value) return '';
  const key = String(value).trim();
  return CATEGORY_TO_API[key] || key;
}

function keywordsToArray(keywords) {
  if (!keywords) return [];
  if (Array.isArray(keywords)) return keywords.map((k) => String(k).trim()).filter(Boolean);
  return String(keywords)
    .split(',')
    .map((keyword) => keyword.trim())
    .filter(Boolean);
}

// Convert a backend record JSON object into the frontend record shape.
export function normalizeRecord(record) {
  if (!record) return record;
  return {
    ...record,
    keywords: keywordsToArray(record.keywords),
  };
}

function normalizeList(payload) {
  if (!payload || !Array.isArray(payload.content)) {
    return { records: [], total: 0 };
  }
  return {
    records: payload.content.map(normalizeRecord),
    total: Number(payload.totalElements) || payload.content.length,
  };
}

export function getAllRecords({ page = 0, size = MAX_PAGE_SIZE } = {}) {
  return apiFetch('/api/records', { params: { page, size } }).then(normalizeList);
}

export function getRecordById(id) {
  return apiFetch(`/api/records/${id}`).then(normalizeRecord);
}

// Build a search request for GET /api/records/search. Only populated
// parameters are sent so the URL stays clean for the required filters.
export function searchRecords({
  q,
  category,
  year,
  status,
  department,
  page = 0,
  size = MAX_PAGE_SIZE,
} = {}) {
  const params = {};
  const query = String(q || '').trim();
  if (query) params.q = query;
  const apiCategory = categoryToApi(category);
  if (apiCategory && apiCategory !== 'All') params.category = apiCategory;
  if (year && year !== 'All Years' && year !== '') params.year = Number(year);
  if (status && status !== 'All') params.status = status;
  if (department && department !== 'All Departments') params.department = department;
  params.page = page;
  params.size = size;
  return apiFetch('/api/records/search', { params }).then(normalizeList);
}

export function getCategories() {
  return apiFetch('/api/categories').then((items) =>
    (Array.isArray(items) ? items : []).map((item) => ({
      code: item?.code,
      name: item?.name,
    }))
  );
}

export function createRecord(payload) {
  return apiFetch('/api/records', { method: 'POST', body: payload }).then(normalizeRecord);
}

export function updateRecord(id, payload) {
  return apiFetch(`/api/records/${id}`, { method: 'PUT', body: payload }).then(normalizeRecord);
}

export function deleteRecord(id) {
  return apiFetch(`/api/records/${id}`, { method: 'DELETE' });
}