/* ═══════════════════════════════════════════════
   EXPERIMATE — api.js
   Fetch wrappers for all backend REST endpoints.
   Base URL is always the same origin (Spring Boot serves both).
═══════════════════════════════════════════════ */

const API_BASE = '';

async function apiFetch(path, options = {}) {
  const res = await fetch(API_BASE + path, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: `HTTP ${res.status}` }));
    throw new Error(err.message || `HTTP ${res.status}`);
  }
  if (res.status === 204) return null;
  return res.json();
}

/* ───────────────────────────────────────────────
   USERS  /api/user
─────────────────────────────────────────────── */
const UserAPI = {
  getAll: ()           => apiFetch('/api/user'),
  getById: (id)        => apiFetch(`/api/user/${id}`),
  create: (dto)        => apiFetch('/api/user',        { method: 'POST', body: JSON.stringify(dto) }),
  update: (id, dto)    => apiFetch(`/api/user/${id}`,  { method: 'PATCH', body: JSON.stringify(dto) }),
  delete: (id)         => apiFetch(`/api/user/${id}`,  { method: 'DELETE' }),
};

/* ───────────────────────────────────────────────
   TOUR LISTINGS  /api/tour-listing
─────────────────────────────────────────────── */
const TourListingAPI = {
  getAll: ()           => apiFetch('/api/tour-listing'),
  getById: (id)        => apiFetch(`/api/tour-listing/${id}`),
  create: (dto)        => apiFetch('/api/tour-listing',        { method: 'POST',   body: JSON.stringify(dto) }),
  update: (id, dto)    => apiFetch(`/api/tour-listing/${id}`,  { method: 'PATCH',  body: JSON.stringify(dto) }),
  delete: (id)         => apiFetch(`/api/tour-listing/${id}`,  { method: 'DELETE' }),
};

/* ───────────────────────────────────────────────
   RESERVATIONS  /api/reservation
─────────────────────────────────────────────── */
const ReservationAPI = {
  getAll: ()           => apiFetch('/api/reservation'),
  getById: (id)        => apiFetch(`/api/reservation/${id}`),
  create: (dto)        => apiFetch('/api/reservation',        { method: 'POST',   body: JSON.stringify(dto) }),
  delete: (id)         => apiFetch(`/api/reservation/${id}`,  { method: 'DELETE' }),
};
