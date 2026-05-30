/* ═══════════════════════════════════════════════
   EXPERIMATE — push-setup.js
   Web Push subscribe / unsubscribe helpers.
   Loaded deferred on every page via topbar fragment.
   Depends on api.js (Auth, apiFetch).
═══════════════════════════════════════════════ */

function urlBase64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - base64String.length % 4) % 4);
  const base64  = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const raw     = atob(base64);
  return Uint8Array.from(raw, c => c.charCodeAt(0));
}

async function subscribeToPush() {
  if (!('serviceWorker' in navigator) || !('PushManager' in window)) return;
  try {
    const reg = await navigator.serviceWorker.ready;
    const { key } = await apiFetch('/api/push/vapid-public-key');
    const subscription = await reg.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToUint8Array(key),
    });
    const { endpoint, keys: { p256dh, auth } } = subscription.toJSON();
    await apiFetch('/api/push/subscribe', {
      method: 'POST',
      body: JSON.stringify({ endpoint, p256dh, auth }),
    });
    localStorage.setItem('push_subscribed', '1');
  } catch (_) { /* non-critical — silently ignore */ }
}

async function unsubscribeFromPush() {
  if (!('serviceWorker' in navigator) || !('PushManager' in window)) return;
  try {
    const reg = await navigator.serviceWorker.ready;
    const subscription = await reg.pushManager.getSubscription();
    if (!subscription) return;
    const { endpoint } = subscription.toJSON();
    const token = Auth.getToken();
    // POST with keepalive so the request survives the page unload on logout
    await fetch('/api/push/unsubscribe', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      body: JSON.stringify({ endpoint }),
      keepalive: true,
    }).catch(() => {});
    await subscription.unsubscribe();
    localStorage.removeItem('push_subscribed');
  } catch (_) { /* best-effort — never block logout */ }
}

// Already-logged-in check: runs after api.js (this script is deferred).
// Covers users who logged in before push was deployed.
(function () {
  if (typeof Auth === 'undefined' || !Auth.getToken() || Auth.isExpired()) return;
  if (typeof Notification === 'undefined') return;

  if (Notification.permission === 'default') {
    Notification.requestPermission().then(p => { if (p === 'granted') subscribeToPush(); });
  } else if (Notification.permission === 'granted' && !localStorage.getItem('push_subscribed')) {
    // Permission already granted (e.g. from a previous session) but no subscription on record
    subscribeToPush();
  }
})();
