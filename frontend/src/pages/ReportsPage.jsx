import { useMemo, useState } from 'react';
import {
  Database,
  FileText,
  CheckCircle2,
  Clock,
  Download,
  Landmark,
  Building2,
} from 'lucide-react';
import { useRecords } from '../context/RecordsContext';
import { StatusBadge } from '../components/StatusBadge';
import { LoadingState, InlineError } from '../components/DataState';
import { buildReportSummary } from '../data/mockReports';
import { buildCsv } from '../lib/searchUtils';
import '../App.css';

export default function ReportsPage() {
  const { records, loading, error, retry } = useRecords();
  const [exported, setExported] = useState(false);

  const report = useMemo(() => buildReportSummary(records), [records]);
  const maxCategory = useMemo(
    () => Math.max(...report.byCategory.map((entry) => entry.count), 1),
    [report]
  );

  if (loading) {
    return (
      <div className="page-stack">
        <div className="content-header report-header">
          <div>
            <h2 className="content-title">Reports</h2>
            <p className="content-subtitle">Loading report statistics...</p>
          </div>
        </div>
        <LoadingState label="Loading report data..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="page-stack">
        <div className="content-header report-header">
          <div>
            <h2 className="content-title">Reports</h2>
            <p className="content-subtitle">Report statistics could not be loaded.</p>
          </div>
        </div>
        <InlineError
          message="Unable to load report data. Please check the backend connection."
          onRetry={retry}
        />
      </div>
    );
  }

  const handleExport = () => {
    const csv = buildCsv(records);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'higher-education-records-summary.csv';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    setExported(true);
    window.setTimeout(() => setExported(false), 3000);
  };

  return (
    <div className="page-stack">
      <div className="content-header report-header">
        <div>
          <h2 className="content-title">Reports</h2>
          <p className="content-subtitle">
            Summary statistics of the records stored in the information system.
          </p>
        </div>
        <button type="button" className="btn btn-primary" onClick={handleExport}>
          <Download className="btn-icon" aria-hidden="true" />
          Export Summary
        </button>
      </div>

      {exported && (
        <div className="toast" role="status">
          Summary CSV downloaded.
        </div>
      )}

      <div className="report-stats-grid">
        <ReportStat
          icon={Database}
          label="Total Records"
          value={report.total}
          hint="Across all categories"
        />
        <ReportStat
          icon={FileText}
          label="Active Records"
          value={report.activeTotal}
          hint="Currently in force"
        />
        <ReportStat
          icon={Clock}
          label="In Draft"
          value={report.draftTotal}
          hint="Under preparation"
        />
        <ReportStat
          icon={Landmark}
          label="Categories"
          value={report.byCategory.length}
          hint="Record classes"
        />
      </div>

      <div className="report-grid">
        <section className="card detail-section" aria-labelledby="by-category-heading">
          <h3 id="by-category-heading" className="detail-section-title">Records by Category</h3>
          <BarDistribution items={report.byCategory} max={maxCategory} />
        </section>

        <section className="card detail-section" aria-labelledby="by-status-heading">
          <h3 id="by-status-heading" className="detail-section-title">Records by Status</h3>
          <StatusDistribution items={report.byStatus} />
        </section>
      </div>

      <div className="report-grid">
        <section className="card detail-section" aria-labelledby="by-year-heading">
          <h3 id="by-year-heading" className="detail-section-title">Records by Year</h3>
          <BarDistribution
            items={report.byYear.slice().sort((a, b) => a.label.localeCompare(b.label))}
            max={Math.max(...report.byYear.map((entry) => entry.count), 1)}
          />
        </section>

        <section className="card detail-section" aria-labelledby="by-dept-heading">
          <h3 id="by-dept-heading" className="detail-section-title">Most Common Departments</h3>
          <BarDistribution
            items={report.departments}
            max={Math.max(...report.departments.map((entry) => entry.count), 1)}
          />
        </section>
      </div>

      <section className="card detail-section" aria-labelledby="recent-heading">
        <h3 id="recent-heading" className="detail-section-title">
          <Building2 className="section-icon" aria-hidden="true" /> Recent Additions
        </h3>
        <div className="table-scroll">
          <table className="table admin-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Category</th>
                <th>Department</th>
                <th>Year</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {report.recentAdditions.map((record) => (
                <tr key={record.id}>
                  <td className="admin-title-cell">
                    <span className="admin-title">{record.title}</span>
                    <span className="admin-ref">{record.referenceNumber}</span>
                  </td>
                  <td>
                    <span className={`category-pill ${record.category.toLowerCase()}`}>
                      {record.category}
                    </span>
                  </td>
                  <td>{record.department}</td>
                  <td>{record.publicationYear}</td>
                  <td>
                    <StatusBadge status={record.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

function ReportStat({ icon: Icon, label, value, hint }) {
  return (
    <div className="report-stat">
      <div className="report-stat-icon">
        <Icon aria-hidden="true" />
      </div>
      <div className="report-stat-body">
        <p className="report-stat-value">{value}</p>
        <p className="report-stat-label">{label}</p>
        <p className="report-stat-hint">{hint}</p>
      </div>
    </div>
  );
}

const CATEGORY_COLOR_MAP = {
  Policy: '#566AA3',
  Scheme: '#8A6FAE',
  Regulation: '#6B5B95',
  Project: '#4E7A73',
  Rules: '#A06D45',
  Rule: '#A06D45',
};

const DEFAULT_BAR_PALETTE = ['#566AA3', '#8A6FAE', '#6B5B95', '#4E7A73', '#A06D45'];

function BarDistribution({ items, max }) {
  return (
    <ul className="bar-distribution">
      {items.map((entry, index) => {
        const width = Math.round((entry.count / max) * 100);
        const color =
          CATEGORY_COLOR_MAP[entry.label] ||
          DEFAULT_BAR_PALETTE[index % DEFAULT_BAR_PALETTE.length];
        return (
          <li key={entry.label} className="bar-row">
            <span className="bar-label">{entry.label}</span>
            <div className="bar-track" role="img" aria-label={`${entry.label}: ${entry.count}`}>
              <div
                className="bar-fill"
                style={{ width: `${Math.max(width, 4)}%`, backgroundColor: color }}
              />
            </div>
            <span className="bar-count">{entry.count}</span>
          </li>
        );
      })}
    </ul>
  );
}

function StatusDistribution({ items }) {
  return (
    <ul className="status-distribution">
      {items.map((entry) => (
        <li key={entry.label} className="status-row">
          <StatusBadge status={entry.label} />
          <span className="status-row-count">{entry.count}</span>
        </li>
      ))}
      <li className="status-row total-row">
        <CheckCircle2 className="meta-icon" aria-hidden="true" />
        <span>Total</span>
        <span className="status-row-count">
          {items.reduce((sum, entry) => sum + entry.count, 0)}
        </span>
      </li>
    </ul>
  );
}