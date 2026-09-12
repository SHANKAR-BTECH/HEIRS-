import { Link } from 'react-router-dom';
import { Clock, ArrowRight, Database, FileText, Scale, FolderKanban, ScrollText, HandCoins } from 'lucide-react';
import { useRecords } from '../context/RecordsContext';
import { SearchBar } from '../components/SearchBar';
import { CategoryCard } from '../components/CategoryCard';
import { RecordCard } from '../components/RecordCard';
import { CATEGORY_ICONS, CATEGORY_DESCRIPTIONS, DEFAULT_RECENT_SEARCHES } from '../data/mockCategories';
import {
  getCategoryCounts,
  getRecentRecords,
} from '../lib/searchUtils';
import '../App.css';

export default function Dashboard() {
  const { records } = useRecords();

  const total = records.length;
  const categoryCounts = getCategoryCounts(records);
  const countFor = (key) =>
    categoryCounts.find((entry) => entry.label === key)?.count || 0;
  const recentRecords = getRecentRecords(records, 5);
  const maxCategory = Math.max(
    ...[...categoryCounts, { count: 1 }].map((entry) => entry.count)
  );

  const quickCategories = [
    { key: 'Regulation', count: countFor('Regulation') },
    { key: 'Policy', count: countFor('Policy') },
    { key: 'Project', count: countFor('Project') },
    { key: 'Rules', count: countFor('Rules') },
    { key: 'Scheme', count: countFor('Scheme') },
  ];

  const metricCells = [
    { label: 'Total Records', value: total, icon: Database },
    { label: 'Policies', value: countFor('Policy'), icon: FileText },
    { label: 'Schemes', value: countFor('Scheme'), icon: HandCoins },
    { label: 'Regulations', value: countFor('Regulation'), icon: Scale },
    { label: 'Projects', value: countFor('Project'), icon: FolderKanban },
    { label: 'Rules', value: countFor('Rules'), icon: ScrollText },
  ];

  return (
    <div className="page-stack">
      <section className="dashboard-hero" aria-labelledby="hero-title">
        <p className="hero-eyebrow">HEIRS &middot; Institutional Document Retrieval Portal</p>
        <h2 id="hero-title" className="hero-title">
          Search higher education documents in one place.
        </h2>
        <p className="hero-subtitle">
          Find policies, regulations, schemes, projects and rules across the
          consolidated higher education information repository.
        </p>

        <div className="hero-search-row">
          <SearchBar />
        </div>

        <div className="hero-chip-row" aria-label="Quick category access">
          {quickCategories.map(({ key, count }) => (
            <Link
              key={key}
              to={`/search?category=${encodeURIComponent(key)}`}
              className={`hero-chip ${key.toLowerCase()}`}
            >
              <span>{key}</span>
              <span className="hero-chip-count">{count}</span>
            </Link>
          ))}
        </div>
      </section>

      <section aria-labelledby="metrics-heading">
        <div className="section-heading">
          <h3 id="metrics-heading" className="section-heading-title">
            Repository Overview
          </h3>
        </div>
        <div className="metrics-strip">
          {metricCells.map(({ label, value, icon: Icon }) => (
            <div key={label} className="metric-cell">
              <span className="metric-icon-wrap">
                <Icon aria-hidden="true" />
              </span>
              <span>
                <div className="metric-value">{value}</div>
                <div className="metric-label">{label}</div>
              </span>
            </div>
          ))}
        </div>
      </section>

      <div className="grid grid-2">
        <section className="card detail-section" aria-labelledby="distribution-heading">
          <div className="section-heading compact">
            <h3 id="distribution-heading" className="section-heading-title">
              Category Distribution
            </h3>
          </div>
          <div className="distribution-list">
            {categoryCounts.map(({ label, count }) => {
              const pct = Math.round((count / maxCategory) * 100);
              return (
                <div key={label} className="distribution-row">
                  <div className="distribution-label">
                    <span className={`category-pill ${label.toLowerCase()}`}>{label}</span>
                  </div>
                  <div className="distribution-track">
                    <div className="distribution-fill" style={{ width: `${Math.max(pct, 6)}%` }} />
                  </div>
                  <span className="distribution-value">{count}</span>
                </div>
              );
            })}
          </div>
        </section>

        <section className="card detail-section" aria-labelledby="recent-searches-heading">
          <div className="section-heading compact">
            <h3 id="recent-searches-heading" className="section-heading-title">
              Recent Activity
            </h3>
          </div>
          <ul className="recent-searches-list">
            {DEFAULT_RECENT_SEARCHES.map((search) => (
              <li key={search}>
                <Link to={`/search?q=${encodeURIComponent(search)}`} className="recent-search-item">
                  <Clock className="meta-icon" aria-hidden="true" />
                  <span>{search}</span>
                  <ArrowRight className="meta-icon" aria-hidden="true" />
                </Link>
              </li>
            ))}
          </ul>
        </section>
      </div>

      <section aria-labelledby="category-heading">
        <div className="section-heading">
          <h3 id="category-heading" className="section-heading-title">
            Browse by Category
          </h3>
          <Link to="/categories" className="section-heading-link">
            View all categories <ArrowRight className="btn-icon" aria-hidden="true" />
          </Link>
        </div>
        <div className="grid grid-5">
          {quickCategories.map(({ key, count }) => (
            <CategoryCard
              key={key}
              icon={CATEGORY_ICONS[key]}
              category={key}
              count={count}
              description={CATEGORY_DESCRIPTIONS[key]}
            />
          ))}
        </div>
      </section>

      <section className="card detail-section" aria-labelledby="recent-added-heading">
        <div className="section-heading compact">
          <h3 id="recent-added-heading" className="section-heading-title">
            Recently Added Records
          </h3>
          <Link to="/search" className="section-heading-link">
            View all <ArrowRight className="btn-icon" aria-hidden="true" />
          </Link>
        </div>
        <div className="results-grid">
          {recentRecords.map((record) => (
            <RecordCard key={record.id} record={record} />
          ))}
        </div>
      </section>
    </div>
  );
}