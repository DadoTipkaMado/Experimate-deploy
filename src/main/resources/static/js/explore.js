/* ═══════════════════════════════════════════════
   EXPERIMATE — explore.js
   Listing card list + AI match panel.
   Feed items are polymorphic: type LISTING or AD.
═══════════════════════════════════════════════ */

let _allFeedItems  = [];   // raw feed items (LISTING + AD) — used when no filter active
let _allListings   = [];   // LISTING items only — used for search/filter
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
    FeedAPI.getPage(0),
    myRequestsPromise,
    UserAPI.getAll().catch(() => []),
  ]).then(([feedPage, myRequests, allUsers]) => {
    (allUsers || []).forEach(u => { if (u.username) _userCache[u.username] = u; });

    _myRequests = {};
    (myRequests || [])
      .filter(r => r.tourListing?.id)
      .forEach(r => { _myRequests[r.tourListing.id] = { status: r.status, id: r.id }; });

    const now = new Date();
    const items = feedPage.content || [];
    _allFeedItems = items.filter(i => i.type !== 'LISTING' || new Date(i.meetingDate) > now);
    _allListings  = _allFeedItems.filter(i => i.type === 'LISTING');
    _currentPage  = 0;
    _isLastPage   = feedPage.last ?? true;

    applyAndRender();

    const idParam = new URLSearchParams(window.location.search).get('id');
    if (idParam) {
      const targetId = parseInt(idParam, 10);
      history.replaceState({}, '', '/explore');
      const found = _allListings.find(l => l.id === targetId);
      if (found) {
        openListingDetailFromExplore(targetId);
      } else {
        TourListingAPI.getById(targetId)
          .then(l => openListingDetail(l, {}))
          .catch(() => showToast('Meet not found', 'error'));
      }
    }
  }).catch(() => renderFeed([]));

  const feedEl = document.getElementById('listing-feed');
  if (feedEl) feedEl.addEventListener('scroll', _onFeedScroll, { passive: true });
});

