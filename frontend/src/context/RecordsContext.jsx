import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { mockRecords } from '../data/mockRecords';

// Frontend-only record store. Mirrors the future Spring Boot CRUD contract
// (GET/POST/PUT/DELETE /api/records) using local state for the prototype.
const RecordsContext = createContext(null);

function generateId(record) {
  const prefixMap = {
    Regulation: 'reg',
    Policy: 'pol',
    Project: 'pro',
    Rules: 'rul',
    Scheme: 'sch',
  };
  const kind = prefixMap[record.category] || 'rec';
  return `${kind}-${Date.now().toString(36)}`;
}

export function RecordsProvider({ children }) {
  const [records, setRecords] = useState(mockRecords);
  const [recentSearches, setRecentSearches] = useState([]);
  const rememberSearch = useCallback((value) => {
    const query = value.trim();
    if (query) {
      setRecentSearches((previous) =>
        [query, ...previous.filter((item) => item !== query)].slice(0, 5)
      );
    }
  }, []);

  const value = useMemo(
    () => ({
      records,
      recentSearches,
      rememberSearch,
      addRecord: (record) =>
        setRecords((prev) => [
          { ...record, id: record.id || generateId(record) },
          ...prev,
        ]),
      updateRecord: (id, updates) =>
        setRecords((prev) =>
          prev.map((record) =>
            record.id === id ? { ...record, ...updates } : record
          )
        ),
      deleteRecord: (id) =>
        setRecords((prev) => prev.filter((record) => record.id !== id)),
    }),
    [records, recentSearches, rememberSearch]
  );

  return (
    <RecordsContext.Provider value={value}>{children}</RecordsContext.Provider>
  );
}

export function useRecords() {
  const context = useContext(RecordsContext);
  if (!context) {
    throw new Error('useRecords must be used within a RecordsProvider');
  }
  return context;
}