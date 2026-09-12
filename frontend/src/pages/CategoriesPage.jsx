import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { useRecords } from '../context/RecordsContext';
import {
  CATEGORY_KEYS,
  CATEGORY_DESCRIPTIONS,
  CATEGORY_ICONS,
} from '../data/mockCategories';
import { getRecentRecords, normalizeCategory } from '../lib/searchUtils';
import '../App.css';

export default function CategoriesPage() {
  const navigate = useNavigate();
  const { records } = useRecords();

  const categorySummary = useMemo(() => {
    const counts = new Map();
    records.forEach((record) => {
      const key = normalizeCategory(record.category);
      counts.set(key, (counts.get(key) || 0) + 1);
    });
    return CATEGORY_KEYS.map((key) => ({
      key,
      count: counts.get(key) || 0,
      recent: getRecentRecords(
        records.filter((record) => normalizeCategory(record.category) === key),
        3
      ),
    }));
  }, [records]);

  return (
    <div className="page-stack">
      <div className="content-header">
        <h2 className="content-title">Browse Categories</h2>
        <p className="content-subtitle">
          Explore the five record classes maintained by the higher education
          information repository.
        </p>
      </div>

      <div className="category-list">
        {categorySummary.map(({ key, count, recent }) => {
          const Icon = CATEGORY_ICONS[key];
          return (
            <article key={key} className={`category-panel ${key.toLowerCase()}`}>
              <div className="category-panel-head">
                <div className="category-icon-square">
                  <Icon className="category-icon" aria-hidden="true" />
                </div>
                <div className="category-panel-title">
                  <h3 className="category-name">{key}</h3>
                  <p className="category-description">{CATEGORY_DESCRIPTIONS[key]}</p>
                </div>
                <span className="category-count-badge">{count} records</span>
              </div>

              {recent.length > 0 && (
                <div className="category-recent">
                  <p className="category-recent-label">Recent records</p>
                  <ul className="category-recent-list">
                    {recent.map((record) => (
                      <li key={record.id}>
                        <button
                          type="button"
                          className="category-recent-item"
                          onClick={() => navigate(`/records/${record.id}`)}
                        >
                          <span className="recent-item-title">{record.title}</span>
                          <span className="recent-item-ref">{record.referenceNumber}</span>
                        </button>
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              <button
                type="button"
                className="btn btn-primary"
                onClick={() => navigate(`/search?category=${encodeURIComponent(key)}`)}
              >
                Browse {key}
                <ArrowRight className="btn-icon" aria-hidden="true" />
              </button>
            </article>
          );
        })}
      </div>
    </div>
  );
}