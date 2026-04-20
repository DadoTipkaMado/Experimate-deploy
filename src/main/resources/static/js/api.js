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

// Shared refresh promise — prevents parallel calls from each triggering /refresh separately
let _refreshPromise = null;

async function apiFetch(path, options = {}, _isRetry = false) {
  const token = Auth.getToken();
  const authHeader = token ? { 'Authorization': `Bearer ${token}` } : {};

  const contentTypeHeader = options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' };
  const res = await fetch(API_BASE + path, {
    headers: { ...contentTypeHeader, ...authHeader, ...options.headers },
    ...options,
  });

  if ((res.status === 401 || res.status === 403) && !_isRetry) {
    if (!_refreshPromise) {
      _refreshPromise = fetch(API_BASE + '/api/auth/refresh', {
        method: 'POST',
        credentials: 'include',
      }).then(async (refreshRes) => {
        if (!refreshRes.ok) {
          Auth.clearToken();
          localStorage.removeItem('userId');
          const onAuthPage = ['/login', '/register'].some(p => window.location.pathname.startsWith(p));
          if (!onAuthPage) window.location.href = '/login';
          throw new Error('Session expired. Please sign in.');
        }
        const { token: newToken } = await refreshRes.json();
        Auth.saveToken(newToken);
      }).finally(() => { _refreshPromise = null; });
    }
    try {
      await _refreshPromise;
    } catch (e) {
      throw e;
    }
    return apiFetch(path, options, true);
  }

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    const friendly = {
      400: 'Check your input and try again.',
      401: 'Not signed in.',
      403: 'You don\'t have permission to do this.',
      404: 'Not found.',
      409: 'Already exists.',
      500: 'Server error — try again shortly.',
    };
    throw new Error(err.message || friendly[res.status] || 'Something went wrong — check your connection.');
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
  getAll: ()               => apiFetch('/api/user'),
  getById: (id)            => apiFetch(`/api/user/${id}`),
  // TODO: swap profile.html to use this once David adds GET /api/user/by-username/{username} (Issue #1)
  getByUsername: (username) => apiFetch(`/api/user/by-username/${username}`),
  search: (query)          => apiFetch(`/api/user/search?query=${encodeURIComponent(query)}`),
  uploadPhoto: (id, blob)  => { const f = new FormData(); f.append('file', blob, 'photo.jpg'); return apiFetch(`/api/user/${id}/profile-photo`, { method: 'POST', body: f }); },
  create: (dto)            => apiFetch('/api/user',        { method: 'POST', body: JSON.stringify(dto) }),
  update: (id, dto)        => apiFetch(`/api/user/${id}`,  { method: 'PATCH', body: JSON.stringify(dto) }),
  delete: (id)             => apiFetch(`/api/user/${id}`,  { method: 'DELETE' }),
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
  checkIn: (id)        => apiFetch(`/api/reservation/check-in/${id}`,  { method: 'PATCH' }),
  endTour: (id)        => apiFetch(`/api/reservation/end-tour/${id}`,  { method: 'PATCH' }),
  cancelTour: (id)     => apiFetch(`/api/reservation/cancel-tour/${id}`, { method: 'PATCH' }),
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

/* ───────────────────────────────────────────────
   SAVED LOCALS  /api/saved
   TODO: swap explore.html localStorage logic to use this once David adds the endpoints (Issue #4)
─────────────────────────────────────────────── */
const SavedAPI = {
  getAll: ()                   => apiFetch('/api/saved'),
  save:   (targetUserId)       => apiFetch(`/api/saved/${targetUserId}`,  { method: 'POST' }),
  unsave: (targetUserId)       => apiFetch(`/api/saved/${targetUserId}`,  { method: 'DELETE' }),
};

/* ───────────────────────────────────────────────
   RATINGS  /api/rating
─────────────────────────────────────────────── */
const RatingAPI = {
  getAll: ()            => apiFetch('/api/rating'),
  getById: (id)         => apiFetch(`/api/rating/${id}`),
  create: (dto)         => apiFetch('/api/rating',       { method: 'POST',  body: JSON.stringify(dto) }),
  update: (id, dto)     => apiFetch(`/api/rating/${id}`, { method: 'PATCH', body: JSON.stringify(dto) }),
  delete: (id, raterId) => apiFetch(`/api/rating/${id}?raterId=${raterId}`, { method: 'DELETE' }),
};
