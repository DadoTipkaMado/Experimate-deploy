/* ═══════════════════════════════════════════════
   EXPERIMATE — map.js
   Leaflet map init, clustered pins, rich popups.
═══════════════════════════════════════════════ */

const MapState = {
  map: null,
  clusterGroup: null,
  allMarkers: [],       // { marker, listing, pinType } — full list for filtering
  dateFrom: null,       // Date | null
  dateTo: null,         // Date | null
  userCache: {},        // username → UserResponse (for popup photos)
  myMeetMap: {},        // listingId → 'my-meet' | 'due-soon'
  unlockedIds: new Set(), // listingIds where exact location is visible (own or accepted)
  userLat: null,
  userLng: null,
  locationMarker: null,
  activeCircle: null,   // the single radius circle currently shown (only on pin click)
};

/* ───────────────────────────────────────────────
   INIT
─────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
  initMap();
  loadPins();
});

function initMap() {
  MapState.map = L.map('map', {
    center:           [45.815, 15.977],
    zoom:             14,
    zoomControl:      false,
    attributionControl: false,
  });

  const tileUrl = () => document.body.classList.contains('theme-light') || document.body.classList.contains('theme-warm')
    ? 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png'
    : 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png';

  MapState.tileLayer = L.tileLayer(tileUrl(), { maxZoom: 19 }).addTo(MapState.map);

  new MutationObserver(() => MapState.tileLayer.setUrl(tileUrl()))
    .observe(document.body, { attributes: true, attributeFilter: ['class'] });

  MapState.clusterGroup = L.markerClusterGroup({
    maxClusterRadius: 50,
    spiderfyOnMaxZoom: true,
    showCoverageOnHover: false,
    zoomToBoundsOnClick: true,
  });
  MapState.map.addLayer(MapState.clusterGroup);

  L.control.zoom({ position: 'bottomleft' }).addTo(MapState.map);

  const flyToRaw = sessionStorage.getItem('mapFlyTo');
  const flyCity  = sessionStorage.getItem('mapSearchCity');
  if (flyToRaw) {
    try {
      const { lat, lng, zoom } = JSON.parse(flyToRaw);
      MapState.map.setView([lat, lng], zoom || 16);
    } catch (e) { /* ignore */ }
    sessionStorage.removeItem('mapFlyTo');
  } else if (flyCity) {
    sessionStorage.removeItem('mapSearchCity');
    fetch(`https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(flyCity)}&format=json&limit=1`)
      .then(r => r.json())
      .then(results => {
        if (results[0]) MapState.map.setView([parseFloat(results[0].lat), parseFloat(results[0].lon)], 14);
      })
      .catch(() => {});
  } else if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      pos => {
        const { latitude, longitude } = pos.coords;
        MapState.userLat = latitude;
        MapState.userLng = longitude;
        MapState.map.flyTo([latitude, longitude], 17, { duration: 1 });
        if (MapState.locationMarker) MapState.map.removeLayer(MapState.locationMarker);
        MapState.locationMarker = L.marker([latitude, longitude], {
          icon: L.divIcon({
            className: '',
            html: `<div class="user-location-pin"></div>`,
            iconSize:   [35, 35],
            iconAnchor: [17, 42],
          }),
          zIndexOffset: 1000,
        }).addTo(MapState.map);
      },
      () => showToast('Location access denied — showing Zagreb by default.', 'default')
    );
  }
}

