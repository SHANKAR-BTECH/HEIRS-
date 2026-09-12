import { useState } from 'react';
import { Menu, X } from 'lucide-react';
import { Routes, Route, Outlet } from 'react-router-dom';
import { AppSidebar } from './components/AppSidebar';
import { AppHeader } from './components/AppHeader';
import { RecordsProvider } from './context/RecordsContext';
import Dashboard from './pages/Dashboard';
import SearchPage from './pages/SearchPage';
import CategoriesPage from './pages/CategoriesPage';
import RecordDetailsPage from './pages/RecordDetailsPage';
import AdminPage from './pages/AdminPage';
import ReportsPage from './pages/ReportsPage';
import './App.css';

function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="app-container">
      <a href="#main-content" className="skip-link">Skip to content</a>
      <button
        type="button"
        className="mobile-menu-toggle"
        aria-label={sidebarOpen ? 'Close navigation' : 'Open navigation'}
        aria-expanded={sidebarOpen}
        aria-controls="primary-navigation"
        onClick={() => setSidebarOpen((open) => !open)}
      >
        {sidebarOpen ? <X size={20} aria-hidden="true" /> : <Menu size={20} aria-hidden="true" />}
      </button>
      <div
        className={`sidebar-overlay ${sidebarOpen ? 'active' : ''}`}
        onClick={() => setSidebarOpen(false)}
        aria-hidden="true"
      />
      <AppSidebar open={sidebarOpen} onNavigate={() => setSidebarOpen(false)} />
      <div className="main-content">
        <AppHeader />
        <main id="main-content" tabIndex={-1} className="content-area">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <RecordsProvider>
      <Routes>
        <Route element={<AppLayout />}>
          <Route index element={<Dashboard />} />
          <Route path="search" element={<SearchPage />} />
          <Route path="categories" element={<CategoriesPage />} />
          <Route path="records/:id" element={<RecordDetailsPage />} />
          <Route path="admin" element={<AdminPage />} />
          <Route path="reports" element={<ReportsPage />} />
          <Route path="*" element={<Dashboard />} />
        </Route>
      </Routes>
    </RecordsProvider>
  );
}