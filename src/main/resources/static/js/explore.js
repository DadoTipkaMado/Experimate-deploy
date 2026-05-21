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

  // Wire header dim-on-scroll behaviour
  const reelHeader = document.getElementById('reel-header');
  const feedEl     = document.getElementById('listing-feed');
  if (reelHeader && feedEl) {
    let _lastIdx = 0;
    feedEl.addEventListener('scroll', () => {
      const idx = Math.round(feedEl.scrollTop / feedEl.clientHeight);
      if (idx === _lastIdx) return;
      _lastIdx = idx;
      reelHeader.classList.toggle('reel-header--dim', idx > 0);
    }, { passive: true });
    reelHeader.addEventListener('pointerdown', () => {
      reelHeader.classList.remove('reel-header--dim');
    });
  }

  // Populate user avatar in reel header if cached
  (function() {
    const userId = localStorage.getItem('userId');
    const photo  = userId ? localStorage.getItem('photo_' + userId) : null;
    const btn    = document.getElementById('reel-user-btn');
    if (btn && photo) btn.innerHTML = `<img src="${photo}" alt="" style="width:100%;height:100%;object-fit:cover;">`;
  })();

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

  // Infinite scroll — listen on the reel-feed container (not window)
  if (feedEl) feedEl.addEventListener('scroll', _onFeedScroll, { passive: true });
  else window.addEventListener('scroll', _onFeedScroll, { passive: true });
});

/* ───────────────────────────────────────────────
   INFINITE SCROLL
─────────────────────────────────────────────── */
function _onFeedScroll() {
  if (_isLoadingPage || _isLastPage) return;
  const feed = document.getElementById('listing-feed');
  const scrollTop    = feed ? feed.scrollTop    : window.scrollY;
  const scrollHeight = feed ? feed.scrollHeight : document.body.scrollHeight;
  const clientHeight = feed ? feed.clientHeight : window.innerHeight;
  if ((clientHeight + scrollTop) < (scrollHeight - 300)) return;
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
  pill.classList.toggle('reel-chip--active', _availableOnly);
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

  if (_availableOnly) items = items.filter(l => (l.currentGuestCount ?? 0) < (l.maxGuests ?? 1));

  items.sort((a, b) => new Date(a.meetingDate) - new Date(b.meetingDate));

  renderFeed(items);
}

