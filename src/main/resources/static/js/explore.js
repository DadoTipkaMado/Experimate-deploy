/* ═══════════════════════════════════════════════
   EXPERIMATE — explore.js
   Listing feed + AI match panel.
═══════════════════════════════════════════════ */

let _allListings   = [];
let _myRequests    = {};   // listingId → { status, id }
let _userCache     = {};
let _availableOnly = false;
let _searchQuery   = '';
let _currentPage   = 0;
let _isLastPage    = false;
let _isLoadingPage = false;

/* ───────────────────────────────────────────────
   INIT
─────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
  initSearch();

  const myRequestsPromise = Auth.getToken()
    ? Promise.all([
        BookingRequestAPI.getMine({ flowDirection: 'outgoing', status: 'PENDING'   }).catch(() => []),
        BookingRequestAPI.getMine({ flowDirection: 'outgoing', status: 'ACCEPTED'  }).catch(() => []),
        BookingRequestAPI.getMine({ flowDirection: 'outgoing', status: 'DECLINED'  }).catch(() => []),
      ]).then(([p, a, d]) => [...p, ...a, ...d])
    : Promise.resolve([]);

  Promise.all([
    TourListingAPI.getPage(0),
    myRequestsPromise,
    UserAPI.getAll().catch(() => []),
  ]).then(([listingPage, myRequests, allUsers]) => {
    (allUsers || []).forEach(u => { if (u.username) _userCache[u.username] = u; });

    _myRequests = {};
    (myRequests || [])
      .filter(r => r.tourListing?.id)
      .forEach(r => { _myRequests[r.tourListing.id] = { status: r.status, id: r.id }; });

    const now = new Date();
    _allListings  = (listingPage.content || []).filter(l => new Date(l.meetingDate) > now);
    _currentPage  = 0;
    _isLastPage   = listingPage.last ?? true;

    applyAndRender();
  }).catch(() => renderFeed([]));

  window.addEventListener('scroll', _onFeedScroll, { passive: true });
});

/* ───────────────────────────────────────────────
   INFINITE SCROLL
─────────────────────────────────────────────── */
function _onFeedScroll() {
  if (_isLoadingPage || _isLastPage) return;
  if ((window.innerHeight + window.scrollY) < (document.body.scrollHeight - 300)) return;
  _isLoadingPage = true;
  _showFeedLoader();
  TourListingAPI.getPage(_currentPage + 1)
    .then(page => {
      _currentPage++;
      _isLastPage = page.last ?? true;
      const now   = new Date();
      const fresh = (page.content || []).filter(l => new Date(l.meetingDate) > now);
      _allListings = [..._allListings, ...fresh];
      applyAndRender();
    })
    .catch(() => {})
    .finally(() => {
      _isLoadingPage = false;
      _hideFeedLoader();
    });
}

function _showFeedLoader() {
  const feed = document.getElementById('listing-feed');
  if (!feed || document.getElementById('feed-loader')) return;
  const el = document.createElement('div');
  el.id = 'feed-loader';
  el.style.cssText = 'display:flex;justify-content:center;padding:20px 0;';
  el.innerHTML = '<div style="width:22px;height:22px;border:2px solid var(--border-2);border-top-color:var(--accent);border-radius:50%;animation:feedSpin 0.7s linear infinite;"></div>';
  feed.appendChild(el);
}

function _hideFeedLoader() {
  document.getElementById('feed-loader')?.remove();
}

/* ───────────────────────────────────────────────
   FILTERS + SORT
─────────────────────────────────────────────── */
function toggleAvailable(pill) {
  _availableOnly = !_availableOnly;
  pill.classList.toggle('pill--active', _availableOnly);
  applyAndRender();
}

