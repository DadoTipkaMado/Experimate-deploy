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
─────────────────────────────────────────────── */
function setCardHeights() {
  const feed = document.getElementById('explore-feed');
  if (!feed) return;
  const h = feed.clientHeight;
  document.querySelectorAll('.explore-card').forEach(card => {
    card.style.height = h + 'px';
  });
}

let _resizeTimer = null;
window.addEventListener('resize', () => {
  clearTimeout(_resizeTimer);
  _resizeTimer = setTimeout(setCardHeights, 150);
});

/* ───────────────────────────────────────────────
   SCROLL HINT
─────────────────────────────────────────────── */
function initScrollHint() {
  const feed     = document.getElementById('explore-feed');
  const firstCard = feed?.querySelector('.explore-card');
  if (!feed || !firstCard) return;

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

  let _hintTimer = setTimeout(() => {
    hint.classList.add('scroll-hint--hidden');
    feed.removeEventListener('scroll', hideHint);
  }, 5000);
  const hideHint = () => {
    clearTimeout(_hintTimer);
    hint.classList.add('scroll-hint--hidden');
    feed.removeEventListener('scroll', hideHint);
  };
  feed.addEventListener('scroll', hideHint, { passive: true });
}

/* ───────────────────────────────────────────────
   SEARCH
   Calls GET /api/match?q= — backend scores +
   AI-parses the query. Clears back to full list
   when input is empty.
─────────────────────────────────────────────── */
function initSearch() {
  const input = document.getElementById('explore-search-input');
  if (!input) return;

  let _searchTimer = null;

  input.addEventListener('input', (e) => {
    const query = e.target.value.trim();
    clearTimeout(_searchTimer);

    // Keep shared query state for explain calls
    window._currentQuery = query;

    if (!query) {
      if (typeof renderExploreSorted === 'function') renderExploreSorted();
      return;
    }

    showExploreLoading();

    _searchTimer = setTimeout(() => {
      UserAPI.search(query)
        .then(resp => {
          const users = resp?.searchResult ?? (Array.isArray(resp) ? resp : []);
          if (typeof renderExploreWithSort === 'function') renderExploreWithSort(users, query);
        })
        .catch(() => {
          if (typeof renderExploreWithSort === 'function') renderExploreWithSort([], query);
        });
    }, 300);
  });

  // Pre-fill from ?q= URL param (e.g. coming from map or profile)
  const urlQ = new URLSearchParams(window.location.search).get('q');
  if (urlQ) {
    input.value = urlQ;
    window._currentQuery = urlQ;
    input.dispatchEvent(new Event('input'));
  }
}

function showExploreLoading() {
  const feed = document.getElementById('explore-feed');
  if (!feed) return;
  feed.innerHTML = `
    <div class="explore-card" style="background:var(--surface);">
      <div style="position:absolute;inset:0;display:flex;flex-direction:column;justify-content:flex-end;padding:0 16px 20px;">
        <div class="skeleton skeleton-line skeleton-line--lg" style="width:55%;margin-bottom:8px;"></div>
        <div class="skeleton skeleton-line skeleton-line--sm" style="width:35%;margin-bottom:14px;"></div>
        <div class="skeleton skeleton-line skeleton-line--w-full" style="margin-bottom:6px;"></div>
        <div class="skeleton skeleton-line skeleton-line--w-3q" style="margin-bottom:6px;"></div>
        <div class="skeleton skeleton-line skeleton-line--w-half" style="margin-bottom:18px;"></div>
        <div class="skeleton skeleton-line" style="width:100%;height:44px;border-radius:12px;"></div>
      </div>
    </div>`;
  setCardHeights();
}
