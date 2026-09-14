import { useCallback, useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search, SlidersHorizontal } from 'lucide-react';
import { useRecords } from '../context/RecordsContext';
import { FilterPanel } from '../components/FilterPanel';
import { RecordCard } from '../components/RecordCard';
import { EmptyState } from '../components/EmptyState';
import { LoadingState, InlineError, CardSkeletons } from '../components/DataState';
import { searchRecords as searchRecordsProvider } from '../data/dataProvider';
import { getDefaultFilters } from '../lib/searchUtils';
import '../App.css';

export default function SearchPage() {
  const { rememberSearch } = useRecords();
  const [searchParams, setSearchParams] = useSearchParams();
  const [query, setQuery] = useState('');
  const [filters, setFilters] = useState(getDefaultFilters());
  const [results, setResults] = useState([]);
  const [totalCount, setTotalCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const requestIdRef = useRef(0);

  useEffect(() => {
    const fromUrl = searchParams.get('q') || '';
    rememberSearch(fromUrl);
    const rawCategory = searchParams.get('category');
    const category = rawCategory === 'Rules' ? 'Rule' : rawCategory || 'All';
    setQuery(fromUrl);
    setFilters((prev) => ({
      ...prev,
      category,
    }));
  }, [searchParams, rememberSearch]);

  const runSearch = useCallback(async (activeQuery, activeFilters) => {
    const requestId = ++requestIdRef.current;
    setLoading(true);
    setError(null);
    try {
      const { records: items, total } = await searchRecordsProvider({
        q: activeQuery,
        category: activeFilters.category,
        year: activeFilters.year,
        status: activeFilters.status,
        department: activeFilters.department,
      });
      if (requestId !== requestIdRef.current) return;
      setResults(items);
      setTotalCount(total);
    } catch (searchError) {
      if (requestId !== requestIdRef.current) return;
      setError(searchError);
      setResults([]);
      setTotalCount(0);
    } finally {
      if (requestId === requestIdRef.current) {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    const requestId = ++requestIdRef.current;
    const timer = window.setTimeout(() => {
      runSearch(query, filters);
    }, 250);
    return () => {
      window.clearTimeout(timer);
      if (requestId === requestIdRef.current) {
        requestIdRef.current += 1;
      }
    };
  }, [query, filters, runSearch]);

  const retrySearch = () => {
    runSearch(query, filters);
  };

  const pushQuery = (q) => {
    const params = new URLSearchParams();
    if (q.trim()) params.set('q', q.trim());
    if (filters.category && filters.category !== 'All') {
      params.set('category', filters.category);
    }
    setSearchParams(params);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    pushQuery(query);
  };

  const handleFiltersChange = (nextFilters) => {
    setFilters(nextFilters);
    const params = new URLSearchParams();
    if (query.trim()) params.set('q', query.trim());
    if (nextFilters.category && nextFilters.category !== 'All') {
      params.set('category', nextFilters.category);
    }
    setSearchParams(params);
  };

  const clearAll = () => {
    setQuery('');
    setFilters(getDefaultFilters());
    setSearchParams({});
  };

  const resultWord = totalCount === 1 ? 'record' : 'records';

  return (
    <div className="page-stack">
      <div className="content-header">
        <h1 className="content-title">Search &amp; Retrieval</h1>
        <p className="content-subtitle">
          Search Higher Education Department records using keywords and filters.
        </p>
      </div>

      <form className="hero-search" onSubmit={handleSubmit} role="search">
        <div className="search-input-wrapper">
          <Search className="search-icon" aria-hidden="true" />
          <label htmlFor="record-search" className="sr-only">
            Search records
          </label>
          <input
            id="record-search"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by title, keyword, department, reference number..."
            className="search-input hero-search-input"
          />
        </div>
        <button type="submit" className="btn btn-primary">
          Search
        </button>
      </form>

      <FilterPanel filters={filters} onFiltersChange={handleFiltersChange} />

      <div className="results-info">
        <h2 className="sr-only">Search results</h2>
        {loading ? (
          <p className="results-count" role="status">
            Loading results...
          </p>
        ) : (
          <p className="results-count" role="status">
            <strong>{totalCount}</strong> {resultWord} found
          </p>
        )}
        <div className="results-tools">
          {query && (
            <span className="result-hint">
              Results for &ldquo;{query}&rdquo;
            </span>
          )}
          {(query ||
            filters.category !== 'All' ||
            filters.year !== 'All Years' ||
            filters.status !== 'All' ||
            filters.department !== 'All Departments') && (
            <button type="button" className="btn btn-sm btn-secondary" onClick={clearAll}>
              <SlidersHorizontal className="btn-icon" aria-hidden="true" />
              Reset
            </button>
          )}
        </div>
      </div>

      {error ? (
        <InlineError
          message="Unable to load records. Please check the backend connection."
          onRetry={retrySearch}
        />
      ) : loading && results.length === 0 ? (
        <CardSkeletons count={3} />
      ) : (
        <>
          {loading && results.length > 0 && (
            <LoadingState label="Updating results..." compact />
          )}
          {results.length === 0 ? (
            <EmptyState
              icon="SearchX"
              title="No matching records found"
              description="Try broader keywords, removing a few filters, or checking the spelling of your search term."
              actionLabel="Clear all filters"
              onAction={clearAll}
            />
          ) : (
            <div className="results-grid section-zone zone-lavender">
              {results.map((record) => (
                <RecordCard key={record.id} record={record} />
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}