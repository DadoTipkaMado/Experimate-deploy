/* ═══════════════════════════════════════════════
   EXPERIMATE — map.js
   Leaflet map init, pin rendering, filter toggles.
   WebSocket live updates handled in websocket.js
═══════════════════════════════════════════════ */

/* ───────────────────────────────────────────────
   STATE
─────────────────────────────────────────────── */
const MapState = {
  map: null,
  layers: {
    gems:   L.layerGroup(),
    events: L.layerGroup(),
    locals: L.layerGroup(),
  },
  activeFilters: new Set(['gems', 'events', 'locals']),
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
    center:           [45.815, 15.982], // Zagreb default — frend can make this dynamic
    zoom:             14,
    zoomControl:      false,
    attributionControl: false,
  });

  // OpenStreetMap tiles
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
  }).addTo(MapState.map);

  // Add layer groups to map
  Object.values(MapState.layers).forEach(layer => layer.addTo(MapState.map));

  // Zoom control — top right, away from search bar
  L.control.zoom({ position: 'bottomright' }).addTo(MapState.map);

  // If Explore page sent us a flyTo target, jump to it
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
   Reads from data attribute injected by Thymeleaf.
   Falls back to demo data if no server data present.
─────────────────────────────────────────────── */
function loadPins() {
  const dataEl = document.getElementById('map-data');
  let pins = [];

  try {
    const raw = dataEl ? dataEl.getAttribute('data-pins') : '[]';
    pins = JSON.parse(raw);
  } catch (e) {
    console.warn('map.js: could not parse map pins JSON', e);
  }

  // If no server data yet — use demo pins for visual testing
  if (!pins || pins.length === 0) {
    pins = DEMO_PINS;
  }

  pins.forEach(pin => addPin(pin));
}

/* ───────────────────────────────────────────────
   ADD PIN
   Called on load AND from websocket.js for live updates.

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

  // Route to correct layer
  const layerKey = pin.type === 'gem' ? 'gems'
                 : pin.type === 'event' ? 'events'
                 : 'locals';

  const layer = MapState.layers[layerKey];
  if (layer) {
    marker.addTo(layer);
  }

  return marker;
}

function buildPopupHTML(pin) {
  const typeLabel = pin.type === 'gem'   ? 'Hidden Gem'
                  : pin.type === 'event' ? 'Event'
                  : 'Local';
  return `
    <div class="popup-name">${escapeHtml(pin.name)}</div>
    <div class="popup-type popup-type--${pin.type}">${typeLabel}</div>
  `;
}

/* ───────────────────────────────────────────────
   FILTERS
   Each pill toggles a layer on/off.
   mapFilterToggle is called from onclick in map.html.
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
   Basic client-side filter — frend can replace with
   real API call if needed.
─────────────────────────────────────────────── */
const searchInput = document.getElementById('map-search-input');
if (searchInput) {
  searchInput.addEventListener('input', (e) => {
    const query = e.target.value.trim().toLowerCase();
    if (!query) {
      // Show all active layers
      Object.entries(MapState.layers).forEach(([key, layer]) => {
        if (MapState.activeFilters.has(key)) {
          MapState.map.addLayer(layer);
        }
      });
      return;
    }
    // Filter markers by name match
    // (basic — works fine until real search endpoint is ready)
    Object.values(MapState.layers).forEach(layer => {
      layer.eachLayer(marker => {
        const popup = marker.getPopup();
        if (!popup) return;
        const content = popup.getContent() || '';
        const visible = content.toLowerCase().includes(query);
        const el = marker.getElement();
        if (el) el.style.display = visible ? '' : 'none';
      });
    });
  });
}

/* ───────────────────────────────────────────────
   PUBLIC API
   websocket.js uses addPin() to push live updates.
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

/* ───────────────────────────────────────────────
   DEMO PINS
   Used when no server data is present.
   Remove or ignore once backend is connected.
─────────────────────────────────────────────── */
const DEMO_PINS = [
  { lat: 45.8127, lng: 15.9750, name: 'Secret Courtyard Café',  type: 'gem'   },
  { lat: 45.8182, lng: 15.9912, name: 'Medveščak Street Art',    type: 'gem'   },
  { lat: 45.8072, lng: 15.9870, name: 'Jazz Bar "Škver"',        type: 'gem'   },
  { lat: 45.8230, lng: 15.9780, name: 'Rooftop Sunset Point',    type: 'event' },
  { lat: 45.8150, lng: 15.9650, name: 'Community Meetup',        type: 'event' },
  { lat: 45.8195, lng: 15.9700, name: 'Ana Horvat',              type: 'local' },
  { lat: 45.8110, lng: 15.9830, name: 'Luka Novak',              type: 'local' },
];