/* ───────────────────────────────────────────────
   LOAD PINS
─────────────────────────────────────────────── */
function loadPins() {
  const myResPromise = Auth.getToken()
    ? ReservationAPI.getMine({ filter: 'joined', timeframe: 'upcoming' }).catch(() => [])
    : Promise.resolve([]);
  const myListingsPromise = Auth.getToken()
    ? TourListingAPI.getMine().catch(() => [])
    : Promise.resolve([]);
  const myAcceptedPromise = Auth.getToken()
    ? BookingRequestAPI.getMine({ flowDirection: 'outgoing', status: 'ACCEPTED' }).catch(() => [])
    : Promise.resolve([]);

  const partnerPinsPromise = PartnerPinAPI.getAll().catch(() => []);

  Promise.allSettled([TourListingAPI.getAll(), UserAPI.getAll(), myResPromise, myListingsPromise, myAcceptedPromise, partnerPinsPromise])
    .then(([listingsResult, usersResult, myResResult, myListingsResult, myAcceptedResult, partnerPinsResult]) => {
      const listings     = listingsResult.status     === 'fulfilled' ? (listingsResult.value     || []) : [];
      const users        = usersResult.status        === 'fulfilled' ? (usersResult.value        || []) : [];
      const myRes        = myResResult.status        === 'fulfilled' ? (myResResult.value        || []) : [];
      const myListings   = myListingsResult.status   === 'fulfilled' ? (myListingsResult.value   || []) : [];
      const myAccepted   = myAcceptedResult.status   === 'fulfilled' ? (myAcceptedResult.value   || []) : [];
      const partnerPins  = partnerPinsResult.status  === 'fulfilled' ? (partnerPinsResult.value  || []) : [];

      (users || []).forEach(u => { if (u.username) MapState.userCache[u.username] = u; });

      const now = new Date();
      const ONE_HOUR = 60 * 60 * 1000;
      myRes.filter(r => r.status !== 'CANCELLED').forEach(r => {
        const lid = r.tourListing?.id;
        if (!lid) return;
        const diff = new Date(r.tourListing.meetingDate) - now;
        if (diff <= 0) return;
        MapState.myMeetMap[lid] = diff <= ONE_HOUR ? 'due-soon' : 'my-meet';
        MapState.unlockedIds.add(lid);
      });
      myListings.forEach(l => MapState.unlockedIds.add(l.id));
      myAccepted.forEach(r => { if (r.tourListing?.id) MapState.unlockedIds.add(r.tourListing.id); });

      const seenIds = new Set();
      const allListings = [...listings, ...myListings];
      allListings.forEach(listing => {
        if (listing.lat == null || listing.lng == null) return;
        if (new Date(listing.meetingDate) < now) return;
        if (seenIds.has(listing.id)) return;
        const isFull = (listing.bookedCount ?? 0) >= (listing.maxGuests ?? 1);
        if (isFull && !MapState.unlockedIds.has(listing.id)) return;
        seenIds.add(listing.id);
        const pinType = MapState.myMeetMap[listing.id]
          ?? (listing.host?.role === 'PARTNER' ? 'partner' : 'default');
        const marker = buildMarker(listing, pinType);
        const circle = buildRadiusCircle(listing.lat, listing.lng, listing.radiusMeters);
        MapState.allMarkers.push({ marker, listing, pinType, circle });
        MapState.clusterGroup.addLayer(marker);
      });

      // Partner venue pins — separate layer, not clustered with TourListings
      partnerPins.filter(p => p.active !== false).forEach(pin => {
        if (pin.latitude == null || pin.longitude == null) return;
        const marker = buildPartnerPinMarker(pin);
        MapState.map.addLayer(marker);
      });

      const count = MapState.allMarkers.length;
      if (count > 0 && !localStorage.getItem('map_tour_seen')) {
        localStorage.setItem('map_tour_seen', '1');
        setTimeout(() => showToast(`${count} local experience${count !== 1 ? 's' : ''} nearby — tap any pin to explore`, 'success'), 1200);
      }

      // Restore overlay if user navigated back from a profile page
      const restoreRaw = sessionStorage.getItem('mapOverlayRestore');
      if (restoreRaw) {
        sessionStorage.removeItem('mapOverlayRestore');
        try {
          const { listing, lat, lng, zoom } = JSON.parse(restoreRaw);
          if (lat != null && lng != null) MapState.map.setView([lat, lng], zoom || 14, { animate: false });
          const isOwn    = !!(Auth.getUsername() && listing.host?.username === Auth.getUsername());
          const reserved = !!(MapState.myMeetMap[listing.id]);
          openListingDetail(listing, { isOwn, reserved, reqStatus: null });
        } catch (_) {}
      }
    });
}


// Leaflet renders to SVG/canvas where CSS var() can't resolve, so read the
// active theme's --accent hex at runtime.
function cssAccent() {
  return getComputedStyle(document.body).getPropertyValue('--accent').trim() || '#00c9a7';
}

function buildRadiusCircle(lat, lng, radiusMeters) {
  if (!radiusMeters) return null;
  const accent = cssAccent();
  return L.circle([lat, lng], {
    radius: radiusMeters,
    color: accent,
    weight: 1.5,
    opacity: 0.30,
    fillColor: accent,
    fillOpacity: 0.05,
    interactive: false,
  });
}

// Radius circles are shown one-at-a-time, only when a pin is tapped.
function showListingCircle(listing) {
  hideListingCircle();
  const entry = MapState.allMarkers.find(m => m.listing && m.listing.id === listing.id);
  if (!entry || !entry.circle) return;
  entry.circle.setStyle({ color: cssAccent(), fillColor: cssAccent() });
  entry.circle.addTo(MapState.map);
  entry.circle
    .bindTooltip('Meet is near here', { permanent: true, direction: 'center', className: 'map-radius-tip' })
    .openTooltip();
  MapState.activeCircle = entry.circle;
}

function hideListingCircle() {
  if (!MapState.activeCircle) return;
  MapState.activeCircle.unbindTooltip();
  MapState.map.removeLayer(MapState.activeCircle);
  MapState.activeCircle = null;
}

function buildMarker(listing, pinType = 'default') {
  // Backend returns masked coords when radiusMeters != null — always use as-is
  const [markerLat, markerLng] = [listing.lat, listing.lng];

  const pinClass = pinType === 'due-soon' ? 'map-pin--due-soon'
                 : pinType === 'my-meet'  ? 'map-pin--my-meet'
                 : pinType === 'partner'  ? 'map-pin--partner'
                 : 'map-pin--event';

  let iconHtml, iconSize, iconAnchor;
  if (pinType === 'partner' && listing.host?.profilePhotoUrl) {
    const photoUrl = listing.host.profilePhotoUrl.startsWith('/')
      ? listing.host.profilePhotoUrl
      : '/api/user/profile-photo/' + listing.host.profilePhotoUrl;
    iconHtml   = `<div class="map-pin--partner-logo"><img src="${photoUrl}" alt="" onerror="this.parentElement.innerHTML='<span class=\\"map-pin--partner-logo__initials\\">${(listing.host.firstName?.[0]??'P').toUpperCase()}</span>'"></div>`;
    iconSize   = [32, 32];
    iconAnchor = [16, 32];
  } else if (pinType === 'partner') {
    const initial = (listing.host?.firstName?.[0] ?? listing.host?.username?.[0] ?? 'P').toUpperCase();
    iconHtml   = `<div class="map-pin--partner-logo"><span class="map-pin--partner-logo__initials">${initial}</span></div>`;
    iconSize   = [32, 32];
    iconAnchor = [16, 32];
  } else {
    iconHtml   = `<div class="map-pin ${pinClass}"></div>`;
    iconSize   = [14, 14];
    iconAnchor = [7, 14];
  }

  const icon = L.divIcon({
    className: '',
    html: iconHtml,
    iconSize,
    iconAnchor,
  });

  const marker = L.marker([markerLat, markerLng], { icon });
  marker.on('click', () => openMapPopup(listing, pinType));
  return marker;
}

