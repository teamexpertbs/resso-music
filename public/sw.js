// Resso Service Worker for PWA Installation & Fast Audio Caching
const CACHE_NAME = 'resso-pwa-v1';

self.addEventListener('install', (event) => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', (event) => {
  // Let network handle audio streams directly to avoid range request caching issues
  if (event.request.url.includes('stream') || event.request.url.includes('audius.co') || event.request.url.includes('apple.com')) {
    return;
  }
  
  event.respondWith(
    fetch(event.request).catch(() => {
      return caches.match(event.request);
    })
  );
});
