/* ═══════════════════════════════════════════════
   EXPERIMATE — map.js
   Leaflet map init, clustered pins, rich popups.
═══════════════════════════════════════════════ */

const MapState = {
  map: null,
  clusterGroup: null,
  allMarkers: [],       // { marker, listing, pinType } — full list for filtering
  availableOnly: false,
  userCache: {},        // username → UserResponse (for popup photos)
  myMeetMap: {},        // listingId → 'my-meet' | 'due-soon'
  unlockedIds: new Set(), // listingIds where exact location is visible (own or accepted)
  userLat: null,
  userLng: null,
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
    center:           [45.815, 15.977],
    zoom:             14,
    zoomControl:      false,
    attributionControl: false,
  });

  const tileUrl = () => document.body.classList.contains('light-mode')
    ? 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png'
    : 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png';

  MapState.tileLayer = L.tileLayer(tileUrl(), { maxZoom: 19 }).addTo(MapState.map);

  new MutationObserver(() => MapState.tileLayer.setUrl(tileUrl()))
    .observe(document.body, { attributes: true, attributeFilter: ['class'] });

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
  } else if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      pos => {
        const { latitude, longitude } = pos.coords;
        MapState.userLat = latitude;
        MapState.userLng = longitude;
        MapState.map.flyTo([latitude, longitude], 17, { duration: 1 });
        L.marker([latitude, longitude], {
          icon: L.divIcon({
            className: '',
            html: `<div class="user-location-pin"></div>`,
            iconSize:   [35, 35],
            iconAnchor: [17, 42],
          }),
          zIndexOffset: 1000,
        }).addTo(MapState.map);
      },
      () => { /* denied or unavailable — keep Zagreb default */ }
    );
  }
}

/* ───────────────────────────────────────────────
   LOAD PINS
─────────────────────────────────────────────── */
function loadPins() {
  const myResPromise = Auth.getToken()
    ? ReservationAPI.getMine({ filter: 'joined', timeframe: 'upcoming' }).catch(() => [])
    : Promise.resolve([]);
  const myListingsPromise = Auth.getToken()
    ? TourListingAPI.getMine().catch(() => [])
    : Promise.resolve([]);
  const myAcceptedPromise = Auth.getToken()
    ? BookingRequestAPI.getMine({ flowDirection: 'outgoing', status: 'ACCEPTED' }).catch(() => [])
    : Promise.resolve([]);

  Promise.allSettled([TourListingAPI.getAll(), UserAPI.getAll(), myResPromise, myListingsPromise, myAcceptedPromise])
    .then(([listingsResult, usersResult, myResResult, myListingsResult, myAcceptedResult]) => {
      const listings    = listingsResult.status    === 'fulfilled' ? (listingsResult.value    || []) : [];
      const users       = usersResult.status       === 'fulfilled' ? (usersResult.value       || []) : [];
      const myRes       = myResResult.status       === 'fulfilled' ? (myResResult.value       || []) : [];
      const myListings  = myListingsResult.status  === 'fulfilled' ? (myListingsResult.value  || []) : [];
      const myAccepted  = myAcceptedResult.status  === 'fulfilled' ? (myAcceptedResult.value  || []) : [];

      (users || []).forEach(u => { if (u.username) MapState.userCache[u.username] = u; });

      const now = new Date();
      const ONE_HOUR = 60 * 60 * 1000;
      myRes.filter(r => r.status !== 'CANCELLED').forEach(r => {
        const lid = r.tourListing?.id;
        if (!lid) return;
        const diff = new Date(r.tourListing.meetingDate) - now;
        if (diff <= 0) return;
        MapState.myMeetMap[lid] = diff <= ONE_HOUR ? 'due-soon' : 'my-meet';
        MapState.unlockedIds.add(lid);
      });
      myListings.forEach(l => MapState.unlockedIds.add(l.id));
      myAccepted.forEach(r => { if (r.tourListing?.id) MapState.unlockedIds.add(r.tourListing.id); });

      const seenIds = new Set();
      const allListings = [...listings, ...myListings];
      allListings.forEach(listing => {
        if (listing.lat == null || listing.lng == null) return;
        if (new Date(listing.meetingDate) < now) return;
        if (seenIds.has(listing.id)) return;
        seenIds.add(listing.id);
        const pinType = MapState.myMeetMap[listing.id] ?? 'default';
        const marker = buildMarker(listing, pinType);
        MapState.allMarkers.push({ marker, listing, pinType });
        MapState.clusterGroup.addLayer(marker);
      });

      const count = MapState.allMarkers.length;
      if (count > 0 && !localStorage.getItem('map_tour_seen')) {
        localStorage.setItem('map_tour_seen', '1');
        setTimeout(() => showToast(`${count} local experience${count !== 1 ? 's' : ''} nearby — tap any pin to explore`, 'success'), 1200);
      }

      // Restore overlay if user navigated back from a profile page
      const restoreRaw = sessionStorage.getItem('mapOverlayRestore');
      if (restoreRaw) {
        sessionStorage.removeItem('mapOverlayRestore');
        try {
          const { listing, lat, lng, zoom } = JSON.parse(restoreRaw);
          if (lat != null && lng != null) MapState.map.setView([lat, lng], zoom || 14, { animate: false });
          const isOwn    = !!(Auth.getUsername() && listing.host?.username === Auth.getUsername());
          const reserved = !!(MapState.myMeetMap[listing.id]);
          openListingDetail(listing, { isOwn, reserved, reqStatus: null });
        } catch (_) {}
      }
    });
}


