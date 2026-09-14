import { useEffect, useId, useRef, useState } from 'react';
import { FileText } from 'lucide-react';
import { LoadingState, InlineError } from './DataState';
import {
  getDocuments,
  uploadDocuments as uploadDocumentsProvider,
  replaceDocument as replaceDocumentProvider,
  deleteDocument as deleteDocumentProvider,
  getDocumentUrls,
  isDemoMode,
} from '../data/dataProvider';
import './SupportingDocuments.css';

function sizeLabel(bytes) {
  return bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export function SupportingDocuments({ recordId, editable = false, disabled = false, onBusyChange }) {
  const labelId = useId();
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [actionError, setActionError] = useState('');
  const [selected, setSelected] = useState([]);
  const [busy, setBusy] = useState('');
  const [notice, setNotice] = useState('');
  const [revision, setRevision] = useState(0);
  const [removeTarget, setRemoveTarget] = useState(null);
  const fileInput = useRef(null);
  const replacementInput = useRef(null);
  const replacementId = useRef(null);
  const running = useRef(false);
  const demoMode = isDemoMode();

  useEffect(() => {
    let active = true;
    getDocuments(recordId).then((data) => {
      if (active) { setDocuments(data); setLoadError(''); }
    }).catch((error) => {
      if (active) setLoadError(error.message);
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [recordId, revision]);

  const retry = () => { setLoading(true); setRevision((value) => value + 1); };
  const perform = async (label, action) => {
    if (running.current || disabled) return;
    running.current = true;
    setBusy(label); setActionError(''); setNotice(''); onBusyChange?.(true);
    try { await action(); }
    catch (error) { setActionError(error.message); }
    finally { running.current = false; setBusy(''); onBusyChange?.(false); }
  };
  const upload = () => perform(`Uploading ${selected.length} documents...`, async () => {
    const added = await uploadDocumentsProvider(recordId, selected);
    setDocuments((current) => [...current, ...added]);
    setSelected([]); fileInput.current.value = '';
    setNotice(`${added.length} document${added.length === 1 ? '' : 's'} uploaded.`);
  });
  const replace = (event) => {
    const file = event.target.files[0];
    event.target.value = '';
    if (!file) return;
    const id = replacementId.current;
    perform('Replacing document...', async () => {
      const updated = await replaceDocumentProvider(id, file);
      setDocuments((current) => current.map((doc) => doc.id === id ? updated : doc));
      setNotice('Document replaced.');
    });
  };
  const remove = () => perform('Removing document...', async () => {
    await deleteDocumentProvider(removeTarget.id);
    setDocuments((current) => current.filter((doc) => doc.id !== removeTarget.id));
    setRemoveTarget(null); setNotice('Document removed.');
  });

  return (
    <div className="supporting-documents" aria-busy={Boolean(busy)}>
      {loading ? <LoadingState compact label="Loading documents..." /> : loadError ? (
        <InlineError message={loadError} onRetry={retry} />
      ) : documents.length === 0 ? (
        <p className="detail-paragraph muted">No supporting documents are currently attached to this record.</p>
      ) : (
        <ul className="document-list">
          {documents.map((doc) => {
            const { preview, download } = getDocumentUrls(doc);
            const unavailable = demoMode && !preview && !download;
            return (
              <li className="document-item" key={doc.id}>
                <div className="document-icon"><FileText className="meta-icon" aria-hidden="true" /></div>
                <div className="document-info">
                  <p className="document-name">{doc.originalFileName}</p>
                  <p className="document-meta">PDF &middot; {sizeLabel(doc.fileSize)}</p>
                  <p className="document-meta">Uploaded {new Date(`${doc.uploadedAt}Z`).toLocaleString()}</p>
                  {doc.updatedAt !== doc.uploadedAt && <p className="document-meta">Updated {new Date(`${doc.updatedAt}Z`).toLocaleString()}</p>}
                  {unavailable && <p className="document-meta">Preview is unavailable in frontend demo mode.</p>}
                </div>
                <div className="document-actions">
                  {preview ? (
                    <a className="btn btn-sm btn-secondary" href={preview} target="_blank" rel="noopener noreferrer" aria-label={`Preview ${doc.originalFileName}`}>Preview</a>
                  ) : (
                    <span className="btn btn-sm btn-secondary demo-unavailable" aria-disabled="true" title="Preview is unavailable in frontend demo mode.">Preview</span>
                  )}
                  {download ? (
                    <a className="btn btn-sm btn-secondary" href={download} aria-label={`Download ${doc.originalFileName}`}>Download</a>
                  ) : (
                    <span className="btn btn-sm btn-secondary demo-unavailable" aria-disabled="true" title="Preview is unavailable in frontend demo mode.">Download</span>
                  )}
                  {editable && <>
                    <button type="button" className="btn btn-sm btn-secondary" disabled={disabled || Boolean(busy)} aria-label={`Replace ${doc.originalFileName}`} onClick={() => { replacementId.current = doc.id; replacementInput.current.click(); }}>Replace</button>
                    <button type="button" className="btn btn-sm btn-secondary" disabled={disabled || Boolean(busy)} aria-label={`Remove ${doc.originalFileName}`} onClick={() => setRemoveTarget(doc)}>Remove</button>
                  </>}
                </div>
              </li>
            );
          })}
        </ul>
      )}
      {editable && <>
        {removeTarget && <div className="document-confirm" role="group" aria-label="Confirm document removal">
          <p className="detail-paragraph">Remove {removeTarget.originalFileName} from this record?</p>
          <div className="document-actions">
            <button type="button" className="btn btn-sm btn-secondary" disabled={disabled || Boolean(busy)} onClick={() => setRemoveTarget(null)}>Keep document</button>
            <button type="button" className="btn btn-sm btn-danger" disabled={disabled || Boolean(busy)} onClick={remove}>Confirm remove</button>
          </div>
        </div>}
        <div className="document-upload form-group">
          <label className="form-label" htmlFor={labelId}>Add Documents</label>
          <p id={`${labelId}-hint`} className="document-meta">{demoMode ? 'Demo mode: chosen PDFs are previewable during this session, but only document metadata persists after refresh. No files are uploaded to a server.' : 'Select one or more PDFs. Default limit: 20 MiB each, 100 MiB per request. Documents are saved separately from record metadata.'}</p>
          <input id={labelId} ref={fileInput} className="form-control" type="file" accept=".pdf,application/pdf" multiple disabled={disabled || Boolean(busy) || loading || Boolean(loadError)} aria-describedby={`${labelId}-hint`} onChange={(event) => { setSelected(Array.from(event.target.files)); setActionError(''); }} />
          <input ref={replacementInput} type="file" accept=".pdf,application/pdf" hidden aria-label="Replacement PDF" onChange={replace} />
          {selected.length > 0 && <>
            <p className="document-meta">{selected.length} selected document{selected.length === 1 ? '' : 's'}</p>
            <ul className="selected-documents">{selected.map((file, index) => <li key={`${file.name}-${index}`}>{file.name} &middot; {sizeLabel(file.size)}</li>)}</ul>
            <button type="button" className="btn btn-sm btn-primary" disabled={disabled || Boolean(busy)} onClick={upload}>Upload {selected.length} Document{selected.length === 1 ? '' : 's'}</button>
          </>}
        </div>
      </>}
      {actionError && <InlineError message={actionError} />}
      {busy && <LoadingState compact label={busy} />}
      <p className="document-meta" role="status">{notice}</p>
    </div>
  );
}