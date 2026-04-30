/* ═══════════════════════════════════════════════
   EXPERIMATE — main.js
   Global utilities used across all pages.
   No frameworks, no build step.
═══════════════════════════════════════════════ */

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
      window.location.href = href;
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
  const skip = ['/login', '/register', '/forgot-password', '/onboarding', '/account/edit'];
  if (skip.some(p => path.startsWith(p))) return;
  if (!document.querySelector('.topbar')) return;
  const userId = typeof Auth !== 'undefined' ? Auth.getUserId() : null;
  if (!userId) return;

  let user = null;
  try { user = await UserAPI.getById(userId); } catch(e) { return; }
  if (!user) return;

  const hasPhoto = !!(user.profilePhotoUrl || localStorage.getItem('photo_' + userId));
  const hasBio   = !!user.bio;
  const hasQuiz  = !!localStorage.getItem('personality_done');
  const pct = (hasPhoto ? 40 : 0) + (hasBio ? 35 : 0) + (hasQuiz ? 25 : 0);
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
    'display:flex;align-items:center;gap:12px;padding:0 20px',
    'background:linear-gradient(90deg,#c94a00,#e05500)',
    'color:#fff;font-family:var(--font-mono,monospace);flex-shrink:0',
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
    <div style="flex:1;min-width:0;overflow:hidden;">
      <span style="font-weight:600;font-size:11px;white-space:nowrap;">${actionText}</span>
      <span style="opacity:0.72;font-size:10px;"> — ${detailText}</span>
    </div>
    <a href="${actionHref}" style="flex-shrink:0;background:rgba(255,255,255,0.18);color:#fff;text-decoration:none;font-size:10px;font-weight:700;letter-spacing:0.06em;padding:5px 12px;border-radius:20px;white-space:nowrap;border:1px solid rgba(255,255,255,0.25);">${ctaLabel}</a>
    <button id="bubble-close" style="background:none;border:none;color:rgba(255,255,255,0.7);cursor:pointer;padding:6px 2px;font-size:16px;line-height:1;flex-shrink:0;">✕</button>
  `;

  const topbar = document.querySelector('.topbar');
  if (!topbar) return;
  topbar.insertAdjacentElement('afterend', bubble);

  requestAnimationFrame(() => {
    bubble.style.maxHeight = '62px';
    bubble.style.opacity   = '1';
    bubble.style.padding   = '14px 20px';
  });

  document.getElementById('bubble-close').addEventListener('click', () => {
    bubble.style.transition = 'max-height 0.25s ease,opacity 0.2s ease,padding 0.25s ease';
    bubble.style.maxHeight  = '0';
    bubble.style.opacity    = '0';
    bubble.style.padding    = '0 20px';
    setTimeout(() => bubble.remove(), 280);
  });
});