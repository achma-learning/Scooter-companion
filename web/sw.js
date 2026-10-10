// Offline support. The app shell + knowledge base are pre-cached; PDFs, covers and the
// pdf.js viewer are cached the first time they are opened. VERSION is stamped at build time
// (scripts/build-web.sh) so every deploy refreshes the shell.
const VERSION = '__BUILD__';
const SHELL = `shell-${VERSION}`;
const RUNTIME = 'runtime-v1';
const PRECACHE = [
  './', 'index.html', 'css/app.css', 'manifest.webmanifest', 'data/agility50.json',
  'js/app.js', 'js/store.js', 'js/planner.js', 'js/ui.js', 'js/icons.js', 'js/config.js',
  'js/views-garage.js', 'js/views-guide.js', 'js/views-manual.js',
  'img/icon.svg', 'img/icon-192.png', 'img/icon-512.png',
  'docs/toc.json', 'docs/manual_index.json', 'docs/passport_index.json', 'docs/cover_manual.jpg', 'docs/cover_passport.jpg',
];

self.addEventListener('install', (e) => {
  e.waitUntil(caches.open(SHELL).then((c) => c.addAll(PRECACHE)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k.startsWith('shell-') && k !== SHELL).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (e) => {
  const req = e.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  const sameOrigin = url.origin === location.origin;
  const isPdfJs = url.hostname === 'cdnjs.cloudflare.com' && url.pathname.includes('/pdf.js/');
  if (!sameOrigin && !isPdfJs) return;
  // pdf.js asks for byte ranges; serve the whole cached file and let it cope.
  e.respondWith(
    caches.match(req, { ignoreSearch: true }).then((hit) => {
      if (hit) return hit;
      return fetch(req).then((res) => {
        const cacheable = res.ok && res.status === 200 && (isPdfJs || /\/docs\/|\.(pdf|jpg|png)$/.test(url.pathname));
        if (cacheable) {
          const copy = res.clone();
          caches.open(RUNTIME).then((c) => c.put(req, copy));
        }
        return res;
      }).catch(() => (req.mode === 'navigate' ? caches.match('index.html') : Response.error()));
    })
  );
});
