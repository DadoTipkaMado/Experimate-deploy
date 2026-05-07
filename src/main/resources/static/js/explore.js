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
   AI MATCH PANEL
─────────────────────────────────────────────── */
let _matchQuery = '';
let _aiActive   = false;

function toggleAiSearch(pill) {
  _aiActive = !_aiActive;
  pill.classList.toggle('pill--active', _aiActive);
  if (!_aiActive) closeMatchPanel();
}

function openMatchPanel(matches, query) {
  _matchQuery = query;
  document.getElementById('match-panel-title').textContent = matches.length
    ? `${matches.length} match${matches.length !== 1 ? 'es' : ''} for "${query}"`
    : `No matches for "${query}"`;

  document.getElementById('match-panel-list').innerHTML = matches.length
    ? matches.map(renderMatchCard).join('')
    : `<div style="text-align:center;padding:36px 16px;color:var(--text-3);">
         <div style="font-size:28px;margin-bottom:12px;">🔍</div>
         <div style="font-size:12px;line-height:1.65;">No matching profiles found.<br>Try different keywords.</div>
       </div>`;

  document.getElementById('match-panel').classList.add('match-panel--open');
  document.getElementById('match-panel-backdrop').classList.add('match-panel-backdrop--visible');
}

function showMatchPanelLoading() {
  document.getElementById('match-panel-title').textContent = 'Searching…';
  document.getElementById('match-panel-list').innerHTML = [1, 2, 3].map(() => `
    <div class="match-card">
      <div class="match-card__avatar" style="background:var(--surface-2);"></div>
      <div class="match-card__body">
        <div class="skeleton" style="height:13px;border-radius:4px;width:55%;margin-bottom:6px;"></div>
        <div class="skeleton" style="height:10px;border-radius:4px;width:38%;margin-bottom:10px;"></div>
        <div class="skeleton" style="height:10px;border-radius:4px;width:92%;margin-bottom:5px;"></div>
        <div class="skeleton" style="height:10px;border-radius:4px;width:68%;"></div>
      </div>
    </div>`).join('');
  document.getElementById('match-panel').classList.add('match-panel--open');
  document.getElementById('match-panel-backdrop').classList.add('match-panel-backdrop--visible');
}

function closeMatchPanel() {
  document.getElementById('match-panel').classList.remove('match-panel--open');
  document.getElementById('match-panel-backdrop').classList.remove('match-panel-backdrop--visible');
}

function renderMatchCard(m) {
  const initials = ((m.firstName?.[0] ?? '') + (m.lastName?.[0] ?? '')).toUpperCase() || '?';
  const hue      = (m.username ?? '').split('').reduce((a, c) => a + c.charCodeAt(0), 0) % 360;
  const photoUrl = UserAPI.photoUrl(m.profilePhotoUrl);

  const avatarHtml = photoUrl
    ? `<img class="match-card__avatar" src="${photoUrl}" alt="${escapeHtml(m.firstName)}">`
    : `<div class="match-card__avatar" style="background:hsl(${hue},35%,20%);border:1.5px solid hsl(${hue},40%,30%);">
         <span style="font-family:var(--font-display);font-weight:800;font-size:16px;color:hsl(${hue},60%,72%);">${initials}</span>
       </div>`;

  const pctHtml  = m.compatibilityScore != null
    ? `<div class="match-card__pct">${m.compatibilityScore}% match</div>` : '';
  const cityHtml = m.activeListing
    ? `<div class="match-card__city">📍 ${escapeHtml(m.activeListing.city)}</div>` : '';
  const ctaHref  = m.activeListing ? `/tours?listing=${m.activeListing.id}` : `/profile/${m.username}`;
  const ctaLabel = m.activeListing ? 'View Day' : 'View Profile';
  const sparkle  = `<svg width="10" height="10" viewBox="0 0 24 24" fill="currentColor"><path d="M12 3c-1 3.5-3.5 6-7 7 3.5 1 6 3.5 7 7 1-3.5 3.5-6 7-7-3.5-1-6-3.5-7-7z"/></svg>`;
  const explainBtn = m.compatibilityScore != null
    ? `<button class="match-card__explain-btn" onclick="toggleExplain(${m.userId}, this)">${sparkle} Why we match</button>` : '';

  return `
    <div class="match-card">
      ${avatarHtml}
      <div class="match-card__body">
        <div class="match-card__top">
          <div style="min-width:0;">
            <div class="match-card__name">${escapeHtml(m.firstName)} ${escapeHtml(m.lastName)}</div>
            <div class="match-card__handle">@${escapeHtml(m.username)}</div>
          </div>
          ${pctHtml}
        </div>
        ${cityHtml}
        <div class="match-card__bio">${escapeHtml(m.bio ?? 'No bio yet.')}</div>
        <div class="match-card__actions">
          ${explainBtn}
          <a href="${ctaHref}" class="btn btn--primary" style="height:30px;padding:0 14px;font-size:11px;">${ctaLabel}</a>
        </div>
        <div class="match-card__explain-area" id="explain-area-${m.userId}">
          <div class="match-card__explain-text" id="explain-text-${m.userId}"></div>
        </div>
      </div>
    </div>`;
}

