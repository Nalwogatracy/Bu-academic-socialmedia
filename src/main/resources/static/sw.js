// sw.js — Bugema Connect Service Worker
// Place this file at: src/main/resources/static/sw.js

const CACHE_NAME = 'bugema-connect-v1';
const STATIC_ASSETS = [
    '/',
    '/login',
    '/register',
    '/offline.html',
    '/css/all.min.css',
    '/css/inter.css',
    '/css/bootstrap.min.css',
    '/css/bootstrap-icons.css',
    '/css/flatpickr.min.css',
    '/js/app.js',
    '/js/chart.umd.min.js',
    '/js/flatpickr.min.js',
    '/js/bootstrap.bundle.min.js',
    '/js/offline.js'
];

// ─── Install: cache static assets ────────────────────────────────────────────
self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then(cache => cache.addAll(STATIC_ASSETS).catch(() => {}))
            .then(() => self.skipWaiting())
    );
});

// ─── Activate: clean up old caches ───────────────────────────────────────────
self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(keys =>
            Promise.all(keys
                .filter(k => k !== CACHE_NAME)
                .map(k => caches.delete(k))
            )
        ).then(() => self.clients.claim())
    );
});

// ─── Fetch: intercept all requests ───────────────────────────────────────────
self.addEventListener('fetch', event => {
    const url = new URL(event.request.url);
    const method = event.request.method;

    // Let SSE pass through always (no caching for real-time stream)
    if (url.pathname.startsWith('/sse/')) {
        return;
    }

    // POST: message send or assignment submit — queue if offline
    if (method === 'POST') {
        event.respondWith(handlePost(event.request.clone()));
        return;
    }

    // GET: try network first, fall back to cache
    if (method === 'GET') {
        event.respondWith(handleGet(event.request));
        return;
    }
});

// ─── Background Sync: flush outbox when online ───────────────────────────────
self.addEventListener('sync', event => {
    if (event.tag === 'sync-messages') {
        event.waitUntil(flushMessageOutbox());
    }
    if (event.tag === 'sync-assignments') {
        event.waitUntil(flushAssignmentOutbox());
    }
});

// ─── GET handler: Network-first with cache fallback ──────────────────────────
async function handleGet(request) {
    const url = new URL(request.url);

    // For message/assignment pages: try network, cache response, serve cache on fail
    const isAppPage = url.pathname.startsWith('/student/') ||
                      url.pathname.startsWith('/lecturer/') ||
                      url.pathname.startsWith('/admin/') ||
                      url.pathname.startsWith('/user/');

    try {
        const networkResponse = await fetch(request);
        if (networkResponse.ok && isAppPage) {
            const cache = await caches.open(CACHE_NAME);
            cache.put(request, networkResponse.clone());
        }
        return networkResponse;
    } catch {
        // Network failed — try cache
        const cached = await caches.match(request);
        if (cached) return cached;

        // For navigation requests, show offline page
        if (request.mode === 'navigate') {
            return caches.match('/offline.html') ||
                new Response('<h1>You are offline</h1>', {
                    headers: { 'Content-Type': 'text/html' }
                });
        }
        return new Response('Offline', { status: 503 });
    }
}

// ─── POST handler: try network, queue to IndexedDB on failure ────────────────
async function handlePost(request) {
    const url = new URL(request.url);

    try {
        const response = await fetch(request);
        return response;
    } catch {
        // Offline — save to IndexedDB outbox
        if (url.pathname.includes('/messages/send')) {
            await queueMessage(request);
            // Register background sync
            self.registration.sync.register('sync-messages').catch(() => {});
            // Return a fake success so the UI can show optimistic message
            return new Response(JSON.stringify({
                queued: true,
                message: 'Message saved and will be sent when you reconnect'
            }), {
                status: 202,
                headers: { 'Content-Type': 'application/json' }
            });
        }

        if (url.pathname.includes('/submit')) {
            await queueAssignment(request);
            self.registration.sync.register('sync-assignments').catch(() => {});
            return new Response(JSON.stringify({
                queued: true,
                message: 'Submission saved and will be uploaded when you reconnect'
            }), {
                status: 202,
                headers: { 'Content-Type': 'application/json' }
            });
        }

        return new Response('Offline', { status: 503 });
    }
}

