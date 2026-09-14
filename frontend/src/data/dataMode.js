// Single source of truth for the frontend data mode.
//
// VITE_DATA_MODE controls which provider the whole app uses:
//   api  -> real Spring Boot backend (React -> Spring Boot -> MySQL)
//   demo -> fully self-contained browser demo (bundled + localStorage data)
//
// Anything that is not exactly "demo" safely defaults to "api" so local
// development keeps working without extra configuration.

const RAW_MODE = String(import.meta.env.VITE_DATA_MODE || '').trim().toLowerCase();

export const DATA_MODE = RAW_MODE === 'demo' ? 'demo' : 'api';

export function isDemoMode() {
  return DATA_MODE === 'demo';
}

export function isApiMode() {
  return DATA_MODE === 'api';
}