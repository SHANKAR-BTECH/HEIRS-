import { Link } from 'react-router-dom';
import { Building2, Hash, CalendarDays, ArrowRight } from 'lucide-react';
import { StatusBadge } from './StatusBadge';
import { normalizeCategory } from '../lib/searchUtils';
import { CATEGORY_ICONS } from '../data/mockCategories';
import '../App.css';

export function RecordCard({ record }) {
  const category = normalizeCategory(record.category);
  const CategoryIcon = CATEGORY_ICONS[category] || CATEGORY_ICONS.Regulation;

  return (
    <article className={`record-card ${category.toLowerCase()}`}>
      <div className="record-card-top">
        <span className="record-cat">
          <span className={`record-cat-icon ${category.toLowerCase()}`}>
            <CategoryIcon aria-hidden="true" />
          </span>
          <span className={`category-pill ${category.toLowerCase()}`}>{category}</span>
        </span>
        <StatusBadge status={record.status} />
      </div>

      <h3 className="record-title">{record.title}</h3>

      <p className="record-description">{record.description}</p>

      {Array.isArray(record.keywords) && record.keywords.length > 0 && (
        <div className="record-card-keywords" aria-label="Keywords">
          {record.keywords.slice(0, 3).map((keyword) => (
            <span key={keyword} className="tag">
              {keyword}
            </span>
          ))}
        </div>
      )}

      <div className="record-meta">
        <span className="record-meta-item" title={record.referenceNumber}>
          <Hash aria-hidden="true" className="meta-icon" />
          <span>{record.referenceNumber}</span>
        </span>
        <span className="record-meta-item" title={record.department}>
          <Building2 aria-hidden="true" className="meta-icon" />
          <span>{record.department}</span>
        </span>
        <span className="record-meta-item">
          <CalendarDays aria-hidden="true" className="meta-icon" />
          <span>{record.publicationYear}</span>
        </span>
      </div>

      <div className="record-card-foot">
        <Link to={`/records/${record.id}`} className="record-card-link">
          View Details
          <ArrowRight aria-hidden="true" className="btn-icon" />
        </Link>
      </div>
    </article>
  );
}