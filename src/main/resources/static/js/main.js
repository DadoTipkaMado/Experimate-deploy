/* ═══════════════════════════════════════════════
   EXPERIMATE — main.js
   Global utilities used across all pages.
   No frameworks, no build step.
═══════════════════════════════════════════════ */

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => navigator.serviceWorker.register('/sw.js'));
}

function friendlyBookingError(err) {
  const msg = (err?.message ?? '').toLowerCase();
  if (msg.includes('already booked a listing on the same date'))
    return 'You already have a meet on this date.';
  if (msg.includes('already listed a tour on the same date'))
    return 'The host already has another meet on this date.';
  if (msg.includes('is already reserved'))
    return 'This meet is already full.';
  if (msg.includes('already has a pending booking request'))
    return 'You\'ve already sent a request for this meet.';
  return err?.message || 'Request failed — try again.';
}

/* ───────────────────────────────────────────────
   ROTATING PLACEHOLDERS
─────────────────────────────────────────────── */
(function() {
  const PLACEHOLDERS = {
    'explore-search-input': [
      'Search by city or host...',
      'Find someone in Zagreb...',
      'Looking for a guide in Split?',
      'Try "coffee in Dubrovnik"...',
      'Find a local in your city...',
      'Who\'s hosting near you?',
    ],
    'map-search-input': [
      'Search by name or host...',
      'Find locals on the map...',
      'Filter by host name...',
      'Who\'s near you?',
      'Search by city...',
    ],
  };

  document.addEventListener('DOMContentLoaded', () => {
    Object.entries(PLACEHOLDERS).forEach(([id, list]) => {
      const el = document.getElementById(id);
      if (!el) return;
      el.placeholder = list[Math.floor(Math.random() * list.length)];
    });
  });
})();

