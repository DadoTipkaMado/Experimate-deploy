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
  _decode: () => {
    const token = localStorage.getItem('jwt');
    if (!token) return null;
    try { return JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))); }
    catch { return null; }
  },
  getUsername: () => { const p = Auth._decode(); return p?.sub ?? null; },
  isExpired:   () => { const p = Auth._decode(); return p ? p.exp * 1000 < Date.now() : true; },
  logout: async () => {
    sessionStorage.setItem('explicit_logout', '1');
    if (typeof unsubscribeFromPush === 'function') await unsubscribeFromPush();
    fetch('/api/auth/logout', { method: 'POST', credentials: 'include', keepalive: true }).catch(() => {});
    localStorage.removeItem('jwt');
    localStorage.removeItem('userId');
    window.location.href = '/login';
  },
};

function buildQuery(params = {}) {
  const q = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join('&');
  return q ? '?' + q : '';
}

// Shared refresh promise — prevents parallel calls from each triggering /refresh separately
let _refreshPromise = null;

async function apiFetch(path, options = {}, _isRetry = false) {
  // Proactively refresh if JWT is expired before making the call.
  // Avoids a guaranteed 401 round-trip and catches the browser-reopen case
  // where the JWT expired while the tab was closed but the refresh cookie is still valid.
  const isAuthPath = ['/api/auth/login', '/api/auth/register', '/api/auth/refresh'].some(p => path.startsWith(p));
  if (!_isRetry && !isAuthPath && Auth.getToken() && Auth.isExpired()) {
    if (!_refreshPromise) {
      _refreshPromise = fetch(API_BASE + '/api/auth/refresh', { method: 'POST', credentials: 'include' })
        .then(async (r) => {
          if (!r.ok) {
            sessionStorage.setItem('explicit_logout', '1');
            Auth.clearToken();
            localStorage.removeItem('userId');
            const onAuthPage = ['/login', '/register'].some(p => window.location.pathname.startsWith(p));
            if (!onAuthPage) window.location.href = '/login';
            throw new Error('Session expired. Please sign in.');
          }
          const { token: newToken } = await r.json();
          Auth.saveToken(newToken);
        }).finally(() => { _refreshPromise = null; });
    }
    await _refreshPromise;
  }

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
    await _refreshPromise;
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
      429: 'Too many requests — slow down and try again shortly.',
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
  getPage: (page = 0, params = {}) => apiFetch('/api/tour-listing' + buildQuery({ page, ...params })),
  getAll: (params = {})  => apiFetch('/api/tour-listing' + buildQuery({ size: 1000, ...params })).then(p => p?.content ?? []),
  getMine: (params = {}) => apiFetch('/api/tour-listing/mine' + buildQuery(params)).then(p => p?.content ?? []),
  getById: (id)        => apiFetch(`/api/tour-listing/${id}`),
  create: (dto)             => apiFetch('/api/tour-listing',                   { method: 'POST',   body: JSON.stringify(dto) }),
  createFromEvent: (dto)    => apiFetch('/api/tour-listing/from-partner-event', { method: 'POST',   body: JSON.stringify(dto) }),
  update: (id, dto)         => apiFetch(`/api/tour-listing/${id}`,              { method: 'PATCH',  body: JSON.stringify(dto) }),
  delete: (id)              => apiFetch(`/api/tour-listing/${id}`,              { method: 'DELETE' }),
};

/* ───────────────────────────────────────────────
   RESERVATIONS  /api/reservation
─────────────────────────────────────────────── */
const ReservationAPI = {
  getAll: ()           => apiFetch('/api/reservation'),
  getMine: (params = {}) => apiFetch('/api/reservation/mine' + buildQuery(params)).then(p => p?.content ?? []),
  getById: (id)        => apiFetch(`/api/reservation/${id}`),
  delete: (id)         => apiFetch(`/api/reservation/${id}`,  { method: 'DELETE' }),
  checkIn: (id)        => apiFetch(`/api/reservation/check-in/${id}`,  { method: 'PATCH' }),
  endTour: (id)        => apiFetch(`/api/reservation/end-tour/${id}`,  { method: 'PATCH' }),
  cancelTour: (id)     => apiFetch(`/api/reservation/cancel-tour/${id}`, { method: 'PATCH' }),
  getPresence: (id)    => apiFetch(`/api/reservation/${id}/presence`),
};

