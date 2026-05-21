/* ═══════════════════════════════════════════════
   EXPERIMATE — community.js
   Community page interactions.
   toggleJoin() lives in main.js.
═══════════════════════════════════════════════ */

document.addEventListener('DOMContentLoaded', () => {
  initCommSearch();
});

/* ───────────────────────────────────────────────
   SEARCH
   Filters community cards by name in real time.
─────────────────────────────────────────────── */
function initCommSearch() {
  const input = document.getElementById('comm-search-input');
  if (!input) return;

  let _t = null;
  input.addEventListener('input', (e) => {
    clearTimeout(_t);
    _t = setTimeout(() => {
      const query = e.target.value.trim().toLowerCase();
      document.querySelectorAll('.comm-card').forEach(card => {
        const name = card.querySelector('.comm-card__name')?.textContent?.toLowerCase() || '';
        card.style.display = (!query || name.includes(query)) ? '' : 'none';
      });
    }, 150);
  });
}