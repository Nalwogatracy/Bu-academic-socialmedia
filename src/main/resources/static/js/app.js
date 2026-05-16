(function() {
    'use strict';

    // Ctrl+K or / → focus search or go to /search
    document.addEventListener('keydown', function(e) {
        if ((e.ctrlKey && e.key === 'k') || (e.key === '/' && !['INPUT', 'TEXTAREA', 'SELECT'].includes(e.target.tagName))) {
            e.preventDefault();
            var searchInput = document.querySelector('.search-bar input[name="q"]');
            if (searchInput) {
                searchInput.focus();
            } else {
                window.location.href = '/search?q=';
            }
        }
    });
})();
