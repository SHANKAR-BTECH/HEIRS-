// Demo-mode supporting document metadata.
//
// In demo mode there is no backend file server. A small set of safe sample
// PDFs is bundled under /demo-documents (see frontend/public). Documents that
// reference a bundled file (`srcFile`) support Preview/Download for real;
// documents without a bundled file render the "Preview is unavailable in
// frontend demo mode." message instead so we never fake a remote file.
//
// Timestamps use naive UTC strings, matching what SupportingDocuments expects
// (it appends a trailing "Z" before formatting).

export const demoDocuments = [
  // Skill Enhancement Policy — 3 supporting documents (multi-doc demo).
  {
    id: 'demo-doc-skill-main',
    recordId: 'te-pol-2026-078',
    originalFileName: 'Main Policy.pdf',
    contentType: 'application/pdf',
    fileSize: 184000,
    demo: true,
    srcFile: '/demo-documents/skill_enhancement_policy_main.pdf',
    uploadedAt: '2026-04-15T09:30:00',
    updatedAt: '2026-04-15T09:30:00',
  },
  {
    id: 'demo-doc-skill-amend1',
    recordId: 'te-pol-2026-078',
    originalFileName: 'Amendment 1.pdf',
    contentType: 'application/pdf',
    fileSize: 96000,
    demo: true,
    srcFile: '/demo-documents/skill_enhancement_policy_amendment1.pdf',
    uploadedAt: '2026-04-20T11:05:00',
    updatedAt: '2026-04-20T11:05:00',
  },
  {
    id: 'demo-doc-skill-annex',
    recordId: 'te-pol-2026-078',
    originalFileName: 'Annexure A.pdf',
    contentType: 'application/pdf',
    fileSize: 132000,
    demo: true,
    srcFile: '/demo-documents/skill_enhancement_policy_annexure.pdf',
    uploadedAt: '2026-04-22T16:40:00',
    updatedAt: '2026-04-22T16:40:00',
  },

  // Student Financial Assistance Scheme — two documents.
  {
    id: 'demo-doc-scheme-guidelines',
    recordId: 'heirs-sch-2025-032',
    originalFileName: 'Scheme Guidelines 2025.pdf',
    contentType: 'application/pdf',
    fileSize: 148000,
    demo: true,
    srcFile: '/demo-documents/student_financial_assistance_guidelines.pdf',
    uploadedAt: '2025-01-15T10:00:00',
    updatedAt: '2025-01-15T10:00:00',
  },
  {
    id: 'demo-doc-scheme-checklist',
    recordId: 'heirs-sch-2025-032',
    originalFileName: 'Application Checklist.pdf',
    contentType: 'application/pdf',
    fileSize: 73000,
    demo: true,
    srcFile: '/demo-documents/application_checklist.pdf',
    uploadedAt: '2025-01-16T12:20:00',
    updatedAt: '2025-01-16T12:20:00',
  },

  // Ph.D. Scholarship and Research Policy — one document.
  {
    id: 'demo-doc-phd-guidelines',
    recordId: 'heirs-pol-2022-026',
    originalFileName: 'Ph.D. Fellowship Guidelines.pdf',
    contentType: 'application/pdf',
    fileSize: 210000,
    demo: true,
    srcFile: '/demo-documents/phd_fellowship_guidelines.pdf',
    uploadedAt: '2022-11-05T08:45:00',
    updatedAt: '2022-11-05T08:45:00',
  },

  // National Merit Scholarship — metadata only (no bundled file) to show the
  // graceful "Preview is unavailable in frontend demo mode." behavior.
  {
    id: 'demo-doc-merit-handbook',
    recordId: 'heirs-sch-2024-018',
    originalFileName: 'Merit Evaluation Guidelines.pdf',
    contentType: 'application/pdf',
    fileSize: 118000,
    demo: true,
    srcFile: null,
    uploadedAt: '2024-07-22T09:10:00',
    updatedAt: '2024-07-22T09:10:00',
  },

  // Women's Empowerment in Higher Education Policy — metadata only.
  {
    id: 'demo-doc-women-circular',
    recordId: 'heirs-pol-2023-019',
    originalFileName: 'Implementation Circular.pdf',
    contentType: 'application/pdf',
    fileSize: 88000,
    demo: true,
    srcFile: null,
    uploadedAt: '2023-03-21T15:30:00',
    updatedAt: '2023-03-21T15:30:00',
  },
];

export function cloneDemoDocuments() {
  return demoDocuments.map((document) => ({ ...document }));
}