/* ───────────────────────────────────────────────
   HTML ESCAPE
─────────────────────────────────────────────── */
function escapeHtml(str) {
  return String(str ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

/* ───────────────────────────────────────────────
   TOAST
   Usage: showToast('Saved!') or showToast('Error', 'warn')
─────────────────────────────────────────────── */
let _toastTimer = null;

function showToast(message, type = 'default') {
  const toast = document.getElementById('toast');
  if (!toast) return;

  toast.textContent = message;
  toast.className = 'toast toast--visible' + (type !== 'default' ? ' toast--' + type : '');

  clearTimeout(_toastTimer);
  _toastTimer = setTimeout(() => {
    toast.classList.remove('toast--visible');
  }, 2400);
}

/* ───────────────────────────────────────────────
   TOGGLE SWITCH
   Usage: <div class="toggle" onclick="toggleSwitch(this)">
─────────────────────────────────────────────── */
function toggleSwitch(el) {
  el.classList.toggle('toggle--on');
}

/* ───────────────────────────────────────────────
   PILL FILTER (single-select group)
   Usage:
     <div class="pill-group">
       <div class="pill pill--active" onclick="selectPill(this)">All</div>
       <div class="pill" onclick="selectPill(this)">Food</div>
     </div>
─────────────────────────────────────────────── */
function selectPill(el) {
  const group = el.closest('.pill-group');
  if (group) {
    group.querySelectorAll('.pill').forEach(p => p.classList.remove('pill--active'));
  }
  el.classList.add('pill--active');
}

/* ───────────────────────────────────────────────
   PILL FILTER (multi-select, no group needed)
   Usage: <div class="pill" onclick="togglePill(this)">Food</div>
─────────────────────────────────────────────── */
function togglePill(el) {
  el.classList.toggle('pill--active');
}

/* ───────────────────────────────────────────────
   BOTTOM SHEET
   Usage:
     <div class="bottom-sheet" id="bottom-sheet">
       <div class="bottom-sheet__handle" onclick="toggleBottomSheet()"></div>
       ...
     </div>
─────────────────────────────────────────────── */
let _sheetOpen = false;

function toggleBottomSheet(forceState) {
  const sheet = document.getElementById('bottom-sheet');
  if (!sheet) return;

  if (typeof forceState === 'boolean') {
    _sheetOpen = forceState;
  } else {
    _sheetOpen = !_sheetOpen;
  }

  sheet.classList.toggle('bottom-sheet--expanded', _sheetOpen);
}

/* ───────────────────────────────────────────────
   TAB SWITCHER
   Usage:
     <div class="tab-bar">
       <div class="tab-bar__item tab-bar__item--active"
            onclick="switchTab(this, 'upcoming')">Upcoming</div>
       <div class="tab-bar__item"
            onclick="switchTab(this, 'past')">Past</div>
     </div>
     <div id="tab-upcoming">...</div>
     <div id="tab-past" hidden>...</div>
─────────────────────────────────────────────── */
function switchTab(el, tabId) {
  // Update tab items
  const bar = el.closest('.tab-bar');
  if (bar) {
    bar.querySelectorAll('.tab-bar__item').forEach(t => {
      t.classList.remove('tab-bar__item--active');
    });
  }
  el.classList.add('tab-bar__item--active');

  // Show/hide panels (panels must have id="tab-{tabId}")
  document.querySelectorAll('[id^="tab-"]').forEach(panel => {
    panel.hidden = panel.id !== `tab-${tabId}`;
  });
}

/* ───────────────────────────────────────────────
   COMMUNITY JOIN TOGGLE
   Usage: <button class="comm-join-btn" onclick="toggleJoin(event, this, 'Community Name')">Join</button>
─────────────────────────────────────────────── */
function toggleJoin(e, btn, name) {
  e.stopPropagation();
  const joined = btn.classList.toggle('join-btn--joined');
  btn.textContent = joined ? 'Joined' : 'Join';
  showToast(joined ? `Joined ${name}` : `Left ${name}`);
}

/* ───────────────────────────────────────────────
   DATE AUTO-FORMAT  DD/MM/YYYY  and  DD/MM/YYYY HH:MM
   Usage: <input data-date-format="date"> or data-date-format="datetime"
   Call initDateInputs() after DOM ready, or on any new input.
─────────────────────────────────────────────── */
function initDateInputs() {
  document.querySelectorAll('[data-date-format]').forEach(input => {
    const mode = input.dataset.dateFormat; // "date" or "datetime"
    input.addEventListener('keydown', e => {
      if (['Backspace','Delete','Tab','ArrowLeft','ArrowRight'].includes(e.key)) return;
      if (!/^\d$/.test(e.key)) { e.preventDefault(); return; }
    });
    let _padTimer = null;
    input.addEventListener('input', e => {
      clearTimeout(_padTimer);
      const adding = !e.inputType || e.inputType.startsWith('insert');
      let digits = input.value.replace(/\D/g, '');
      const max  = mode === 'datetime' ? 12 : 8;
      if (digits.length > max) digits = digits.slice(0, max);

      if (adding && mode !== 'datetime') {
        // Immediate leading zero: day digit > 3 or month digit > 1 can only mean 0x
        if (digits.length === 1 && parseInt(digits) > 3) {
          digits = '0' + digits;
        } else if (digits.length === 3 && parseInt(digits[2]) > 1) {
          digits = digits.slice(0, 2) + '0' + digits.slice(2);
        } else if (digits.length === 1 || digits.length === 3) {
          // Ambiguous (1/2/3 for day, 1 for month): wait 500ms, then pad if still same length
          const snapshot = digits;
          _padTimer = setTimeout(() => {
            let cur = input.value.replace(/\D/g, '');
            if (cur === snapshot) {
              if (cur.length === 1) cur = '0' + cur;
              else if (cur.length === 3) cur = cur.slice(0, 2) + '0' + cur.slice(2);
              input.value = formatDate(cur);
            }
          }, 300);
        }
      }

      input.value = mode === 'datetime' ? formatDatetime(digits) : formatDate(digits);
    });
    input.addEventListener('blur', () => {
      // Pad on blur: 5/3/2000 → 05/03/2000
      const parts = input.value.split('/');
      if (parts.length >= 2) {
        parts[0] = parts[0].padStart(2, '0');
        parts[1] = parts[1].padStart(2, '0');
        input.value = parts.join('/');
      }
    });
  });
}

function formatDate(d) {
  // d = raw digits, max 8
  if (d.length <= 2) return d;
  if (d.length <= 4) return d.slice(0,2) + '/' + d.slice(2);
  return d.slice(0,2) + '/' + d.slice(2,4) + '/' + d.slice(4);
}

function formatDatetime(d) {
  // d = raw digits, max 12: DDMMYYYYHHMM
  let out = formatDate(d.slice(0, 8));
  if (d.length > 8) out += ' ' + d.slice(8, 10);
  if (d.length > 10) out += ':' + d.slice(10, 12);
  return out;
}

/* ───────────────────────────────────────────────
   INIT — runs after DOM is ready
─────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {

  initDateInputs();

  // Page enter animation
  const shell = document.querySelector('.app-shell');
  if (shell) shell.classList.add('anim-fade-up');

  // Page exit transition — intercept internal link clicks
  document.addEventListener('click', e => {
    const a = e.target.closest('a[href]');
    if (!a) return;
    const href = a.getAttribute('href');
    if (!href || href.startsWith('#') || href.startsWith('http') || href.startsWith('mailto') || a.target === '_blank') return;
    if (e.ctrlKey || e.metaKey || e.shiftKey) return;
    e.preventDefault();
    const shell = document.querySelector('.app-shell');
    if (shell) {
      shell.classList.add('app-shell--exit');
      setTimeout(() => { window.location.href = href; }, 250);
    } else {
      window.location.href = href;
    }
  });

  // Bottom sheet drag-to-expand (touch + mouse)
  const sheet = document.getElementById('bottom-sheet');
  if (sheet) {
    let startY = 0;
    let startExpanded = false;

    const onStart = (y) => {
      startY = y;
      startExpanded = _sheetOpen;
    };

    const onEnd = (y) => {
      const delta = startY - y; // positive = dragged up
      if (delta > 40)  toggleBottomSheet(true);   // dragged up → expand
      if (delta < -40) toggleBottomSheet(false);  // dragged down → collapse
    };

    sheet.addEventListener('touchstart', e => onStart(e.touches[0].clientY), { passive: true });
    sheet.addEventListener('touchend',   e => onEnd(e.changedTouches[0].clientY));
    sheet.addEventListener('mousedown',  e => onStart(e.clientY));
    sheet.addEventListener('mouseup',    e => onEnd(e.clientY));

    // Tap handle to toggle
    const handle = sheet.querySelector('.bottom-sheet__handle');
    if (handle) {
      handle.addEventListener('click', () => toggleBottomSheet());
    }
  }

});

/* ───────────────────────────────────────────────
   DATE / TIME DISPLAY FORMATTERS
─────────────────────────────────────────────── */
const _MONTHS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

function fmtDate(iso) {
  const d = new Date(iso);
  return `${String(d.getDate()).padStart(2,'0')} ${_MONTHS[d.getMonth()]}`;
}

function fmtTime(iso) {
  const d = new Date(iso);
  return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
}

function fmtDatetime(iso) {
  return `${fmtDate(iso)} · ${fmtTime(iso)}`;
}

/* ───────────────────────────────────────────────
   USER AVATAR
   Returns HTML string for a circular avatar.
   Pass the cached UserResponse as userObj for photo/initials;
   omit for username-initial fallback.
─────────────────────────────────────────────── */
function userAvatar(username, size, userObj) {
  size = size || 24;
  const hue = (username ?? '').split('').reduce((a, c) => a + c.charCodeAt(0), 0) % 360;
  const initials = userObj
    ? ((userObj.firstName?.[0] ?? '') + (userObj.lastName?.[0] ?? '')).toUpperCase() || '?'
    : (username?.[0]?.toUpperCase() ?? '?');
  const photoUrl = userObj?.profilePhotoUrl ? UserAPI.photoUrl(userObj.profilePhotoUrl) : null;
  const base = `width:${size}px;height:${size}px;border-radius:50%;flex-shrink:0;`;
  if (photoUrl) {
    return `<div style="${base}overflow:hidden;border:1px solid hsl(${hue},40%,30%);"><img src="${photoUrl}" style="width:100%;height:100%;object-fit:cover;" loading="lazy"></div>`;
  }
  return `<div style="${base}background:hsl(${hue},35%,22%);border:1px solid hsl(${hue},40%,35%);display:flex;align-items:center;justify-content:center;font-size:${Math.floor(size * 0.4)}px;font-weight:700;color:hsl(${hue},60%,72%);">${initials}</div>`;
}

/* ───────────────────────────────────────────────
   PROFILE COMPLETION BUBBLE
   Appears on every app page until profile is 100%.
   Dismissable per-visit only — comes back every page.
   Weight: photo 40% · bio 35% · quiz 25%
─────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', async function _completionBubble() {
  const path = window.location.pathname;
  const skip = ['/login', '/register', '/forgot-password', '/onboarding', '/account/edit', '/explore'];
  if (skip.some(p => path.startsWith(p))) return;
  if (!document.querySelector('.topbar')) return;
  const userId = typeof Auth !== 'undefined' ? Auth.getUserId() : null;
  if (!userId) return;
  if (sessionStorage.getItem('bubble_dismissed')) return;

  // Cache derived fields for 5 min to avoid 2 API calls on every page navigation
  const CACHE_TTL = 5 * 60 * 1000;
  let hasPhoto, hasBio, hasQuiz, pct;
  const cachedRaw = sessionStorage.getItem('bubble_state');
  const cachedAt  = parseInt(sessionStorage.getItem('bubble_state_ts') || '0', 10);
  if (cachedRaw && Date.now() - cachedAt < CACHE_TTL) {
    try {
      ({ hasPhoto, hasBio, hasQuiz, pct } = JSON.parse(cachedRaw));
    } catch (_) {}
  }
  if (pct == null) {
    let user = null, quizStatus = null;
    try {
      [user, quizStatus] = await Promise.all([
        UserAPI.getById(userId),
        OnboardingAPI.getStatus().catch(() => null),
      ]);
    } catch(e) { return; }
    if (!user) return;
    hasPhoto = !!(user.profilePhotoUrl || localStorage.getItem('photo_' + userId));
    hasBio   = !!user.bio;
    hasQuiz  = quizStatus?.status === 'COMPLETED' || !!localStorage.getItem('personality_done');
    pct      = (hasPhoto ? 40 : 0) + (hasBio ? 35 : 0) + (hasQuiz ? 25 : 0);
    sessionStorage.setItem('bubble_state',    JSON.stringify({ hasPhoto, hasBio, hasQuiz, pct }));
    sessionStorage.setItem('bubble_state_ts', String(Date.now()));
  }
  if (pct >= 100) return;

  let actionText, actionHref = '/account/edit', detailText, ctaLabel;
  if (!hasPhoto) {
    actionText  = 'Add a profile photo';
    detailText  = 'people are 3× more likely to connect with you';
    ctaLabel    = 'Add photo →';
  } else if (!hasBio) {
    actionText  = 'Write your bio';
    detailText  = 'let people know who you are';
    ctaLabel    = 'Write bio →';
  } else {
    actionText  = 'Take the personality quiz';
    detailText  = 'unlock AI match scoring';
    actionHref  = '/onboarding';
    ctaLabel    = 'Take quiz →';
  }

  const C      = 75.4;
  const filled = ((pct / 100) * C).toFixed(1);

  const bubble = document.createElement('div');
  bubble.id = 'completion-bubble';
  bubble.style.cssText = [
    'display:flex;align-items:center;gap:8px;padding:0 20px',
    'background:linear-gradient(90deg,#c94a00,#e05500)',
    'color:#fff;font-family:var(--font-mono,monospace);flex-shrink:0;line-height:1',
    'overflow:hidden;max-height:0;opacity:0;border-bottom:1px solid rgba(0,0,0,0.2)',
    'transition:max-height 0.4s cubic-bezier(0.32,0.72,0,1),opacity 0.3s ease',
  ].join(';');

  bubble.innerHTML = `
    <svg width="26" height="26" viewBox="0 0 32 32" style="flex-shrink:0;transform:rotate(-90deg);">
      <circle cx="16" cy="16" r="12" fill="none" stroke="rgba(255,255,255,0.22)" stroke-width="3"/>
      <circle cx="16" cy="16" r="12" fill="none" stroke="#fff" stroke-width="3"
        stroke-dasharray="${filled} ${C}" stroke-linecap="round"/>
    </svg>
    <span style="font-size:12px;font-weight:700;">${pct}%</span>
    <div style="flex:1;min-width:0;">
      <span style="font-weight:600;font-size:11px;">${actionText}</span>
      <span style="opacity:0.72;font-size:10px;"> — ${detailText}</span>
    </div>
    <a href="${actionHref}" style="flex-shrink:0;background:rgba(255,255,255,0.18);color:#fff;text-decoration:none;font-size:10px;font-weight:700;letter-spacing:0.06em;padding:5px 12px;border-radius:20px;white-space:nowrap;border:1px solid rgba(255,255,255,0.25);">${ctaLabel}</a>
    <button id="bubble-close" style="background:none;border:none;color:rgba(255,255,255,0.7);cursor:pointer;padding:6px 2px;font-size:16px;line-height:1;flex-shrink:0;">✕</button>
  `;

  // Desktop sidebar: orange dot on Account nav item
  const accountNavItem = document.getElementById('navbar-account');
  if (accountNavItem) {
    accountNavItem.style.position = 'relative';
    const dot = document.createElement('div');
    dot.id = 'profile-completion-dot';
    dot.style.cssText = 'position:absolute;top:8px;right:8px;width:8px;height:8px;border-radius:50%;background:#ff6b35;border:2px solid var(--bg,#0a0a0a);pointer-events:none;';
    accountNavItem.appendChild(dot);
  }

  const topbar = document.querySelector('.topbar');
  if (!topbar) return;
  topbar.insertAdjacentElement('afterend', bubble);

  requestAnimationFrame(() => {
    bubble.style.maxHeight = '80px';
    bubble.style.opacity   = '1';
    bubble.style.padding   = '8px 16px';
  });

  document.getElementById('bubble-close')?.addEventListener('click', () => {
    sessionStorage.setItem('bubble_dismissed', '1');
    bubble.style.transition = 'max-height 0.25s ease,opacity 0.2s ease,padding 0.25s ease';
    bubble.style.maxHeight  = '0';
    bubble.style.opacity    = '0';
    bubble.style.padding    = '0 20px';
    setTimeout(() => bubble.remove(), 260);
  });
});

/* ───────────────────────────────────────────────
   LISTING DETAIL OVERLAY
   Opens a bottom-sheet with full listing info.
   Called from map.js and explore.js.
   opts: { isOwn, reserved, reqStatus, onJoinSuccess }
─────────────────────────────────────────────── */
let _listingDetailEl = null;
let _listingDetailJoinCb = null;
let _listingDetailOnClose = null;
let _listingDetailCountdownTimer = null;
let _currentDetailListing = null;

function _ensureListingDetailOverlay() {
  if (_listingDetailEl) return;
  const el = document.createElement('div');
  el.id = 'listing-detail-overlay';
  el.className = 'listing-detail-overlay';
  el.innerHTML = `
    <div class="listing-detail-card">
      <button class="listing-detail-card__close" onclick="closeListingDetail()">✕</button>
      <div class="listing-detail-card__body">
        <div id="ld-city" class="ld-city"></div>
        <div id="ld-host" class="ld-host"></div>
        <div id="ld-date" class="ld-date"></div>
        <div id="ld-guests" class="ld-date" style="display:none;"></div>
        <div id="ld-status" class="ld-status"></div>
        <div id="ld-desc" class="ld-desc"></div>
        <div id="ld-presence" style="display:none;margin-top:10px;display:flex;flex-direction:column;gap:6px;"></div>
      </div>
      <div class="listing-detail-card__footer" id="ld-footer"></div>
    </div>`;
  el.addEventListener('click', e => { if (e.target === el) closeListingDetail(); });
  document.body.appendChild(el);
  _listingDetailEl = el;
}

function openListingDetail(listing, opts) {
  opts = opts || {};
  const { isOwn, reserved, reqStatus, onJoinSuccess, reminder, onClose } = opts;
  _listingDetailJoinCb  = onJoinSuccess || null;
  _listingDetailOnClose = onClose || null;
  _currentDetailListing = listing;
  if (_listingDetailCountdownTimer) { clearInterval(_listingDetailCountdownTimer); _listingDetailCountdownTimer = null; }
  _ensureListingDetailOverlay();

  const hostHandle = listing.host?.username ?? '';
  const hostName   = ((listing.host?.firstName ?? '') + ' ' + (listing.host?.lastName ?? '')).trim() || hostHandle;
  const userObj    = (typeof _userCache !== 'undefined' && _userCache[hostHandle])
                   || (typeof MapState  !== 'undefined' && MapState.userCache?.[hostHandle])
                   || null;

  const locationUnlocked = isOwn || reserved || reqStatus === 'ACCEPTED';
  document.getElementById('ld-city').textContent = locationUnlocked
    ? (listing.city ?? '')
    : `Near ${listing.city ?? ''}`;

  const avatar = userAvatar(hostHandle, 28, userObj);
  const profileUrl = hostHandle ? `/profile/${encodeURIComponent(hostHandle)}` : '#';
  document.getElementById('ld-host').innerHTML = `<a href="${profileUrl}" onclick="closeListingDetail();_saveMapOverlayState();" style="display:flex;align-items:center;gap:7px;text-decoration:none;color:inherit;">${avatar}<span>${escapeHtml(hostName)}<span style="color:var(--text-3);font-size:12px;margin-left:4px;">@${escapeHtml(hostHandle)}</span></span></a>`;

  const d = new Date(listing.meetingDate);
  const dateStr = `${String(d.getDate()).padStart(2,'0')} ${_MONTHS[d.getMonth()]} ${d.getFullYear()} · ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
  document.getElementById('ld-date').textContent = `📅 ${dateStr}`;

  const maxG   = listing.maxGuests ?? 1;
  const curG   = listing.bookedCount ?? 0;
  const guestInfoEl = document.getElementById('ld-guests');
  if (guestInfoEl) {
    guestInfoEl.textContent = `👥 ${curG}/${maxG} spots taken`;
    guestInfoEl.style.display = '';
  }

  if (reminder) {
    // Reminder mode: show live countdown, no status dot
    function _fmtCountdown() {
      const diff = new Date(listing.meetingDate) - new Date();
      if (diff <= 0) return 'now';
      const h = Math.floor(diff / 3600000);
      const m = Math.ceil((diff % 3600000) / 60000);
      return h > 0 ? `${h}h ${m}m` : `${m}m`;
    }
    const statusEl = document.getElementById('ld-status');
    statusEl.innerHTML = `<div style="font-size:13px;color:var(--accent);font-family:var(--font-mono);letter-spacing:0.06em;">⏰ Meet in <span id="ld-countdown">${_fmtCountdown()}</span></div>`;
    _listingDetailCountdownTimer = setInterval(() => {
      const el = document.getElementById('ld-countdown');
      if (el) el.textContent = _fmtCountdown();
    }, 30000);
    document.getElementById('ld-footer').innerHTML =
      `<button class="btn btn--primary popup-action" style="width:100%;" onclick="closeListingDetail()">Got it, remind me later</button>`;
  } else {
    const available  = !reserved;
    const dotColor   = available ? '#00c9a7' : 'rgba(239,239,239,0.3)';
    const dotGlow    = available ? 'box-shadow:0 0 5px #00c9a7;' : '';
    const statusLabel = isOwn ? 'Your listing'
      : reserved          ? 'Joined'
      : reqStatus === 'PENDING'  ? 'Pending'
      : reqStatus === 'ACCEPTED' ? 'Accepted'
      : available ? 'Available' : 'Booked';
    document.getElementById('ld-status').innerHTML = `
      <div class="ld-status__dot" style="background:${dotColor};${dotGlow}"></div>
      <div class="ld-status__label" style="color:${dotColor};">${statusLabel}</div>`;

    let joinBtn;
    if (isOwn) {
      joinBtn = '';
    } else if (reserved) {
      joinBtn = `<button class="btn" style="border-color:var(--accent-border);color:var(--accent);background:var(--accent-dim);" disabled>Joined</button>`;
    } else if (reqStatus === 'PENDING') {
      joinBtn = `<button class="btn" style="border-color:#ff9944;color:#ff9944;background:rgba(255,153,68,0.08);" disabled>Pending</button>`;
    } else if (reqStatus === 'ACCEPTED') {
      joinBtn = `<button class="btn" style="border-color:var(--accent-border);color:var(--accent);background:var(--accent-dim);" disabled>Accepted ✓</button>`;
    } else if ((listing.bookedCount ?? 0) >= (listing.maxGuests ?? 1)) {
      joinBtn = `<button class="btn" style="opacity:0.45;cursor:default;" disabled>Full</button>`;
    } else if (typeof Auth !== 'undefined' && Auth.getToken()) {
      joinBtn = `<button class="btn btn--primary popup-action" data-listing-id="${listing.id}" onclick="_joinFromDetail(this)">Join</button>`;
    } else {
      joinBtn = `<a href="/login" class="btn btn--ghost popup-action">Sign in to join</a>`;
    }
    document.getElementById('ld-footer').innerHTML = `
      <a href="/profile/${encodeURIComponent(hostHandle)}" class="btn btn--ghost popup-action" onclick="closeListingDetail();_saveMapOverlayState()">View profile</a>
      ${joinBtn}`;
  }

  document.getElementById('ld-desc').textContent = listing.tourDescription ?? '';

  // ── Presence cards (only for participants who pass resId) ─────
  const ldPresence = document.getElementById('ld-presence');
  if (ldPresence) {
    ldPresence.innerHTML = '';
    ldPresence.style.display = 'none';
    const resId = opts.resId ?? null;
    if (resId && (isOwn || reserved || reqStatus === 'ACCEPTED') && typeof ReservationAPI !== 'undefined') {
      ReservationAPI.getPresence(resId).then(people => {
        if (!people || !people.length) return;
        ldPresence.style.display = 'flex';
        ldPresence.innerHTML = people.map(p => {
          const initials   = ((p.firstName?.[0] ?? '') + (p.lastName?.[0] ?? '')).toUpperCase() || '?';
          const hue        = (p.username ?? '').split('').reduce((a, c) => a + c.charCodeAt(0), 0) % 360;
          const photoUrl   = p.profilePhotoUrl ? UserAPI.photoUrl(p.profilePhotoUrl) : null;
          const avatarInner = photoUrl
            ? `<img src="${photoUrl}" alt="" style="width:100%;height:100%;object-fit:cover;border-radius:50%;">`
            : `<span style="font-size:11px;font-weight:700;color:hsl(${hue},60%,72%);">${initials}</span>`;
          const avatarBg   = photoUrl ? '' : `background:hsl(${hue},35%,22%);`;
          const hostBadge  = p.isHost ? `<span style="font-size:9px;background:var(--accent-dim);color:var(--accent);border:1px solid var(--accent-border);border-radius:4px;padding:1px 5px;letter-spacing:0.07em;flex-shrink:0;">HOST</span>` : '';
          const checkMark  = p.checkedIn ? `<span style="font-size:12px;color:var(--accent);flex-shrink:0;">✓</span>` : '';
          return `<a href="/profile/${encodeURIComponent(p.username)}" onclick="closeListingDetail()"
            style="display:flex;align-items:center;gap:10px;padding:9px 12px;background:var(--surface-2);border:1px solid var(--border);border-radius:var(--radius-sm);text-decoration:none;color:inherit;transition:border-color var(--t-fast);"
            onmouseover="this.style.borderColor='var(--accent-border)'" onmouseout="this.style.borderColor='var(--border)'">
            <div style="width:34px;height:34px;border-radius:50%;flex-shrink:0;display:flex;align-items:center;justify-content:center;${avatarBg}">${avatarInner}</div>
            <div style="flex:1;min-width:0;">
              <div style="font-size:13px;font-weight:600;color:var(--text);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(p.firstName)} ${escapeHtml(p.lastName)}</div>
              <div style="font-size:11px;color:var(--text-3);">@${escapeHtml(p.username)}</div>
            </div>
            <div style="display:flex;align-items:center;gap:5px;">${hostBadge}${checkMark}</div>
          </a>`;
        }).join('');
      }).catch(() => {});
    }
  }

  requestAnimationFrame(() => _listingDetailEl.classList.add('listing-detail-overlay--visible'));
  document.body.style.overflow = 'hidden';
}

function closeListingDetail() {
  if (!_listingDetailEl) return;
  _listingDetailEl.classList.remove('listing-detail-overlay--visible');
  document.body.style.overflow = '';
  if (_listingDetailCountdownTimer) { clearInterval(_listingDetailCountdownTimer); _listingDetailCountdownTimer = null; }
  if (_listingDetailOnClose) { const cb = _listingDetailOnClose; _listingDetailOnClose = null; cb(); }
}

function _saveMapOverlayState() {
  if (window.location.pathname !== '/map') return;
  if (!_currentDetailListing) return;
  try {
    const center = typeof MapState !== 'undefined' ? MapState.map.getCenter() : null;
    const zoom   = typeof MapState !== 'undefined' ? MapState.map.getZoom()   : null;
    sessionStorage.setItem('mapOverlayRestore', JSON.stringify({
      listing: _currentDetailListing,
      lat: center?.lat,
      lng: center?.lng,
      zoom,
    }));
  } catch (_) {}
}

function _joinFromDetail(btn) {
  const listingId = parseInt(btn.dataset.listingId, 10);
  btn.disabled = true;
  btn.textContent = '...';
  BookingRequestAPI.create({ listingId })
    .then(() => {
      btn.textContent = 'Pending';
      btn.style.cssText = 'border-color:#ff9944;color:#ff9944;background:rgba(255,153,68,0.08);';
      showToast('Request sent!', 'success');
      if (_listingDetailJoinCb) _listingDetailJoinCb(listingId);
    })
    .catch(err => {
      btn.disabled = false;
      btn.textContent = 'Join';
      showToast(friendlyBookingError(err), 'error');
    });
}

/* ───────────────────────────────────────────────
   PRE-MEET LOCK SCREEN
   Shown globally 45 min before any upcoming meet.
   Skipped on profile/auth pages so user can check
   the host's profile and navigate back freely.
─────────────────────────────────────────────── */
let _pmsResId    = null;
let _pmsOther    = null;
let _pmsMeetDate = null;
let _pmsTimer    = null;
let _pmsChatWs   = null;
let _pmsChatMyUsername = null;
let _pmsGraphicColor  = null;
let _pmsGraphicSymbol = null;

const MEET_COLORS = [
  { hex: '#EF4444', name: 'Red' },       { hex: '#F97316', name: 'Orange' },
  { hex: '#F59E0B', name: 'Amber' },     { hex: '#EAB308', name: 'Yellow' },
  { hex: '#84CC16', name: 'Lime' },      { hex: '#22C55E', name: 'Green' },
  { hex: '#10B981', name: 'Emerald' },   { hex: '#14B8A6', name: 'Teal' },
  { hex: '#06B6D4', name: 'Cyan' },      { hex: '#0EA5E9', name: 'Sky' },
  { hex: '#3B82F6', name: 'Blue' },      { hex: '#6366F1', name: 'Indigo' },
  { hex: '#8B5CF6', name: 'Purple' },    { hex: '#A855F7', name: 'Violet' },
  { hex: '#EC4899', name: 'Pink' },      { hex: '#F43F5E', name: 'Rose' },
  { hex: '#DC2626', name: 'Crimson' },   { hex: '#7C3AED', name: 'Dark Violet' },
  { hex: '#059669', name: 'Forest' },    { hex: '#0369A1', name: 'Ocean' },
];
const MEET_SYMBOLS = [
  '★', '●', '▲', '◆', '✦', '⚡', '☀', '❄',
  '⚓', '♠', '♥', '♣', '✈', '☯', '⚜', '⊕',
  '⚛', '♫', '✿', '⚔', '❋', '✺', '⌘', '☁',
  '✙', '◎', '✧', '⚘', '☮', '❃',
];

document.addEventListener('DOMContentLoaded', async function _preMeetCheck() {
  const path = window.location.pathname;
  const skip = ['/login', '/register', '/forgot-password', '/onboarding', '/profile'];
  if (skip.some(p => path.startsWith(p))) return;
  if (typeof ReservationAPI === 'undefined' || typeof Auth === 'undefined') return;
  if (!Auth.getToken()) return;

  try {
    const [joined, hosted] = await Promise.all([
      ReservationAPI.getMine({ filter: 'joined', timeframe: 'upcoming', direction: 'ASC' }).catch(() => []),
      ReservationAPI.getMine({ filter: 'hosted', timeframe: 'upcoming', direction: 'ASC' }).catch(() => []),
    ]);
    const now        = new Date();
    const LOCK_MS    = 60 * 60 * 1000;
    const REMIND_MS  = 3 * 60 * 60 * 1000;
    const GRACE_MS   = 55 * 60 * 1000; // backend expires CONFIRMED reservations after 60 min

    const all = [
      ...(joined || []).map(r => ({ ...r, _isGuest: true  })),
      ...(hosted || []).map(r => ({ ...r, _isGuest: false })),
    ].filter(r => r.status === 'CONFIRMED');

    // Lock screen: ≤ 45 min
    const lockDue = all
      .filter(r => { const d = new Date(r.tourListing.meetingDate) - now; return d <= LOCK_MS && d > -GRACE_MS; })
      .sort((a, b) => new Date(a.tourListing.meetingDate) - new Date(b.tourListing.meetingDate))[0];
    if (lockDue) { _showPreMeetScreen(lockDue); return; }

    // Reminder: ≤ 3 h (but > 45 min) — only on map and explore
    if (!['/map', '/explore'].some(p => path.startsWith(p))) return;
    const remindDue = all
      .filter(r => { const d = new Date(r.tourListing.meetingDate) - now; return d <= REMIND_MS && d > LOCK_MS; })
      .sort((a, b) => new Date(a.tourListing.meetingDate) - new Date(b.tourListing.meetingDate))[0];
    if (remindDue && _reminderAllowed()) _showReminder(remindDue);
  } catch (_) {}
});

function _ensurePreMeetScreen() {
  if (document.getElementById('premeet-screen')) return;

  // Lock screen
  const el = document.createElement('div');
  el.id = 'premeet-screen';
  el.className = 'premeet-screen';
  el.innerHTML = `
    <div class="premeet-screen__header">
      <span class="premeet-screen__title">Upcoming Meet</span>
      <span class="premeet-screen__badge">Meet in <span id="pms-countdown" class="premeet-screen__badge-val">--</span></span>
    </div>
    <div class="premeet-screen__body">
      <a id="pms-avatar-link" class="premeet-screen__avatar-link" href="#">
        <div id="pms-avatar" style="width:100%;height:100%;"></div>
      </a>
      <a id="pms-name" class="premeet-screen__name" href="#"></a>
      <div id="pms-handle" class="premeet-screen__handle"></div>
      <div id="pms-role" class="premeet-screen__role"></div>
      <div class="premeet-screen__details">
        <div id="pms-city" class="premeet-screen__detail"></div>
        <div id="pms-datetime" class="premeet-screen__detail"></div>
      </div>
      <div id="pms-desc" class="premeet-screen__desc"></div>
      <div class="pms-chat" id="pms-chat">
        <div class="pms-chat__label">Group chat</div>
        <div class="pms-chat__msgs" id="pms-chat-msgs">
          <div class="pms-chat__status" id="pms-chat-status">Connecting...</div>
        </div>
        <div class="pms-chat__input-row">
          <textarea class="pms-chat__input" id="pms-chat-input" placeholder="Message..." maxlength="500" rows="1"
            onkeydown="_pmsChatKeydown(event)"></textarea>
          <button class="pms-chat__send" id="pms-chat-send" onclick="_pmsChatSend()" title="Send">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
          </button>
        </div>
      </div>
      <div class="pms-meet-graphic" id="pms-meet-graphic" style="display:none;">
        <div class="pms-meet-graphic__header">Meet identifier</div>
        <div class="pms-meet-graphic__row">
          <div class="pms-meet-graphic__swatch" id="pms-graphic-swatch" onclick="_showMeetGraphicFS()" title="Show full screen">
            <span id="pms-graphic-symbol">★</span>
          </div>
          <div class="pms-meet-graphic__meta">
            <div class="pms-meet-graphic__colorname" id="pms-graphic-colorname">—</div>
            <div class="pms-meet-graphic__hint">Show this to your Mate to find each other</div>
          </div>
          <button class="pms-meet-graphic__expand" onclick="_showMeetGraphicFS()" title="Show full screen">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="15 3 21 3 21 9"/><polyline points="9 21 3 21 3 15"/><line x1="21" y1="3" x2="14" y2="10"/><line x1="3" y1="21" x2="10" y2="14"/></svg>
          </button>
        </div>
      </div>
    </div>
    <div class="premeet-screen__footer">
      <button class="btn btn--ghost" onclick="_pmsCancelClick()">Cancel meet</button>
      <button class="btn btn--primary" id="pms-checkin-btn" onclick="_pmsCheckIn()">I'm here ✓</button>
    </div>`;
  document.body.appendChild(el);

  // Cancel dialog — separate fixed element, z-index above the lock screen
  const dlg = document.createElement('div');
  dlg.id = 'pms-cancel-dialog';
  dlg.style.cssText = 'display:none;position:fixed;top:0;right:0;bottom:0;left:0;z-index:800;background:rgba(0,0,0,0.82);align-items:center;justify-content:center;padding:24px;box-sizing:border-box;';
  dlg.addEventListener('click', e => { if (e.target === dlg) _pmsCloseCancelDlg(); });

  const card = document.createElement('div');
  card.style.cssText = 'background:var(--surface);border:1px solid var(--border-2);border-radius:var(--radius);padding:24px;width:100%;max-width:360px;display:flex;flex-direction:column;gap:16px;box-sizing:border-box;';

  const title = document.createElement('div');
  title.style.cssText = 'font-family:var(--font-display);font-weight:800;font-size:20px;color:var(--text);';
  title.textContent = 'Cancel this meet?';

  const warning = document.createElement('div');
  warning.style.cssText = 'font-size:12px;line-height:1.65;color:rgba(255,160,60,0.95);';
  warning.textContent = '⚠️ Cancelling wastes your Mate\'s time. Repeated cancellations will result in account sanctions. Please explain why you need to cancel:';

  const ta = document.createElement('textarea');
  ta.id = 'pms-cancel-reason';
  ta.placeholder = 'Reason for cancellation...';
  ta.maxLength = 300;
  ta.style.cssText = 'background:var(--surface-2,#1a1a1a);border:1px solid var(--border-2);border-radius:var(--radius-sm);padding:12px 14px;color:var(--text);font-family:var(--font-mono);font-size:13px;resize:vertical;min-height:90px;width:100%;box-sizing:border-box;line-height:1.5;';

  const actions = document.createElement('div');
  actions.style.cssText = 'display:flex;gap:10px;';

  const keepBtn = document.createElement('button');
  keepBtn.className = 'btn btn--ghost';
  keepBtn.style.cssText = 'flex:1;height:46px;font-size:13px;';
  keepBtn.textContent = 'Keep it';
  keepBtn.addEventListener('click', _pmsCloseCancelDlg);

  const confirmBtn = document.createElement('button');
  confirmBtn.id = 'pms-confirm-cancel-btn';
  confirmBtn.className = 'btn btn--danger';
  confirmBtn.style.cssText = 'flex:1;height:46px;font-size:13px;';
  confirmBtn.textContent = 'Cancel meet';
  confirmBtn.addEventListener('click', _pmsConfirmCancel);

  actions.appendChild(keepBtn);
  actions.appendChild(confirmBtn);
  card.appendChild(title);
  card.appendChild(warning);
  card.appendChild(ta);
  card.appendChild(actions);
  dlg.appendChild(card);
  document.body.appendChild(dlg);

  // Meet graphic full-screen overlay
  if (!document.getElementById('pms-graphic-fullscreen')) {
    const fs = document.createElement('div');
    fs.id = 'pms-graphic-fullscreen';
    fs.className = 'pms-graphic-fullscreen';
    fs.innerHTML = `
      <button class="pms-graphic-fullscreen__close" onclick="_hideMeetGraphicFS()">✕</button>
      <div class="pms-graphic-fullscreen__symbol" id="pms-graphic-fs-symbol">★</div>
      <div class="pms-graphic-fullscreen__colorname" id="pms-graphic-fs-colorname">—</div>
      <div class="pms-graphic-fullscreen__sub">Show this to your Mate</div>`;
    fs.addEventListener('click', e => { if (e.target === fs) _hideMeetGraphicFS(); });
    document.body.appendChild(fs);
  }
}

function _showPreMeetScreen(res) {
  _ensurePreMeetScreen();
  _pmsResId    = res.id;
  _pmsMeetDate = new Date(res.tourListing.meetingDate);
  _pmsOther    = res._isGuest ? res.tourListing.host : res.guest;

  const handle = _pmsOther?.username ?? '';
  const name   = _pmsOther ? ((_pmsOther.firstName ?? '') + ' ' + (_pmsOther.lastName ?? '')).trim() || handle : handle;
  const profileUrl = handle ? `/profile/${encodeURIComponent(handle)}` : '#';

  document.getElementById('pms-avatar-link').href = profileUrl;
  document.getElementById('pms-avatar').innerHTML  = userAvatar(handle, 80, _pmsOther);
  const nameEl = document.getElementById('pms-name');
  nameEl.textContent = name; nameEl.href = profileUrl;
  document.getElementById('pms-handle').textContent = handle ? `@${handle}` : '';
  document.getElementById('pms-role').textContent   = res._isGuest ? 'Your host' : 'Your Mate';

  const l = res.tourListing;
  document.getElementById('pms-city').textContent     = `📍 ${l.city ?? ''}`;
  const d = _pmsMeetDate;
  document.getElementById('pms-datetime').textContent =
    `📅 ${String(d.getDate()).padStart(2,'0')} ${_MONTHS[d.getMonth()]} ${d.getFullYear()} · ` +
    `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
  document.getElementById('pms-desc').textContent = l.tourDescription ?? '';

  if (_pmsTimer) clearInterval(_pmsTimer);
  const CHECKIN_OPEN_MS = 30 * 60 * 1000; // matches backend MINS_DIFF_TO_CHECK_IN_MIN
  function tick() {
    const diff = _pmsMeetDate - new Date();
    const el = document.getElementById('pms-countdown');
    if (!el) return;
    if (diff <= 0) { el.textContent = 'now'; return; }
    const m = Math.ceil(diff / 60000);
    el.textContent = m >= 60 ? `${Math.floor(m/60)}h ${m%60}m` : `${m}m`;
    const btn = document.getElementById('pms-checkin-btn');
    if (!btn || btn.textContent === '...' || btn.textContent === 'Waiting…' || btn.textContent === 'Unavailable') return;
    if (diff > CHECKIN_OPEN_MS) {
      btn.disabled = true;
      const mLeft = Math.ceil((diff - CHECKIN_OPEN_MS) / 60000);
      btn.textContent = `Opens in ${mLeft}m`;
    } else {
      if (btn.disabled && btn.textContent.startsWith('Opens')) {
        btn.disabled = false;
        btn.textContent = "I'm here ✓";
      }
    }
  }
  tick();
  _pmsTimer = setInterval(tick, 30000);

  _pmsChatConnect(res.id);
  document.getElementById('premeet-screen').classList.add('premeet-screen--visible');
  document.body.style.overflow = 'hidden';
  const toast = document.getElementById('toast');
  if (toast) toast.style.bottom = '110px';
}

function _pmsCheckIn() {
  const btn = document.getElementById('pms-checkin-btn');
  btn.disabled = true; btn.textContent = '...';
  ReservationAPI.checkIn(_pmsResId)
    .then(res => {
      if (res && res.guestCheckedIn && res.hostCheckedIn) {
        const city     = document.getElementById('pms-city').textContent.replace('📍 ', '');
        const hostName = document.getElementById('pms-name').textContent;
        localStorage.setItem('activeTour', JSON.stringify({ resId: _pmsResId, city, hostName }));
        _loadMeetGraphic(_pmsResId);
        _pmsClose();
        showToast('Both checked in — meet is live!', 'success');
        setTimeout(() => { window.location.href = '/tours'; }, 900);
      } else {
        btn.textContent = 'Waiting…';
        showToast('Checked in! Waiting for your Mate.', 'success');
        _loadMeetGraphic(_pmsResId);
      }
    })
    .catch(err => {
      const msg = err.message || '';
      const permanent = msg.includes('CONFIRMED') || msg.includes('already checked in');
      if (permanent) {
        btn.textContent = 'Unavailable';
        showToast(msg, 'error');
      } else {
        btn.disabled = false;
        btn.textContent = "I'm here ✓";
        showToast(msg || 'Check-in failed — try again.', 'error');
      }
    });
}

function _pmsCancelClick() {
  document.getElementById('pms-cancel-reason').value = '';
  document.getElementById('pms-cancel-dialog').style.display = 'flex';
}

function _pmsCloseCancelDlg() {
  document.getElementById('pms-cancel-dialog').style.display = 'none';
}

async function _loadMeetGraphic(resId) {
  const el = document.getElementById('pms-meet-graphic');
  if (!el) return;

  let colorIdx = null, symbolIdx = null;
  try {
    const presence = await ReservationAPI.getPresence(resId);
    if (presence?.colorIndex  != null) colorIdx  = presence.colorIndex;
    if (presence?.symbolIndex != null) symbolIdx = presence.symbolIndex;
  } catch { /* backend pending — use fallback */ }

  // Deterministic fallback from resId so testing always shows something
  if (colorIdx  === null) colorIdx  = resId % MEET_COLORS.length;
  if (symbolIdx === null) symbolIdx = (resId * 7 + 3) % MEET_SYMBOLS.length;

  const color  = MEET_COLORS[colorIdx  % MEET_COLORS.length];
  const symbol = MEET_SYMBOLS[symbolIdx % MEET_SYMBOLS.length];
  _pmsGraphicColor  = color;
  _pmsGraphicSymbol = symbol;

  const swatch = document.getElementById('pms-graphic-swatch');
  if (swatch) swatch.style.background = color.hex;
  const symEl = document.getElementById('pms-graphic-symbol');
  if (symEl) symEl.textContent = symbol;
  const nameEl = document.getElementById('pms-graphic-colorname');
  if (nameEl) nameEl.textContent = color.name;

  el.style.display = '';
}

function _showMeetGraphicFS() {
  if (!_pmsGraphicColor) return;
  const fs = document.getElementById('pms-graphic-fullscreen');
  if (!fs) return;
  fs.style.background = _pmsGraphicColor.hex;
  document.getElementById('pms-graphic-fs-symbol').textContent    = _pmsGraphicSymbol;
  document.getElementById('pms-graphic-fs-colorname').textContent = _pmsGraphicColor.name;
  fs.classList.add('visible');
}

function _hideMeetGraphicFS() {
  const fs = document.getElementById('pms-graphic-fullscreen');
  if (fs) fs.classList.remove('visible');
}

function _pmsConfirmCancel() {
  if (!document.getElementById('pms-cancel-reason').value.trim()) {
    showToast('Please provide a reason.'); return;
  }
  const btn = document.getElementById('pms-confirm-cancel-btn');
  btn.disabled = true; btn.textContent = 'Cancelling...';
  ReservationAPI.cancelTour(_pmsResId)
    .then(() => { _pmsCloseCancelDlg(); _pmsClose(); showToast('Meet cancelled.', 'success'); })
    .catch(err => {
      btn.disabled = false; btn.textContent = 'Cancel meet';
      showToast(err.message || 'Could not cancel — try again.', 'error');
    });
}

function _pmsClose() {
  const el = document.getElementById('premeet-screen');
  if (el) el.classList.remove('premeet-screen--visible');
  document.body.style.overflow = '';
  const toast = document.getElementById('toast');
  if (toast) toast.style.bottom = '';
  if (_pmsTimer) { clearInterval(_pmsTimer); _pmsTimer = null; }
  _pmsChatDisconnect();
  _pmsResId = _pmsOther = _pmsMeetDate = null;
}

/* ───────────────────────────────────────────────
   PRE-MEET CHAT
   WebSocket chat embedded in the lock screen.
   Backend endpoint: ws://<host>/ws/chat/{reservationId}?token=<jwt>
   Inbound frames:
     { type: 'HISTORY', messages: [ChatMessage] }
     { type: 'MESSAGE', ...ChatMessage }
   Outbound frames:
     { text: '...' }
   ChatMessage: { senderUsername, senderFirstName, senderLastName, text, sentAt }
─────────────────────────────────────────────── */
function _pmsChatConnect(resId) {
  _pmsChatMyUsername = Auth.getUsername();
  const msgsEl   = document.getElementById('pms-chat-msgs');
  const statusEl = document.getElementById('pms-chat-status');
  if (!msgsEl) return;

  const proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
  const token = Auth.getToken() ?? '';
  const url   = `${proto}//${location.host}/ws/chat/${resId}?token=${encodeURIComponent(token)}`;

  try { _pmsChatWs = new WebSocket(url); }
  catch (_) {
    if (statusEl) statusEl.textContent = 'Chat unavailable.';
    return;
  }

  _pmsChatWs.addEventListener('open', () => {
    if (statusEl) statusEl.remove();
  });

  _pmsChatWs.addEventListener('message', evt => {
    try {
      const data = JSON.parse(evt.data);
      if (data.type === 'HISTORY') (data.messages || []).forEach(_pmsChatAddMsg);
      else if (data.type === 'MESSAGE') _pmsChatAddMsg(data);
    } catch (_) {}
  });

  _pmsChatWs.addEventListener('close', () => {
    const msgsDiv = document.getElementById('pms-chat-msgs');
    if (!msgsDiv) return;
    const existing = document.getElementById('pms-chat-status');
    if (!existing) {
      const div = document.createElement('div');
      div.id = 'pms-chat-status';
      div.className = 'pms-chat__status';
      div.textContent = 'Disconnected — refresh to reconnect.';
      msgsDiv.appendChild(div);
    }
  });

  _pmsChatWs.addEventListener('error', () => {
    const s = document.getElementById('pms-chat-status');
    if (s) s.textContent = 'Chat unavailable.';
  });
}

function _pmsChatAddMsg(msg) {
  const msgsEl = document.getElementById('pms-chat-msgs');
  if (!msgsEl) return;
  const isMine = msg.senderUsername === _pmsChatMyUsername;
  const name   = isMine ? 'You'
    : msg.senderFirstName ? `${msg.senderFirstName} ${msg.senderLastName}` : msg.senderUsername;
  const time = msg.sentAt
    ? new Date(msg.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '';
  const div = document.createElement('div');
  div.className = `pms-chat__msg pms-chat__msg--${isMine ? 'mine' : 'theirs'}`;
  div.innerHTML = `<div class="pms-chat__msg-bubble">${escapeHtml(msg.text)}</div>`
    + `<div class="pms-chat__msg-meta">${escapeHtml(name)}${time ? ' · ' + time : ''}</div>`;
  msgsEl.appendChild(div);
  msgsEl.scrollTop = msgsEl.scrollHeight;
}

function _pmsChatSend() {
  const input = document.getElementById('pms-chat-input');
  const text  = input?.value.trim();
  if (!text || !_pmsChatWs || _pmsChatWs.readyState !== WebSocket.OPEN) return;
  _pmsChatWs.send(JSON.stringify({ text }));
  input.value = '';
  input.style.height = '';
}

function _pmsChatKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); _pmsChatSend(); }
}

