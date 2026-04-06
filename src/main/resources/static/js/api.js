/* ═══════════════════════════════════════════════
   EXPERIMATE — api.js
   Fetch wrappers for all backend REST endpoints.
   Base URL is always the same origin (Spring Boot serves both).
═══════════════════════════════════════════════ */

const API_BASE = '';

/* ───────────────────────────────────────────────
   JWT TOKEN STORAGE
─────────────────────────────────────────────── */
const Auth = {
  getToken:    ()       => localStorage.getItem('jwt'),
  saveToken:   (token)  => localStorage.setItem('jwt', token),
  clearToken:  ()       => localStorage.removeItem('jwt'),
  getUserId:   ()       => { const id = localStorage.getItem('userId'); return id ? parseInt(id, 10) : null; },
  saveUserId:  (id)     => localStorage.setItem('userId', String(id)),
  getUsername: ()       => {
    const token = localStorage.getItem('jwt');
    if (!token) return null;
    try {
      return JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))).sub;
    } catch { return null; }
  },
  logout: () => {
    localStorage.removeItem('jwt');
    localStorage.removeItem('userId');
    window.location.href = '/login';
  },
};

async function apiFetch(path, options = {}, _isRetry = false) {
  const token = Auth.getToken();
  const authHeader = token ? { 'Authorization': `Bearer ${token}` } : {};

  const res = await fetch(API_BASE + path, {
    headers: { 'Content-Type': 'application/json', ...authHeader, ...options.headers },
    ...options,
  });

  if ((res.status === 401 || res.status === 403) && !_isRetry) {
    const refreshRes = await fetch(API_BASE + '/api/auth/refresh', {
      method: 'POST',
      credentials: 'include',
    });
    if (!refreshRes.ok) {
      Auth.logout();
      return;
    }
    const { token: newToken } = await refreshRes.json();
    Auth.saveToken(newToken);
    return apiFetch(path, options, true);
  }

  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: `HTTP ${res.status}` }));
    throw new Error(err.message || `HTTP ${res.status}`);
  }
  if (res.status === 204) return null;
  return res.json();
}

/* ───────────────────────────────────────────────
   AUTH  /api/auth
─────────────────────────────────────────────── */
const AuthAPI = {
  login: (username, password) => apiFetch('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  }),
};

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

/* ───────────────────────────────────────────────
   BOOKING REQUESTS  /api/booking-request
─────────────────────────────────────────────── */
const BookingRequestAPI = {
  getAll: ()           => apiFetch('/api/booking-request'),
  getById: (id)        => apiFetch(`/api/booking-request/${id}`),
  create: (dto)        => apiFetch('/api/booking-request',             { method: 'POST',   body: JSON.stringify(dto) }),
  accept: (id)         => apiFetch(`/api/booking-request/accept/${id}`, { method: 'PATCH' }),
  decline: (id)        => apiFetch(`/api/booking-request/decline/${id}`, { method: 'PATCH' }),
  delete: (id)         => apiFetch(`/api/booking-request/${id}`,       { method: 'DELETE' }),
};