function _approxCoords(lat, lng, id) {
  // ~300m deterministic offset for listings where exact location is not yet unlocked
  const dlat = ((id * 17) % 11 - 5) * 0.0015;
  const dlng = ((id * 13) % 9  - 4) * 0.0015;
  return [lat + dlat, lng + dlng];
}

function buildMarker(listing, pinType = 'default') {
  const unlocked = MapState.unlockedIds.has(listing.id);
  const [markerLat, markerLng] = unlocked
    ? [listing.lat, listing.lng]
    : _approxCoords(listing.lat, listing.lng, listing.id);

  const pinClass = pinType === 'due-soon' ? 'map-pin--due-soon'
                 : pinType === 'my-meet'  ? 'map-pin--my-meet'
                 : 'map-pin--event';

  const icon = L.divIcon({
    className: '',
    html: `<div class="map-pin ${pinClass}"></div>`,
    iconSize:   [14, 14],
    iconAnchor: [7, 14],
  });

  const marker = L.marker([markerLat, markerLng], { icon });
  marker.on('click', () => openMapPopup(listing, pinType, unlocked));
  return marker;
}

function openMapPopup(listing, pinType = 'default') {
  MapState._popupListing  = listing;
  MapState._popupPinType  = pinType;
  MapState._popupUnlocked = MapState.unlockedIds.has(listing.id);
  document.getElementById('map-popup-body').innerHTML = buildPopupContent(listing, pinType, MapState._popupUnlocked);
  document.getElementById('map-popup-footer').innerHTML = `<button class="popup-action" onclick="openListingDetailFromMap()">See listing →</button>`;
  document.getElementById('map-popup-overlay').style.display = 'flex';
}

function closeMapPopup() {
  document.getElementById('map-popup-overlay').style.display = 'none';
}

function openListingDetailFromMap() {
  const listing = MapState._popupListing;
  if (!listing) return;
  const isOwn     = !!(Auth.getUsername() && listing.host?.username === Auth.getUsername());
  const reserved  = !!(MapState.myMeetMap[listing.id]);
  const unlocked  = MapState.unlockedIds.has(listing.id);
  const reqStatus = unlocked && !reserved && !isOwn ? 'ACCEPTED' : null;
  closeMapPopup();
  openListingDetail(listing, { isOwn, reserved, reqStatus });
}

