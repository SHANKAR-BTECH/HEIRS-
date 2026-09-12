// Reports data provider. Aggregates the mock record catalogue into the
// summary shape the future GET /api/reports/summary endpoint will return.
import { getReportSummary } from '../lib/searchUtils';

export function buildReportSummary(records) {
  return getReportSummary(records);
}