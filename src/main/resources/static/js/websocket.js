/* ═══════════════════════════════════════════════
   EXPERIMATE — websocket.js
   Live map pin updates via WebSocket.
   Depends on: map.js (window.MapAPI must exist)
   Endpoint /ws/map — backend not yet implemented.
   Client reconnects with exponential backoff until
   the endpoint exists.
═══════════════════════════════════════════════ */

const WS_URL      = '/ws/map';
const BACKOFF_MAX = 30000;

let _ws           = null;
let _retryDelay   = 2000;
let _retryTimer   = null;
let _intentional  = false;

function connectWebSocket() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const fullUrl  = `${protocol}//${window.location.host}${WS_URL}`;

  try {
    _ws = new WebSocket(fullUrl);
  } catch (e) {
    scheduleReconnect();
    return;
  }

  _ws.onopen    = () => { _retryDelay = 2000; };
  _ws.onmessage = (event) => { handleMessage(event.data); };
  _ws.onerror   = () => {};
  _ws.onclose   = () => { if (!_intentional) scheduleReconnect(); };
}

function scheduleReconnect() {
  clearTimeout(_retryTimer);
  _retryTimer  = setTimeout(connectWebSocket, _retryDelay);
  _retryDelay  = Math.min(_retryDelay * 2, BACKOFF_MAX);
}

function disconnectWebSocket() {
  _intentional = true;
  clearTimeout(_retryTimer);
  if (_ws) { _ws.close(); _ws = null; }
}

function handleMessage(raw) {
  let pin;
  try {
    pin = JSON.parse(raw);
  } catch (e) {
    return;
  }
  if (!pin.lat || !pin.lng || !pin.name || !pin.type) return;
  if (window.MapAPI && typeof window.MapAPI.addPin === 'function') {
    window.MapAPI.addPin(pin);
    showToast(`New ${pin.type}: ${pin.name}`);
  }
}

document.addEventListener('DOMContentLoaded', () => {
  if (document.getElementById('map')) connectWebSocket();
});

document.addEventListener('visibilitychange', () => {
  if (document.hidden) {
    disconnectWebSocket();
  } else {
    _intentional = false;
    connectWebSocket();
  }
});