function applyAndRender() {
  const now = new Date();
  let items = _allListings.filter(l => new Date(l.meetingDate) > now);

  if (_searchQuery) {
    const q = _searchQuery.toLowerCase();
    items = items.filter(l => {
      const city = (l.city ?? '').toLowerCase();
      const host = [l.host?.firstName, l.host?.lastName, l.host?.username].filter(Boolean).join(' ').toLowerCase();
      return city.includes(q) || host.includes(q);
    });
  }

  if (_availableOnly) items = items.filter(l => !l.reserved);

  items.sort((a, b) => new Date(a.meetingDate) - new Date(b.meetingDate));

  renderFeed(items);
}

/* ───────────────────────────────────────────────
   RENDER
─────────────────────────────────────────────── */
function renderFeed(listings) {
  const feed = document.getElementById('listing-feed');
  if (!feed) return;

  if (!listings.length) {
    feed.innerHTML = `
      <div class="feed-empty">
        <div class="feed-empty__icon">${_searchQuery ? '🔍' : '🗺️'}</div>
        <div class="feed-empty__title">${_searchQuery ? `No results for "${escapeHtml(_searchQuery)}"` : 'No meets yet'}</div>
        <div class="feed-empty__sub">${_searchQuery ? 'Try a different city or host name.' : 'Be the first to host a local experience.'}</div>
        ${!_searchQuery ? `<a href="/listings/new" class="btn btn--primary" style="margin-top:12px;height:40px;padding:0 22px;font-size:12px;">+ Create a listing</a>` : ''}
      </div>`;
    return;
  }

  const currentUserId = Auth.getUserId();
  const currentUsername = Auth.getUsername();

  feed.innerHTML = listings.map((l, i) => {
    const myReq     = _myRequests[l.id];
    const reqStatus = myReq?.status;
    const isOwn     = currentUsername && l.host?.username === currentUsername;

    const maxGuests   = l.maxGuests ?? 1;
    const guestCount  = l.currentGuestCount ?? (l.reserved ? 1 : 0);
    const spotsLeft   = Math.max(0, maxGuests - guestCount);
    const isFull      = spotsLeft === 0;
    const isGroup     = maxGuests > 1;

    const dotColor = isFull             ? 'var(--text-3)'
      : reqStatus === 'PENDING'         ? '#ff9944'
      : reqStatus === 'ACCEPTED'        ? 'var(--accent)'
      : reqStatus === 'DECLINED'        ? 'rgba(255,80,80,0.7)'
      : 'var(--accent)';
    const dotGlow   = (!isFull && !reqStatus) ? 'box-shadow:0 0 5px var(--accent);' : '';
    const statusLabel = isFull          ? (isGroup ? 'Full' : 'Joined')
      : reqStatus === 'PENDING'         ? 'Pending'
      : reqStatus === 'ACCEPTED'        ? 'Accepted'
      : reqStatus === 'DECLINED'        ? 'Declined'
      : isGroup ? `${spotsLeft} spot${spotsLeft !== 1 ? 's' : ''} left` : 'Available';

    const cardClass = l.reserved       ? ' feed-card--reserved'
      : reqStatus === 'PENDING'         ? ' feed-card--pending'
      : reqStatus === 'DECLINED'        ? ''
      : ' feed-card--available';

    const joinBtn = isOwn ? `<span style="font-size:10px;color:var(--text-3);letter-spacing:0.06em;">Your listing</span>`
      : isFull
        ? `<button class="btn" style="height:34px;font-size:10px;border-color:var(--border-2);color:var(--text-3);" disabled>${isGroup ? 'Full' : 'Joined'}</button>`
      : reqStatus === 'PENDING'
        ? `<button class="btn" style="height:34px;font-size:10px;border-color:#ff9944;color:#ff9944;background:rgba(255,153,68,0.08);" disabled>Pending</button>`
      : reqStatus === 'ACCEPTED'
        ? `<button class="btn" style="height:34px;font-size:10px;border-color:var(--accent-border);color:var(--accent);background:var(--accent-dim);" disabled>Accepted ✓</button>`
      : currentUserId
        ? `<button class="btn btn--primary" style="height:34px;font-size:10px;" data-listing-id="${l.id}" onclick="joinListing(this)">Join</button>`
        : `<a href="/login" class="btn btn--ghost" style="height:34px;font-size:10px;">Sign in</a>`;

    const hostHandle = l.host?.username ?? '';
    const hostName   = l.host ? l.host.firstName + ' ' + l.host.lastName : '?';
    const u          = _userCache[hostHandle];
    const hue        = hostHandle.split('').reduce((a, c) => a + c.charCodeAt(0), 0) % 360;
    const photoUrl   = u?.profilePhotoUrl ? UserAPI.photoUrl(u.profilePhotoUrl) : null;
    const initials   = ((u?.firstName?.[0] ?? '') + (u?.lastName?.[0] ?? '')).toUpperCase() || hostHandle[0]?.toUpperCase() || '?';

    const heroBg = photoUrl
      ? `style="background-image:url('${photoUrl}')"`
      : `style="background:hsl(${hue},30%,14%);"`;
    const avatarInner = photoUrl
      ? `<img src="${photoUrl}" alt="">`
      : `<div style="width:100%;height:100%;display:flex;align-items:center;justify-content:center;background:hsl(${hue},35%,22%);font-family:var(--font-display);font-weight:800;font-size:18px;color:hsl(${hue},60%,72%);">${initials}</div>`;

    const locationUnlocked = isOwn || reqStatus === 'ACCEPTED';
    const cityLabel = locationUnlocked ? escapeHtml(l.city) : `Near ${escapeHtml(l.city)}`;
    const mapBtn = l.lat != null && locationUnlocked
      ? `<button class="btn btn--ghost" style="height:34px;font-size:10px;" data-lat="${l.lat}" data-lng="${l.lng}" onclick="goToMap(this)">📍 Map</button>`
      : '';

    return `
      <div class="feed-card anim-fade-up${cardClass}" style="animation-delay:${i * 0.03}s;">
        <div class="feed-card__hero">
          <div class="feed-card__hero-bg" ${heroBg}></div>
          <div class="feed-card__hero-avatar">${avatarInner}</div>
          <div class="feed-card__hero-info">
            <div class="feed-card__city">${cityLabel}</div>
            <a href="/profile/${hostHandle}" class="feed-card__host-name" onclick="event.stopPropagation()">${escapeHtml(hostName)}</a>
          </div>
          <div class="feed-card__hero-date">
            <div class="feed-card__date">${fmtDate(l.meetingDate)}</div>
            <div class="feed-card__time">${fmtTime(l.meetingDate)}</div>
          </div>
        </div>
        <div class="feed-card__body">
          <div class="feed-card__desc">${(function(){const MAX=120,d=l.tourDescription??'';return escapeHtml(d.slice(0,MAX))+(d.length>MAX?`<span style="color:var(--accent);cursor:pointer;font-weight:600;" onclick="openListingDetailFromExplore(${l.id})"> …more</span>`:'');})()}</div>
          <div class="feed-card__foot">
            <div class="feed-card__status">
              <div class="feed-card__dot" style="background:${dotColor};${dotGlow}"></div>
              <div class="feed-card__label" style="color:${dotColor};">${statusLabel}</div>
            </div>
            <div style="display:flex;gap:6px;align-items:center;">
              <button class="btn btn--ghost" style="height:34px;font-size:10px;padding:0 10px;" onclick="openListingDetailFromExplore(${l.id})">Details</button>
              ${mapBtn}
              ${joinBtn}
            </div>
          </div>
        </div>
      </div>`;
  }).join('');
}

