import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Eye, Pencil, Trash2, X, AlertTriangle } from 'lucide-react';
import { useRecords } from '../context/RecordsContext';
import { StatusBadge } from '../components/StatusBadge';
import { SupportingDocuments } from '../components/SupportingDocuments';
import { isDemoMode } from '../data/dataProvider';
import '../App.css';

const EMPTY_FORM = {
  title: '',
  description: '',
  category: 'Policy',
  department: 'Higher Education Department',
  referenceNumber: '',
  publicationYear: new Date().getFullYear(),
  publishedDate: '',
  status: 'Active',
  source: '',
  keywords: '',
};

function buildPayload(form) {
  return {
    title: form.title.trim(),
    description: form.description.trim(),
    category: form.category,
    department: form.department,
    referenceNumber: form.referenceNumber.trim(),
    publicationYear: Number(form.publicationYear),
    publishedDate: form.publishedDate?.trim() || null,
    status: form.status,
    source: form.source?.trim() || null,
    keywords: (form.keywords || '')
      .split(',')
      .map((keyword) => keyword.trim())
      .filter(Boolean)
      .join(', '),
  };
}

function extractErrorMessage(error) {
  if (error?.fieldErrors && Object.keys(error.fieldErrors).length) {
    return Object.entries(error.fieldErrors)
      .map(([field, message]) => `${field}: ${message}`)
      .join('. ');
  }
  return error?.message || 'The request could not be completed.';
}

