// Client-side search and retrieval utilities for mock data.
// These pure functions mirror the operations a future Spring Boot backend
// will provide via GET /api/records/search, /api/reports/summary, etc.

const CATEGORY_ALIASES = {
  regulation: 'Regulation',
  regulations: 'Regulation',
  policy: 'Policy',
  policies: 'Policy',
  project: 'Project',
  projects: 'Project',
  rule: 'Rules',
  rules: 'Rules',
  scheme: 'Scheme',
  schemes: 'Scheme',
};

export const normalizeCategory = (value) => {
  if (!value) return '';
  const key = String(value).trim().toLowerCase();
  return CATEGORY_ALIASES[key] || String(value).trim();
};

const emptyFilters = (overrides = {}) => ({
  query: '',
  category: 'All',
  year: 'All Years',
  status: 'All',
  department: 'All Departments',
  ...overrides,
});

export const getDefaultFilters = () => emptyFilters();

// Case-insensitive search across title, description, keywords, department,
// category and reference number.
export function searchRecords(records, rawFilters = {}) {
  const filters = { ...emptyFilters(), ...rawFilters };
  const query = (filters.query || '').trim().toLowerCase();
  const category = normalizeCategory(filters.category);
  const year = filters.year === 'All Years' ? null : Number(filters.year);
  const status = filters.status === 'All' ? null : filters.status;
  const department =
    filters.department === 'All Departments' ? null : filters.department;

  return records.filter((record) => {
    if (query) {
      const haystack = [
        record.title,
        record.description,
        record.department,
        record.referenceNumber,
        record.category,
        record.source,
        ...(Array.isArray(record.keywords) ? record.keywords : []),
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      if (!haystack.includes(query)) return false;
    }
    if (category && category !== 'All' && normalizeCategory(record.category) !== category) {
      return false;
    }
    if (year && record.publicationYear !== year) return false;
    if (status && record.status !== status) return false;
    if (department && record.department !== department) return false;
    return true;
  });
}

export function getRecordById(records, id) {
  return records.find((record) => record.id === id) || null;
}

// Sort by publication year then title to approximate "recently added".
export function getRecentRecords(records, limit = 5) {
  return [...records]
    .sort(
      (a, b) =>
        b.publicationYear - a.publicationYear || a.title.localeCompare(b.title)
    )
    .slice(0, limit);
}

export function getRelatedRecords(records, record, limit = 3) {
  const category = normalizeCategory(record.category);
  return records
    .filter(
      (r) =>
        r.id !== record.id && normalizeCategory(r.category) === category
    )
    .slice(0, limit);
}

function countBy(records, keyFn) {
  const map = new Map();
  records.forEach((record) => {
    const key = keyFn(record);
    map.set(key, (map.get(key) || 0) + 1);
  });
  return [...map.entries()]
    .map(([label, count]) => ({ label, count }))
    .sort((a, b) => b.count - a.count);
}

export function getCategoryCounts(records) {
  return countBy(records, (r) => normalizeCategory(r.category));
}

export function getStatusCounts(records) {
  return countBy(records, (r) => r.status);
}

export function getYearCounts(records) {
  return countBy(records, (r) => String(r.publicationYear));
}

export function getDepartmentCounts(records, limit = 5) {
  return countBy(records, (r) => r.department).slice(0, limit);
}

export function getReportSummary(records) {
  const total = records.length;
  const byCategory = getCategoryCounts(records);
  const byYear = getYearCounts(records);
  const byStatus = getStatusCounts(records);
  const departments = getDepartmentCounts(records, 6);
  const recentAdditions = getRecentRecords(records, 6);

  return {
    total,
    byCategory,
    byYear,
    byStatus,
    departments,
    recentAdditions,
    activeTotal: byStatus.find((s) => s.label === 'Active')?.count || 0,
    draftTotal: byStatus.find((s) => s.label === 'Draft')?.count || 0,
  };
}

// Key information blocks shown on the record details page. Falls back to
// category-appropriate defaults so every record renders complete detail.
export function getKeyInformation(record) {
  const common = {
    Effective: record.status === 'Active' ? 'In effect' : 'Under review',
    'Applicable Institutions': record.department,
    'Implementation Authority': record.source || record.department,
  };

  const category = normalizeCategory(record.category);
  const byCategory = {
    Regulation: {
      Scope: `Applies to all institutions governed by ${record.department}.`,
      Compliance: 'Annual compliance reporting is mandatory for covered institutions.',
      'Effective Date': record.publishedDate,
    },
    Policy: {
      Objective:
        'Provide a consistent framework for decision-making and administration across higher education institutions.',
      Implementation: 'Implemented through departmental orders and institutional committees.',
    },
    Project: {
      Objective: 'Deliver planned infrastructure, capability and system outcomes within defined timelines.',
      Status: `${record.status} — progress reviewed by the nodal department.`,
      Funding: 'Funded through departmental allocation and state resources.',
    },
    Rules: {
      Applicability: `Binding on all staff and students under ${record.department}.`,
      Enforcement: 'Enforced through institutional administrators and disciplinary procedures.',
    },
    Scheme: {
      Eligibility: 'Open to eligible institutions and students as defined in the scheme guidelines.',
      Benefit: 'Financial and institutional support as per scheme provisions.',
    },
  };

  return { ...(byCategory[category] || {}), ...common };
}


export function getStatusVariant(status) {
  const map = {
    Active: 'success',
    Completed: 'success',
    Draft: 'warning',
    Archived: 'neutral',
  };
  return map[status] || 'neutral';
}

export function getStatusLabel(status) {
  const map = {
    Completed: 'Completed',
    Draft: 'Draft',
    Archived: 'Archived',
  };
  return map[status] || 'Active';
}

// Build a CSV export of the record catalogue.
export function buildCsv(records) {
  const headers = [
    'Title',
    'Category',
    'Department',
    'Reference Number',
    'Publication Year',
    'Published Date',
    'Status',
    'Source',
    'Keywords',
  ];
  const escape = (value) => {
    const text = String(value ?? '');
    return `"${text.replace(/"/g, '""')}"`;
  };
  const rows = records.map((record) =>
    [
      record.title,
      record.category,
      record.department,
      record.referenceNumber,
      record.publicationYear,
      record.publishedDate,
      record.status,
      record.source,
      (record.keywords || []).join(', '),
    ]
      .map(escape)
      .join(',')
  );
  return [headers.map(escape).join(','), ...rows].join('\n');
}

export const CATEGORY_OPTIONS = ['All', 'Regulation', 'Policy', 'Project', 'Rule', 'Scheme'];
export const YEAR_OPTIONS = ['All Years', '2026', '2025', '2024', '2023', '2022'];
export const STATUS_OPTIONS = ['All', 'Active', 'Draft', 'Archived', 'Completed'];
export const DEPARTMENT_OPTIONS = [
  'All Departments',
  'Higher Education Department',
  'Directorate of Collegiate Education',
  'Technical Education',
  'University Administration',
  'Student Welfare',
];
