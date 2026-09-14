// API data provider. Reuses the existing Spring Boot API modules exactly as
// they are today; this layer only exposes them behind the shared provider
// interface so components never need to know which mode is active.

import * as recordsApi from '../api/recordsApi';
import * as documentsApi from '../api/documentsApi';

export const apiDataProvider = {
  getAllRecords: (options) => recordsApi.getAllRecords(options),
  getRecordById: (id) => recordsApi.getRecordById(id),
  searchRecords: (params) => recordsApi.searchRecords(params),
  getCategories: () => recordsApi.getCategories(),

  createRecord: (payload) => recordsApi.createRecord(payload),
  updateRecord: (id, payload) => recordsApi.updateRecord(id, payload),
  deleteRecord: (id) => recordsApi.deleteRecord(id),

  getDocuments: (recordId) => documentsApi.getDocuments(recordId),
  uploadDocuments: (recordId, files) => documentsApi.uploadDocuments(recordId, files),
  replaceDocument: (documentId, file) => documentsApi.replaceDocument(documentId, file),
  deleteDocument: (documentId) => documentsApi.deleteDocument(documentId),
  getDocumentUrls: (doc) => ({
    preview: documentsApi.previewDocument(doc.id),
    download: documentsApi.downloadDocument(doc.id),
  }),

  resetDemoData: async () => undefined,
};