/* ───────────────────────────────────────────────
   INFINITE SCROLL
─────────────────────────────────────────────── */
function _onFeedScroll() {
  if (_isLoadingPage || _isLastPage) return;
  const pc = document.getElementById('listing-feed');
  if (!pc) return;
  // Prefetch the next page once the user is within ~1.5 reels of the bottom.
  const remaining = pc.scrollHeight - pc.clientHeight - pc.scrollTop;
  if (remaining > pc.clientHeight * 1.5) return;
  _isLoadingPage = true;
  _showFeedLoader();
  FeedAPI.getPage(_currentPage + 1)
    .then(page => {
      _currentPage++;
      _isLastPage = page.last ?? true;
      const now  = new Date();
      const fresh = (page.content || []).filter(i => i.type !== 'LISTING' || new Date(i.meetingDate) > now);
      _allFeedItems = [..._allFeedItems, ...fresh];
      _allListings  = [..._allListings, ...fresh.filter(i => i.type === 'LISTING')];
      // renderFeed() replaces innerHTML of the snap scroller, which resets
      // scrollTop to 0. Preserve position so appending a page doesn't jump
      // the user back to the first reel (earlier reels keep the same height).
      const prevScroll = pc.scrollTop;
      applyAndRender();
      pc.scrollTop = prevScroll;
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
  el.style.cssText = 'display:flex;justify-content:center;padding:24px 0;';
  el.innerHTML = '<div style="width:20px;height:20px;border:2px solid var(--border-2);border-top-color:var(--accent);border-radius:50%;animation:feedSpin 0.7s linear infinite;"></div>';
  feed.appendChild(el);
}

function _hideFeedLoader() {
  document.getElementById('feed-loader')?.remove();
}

/* ───────────────────────────────────────────────
   FILTERS
─────────────────────────────────────────────── */
function toggleAvailable(pill) {
  _availableOnly = !_availableOnly;
  pill.classList.toggle('pill--active', _availableOnly);
  applyAndRender();
}

function applyAndRender() {
  const filterActive = !!_searchQuery || _availableOnly;

  if (!filterActive) {
    renderFeed(_allFeedItems);
    return;
  }

  // Filter mode: show only LISTING items that match; ads hidden during search/filter
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

  if (_availableOnly) items = items.filter(l => (l.bookedCount ?? 0) < (l.maxGuests ?? 1));

  items.sort((a, b) => new Date(a.meetingDate) - new Date(b.meetingDate));

  renderFeed(items);
}

/* ───────────────────────────────────────────────
   ICONS — inline SVGs for the reel action rail
─────────────────────────────────────────────── */
const _svg = (inner) =>
  `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor"
        stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${inner}</svg>`;

const REEL_ICON = {
  plus:  _svg('<line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>'),
  check: _svg('<polyline points="20 6 9 17 4 12"/>'),
  clock: _svg('<circle cx="12" cy="12" r="9"/><polyline points="12 7 12 12 15 14"/>'),
  ban:   _svg('<circle cx="12" cy="12" r="9"/><line x1="5.6" y1="5.6" x2="18.4" y2="18.4"/>'),
  eye:   _svg('<path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7z"/><circle cx="12" cy="12" r="3"/>'),
  login: _svg('<path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/><polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/>'),
  share: _svg('<circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.6" y1="13.5" x2="15.4" y2="17.5"/><line x1="15.4" y1="6.5" x2="8.6" y2="10.5"/>'),
  link:  _svg('<path d="M7 17 17 7"/><polyline points="8 7 17 7 17 16"/>'),
};

/** Builds one action-rail button: a circular icon with a small label beneath. */
function reelActionButton(modifier, icon, label, attrs) {
  return `<button class="reel__action reel__action--${modifier}" ${attrs}>
            <span class="reel__action-icon">${icon}</span><span>${label}</span>
          </button>`;
}

/* ───────────────────────────────────────────────
   RENDER — full-screen snap reels (listings + ads)
─────────────────────────────────────────────── */
function renderFeed(items) {
  const feed = document.getElementById('listing-feed');
  if (!feed) return;

  const listingsOnly = items.filter(i => !i.type || i.type === 'LISTING');

  if (!items.length || !listingsOnly.length) {
    feed.innerHTML = `
      <div class="explore-empty">
        <div class="explore-empty__icon">${_searchQuery ? '🔍' : '🗺️'}</div>
        <div class="explore-empty__title">${_searchQuery ? `No meets for "${escapeHtml(_searchQuery)}"` : 'No meets yet'}</div>
        <div class="explore-empty__sub">${_searchQuery ? 'Try a different city or host name.' : 'Be the first to host a local experience.'}</div>
        ${!_searchQuery ? `<a href="/listings/new" class="btn btn--primary" style="margin-top:8px;">+ Host a meet</a>` : ''}
      </div>`;
    return;
  }

  const currentUsername = Auth.getUsername();
  const currentUserId   = Auth.getUserId();

  feed.innerHTML = items.map((item) => {
    if (item.type === 'AD') return renderAdReel(item);

    const l = item;
    const myReq     = _myRequests[l.id];
    const reqStatus = myReq?.status;
    const isOwn     = !!(currentUsername && l.host?.username === currentUsername);
    const maxGuests = l.maxGuests ?? 1;
    const guestCnt  = l.bookedCount ?? 0;
    const isFull    = guestCnt >= maxGuests;
    const spotsLeft = Math.max(0, maxGuests - guestCnt);

    const hostHandle = l.host?.username ?? '';
    const hostName   = l.host ? ((l.host.firstName ?? '') + ' ' + (l.host.lastName ?? '')).trim() || hostHandle : hostHandle;
    const u          = _userCache[hostHandle];
    const hue        = hostHandle.split('').reduce((a, c) => a + c.charCodeAt(0), 0) % 360;
    const photoUrl   = u?.profilePhotoUrl ? UserAPI.photoUrl(u.profilePhotoUrl) : null;
    const initials   = ((u?.firstName?.[0] ?? '') + (u?.lastName?.[0] ?? '')).toUpperCase() || hostHandle[0]?.toUpperCase() || '?';
    const locationUnlocked = isOwn || reqStatus === 'ACCEPTED';
    const cityLabel  = locationUnlocked ? escapeHtml(l.city ?? '') : `Near ${escapeHtml(l.city ?? '')}`;

    const avatarHtml = photoUrl
      ? `<img src="${photoUrl}" alt="" loading="lazy">`
      : `<span style="color:hsl(${hue},60%,72%);">${initials}</span>`;
    const avatarBg = photoUrl ? '' : `background:hsl(${hue},35%,16%);`;

    let badgeClass, badgeLabel;
    if (isOwn) {
      badgeClass = 'own';      badgeLabel = 'Hosting';
    } else if (reqStatus === 'ACCEPTED') {
      badgeClass = 'accepted'; badgeLabel = 'Going';
    } else if (reqStatus === 'PENDING') {
      badgeClass = 'pending';  badgeLabel = 'Pending';
    } else if (isFull) {
      badgeClass = 'full';     badgeLabel = 'Full';
    } else {
      badgeClass = 'available';
      badgeLabel = maxGuests > 1 ? `${spotsLeft} spot${spotsLeft !== 1 ? 's' : ''}` : 'Open';
    }

    // Primary rail action — mirrors the join state machine.
    let actionBtn;
    if (isOwn) {
      actionBtn = reelActionButton('own', REEL_ICON.eye, 'View',
        `onclick="event.stopPropagation();openListingDetailFromExplore(${l.id})"`);
    } else if (isFull) {
      actionBtn = reelActionButton('full', REEL_ICON.ban, 'Full', 'disabled');
    } else if (reqStatus === 'PENDING') {
      actionBtn = reelActionButton('pending', REEL_ICON.clock, 'Pending', 'disabled');
    } else if (reqStatus === 'ACCEPTED') {
      actionBtn = reelActionButton('accepted', REEL_ICON.check, 'Going', 'disabled');
    } else if (currentUserId && new Date(l.meetingDate).toDateString() === new Date().toDateString()) {
      actionBtn = reelActionButton('full', REEL_ICON.clock, 'Today', 'disabled');
    } else if (currentUserId) {
      actionBtn = reelActionButton('join', REEL_ICON.plus, 'Join',
        `data-listing-id="${l.id}" onclick="cardJoin(event,${l.id})"`);
    } else {
      actionBtn = reelActionButton('login', REEL_ICON.login, 'Sign in',
        `onclick="event.stopPropagation();window.location.href='/login'"`);
    }

    const shareBtn = reelActionButton('share', REEL_ICON.share, 'Share',
      `onclick="event.stopPropagation();reelShare(${l.id})"`);

    const showDots = maxGuests > 1 && maxGuests <= 8;
    const spotsDots = showDots
      ? `<span class="reel__spots">${
          Array.from({length: maxGuests}, (_, i) =>
            `<span class="reel__spot-dot${i < guestCnt ? ' reel__spot-dot--taken' : ''}"></span>`
          ).join('')
        }</span>`
      : '';

    return `
      <div class="reel" style="--card-hue:${hue}" onclick="openListingDetailFromExplore(${l.id})">
        <div class="reel__bg"></div>
        <div class="reel__rail">
          <a class="reel__avatar" href="/profile/${encodeURIComponent(hostHandle)}" style="${avatarBg}"
             onclick="event.stopPropagation()">${avatarHtml}</a>
          ${actionBtn}
          ${shareBtn}
        </div>
        <div class="reel__content">
          <span class="reel__badge reel__badge--${badgeClass}">${badgeLabel}</span>
          <div class="reel__host">${escapeHtml(hostName)}</div>
          <div class="reel__handle">@${escapeHtml(hostHandle)} <span class="reel__city">📍 ${cityLabel}</span></div>
          <div class="reel__desc">${escapeHtml(l.tourDescription ?? '')}</div>
          <div class="reel__date">${relTime(l.meetingDate)} · ${fmtTime(l.meetingDate)}${spotsDots}</div>
        </div>
      </div>`;
  }).join('');
}

/* ───────────────────────────────────────────────
   AD REEL
─────────────────────────────────────────────── */
function renderAdReel(ad) {
  const isEvent = ad.eventId != null;
  const label   = isEvent ? 'Sponsored event' : 'Sponsored';
  const ctaText = isEvent ? 'Tickets' : 'Visit';
  const imgHtml = ad.imageUrl
    ? `<div class="reel__ad-img"><img src="${escapeHtml(ad.imageUrl)}" alt="${escapeHtml(ad.title)}" loading="lazy"></div>`
    : '';
  const cardClick = ad.linkUrl
    ? `onclick="window.open('${escapeHtml(ad.linkUrl)}','_blank','noopener')" style="cursor:pointer;"`
    : 'style="cursor:default;"';
  const ctaBtn = ad.linkUrl
    ? reelActionButton('join', REEL_ICON.link, ctaText,
        `onclick="event.stopPropagation();window.open('${escapeHtml(ad.linkUrl)}','_blank','noopener')"`)
    : '';
  return `
    <div class="reel reel--ad" ${cardClick}>
      <div class="reel__bg"></div>
      <div class="reel__ad-label">${label}</div>
      ${imgHtml}
      <div class="reel__rail">${ctaBtn}</div>
      <div class="reel__content">
        <div class="reel__ad-title">${escapeHtml(ad.title)}</div>
        ${ad.description ? `<div class="reel__desc">${escapeHtml(ad.description)}</div>` : ''}
      </div>
    </div>`;
}

/* ───────────────────────────────────────────────
   CARD JOIN (in-place update, no full re-render)
─────────────────────────────────────────────── */
function cardJoin(e, listingId) {
  e.stopPropagation();
  if (!Auth.getToken()) { window.location.href = '/login'; return; }
  if (_myRequests[listingId]) return;

  const btn   = e.currentTarget;
  const label = btn.querySelector('span:last-child');
  btn.disabled = true;
  if (label) label.textContent = '…';

  BookingRequestAPI.create({ listingId })
    .then(res => {
      _myRequests[listingId] = { status: 'PENDING', id: res?.id };
      btn.className = 'reel__action reel__action--pending';
      btn.innerHTML = `<span class="reel__action-icon">${REEL_ICON.clock}</span><span>Pending</span>`;
      showToast('Request sent!', 'success');
    })
    .catch(err => {
      btn.disabled = false;
      btn.className = 'reel__action reel__action--join';
      btn.innerHTML = `<span class="reel__action-icon">${REEL_ICON.plus}</span><span>Join</span>`;
      showToast(friendlyBookingError(err), 'error');
    });
}

function openListingDetailFromExplore(listingId) {
  const listing = _allListings.find(l => l.id === listingId);
  if (!listing) return;
  const myReq = _myRequests[listingId];
  const isOwn = !!(Auth.getUsername() && listing.host?.username === Auth.getUsername());
  openListingDetail(listing, {
    isOwn,
    reserved:  (listing.bookedCount ?? 0) >= (listing.maxGuests ?? 1),
    reqStatus: myReq?.status ?? null,
    onJoinSuccess: (id) => {
      _myRequests[id] = { status: 'PENDING' };
      applyAndRender();
    },
  });
}

function reelShare(listingId) {
  const listing = _allListings.find(l => l.id === listingId);
  const url     = `${window.location.origin}/explore?id=${listingId}`;
  const city    = listing?.city ?? '';
  const title   = city ? `Meet in ${city} — ExperiMate` : 'ExperiMate';
  const text    = listing?.tourDescription || 'Check out this local experience on ExperiMate.';

  if (navigator.share) {
    navigator.share({ title, text, url }).catch(() => {});
  } else {
    navigator.clipboard.writeText(url)
      .then(() => showToast('Link copied!', 'success'))
      .catch(() => showToast('Could not copy link', 'error'));
  }
}

/* ───────────────────────────────────────────────
   RELATIVE TIME HELPER
─────────────────────────────────────────────── */
function relTime(dateStr) {
  const d     = new Date(dateStr);
  const diffMs = d - Date.now();
  const diffH  = diffMs / 3_600_000;
  const diffD  = diffMs / 86_400_000;
  if (diffH < 1)   return 'Soon';
  if (diffH < 6)   return `In ${Math.round(diffH)}h`;
  if (diffH < 20)  return 'Today';
  if (diffD < 1.5) return 'Tomorrow';
  if (diffD < 7)   return `In ${Math.floor(diffD)} days`;
  return fmtDate(dateStr);
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
        .catch(err => {
          const msg = err?.message ?? '';
          if (err?.status === 403 || msg.includes('permission') || msg.includes('premium')) {
            openMatchPanelPremium();
          } else if (msg.includes('slow down')) {
            openMatchPanelError('Too many searches. Please wait a moment and try again.');
          } else {
            closeMatchPanel();
            showToast('AI search failed — check your connection.', 'error');
          }
        });
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
  pill.classList.toggle('pill--ai', _aiActive);
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

function openMatchPanelPremium() {
  document.getElementById('match-panel-title').textContent = 'Premium feature';
  document.getElementById('match-panel-list').innerHTML = `
    <div style="text-align:center;padding:40px 20px;">
      <div style="font-size:32px;margin-bottom:14px;filter:drop-shadow(0 0 8px rgba(234,179,8,0.4));">✦</div>
      <div style="font-family:var(--font-display);font-weight:800;font-size:16px;color:var(--text);margin-bottom:8px;">AI search is Premium</div>
      <div style="font-size:12px;color:var(--text-3);line-height:1.65;max-width:220px;margin:0 auto 20px;">
        Find deeper personality matches with natural-language search — available on Premium.
      </div>
      <a href="/premium" style="display:inline-flex;align-items:center;height:38px;padding:0 20px;background:linear-gradient(135deg,#fbbf24 0%,#d97706 100%);color:#1a0e00;border-radius:var(--radius-pill);font-family:var(--font-display);font-weight:800;font-size:13px;text-decoration:none;box-shadow:0 4px 14px rgba(217,119,6,0.35);">Upgrade to Premium →</a>
    </div>`;
  document.getElementById('match-panel').classList.add('match-panel--open');
  document.getElementById('match-panel-backdrop').classList.add('match-panel-backdrop--visible');
}

function openMatchPanelError(message) {
  document.getElementById('match-panel-title').textContent = 'Search unavailable';
  document.getElementById('match-panel-list').innerHTML = `
    <div style="text-align:center;padding:36px 16px;color:var(--text-3);">
      <div style="font-size:28px;margin-bottom:12px;">⚠️</div>
      <div style="font-size:12px;line-height:1.65;">${escapeHtml(message)}</div>
    </div>`;
  document.getElementById('match-panel').classList.add('match-panel--open');
  document.getElementById('match-panel-backdrop').classList.add('match-panel-backdrop--visible');
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
  const pctHtml  = m.compatibilityScore != null ? `<div class="match-card__pct">${m.compatibilityScore}% match</div>` : '';
  const cityHtml = m.activeListing ? `<div class="match-card__city">📍 ${escapeHtml(m.activeListing.city)}</div>` : '';
  const sparkle  = `<svg width="10" height="10" viewBox="0 0 24 24" fill="currentColor"><path d="M12 3c-1 3.5-3.5 6-7 7 3.5 1 6 3.5 7 7 1-3.5 3.5-6 7-7-3.5-1-6-3.5-7-7z"/></svg>`;
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
          <a href="/profile/${m.username}" class="btn btn--primary" style="height:30px;padding:0 14px;font-size:11px;">View Profile</a>
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
  } catch (err) {
    const _m = err?.message ?? '';
    textEl.textContent = (err?.status === 403 || _m.includes('permission') || _m.includes('premium'))
      ? 'AI explanations are a Premium feature.'
      : 'Could not load explanation — try again.';
  }
}
