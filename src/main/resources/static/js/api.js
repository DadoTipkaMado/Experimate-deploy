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
    sessionStorage.setItem('explicit_logout', '1');
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

  if (res.status === 403) {
    // 403 from the refresh endpoint = refresh token expired → logout
    // 403 from any other endpoint = authorization failure → throw normally, don't touch token
    if (path === '/api/auth/refresh') {
      sessionStorage.setItem('explicit_logout', '1');
      Auth.clearToken();
      localStorage.removeItem('userId');
      window.location.href = '/login';
      throw new Error('Session expired. Please sign in.');
    }
    // fall through to !res.ok handler below
  }

  if (res.status === 401 && !_isRetry) {
    const isAuthEndpoint = ['/api/auth/login', '/api/auth/register'].some(p => path.startsWith(p));
    if (isAuthEndpoint) {
      throw new Error('Incorrect username or password.');
    }
    if (!_refreshPromise) {
      _refreshPromise = fetch(API_BASE + '/api/auth/refresh', {
        method: 'POST',
        credentials: 'include',
      }).then(async (refreshRes) => {
        if (!refreshRes.ok) {
          sessionStorage.setItem('explicit_logout', '1');
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
  getByUsername: (username) => apiFetch(`/api/user/by-username/${username}`),
  search: (query)          => apiFetch(`/api/user/search?query=${encodeURIComponent(query)}`),
  uploadPhoto: (id, blob)  => { const f = new FormData(); f.append('file', blob, 'photo.jpg'); return apiFetch(`/api/user/${id}/profile-photo`, { method: 'POST', body: f }); },
  photoUrl: (url)          => url ? (url.startsWith('/') ? url : `/api/user/profile-photo/${url}`) : null,
  create: (dto)            => apiFetch('/api/user',        { method: 'POST', body: JSON.stringify(dto) }),
  update: (id, dto)        => apiFetch(`/api/user/${id}`,  { method: 'PATCH', body: JSON.stringify(dto) }),
  delete: (id)             => apiFetch(`/api/user/${id}`,  { method: 'DELETE' }),
};

/* ───────────────────────────────────────────────
   TOUR LISTINGS  /api/tour-listing
─────────────────────────────────────────────── */
const TourListingAPI = {
  getAll: ()           => apiFetch('/api/tour-listing'),
  getMine: ()          => apiFetch('/api/tour-listing/mine'),
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
  getMine: ()          => apiFetch('/api/reservation/mine'),   // returns { asGuest: [], asHost: [] }
  getById: (id)        => apiFetch(`/api/reservation/${id}`),
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
   SAVED LOCALS  /api/saved  (post-MVP, endpoints pending)
─────────────────────────────────────────────── */
const SavedAPI = {
  getAll: ()                   => apiFetch('/api/saved'),
  save:   (targetUserId)       => apiFetch(`/api/saved/${targetUserId}`,  { method: 'POST' }),
  unsave: (targetUserId)       => apiFetch(`/api/saved/${targetUserId}`,  { method: 'DELETE' }),
};

/* ───────────────────────────────────────────────
   ONBOARDING  /api/onboarding
─────────────────────────────────────────────── */
const OnboardingAPI = {
  getQuestions: ()       => apiFetch('/api/onboarding/questions'),
  getStatus:    ()       => apiFetch('/api/onboarding/status'),
  submit:    (answers)   => apiFetch('/api/onboarding/answers', { method: 'POST', body: JSON.stringify({ answers }) }),
  cancel:       ()       => apiFetch('/api/onboarding/cancel',  { method: 'POST' }),
  deleteData:   ()       => apiFetch('/api/onboarding/data',    { method: 'DELETE' }),
};

/* ───────────────────────────────────────────────
   MATCH  /api/match
─────────────────────────────────────────────── */
const MatchAPI = {
  findMatches:  (q)                => apiFetch('/api/match' + (q ? `?q=${encodeURIComponent(q)}` : '')),
  explainMatch: (candidateId, q)   => apiFetch(`/api/match/${candidateId}/explain` + (q ? `?q=${encodeURIComponent(q)}` : '')),
};

/* ───────────────────────────────────────────────
   RATINGS  /api/rating
─────────────────────────────────────────────── */
const RatingAPI = {
  getAll: ()            => apiFetch('/api/rating'),
  getById: (id)         => apiFetch(`/api/rating/${id}`),
  create: (dto)         => apiFetch('/api/rating',       { method: 'POST',  body: JSON.stringify(dto) }),
  update: (id, dto)     => apiFetch(`/api/rating/${id}`, { method: 'PATCH', body: JSON.stringify(dto) }),
  delete: (id)          => apiFetch(`/api/rating/${id}`, { method: 'DELETE' }),
};

/* ───────────────────────────────────────────────
   SHARED USER CACHE HELPER
   Saves initials, hue, and photo to localStorage.
   Called after login and auto-redirect so the
   topbar avatar renders instantly on next page.
─────────────────────────────────────────────── */
function _cacheUser(user, userId) {
  if (!user) return;
  const initials = ((user.firstName?.[0] ?? '') + (user.lastName?.[0] ?? '')).toUpperCase()
    || user.username?.[0]?.toUpperCase() || '?';
  const hue = (user.username ?? '').split('').reduce((a, c) => a + c.charCodeAt(0), 0) % 360;
  localStorage.setItem('user_initials', initials);
  localStorage.setItem('user_hue', String(hue));
  if (userId) {
    if (user.profilePhotoUrl) localStorage.setItem('photo_' + userId, UserAPI.photoUrl(user.profilePhotoUrl));
    else localStorage.removeItem('photo_' + userId);
  }
  return { initials, hue };
}