function buildPartnerPinMarker(pin) {
  const hl = pin.highlighted === true;
  const pinClass = `map-pin--partner-logo${hl ? ' map-pin--partner-logo--highlighted' : ''}`;
  const badgeHtml = hl ? `<div class="map-pin--highlighted-badge">FEATURED</div>` : '';
  const wrapStyle = hl ? 'position:relative;display:inline-block;' : '';

  let innerHtml;
  if (pin.logoUrl) {
    innerHtml = `<img src="${escapeHtml(pin.logoUrl)}" alt="" onerror="this.parentElement.innerHTML='<span class=\\"map-pin--partner-logo__initials\\">${(pin.name?.[0] ?? 'P').toUpperCase()}</span>'">`;
  } else {
    innerHtml = `<span class="map-pin--partner-logo__initials">${(pin.name?.[0] ?? 'P').toUpperCase()}</span>`;
  }

  const iconHtml = `<div style="${wrapStyle}"><div class="${pinClass}">${innerHtml}</div>${badgeHtml}</div>`;
  const size = hl ? [54, 54] : [36, 36];
  const anchor = hl ? [27, 54] : [18, 36];
  const icon = L.divIcon({ className: '', html: iconHtml, iconSize: size, iconAnchor: anchor });
  const marker = L.marker([pin.latitude, pin.longitude], { icon, zIndexOffset: hl ? 200 : 0 });
  marker.on('click', () => openPartnerPinPopup(pin));
  return marker;
}

function openPartnerPinPopup(pin) {
  const logoHtml = pin.logoUrl
    ? `<img src="${escapeHtml(pin.logoUrl)}" style="width:36px;height:36px;border-radius:10px;object-fit:cover;flex-shrink:0;" alt="">`
    : `<div style="width:36px;height:36px;border-radius:10px;background:rgba(37,99,235,0.18);border:1px solid rgba(37,99,235,0.35);display:flex;align-items:center;justify-content:center;font-weight:800;font-size:15px;color:#60a5fa;flex-shrink:0;">${(pin.name?.[0] ?? 'P').toUpperCase()}</div>`;

  const body = document.getElementById('map-popup-body');
  body.innerHTML = `
    <div style="display:flex;align-items:center;gap:10px;margin-bottom:10px;">
      ${logoHtml}
      <div>
        <div class="popup-name" style="margin:0;">${escapeHtml(pin.name)}</div>
        ${pin.partnerCompanyName ? `<div style="font-size:10px;color:var(--text-3);margin-top:2px;">${escapeHtml(pin.partnerCompanyName)}</div>` : ''}
      </div>
    </div>
    <div style="display:flex;align-items:center;gap:6px;margin-bottom:10px;">
      <div style="display:inline-flex;align-items:center;gap:5px;background:rgba(37,99,235,0.12);border:1px solid rgba(37,99,235,0.28);border-radius:6px;padding:3px 8px;font-size:10px;color:#60a5fa;letter-spacing:0.08em;font-weight:700;">PARTNER VENUE</div>
      ${pin.highlighted ? `<div style="display:inline-flex;align-items:center;gap:4px;background:rgba(37,99,235,0.22);border:1px solid rgba(96,165,250,0.55);border-radius:6px;padding:3px 8px;font-size:10px;color:#93c5fd;letter-spacing:0.08em;font-weight:700;"><span style="width:6px;height:6px;border-radius:50%;background:#60a5fa;box-shadow:0 0 6px rgba(96,165,250,0.9);display:inline-block;flex-shrink:0;"></span>FEATURED</div>` : ''}
    </div>
    ${pin.description ? `<div class="popup-desc">${escapeHtml(pin.description)}</div>` : ''}
    <div id="pin-events-list" style="margin-top:10px;"><div style="font-size:10px;color:var(--text-3);">Loading events…</div></div>
  `;
  document.getElementById('map-popup-footer').innerHTML = '';
  document.getElementById('map-bottom-sheet').classList.add('map-bs--open');
  document.getElementById('map-date-panel').classList.remove('map-date-panel--open');
  document.getElementById('pill-date')?.classList.remove('pill--active');

  PartnerEventAPI.listForPin(pin.id).then(events => {
    const el = document.getElementById('pin-events-list');
    if (!el) return;
    if (!events || events.length === 0) {
      el.innerHTML = `<div style="font-size:10px;color:var(--text-3);">No upcoming events at this venue.</div>`;
      return;
    }
    el.innerHTML = `<div style="font-size:9px;color:var(--text-3);letter-spacing:0.1em;text-transform:uppercase;margin-bottom:8px;">Upcoming events</div>` +
      events.map(ev => {
        const start = new Date(ev.startDatetime);
        const dateStr = start.toLocaleDateString('en', { month: 'short', day: 'numeric' });
        const timeStr = start.toLocaleTimeString('en', { hour: '2-digit', minute: '2-digit' });
        const ticketBtn = ev.ticketVendorUrl
          ? `<button onclick="window.open('${escapeHtml(ev.ticketVendorUrl)}','_blank','noopener')" style="background:#2563eb;color:#fff;border:none;border-radius:8px;padding:4px 10px;font-size:10px;font-weight:700;cursor:pointer;white-space:nowrap;flex-shrink:0;">Tickets</button>`
          : '';
        const evJson = escapeHtml(JSON.stringify({ id: ev.id, title: ev.title, startDatetime: ev.startDatetime, pinLatitude: ev.pinLatitude, pinLongitude: ev.pinLongitude }));
        const joinBtn = Auth.getToken()
          ? `<button onclick="openJoinAsHostForm(JSON.parse(decodeURIComponent('${encodeURIComponent(JSON.stringify({ id: ev.id, title: ev.title, startDatetime: ev.startDatetime, pinLatitude: ev.pinLatitude, pinLongitude: ev.pinLongitude }))}')),this)" style="background:var(--accent);color:#000;border:none;border-radius:8px;padding:4px 10px;font-size:10px;font-weight:700;cursor:pointer;white-space:nowrap;flex-shrink:0;">Join as host</button>`
          : '';
        return `
          <div style="display:flex;align-items:flex-start;gap:8px;padding:8px 0;border-bottom:1px solid rgba(255,255,255,0.05);">
            <div style="min-width:38px;text-align:center;background:rgba(37,99,235,0.12);border:1px solid rgba(37,99,235,0.22);border-radius:8px;padding:4px 0;">
              <div style="font-size:14px;font-weight:800;color:#60a5fa;line-height:1;">${start.getDate()}</div>
              <div style="font-size:8px;color:rgba(96,165,250,0.65);text-transform:uppercase;">${dateStr.split(' ')[0]}</div>
            </div>
            <div style="flex:1;min-width:0;">
              <div style="font-size:12px;color:var(--text);font-weight:500;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(ev.title)}</div>
              <div style="font-size:10px;color:var(--text-3);margin-top:1px;">${timeStr}</div>
            </div>
            <div style="display:flex;gap:5px;flex-shrink:0;">
              ${ticketBtn}
              ${joinBtn}
            </div>
          </div>`;
      }).join('');
  }).catch(() => {
    const el = document.getElementById('pin-events-list');
    if (el) el.innerHTML = '';
  });
}

