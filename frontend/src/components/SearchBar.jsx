import { Search, X } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import '../App.css';

export function SearchBar() {
  const [query, setQuery] = useState('');
  const navigate = useNavigate();

  const handleSearch = (e) => {
    e.preventDefault();
    if (query.trim()) {
      navigate(`/search?q=${encodeURIComponent(query.trim())}`);
    }
  };

  return (
    <form role="search" className="search-bar" onSubmit={handleSearch}>
      <div className="search-input-wrapper">
        <Search className="search-icon" aria-hidden="true" />
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search regulations, policies, schemes, rules or projects..."
          className="search-input"
          aria-label="Search records"
        />
        {query && (
          <button
            type="button"
            onClick={() => setQuery('')}
            className="clear-search"
            aria-label="Clear search"
            title="Clear search"
          >
            <X className="clear-icon" aria-hidden="true" />
          </button>
        )}
      </div>
      <button type="submit" className="search-button">
        Search
      </button>
    </form>
  );
}