function centerOnUser() {
  if (MapState.userLat !== null) {
    MapState.map.flyTo([MapState.userLat, MapState.userLng], 17, { duration: 1 });
    return;
  }
  if (!navigator.geolocation) return;
  navigator.geolocation.getCurrentPosition(
    pos => {
      const { latitude, longitude } = pos.coords;
      MapState.userLat = latitude;
      MapState.userLng = longitude;
      MapState.map.flyTo([latitude, longitude], 17, { duration: 1 });
      L.marker([latitude, longitude], {
        icon: L.divIcon({
          className: '',
          html: `<div class="user-location-pin"></div>`,
          iconSize:   [35, 35],
          iconAnchor: [17, 42],
        }),
        zIndexOffset: 1000,
      }).addTo(MapState.map);
    },
    () => showToast('Location access denied.', 'error')
  );
}

function buildPopupContent(listing, pinType = 'default', unlocked = false) {
  const dateStr = `${fmtDate(listing.meetingDate)} ${new Date(listing.meetingDate).getFullYear()} · ${fmtTime(listing.meetingDate)}`;
  const hostName   = listing.host ? listing.host.firstName + ' ' + listing.host.lastName : '';
  const hostHandle = listing.host?.username ?? '';
  const maxG       = listing.maxGuests ?? 1;
  const curG       = listing.currentGuestCount ?? (listing.reserved ? 1 : 0);
  const isFull     = curG >= maxG;
  const dotColor   = isFull ? 'rgba(239,239,239,0.3)' : '#00c9a7';
  const dotGlow    = isFull ? '' : 'box-shadow:0 0 5px #00c9a7;';
  const statusLabel = isFull
    ? (maxG > 1 ? 'Full' : 'Booked')
    : maxG > 1 ? `${maxG - curG} spot${maxG - curG !== 1 ? 's' : ''} left` : 'Available';
  const guestHtml  = maxG > 1
    ? `<div class="popup-date">👥 ${curG}/${maxG} joined</div>` : '';

  let hostHtml = '';
  if (hostName) {
    const avatar = userAvatar(hostHandle, 28, MapState.userCache[hostHandle]);
    hostHtml = `<div class="popup-host" style="display:flex;align-items:center;gap:7px;">${avatar}<span>${escapeHtml(hostName)}<span style="color:var(--text-3);font-size:12px;margin-left:4px;">@${escapeHtml(hostHandle)}</span></span></div>`;
  }

  const MAX_DESC = 160;
  const fullDesc  = listing.tourDescription ?? '';
  const descHtml  = fullDesc
    ? `<div class="popup-desc">${escapeHtml(fullDesc.slice(0, MAX_DESC))}${fullDesc.length > MAX_DESC
        ? `<span style="color:var(--accent);cursor:pointer;font-weight:600;" onclick="openListingDetailFromMap()"> …more</span>`
        : ''}</div>`
    : '';

  let badgeHtml = '';
  if (pinType === 'due-soon') {
    badgeHtml = `<div style="display:inline-flex;align-items:center;gap:5px;background:rgba(245,158,11,0.15);border:1px solid #f59e0b;border-radius:6px;padding:3px 8px;font-size:11px;color:#f59e0b;letter-spacing:0.08em;font-weight:700;">⏱ DUE SOON</div>`;
  } else if (pinType === 'my-meet') {
    badgeHtml = `<div style="display:inline-flex;align-items:center;gap:5px;background:rgba(168,85,247,0.15);border:1px solid #a855f7;border-radius:6px;padding:3px 8px;font-size:11px;color:#a855f7;letter-spacing:0.08em;font-weight:700;">✓ YOUR MEET</div>`;
  }

  const cityLabel = unlocked ? escapeHtml(listing.city) : `Near ${escapeHtml(listing.city)}`;

  return `
    <div class="popup-name">${cityLabel}</div>
    ${badgeHtml}
    ${hostHtml}
    <div class="popup-date">📅 ${dateStr}</div>
    ${guestHtml}
    <div class="popup-status">
      <div class="popup-status__dot" style="background:${dotColor};${dotGlow}"></div>
      <div class="popup-status__label" style="color:${dotColor};">${statusLabel}</div>
    </div>
    ${descHtml}
  `;
}

/* ───────────────────────────────────────────────
   FILTERS
─────────────────────────────────────────────── */

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

