import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft,
  Building2,
  Hash,
  CalendarDays,
  FolderOpen,
  Landmark,
  CalendarClock,
  Tag,
} from 'lucide-react';
import { useRecords } from '../context/RecordsContext';
import { StatusBadge } from '../components/StatusBadge';
import { SupportingDocuments } from '../components/SupportingDocuments';
import { EmptyState } from '../components/EmptyState';
import { RecordCard } from '../components/RecordCard';
import { LoadingState, InlineError } from '../components/DataState';
import { getRecordById as getRecordByIdProvider } from '../data/dataProvider';
import {
  getRelatedRecords,
  getKeyInformation,
  normalizeCategory,
} from '../lib/searchUtils';
import '../App.css';

export default function RecordDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { records } = useRecords();
  const [record, setRecord] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const requestIdRef = useRef(0);

  useEffect(() => {
    const requestId = ++requestIdRef.current;
    setLoading(true);
    setError(null);
    setRecord(null);
    getRecordByIdProvider(id)
      .then((loaded) => {
        if (requestId !== requestIdRef.current) return;
        setRecord(loaded);
      })
      .catch((loadError) => {
        if (requestId !== requestIdRef.current) return;
        setError(loadError);
      })
      .finally(() => {
        if (requestId === requestIdRef.current) {
          setLoading(false);
        }
      });
  }, [id]);

  const retry = () => {
    const requestId = ++requestIdRef.current;
    setLoading(true);
    setError(null);
    getRecordByIdProvider(id)
      .then((loaded) => {
        if (requestId !== requestIdRef.current) return;
        setRecord(loaded);
      })
      .catch((loadError) => {
        if (requestId !== requestIdRef.current) return;
        setError(loadError);
      })
      .finally(() => {
        if (requestId === requestIdRef.current) {
          setLoading(false);
        }
      });
  };

  const related = useMemo(
    () => (record ? getRelatedRecords(records, record, 3) : []),
    [records, record]
  );

  if (loading) {
    return (
      <div className="page-stack">
        <div className="content-header">
          <h1 className="content-title">Record Details</h1>
          <p className="content-subtitle">Loading the selected record...</p>
        </div>
        <LoadingState label="Loading record..." />
      </div>
    );
  }

  if (error && (error.status === 404 || error.status === 400)) {
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

  if (error) {
    return (
      <div className="page-stack">
        <div className="content-header">
          <h1 className="content-title">Record Details</h1>
          <p className="content-subtitle">This record could not be loaded.</p>
        </div>
        <InlineError
          message="Unable to load this record. Please check the backend connection."
          onRetry={retry}
        />
      </div>
    );
  }

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
        <h1 className="detail-title">{record.title}</h1>
      </header>

      <div className="detail-grid">
        <div className="detail-main">
          <section className="detail-section detail-section-flush" aria-labelledby="metadata-heading">
            <h2 id="metadata-heading" className="detail-section-title">
              Record Metadata
            </h2>
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

          <section className="detail-section detail-section-flush" aria-labelledby="overview-heading">
            <h2 id="overview-heading" className="detail-section-title">Overview</h2>
            <p className="detail-paragraph">{record.description}</p>
          </section>

          <section className="detail-section detail-section-flush" aria-labelledby="key-info-heading">
            <h2 id="key-info-heading" className="detail-section-title">Key Information</h2>
            <dl className="key-info-list">
              {Object.entries(keyInfo).map(([label, value]) => (
                <div key={label} className="key-info-item">
                  <dt className="key-info-label">{label}</dt>
                  <dd className="key-info-value">{value}</dd>
                </div>
              ))}
            </dl>
          </section>

          <section className="detail-section detail-section-flush" aria-labelledby="docs-heading">
            <h2 id="docs-heading" className="detail-section-title">Supporting Documents</h2>
            <SupportingDocuments key={record.id} recordId={record.id} />
          </section>
        </div>

        <aside className="detail-side">
          <section className="card detail-section" aria-labelledby="keywords-heading">
            <h2 id="keywords-heading" className="detail-section-title">
              <Tag className="section-icon" aria-hidden="true" /> Keywords
            </h2>
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
        </aside>
      </div>

      {related.length > 0 && (
        <section aria-labelledby="related-cards-heading" className="related-cards-section">
          <h2 id="related-cards-heading" className="detail-section-title">Related Records</h2>
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
