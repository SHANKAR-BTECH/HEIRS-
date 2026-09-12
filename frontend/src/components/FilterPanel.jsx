import { ChevronDown, X } from 'lucide-react';
import {
  CATEGORY_OPTIONS,
  YEAR_OPTIONS,
  STATUS_OPTIONS,
  DEPARTMENT_OPTIONS,
  getDefaultFilters,
} from '../lib/searchUtils';
import '../App.css';

export function FilterPanel({ filters, onFiltersChange }) {
  const activeFilters = filters || getDefaultFilters();

  const update = (key, value) => {
    onFiltersChange({ ...activeFilters, [key]: value });
  };

  const clear = () => {
    onFiltersChange(getDefaultFilters());
  };

  const hasActiveFilters =
    activeFilters.category !== 'All' ||
    activeFilters.year !== 'All Years' ||
    activeFilters.status !== 'All' ||
    activeFilters.department !== 'All Departments';

  const renderSelect = (label, value, options, onChange) => (
    <div className="filter-group">
      <label className="filter-label" htmlFor={`filter-${label.toLowerCase()}`}>
        {label}
      </label>
      <div className="select-wrap">
        <select
          id={`filter-${label.toLowerCase()}`}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="form-control select-control"
        >
          {options.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
        <ChevronDown className="select-chevron" aria-hidden="true" />
      </div>
    </div>
  );

  return (
    <div className="filter-panel">
      <div className="filter-header">
        <div className="filter-title-wrap">
          <h3 className="filter-title">Filters</h3>
          {hasActiveFilters && (
            <span className="filter-count">{countActive(activeFilters)} active</span>
          )}
        </div>
        <button type="button" className="btn btn-sm btn-secondary" onClick={clear}>
          Clear Filters
        </button>
      </div>

      <div className="filter-grid">
        {renderSelect('Category', activeFilters.category, CATEGORY_OPTIONS, (v) =>
          update('category', v)
        )}
        {renderSelect('Year', activeFilters.year, YEAR_OPTIONS, (v) => update('year', v))}
        {renderSelect('Status', activeFilters.status, STATUS_OPTIONS, (v) =>
          update('status', v)
        )}
        {renderSelect(
          'Department',
          activeFilters.department,
          DEPARTMENT_OPTIONS,
          (v) => update('department', v)
        )}
      </div>

      {hasActiveFilters && (
        <div className="filter-chips" aria-label="Active filters">
          {activeFilters.category !== 'All' && (
            <Chip label={`Category: ${activeFilters.category}`} onRemove={() => update('category', 'All')} />
          )}
          {activeFilters.year !== 'All Years' && (
            <Chip label={`Year: ${activeFilters.year}`} onRemove={() => update('year', 'All Years')} />
          )}
          {activeFilters.status !== 'All' && (
            <Chip label={`Status: ${activeFilters.status}`} onRemove={() => update('status', 'All')} />
          )}
          {activeFilters.department !== 'All Departments' && (
            <Chip
              label={`Department: ${activeFilters.department}`}
              onRemove={() => update('department', 'All Departments')}
            />
          )}
        </div>
      )}
    </div>
  );
}

function countActive(filters) {
  return [
    filters.category !== 'All',
    filters.year !== 'All Years',
    filters.status !== 'All',
    filters.department !== 'All Departments',
  ].filter(Boolean).length;
}

function Chip({ label, onRemove }) {
  return (
    <span className="filter-chip">
      {label}
      <button
        type="button"
        className="chip-remove"
        onClick={onRemove}
        aria-label={`Remove filter ${label}`}
      >
        <X className="chip-remove-icon" aria-hidden="true" />
      </button>
    </span>
  );
}