function _pmsChatDisconnect() {
  if (_pmsChatWs) { _pmsChatWs.close(); _pmsChatWs = null; }
  _pmsChatMyUsername = null;
  const msgsEl = document.getElementById('pms-chat-msgs');
  if (msgsEl) msgsEl.innerHTML = '<div class="pms-chat__status" id="pms-chat-status">Connecting...</div>';
}

/* ───────────────────────────────────────────────
   REMINDER POPUP
   Shows on /map and /explore, 3 h → 45 min before
   a meet. Closes, then re-opens every 10 min.
─────────────────────────────────────────────── */
const _REMIND_SNOOZE_MS = 10 * 60 * 1000;

function _reminderAllowed() {
  const ts = sessionStorage.getItem('reminderDismissedAt');
  if (!ts) return true;
  return Date.now() - parseInt(ts, 10) > _REMIND_SNOOZE_MS;
}

function _showReminder(res) {
  const listing = res.tourListing;
  openListingDetail(listing, {
    isOwn:    !res._isGuest,
    reserved:  res._isGuest,
    reminder:  true,
    onClose: () => {
      sessionStorage.setItem('reminderDismissedAt', Date.now().toString());
      setTimeout(() => {
        if (_reminderAllowed()) _showReminder(res);
      }, _REMIND_SNOOZE_MS);
    },
  });
}

