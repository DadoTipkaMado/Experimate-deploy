/* ═══════════════════════════════════════════════
   EXPERIMATE — map.js
   Leaflet map init, pin rendering, filter toggles.
   WebSocket live updates handled in websocket.js
═══════════════════════════════════════════════ */

/* ───────────────────────────────────────────────
   STATE
   All filters start ON — pills all start active.
─────────────────────────────────────────────── */
const MapState = {
  map: null,
  layers: {
    gems:   L.layerGroup(),
    events: L.layerGroup(),
  },
  activeFilters: new Set(['gems', 'events']),
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

  // Add all layer groups to map (all visible by default)
  Object.values(MapState.layers).forEach(layer => layer.addTo(MapState.map));

  // Zoom control bottom-left so it doesn't overlap bottom sheet
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
   LOAD PINS — fetches tour listings from API
─────────────────────────────────────────────── */
function loadPins() {
  fetch('/api/tour-listing')
    .then(res => res.ok ? res.json() : [])
    .then(listings => {
      listings.forEach(listing => {
        if (listing.lat == null || listing.lng == null) return;
        addPin({
          lat:  listing.lat,
          lng:  listing.lng,
          name: listing.city + (listing.host ? ' · ' + listing.host.firstName : ''),
          type: 'event',
        });
      });
    })
    .catch(() => {});
}

/* ───────────────────────────────────────────────
   ADD PIN
   pin = { lat, lng, name, type }
   type = 'gem' | 'event' | 'local'
─────────────────────────────────────────────── */
function addPin(pin) {
  const icon = L.divIcon({
    className: '',
    html: `<div class="map-pin map-pin--${pin.type}"></div>`,
    iconSize:   [14, 14],
    iconAnchor: [7, 14],
    popupAnchor:[0, -16],
  });

  const marker = L.marker([pin.lat, pin.lng], { icon })
    .bindPopup(buildPopupHTML(pin), { className: 'dark-popup', maxWidth: 200 });

  const layerKey = pin.type === 'gem' ? 'gems' : 'events';

  const layer = MapState.layers[layerKey];
  if (layer) marker.addTo(layer);

  return marker;
}

function buildPopupHTML(pin) {
  const typeLabel = pin.type === 'gem' ? 'Hidden Gem' : 'Event';
  return `
    <div class="popup-name">${escapeHtml(pin.name)}</div>
    <div class="popup-type popup-type--${pin.type}">${typeLabel}</div>
  `;
}

/* ───────────────────────────────────────────────
   FILTERS
   Active pill = layer visible. Inactive = hidden.
   All start active so initial state matches pills.
─────────────────────────────────────────────── */
function mapFilterToggle(filterName) {
  const layer = MapState.layers[filterName];
  if (!layer || !MapState.map) return;

  if (MapState.activeFilters.has(filterName)) {
    MapState.map.removeLayer(layer);
    MapState.activeFilters.delete(filterName);
  } else {
    MapState.map.addLayer(layer);
    MapState.activeFilters.add(filterName);
  }
}

/* ───────────────────────────────────────────────
   SEARCH
─────────────────────────────────────────────── */
const searchInput = document.getElementById('map-search-input');
if (searchInput) {
  searchInput.addEventListener('input', (e) => {
    const query = e.target.value.trim().toLowerCase();
    if (!query) {
      Object.entries(MapState.layers).forEach(([key, layer]) => {
        if (MapState.activeFilters.has(key)) MapState.map.addLayer(layer);
      });
      return;
    }
    Object.values(MapState.layers).forEach(layer => {
      layer.eachLayer(marker => {
        const popup = marker.getPopup();
        if (!popup) return;
        const content = popup.getContent() || '';
        const el = marker.getElement();
        if (el) el.style.display = content.toLowerCase().includes(query) ? '' : 'none';
      });
    });
  });
}

/* ───────────────────────────────────────────────
   PUBLIC API — websocket.js uses addPin()
─────────────────────────────────────────────── */
window.MapAPI = { addPin };

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
