/* ═══════════════════════════════════════════════
   EXPERIMATE — meet.js
   Meet page interactions.
   Tab switching is handled by main.js switchTab().
   This file handles meet-specific logic.
═══════════════════════════════════════════════ */

document.addEventListener('DOMContentLoaded', () => {
  highlightTodaysMeets();
});

/* ───────────────────────────────────────────────
   HIGHLIGHT TODAY'S MEETS
   If a meet is happening today, add a subtle accent
   border to make it stand out.
─────────────────────────────────────────────── */
function highlightTodaysMeets() {
  const today = new Date();
  const todayDay   = String(today.getDate());
  const todayMonth = today.toLocaleString('en', { month: 'short' }).toUpperCase();

  document.querySelectorAll('.meet-card').forEach(card => {
    const day   = card.querySelector('.meet-card__day')?.textContent?.trim();
    const month = card.querySelector('.meet-card__month')?.textContent?.trim();

    if (day === todayDay && month === todayMonth) {
      card.style.borderColor = 'var(--accent-border)';
      card.style.boxShadow   = '0 0 0 1px var(--accent-border)';
    }
  });
}