async function toggleExplain(userId, btn) {
  const area   = document.getElementById(`explain-area-${userId}`);
  const textEl = document.getElementById(`explain-text-${userId}`);
  const isOpen = area.classList.toggle('match-card__explain-area--open');
  const sparkle = `<svg width="10" height="10" viewBox="0 0 24 24" fill="currentColor"><path d="M12 3c-1 3.5-3.5 6-7 7 3.5 1 6 3.5 7 7 1-3.5 3.5-6 7-7-3.5-1-6-3.5-7-7z"/></svg>`;
  btn.innerHTML = isOpen ? `${sparkle} Hide` : `${sparkle} Why we match`;
  if (!isOpen || textEl.dataset.loaded) return;

  textEl.innerHTML = `
    <div class="skeleton" style="height:12px;border-radius:4px;margin-bottom:6px;width:90%"></div>
    <div class="skeleton" style="height:12px;border-radius:4px;margin-bottom:6px;width:75%"></div>
    <div class="skeleton" style="height:12px;border-radius:4px;width:55%"></div>`;

  try {
    const res = await MatchAPI.explainMatch(userId, _matchQuery || null);
    textEl.textContent = res.explanation || 'No explanation available.';
    textEl.dataset.loaded = '1';
  } catch {
    textEl.textContent = 'Could not load explanation — try again.';
  }
}

/* ───────────────────────────────────────────────
   SEARCH
─────────────────────────────────────────────── */
function initSearch() {
  const input = document.getElementById('explore-search-input');
  if (!input) return;

  function runSearch(query) {
    window._currentQuery = query;
    if (!query) {
      closeMatchPanel();
      if (typeof renderExploreSorted === 'function') renderExploreSorted();
      return;
    }
    if (_aiActive) {
      showMatchPanelLoading();
      MatchAPI.findMatches(query)
        .then(matches => openMatchPanel(matches, query))
        .catch(() => openMatchPanel([], query));
    } else {
      showExploreLoading();
      UserAPI.search(query)
        .then(resp => {
          const users = resp?.searchResult ?? (Array.isArray(resp) ? resp : []);
          if (typeof renderExploreWithSort === 'function') renderExploreWithSort(users, query);
        })
        .catch(() => {
          if (typeof renderExploreWithSort === 'function') renderExploreWithSort([], query);
        });
    }
  }

  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') runSearch(input.value.trim());
  });

  input.addEventListener('input', (e) => {
    if (!e.target.value.trim()) runSearch('');
  });

  const urlQ = new URLSearchParams(window.location.search).get('q');
  if (urlQ) {
    input.value = urlQ;
    runSearch(urlQ);
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
