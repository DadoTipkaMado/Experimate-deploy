const CACHE = 'experimate-v9';

const STATIC = [
  '/css/main.css',
  '/js/api.js',
  '/js/main.js',
  '/js/explore.js',
  '/js/map.js',
  '/js/tour.js',
  '/js/community.js',
  '/favicon.svg',
  '/icons/icon.svg',
  '/manifest.json',
];

self.addEventListener('install', e => {
  e.waitUntil(
    caches.open(CACHE).then(c => c.addAll(STATIC)).then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', e => {
  const { request } = e;
  const url = new URL(request.url);

  // Always network for API calls, auth, and Thymeleaf pages
  if (url.pathname.startsWith('/api/') || request.method !== 'GET') return;

  // Cache-first for static assets
  if (url.pathname.startsWith('/css/') || url.pathname.startsWith('/js/') || url.pathname.startsWith('/icons/')) {
    e.respondWith(
      caches.match(request).then(cached => cached || fetch(request).then(res => {
        const clone = res.clone();
        caches.open(CACHE).then(c => c.put(request, clone));
        return res;
      }))
    );
    return;
  }

  // Network-first for everything else (HTML pages, fonts, etc.)
  e.respondWith(
    fetch(request).catch(() => caches.match(request))
  );
});
