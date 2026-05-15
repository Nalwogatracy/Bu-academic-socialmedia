// offline.js — Bugema Connect Offline Manager
// Place this at: src/main/resources/static/js/offline.js
// Include in ALL your pages: <script src="/js/offline.js"></script>

(function () {
    'use strict';

    // ─── 1. Register the Service Worker ──────────────────────────────────────
    if ('serviceWorker' in navigator) {
        window.addEventListener('load', () => {
            navigator.serviceWorker.register('/sw.js', { scope: '/' })
                .then(reg => {
                    console.log('[SW] Registered, scope:', reg.scope);
                    // Listen for messages from the SW (sync events)
                    navigator.serviceWorker.addEventListener('message', onSWMessage);
                })
                .catch(err => console.error('[SW] Registration failed:', err));
        });
    }

    // ─── 2. Offline/Online detection ─────────────────────────────────────────
    let offlineBanner = null;

    function createOfflineBanner() {
        const banner = document.createElement('div');
        banner.id = 'offline-banner';
        banner.innerHTML = `
            <span>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
                     stroke="currentColor" stroke-width="2" style="vertical-align:middle;margin-right:6px">
                    <line x1="1" y1="1" x2="23" y2="23"/>
                    <path d="M16.72 11.06A10.94 10.94 0 0 1 19 12.55"/>
                    <path d="M5 12.55a10.94 10.94 0 0 1 5.17-2.39"/>
                    <path d="M10.71 5.05A16 16 0 0 1 22.56 9"/>
                    <path d="M1.42 9a15.91 15.91 0 0 1 4.7-2.88"/>
                    <path d="M8.53 16.11a6 6 0 0 1 6.95 0"/>
                    <line x1="12" y1="20" x2="12.01" y2="20"/>
                </svg>
                You're offline — messages and submissions will sync when you reconnect
            </span>
            <span id="offline-queue-count" style="
                background: rgba(255,255,255,0.2);
                padding: 2px 8px;
                border-radius: 12px;
                font-size: 0.75rem;
            "></span>
        `;
        banner.style.cssText = `
            position: fixed;
            top: 64px;
            left: 0;
            right: 0;
            background: #1a2634;
            color: white;
            padding: 10px 24px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            z-index: 999;
            font-size: 0.88rem;
            font-family: 'Inter', sans-serif;
            font-weight: 500;
            transform: translateY(-100%);
            transition: transform 0.3s ease;
        `;
        document.body.appendChild(banner);
        return banner;
    }

    function showOfflineBanner() {
        if (!offlineBanner) offlineBanner = createOfflineBanner();
        requestAnimationFrame(() => {
            offlineBanner.style.transform = 'translateY(0)';
        });
        updateQueueCount();
    }

    function hideOfflineBanner() {
        if (offlineBanner) {
            offlineBanner.style.transform = 'translateY(-100%)';
        }
    }

    async function updateQueueCount() {
        const count = await getPendingCount();
        const el = document.getElementById('offline-queue-count');
        if (el && count > 0) {
            el.textContent = `${count} pending`;
        } else if (el) {
            el.textContent = '';
        }
    }

    window.addEventListener('online', () => {
        hideOfflineBanner();
        showSyncToast();
    });

    window.addEventListener('offline', () => {
        showOfflineBanner();
    });

    // Show banner immediately if already offline
    if (!navigator.onLine) {
        document.addEventListener('DOMContentLoaded', showOfflineBanner);
    }

    // ─── 3. Intercept message form for optimistic UI ──────────────────────────
    document.addEventListener('DOMContentLoaded', () => {
        const messageForm = document.getElementById('messageForm');
        if (messageForm) {
            // Override the existing submit handler for offline support
            messageForm.addEventListener('submit', async function (e) {
                if (navigator.onLine) return; // let normal submit happen

                e.preventDefault();
                e.stopImmediatePropagation();

                const input = document.getElementById('messageInput');
                const content = input?.value?.trim();
                if (!content) return;

                const recipientIdEl = messageForm.querySelector('[name="recipientId"]');
                const recipientId = recipientIdEl?.value;

                // Show the message optimistically in the UI
                addOptimisticMessage(content);
                input.value = '';
                document.getElementById('sendButton').disabled = true;

                // Save to IndexedDB outbox
                await saveMessageToOutbox(recipientId, content, messageForm.action);

                showToastIfAvailable('Message queued', 'Will be sent when you reconnect', 'info');
                updateQueueCount();
            }, true); // capture phase so it runs before existing listeners
        }

        // Override assignment submit for offline
        const subForm = document.getElementById('submissionForm');
        if (subForm) {
            subForm.addEventListener('submit', async function (e) {
                if (navigator.onLine) return;

                e.preventDefault();
                e.stopImmediatePropagation();

                const textAnswer = subForm.querySelector('[name="textAnswer"]')?.value || '';
                const fileInput = document.getElementById('submissionFile');
                const file = fileInput?.files?.[0] || null;

                if (!textAnswer && !file) {
                    alert('Please provide a written answer or upload a file.');
                    return;
                }

                await saveAssignmentToOutbox(subForm.action, textAnswer, file);

                // Show success state
                subForm.style.display = 'none';
                const successDiv = document.createElement('div');
                successDiv.style.cssText = `
                    background: #e8f5e9; border: 1px solid #a5d6a7;
                    border-radius: 12px; padding: 32px; text-align: center;
                    font-family: 'Inter', sans-serif;
                `;
                successDiv.innerHTML = `
                    <div style="font-size:2.5rem;margin-bottom:12px">📥</div>
                    <h3 style="color:#1b5e20;margin-bottom:8px">Submission saved offline</h3>
                    <p style="color:#2e7d32">It will be automatically submitted when you reconnect to the internet.</p>
                `;
                subForm.parentNode.insertBefore(successDiv, subForm.nextSibling);

                updateQueueCount();
            }, true);
        }
    });

    // ─── 4. SW message handler (sync complete notifications) ─────────────────
    function onSWMessage(event) {
        const { type, item } = event.data || {};

        if (type === 'MESSAGE_SYNCED') {
            // Reload the messages container if on the right page
            const container = document.getElementById('messagesContainer');
            if (container && window.refreshMessages) {
                window.refreshMessages();
            }
            showToastIfAvailable('Message sent', 'Your queued message was delivered', 'success');
        }

        if (type === 'ASSIGNMENT_SYNCED') {
            showToastIfAvailable('Assignment submitted', 'Your offline submission was uploaded', 'success');
            // Reload after a short delay so the page reflects the new state
            setTimeout(() => window.location.reload(), 2000);
        }
    }

    // ─── 5. Optimistic message UI ─────────────────────────────────────────────
    function addOptimisticMessage(content) {
        const container = document.getElementById('messagesContainer');
        if (!container) return;

        const now = new Date();
        const time = now.getHours().toString().padStart(2, '0') + ':' +
                     now.getMinutes().toString().padStart(2, '0');

        const wrapper = document.createElement('div');
        wrapper.className = 'message-wrapper sent';
        wrapper.style.opacity = '0.65'; // dim to show it's pending
        wrapper.dataset.pending = 'true';
        wrapper.innerHTML = `
            <div class="message-content">
                <div class="message-text">${escapeHtml(content)}</div>
                <div class="message-meta">
                    <span class="message-time">${time}</span>
                    <span class="message-status" title="Pending (offline)">
                        <i class="fas fa-clock" style="color:rgba(255,255,255,0.6);font-size:0.65rem;"></i>
                    </span>
                </div>
            </div>
        `;
        container.appendChild(wrapper);
        container.scrollTop = container.scrollHeight;
    }

    // ─── 6. IndexedDB helpers (client-side) ───────────────────────────────────
    function openDB() {
        return new Promise((resolve, reject) => {
            const req = indexedDB.open('BugemaConnectDB', 2);
            req.onupgradeneeded = e => {
                const db = e.target.result;
                ['messageOutbox', 'assignmentOutbox', 'cachedMessages'].forEach(name => {
                    if (!db.objectStoreNames.contains(name)) {
                        db.createObjectStore(name, { keyPath: 'id' });
                    }
                });
            };
            req.onsuccess = e => resolve(e.target.result);
            req.onerror = () => reject(req.error);
        });
    }

    async function saveMessageToOutbox(recipientId, content, url) {
        const db = await openDB();
        return new Promise((resolve, reject) => {
            const entry = {
                id: Date.now(),
                timestamp: new Date().toISOString(),
                url,
                recipientId,
                content,
                synced: false
            };
            const tx = db.transaction('messageOutbox', 'readwrite');
            const req = tx.objectStore('messageOutbox').put(entry);
            req.onsuccess = () => resolve();
            req.onerror = () => reject(req.error);
        });
    }

    async function saveAssignmentToOutbox(url, textAnswer, file) {
        const db = await openDB();
        let fileData = null;
        if (file) {
            fileData = {
                name: file.name,
                type: file.type,
                size: file.size,
                buffer: await file.arrayBuffer()
            };
        }
        return new Promise((resolve, reject) => {
            const entry = {
                id: Date.now(),
                timestamp: new Date().toISOString(),
                url,
                textAnswer,
                fileData,
                synced: false
            };
            const tx = db.transaction('assignmentOutbox', 'readwrite');
            const req = tx.objectStore('assignmentOutbox').put(entry);
            req.onsuccess = () => resolve();
            req.onerror = () => reject(req.error);
        });
    }

    async function getPendingCount() {
        try {
            const db = await openDB();
            const counts = await Promise.all(['messageOutbox', 'assignmentOutbox'].map(store =>
                new Promise((resolve) => {
                    const tx = db.transaction(store, 'readonly');
                    const req = tx.objectStore(store).getAll();
                    req.onsuccess = () => resolve((req.result || []).filter(i => !i.synced).length);
                    req.onerror = () => resolve(0);
                })
            ));
            return counts.reduce((a, b) => a + b, 0);
        } catch { return 0; }
    }

    // ─── 7. Helpers ───────────────────────────────────────────────────────────
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function showToastIfAvailable(title, message, type) {
        if (typeof window.showToast === 'function') {
            window.showToast(title, message, type);
        }
    }

    function showSyncToast() {
        showToastIfAvailable(
            'Back online',
            'Syncing your queued messages and submissions…',
            'success'
        );
    }

})();