function openJoinAsHostForm(ev, triggerBtn) {
  if (triggerBtn) { triggerBtn.disabled = true; triggerBtn.textContent = '…'; }
  const body   = document.getElementById('map-popup-body');
  const footer = document.getElementById('map-popup-footer');
  const startIso = ev.startDatetime?.slice(0, 16) ?? '';

  body.innerHTML = `
    <div style="display:flex;align-items:center;gap:8px;margin-bottom:14px;">
      <button id="jah-back" style="background:none;border:none;color:var(--text-3);cursor:pointer;padding:0;display:flex;align-items:center;gap:4px;font-size:11px;">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="15 18 9 12 15 6"/></svg>
        Back
      </button>
      <div style="font-family:var(--font-display);font-weight:800;font-size:15px;color:var(--text);">Join as host</div>
    </div>
    <div style="font-size:11px;color:var(--text-3);margin-bottom:12px;line-height:1.55;">
      Creating a Meet linked to: <strong style="color:var(--text);">${escapeHtml(ev.title)}</strong>
    </div>

    <div style="display:flex;flex-direction:column;gap:10px;">
      <div>
        <div style="font-size:9px;color:var(--text-2);letter-spacing:0.08em;text-transform:uppercase;margin-bottom:5px;">City *</div>
        <input id="jah-city" type="text" placeholder="e.g. Zagreb" autocomplete="address-level2" style="background:rgba(0,0,0,0.28);border:1px solid rgba(255,255,255,0.07);border-top-color:rgba(0,0,0,0.45);border-radius:10px;padding:10px 12px;color:var(--text);font-family:var(--font-mono);font-size:12px;width:100%;box-sizing:border-box;box-shadow:inset 0 1px 4px rgba(0,0,0,0.38);">
      </div>
      <div>
        <div style="font-size:9px;color:var(--text-2);letter-spacing:0.08em;text-transform:uppercase;margin-bottom:5px;">Description *</div>
        <textarea id="jah-desc" placeholder="What will you offer guests at this event?" rows="3" style="background:rgba(0,0,0,0.28);border:1px solid rgba(255,255,255,0.07);border-top-color:rgba(0,0,0,0.45);border-radius:10px;padding:10px 12px;color:var(--text);font-family:var(--font-mono);font-size:12px;width:100%;box-sizing:border-box;resize:vertical;line-height:1.5;box-shadow:inset 0 1px 4px rgba(0,0,0,0.38);"></textarea>
      </div>
      <div>
        <div style="font-size:9px;color:var(--text-2);letter-spacing:0.08em;text-transform:uppercase;margin-bottom:5px;">Max guests *</div>
        <input id="jah-guests" type="number" value="2" min="1" max="20" style="background:rgba(0,0,0,0.28);border:1px solid rgba(255,255,255,0.07);border-top-color:rgba(0,0,0,0.45);border-radius:10px;padding:10px 12px;color:var(--text);font-family:var(--font-mono);font-size:12px;width:100%;box-sizing:border-box;box-shadow:inset 0 1px 4px rgba(0,0,0,0.38);">
      </div>
      <div>
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:5px;">
          <div style="font-size:9px;color:var(--text-2);letter-spacing:0.08em;text-transform:uppercase;">Meeting time</div>
          <label style="display:flex;align-items:center;gap:5px;font-size:9px;color:var(--text-3);cursor:pointer;">
            <input type="checkbox" id="jah-override-time" onchange="document.getElementById('jah-time-row').style.display=this.checked?'block':'none'"> Use different time
          </label>
        </div>
        <div style="font-size:11px;color:var(--text-3);">${escapeHtml(startIso.replace('T', ' '))}</div>
        <div id="jah-time-row" style="display:none;margin-top:8px;">
          <input id="jah-date" type="datetime-local" value="${escapeHtml(startIso)}" style="background:rgba(0,0,0,0.28);border:1px solid rgba(255,255,255,0.07);border-top-color:rgba(0,0,0,0.45);border-radius:10px;padding:10px 12px;color:var(--text);font-family:var(--font-mono);font-size:12px;width:100%;box-sizing:border-box;box-shadow:inset 0 1px 4px rgba(0,0,0,0.38);">
        </div>
      </div>
    </div>
    <div id="jah-error" style="display:none;background:rgba(255,80,80,0.08);border:1px solid rgba(255,80,80,0.22);border-radius:8px;padding:8px 12px;font-size:11px;color:rgba(255,110,110,0.9);margin-top:10px;"></div>
  `;

  footer.innerHTML = `<button class="popup-action" style="width:100%;background:var(--accent);color:#000;" id="jah-submit">Create Meet →</button>`;

  document.getElementById('jah-back').addEventListener('click', closeMapPopup);
  document.getElementById('jah-submit').addEventListener('click', async () => {
    const city    = document.getElementById('jah-city').value.trim();
    const desc    = document.getElementById('jah-desc').value.trim();
    const guests  = parseInt(document.getElementById('jah-guests').value, 10);
    const useTime = document.getElementById('jah-override-time').checked;
    const dateVal = useTime ? document.getElementById('jah-date').value : null;
    const errEl   = document.getElementById('jah-error');
    const btn     = document.getElementById('jah-submit');

    errEl.style.display = 'none';
    if (!city)        { errEl.textContent = 'City is required.';              errEl.style.display = 'block'; return; }
    if (!desc)        { errEl.textContent = 'Description is required.';       errEl.style.display = 'block'; return; }
    if (!guests || guests < 1) { errEl.textContent = 'Enter a valid guest count.'; errEl.style.display = 'block'; return; }
    if (useTime && (!dateVal || new Date(dateVal) < new Date())) {
      errEl.textContent = 'Override date must be in the future.'; errEl.style.display = 'block'; return;
    }

    btn.disabled = true; btn.textContent = 'Creating…';

    const dto = {
      partnerEventId: ev.id,
      city,
      tourDescription: desc,
      maxGuests: guests,
      overrideMeetingDate: useTime && dateVal ? dateVal + ':00' : null,
      overrideLatitude: null,
      overrideLongitude: null,
    };

    try {
      await TourListingAPI.createFromEvent(dto);
      closeMapPopup();
      showToast('Meet created! Check My Meets.', 'success');
      setTimeout(() => { window.location.href = '/tours'; }, 1200);
    } catch (e) {
      errEl.textContent = e.message || 'Could not create Meet.';
      errEl.style.display = 'block';
      btn.disabled = false; btn.textContent = 'Create Meet →';
    }
  });
}

