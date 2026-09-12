import { useState } from 'react';
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
      <button
        type="button"
        className="mobile-menu-toggle"
        aria-label="Toggle navigation"
        onClick={() => setSidebarOpen((open) => !open)}
      >
        <svg
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <line x1="4" y1="12" x2="20" y2="12" />
          <line x1="4" y1="6" x2="20" y2="6" />
          <line x1="4" y1="18" x2="20" y2="18" />
        </svg>
      </button>
      <div
        className={`sidebar-overlay ${sidebarOpen ? 'active' : ''}`}
        onClick={() => setSidebarOpen(false)}
        aria-hidden="true"
      />
      <AppSidebar open={sidebarOpen} onNavigate={() => setSidebarOpen(false)} />
      <div className="main-content">
        <AppHeader />
        <main className="content-area">
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