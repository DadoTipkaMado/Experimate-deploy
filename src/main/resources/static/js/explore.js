/* ═══════════════════════════════════════════════
   EXPERIMATE — explore.js
   TikTok snap-scroll feed logic.
═══════════════════════════════════════════════ */

document.addEventListener('DOMContentLoaded', () => {
  setCardHeights();
  initScrollHint();
  initSearch();
});

/* ───────────────────────────────────────────────
   CARD HEIGHTS
   Each card must fill exactly the feed container
   height so snap-scroll works correctly.
─────────────────────────────────────────────── */
function setCardHeights() {
  const feed = document.getElementById('explore-feed');
  if (!feed) return;

  const h = feed.clientHeight;
  document.querySelectorAll('.explore-card').forEach(card => {
    card.style.height = h + 'px';
  });
}

// Recalculate on resize (e.g. mobile keyboard opens)
window.addEventListener('resize', setCardHeights);

/* ───────────────────────────────────────────────
   SCROLL HINT
   Show "swipe up" hint on first card,
   hide it after user scrolls once.
─────────────────────────────────────────────── */
function initScrollHint() {
  const feed = document.getElementById('explore-feed');
  const firstCard = feed?.querySelector('.explore-card');
  if (!feed || !firstCard) return;

  // Inject hint into first card
  const hint = document.createElement('div');
  hint.className = 'scroll-hint';
  hint.innerHTML = `
    <div class="scroll-hint__arrow">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
           stroke="rgba(239,239,239,0.35)" stroke-width="2" stroke-linecap="round">
        <line x1="12" y1="19" x2="12" y2="5"/>
        <polyline points="5 12 12 5 19 12"/>
      </svg>
    </div>
    <div class="scroll-hint__label">Swipe up</div>
  `;
  firstCard.appendChild(hint);

  // Hide after first scroll
  const hideHint = () => {
    hint.classList.add('scroll-hint--hidden');
    feed.removeEventListener('scroll', hideHint);
  };
  feed.addEventListener('scroll', hideHint, { passive: true });
}

/* ───────────────────────────────────────────────
   SEARCH
   Hides cards that don't match query.
   Works on placeholder AND real Thymeleaf data.
─────────────────────────────────────────────── */
function initSearch() {
  const input = document.getElementById('explore-search-input');
  if (!input) return;

  input.addEventListener('input', (e) => {
    const query = e.target.value.trim().toLowerCase();
    const cards = document.querySelectorAll('.explore-card');

    cards.forEach(card => {
      if (!query) {
        card.style.display = '';
        return;
      }
      // Search in name, city, tags, desc
      const text = card.textContent.toLowerCase();
      card.style.display = text.includes(query) ? '' : 'none';
    });

    // Recalculate heights after filter
    setCardHeights();
  });
}

/* ───────────────────────────────────────────────
   SAVE LOCAL
   Called from th:onclick in explore.html.
   Frend can hook this up to a real API call.

   Usage: saveLocal(localId, localName)
─────────────────────────────────────────────── */
function saveLocal(localId, localName) {
  // TODO: POST /api/saved-locals when backend ready
  showToast(`Saved ${localName}`);
}

/* ───────────────────────────────────────────────
   VIEW ON MAP
   Navigates to map page and centers on local's pin.
   Passes coords via sessionStorage so map.js picks them up.

   Usage: viewOnMap(lat, lng)
─────────────────────────────────────────────── */
function viewOnMap(lat, lng) {
  if (!lat || !lng) {
    showToast('Location not available');
    return;
  }
  // Store target coords for map.js to read on load
  sessionStorage.setItem('mapFlyTo', JSON.stringify({ lat, lng, zoom: 16 }));
  window.location.href = '/map';
}