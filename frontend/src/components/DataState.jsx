import { AlertCircle, Loader2 } from 'lucide-react';
import '../App.css';

// Small centered loading indicator, matching the approved typography.
export function LoadingState({ label = 'Loading records...', compact = false }) {
  return (
    <div className={`async-state ${compact ? 'async-state-compact' : ''}`} role="status">
      <Loader2 className="async-spinner" aria-hidden="true" />
      <p className="async-message">{label}</p>
    </div>
  );
}

// Inline backend-error state with a Retry action.
export function InlineError({ message, onRetry, retryLabel = 'Retry' }) {
  return (
    <div className={`async-error ${onRetry ? 'has-retry' : ''}`} role="alert">
      <AlertCircle className="async-error-icon" aria-hidden="true" />
      <p className="async-message">
        {message || 'Unable to load records. Please check the backend connection.'}
      </p>
      {onRetry && (
        <button type="button" className="btn btn-secondary" onClick={onRetry}>
          {retryLabel}
        </button>
      )}
    </div>
  );
}

// Skeleton record-card placeholders shown while records load.
export function CardSkeletons({ count = 3 }) {
  return (
    <div className="results-grid" aria-hidden="true">
      {Array.from({ length: count }, (_, index) => (
        <div className="record-card record-card-skeleton" key={index}>
          <div className="skeleton-block skeleton-badge" />
          <div className="skeleton-block skeleton-title" />
          <div className="skeleton-block skeleton-line" />
          <div className="skeleton-block skeleton-line short" />
          <div className="skeleton-meta">
            <div className="skeleton-block skeleton-meta-line" />
            <div className="skeleton-block skeleton-meta-line" />
          </div>
        </div>
      ))}
    </div>
  );
}