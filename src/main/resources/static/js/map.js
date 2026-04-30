/* ═══════════════════════════════════════════════
   EXPERIMATE — map.js
   Leaflet map init, clustered pins, rich popups.
═══════════════════════════════════════════════ */

const MapState = {
  map: null,
  clusterGroup: null,
  allMarkers: [],       // { marker, listing } — full list for filtering
  availableOnly: false,
  userCache: {},        // username → UserResponse (for popup photos)
};

/* ───────────────────────────────────────────────
   INIT
─────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
  initMap();
  loadPins();
});

function initMap() {
  MapState.map = L.map('map', {
    center:           [45.815, 15.982],
    zoom:             14,
    zoomControl:      false,
    attributionControl: false,
  });

  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    maxZoom: 19,
  }).addTo(MapState.map);

  MapState.clusterGroup = L.markerClusterGroup({
    maxClusterRadius: 50,
    spiderfyOnMaxZoom: true,
    showCoverageOnHover: false,
    zoomToBoundsOnClick: true,
  });
  MapState.map.addLayer(MapState.clusterGroup);

  L.control.zoom({ position: 'bottomleft' }).addTo(MapState.map);

  const flyToRaw = sessionStorage.getItem('mapFlyTo');
  if (flyToRaw) {
    try {
      const { lat, lng, zoom } = JSON.parse(flyToRaw);
      MapState.map.setView([lat, lng], zoom || 16);
    } catch (e) { /* ignore */ }
    sessionStorage.removeItem('mapFlyTo');
  }
}

/* ───────────────────────────────────────────────
   LOAD PINS
─────────────────────────────────────────────── */
function loadPins() {
  Promise.allSettled([TourListingAPI.getAll(), UserAPI.getAll()])
    .then(([listingsResult, usersResult]) => {
      const listings = listingsResult.status === 'fulfilled' ? listingsResult.value : [];
      const users    = usersResult.status  === 'fulfilled' ? usersResult.value  : [];
      (users || []).forEach(u => { if (u.username) MapState.userCache[u.username] = u; });
      (listings || []).forEach(listing => {
        if (listing.lat == null || listing.lng == null) return;
        const marker = buildMarker(listing);
        MapState.allMarkers.push({ marker, listing });
        MapState.clusterGroup.addLayer(marker);
      });

      const count = MapState.allMarkers.length;
      if (count === 0) {
        showMapEmptyState();
      } else if (!localStorage.getItem('map_tour_seen')) {
        localStorage.setItem('map_tour_seen', '1');
        setTimeout(() => showToast(`${count} local experience${count !== 1 ? 's' : ''} nearby — tap any pin to explore`, 'success'), 1200);
      }
    });
}

function showMapEmptyState() {
  const container = document.getElementById('page-map-content');
  if (!container) return;
  const el = document.createElement('div');
  el.id = 'map-empty-state';
  el.style.cssText = [
    'position:absolute;bottom:120px;left:50%;transform:translateX(-50%)',
    'z-index:10;background:rgba(10,10,10,0.92);backdrop-filter:blur(14px)',
    '-webkit-backdrop-filter:blur(14px);border:1px solid var(--border-2)',
    'border-radius:var(--radius);padding:14px 18px;text-align:center',
    'max-width:280px;width:calc(100% - 32px)',
  ].join(';');
  el.innerHTML = `
    <div style="font-family:var(--font-display);font-weight:700;font-size:13px;color:var(--text);margin-bottom:4px;">No listings here yet</div>
    <div style="font-size:11px;color:var(--text-2);line-height:1.6;margin-bottom:12px;">Try searching a city above, or be the first to host one.</div>
    <a href="/listings/new" style="display:inline-block;padding:7px 18px;border-radius:var(--radius-pill);background:var(--accent);color:#000;font-family:var(--font-display);font-weight:700;font-size:11px;text-decoration:none;">Host a day +</a>
  `;
  container.appendChild(el);
}

