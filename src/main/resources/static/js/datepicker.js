/* Shared date picker — opens native <input type="date">, writes back as DD/MM/YYYY.
   Supports { min: Date } for future-only pickers and { max: Date } for age-gated ones. */
const DatePicker = {
  open(el, opts) {
    const inp = document.createElement('input');
    inp.type = 'date';
    const toISO = d => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    if (opts) {
      if (opts.min instanceof Date) inp.min = toISO(opts.min);
      if (opts.max instanceof Date) inp.max = toISO(opts.max);
    }
    Object.assign(inp.style, {
      position: 'fixed', opacity: '0', pointerEvents: 'none',
      width: '0', height: '0', top: '0', left: '0'
    });
    const cleanup = () => { if (document.body.contains(inp)) document.body.removeChild(inp); };
    inp.addEventListener('change', function () {
      if (!this.value) { cleanup(); return; }
      const [y, m, d] = this.value.split('-');
      el.value = `${d}/${m}/${y}`;
      el.dispatchEvent(new Event('change'));
      cleanup();
    });
    inp.addEventListener('blur', () => setTimeout(cleanup, 300));
    document.body.appendChild(inp);
    inp.focus();
    try { inp.showPicker(); } catch (_) { inp.click(); }
  }
};
