import { apiFetch, API_BASE_URL } from './apiClient';

export const getDocuments = (recordId) => apiFetch(`/api/records/${recordId}/documents`);
export function uploadDocuments(recordId, files) {
  const body = new FormData();
  files.forEach((file) => body.append('files', file));
  return apiFetch(`/api/records/${recordId}/documents`, { method: 'POST', body });
}
export function replaceDocument(documentId, file) {
  const body = new FormData();
  body.append('file', file);
  return apiFetch(`/api/documents/${documentId}/replace`, { method: 'PUT', body });
}
export const deleteDocument = (id) => apiFetch(`/api/documents/${id}`, { method: 'DELETE' });
export const previewDocument = (id) => `${API_BASE_URL}/api/documents/${id}/preview`;
export const downloadDocument = (id) => `${API_BASE_URL}/api/documents/${id}/download`;