function buildMarker(listing) {
  const icon = L.divIcon({
    className: '',
    html: `<div class="map-pin map-pin--event"></div>`,
    iconSize:   [14, 14],
    iconAnchor: [7, 14],
    popupAnchor:[0, -16],
  });

  const marker = L.marker([listing.lat, listing.lng], { icon });
  marker.bindPopup(buildPopup(listing), { className: 'dark-popup', maxWidth: 220 });
  return marker;
}

function buildPopup(listing) {
  const dateStr = `${fmtDate(listing.meetingDate)} · ${fmtTime(listing.meetingDate)}`;
  const hostName   = listing.host ? listing.host.firstName + ' ' + listing.host.lastName : '';
  const hostHandle = listing.host?.username ?? '';
  const available  = !listing.reserved;
  const dotColor   = available ? '#00c9a7' : 'rgba(239,239,239,0.3)';
  const dotGlow    = available ? 'box-shadow:0 0 5px #00c9a7;' : '';
  const statusLabel = available ? 'Available' : 'Booked';
  const actionHref  = `/tours?listing=${listing.id}`;

  let hostHtml = '';
  if (hostName) {
    const avatar = userAvatar(hostHandle, 28, MapState.userCache[hostHandle]);
    hostHtml = `<div class="popup-host" style="display:flex;align-items:center;gap:7px;">${avatar}<span>${escapeHtml(hostName)}<span style="color:var(--text-3);font-size:9px;margin-left:4px;">@${escapeHtml(hostHandle)}</span></span></div>`;
  }

  return `
    <div class="popup-name">${escapeHtml(listing.city)}</div>
    ${hostHtml}
    <div class="popup-date">📅 ${dateStr}</div>
    <div class="popup-status">
      <div class="popup-status__dot" style="background:${dotColor};${dotGlow}"></div>
      <div class="popup-status__label" style="color:${dotColor};">${statusLabel}</div>
    </div>
    <a href="${actionHref}" class="popup-action">See listing →</a>
  `;
}

/* ───────────────────────────────────────────────
   FILTERS
─────────────────────────────────────────────── */
function mapFilterToggle(filterName) {
  // Legacy gem/event filter — no-op for now (all pins are events)
  // Kept for API compatibility with map.html pill buttons
}

function mapFilterAvailable() {
  MapState.availableOnly = !MapState.availableOnly;
  applyMarkerFilter();
}

function applyMarkerFilter() {
  MapState.clusterGroup.clearLayers();
  MapState.allMarkers.forEach(({ marker, listing }) => {
    if (MapState.availableOnly && listing.reserved) return;
    MapState.clusterGroup.addLayer(marker);
  });
}

/* ───────────────────────────────────────────────
   MATCH RESULTS PANEL
─────────────────────────────────────────────── */
let _matchQuery = '';

function openMatchPanel(matches, query) {
  _matchQuery = query;
  const title = document.getElementById('match-panel-title');
  const list  = document.getElementById('match-panel-list');

  title.textContent = matches.length
    ? `${matches.length} match${matches.length !== 1 ? 'es' : ''} for "${query}"`
    : `No matches for "${query}"`;

  list.innerHTML = matches.length
    ? matches.map(renderMatchCard).join('')
    : `<div style="text-align:center;padding:36px 16px;color:var(--text-3);">
         <div style="font-size:28px;margin-bottom:12px;">🔍</div>
         <div style="font-size:12px;line-height:1.65;">No matching profiles found.<br>Try different keywords.</div>
       </div>`;

  document.getElementById('match-panel').classList.add('match-panel--open');
  document.getElementById('match-panel-backdrop').classList.add('match-panel-backdrop--visible');
}