/* ═══════════════════════════════════════════════
   CUSTOM DATE PICKER — shared across all pages
   Usage: DatePicker.open(inputEl, { min, max })
   Writes DD/MM/YYYY into inputEl.value and fires input + change events.
   - Typed input with DD/MM/YYYY auto-formatting
   - Click month → month grid; click year → scrollable year list
   - Mouse wheel on month/year labels scrolls them
═══════════════════════════════════════════════ */
const DatePicker = (() => {
  const MONTHS_LONG  = ['January','February','March','April','May','June','July','August','September','October','November','December'];
  const MONTHS_SHORT = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  const DAYS         = ['Mo','Tu','We','Th','Fr','Sa','Su'];

  let _el   = null;
  let _opts = {};
  let _view = null;   // { year, month }
  let _mode = 'days'; // 'days' | 'months' | 'years'
  let _wrap = null;

  function open(inputEl, opts = {}) {
    _el   = inputEl;
    _opts = opts;
    _mode = 'days';
    inputEl._dpOpts = opts;

    const parts = (inputEl.value || '').split('/');
    const today = new Date();
    if (parts.length === 3 && parts[2].length === 4) {
      _view = { year: parseInt(parts[2]), month: parseInt(parts[1]) - 1 };
    } else if (opts.max) {
      _view = { year: opts.max.getFullYear(), month: opts.max.getMonth() };
    } else {
      _view = { year: today.getFullYear(), month: today.getMonth() };
    }

    if (!_wrap) {
      _wrap = document.createElement('div');
      _wrap.className = 'dp-wrap';
      document.body.appendChild(_wrap);
      document.addEventListener('click', _outsideClick, true);
      document.addEventListener('keydown', e => { if (e.key === 'Escape') close(); });
    }

    if (!inputEl.dataset.dpWired) {
      inputEl.dataset.dpWired = '1';
      inputEl.removeAttribute('readonly');
      inputEl.style.cursor = '';
      inputEl.addEventListener('keydown', _handleKeydown);
      inputEl.addEventListener('input',   _handleInput);
      inputEl.addEventListener('focus', function () {
        if (_wrap && _wrap.style.display !== 'none' && _el === this) return;
        open(this, this._dpOpts || {});
      });
    }

    _render();
    _position();
    _wrap.style.display = 'block';
  }

  function _handleKeydown(e) {
    if (e.ctrlKey || e.metaKey || e.altKey) return;
    const nav = ['Backspace','Delete','ArrowLeft','ArrowRight','ArrowUp','ArrowDown','Tab','Home','End','Enter'];
    if (!nav.includes(e.key) && !/^\d$/.test(e.key)) e.preventDefault();
  }

  function _handleInput(e) {
    const input  = e.target;
    const digits = input.value.replace(/\D/g, '').slice(0, 8);
    let formatted = digits.slice(0, 2);
    if (digits.length > 2) formatted += '/' + digits.slice(2, 4);
    if (digits.length > 4) formatted += '/' + digits.slice(4, 8);
    if (formatted !== input.value) input.value = formatted;

    const parts = formatted.split('/');
    if (parts.length === 3 && parts[2].length === 4) {
      const y = parseInt(parts[2]), m = parseInt(parts[1]) - 1;
      if (!isNaN(y) && m >= 0 && m <= 11) {
        _view = { year: y, month: m };
        _mode = 'days';
        if (_wrap && _wrap.style.display !== 'none') _render();
      }
    }
  }

  function _render() {
    if (_mode === 'days')   _renderDays();
    if (_mode === 'months') _renderMonths();
    if (_mode === 'years')  _renderYears();
  }

  function _renderDays() {
    const { year, month } = _view;
    const today     = new Date(); today.setHours(0,0,0,0);
    const selParts  = (_el.value || '').split('/');
    const selDate   = selParts.length === 3 && selParts[2].length === 4
      ? new Date(parseInt(selParts[2]), parseInt(selParts[1])-1, parseInt(selParts[0]))
      : null;
    const firstDay  = new Date(year, month, 1);
    const daysInMon = new Date(year, month + 1, 0).getDate();
    let dow = firstDay.getDay() - 1; if (dow < 0) dow = 6;

    let cells = '';
    for (let i = 0; i < dow; i++) cells += '<div class="dp-cell dp-cell--empty"></div>';
    for (let d = 1; d <= daysInMon; d++) {
      const date     = new Date(year, month, d);
      const isToday  = date.getTime() === today.getTime();
      const isSel    = selDate && date.getTime() === selDate.getTime();
      const disabled = (_opts.min && date < _opts.min) || (_opts.max && date > _opts.max);
      cells += `<div class="dp-cell${isToday ? ' dp-cell--today' : ''}${isSel ? ' dp-cell--selected' : ''}${disabled ? ' dp-cell--disabled' : ''}"
        ${!disabled ? `data-y="${year}" data-m="${month}" data-d="${d}"` : ''}>${d}</div>`;
    }

    _wrap.innerHTML = `
      <div class="dp-header">
        <button class="dp-nav dp-nav--m-prev" type="button" title="Previous month">&#8249;</button>
        <div class="dp-header-selectors">
          <button class="dp-sel dp-sel--month" type="button">${MONTHS_LONG[month]}</button>
          <button class="dp-sel dp-sel--year" type="button">${year}</button>
        </div>
        <button class="dp-nav dp-nav--m-next" type="button" title="Next month">&#8250;</button>
      </div>
      <div class="dp-dow-row">${DAYS.map(d => `<div class="dp-dow">${d}</div>`).join('')}</div>
      <div class="dp-grid">${cells}</div>
    `;

    _wrap.querySelector('.dp-nav--m-prev').addEventListener('click', e => { e.stopPropagation(); _navMonth(-1); });
    _wrap.querySelector('.dp-nav--m-next').addEventListener('click', e => { e.stopPropagation(); _navMonth(1); });

    const mBtn = _wrap.querySelector('.dp-sel--month');
    const yBtn = _wrap.querySelector('.dp-sel--year');
    mBtn.addEventListener('click', e => { e.stopPropagation(); _mode = 'months'; _render(); });
    yBtn.addEventListener('click', e => { e.stopPropagation(); _mode = 'years'; _render(); });
    mBtn.addEventListener('wheel', e => { e.preventDefault(); e.stopPropagation(); _navMonth(e.deltaY > 0 ? 1 : -1); }, { passive: false });
    yBtn.addEventListener('wheel', e => { e.preventDefault(); e.stopPropagation(); _navYear(e.deltaY > 0 ? 1 : -1); }, { passive: false });

    _wrap.querySelectorAll('.dp-cell[data-d]').forEach(cell => {
      cell.addEventListener('click', e => { e.stopPropagation(); _pick(cell); });
    });
  }

  function _renderMonths() {
    const cells = MONTHS_SHORT.map((m, i) =>
      `<div class="dp-month-cell${i === _view.month ? ' dp-month-cell--selected' : ''}" data-m="${i}">${m}</div>`
    ).join('');

    _wrap.innerHTML = `
      <div class="dp-header dp-header--sub">
        <button class="dp-nav dp-nav--back" type="button">&#8249;</button>
        <span class="dp-sub-title">Month · <button class="dp-sel dp-sel--year-link" type="button">${_view.year} &#8250;</button></span>
        <span></span>
      </div>
      <div class="dp-month-grid">${cells}</div>
    `;

    _wrap.querySelector('.dp-nav--back').addEventListener('click', e => { e.stopPropagation(); _mode = 'days'; _render(); });
    _wrap.querySelector('.dp-sel--year-link').addEventListener('click', e => { e.stopPropagation(); _mode = 'years'; _render(); });
    _wrap.querySelectorAll('.dp-month-cell').forEach(cell => {
      cell.addEventListener('click', e => {
        e.stopPropagation();
        _view.month = parseInt(cell.dataset.m);
        _mode = 'days';
        _render();
      });
    });
  }

  function _renderYears() {
    const today   = new Date();
    const minYear = _opts.min ? _opts.min.getFullYear() : 1900;
    const maxYear = _opts.max ? _opts.max.getFullYear() : today.getFullYear() + 10;
    const selYear = _view.year;

    let items = '';
    for (let y = maxYear; y >= minYear; y--) {
      items += `<div class="dp-year-item${y === selYear ? ' dp-year-item--selected' : ''}" data-y="${y}">${y}</div>`;
    }

    _wrap.innerHTML = `
      <div class="dp-header dp-header--sub">
        <button class="dp-nav dp-nav--back" type="button">&#8249;</button>
        <span class="dp-sub-title">Select year</span>
        <span></span>
      </div>
      <div class="dp-year-list" id="dp-year-list">${items}</div>
    `;

    _wrap.querySelector('.dp-nav--back').addEventListener('click', e => { e.stopPropagation(); _mode = 'days'; _render(); });
    _wrap.querySelectorAll('.dp-year-item').forEach(item => {
      item.addEventListener('click', e => {
        e.stopPropagation();
        _view.year = parseInt(item.dataset.y);
        _mode = 'months';
        _render();
      });
    });

    requestAnimationFrame(() => {
      const list = document.getElementById('dp-year-list');
      const sel  = list && list.querySelector('.dp-year-item--selected');
      if (list && sel) list.scrollTop = sel.offsetTop - list.clientHeight / 2 + sel.clientHeight / 2;
    });
  }

  function _navMonth(dir) {
    _view.month += dir;
    if (_view.month < 0)  { _view.month = 11; _view.year--; }
    if (_view.month > 11) { _view.month = 0;  _view.year++; }
    _render();
  }

  function _navYear(dir) {
    _view.year += dir;
    _render();
  }

  function _pick(cell) {
    const d = cell.dataset.d.padStart(2,'0');
    const m = (parseInt(cell.dataset.m)+1).toString().padStart(2,'0');
    const y = cell.dataset.y;
    _el.value = `${d}/${m}/${y}`;
    _el.dispatchEvent(new Event('input',  { bubbles: true }));
    _el.dispatchEvent(new Event('change', { bubbles: true }));
    close();
  }

  function _position() {
    const rect = _el.getBoundingClientRect();
    const spaceBelow = window.innerHeight - rect.bottom;
    _wrap.style.left = Math.min(rect.left, window.innerWidth - 310) + 'px';
    if (spaceBelow >= 340) {
      _wrap.style.top    = (rect.bottom + 6) + 'px';
      _wrap.style.bottom = 'auto';
    } else {
      _wrap.style.bottom = (window.innerHeight - rect.top + 6) + 'px';
      _wrap.style.top    = 'auto';
    }
  }

  function _outsideClick(e) {
    if (_wrap && _wrap.style.display !== 'none' && !_wrap.contains(e.target) && e.target !== _el) close();
  }

  function close() {
    if (_wrap) _wrap.style.display = 'none';
  }

  return { open, close };
})();
window.DatePicker = DatePicker;