/* ───────────────────────────────────────────────
   RENDER — TikTok-style snap-scroll reels
─────────────────────────────────────────────── */
function renderFeed(listings) {
  const feed = document.getElementById('listing-feed');
  if (!feed) return;

  if (!listings.length) {
    feed.innerHTML = `
      <div class="reel-empty">
        <div class="reel-empty__icon">${_searchQuery ? '🔍' : '🗺️'}</div>
        <div class="reel-empty__title">${_searchQuery ? `No meets for "${escapeHtml(_searchQuery)}"` : 'No meets yet'}</div>
        <div class="reel-empty__sub">${_searchQuery ? 'Try a different city or host name.' : 'Be the first to host a local experience.'}</div>
        ${!_searchQuery ? `<a href="/listings/new" class="btn btn--primary" style="margin-top:8px;">+ Host a meet</a>` : ''}
      </div>`;
    return;
  }

  const currentUsername = Auth.getUsername();
  const currentUserId   = Auth.getUserId();

  // Show swipe hint on first ever visit
  const showHint = !localStorage.getItem('reel_hint_seen');
  if (showHint) localStorage.setItem('reel_hint_seen', '1');

  feed.innerHTML = listings.map((l) => {
    const myReq     = _myRequests[l.id];
    const reqStatus = myReq?.status;
    const isOwn     = !!(currentUsername && l.host?.username === currentUsername);
    const maxGuests = l.maxGuests ?? 1;
    const guestCnt  = l.currentGuestCount ?? 0;
    const isFull    = guestCnt >= maxGuests;
    const spotsLeft = Math.max(0, maxGuests - guestCnt);

    const hostHandle = l.host?.username ?? '';
    const hostName   = l.host ? ((l.host.firstName ?? '') + ' ' + (l.host.lastName ?? '')).trim() || hostHandle : hostHandle;
    const u          = _userCache[hostHandle];
    const hue        = hostHandle.split('').reduce((a, c) => a + c.charCodeAt(0), 0) % 360;
    const photoUrl   = u?.profilePhotoUrl ? UserAPI.photoUrl(u.profilePhotoUrl) : null;
    const initials   = ((u?.firstName?.[0] ?? '') + (u?.lastName?.[0] ?? '')).toUpperCase() || hostHandle[0]?.toUpperCase() || '?';

    const bgStyle = photoUrl
      ? `background-image:url('${photoUrl}');background-size:cover;background-position:center;`
      : `background:radial-gradient(ellipse at 30% 65%, hsl(${hue},55%,18%) 0%, hsl(${(hue+40)%360},35%,08%) 60%, hsl(${(hue+180)%360},25%,05%) 100%);`;

    const avatarHtml = photoUrl
      ? `<img src="${photoUrl}" alt="${escapeHtml(hostHandle)}" loading="lazy">`
      : `<div class="reel-host-avatar__initials" style="background:hsl(${hue},35%,16%);color:hsl(${hue},60%,72%);">${initials}</div>`;

    // Status
    const dotColor = isFull             ? 'rgba(255,255,255,0.28)'
      : reqStatus === 'PENDING'         ? '#ff9944'
      : reqStatus === 'ACCEPTED'        ? '#00c9a7'
      : reqStatus === 'DECLINED'        ? 'rgba(255,80,80,0.7)'
      : '#00c9a7';
    const statusLabel = isFull          ? (maxGuests > 1 ? 'Full' : 'Joined')
      : reqStatus === 'PENDING'         ? 'Pending'
      : reqStatus === 'ACCEPTED'        ? 'Accepted'
      : reqStatus === 'DECLINED'        ? 'Declined'
      : maxGuests > 1 ? `${spotsLeft} spot${spotsLeft !== 1 ? 's' : ''} left` : 'Available';

    const locationUnlocked = isOwn || reqStatus === 'ACCEPTED';
    const cityLabel = locationUnlocked ? escapeHtml(l.city ?? '') : `Near ${escapeHtml(l.city ?? '')}`;

    // Right sidebar — join button
    let joinActionClass, joinSvg, joinLabel, joinClick;
    if (isOwn) {
      joinActionClass = 'reel-action--own';
      joinSvg  = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#ff6b35" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/></svg>`;
      joinLabel = 'Yours';
      joinClick = `onclick="event.stopPropagation();openListingDetailFromExplore(${l.id})"`;
    } else if (isFull) {
      joinActionClass = 'reel-action--full';
      joinSvg  = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,0.35)" stroke-width="2" stroke-linecap="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>`;
      joinLabel = maxGuests > 1 ? 'Full' : 'Joined';
      joinClick = '';
    } else if (reqStatus === 'PENDING') {
      joinActionClass = 'reel-action--pending';
      joinSvg  = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#ff9944" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="9"/><polyline points="12 7 12 12 15 15"/></svg>`;
      joinLabel = 'Pending';
      joinClick = '';
    } else if (reqStatus === 'ACCEPTED') {
      joinActionClass = 'reel-action--accepted';
      joinSvg  = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#00c9a7" stroke-width="2.5" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>`;
      joinLabel = 'Joined';
      joinClick = '';
    } else if (currentUserId) {
      joinActionClass = 'reel-action--join';
      joinSvg  = `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>`;
      joinLabel = 'Join';
      joinClick = `onclick="event.stopPropagation();reelJoin(event,${l.id})"`;
    } else {
      joinActionClass = 'reel-action--join';
      joinSvg  = `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>`;
      joinLabel = 'Sign in';
      joinClick = `onclick="event.stopPropagation();window.location.href='/login'"`;
    }

    const mapActionHtml = (l.lat != null && locationUnlocked) ? `
      <div class="reel-action" onclick="event.stopPropagation();reelGoToMap(${l.lat},${l.lng})">
        <div class="reel-action__btn">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,0.82)" stroke-width="2" stroke-linecap="round"><polygon points="3 6 9 3 15 6 21 3 21 18 15 21 9 18 3 21"/><line x1="9" y1="3" x2="9" y2="18"/><line x1="15" y1="6" x2="15" y2="21"/></svg>
        </div>
        <span class="reel-action__label">Map</span>
      </div>` : '';

    return `
      <div class="reel-card">
        <div class="reel-card__bg" style="${bgStyle}"></div>
        <div class="reel-card__overlay"></div>
        <div class="reel-tap-zone" onclick="openListingDetailFromExplore(${l.id})"></div>

        <div class="reel-sidebar">
          <div class="reel-host-avatar" onclick="event.stopPropagation();window.location.href='/profile/${encodeURIComponent(hostHandle)}'">${avatarHtml}</div>
          <div class="reel-action ${joinActionClass}" data-listing-id="${l.id}" ${joinClick}>
            <div class="reel-action__btn">${joinSvg}</div>
            <span class="reel-action__label">${joinLabel}</span>
          </div>
          ${mapActionHtml}
        </div>

        <div class="reel-info" onclick="openListingDetailFromExplore(${l.id})">
          <div class="reel-info__city">${cityLabel}</div>
          <div class="reel-info__host">
            <span class="reel-info__host-name">${escapeHtml(hostName)}</span>
            <span class="reel-info__handle">@${escapeHtml(hostHandle)}</span>
          </div>
          <div class="reel-info__desc">${escapeHtml(l.tourDescription ?? '')}</div>
          <div class="reel-info__meta">
            <div class="reel-info__status">
              <div class="reel-info__dot" style="background:${dotColor};"></div>
              <span style="color:${dotColor};">${statusLabel}</span>
            </div>
            <div class="reel-info__date">${fmtDate(l.meetingDate)} · ${fmtTime(l.meetingDate)}</div>
          </div>
        </div>
        ${showHint && listings.indexOf(l) === 0 ? '<div class="reel-swipe-hint"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="rgba(255,255,255,0.6)" stroke-width="2.5" stroke-linecap="round"><polyline points="18 15 12 9 6 15"/></svg><span class="reel-swipe-hint__text">Swipe up</span></div>' : ''}
      </div>`;
  }).join('');
}