function goToMap(btn) {
  sessionStorage.setItem('mapFlyTo', JSON.stringify({ lat: parseFloat(btn.dataset.lat), lng: parseFloat(btn.dataset.lng), zoom: 16 }));
  window.location.href = '/map';
}

function openListingDetailFromExplore(listingId) {
  const listing = _allListings.find(l => l.id === listingId);
  if (!listing) return;
  const myReq    = _myRequests[listingId];
  const isOwn    = !!(Auth.getUsername() && listing.host?.username === Auth.getUsername());
  openListingDetail(listing, {
    isOwn,
    reserved:   !!listing.reserved,
    reqStatus:  myReq?.status ?? null,
    onJoinSuccess: (id) => {
      _myRequests[id] = { status: 'PENDING' };
      applyAndRender();
    },
  });
}

function joinListing(btn) {
  const listingId = parseInt(btn.dataset.listingId, 10);
  if (!Auth.getToken()) { window.location.href = '/login'; return; }
  if (_myRequests[listingId]) { applyAndRender(); return; }
  btn.disabled = true;
  btn.textContent = '...';
  BookingRequestAPI.create({ listingId })
    .then(res => {
      _myRequests[listingId] = { status: 'PENDING', id: res?.id };
      showToast('Request sent!', 'success');
      applyAndRender();
    })
    .catch(err => {
      btn.disabled = false;
      btn.textContent = 'Join';
      showToast(friendlyBookingError(err), 'error');
    });
}