function openMapPopup(listing, pinType = 'default') {
  MapState._popupListing  = listing;
  MapState._popupPinType  = pinType;
  MapState._popupUnlocked = MapState.unlockedIds.has(listing.id);
  showListingCircle(listing);   // reveal the meet's radius only now (on pin tap)
  document.getElementById('map-popup-body').innerHTML = buildPopupContent(listing, pinType, MapState._popupUnlocked);
  document.getElementById('map-popup-footer').innerHTML = `<button class="popup-action" style="width:100%;" onclick="openListingDetailFromMap()">See listing →</button>`;
  document.getElementById('map-bottom-sheet').classList.add('map-bs--open');
  // Close date panel if open
  document.getElementById('map-date-panel').classList.remove('map-date-panel--open');
  document.getElementById('pill-date')?.classList.remove('pill--active');
}

function closeMapPopup() {
  document.getElementById('map-bottom-sheet').classList.remove('map-bs--open');
  hideListingCircle();
}

function openListingDetailFromMap() {
  const listing = MapState._popupListing;
  if (!listing) return;
  const isOwn     = !!(Auth.getUsername() && listing.host?.username === Auth.getUsername());
  const reserved  = !!(MapState.myMeetMap[listing.id]);
  const unlocked  = MapState.unlockedIds.has(listing.id);
  const reqStatus = unlocked && !reserved && !isOwn ? 'ACCEPTED' : null;
  closeMapPopup();
  openListingDetail(listing, { isOwn, reserved, reqStatus });
}

function centerOnUser() {
  if (MapState.userLat !== null) {
    MapState.map.flyTo([MapState.userLat, MapState.userLng], 17, { duration: 1 });
    return;
  }
  if (!navigator.geolocation) return;
  navigator.geolocation.getCurrentPosition(
    pos => {
      const { latitude, longitude } = pos.coords;
      MapState.userLat = latitude;
      MapState.userLng = longitude;
      MapState.map.flyTo([latitude, longitude], 17, { duration: 1 });
      if (MapState.locationMarker) MapState.map.removeLayer(MapState.locationMarker);
      MapState.locationMarker = L.marker([latitude, longitude], {
        icon: L.divIcon({
          className: '',
          html: `<div class="user-location-pin"></div>`,
          iconSize:   [35, 35],
          iconAnchor: [17, 42],
        }),
        zIndexOffset: 1000,
      }).addTo(MapState.map);
    },
    () => showToast('Location access denied.', 'error')
  );
}