function showMatchPanelLoading(query) {
  document.getElementById('match-panel-title').textContent = `Searching…`;
  document.getElementById('match-panel-list').innerHTML = [1, 2, 3].map(() => `
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

  const pctHtml  = m.compatibilityScore != null
    ? `<div class="match-card__pct">${m.compatibilityScore}% match</div>` : '';
  const cityHtml = m.activeListing
    ? `<div class="match-card__city">📍 ${escapeHtml(m.activeListing.city)}</div>` : '';
  const ctaHref  = m.activeListing ? `/tours?listing=${m.activeListing.id}` : `/profile/${m.username}`;
  const ctaLabel = m.activeListing ? 'View Day' : 'View Profile';
  const explainBtn = m.compatibilityScore != null
    ? `<button class="match-card__explain-btn" onclick="toggleExplain(${m.userId}, this)">✦ Why we match</button>` : '';

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

  btn.textContent = isOpen ? '✦ Hide' : '✦ Why we match';
  if (!isOpen) return;
  if (textEl.dataset.loaded) return;

  textEl.innerHTML = `
    <div class="skeleton" style="height:12px;border-radius:4px;margin-bottom:6px;width:90%"></div>
    <div class="skeleton" style="height:12px;border-radius:4px;margin-bottom:6px;width:75%"></div>
    <div class="skeleton" style="height:12px;border-radius:4px;width:55%"></div>`;

  try {
    const res = await MatchAPI.explainMatch(userId, _matchQuery || null);
    textEl.textContent  = res.explanation || 'No explanation available.';
    textEl.dataset.loaded = '1';
  } catch {
    textEl.textContent = 'Could not load explanation — try again.';
  }
}

/* ───────────────────────────────────────────────
   SEARCH + GEOCODING
─────────────────────────────────────────────── */
const searchInput = document.getElementById('map-search-input');
if (searchInput) {
  let _matchTimer = null;

  // Live pin filter + debounced AI match search
  searchInput.addEventListener('input', e => {
    const raw   = e.target.value.trim();
    const lower = raw.toLowerCase();

    // Immediate: filter existing pins
    MapState.clusterGroup.clearLayers();
    MapState.allMarkers.forEach(({ marker, listing }) => {
      if (MapState.availableOnly && listing.reserved) return;
      if (!lower) { MapState.clusterGroup.addLayer(marker); return; }
      const text = [listing.city, listing.host?.firstName, listing.host?.lastName, listing.host?.username]
        .filter(Boolean).join(' ').toLowerCase();
      if (text.includes(lower)) MapState.clusterGroup.addLayer(marker);
    });

    clearTimeout(_matchTimer);
    if (!raw) { closeMatchPanel(); return; }

    // Debounced: AI match search panel
    _matchTimer = setTimeout(() => {
      showMatchPanelLoading(raw);
      MatchAPI.findMatches(raw)
        .then(matches => openMatchPanel(matches, raw))
        .catch(() => openMatchPanel([], raw));
    }, 420);
  });

  // Enter → geocode via Nominatim and fly to city
  searchInput.addEventListener('keydown', async e => {
    if (e.key !== 'Enter') return;
    const query = searchInput.value.trim();
    if (!query) return;
    searchInput.disabled = true;
    try {
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query)}&format=json&limit=1`,
        { headers: { 'Accept-Language': 'en' } }
      );
      const results = await res.json();
      if (!results.length) { showToast(`No location found for "${query}"`, 'error'); return; }
      const { lat, lon, display_name } = results[0];
      MapState.map.flyTo([parseFloat(lat), parseFloat(lon)], 13, { duration: 1.2 });
      showToast(`Flew to ${display_name.split(',')[0]}`, 'success');
    } catch {
      showToast('Could not reach geocoding service.', 'error');
    } finally {
      searchInput.disabled = false;
      searchInput.focus();
    }
  });
}

/* ───────────────────────────────────────────────
   PUBLIC API — websocket.js uses addPin()
─────────────────────────────────────────────── */
window.MapAPI = {
  addPin: (pin) => {
    // Legacy compat: minimal listing-like object
    const marker = buildMarker({
      lat: pin.lat, lng: pin.lng, city: pin.name,
      meetingDate: new Date().toISOString(), reserved: false, host: null,
    });
    MapState.allMarkers.push({ marker, listing: { reserved: false } });
    MapState.clusterGroup.addLayer(marker);
  },
};

/* ───────────────────────────────────────────────
   UTILS
─────────────────────────────────────────────── */
function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
