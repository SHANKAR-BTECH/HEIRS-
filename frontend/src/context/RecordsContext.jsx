import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import {
  getAllRecords,
  getCategories,
  createRecord,
  updateRecord as updateRecordApi,
  deleteRecord as deleteRecordApi,
} from '../api/recordsApi';

// API-backed record store. All runtime data comes from the Spring Boot
// backend; local state is refreshed cache that pages render.
const RecordsContext = createContext(null);

export function RecordsProvider({ children }) {
  const [records, setRecords] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [categoriesLoading, setCategoriesLoading] = useState(true);
  const [error, setError] = useState(null);
  const [categoriesError, setCategoriesError] = useState(null);
  const [recentSearches, setRecentSearches] = useState([]);

  const rememberSearch = useCallback((value) => {
    const query = String(value || '').trim();
    if (query) {
      setRecentSearches((previous) =>
        [query, ...previous.filter((item) => item !== query)].slice(0, 5)
      );
    }
  }, []);

  const loadRecords = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const { records: loaded } = await getAllRecords();
      setRecords(loaded);
      return loaded;
    } catch (loadError) {
      setError(loadError);
      setRecords([]);
      throw loadError;
    } finally {
      setLoading(false);
    }
  }, []);

  const loadCategories = useCallback(async () => {
    setCategoriesLoading(true);
    setCategoriesError(null);
    try {
      const items = await getCategories();
      setCategories(items.map((item) => item.name).filter(Boolean));
      return items;
    } catch (loadError) {
      setCategoriesError(loadError);
      setCategories([]);
      throw loadError;
    } finally {
      setCategoriesLoading(false);
    }
  }, []);

  useEffect(() => {
    loadRecords().catch(() => {});
    loadCategories().catch(() => {});
  }, [loadRecords, loadCategories]);

  const retry = useCallback(() => {
    return Promise.all([loadRecords(), loadCategories()]).then(() => undefined);
  }, [loadRecords, loadCategories]);

  const addRecord = useCallback(async (payload) => {
    const created = await createRecord(payload);
    setRecords((prev) => [created, ...prev]);
    return created;
  }, []);

  const updateRecord = useCallback(async (id, payload) => {
    const updated = await updateRecordApi(id, payload);
    setRecords((prev) =>
      prev.map((record) => (record.id === id ? updated : record))
    );
    return updated;
  }, []);

  const deleteRecord = useCallback(async (id) => {
    await deleteRecordApi(id);
    setRecords((prev) => prev.filter((record) => record.id !== id));
  }, []);

  const value = useMemo(
    () => ({
      records,
      categories,
      loading,
      categoriesLoading,
      error,
      categoriesError,
      recentSearches,
      rememberSearch,
      refresh: loadRecords,
      retry,
      addRecord,
      updateRecord,
      deleteRecord,
    }),
    [
      records,
      categories,
      loading,
      categoriesLoading,
      error,
      categoriesError,
      recentSearches,
      rememberSearch,
      loadRecords,
      retry,
      addRecord,
      updateRecord,
      deleteRecord,
    ]
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