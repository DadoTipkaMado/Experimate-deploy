/* ═══════════════════════════════════════════════
   EXPERIMATE — websocket.js
   Live map pin updates via WebSocket.

   Depends on: map.js (window.MapAPI must exist)

   ─── HOW IT WORKS ───────────────────────────
   1. Connects to Spring Boot WebSocket endpoint
   2. Server pushes new pin JSON when a local adds a gem/event
   3. We call MapAPI.addPin() to drop it on the map instantly
   4. No page refresh needed

   ─── FREND SETUP NEEDED ─────────────────────
   Spring Boot dependency in pom.xml:
     spring-boot-starter-websocket

   Controller sends message in format:
   {
     "lat":  45.815,
     "lng":  15.982,
     "name": "New Hidden Gem",
     "type": "gem"
   }

   WebSocket endpoint URL: /ws/map
   (change WS_URL below if frend uses a different path)
═══════════════════════════════════════════════ */

const WS_URL = '/ws/map';

/* ───────────────────────────────────────────────
   CONNECTION
─────────────────────────────────────────────── */
let _ws = null;
let _reconnectTimer = null;
let _reconnectDelay = 3000; // ms, doubles on each failed attempt

function connectWebSocket() {
  // Build full WS URL from current host
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const host     = window.location.host;
  const fullUrl  = `${protocol}//${host}${WS_URL}`;

  try {
    _ws = new WebSocket(fullUrl);
  } catch (e) {
    console.warn('websocket.js: could not create WebSocket', e);
    scheduleReconnect();
    return;
  }

  _ws.onopen = () => {
    console.info('websocket.js: connected to', fullUrl);
    _reconnectDelay = 3000; // reset backoff on success
  };

  _ws.onmessage = (event) => {
    handleMessage(event.data);
  };

  _ws.onerror = (err) => {
    console.warn('websocket.js: error', err);
  };

  _ws.onclose = () => {
    console.info('websocket.js: connection closed, retrying...');
    scheduleReconnect();
  };
}

function scheduleReconnect() {
  clearTimeout(_reconnectTimer);
  _reconnectTimer = setTimeout(() => {
    _reconnectDelay = Math.min(_reconnectDelay * 2, 30000); // max 30s
    connectWebSocket();
  }, _reconnectDelay);
}

/* ───────────────────────────────────────────────
   MESSAGE HANDLER
─────────────────────────────────────────────── */
function handleMessage(raw) {
  let pin;

  try {
    pin = JSON.parse(raw);
  } catch (e) {
    console.warn('websocket.js: received non-JSON message', raw);
    return;
  }

  // Validate required fields
  if (!pin.lat || !pin.lng || !pin.name || !pin.type) {
    console.warn('websocket.js: pin missing required fields', pin);
    return;
  }

  // Push to map
  if (window.MapAPI && typeof window.MapAPI.addPin === 'function') {
    window.MapAPI.addPin(pin);
    showToast(`New ${pin.type}: ${pin.name}`);
  } else {
    console.warn('websocket.js: MapAPI not ready yet');
  }
}

/* ───────────────────────────────────────────────
   INIT
   Only connect if we're on the map page.
─────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
  // map page has #map element
  if (document.getElementById('map')) {
    connectWebSocket();
  }
});