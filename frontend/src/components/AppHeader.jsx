import { User } from 'lucide-react';
import { isDemoMode } from '../data/dataProvider';
import '../App.css';

export function AppHeader() {
  const demoMode = isDemoMode();

  return (
    <header className="app-header">
      <div className="header-content">
        <div className="header-actions">
          <div className="system-status" title={demoMode ? 'Frontend demo mode — data is stored locally in this browser' : undefined}>
            <div className="status-badge">
              <span className="status-dot active" aria-hidden="true" />
              <span>{demoMode ? 'Demo Ready' : 'System Ready'}</span>
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