function buildPopupContent(listing, pinType = 'default', unlocked = false) {
  const dateStr = `${fmtDate(listing.meetingDate)} ${new Date(listing.meetingDate).getFullYear()} · ${fmtTime(listing.meetingDate)}`;
  const hostName   = listing.host ? listing.host.firstName + ' ' + listing.host.lastName : '';
  const hostHandle = listing.host?.username ?? '';
  const maxG       = listing.maxGuests ?? 1;
  const curG       = listing.bookedCount ?? 0;
  const isFull     = curG >= maxG;
  const dotColor   = isFull ? 'rgba(239,239,239,0.3)' : 'var(--accent)';
  const dotGlow    = isFull ? '' : 'box-shadow:0 0 5px var(--accent);';
  const statusLabel = isFull
    ? (maxG > 1 ? 'Full' : 'Booked')
    : maxG > 1 ? `${maxG - curG} spot${maxG - curG !== 1 ? 's' : ''} left` : 'Available';
  const guestHtml  = `<div class="popup-date">👥 ${curG}/${maxG} spots taken</div>`;

  let hostHtml = '';
  if (hostName) {
    const avatar = userAvatar(hostHandle, 28, MapState.userCache[hostHandle]);
    hostHtml = `<div class="popup-host" style="display:flex;align-items:center;gap:7px;">${avatar}<span>${escapeHtml(hostName)}<span style="color:var(--text-3);font-size:12px;margin-left:4px;">@${escapeHtml(hostHandle)}</span></span></div>`;
  }

  const MAX_DESC = 160;
  const fullDesc  = listing.tourDescription ?? '';
  const descHtml  = fullDesc
    ? `<div class="popup-desc">${escapeHtml(fullDesc.slice(0, MAX_DESC))}${fullDesc.length > MAX_DESC
        ? `<span style="color:var(--accent);cursor:pointer;font-weight:600;" onclick="openListingDetailFromMap()"> …more</span>`
        : ''}</div>`
    : '';

  let badgeHtml = '';
  if (pinType === 'due-soon') {
    badgeHtml = `<div style="display:inline-flex;align-items:center;gap:5px;background:rgba(245,158,11,0.15);border:1px solid #f59e0b;border-radius:6px;padding:3px 8px;font-size:11px;color:#f59e0b;letter-spacing:0.08em;font-weight:700;">⏱ DUE SOON</div>`;
  } else if (pinType === 'my-meet') {
    badgeHtml = `<div style="display:inline-flex;align-items:center;gap:5px;background:rgba(168,85,247,0.15);border:1px solid #a855f7;border-radius:6px;padding:3px 8px;font-size:11px;color:#a855f7;letter-spacing:0.08em;font-weight:700;">✓ GOING</div>`;
  }

  const cityLabel = listing.radiusMeters == null ? escapeHtml(listing.city) : `Near ${escapeHtml(listing.city)}`;

  return `
    <div class="popup-name">${cityLabel}</div>
    ${badgeHtml}
    ${hostHtml}
    <div class="popup-date">📅 ${dateStr}</div>
    ${guestHtml}
    <div class="popup-status">
      <div class="popup-status__dot" style="background:${dotColor};${dotGlow}"></div>
      <div class="popup-status__label" style="color:${dotColor};">${statusLabel}</div>
    </div>
    ${descHtml}
  `;
}

/* ───────────────────────────────────────────────
   FILTERS
─────────────────────────────────────────────── */


function applyMarkerFilter() {
  // Collect active proximity state — defined later in file, safe at call time
  const activePoiKeys = typeof _poiActive !== 'undefined'
    ? Object.keys(_poiActive).filter(k => _poiActive[k])
    : [];
  const allVenues = activePoiKeys.flatMap(k => (_venueCoords?.[k]) || []);

  MapState.clusterGroup.clearLayers();
  MapState.allMarkers.forEach(({ circle }) => { if (circle) MapState.map.removeLayer(circle); });

  MapState.allMarkers.forEach(({ marker, listing, circle }) => {
    // Date filter
    if (MapState.dateFrom || MapState.dateTo) {
      const d = new Date(listing.meetingDate);
      if (MapState.dateFrom && d < MapState.dateFrom) return;
      if (MapState.dateTo && d > MapState.dateTo) return;
    }
    // Proximity (venue) filter — composable with date filter in a single pass
    if (activePoiKeys.length) {
      const mLat = listing.lat ?? listing.latitude;
      const mLng = listing.lng ?? listing.longitude;
      if (mLat == null || mLng == null) return;
      if (!allVenues.some(v => _haversine(mLat, mLng, v.lat, v.lng) <= PROXIMITY_M)) return;
    }
    MapState.clusterGroup.addLayer(marker);
  });
}

/* ── Date filter ── */
window.toggleDatePanel = function(btn) {
  const panel = document.getElementById('map-date-panel');
  const open  = panel.classList.toggle('map-date-panel--open');
  btn.classList.toggle('pill--active', open);
  // Close venue dropdown if open
  document.getElementById('map-venue-dropdown')?.classList.remove('map-venue-dropdown--open');
  document.getElementById('pill-venues')?.classList.remove('pill--active');
  closeMapPopup();
};

window.applyDateFilter = function() {
  const fromVal = document.getElementById('date-from').value;
  const toVal   = document.getElementById('date-to').value;
  if (fromVal && toVal && toVal < fromVal) {
    showToast('End date must be after start date.', 'error');
    return;
  }
  MapState.dateFrom = fromVal ? new Date(fromVal + 'T00:00:00') : null;
  MapState.dateTo   = toVal   ? new Date(toVal   + 'T23:59:59') : null;
  const hasFilter = !!(fromVal || toVal);
  document.getElementById('pill-date').classList.toggle('pill--active', hasFilter);
  document.getElementById('map-date-panel').classList.remove('map-date-panel--open');
  if (!hasFilter) document.getElementById('pill-date').classList.remove('pill--active');
  applyMarkerFilter();
  const count = MapState.allMarkers.filter(({ marker }) => MapState.clusterGroup.hasLayer(marker)).length;
  showToast(`${count} listing${count !== 1 ? 's' : ''} in selected range`, 'default');
};

window.clearDateFilter = function() {
  document.getElementById('date-from').value = '';
  document.getElementById('date-to').value   = '';
  MapState.dateFrom = null;
  MapState.dateTo   = null;
  document.getElementById('pill-date').classList.remove('pill--active');
  document.getElementById('map-date-panel').classList.remove('map-date-panel--open');
  applyMarkerFilter();
};

