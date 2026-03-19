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
  toast.classList.add('toast--visible');

  clearTimeout(_toastTimer);
  _toastTimer = setTimeout(() => {
    toast.classList.remove('toast--visible');
  }, 2200);
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
  const joined = btn.classList.toggle('comm-join-btn--joined');
  btn.textContent = joined ? 'Joined' : 'Join';
  showToast(joined ? `Joined ${name}` : `Left ${name}`);
}

/* ───────────────────────────────────────────────
   INIT — runs after DOM is ready
─────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {

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