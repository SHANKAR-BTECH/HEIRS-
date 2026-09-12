import { getStatusVariant, getStatusLabel } from '../lib/searchUtils';
import '../App.css';

// Text plus color so status is never communicated by color alone.
export function StatusBadge({ status, label }) {
  const variant = getStatusVariant(status);
  const statusKey = String(status || '').trim().toLowerCase();
  const text = label || getStatusLabel(status);
  return (
    <span className={`status-badge-pill ${variant} status-${statusKey}`}>
      <span className={`status-badge-dot status-dot--${statusKey}`} aria-hidden="true" />
      <span>{text}</span>
    </span>
  );
}