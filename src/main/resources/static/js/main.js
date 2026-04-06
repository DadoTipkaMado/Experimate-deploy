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