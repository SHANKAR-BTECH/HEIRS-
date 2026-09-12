import { Home, Search, FolderKanban, FileText, BarChart2, ChevronRight, Landmark } from 'lucide-react';
import { NavLink } from 'react-router-dom';
import '../App.css';

export function AppSidebar({ open = false, onNavigate }) {
  const navItems = [
    { icon: Home, label: 'Dashboard', to: '/' },
    { icon: Search, label: 'Search & Retrieval', to: '/search' },
    { icon: FolderKanban, label: 'Browse Categories', to: '/categories' },
    { icon: FileText, label: 'Manage Records', to: '/admin' },
    { icon: BarChart2, label: 'Reports', to: '/reports' },
  ];

  return (
    <aside className={`app-sidebar ${open ? 'open' : ''}`}>
      <div className="sidebar-header">
        <div className="sidebar-brand-row">
          <div className="sidebar-logo">
            <Landmark className="logo-mark-icon" aria-hidden="true" />
          </div>
          <div className="sidebar-brand-name">
            <span>HEI</span><span className="sidebar-brand-accent">RS</span>
          </div>
        </div>
        <div className="sidebar-subtitle">Institutional Document Retrieval Portal</div>
      </div>

      <nav className="sidebar-nav" aria-label="Primary navigation">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            onClick={onNavigate}
            className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
            end={item.to === '/'}
          >
            <item.icon className="nav-icon" aria-hidden="true" />
            <span className="nav-label">{item.label}</span>
            <ChevronRight className="nav-arrow" aria-hidden="true" />
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="status-indicator">
          <span className="status-dot active" aria-hidden="true" />
          <span>System Ready</span>
        </div>
        <div className="version-info">Frontend Prototype · HEIRS v1.0</div>
      </div>
    </aside>
  );
}