/* ── Venue strip collapse ── */
window.toggleVenueStrip = function() {
  document.getElementById('map-venues-strip').classList.toggle('map-venues-strip--collapsed');
};

/* ───────────────────────────────────────────────
   SEARCH — pin filter only
─────────────────────────────────────────────── */
const searchInput = document.getElementById('map-search-input');
if (searchInput) {
  searchInput.addEventListener('input', e => {
    const lower = e.target.value.trim().toLowerCase();
    MapState.clusterGroup.clearLayers();
    MapState.allMarkers.forEach(({ circle }) => { if (circle) MapState.map.removeLayer(circle); });
    MapState.allMarkers.forEach(({ marker, listing, circle }) => {
      if (!lower) {
        MapState.clusterGroup.addLayer(marker);
        return;
      }
      const text = [listing.city, listing.host?.firstName, listing.host?.lastName, listing.host?.username]
        .filter(Boolean).join(' ').toLowerCase();
      if (text.includes(lower)) {
        MapState.clusterGroup.addLayer(marker);
      }
    });
  });
}

/* ── Geo search — fly to city via Nominatim ── */
const geoInput = document.getElementById('map-geo-input');
if (geoInput) {
  geoInput.addEventListener('keydown', async e => {
    if (e.key !== 'Enter') return;
    const query = geoInput.value.trim();
    if (!query) return;
    geoInput.disabled = true;
    try {
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query)}&format=json&limit=1`,
        { headers: { 'Accept-Language': 'en' } }
      );
      const results = await res.json();
      if (!results.length) { showToast(`No location found for "${query}"`, 'error'); return; }
      const { lat, lon, display_name } = results[0];
      MapState.map.flyTo([parseFloat(lat), parseFloat(lon)], 13, { duration: 1.2 });
      geoInput.blur();
    } catch {
      showToast('Could not reach geocoding service.', 'error');
    } finally {
      geoInput.disabled = false;
    }
  });
}

/* ═══════════════════════════════════════════════
   POI LAYER — Overpass API venue filtering
═══════════════════════════════════════════════ */

const _POI = {
  food:     { label: 'Food & Drink',    color: '#f97316', emoji: '🍽️', filters: [['amenity','restaurant'],['amenity','fast_food'],['amenity','food_court'],['amenity','cafe']] },
  coffee:   { label: 'Coffee',          color: '#f59e0b', emoji: '☕', filters: [['amenity','cafe']] },
  nightlife:{ label: 'Bars & Nightlife',color: '#ef4444', emoji: '🍺', filters: [['amenity','bar'],['amenity','nightclub'],['amenity','pub']] },
  culture:  { label: 'Culture',         color: '#8b5cf6', emoji: '🏛️', filters: [['tourism','museum'],['tourism','gallery'],['amenity','arts_centre']] },
  outdoors: { label: 'Parks & Outdoors',color: '#22c55e', emoji: '🌿', filters: [['leisure','park'],['leisure','garden'],['leisure','nature_reserve'],['boundary','national_park']] },
  sports:   { label: 'Sports',          color: '#06b6d4', emoji: '🏟️', filters: [['leisure','stadium'],['leisure','sports_centre'],['leisure','swimming_pool'],['leisure','fitness_centre']] },
  arts:     { label: 'Arts & Theatre',  color: '#f472b6', emoji: '🎭', filters: [['amenity','theatre'],['amenity','cinema'],['amenity','music_venue']] },
  history:  { label: 'History',         color: '#a16207', emoji: '🏰', filters: [['historic','monument'],['historic','memorial'],['historic','castle'],['tourism','attraction'],['historic','ruins']] },
  market:   { label: 'Markets',         color: '#10b981', emoji: '🛒', filters: [['amenity','marketplace'],['shop','mall'],['amenity','food_court']] },
};

const _poiLayers   = {};   // catKey → L.LayerGroup
const _poiActive   = {};   // catKey → bool
let   _poiDebounce = null;
let   _poiAbort    = null;

window.toggleVenueDropdown = function(pill) {
  const dropdown = document.getElementById('map-venue-dropdown');
  const isOpen = dropdown.classList.toggle('map-venue-dropdown--open');
  pill.classList.toggle('pill--active', isOpen);
  // Close date panel if open
  document.getElementById('map-date-panel').classList.remove('map-date-panel--open');
  document.getElementById('pill-date')?.classList.remove('pill--active');
};

window.togglePoiFilter = function(btn, catKey) {
  const isNowActive = !_poiActive[catKey];
  _poiActive[catKey] = isNowActive;
  btn.classList.toggle('pill--active', isNowActive);

  if (!isNowActive) {
    _clearPoiLayer(catKey);
    _updateLegendVenues();
    _applyProximityFilter();
    return;
  }
  _fetchPoisForActive();
  _updateLegendVenues();
};

function _clearPoiLayer(catKey) {
  if (_poiLayers[catKey]) {
    MapState.map.removeLayer(_poiLayers[catKey]);
    delete _poiLayers[catKey];
  }
  delete _venueCoords[catKey];
}

// Haversine distance in metres between two lat/lng points
function _haversine(lat1, lng1, lat2, lng2) {
  const R = 6371000;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLng = (lng2 - lng1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

// Venue coords fetched per category: catKey → [{lat, lng}]
const _venueCoords = {};
const PROXIMITY_M  = 250; // meets within this distance of a venue are shown

function _updateLegendVenues() {
  const el = document.getElementById('legend-venues');
  if (!el) return;
  const active = Object.keys(_poiActive).filter(k => _poiActive[k]);
  if (!active.length) { el.innerHTML = ''; return; }
  el.innerHTML = `<div class="map-legend__section">Showing meets near</div>` +
    active.map(k => {
      const cat = _POI[k];
      return `<div class="map-legend__row"><div class="map-legend__dot" style="background:${cat.color};opacity:1;"></div>${cat.emoji} ${cat.label}</div>`;
    }).join('');
}

function _applyProximityFilter() {
  const activeKeys = Object.keys(_poiActive).filter(k => _poiActive[k]);

  if (!activeKeys.length) {
    // No filter active — show all meets (radius circles stay hidden until a pin is tapped)
    MapState.allMarkers.forEach(({ marker }) => {
      if (!MapState.clusterGroup.hasLayer(marker)) MapState.clusterGroup.addLayer(marker);
    });
    return;
  }

  // Gather all venue coords across active categories
  const allVenues = activeKeys.flatMap(k => _venueCoords[k] || []);

  MapState.allMarkers.forEach(({ marker, listing, circle }) => {
    const mLat = listing.lat ?? listing.latitude;
    const mLng = listing.lng ?? listing.longitude;
    if (mLat == null || mLng == null) {
      MapState.clusterGroup.removeLayer(marker);
      if (circle) MapState.map.removeLayer(circle);
      return;
    }
    const near = allVenues.some(v => _haversine(mLat, mLng, v.lat, v.lng) <= PROXIMITY_M);
    if (near) {
      if (!MapState.clusterGroup.hasLayer(marker)) MapState.clusterGroup.addLayer(marker);
    } else {
      MapState.clusterGroup.removeLayer(marker);
      if (circle) MapState.map.removeLayer(circle);
    }
  });
}

function _fetchPoisForActive() {
  clearTimeout(_poiDebounce);
  _poiDebounce = setTimeout(_doFetchPois, 700);
}

async function _doFetchPois() {
  const activeKeys = Object.keys(_poiActive).filter(k => _poiActive[k]);
  if (!activeKeys.length) { _applyProximityFilter(); return; }

  const zoom = MapState.map.getZoom();
  if (zoom < 11) {
    showToast('Zoom in closer to filter by venue type.', 'default');
    return;
  }

  // Expand bounds slightly for better coverage
  const b    = MapState.map.getBounds().pad(0.3);
  const bbox = `${b.getSouth()},${b.getWest()},${b.getNorth()},${b.getEast()}`;

  // Only fetch categories whose coords we don't have yet for this view
  const toFetch = activeKeys;
  const queryParts = toFetch.flatMap(k =>
    _POI[k].filters.flatMap(([key, val]) => [
      `node["${key}"="${val}"](${bbox});`,
      `way["${key}"="${val}"](${bbox});`,
    ])
  ).join('');
  const query = `[out:json][timeout:20];(${queryParts});out center;`;

  if (_poiAbort) _poiAbort.abort();
  _poiAbort = new AbortController();
  const _poiTimeout = setTimeout(() => _poiAbort.abort(), 15000);

  try {
    const res  = await fetch(
      'https://overpass-api.de/api/interpreter',
      { method: 'POST', body: query, signal: _poiAbort.signal }
    );
    const data = await res.json();

    // Clear old venue layers
    activeKeys.forEach(k => _clearPoiLayer(k));

    // Store venue coords and build dim background markers
    activeKeys.forEach(catKey => {
      const cat   = _POI[catKey];
      const coords = [];
      const group  = L.layerGroup().addTo(MapState.map);
      _poiLayers[catKey] = group;

      data.elements.forEach(el => {
        if (!_elementMatchesCat(el, catKey)) return;
        const lat = el.lat ?? el.center?.lat;
        const lng = el.lon ?? el.center?.lon;
        if (lat == null || lng == null) return;
        coords.push({ lat, lng });

        // Small venue pin — clickable, shows venue name on hover
        const venueName = el.tags?.name;
        const venueIcon = L.divIcon({
          className: '',
          html: `<div style="width:10px;height:14px;border-radius:50% 50% 50% 0;transform:rotate(-45deg);background:${cat.color};border:2px solid rgba(255,255,255,0.55);box-shadow:0 1px 6px rgba(0,0,0,0.45);cursor:pointer;"></div>`,
          iconSize: [10, 14], iconAnchor: [5, 14],
        });
        const venueMarker = L.marker([lat, lng], { icon: venueIcon, zIndexOffset: -200 });
        if (venueName) venueMarker.bindTooltip(venueName, { permanent: false, direction: 'top', offset: [0, -16], className: 'poi-tooltip' });
        group.addLayer(venueMarker);
      });

      _venueCoords[catKey] = coords;
    });

    _applyProximityFilter();
  } catch (err) {
    if (err.name !== 'AbortError') showToast('Could not load venue data.', 'error');
  } finally {
    clearTimeout(_poiTimeout);
  }
}

function _elementMatchesCat(el, catKey) {
  const tags = el.tags || {};
  return _POI[catKey].filters.some(([key, val]) => tags[key] === val);
}

// Refresh POI markers when the map moves (debounced)
document.addEventListener('DOMContentLoaded', () => {
  setTimeout(() => {
    if (!MapState.map) return;
    MapState.map.on('moveend zoomend', () => {
      if (Object.values(_poiActive).some(Boolean)) _fetchPoisForActive();
    });
  }, 1000);
});

/* ───────────────────────────────────────────────
   PUBLIC API — websocket.js uses addPin()
─────────────────────────────────────────────── */
window.MapAPI = {
  addPin: (pin) => {
    const listing = {
      lat: pin.lat, lng: pin.lng, city: pin.name,
      meetingDate: new Date().toISOString(), host: null,
      bookedCount: 0, maxGuests: 1,
    };
    const marker = buildMarker(listing);
    MapState.allMarkers.push({ marker, listing, circle: null });
    MapState.clusterGroup.addLayer(marker);
  },
};

