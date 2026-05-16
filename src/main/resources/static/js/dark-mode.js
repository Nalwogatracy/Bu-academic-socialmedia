(function() {
  'use strict';

  const STORAGE_KEY = 'bugema-dark-mode';
  const ATTR = 'data-theme';

  function getTheme() {
    return localStorage.getItem(STORAGE_KEY) || 'light';
  }

  function setTheme(theme) {
    document.documentElement.setAttribute(ATTR, theme);
    localStorage.setItem(STORAGE_KEY, theme);
    updateIcons(theme);
  }

  function toggleTheme() {
    const next = getTheme() === 'dark' ? 'light' : 'dark';
    setTheme(next);
  }

  function updateIcons(theme) {
    const isDark = theme === 'dark';
    document.querySelectorAll('.dark-toggle-icon').forEach(function(el) {
      if (isDark) {
        el.className = el.className.replace(/fa-moon|fa-sun/g, '');
        el.classList.add('fas', 'fa-sun');
      } else {
        el.className = el.className.replace(/fa-moon|fa-sun/g, '');
        el.classList.add('fas', 'fa-moon');
      }
    });
    document.querySelectorAll('.dark-mode-toggle .toggle-text').forEach(function(el) {
      el.textContent = isDark ? 'Light Mode' : 'Dark Mode';
    });
  }

  // Apply saved theme immediately (before DOM paints)
  var saved = getTheme();
  if (saved === 'dark') {
    document.documentElement.setAttribute(ATTR, 'dark');
  }

  // After DOM ready, wire up toggles
  document.addEventListener('DOMContentLoaded', function() {
    updateIcons(getTheme());

    document.querySelectorAll('.dark-mode-toggle').forEach(function(el) {
      el.addEventListener('click', toggleTheme);
    });
    document.querySelectorAll('.nav-dark-toggle').forEach(function(el) {
      el.addEventListener('click', toggleTheme);
    });
  });
})();
