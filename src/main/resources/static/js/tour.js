/* ═══════════════════════════════════════════════
   EXPERIIMATE — App tour for first-time users.
   Triggers once on /map. Stored in localStorage.
═══════════════════════════════════════════════ */
(function () {
  if (localStorage.getItem('app_tour_done')) return;

  const STEPS = [
    {
      sel: '.navbar__item[href="/map"]',
      icon: '🗺️',
      title: 'The Map',
      body: 'Home base. Every pin is a real experience hosted by a local. Tap one to see who\'s behind it.'
    },
    {
      sel: '.navbar__item[href="/community"]',
      icon: '👥',
      title: 'Community',
      body: 'Discover local communities and connect with people who share your interests.'
    },
    {
      sel: '.navbar__item[href="/explore"]',
      icon: '🔍',
      title: 'Discover',
      body: 'Find people you\'d click with. Take the personality quiz to unlock match scoring.'
    },
    {
      sel: '.navbar__item[href="/tours"]',
      icon: '📅',
      title: 'Meets',
      body: 'Browse available Meets, manage your bookings, and host your own experiences.'
    },
    {
      sel: '.navbar__item[href="/account"]',
      icon: '👤',
      title: 'Account',
      body: 'Your profile, join requests, ratings, and settings all live here.'
    },
    {
      sel: '.map-fab',
      icon: '✦',
      title: 'New Meet',
      body: 'Ready to host? Tap here to create a Meet and put your experience on the map.'
    },
  ];

  let step = 0;
  let elSpotlight, elTooltip, elOverlay;

  /* ── Kick off after page settles ─────────────── */
  window.addEventListener('load', function () {
    setTimeout(injectAndShow, 900);
  });

  /* ── Styles ───────────────────────────────────── */
  function injectStyles() {
    const style = document.createElement('style');
    style.textContent = `
      /* Welcome modal */
      #tour-welcome {
        position: fixed; inset: 0; z-index: 9900;
        background: rgba(0,0,0,0.86);
        backdrop-filter: blur(14px); -webkit-backdrop-filter: blur(14px);
        display: flex; align-items: center; justify-content: center;
        padding: 24px;
        animation: _tfade .3s ease both;
      }
      #tour-welcome-card {
        background: var(--surface, #1c1c1c);
        border: 1px solid var(--border-2, #2e2e2e);
        border-radius: 24px;
        padding: 36px 28px 28px;
        max-width: 320px; width: 100%; text-align: center;
        box-shadow: 0 32px 80px rgba(0,0,0,0.7);
        animation: _tcard .4s cubic-bezier(.16,1,.3,1) .08s both;
      }
      .twc-logo { font-family: var(--font-display, sans-serif); font-weight: 800; font-size: 22px; color: var(--text, #efefef); letter-spacing: -0.03em; }
      .twc-logo span { color: #00c9a7; }
      .twc-emoji { font-size: 52px; margin: 18px 0 14px; display: block; }
      .twc-h { font-family: var(--font-display, sans-serif); font-weight: 800; font-size: 20px; color: var(--text, #efefef); letter-spacing: -0.01em; margin-bottom: 10px; }
      .twc-sub { font-size: 13px; color: var(--text-2, #999); line-height: 1.65; margin-bottom: 26px; }
      .twc-cta {
        width: 100%; height: 48px;
        background: #00c9a7; color: #000;
        border: none; border-radius: 14px;
        font-family: var(--font-display, sans-serif); font-size: 14px; font-weight: 700;
        cursor: pointer; letter-spacing: 0.01em;
        transition: transform .12s ease, box-shadow .12s ease;
        margin-bottom: 14px;
      }
      .twc-cta:hover { transform: scale(1.02); box-shadow: 0 6px 20px rgba(0,201,167,0.35); }
      .twc-skip { background: none; border: none; color: var(--text-3, #555); font-size: 11px; letter-spacing: 0.06em; cursor: pointer; font-family: var(--font-mono, monospace); }
      .twc-skip:hover { color: var(--text-2, #999); }

      /* Overlay (blocks outside clicks) */
      #tour-overlay {
        position: fixed; inset: 0; z-index: 9800; cursor: default;
      }

      /* Spotlight */
      #tour-spotlight {
        position: fixed; z-index: 9801; border-radius: 14px;
        pointer-events: none;
        box-shadow: 0 0 0 9999px rgba(0,0,0,0.78);
        outline: 2px solid rgba(0,201,167,0.55);
        outline-offset: 3px;
        opacity: 0;
        transition:
          top .38s cubic-bezier(.32,.72,0,1),
          left .38s cubic-bezier(.32,.72,0,1),
          width .38s cubic-bezier(.32,.72,0,1),
          height .38s cubic-bezier(.32,.72,0,1),
          opacity .22s ease;
      }

      /* Tooltip */
      #tour-tooltip {
        position: fixed; z-index: 9802;
        background: var(--surface, #1c1c1c);
        border: 1px solid var(--border-2, #2e2e2e);
        border-radius: 18px;
        padding: 20px;
        width: min(280px, calc(100vw - 32px));
        box-shadow: 0 16px 48px rgba(0,0,0,0.65), 0 0 0 1px rgba(0,201,167,0.06);
        font-family: var(--font-mono, monospace);
        transition: opacity .2s ease, transform .2s ease;
      }
      #tour-tooltip.tt-hidden { opacity: 0; transform: translateY(8px); pointer-events: none; }
      .tt-icon { font-size: 24px; margin-bottom: 10px; }
      .tt-title { font-family: var(--font-display, sans-serif); font-weight: 800; font-size: 16px; color: var(--text, #efefef); letter-spacing: -0.01em; margin-bottom: 6px; }
      .tt-body { font-size: 11px; color: var(--text-2, #999); line-height: 1.75; margin-bottom: 16px; }
      .tt-footer { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
      .tt-dots { display: flex; gap: 5px; align-items: center; }
      .tt-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--border-2, #2e2e2e); transition: background .2s, transform .2s; }
      .tt-dot--active { background: #00c9a7; transform: scale(1.3); }
      .tt-btns { display: flex; gap: 8px; align-items: center; }
      .tt-next {
        background: #00c9a7; color: #000; border: none; border-radius: 20px;
        padding: 8px 18px;
        font-family: var(--font-display, sans-serif); font-size: 12px; font-weight: 700;
        cursor: pointer; letter-spacing: 0.02em; white-space: nowrap;
      }
      .tt-skip { background: none; border: none; color: var(--text-3, #555); font-size: 10px; letter-spacing: 0.08em; cursor: pointer; font-family: var(--font-mono, monospace); }

      @keyframes _tfade { from { opacity:0; } to { opacity:1; } }
      @keyframes _tcard { from { opacity:0; transform:translateY(18px) scale(.97); } to { opacity:1; transform:none; } }
    `;
    document.head.appendChild(style);
  }

  /* ── Entry ────────────────────────────────────── */
  function injectAndShow() {
    injectStyles();
    showWelcome();
  }

  /* ── Welcome modal ────────────────────────────── */
  function showWelcome() {
    const el = document.createElement('div');
    el.id = 'tour-welcome';
    el.innerHTML = `
      <div id="tour-welcome-card">
        <div class="twc-logo">Experi<span>Mate</span></div>
        <span class="twc-emoji">👋</span>
        <div class="twc-h">Welcome aboard!</div>
        <div class="twc-sub">First time here? Let us show you the essentials — takes about 20 seconds.</div>
        <button class="twc-cta" id="tour-yes">Show me around →</button>
        <br>
        <button class="twc-skip" id="tour-no">I'll figure it out</button>
      </div>
    `;
    document.body.appendChild(el);

    document.getElementById('tour-yes').addEventListener('click', () => {
      el.style.transition = 'opacity .2s ease';
      el.style.opacity = '0';
      setTimeout(() => { el.remove(); beginSteps(); }, 200);
    });
    document.getElementById('tour-no').addEventListener('click', endTour);
  }

  /* ── Spotlight steps ──────────────────────────── */
  function beginSteps() {
    elOverlay = document.createElement('div');
    elOverlay.id = 'tour-overlay';

    elSpotlight = document.createElement('div');
    elSpotlight.id = 'tour-spotlight';

    elTooltip = document.createElement('div');
    elTooltip.id = 'tour-tooltip';
    elTooltip.classList.add('tt-hidden');

    document.body.append(elOverlay, elSpotlight, elTooltip);
    showStep();
  }

  function showStep() {
    if (step >= STEPS.length) { endTour(); return; }

    const s = STEPS[step];
    const target = document.querySelector(s.sel);
    if (!target) { step++; showStep(); return; }

    const r = target.getBoundingClientRect();
    const PAD = 10;

    // Move spotlight
    elSpotlight.style.opacity = '1';
    elSpotlight.style.top    = (r.top    - PAD) + 'px';
    elSpotlight.style.left   = (r.left   - PAD) + 'px';
    elSpotlight.style.width  = (r.width  + PAD * 2) + 'px';
    elSpotlight.style.height = (r.height + PAD * 2) + 'px';

    // Dots
    const dots = STEPS.map((_, i) =>
      `<div class="tt-dot${i === step ? ' tt-dot--active' : ''}"></div>`
    ).join('');

    const isLast = step === STEPS.length - 1;

    // Fade tooltip out, swap content, fade back in
    elTooltip.classList.add('tt-hidden');
    setTimeout(() => {
      elTooltip.innerHTML = `
        <div class="tt-icon">${s.icon}</div>
        <div class="tt-title">${s.title}</div>
        <div class="tt-body">${s.body}</div>
        <div class="tt-footer">
          <div class="tt-dots">${dots}</div>
          <div class="tt-btns">
            <button class="tt-skip" id="tt-skip">Skip</button>
            <button class="tt-next" id="tt-next">${isLast ? 'Done ✓' : 'Next →'}</button>
          </div>
        </div>
      `;

      positionTooltip(r);
      elTooltip.classList.remove('tt-hidden');

      document.getElementById('tt-next').addEventListener('click', () => { step++; showStep(); });
      document.getElementById('tt-skip').addEventListener('click', endTour);
    }, step === 0 ? 0 : 90);
  }

  function positionTooltip(targetRect) {
    const TT_W   = 280;
    const TT_H   = 180; // rough estimate
    const EDGE   = 14;
    const vw     = window.innerWidth;
    const vh     = window.innerHeight;
    const cx     = targetRect.left + targetRect.width / 2;

    // Horizontal: centre on element, clamp to screen
    let left = Math.max(EDGE, Math.min(cx - TT_W / 2, vw - TT_W - EDGE));

    // Vertical: above element if bottom half of screen, below if top half
    let top;
    if (targetRect.top > vh * 0.55) {
      top = targetRect.top - TT_H - 18;
    } else {
      top = targetRect.bottom + 16;
    }
    top = Math.max(EDGE, Math.min(top, vh - TT_H - EDGE));

    elTooltip.style.left = left + 'px';
    elTooltip.style.top  = top  + 'px';
  }

  /* ── End ──────────────────────────────────────── */
  function endTour() {
    localStorage.setItem('app_tour_done', '1');
    [elOverlay, elSpotlight, elTooltip].forEach(el => {
      if (!el) return;
      el.style.transition = 'opacity .2s ease';
      el.style.opacity = '0';
      setTimeout(() => el.remove(), 220);
    });
    const welcome = document.getElementById('tour-welcome');
    if (welcome) welcome.remove();
  }
})();
