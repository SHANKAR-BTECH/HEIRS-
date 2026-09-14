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
  // ── heirs-pol-2026-014: National Digital Learning Policy — 3 docs ──
  {
    id: 'demo-doc-dlp-main',
    recordId: 'heirs-pol-2026-014',
    originalFileName: 'Main Policy.pdf',
    contentType: 'application/pdf',
    fileSize: 245000,
    demo: true,
    srcFile: '/demo-documents/digital_learning_policy_main.pdf',
    uploadedAt: '2026-03-12T09:00:00',
    updatedAt: '2026-03-12T09:00:00',
  },
  {
    id: 'demo-doc-dlp-guidelines',
    recordId: 'heirs-pol-2026-014',
    originalFileName: 'Implementation Guidelines.pdf',
    contentType: 'application/pdf',
    fileSize: 178000,
    demo: true,
    srcFile: '/demo-documents/digital_learning_policy_guidelines.pdf',
    uploadedAt: '2026-03-14T11:30:00',
    updatedAt: '2026-03-14T11:30:00',
  },
  {
    id: 'demo-doc-dlp-annexure',
    recordId: 'heirs-pol-2026-014',
    originalFileName: 'Annexure.pdf',
    contentType: 'application/pdf',
    fileSize: 94000,
    demo: true,
    srcFile: '/demo-documents/digital_learning_policy_annexure.pdf',
    uploadedAt: '2026-03-15T14:20:00',
    updatedAt: '2026-03-15T14:20:00',
  },

  // ── te-pol-2026-078: Skill Enhancement Policy — 3 docs ──
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

  // ── heirs-sch-2025-032: Student Financial Assistance Scheme — 2 docs ──
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

  // ── heirs-pol-2022-026: Ph.D. Scholarship — 1 doc ──
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

  // ── heirs-pol-2025-033: Distance Learning Policy — 2 docs ──
  {
    id: 'demo-doc-dlp2-main',
    recordId: 'heirs-pol-2025-033',
    originalFileName: 'Distance Learning Policy.pdf',
    contentType: 'application/pdf',
    fileSize: 167000,
    demo: true,
    srcFile: '/demo-documents/distance_learning_policy_main.pdf',
    uploadedAt: '2025-09-18T10:15:00',
    updatedAt: '2025-09-18T10:15:00',
  },
  {
    id: 'demo-doc-dlp2-credit',
    recordId: 'heirs-pol-2025-033',
    originalFileName: 'Credit Transfer Guidelines.pdf',
    contentType: 'application/pdf',
    fileSize: 112000,
    demo: true,
    srcFile: '/demo-documents/distance_learning_credit_transfer.pdf',
    uploadedAt: '2025-09-20T09:00:00',
    updatedAt: '2025-09-20T09:00:00',
  },

  // ── ua-pol-2025-044: Digital Governance Policy — 2 docs ──
  {
    id: 'demo-doc-dg-framework',
    recordId: 'ua-pol-2025-044',
    originalFileName: 'E-Administration Framework.pdf',
    contentType: 'application/pdf',
    fileSize: 195000,
    demo: true,
    srcFile: '/demo-documents/digital_governance_framework.pdf',
    uploadedAt: '2025-07-17T13:00:00',
    updatedAt: '2025-07-17T13:00:00',
  },
  {
    id: 'demo-doc-dg-impl',
    recordId: 'ua-pol-2025-044',
    originalFileName: 'Implementation Guide.pdf',
    contentType: 'application/pdf',
    fileSize: 143000,
    demo: true,
    srcFile: '/demo-documents/digital_governance_implementation.pdf',
    uploadedAt: '2025-07-19T16:10:00',
    updatedAt: '2025-07-19T16:10:00',
  },

  // ── heirs-pol-2026-067: Placement and Recruitment Policy — 1 doc ──
  {
    id: 'demo-doc-placement-guidelines',
    recordId: 'heirs-pol-2026-067',
    originalFileName: 'Placement Guidelines.pdf',
    contentType: 'application/pdf',
    fileSize: 156000,
    demo: true,
    srcFile: '/demo-documents/placement_recruitment_guidelines.pdf',
    uploadedAt: '2026-03-02T08:45:00',
    updatedAt: '2026-03-02T08:45:00',
  },

  // ── heirs-sch-2024-018: National Merit Scholarship — metadata only (no bundled file) ──
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

  // ── heirs-pol-2023-019: Women's Empowerment — metadata only ──
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