// ─── Queue a message to IndexedDB ────────────────────────────────────────────
async function queueMessage(request) {
    const formData = await request.formData().catch(() => null);
    if (!formData) return;

    const entry = {
        id: Date.now(),
        timestamp: new Date().toISOString(),
        url: request.url,
        recipientId: formData.get('recipientId'),
        content: formData.get('content'),
        synced: false
    };

    return dbSet('messageOutbox', entry.id, entry);
}

// ─── Queue an assignment submission to IndexedDB ──────────────────────────────
async function queueAssignment(request) {
    const formData = await request.formData().catch(() => null);
    if (!formData) return;

    // Note: files are stored as ArrayBuffer since IndexedDB can't store File objects
    const fileField = formData.get('file');
    let fileData = null;
    if (fileField && fileField.size > 0) {
        fileData = {
            name: fileField.name,
            type: fileField.type,
            size: fileField.size,
            buffer: await fileField.arrayBuffer()
        };
    }

    const entry = {
        id: Date.now(),
        timestamp: new Date().toISOString(),
        url: request.url,
        textAnswer: formData.get('textAnswer') || '',
        fileData,
        synced: false
    };

    return dbSet('assignmentOutbox', entry.id, entry);
}

// ─── Flush message outbox when connection restored ────────────────────────────
async function flushMessageOutbox() {
    const items = await dbGetAll('messageOutbox');
    for (const item of items) {
        if (item.synced) continue;
        try {
            const body = new FormData();
            body.append('recipientId', item.recipientId);
            body.append('content', item.content);

            const response = await fetch(item.url, { method: 'POST', body });
            if (response.ok || response.redirected) {
                item.synced = true;
                await dbSet('messageOutbox', item.id, item);
                // Notify the page
                notifyClients({ type: 'MESSAGE_SYNCED', item });
            }
        } catch (e) {
            // Still offline, try later
        }
    }
}

// ─── Flush assignment outbox when connection restored ─────────────────────────
async function flushAssignmentOutbox() {
    const items = await dbGetAll('assignmentOutbox');
    for (const item of items) {
        if (item.synced) continue;
        try {
            const body = new FormData();
            body.append('textAnswer', item.textAnswer);
            if (item.fileData) {
                const blob = new Blob([item.fileData.buffer], { type: item.fileData.type });
                body.append('file', blob, item.fileData.name);
            }

            const response = await fetch(item.url, { method: 'POST', body });
            if (response.ok || response.redirected) {
                item.synced = true;
                await dbSet('assignmentOutbox', item.id, item);
                notifyClients({ type: 'ASSIGNMENT_SYNCED', item });
            }
        } catch (e) {
            // Still offline
        }
    }
}

// ─── Notify all open pages ────────────────────────────────────────────────────
async function notifyClients(data) {
    const clients = await self.clients.matchAll({ includeUncontrolled: true });
    clients.forEach(client => client.postMessage(data));
}

// ─── IndexedDB helpers (no library needed) ───────────────────────────────────
function openDB() {
    return new Promise((resolve, reject) => {
        const req = indexedDB.open('BugemaConnectDB', 2);
        req.onupgradeneeded = e => {
            const db = e.target.result;
            if (!db.objectStoreNames.contains('messageOutbox')) {
                db.createObjectStore('messageOutbox', { keyPath: 'id' });
            }
            if (!db.objectStoreNames.contains('assignmentOutbox')) {
                db.createObjectStore('assignmentOutbox', { keyPath: 'id' });
            }
            if (!db.objectStoreNames.contains('cachedMessages')) {
                const store = db.createObjectStore('cachedMessages', { keyPath: 'id' });
                store.createIndex('recipientId', 'recipientId', { unique: false });
            }
        };
        req.onsuccess = e => resolve(e.target.result);
        req.onerror = () => reject(req.error);
    });
}

async function dbSet(storeName, key, value) {
    const db = await openDB();
    return new Promise((resolve, reject) => {
        const tx = db.transaction(storeName, 'readwrite');
        const req = tx.objectStore(storeName).put(value);
        req.onsuccess = () => resolve();
        req.onerror = () => reject(req.error);
    });
}

async function dbGetAll(storeName) {
    const db = await openDB();
    return new Promise((resolve, reject) => {
        const tx = db.transaction(storeName, 'readonly');
        const req = tx.objectStore(storeName).getAll();
        req.onsuccess = () => resolve(req.result || []);
        req.onerror = () => reject(req.error);
    });
}