export default function AdminPage() {
  const { records, addRecord, updateRecord, deleteRecord, resetDemoData } = useRecords();
  const navigate = useNavigate();
  const demoMode = isDemoMode();

  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [documentsBusy, setDocumentsBusy] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [toast, setToast] = useState('');
  const modalFocusRef = useRef(null);
  const lastActiveRef = useRef(null);

  const showToast = (message) => {
    setToast(message);
    window.setTimeout(() => setToast(''), 2600);
  };

  const openAdd = () => {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFormError('');
    setModalOpen(true);
  };

  const openEdit = (record) => {
    setEditingId(record.id);
    setForm({
      title: record.title,
      description: record.description,
      category: record.category,
      department: record.department,
      referenceNumber: record.referenceNumber,
      publicationYear: record.publicationYear,
      publishedDate: record.publishedDate || '',
      status: record.status,
      source: record.source || '',
      keywords: (record.keywords || []).join(', '),
    });
    setFormError('');
    setModalOpen(true);
  };

  const handleField = (key) => (e) => {
    const value =
      key === 'publicationYear' ? Number(e.target.value) : e.target.value;
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  const validate = () => {
    if (!form.title.trim()) return 'Title is required.';
    if (!form.description.trim()) return 'Description is required.';
    if (!form.referenceNumber.trim()) return 'Reference number is required.';
    if (!form.department.trim()) return 'Department is required.';
    if (
      !form.publicationYear ||
      Number(form.publicationYear) < 2000 ||
      Number(form.publicationYear) > 2030
    ) {
      return 'Publication year must be between 2000 and 2030.';
    }
    return '';
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (saving || documentsBusy) return;
    const clientError = validate();
    if (clientError) {
      setFormError(clientError);
      return;
    }
    const payload = buildPayload(form);
    setSaving(true);
    setFormError('');
    try {
      if (editingId) {
        await updateRecord(editingId, payload);
        showToast('Record updated.');
        setModalOpen(false);
      } else {
        const created = await addRecord(payload);
        setEditingId(created.id);
        showToast('Record added. You can now attach documents.');
      }
    } catch (saveError) {
      setFormError(extractErrorMessage(saveError));
    } finally {
      setSaving(false);
    }
  };

  const confirmDelete = async () => {
    if (!deleteTarget || deleting) return;
    setDeleting(true);
    try {
      await deleteRecord(deleteTarget.id);
      showToast('Record deleted.');
      setDeleteTarget(null);
    } catch (deleteError) {
      showToast(`Delete failed: ${extractErrorMessage(deleteError)}`);
      setDeleteTarget(null);
    } finally {
      setDeleting(false);
    }
  };

  const sortedRecords = useMemo(
    () =>
      [...records].sort(
        (a, b) => b.publicationYear - a.publicationYear || a.title.localeCompare(b.title)
      ),
    [records]
  );

  const handleResetDemo = async () => {
    const confirmed = window.confirm(
      'Reset all demo data back to the bundled defaults? Changes made in this browser will be discarded.'
    );
    if (!confirmed) return;
    try {
      await resetDemoData();
      showToast('Demo data reset to bundled defaults.');
    } catch (resetError) {
      showToast(`Reset failed: ${extractErrorMessage(resetError)}`);
    }
  };

  useEffect(() => {
    const onKey = (e) => {
      if (e.key === 'Tab' && modalFocusRef.current) {
        const items = Array.from(modalFocusRef.current.querySelectorAll('button, a[href], input, select, textarea, [tabindex="0"]')).filter((item) => !item.disabled && !item.hidden);
        const first = items[0];
        const last = items[items.length - 1];
        if (e.shiftKey && document.activeElement === first) {
          e.preventDefault();
          last?.focus();
        } else if (!e.shiftKey && document.activeElement === last) {
          e.preventDefault();
          first?.focus();
        }
      }
      if (e.key === 'Escape' && !documentsBusy && !saving) {
        setModalOpen(false);
        setDeleteTarget(null);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [documentsBusy, saving]);

  useEffect(() => {
    if (modalOpen || deleteTarget) {
      lastActiveRef.current = document.activeElement;
      const timer = window.setTimeout(() => {
        modalFocusRef.current
          ?.querySelector('button, input, select, textarea, [tabindex]')
          ?.focus();
      }, 0);
      return () => {
        window.clearTimeout(timer);
        lastActiveRef.current?.focus?.();
        lastActiveRef.current = null;
      };
    }
  }, [modalOpen, deleteTarget]);

  return (
    <div className="page-stack">
      <div className="content-header admin-header">
        <div>
          <h1 className="content-title">Manage Records</h1>
          <p className="content-subtitle">
            Add, review and maintain the records in the information repository.
          </p>
        </div>
        <button type="button" className="btn btn-primary" onClick={openAdd}>
          <Plus className="btn-icon" aria-hidden="true" />
          Add Record
        </button>
      </div>

      <div className={`admin-note ${demoMode ? 'admin-note-row' : ''}`} role="note">
        {demoMode ? (
          <>
            Demo mode: changes are stored in this browser and persist after reload. They are not shared with other users.
            <button
              type="button"
              className="btn btn-sm btn-secondary"
              onClick={handleResetDemo}
              aria-label="Reset demo data to bundled defaults"
            >
              Reset Demo Data
            </button>
          </>
        ) : (
          'Changes are saved to the repository database and persist after reload.'
        )}
      </div>

      <div className="card table-card">
        {/* eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex -- Focus enables native keyboard scrolling of this named table region. */}
        <div className="table-scroll" role="region" aria-label="Manage records table" tabIndex={0}>
          <table className="table admin-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Category</th>
                <th>Department</th>
                <th>Year</th>
                <th>Status</th>
                <th className="text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {sortedRecords.map((record) => (
                <tr key={record.id}>
                  <td className="admin-title-cell">
                    <span className="admin-title">{record.title}</span>
                    <span className="admin-ref">{record.referenceNumber}</span>
                  </td>
                  <td>
                    <span className={`category-pill ${record.category.toLowerCase()}`}>
                      {record.category}
                    </span>
                  </td>
                  <td>{record.department}</td>
                  <td>{record.publicationYear}</td>
                  <td>
                    <StatusBadge status={record.status} />
                  </td>
                  <td className="text-right">
                    <div className="action-group">
                      <button
                        type="button"
                        className="btn-icon-action"
                        title="View"
                        aria-label={`View ${record.title}`}
                        onClick={() => navigate(`/records/${record.id}`)}
                      >
                        <Eye className="action-icon" aria-hidden="true" />
                      </button>
                      <button
                        type="button"
                        className="btn-icon-action"
                        title="Edit"
                        aria-label={`Edit ${record.title}`}
                        onClick={() => openEdit(record)}
                      >
                        <Pencil className="action-icon" aria-hidden="true" />
                      </button>
                      <button
                        type="button"
                        className="btn-icon-action danger"
                        title="Delete"
                        aria-label={`Delete ${record.title}`}
                        onClick={() => setDeleteTarget(record)}
                      >
                        <Trash2 className="action-icon" aria-hidden="true" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {modalOpen && (
        <div
          ref={modalFocusRef}
          className="modal-overlay active"
          role="dialog"
          aria-modal="true"
          aria-labelledby="record-modal-title"
        >
          <div className="modal">
            <div className="modal-header">
              <h3 id="record-modal-title" className="modal-title">
                {editingId ? 'Edit Record' : 'Add Record'}
              </h3>
              <button
                type="button"
                className="modal-close"
                onClick={() => setModalOpen(false)}
                aria-label="Close"
                disabled={saving || documentsBusy}
              >
                <X aria-hidden="true" />
              </button>
            </div>

            <form onSubmit={handleSave}>
              <div className="form-row form-row-full">
                <div className="form-group">
                  <label className="form-label" htmlFor="field-title">
                    Title <span className="required">*</span>
                  </label>
                  <input
                    id="field-title"
                    className="form-control"
                    value={form.title}
                    onChange={handleField('title')}
                    placeholder="Title of the record"
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="field-description">
                  Description <span className="required">*</span>
                </label>
                <textarea
                  id="field-description"
                  className="form-control"
                  value={form.description}
                  onChange={handleField('description')}
                  placeholder="Short description of the record"
                />
              </div>

              <div className="form-row strip">
                <div className="form-group">
                  <label className="form-label" htmlFor="field-category">Category</label>
                  <select
                    id="field-category"
                    className="form-control"
                    value={form.category}
                    onChange={handleField('category')}
                  >
                    {['Regulation', 'Policy', 'Project', 'Rules', 'Scheme'].map((option) => (
                      <option key={option} value={option}>
                        {option}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label" htmlFor="field-status">Status</label>
                  <select
                    id="field-status"
                    className="form-control"
                    value={form.status}
                    onChange={handleField('status')}
                  >
                    {['Active', 'Draft', 'Archived', 'Completed'].map((option) => (
                      <option key={option} value={option}>
                        {option}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="field-department">Department</label>
                <select
                  id="field-department"
                  className="form-control"
                  value={form.department}
                  onChange={handleField('department')}
                >
                  {[
                    'Higher Education Department',
                    'Directorate of Collegiate Education',
                    'Technical Education',
                    'University Administration',
                    'Student Welfare',
                  ].map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-row strip">
                <div className="form-group">
                  <label className="form-label" htmlFor="field-reference">
                    Reference Number <span className="required">*</span>
                  </label>
                  <input
                    id="field-reference"
                    className="form-control"
                    value={form.referenceNumber}
                    onChange={handleField('referenceNumber')}
                    placeholder="HEIRS/POL/2026/014"
                  />
                </div>
                <div className="form-group">
                  <label className="form-label" htmlFor="field-year">Publication Year</label>
                  <input
                    id="field-year"
                    type="number"
                    min="2000"
                    max="2030"
                    className="form-control"
                    value={form.publicationYear}
                    onChange={handleField('publicationYear')}
                  />
                </div>
              </div>

              <div className="form-row strip">
                <div className="form-group">
                  <label className="form-label" htmlFor="field-published">Published Date</label>
                  <input
                    id="field-published"
                    className="form-control"
                    value={form.publishedDate}
                    onChange={handleField('publishedDate')}
                    placeholder="e.g. 12 March 2026"
                  />
                </div>
                <div className="form-group">
                  <label className="form-label" htmlFor="field-source">Source</label>
                  <input
                    id="field-source"
                    className="form-control"
                    value={form.source}
                    onChange={handleField('source')}
                    placeholder="Issuing department or authority"
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="field-keywords">
                  Keywords <span className="hint">(comma separated)</span>
                </label>
                <input
                  id="field-keywords"
                  className="form-control"
                  value={form.keywords}
                  onChange={handleField('keywords')}
                  placeholder="Scholarship, Students, Eligibility"
                />
              </div>

              <section aria-labelledby="edit-documents-heading">
                <h4 id="edit-documents-heading" className="detail-section-title">Supporting Documents</h4>
                {editingId ? <SupportingDocuments key={editingId} recordId={editingId} editable disabled={saving} onBusyChange={setDocumentsBusy} /> : (
                  <p className="document-meta">Create this record first. You can then attach one or more PDF documents here.</p>
                )}
              </section>

              {formError && (
                <p className="form-error" role="alert">
                  {formError}
                </p>
              )}

              <div className="modal-actions">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setModalOpen(false)}
                  disabled={saving || documentsBusy}
                >
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={saving || documentsBusy}>
                  {saving
                    ? 'Saving...'
                    : editingId
                    ? 'Save Changes'
                    : 'Add Record'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {deleteTarget && (
        <div
          ref={modalFocusRef}
          className="modal-overlay active"
          role="dialog"
          aria-modal="true"
          aria-labelledby="confirm-title"
        >
          <div className="modal confirm-modal">
            <div className="confirm-icon">
              <AlertTriangle aria-hidden="true" />
            </div>
            <h3 id="confirm-title" className="modal-title">
              Delete record?
            </h3>
            <p className="confirm-text">
              &ldquo;{deleteTarget.title}&rdquo; will be permanently removed
              from the repository, including all supporting documents. This action cannot be undone.
            </p>
            <div className="modal-actions">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setDeleteTarget(null)}
                disabled={deleting}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-danger"
                onClick={confirmDelete}
                disabled={deleting}
              >
                {deleting ? 'Deleting...' : 'Delete Record'}
              </button>
            </div>
          </div>
        </div>
      )}

      {toast && (
        <div className="toast" role="status">
          {toast}
        </div>
      )}
    </div>
  );
}
