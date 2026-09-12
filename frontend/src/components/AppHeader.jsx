import { User } from 'lucide-react';
import '../App.css';

export function AppHeader() {
  return (
    <header className="app-header">
      <div className="header-content">
        <div className="header-brand">
          <h1 className="header-h1">
            <span className="brand-wordmark">
              HEI<span>RS</span>
            </span>
            <span className="brand-name">Higher Education Information Retrieval System</span>
          </h1>
          <p className="header-subtitle">
            Institutional Document Retrieval Portal &middot; Regulations &middot;
            Policies &middot; Schemes &middot; Projects &middot; Rules
          </p>
        </div>
        <div className="header-actions">
          <div className="system-status">
            <div className="status-badge">
              <span className="status-dot active" aria-hidden="true" />
              <span>System Ready</span>
            </div>
          </div>
          <button
            type="button"
            className="profile-button"
            title="Profile management joins in the backend phase"
            onClick={() =>
              window.alert(
                'Profile management is not part of this frontend prototype. It will be added with the backend integration.'
              )
            }
          >
            <User className="profile-icon" aria-hidden="true" />
            <span>Profile</span>
          </button>
        </div>
      </div>
    </header>
  );
}