/* ───────────────────────────────────────────────
   SEARCH
─────────────────────────────────────────────── */
function initSearch() {
  const input = document.getElementById('explore-search-input');
  if (!input) return;

  input.addEventListener('input', (e) => {
    _searchQuery = e.target.value.trim();
    if (_aiActive && !_searchQuery) closeMatchPanel();
    applyAndRender();
  });

  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && _aiActive && _searchQuery) {
      showMatchPanelLoading();
      MatchAPI.findMatches(_searchQuery)
        .then(matches => openMatchPanel(matches, _searchQuery))
        .catch(() => openMatchPanel([], _searchQuery));
    }
  });

  const urlQ = new URLSearchParams(window.location.search).get('q');
  if (urlQ) { input.value = urlQ; _searchQuery = urlQ; }
}

/* ───────────────────────────────────────────────
   AI MATCH PANEL
─────────────────────────────────────────────── */
let _matchQuery = '';
let _aiActive   = false;

function toggleAiSearch(pill) {
  _aiActive = !_aiActive;
  pill.classList.toggle('pill--active', _aiActive);
  if (!_aiActive) closeMatchPanel();
}

function openMatchPanel(matches, query) {
  _matchQuery = query;
  document.getElementById('match-panel-title').textContent = matches.length
    ? `${matches.length} match${matches.length !== 1 ? 'es' : ''} for "${query}"`
    : `No matches for "${query}"`;
  document.getElementById('match-panel-list').innerHTML = matches.length
    ? matches.map(renderMatchCard).join('')
    : `<div style="text-align:center;padding:36px 16px;color:var(--text-3);">
         <div style="font-size:28px;margin-bottom:12px;">🔍</div>
         <div style="font-size:12px;line-height:1.65;">No matching profiles found.<br>Try different keywords.</div>
       </div>`;
  document.getElementById('match-panel').classList.add('match-panel--open');
  document.getElementById('match-panel-backdrop').classList.add('match-panel-backdrop--visible');
}

function showMatchPanelLoading() {
  document.getElementById('match-panel-title').textContent = 'Searching…';
  document.getElementById('match-panel-list').innerHTML = [1,2,3].map(() => `
    <div class="match-card">
      <div class="match-card__avatar" style="background:var(--surface-2);"></div>
      <div class="match-card__body">
        <div class="skeleton" style="height:13px;border-radius:4px;width:55%;margin-bottom:6px;"></div>
        <div class="skeleton" style="height:10px;border-radius:4px;width:38%;margin-bottom:10px;"></div>
        <div class="skeleton" style="height:10px;border-radius:4px;width:92%;margin-bottom:5px;"></div>
        <div class="skeleton" style="height:10px;border-radius:4px;width:68%;"></div>
      </div>
    </div>`).join('');
  document.getElementById('match-panel').classList.add('match-panel--open');
  document.getElementById('match-panel-backdrop').classList.add('match-panel-backdrop--visible');
}

function closeMatchPanel() {
  document.getElementById('match-panel').classList.remove('match-panel--open');
  document.getElementById('match-panel-backdrop').classList.remove('match-panel-backdrop--visible');
}