/* ── Navigation progress bar + hover prefetch ─────────────────────────── */
(function () {
  let _bar, _t;

  function bar() {
    if (!_bar) { _bar = document.createElement('div'); _bar.id = 'np'; document.body.prepend(_bar); }
    return _bar;
  }

  function npStart() {
    const b = bar(); clearTimeout(_t);
    b.style.cssText = 'width:0;opacity:1;transition:none';
    void b.offsetWidth;
    b.style.cssText = 'width:82%;opacity:1;transition:width 0.5s cubic-bezier(0.1,0.5,0.5,1)';
    sessionStorage.setItem('_np', '1');
  }

  function npDone() {
    const b = bar();
    b.style.cssText = 'width:100%;opacity:1;transition:width 0.15s ease';
    _t = setTimeout(() => {
      b.style.cssText = 'width:100%;opacity:0;transition:opacity 0.3s ease';
      setTimeout(() => { b.style.cssText = 'width:0;opacity:0'; }, 350);
    }, 150);
  }

  // Complete bar on new page load (non-View-Transition fallback)
  if (sessionStorage.getItem('_np')) {
    sessionStorage.removeItem('_np');
    requestAnimationFrame(() => requestAnimationFrame(npDone));
  }

  // Start on any internal nav click
  document.addEventListener('click', e => {
    const a = e.target.closest('a[href]');
    if (!a || a.target === '_blank' || !a.href.startsWith(location.origin) || a.href === location.href) return;
    npStart();
  }, true);

  // Prefetch on hover
  const _seen = new Set();
  document.addEventListener('mouseover', e => {
    const a = e.target.closest('a[href]');
    if (!a || !a.href.startsWith(location.origin) || _seen.has(a.href)) return;
    _seen.add(a.href);
    const lnk = document.createElement('link');
    lnk.rel = 'prefetch'; lnk.href = a.href;
    document.head.appendChild(lnk);
  });
})();