function goToMap(btn) {
  sessionStorage.setItem('mapFlyTo', JSON.stringify({ lat: parseFloat(btn.dataset.lat), lng: parseFloat(btn.dataset.lng), zoom: 16 }));
  window.location.href = '/map';
}

/* ── Reel sidebar join (no feed rebuild, update in-place) ── */
function reelJoin(e, listingId) {
  e.stopPropagation();
  if (!Auth.getToken()) { window.location.href = '/login'; return; }
  if (_myRequests[listingId]) return;

  const actionEl = e.currentTarget.closest('.reel-action');
  const btnEl    = actionEl?.querySelector('.reel-action__btn');
  const labelEl  = actionEl?.querySelector('.reel-action__label');
  if (btnEl) btnEl.innerHTML = `<div style="width:18px;height:18px;border:2px solid rgba(255,255,255,0.3);border-top-color:#fff;border-radius:50%;animation:feedSpin 0.7s linear infinite;"></div>`;

  BookingRequestAPI.create({ listingId })
    .then(res => {
      _myRequests[listingId] = { status: 'PENDING', id: res?.id };
      if (actionEl) { actionEl.className = 'reel-action reel-action--pending'; actionEl.removeAttribute('onclick'); }
      if (btnEl)    btnEl.innerHTML = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#ff9944" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="9"/><polyline points="12 7 12 12 15 15"/></svg>`;
      if (labelEl)  labelEl.textContent = 'Pending';
      showToast('Request sent!', 'success');
    })
    .catch(err => {
      if (actionEl) actionEl.className = 'reel-action reel-action--join';
      if (btnEl)    btnEl.innerHTML = `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>`;
      showToast(friendlyBookingError(err), 'error');
    });
}

function reelGoToMap(lat, lng) {
  sessionStorage.setItem('mapFlyTo', JSON.stringify({ lat, lng, zoom: 16 }));
  window.location.href = '/map';
}

function openListingDetailFromExplore(listingId) {
  const listing = _allListings.find(l => l.id === listingId);
  if (!listing) return;
  const myReq    = _myRequests[listingId];
  const isOwn    = !!(Auth.getUsername() && listing.host?.username === Auth.getUsername());
  openListingDetail(listing, {
    isOwn,
    reserved:   (listing.currentGuestCount ?? 0) >= (listing.maxGuests ?? 1),
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
  pill.classList.toggle('reel-chip--active', _aiActive);
  pill.classList.toggle('reel-chip--ai', _aiActive);
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
    ? `<button class="match-card__explain-btn" data-user-id="${m.userId}" onclick="toggleExplain(this)">${sparkle} Why we match</button>` : '';
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
        <div class="match-card__explain-area">
          <div class="match-card__explain-text"></div>
        </div>
      </div>
    </div>`;
}

async function toggleExplain(btn) {
  const userId = parseInt(btn.dataset.userId);
  const card   = btn.closest('.match-card');
  const area   = card.querySelector('.match-card__explain-area');
  const textEl = card.querySelector('.match-card__explain-text');
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
