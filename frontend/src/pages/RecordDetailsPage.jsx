import { useMemo } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import {
  ArrowLeft,
  ArrowRight,
  Building2,
  Hash,
  CalendarDays,
  FileText,
  FileDown,
  FolderOpen,
  Landmark,
  CalendarClock,
  Tag,
} from 'lucide-react';
import { useRecords } from '../context/RecordsContext';
import { StatusBadge } from '../components/StatusBadge';
import { EmptyState } from '../components/EmptyState';
import { RecordCard } from '../components/RecordCard';
import {
  getRecordById,
  getRelatedRecords,
  getKeyInformation,
  getDocuments,
  normalizeCategory,
} from '../lib/searchUtils';
import '../App.css';

export default function RecordDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { records } = useRecords();

  const record = useMemo(() => getRecordById(records, id), [records, id]);

  if (!record) {
    return (
      <div className="page-stack">
        <EmptyState
          icon="FileX2"
          title="Record not found"
          description="The record you are looking for does not exist or may have been removed."
          actionLabel="Back to Search"
          onAction={() => navigate('/search')}
        />
      </div>
    );
  }

  const category = normalizeCategory(record.category);
  const keyInfo = getKeyInformation(record);
  const documents = getDocuments(record);
  const related = getRelatedRecords(records, record, 3);

  const metadata = [
    { label: 'Reference Number', value: record.referenceNumber, icon: Hash },
    { label: 'Department', value: record.department, icon: Building2 },
    { label: 'Published Date', value: record.publishedDate, icon: CalendarDays },
    { label: 'Publication Year', value: record.publicationYear, icon: CalendarClock },
    { label: 'Category', value: category, icon: FolderOpen },
    { label: 'Status', value: record.status, icon: Landmark },
  ];

  return (
    <div className="page-stack">
      <button
        type="button"
        className="btn btn-sm btn-secondary back-link"
        onClick={() => navigate(-1)}
      >
        <ArrowLeft className="btn-icon" aria-hidden="true" />
        Back
      </button>

      <header className="detail-header">
        <div className="detail-badges">
          <span className={`category-pill ${category.toLowerCase()}`}>{category}</span>
          <StatusBadge status={record.status} />
        </div>
        <h2 className="detail-title">{record.title}</h2>
        <p className="detail-subtitle">{record.description}</p>
      </header>

      <div className="detail-grid">
        <div className="detail-main">
          <section className="card detail-section" aria-labelledby="metadata-heading">
            <h3 id="metadata-heading" className="detail-section-title">
              Record Metadata
            </h3>
            <dl className="metadata-list">
              {metadata.map(({ label, value, icon: IconComponent }) => (
                <div key={label} className="metadata-item">
                  <dt className="metadata-label">
                    <IconComponent className="meta-icon" aria-hidden="true" />
                    {label}
                  </dt>
                  <dd className="metadata-value">{value}</dd>
                </div>
              ))}
            </dl>
          </section>

          <section className="card detail-section" aria-labelledby="overview-heading">
            <h3 id="overview-heading" className="detail-section-title">Overview</h3>
            <p className="detail-paragraph">{record.description}</p>
            <p className="detail-paragraph">
              This {category.toLowerCase()} is maintained by the{' '}
              {record.department} and is identified by reference number{' '}
              {record.referenceNumber}. It is part of the consolidated Higher
              Education information repository available to institutions,
              administrators and the public.
            </p>
          </section>

          <section className="card detail-section" aria-labelledby="key-info-heading">
            <h3 id="key-info-heading" className="detail-section-title">Key Information</h3>
            <dl className="key-info-list">
              {Object.entries(keyInfo).map(([label, value]) => (
                <div key={label} className="key-info-item">
                  <dt className="key-info-label">{label}</dt>
                  <dd className="key-info-value">{value}</dd>
                </div>
              ))}
            </dl>
          </section>

          <section className="card detail-section" aria-labelledby="docs-heading">
            <h3 id="docs-heading" className="detail-section-title">Supporting Documents</h3>
            <ul className="document-list">
              {documents.map((doc) => (
                <li key={doc.name} className="document-item">
                  <div className="document-icon">
                    <FileText className="meta-icon" aria-hidden="true" />
                  </div>
                  <div className="document-info">
                    <p className="document-name">{doc.name}</p>
                    <p className="document-meta">
                      {doc.type} &middot; {doc.size}
                    </p>
                  </div>
                  <button
                    type="button"
                    className="btn btn-sm btn-secondary document-download"
                    onClick={() => {
                      window.alert(
                        'Document download will be available when backend integration is connected.'
                      );
                    }}
                  >
                    <FileDown className="btn-icon" aria-hidden="true" />
                    Download
                  </button>
                </li>
              ))}
            </ul>
            <p className="prototype-note">
              Prototype mode: files are placeholders. Download becomes active with the backend integration.
            </p>
          </section>
        </div>

        <aside className="detail-side">
          <section className="card detail-section" aria-labelledby="keywords-heading">
            <h3 id="keywords-heading" className="detail-section-title">
              <Tag className="section-icon" aria-hidden="true" /> Keywords
            </h3>
            <div className="detail-tags">
              {(record.keywords || []).map((keyword) => (
                <span key={keyword} className="tag">
                  {keyword}
                </span>
              ))}
              {(!record.keywords || record.keywords.length === 0) && (
                <p className="detail-paragraph muted">No keywords listed.</p>
              )}
            </div>
          </section>

          <section className="card detail-section" aria-labelledby="related-heading">
            <h3 id="related-heading" className="detail-section-title">Related {category}</h3>
            {related.length > 0 ? (
              <ul className="related-list">
                {related.map((relatedRecord) => (
                  <li key={relatedRecord.id}>
                    <Link to={`/records/${relatedRecord.id}`} className="related-item">
                      <span className="related-item-title">{relatedRecord.title}</span>
                      <span className="related-item-ref">
                        {relatedRecord.referenceNumber}
                        <ArrowRight className="related-arrow" aria-hidden="true" />
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="detail-paragraph muted">No related records available.</p>
            )}
          </section>
        </aside>
      </div>

      {related.length > 0 && (
        <section aria-labelledby="related-cards-heading" className="related-cards-section">
          <h3 id="related-cards-heading" className="detail-section-title">More in this category</h3>
          <div className="results-grid">
            {related.map((relatedRecord) => (
              <RecordCard key={relatedRecord.id} record={relatedRecord} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}