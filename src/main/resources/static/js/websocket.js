/* ═══════════════════════════════════════════════
   EXPERIMATE — websocket.js
   Live map pin updates via WebSocket.
   Depends on: map.js (window.MapAPI must exist)
   Endpoint /ws/map not yet implemented — connect
   attempt is made but onclose does not retry.
═══════════════════════════════════════════════ */

const WS_URL = '/ws/map';

let _ws = null;
let _reconnectTimer = null;
let _reconnectDelay = 3000;

function connectWebSocket() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const fullUrl  = `${protocol}//${window.location.host}${WS_URL}`;

  try {
    _ws = new WebSocket(fullUrl);
  } catch (e) {
    scheduleReconnect();
    return;
  }

  _ws.onopen    = () => { _reconnectDelay = 3000; };
  _ws.onmessage = (event) => { handleMessage(event.data); };
  _ws.onerror   = () => {};
  _ws.onclose   = () => { /* endpoint not yet implemented — don't retry */ };
}

function scheduleReconnect() {
  clearTimeout(_reconnectTimer);
  _reconnectTimer = setTimeout(() => {
    _reconnectDelay = Math.min(_reconnectDelay * 2, 30000);
    connectWebSocket();
  }, _reconnectDelay);
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
