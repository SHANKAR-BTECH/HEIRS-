// Demo-mode record dataset. Reuses the existing mock catalogue directly so the
// two sources cannot drift apart. Demo records intentionally expose the same
// shape as API records (id, title, description, category, department,
// referenceNumber, publicationYear, publishedDate, status, source, keywords).

import { mockRecords } from './mockRecords';

export const demoRecords = [...mockRecords];

export function cloneDemoRecords() {
  return demoRecords.map((record) => ({
    ...record,
    keywords: Array.isArray(record.keywords) ? [...record.keywords] : [],
  }));
}