// Service Worker Desativado e Auto-Limpante
// Limpa qualquer cache e se desregistra imediatamente do navegador
self.addEventListener('install', (event) => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(keys.map((key) => caches.delete(key)));
    }).then(() => self.clients.claim()).then(() => {
      // Notifica as abas para recarregarem se necessário
      return self.clients.matchAll().then((clients) => {
        clients.forEach((client) => client.navigate(client.url));
      });
    })
  );
});

// Em fetch, não intercepta absolutamente nada, deixa o navegador buscar direto da rede
self.addEventListener('fetch', (event) => {
  return;
});
