import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
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

      <dl className="record-meta">
        <div className="record-department">
          <dt className="sr-only">Department</dt>
          <dd>{record.department}</dd>
        </div>
        <div className="record-reference">
          <dt className="sr-only">Reference number</dt>
          <dd>{record.referenceNumber}</dd>
        </div>
        <div className="record-year">
          <dt className="sr-only">Publication year</dt>
          <dd>{record.publicationYear}</dd>
        </div>
      </dl>

      <div className="record-card-foot">
        <Link to={`/records/${record.id}`} className="record-card-link" aria-label={`View details: ${record.title}`}>
          View Details
          <ArrowRight aria-hidden="true" className="btn-icon" />
        </Link>
      </div>
    </article>
  );
}