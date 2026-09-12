import { getStatusVariant, getStatusLabel } from '../lib/searchUtils';
import '../App.css';

// Text plus color so status is never communicated by color alone.
export function StatusBadge({ status, label }) {
  const variant = getStatusVariant(status);
  return (
    <span className={`status-badge-pill ${variant}`}>
      <span className="status-badge-dot" aria-hidden="true" />
      {label || getStatusLabel(status)}
    </span>
  );
}