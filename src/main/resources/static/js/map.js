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
      if (count > 0 && !localStorage.getItem('map_tour_seen')) {
        localStorage.setItem('map_tour_seen', '1');
        setTimeout(() => showToast(`${count} local experience${count !== 1 ? 's' : ''} nearby — tap any pin to explore`, 'success'), 1200);
      }
    });
}


function buildMarker(listing) {
  const icon = L.divIcon({
    className: '',
    html: `<div class="map-pin map-pin--event"></div>`,
    iconSize:   [14, 14],
    iconAnchor: [7, 14],
  });

  const marker = L.marker([listing.lat, listing.lng], { icon });
  marker.on('click', () => openMapPopup(listing));
  return marker;
}

function openMapPopup(listing) {
  document.getElementById('map-popup-body').innerHTML = buildPopupContent(listing);
  document.getElementById('map-popup-footer').innerHTML = `<a class="popup-action">See listing →</a>`;
  document.getElementById('map-popup-overlay').style.display = 'flex';
}

function closeMapPopup() {
  document.getElementById('map-popup-overlay').style.display = 'none';
}

function buildPopupContent(listing) {
  const dateStr = `${fmtDate(listing.meetingDate)} ${new Date(listing.meetingDate).getFullYear()} · ${fmtTime(listing.meetingDate)}`;
  const hostName   = listing.host ? listing.host.firstName + ' ' + listing.host.lastName : '';
  const hostHandle = listing.host?.username ?? '';
  const available  = !listing.reserved;
  const dotColor   = available ? '#00c9a7' : 'rgba(239,239,239,0.3)';
  const dotGlow    = available ? 'box-shadow:0 0 5px #00c9a7;' : '';
  const statusLabel = available ? 'Available' : 'Booked';

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
   SEARCH — pin filter only
─────────────────────────────────────────────── */
const searchInput = document.getElementById('map-search-input');
if (searchInput) {
  searchInput.addEventListener('input', e => {
    const lower = e.target.value.trim().toLowerCase();
    MapState.clusterGroup.clearLayers();
    MapState.allMarkers.forEach(({ marker, listing }) => {
      if (MapState.availableOnly && listing.reserved) return;
      if (!lower) { MapState.clusterGroup.addLayer(marker); return; }
      const text = [listing.city, listing.host?.firstName, listing.host?.lastName, listing.host?.username]
        .filter(Boolean).join(' ').toLowerCase();
      if (text.includes(lower)) MapState.clusterGroup.addLayer(marker);
    });
  });
}

/* ── Geo search — fly to city via Nominatim ── */
const geoInput = document.getElementById('map-geo-input');
if (geoInput) {
  geoInput.addEventListener('keydown', async e => {
    if (e.key !== 'Enter') return;
    const query = geoInput.value.trim();
    if (!query) return;
    geoInput.disabled = true;
    try {
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query)}&format=json&limit=1`,
        { headers: { 'Accept-Language': 'en' } }
      );
      const results = await res.json();
      if (!results.length) { showToast(`No location found for "${query}"`, 'error'); return; }
      const { lat, lon, display_name } = results[0];
      MapState.map.flyTo([parseFloat(lat), parseFloat(lon)], 13, { duration: 1.2 });
      geoInput.blur();
    } catch {
      showToast('Could not reach geocoding service.', 'error');
    } finally {
      geoInput.disabled = false;
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
