import { Link } from 'react-router-dom';
import {
  ArrowRight,
  Clock,
  Database,
  FileText,
  HandCoins,
  Landmark,
  FolderKanban,
  BookOpen,
} from 'lucide-react';
import { useRecords } from '../context/RecordsContext';
import { SearchBar } from '../components/SearchBar';
import { CategoryCard } from '../components/CategoryCard';
import { RecordCard } from '../components/RecordCard';
import { LoadingState, InlineError, CardSkeletons } from '../components/DataState';
import { EmptyState } from '../components/EmptyState';
import { CATEGORY_ICONS, CATEGORY_DESCRIPTIONS } from '../data/mockCategories';
import {
  getCategoryCounts,
  getRecentRecords,
} from '../lib/searchUtils';
import '../App.css';

export default function Dashboard() {
  const { records, loading, error, categoriesLoading, retry, recentSearches } = useRecords();

  const total = records.length;
  const categoryCounts = getCategoryCounts(records);
  const countFor = (key) =>
    categoryCounts.find((entry) => entry.label === key)?.count || 0;
  const recentRecords = getRecentRecords(records, 6);
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
    { label: 'Regulations', value: countFor('Regulation'), icon: Landmark },
    { label: 'Projects', value: countFor('Project'), icon: FolderKanban },
    { label: 'Rules', value: countFor('Rules'), icon: BookOpen },
  ];

  return (
    <div className="page-stack">
      <section className="dashboard-hero" aria-labelledby="hero-title">
        <h1 id="hero-title" className="hero-title">
          Higher Education Information Retrieval
        </h1>
        <p className="hero-subtitle">
          Search and manage regulations, policies, schemes, projects and rules.
        </p>

        <div className="hero-search-row section-zone zone-periwinkle">
          <SearchBar />
        </div>
      </section>

      {error && (
        <InlineError
          message="Unable to load records. Please check the backend connection."
          onRetry={retry}
        />
      )}

      <section aria-labelledby="metrics-heading">
        <div className="section-heading">
          <h2 id="metrics-heading" className="section-heading-title">
            Repository Overview
          </h2>
        </div>
        <div className="metrics-strip">
          {metricCells.map(({ label, value, icon: Icon }) => (
            <div key={label} className="metric-cell">
              <div className="metric-icon-wrap" aria-hidden="true">
                <Icon className="metric-icon" />
              </div>
              <div className="metric-data">
                {loading ? (
                  <>
                    <div className="skeleton-block skeleton-metric-value" />
                    <div className="skeleton-block skeleton-metric-label" />
                  </>
                ) : (
                  <>
                    <div className="metric-value">{value}</div>
                    <div className="metric-label">{label}</div>
                  </>
                )}
              </div>
            </div>
          ))}
        </div>
      </section>

      <section aria-labelledby="recent-added-heading">
        <div className="section-heading compact">
          <h2 id="recent-added-heading" className="section-heading-title">
            Recent Records
          </h2>
          <Link to="/search" className="section-heading-link">
            View all <ArrowRight className="btn-icon" aria-hidden="true" />
          </Link>
        </div>
        {loading ? (
          <CardSkeletons count={3} />
        ) : error ? (
          <div className="results-grid">
            <InlineError message="Recent records are unavailable while the backend is offline." onRetry={retry} />
          </div>
        ) : recentRecords.length > 0 ? (
          <div className="results-grid">
            {recentRecords.map((record) => (
              <RecordCard key={record.id} record={record} />
            ))}
          </div>
        ) : (
          <EmptyState
            icon="Inbox"
            title="No records found"
            description="The repository has no records yet. Add a record from the Manage Records page."
          />
        )}
      </section>

      <section className="section-zone zone-mist" aria-labelledby="category-heading">
        <div className="section-heading">
          <h2 id="category-heading" className="section-heading-title">
            Browse by Category
          </h2>
          <Link to="/categories" className="section-heading-link">
            View all categories <ArrowRight className="btn-icon" aria-hidden="true" />
          </Link>
        </div>
        {categoriesLoading ? (
          <LoadingState label="Loading categories..." compact />
        ) : (
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
        )}
      </section>

      <section className="section-zone zone-gray" aria-labelledby="recent-searches-heading">
        <div className="section-heading">
          <h2 id="recent-searches-heading" className="section-heading-title">Recent Searches</h2>
        </div>
        {recentSearches && recentSearches.length ? (
          <ul className="recent-searches-list">
            {recentSearches.map((query) => (
              <li key={query}>
                <Link className="recent-search-link" to={`/search?q=${encodeURIComponent(query)}`}>
                  <Clock className="meta-icon" aria-hidden="true" />
                  {query}
                </Link>
              </li>
            ))}
          </ul>
        ) : (
          <p className="content-subtitle">Searches from this session will appear here.</p>
        )}
      </section>
    </div>
  );
}