function renderMatchCard(m) {
  const initials = ((m.firstName?.[0] ?? '') + (m.lastName?.[0] ?? '')).toUpperCase() || '?';
  const hue      = (m.username ?? '').split('').reduce((a, c) => a + c.charCodeAt(0), 0) % 360;
  const photoUrl = UserAPI.photoUrl(m.profilePhotoUrl);
  const avatarHtml = photoUrl
    ? `<img class="match-card__avatar" src="${photoUrl}" alt="${escapeHtml(m.firstName)}">`
    : `<div class="match-card__avatar" style="background:hsl(${hue},35%,20%);border:1.5px solid hsl(${hue},40%,30%);">
         <span style="font-family:var(--font-display);font-weight:800;font-size:16px;color:hsl(${hue},60%,72%);">${initials}</span>
       </div>`;
  const pctHtml    = m.compatibilityScore != null ? `<div class="match-card__pct">${m.compatibilityScore}% match</div>` : '';
  const cityHtml   = m.activeListing ? `<div class="match-card__city">📍 ${escapeHtml(m.activeListing.city)}</div>` : '';
  const ctaHref    = `/profile/${m.username}`;
  const ctaLabel   = 'View Profile';
  const sparkle    = `<svg width="10" height="10" viewBox="0 0 24 24" fill="currentColor"><path d="M12 3c-1 3.5-3.5 6-7 7 3.5 1 6 3.5 7 7 1-3.5 3.5-6 7-7-3.5-1-6-3.5-7-7z"/></svg>`;
  const explainBtn = m.compatibilityScore != null
    ? `<button class="match-card__explain-btn" onclick="toggleExplain(${m.userId},this)">${sparkle} Why we match</button>` : '';
  return `
    <div class="match-card">
      ${avatarHtml}
      <div class="match-card__body">
        <div class="match-card__top">
          <div style="min-width:0;">
            <div class="match-card__name">${escapeHtml(m.firstName)} ${escapeHtml(m.lastName)}</div>
            <div class="match-card__handle">@${escapeHtml(m.username)}</div>
          </div>
          ${pctHtml}
        </div>
        ${cityHtml}
        <div class="match-card__bio">${escapeHtml(m.bio ?? 'No bio yet.')}</div>
        <div class="match-card__actions">
          ${explainBtn}
          <a href="${ctaHref}" class="btn btn--primary" style="height:30px;padding:0 14px;font-size:11px;">${ctaLabel}</a>
        </div>
        <div class="match-card__explain-area" id="explain-area-${m.userId}">
          <div class="match-card__explain-text" id="explain-text-${m.userId}"></div>
        </div>
      </div>
    </div>`;
}

async function toggleExplain(userId, btn) {
  const area   = document.getElementById(`explain-area-${userId}`);
  const textEl = document.getElementById(`explain-text-${userId}`);
  const isOpen = area.classList.toggle('match-card__explain-area--open');
  const sparkle = `<svg width="10" height="10" viewBox="0 0 24 24" fill="currentColor"><path d="M12 3c-1 3.5-3.5 6-7 7 3.5 1 6 3.5 7 7 1-3.5 3.5-6 7-7-3.5-1-6-3.5-7-7z"/></svg>`;
  btn.innerHTML = isOpen ? `${sparkle} Hide` : `${sparkle} Why we match`;
  if (!isOpen || textEl.dataset.loaded) return;
  textEl.innerHTML = `
    <div class="skeleton" style="height:12px;border-radius:4px;margin-bottom:6px;width:90%"></div>
    <div class="skeleton" style="height:12px;border-radius:4px;margin-bottom:6px;width:75%"></div>
    <div class="skeleton" style="height:12px;border-radius:4px;width:55%"></div>`;
  try {
    const res = await MatchAPI.explainMatch(userId, _matchQuery || null);
    textEl.textContent = res.explanation || 'No explanation available.';
    textEl.dataset.loaded = '1';
  } catch {
    textEl.textContent = 'Could not load explanation — try again.';
  }
}
