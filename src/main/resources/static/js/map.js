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
  Promise.allSettled([apiFetch('/api/tour-listing'), UserAPI.getAll()])
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
    });
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
  const MONTHS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  const d = new Date(listing.meetingDate);
  const dateStr = `${String(d.getDate()).padStart(2,'0')} ${MONTHS[d.getMonth()]} · ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
  const hostName   = listing.host ? listing.host.firstName + ' ' + listing.host.lastName : '';
  const hostHandle = listing.host?.username ?? '';
  const available  = !listing.reserved;
  const dotColor   = available ? '#00c9a7' : 'rgba(239,239,239,0.3)';
  const dotGlow    = available ? 'box-shadow:0 0 5px #00c9a7;' : '';
  const statusLabel = available ? 'Available' : 'Booked';
  const actionHref  = hostHandle ? `/tours?host=${hostHandle}` : '/tours';

  let hostHtml = '';
  if (hostName) {
    const u = MapState.userCache[hostHandle];
    const hue = hostHandle.split('').reduce((a, c) => a + c.charCodeAt(0), 0) % 360;
    const initials = listing.host ? ((listing.host.firstName[0]||'') + (listing.host.lastName[0]||'')).toUpperCase() : '?';
    const photoUrl = u?.profilePhotoUrl ? UserAPI.photoUrl(u.profilePhotoUrl) : null;
    const avatarInner = photoUrl
      ? `<img src="${photoUrl}" style="width:100%;height:100%;object-fit:cover;" loading="lazy">`
      : `<span style="font-size:10px;font-weight:700;color:hsl(${hue},60%,72%);">${initials}</span>`;
    const avatar = `<div style="width:28px;height:28px;border-radius:50%;overflow:hidden;background:hsl(${hue},35%,22%);border:1px solid hsl(${hue},40%,35%);display:flex;align-items:center;justify-content:center;flex-shrink:0;">${avatarInner}</div>`;
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
   SEARCH
─────────────────────────────────────────────── */
const searchInput = document.getElementById('map-search-input');
if (searchInput) {
  searchInput.addEventListener('input', e => {
    const query = e.target.value.trim().toLowerCase();
    MapState.clusterGroup.clearLayers();
    MapState.allMarkers.forEach(({ marker, listing }) => {
      if (MapState.availableOnly && listing.reserved) return;
      if (!query) { MapState.clusterGroup.addLayer(marker); return; }
      const text = [listing.city, listing.host?.firstName, listing.host?.lastName, listing.host?.username]
        .filter(Boolean).join(' ').toLowerCase();
      if (text.includes(query)) MapState.clusterGroup.addLayer(marker);
    });
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
