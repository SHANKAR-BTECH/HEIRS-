import { SearchX, FileX2, Inbox } from 'lucide-react';
import '../App.css';

const ICONS = { SearchX, FileX2, Inbox };

export function EmptyState({
  icon = 'SearchX',
  title = 'No matching records found',
  description,
  actionLabel,
  onAction,
  hue = 'neutral',
}) {
  const IconComponent = ICONS[icon] || SearchX;

  return (
    <div className={`empty-state empty-${hue}`}>
      <div className="empty-icon-circle" aria-hidden="true">
        <IconComponent className="empty-icon" />
      </div>
      <h3 className="empty-title">{title}</h3>
      {description && <p className="empty-description">{description}</p>}
      {actionLabel && onAction && (
        <button type="button" className="btn btn-secondary" onClick={onAction}>
          {actionLabel}
        </button>
      )}
    </div>
  );
}