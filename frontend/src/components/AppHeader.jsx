import { User } from 'lucide-react';
import '../App.css';

export function AppHeader() {
  return (
    <header className="app-header">
      <div className="header-content">
        <div className="header-actions">
          <div className="system-status">
            <div className="status-badge">
              <span className="status-dot active" aria-hidden="true" />
              <span>System Ready</span>
            </div>
          </div>
          <button
            type="button"
            className="btn btn-secondary profile-button"
            title="Profile"
            onClick={() =>
              window.alert(
                'Profile management is not available in this preview.'
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