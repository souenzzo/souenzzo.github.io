self.addEventListener('install', function (e) {
    e.waitUntil(
        caches.open('souenzzo.github.io/v1').then(function (cache) {
            return cache.addAll([
                '/',
                '/me.jpg',
                '/serviceWorker.js',
                'https://souenzzo.com.br/me192.png',
                'https://souenzzo.com.br/me512.png',
                '/manifest.webmanifest'
            ]);
        })
    );
});

self.addEventListener('fetch', function (event) {
    event.respondWith(
        caches.match(event.request).then(function (response) {
            return response || fetch(event.request);
        })
    );
});
