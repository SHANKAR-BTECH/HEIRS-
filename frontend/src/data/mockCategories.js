import { Scale, FileText, FolderKanban, ScrollText, HandCoins } from 'lucide-react';

// Category catalogue with icons and descriptions used across the dashboard,
// browse page and search filters.
export const CATEGORY_DESCRIPTIONS = {
  Regulation:
    'Official regulatory frameworks governing accreditation, compliance and institutional conduct.',
  Policy:
    'Departmental policy decisions and operational frameworks guiding higher education administration.',
  Project:
    'Ongoing and completed Higher Education Department projects for infrastructure and capability.',
  Rules:
    'Administrative and institutional rules covering service, conduct and operational procedures.',
  Scheme:
    'Financial assistance, welfare and development schemes for students, faculty and institutions.',
};

export const CATEGORY_ICONS = {
  Regulation: Scale,
  Policy: FileText,
  Project: FolderKanban,
  Rules: ScrollText,
  Scheme: HandCoins,
};

export const CATEGORY_KEYS = ['Regulation', 'Policy', 'Project', 'Rules', 'Scheme'];

export const DEFAULT_RECENT_SEARCHES = [
  'scholarship eligibility',
  'university accreditation',
  'faculty recruitment policy',
  'student financial assistance',
  'digital learning',
];