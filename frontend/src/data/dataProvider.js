// Central data-provider selection. Components, pages and RecordsContext import
// ONLY from here. The active provider is chosen once from VITE_DATA_MODE:
//
//   VITE_DATA_MODE=demo -> demoDataProvider (bundled + localStorage, no backend)
//   anything else       -> apiDataProvider (real Spring Boot / MySQL backend)
//
// Both providers expose the same interface, so UI code never knows which mode
// it is running in.

import { isDemoMode, isApiMode, DATA_MODE } from './dataMode';
import { apiDataProvider } from './apiDataProvider';
import { demoDataProvider } from './demoDataProvider';

export { isDemoMode, isApiMode, DATA_MODE };

const active = isDemoMode() ? demoDataProvider : apiDataProvider;

export const getAllRecords = (options) => active.getAllRecords(options);
export const getRecordById = (id) => active.getRecordById(id);
export const searchRecords = (params) => active.searchRecords(params);
export const getCategories = () => active.getCategories();

export const createRecord = (payload) => active.createRecord(payload);
export const updateRecord = (id, payload) => active.updateRecord(id, payload);
export const deleteRecord = (id) => active.deleteRecord(id);

export const getDocuments = (recordId) => active.getDocuments(recordId);
export const uploadDocuments = (recordId, files) => active.uploadDocuments(recordId, files);
export const replaceDocument = (documentId, file) => active.replaceDocument(documentId, file);
export const deleteDocument = (documentId) => active.deleteDocument(documentId);
export const getDocumentUrls = (doc) => active.getDocumentUrls(doc);

export const resetDemoData = () => active.resetDemoData();