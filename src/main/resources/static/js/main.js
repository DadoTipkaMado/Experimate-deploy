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
   PERSONALITY NUDGE
   Shows a one-time bottom nudge to logged-in users
   who haven't completed the onboarding quiz yet.
   Skipping sets a flag so it never shows again.
─────────────────────────────────────────────── */
(function initPersonalityNudge() {
  // Only on app pages (topbar exists), only if logged in, only if quiz not done
  if (!document.getElementById('topbar-avatar')) return;
  if (!localStorage.getItem('jwt')) return;
  if (localStorage.getItem('personality_done')) return;
  if (localStorage.getItem('personality_skipped')) return;
  // Don't show on the onboarding page itself
  if (window.location.pathname === '/onboarding') return;

  const nudge = document.createElement('div');
  nudge.id = 'personality-nudge';
  nudge.style.cssText = `
    position:fixed; bottom:calc(var(--navbar-h,64px) + 10px); left:50%;
    transform:translateX(-50%) translateY(20px);
    width:calc(100% - 32px); max-width:440px;
    background:var(--surface); border:1px solid var(--accent-border);
    border-radius:16px; padding:14px 16px;
    display:flex; align-items:center; gap:12px;
    box-shadow:0 8px 32px rgba(0,0,0,0.35), 0 0 0 1px var(--accent-border);
    z-index:800; opacity:0;
    transition:opacity 0.3s ease, transform 0.3s cubic-bezier(0.32,0.72,0,1);
    font-family:var(--font-mono,monospace);
  `;
  nudge.innerHTML = `
    <div style="width:36px;height:36px;border-radius:10px;background:var(--accent-dim);border:1px solid var(--accent-border);display:flex;align-items:center;justify-content:center;flex-shrink:0;">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2" stroke-linecap="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
    </div>
    <div style="flex:1;min-width:0;">
      <div style="font-size:12px;color:var(--text);font-weight:600;margin-bottom:2px;">Discover your Mates</div>
      <div style="font-size:10px;color:var(--text-3);letter-spacing:0.03em;">Take a 70-second quiz to unlock AI matching</div>
    </div>
    <a href="/onboarding" style="flex-shrink:0;background:var(--accent);color:#000;border:none;border-radius:20px;padding:7px 14px;font-family:var(--font-mono,monospace);font-size:10px;letter-spacing:0.06em;text-decoration:none;white-space:nowrap;display:inline-flex;align-items:center;">
      Start →
    </a>
    <button onclick="dismissPersonalityNudge()" style="background:none;border:none;cursor:pointer;padding:4px;color:var(--text-3);flex-shrink:0;line-height:1;font-size:16px;">✕</button>
  `;
  document.body.appendChild(nudge);

  // Animate in after a short delay
  setTimeout(() => {
    nudge.style.opacity = '1';
    nudge.style.transform = 'translateX(-50%) translateY(0)';
  }, 1200);
})();

function dismissPersonalityNudge() {
  localStorage.setItem('personality_skipped', '1');
  const nudge = document.getElementById('personality-nudge');
  if (!nudge) return;
  nudge.style.opacity = '0';
  nudge.style.transform = 'translateX(-50%) translateY(20px)';
  setTimeout(() => nudge.remove(), 300);
}