/* ───────────────────────────────────────────────
   BOOKING REQUESTS  /api/booking-request
─────────────────────────────────────────────── */
const BookingRequestAPI = {
  getAll: ()           => apiFetch('/api/booking-request'),
  getMine: (params = {}) => apiFetch('/api/booking-request/mine' + buildQuery(params)).then(p => p?.content ?? []),
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
   PREMIUM  /api/premium
─────────────────────────────────────────────── */
const PremiumAPI = {
  getStatus: ()                  => apiFetch('/api/premium/status'),
  purchase:  (premiumPackage)    => apiFetch('/api/premium/purchase', { method: 'POST', body: JSON.stringify({ premiumPackage }) }),
  cancel:    ()                  => apiFetch('/api/premium/cancel',   { method: 'POST' }),
};

/* ───────────────────────────────────────────────
   PARTNER  /api/partner  (B2B — issue #107)
─────────────────────────────────────────────── */
const PartnerAPI = {
  getStatus:  () => apiFetch('/api/partner/status'),
  getProfile: () => apiFetch('/api/partner/profile'),
  getStats:   () => apiFetch('/api/partner/stats'),
  apply: (dto) => apiFetch('/api/partner/apply', { method: 'POST', body: JSON.stringify(dto) }),
};

/* ───────────────────────────────────────────────
   PARTNER PINS  /api/partner-pins  (issue #130)
─────────────────────────────────────────────── */
const PartnerPinAPI = {
  getAll:    ()         => apiFetch('/api/partner-pins'),
  getMine:   ()         => apiFetch('/api/partner-pins/mine'),
  getById:   (id)       => apiFetch(`/api/partner-pins/${id}`),
  create:    (dto)      => apiFetch('/api/partner-pins',        { method: 'POST',   body: JSON.stringify(dto) }),
  update:    (id, dto)  => apiFetch(`/api/partner-pins/${id}`,  { method: 'PUT',    body: JSON.stringify(dto) }),
  delete:    (id)       => apiFetch(`/api/partner-pins/${id}`,  { method: 'DELETE' }),
  uploadLogo:(id, file) => { const f = new FormData(); f.append('file', file); return apiFetch(`/api/partner-pins/${id}/logo`, { method: 'POST', body: f }); },
  logoUrl:   (filename) => filename ? `/api/partner-pins/logo/${filename}` : null,
};

/* ───────────────────────────────────────────────
   PARTNER EVENTS  /api/partner-events  (issue #131)
─────────────────────────────────────────────── */
const PartnerEventAPI = {
  createForPin:  (pinId, dto) => apiFetch(`/api/partner-pins/${pinId}/events`, { method: 'POST', body: JSON.stringify(dto) }),
  listForPin:    (pinId)      => apiFetch(`/api/partner-pins/${pinId}/events`),
  getUpcoming:   (page = 0, size = 20) => apiFetch(`/api/partner-events/upcoming?page=${page}&size=${size}&sort=startDatetime,asc`),
  getById:       (id)         => apiFetch(`/api/partner-events/${id}`),
  update:        (id, dto)    => apiFetch(`/api/partner-events/${id}`,   { method: 'PUT',    body: JSON.stringify(dto) }),
  delete:        (id)         => apiFetch(`/api/partner-events/${id}`,   { method: 'DELETE' }),
  getMine:       (filter = 'upcoming') => apiFetch(`/api/partner/events?filter=${filter}`),
  promote:       (id, dto = {}) => apiFetch(`/api/partner-events/${id}/promote`, { method: 'POST',   body: JSON.stringify(dto) }),
  unpromote:     (id)           => apiFetch(`/api/partner-events/${id}/promote`, { method: 'DELETE' }),
};

/* ───────────────────────────────────────────────
   PROMOTED ADS  /api/promoted-ads  (issue #132)
─────────────────────────────────────────────── */
const PromotedAdAPI = {
  create:      (dto)      => apiFetch('/api/promoted-ads',           { method: 'POST',   body: JSON.stringify(dto) }),
  getMine:     ()         => apiFetch('/api/promoted-ads/mine'),
  update:      (id, dto)  => apiFetch(`/api/promoted-ads/${id}`,     { method: 'PUT',    body: JSON.stringify(dto) }),
  delete:      (id)       => apiFetch(`/api/promoted-ads/${id}`,     { method: 'DELETE' }),
  uploadImage: (id, file) => { const f = new FormData(); f.append('file', file); return apiFetch(`/api/promoted-ads/${id}/image`, { method: 'POST', body: f }); },
  imageUrl:    (filename) => filename ? `/api/promoted-ads/image/${filename}` : null,
};

/* ───────────────────────────────────────────────
   FEED  /api/feed  (issue #133)
─────────────────────────────────────────────── */
const FeedAPI = {
  getPage: (page = 0, size = 20, adFrequency = 5) =>
    apiFetch(`/api/feed?page=${page}&size=${size}&adFrequency=${adFrequency}`),
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
