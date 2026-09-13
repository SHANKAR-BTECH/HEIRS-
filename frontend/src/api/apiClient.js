// Small reusable HTTP client for the HEIRS Spring Boot backend.
// Centralizes the base URL and turns backend responses into consistent
// data or meaningful errors. No third-party HTTP dependency.

export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080';

export class ApiClientError extends Error {
  constructor(message, { status = 0, path = '', fieldErrors = {}, endpoint = '' } = {}) {
    super(message);
    this.name = 'ApiClientError';
    this.status = status;
    this.path = path;
    this.fieldErrors = fieldErrors || {};
    this.endpoint = endpoint;
  }
}

function buildUrl(path, params) {
  const url = `${API_BASE_URL}${path}`;
  if (!params) return url;
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value));
    }
  });
  const query = search.toString();
  return query ? `${url}?${query}` : url;
}

function parseFieldErrors(body) {
  const fieldErrors = body?.fieldErrors;
  if (fieldErrors && typeof fieldErrors === 'object' && !Array.isArray(fieldErrors)) {
    const cleaned = {};
    Object.entries(fieldErrors).forEach(([key, value]) => {
      if (value) cleaned[key] = String(value);
    });
    return cleaned;
  }
  return {};
}

async function parseErrorResponse(response, endpoint) {
  let body = null;
  try {
    body = await response.json();
  } catch {
    body = null;
  }
  const fieldErrors = parseFieldErrors(body);
  const message =
    body?.message ||
    (response.status === 0
      ? 'Network request failed.'
      : `Request failed with status ${response.status}.`);
  return new ApiClientError(message, {
    status: response.status,
    path: body?.path || '',
    fieldErrors,
    endpoint,
  });
}

// Core request helper. Returns parsed JSON, or `null` for 204 responses.
// Throws ApiClientError for non-2xx responses and network failures.
export async function apiFetch(path, { method = 'GET', body, params } = {}) {
  const options = { method };
  const headers = {};
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(body);
  }
  options.headers = headers;

  let response;
  try {
    response = await fetch(buildUrl(path, params), options);
  } catch (error) {
    throw new ApiClientError(
      'Unable to reach the backend server. Please check that the backend is running.',
      { status: 0, endpoint: buildUrl(path, params) }
    );
  }

  if (!response.ok) {
    throw await parseErrorResponse(response, buildUrl(path, params));
  }

  if (response.status === 204) {
    return null;
  }

  const text = await response.text();
  if (!text) return null;
  return JSON.parse(text);
}