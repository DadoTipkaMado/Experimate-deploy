# Experimate — Claude Instructions

## Team split
- **Vito** → frontend + view controllers (`vito/frontend-clean` branch)
- **David** → backend only (`david/backend` branch)
- David merges both into `main` for testing

## Frontend rules (stupidly dumb frontend)
1. **UI + map only.** No business logic on the frontend.
2. **Never hash passwords on the frontend.** HTTPS handles transit, Spring handles hashing.
3. **Age verification** — date of birth input, check 18+ client-side only as UX (not security).
4. **Sign in with Google** — do not touch until David wires Spring backend to support it.
5. **Map is Vito's domain** — full freedom here, this is the one purely frontend feature.
6. **Send data to backend, don't think too much.** Forms submit, JS fetches, templates render.

## Claude's role
- Help Vito with **frontend + view controllers** (`src/main/resources/` + `src/main/java/.../view/`).
- Do NOT edit Java files outside `view/` unless Vito explicitly gives permission.
- Reading any backend file for context is always fine.

## Git workflow (agreed with David)
- Vito commits only `src/main/resources/` and `src/main/java/.../view/` to `vito/frontend-clean`
- David commits only Java files to `david/backend`
- No overlapping folders = no merge conflicts
- David merges both into `main` when ready to test
- To pull David's latest: `git checkout david/backend && git pull origin david/backend && git checkout vito/frontend-clean`
- For experimental testing: throwaway branch from main, mess around, delete if broken
- Commit command: `git add src/main/resources/ src/main/java/hr/tvz/experimate/experimate/view/`

## View controllers (view/ package)
- Vito owns these — David doesn't touch Thymeleaf/view controllers
- All view controllers just serve HTML templates — no model population, no session auth
- JS handles all data fetching and auth via JWT/localStorage
- **Current routes mapped:**
  - `/map` → MapController
  - `/explore` → ExploreController
  - `/community` → CommunityViewController
  - `/tours` → TourListingViewController
  - `/listings/new` → TourListingViewController
  - `/account` → AccountViewController
  - `/account/edit` → AccountViewController
  - `/requests` → AccountViewController
  - `/ratings` → AccountViewController
  - `/profile/{username}` → AccountViewController
  - `/login` → AuthViewController
  - `/register` → AuthViewController
  - `/forgot-password` → AuthViewController
  - `/settings` → AccountViewController
  - `/meet` → MeetViewController (legacy, can ignore)
- **Do NOT** use `controller/` package for view controllers — those were duplicates and got deleted. Only `view/` package.

## Roadmap (agreed 2026-04-03)
1. **MVP** — finish and merge to `main` (targeting ~2026-04-10)
2. **AI integration** — after MVP
3. **Gamification** — after AI

### Post-MVP feature ideas (don't build yet)
- Achievements + ELO-style rating system, seasonal point collection
- Tokens earned through activity; premium gives multiplier not paywall
- Community tab split: local events (dog walking, running) vs travel section (like Nomadtable)

### Post-MVP UX ideas (discussed 2026-04-03, don't build yet)
- TikTok-style UX philosophy: everything in 3 clicks, feed-like layout
- Dyslexia/accessibility support (e.g. accessibility toolbar widget)
- UI reorganization toward a social feed feel

### Post-MVP trust & safety — Couchsurfing model (don't build yet)
1. **Mutual reviews** — both host and guest leave a reference after a tour; neither sees the other's until both submit (prevents retaliation). `rating` field already exists on `UserResponse`.
2. **Vouching** — trusted users can vouch for someone, boosting credibility. Implies a social graph.
3. **Profile completeness score** — more filled in (bio, photo, verified email) = higher trust signal. Incentivises non-ghost profiles.
4. **Verified ID / verified phone** — optional verification badge on profile; backend marks a field, frontend shows a badge.
5. **Response rate + last active** — "responds 90% of the time", "last online 2 days ago" — reduces anxiety when booking a stranger.
6. **References visible on profile** — all past reviews public on profile page, not just an average number.

### Presentation priority
- **Braun (professor/mentor) said UX and prototype UI matter most** — polish > features for the demo

---

## Current state (as of 2026-04-25)
- **UI copy rename** — "My Tours"→"My Days", sub-tabs Joined/Hosted/Open Days, "Rate your Mate", "Join Requests" everywhere; all templates updated
- **Settings — Personality section** — "My Stat Sheet" (disabled, Soon pill), "Retake quiz" → `/onboarding`, "Why we ask this" expandable panel, "Delete my personality data" danger modal (shows toast, no backend yet)
- **Settings — Danger zone** — "Delete account" added; confirmation modal calls `UserAPI.delete(userId)`, clears `localStorage`, redirects to `/login`
- **Landing page full redesign** — split hero with animated word cycling (curious/calm/adventurous/thoughtful/bold), 3-card stack mockup (skeleton/listing/"Ana K. 94%"), city marquee strip, stats bar, How It Works with SVG icons, AI Match section with animated radar pentagon (two overlapping polygons teal=You/orange=Ana), Why/Features/CTA/Footer all updated to Days+Mates copy
- **Landing horizontal scroll fixed** — `overflow-x:hidden` added to both `html` and `body`; card stack gets `scale(0.88)` at 700px and `scale(0.75)` at 400px breakpoints
- **main.css polish** — shadow tokens (`--shadow-sm/md/lg/glow`), `::selection` teal highlight, `:focus-visible` ring, `btn--danger` globalised (was inline in tours.html, broken in settings.html), `btn--sm`/`btn--lg` variants, glow on `btn--primary:hover`, card shadow lift on hover, `divider` utility, `will-change` on skeleton shimmer
- **`btn--danger` inline removed from tours.html** — now uses global definition from main.css
- **local-test branch** — is LOCAL ONLY, never push to remote

## Pending tasks (as of 2026-04-25)
- **Account → Memories tab** — new tab in account.html, grid of Memory Cards + fullscreen modal (no backend yet, skeleton UI)
- **`/onboarding` quiz page** — new Thymeleaf template + view controller route, 10 BFI-10 questions one per screen, progress bar, opt-out, "Analyzing your vibe…" animation, result card; register should redirect here after signup
- **Explore page redesign** — natural language search, POST /api/match wiring, MatchCard with `whyMatch`, "Personalized ON/OFF" chip
- **Post-deploy migration modal** — one-time prompt for existing users to take the quiz (depends on /onboarding existing)

## Current state (as of 2026-04-24)
- **Landing page fix** — removed `"/"` from `AuthViewController`; was conflicting with `LandingViewController`; landing page now serves at `/` and JS redirects to `/map` if already logged in
- **Map button on booked cards** — removed `pointer-events: none` from `.listing-card--reserved`; reserve button is already `disabled` via HTML so blocking all clicks was too broad
- **Light mode persistence** — theme-init script was outside the `th:fragment="topbar"` definition so Thymeleaf never included it; moved inside the fragment — now runs on every app page load
- **Light mode colour fixes** — `body.light-mode` was missing `--accent-dim`, `--accent-border`, `--warm-dim`, `--warm-border` overrides; `.btn--ghost`/`.btn--icon` used `rgba(255,255,255,...)` (invisible on light bg); `.settings-row` border used `rgba(255,255,255,0.04)`; `.bottom-sheet` hardcoded dark bg; all fixed in `main.css`
- **Issue #44 root cause found — pending David** — `TourListingDetails` record is missing `id` field so `r.tourListing?.id` is always `undefined` on the frontend; `_myRequests` is always empty; all listings always show "Request" button. Backend correctly rejects duplicates at service layer. Fix: David adds `Integer id` to `TourListingDetails` and passes `request.getListing().getId()` in `BookingRequestService.createBookingRequestResponse()`
- **local-test branch** — is LOCAL ONLY, never push to remote

## Current state (as of 2026-04-23, session 3)
- **Backend endpoint remapping done** — all caller IDs removed from request bodies; `ReservationAPI.getMine()` → `{asGuest, asHost}`; `TourListingAPI.getMine()`; `GET /api/user/by-username/{username}` live
- **Issue #42 fixed** — `apiFetch`: 401 → refresh; 403 from non-refresh endpoint → throw normally; prevents token refresh on permission errors
- **Issue #43 done** — removed `guestId` from `BookingRequestAPI.create`, `raterId` from `RatingAPI.create`/`delete`, `hostId` from `TourListingAPI.create`; tours.html/listings-new.html/profile.html updated
- **Issue #38 done** — real `ReservationStatus` field now used; workaround filter removed; `renderMeetups`/`renderHostMeetups` use `asGuest`/`asHost` from `getMine()`
- **Issue #26 done** — profile.html uses `UserAPI.getByUsername()` directly
- **Issue #44 done** — `_myRequests` keyed by `listing.id` (was `meetingDate`); both card list AND modal use it; profile "Request" deep-links to `/tours?listing={id}`
- **Settings page live** — `/settings` with theme toggle, available-to-meet toggle, links, sign out
- **Dark/light mode** — `body.light-mode` on app pages, `html.light-mode` on landing; toggle in topbar and settings
- **#40 (back button)** — ✅ closed: sessionStorage queue (max 6), `‹` button in topbar, hidden until history exists, auth pages excluded (no topbar fragment)
- **#41 (map geocoding)** — ✅ closed: Enter key in map search → Nominatim OSS API → flyTo; live pin-filter on typing unchanged; no API key needed

## Known pending issues (updated 2026-04-23 s3)
- **Topbar avatar broken** — shows empty gray circle; needs browser devtools to diagnose
- **Issue C/E** — `RatingResponse` has no `raterUsername`/`ratedUsername`; ratings section on profile blocked; waiting on David
- **Issue D** — `POST /api/auth/logout` server-side invalidation pending David
- **Remove photo** — no DELETE endpoint yet
- **#27 availableToMeet** — localStorage only, post-MVP
- **#31 saved locals** — post-MVP

## GitHub issues for David — updated 2026-04-23 s3

| # | Issue | Status | Notes |
|---|-------|--------|-------|
| 26 | `GET /api/user/by-username/{username}` | ✅ Done | Wired in profile.html |
| 27 | `availableToMeet` | 🟡 Post-MVP | localStorage toggle in settings |
| 30 | `profilePhotoUrl` VARCHAR | ✅ Obsolete | closed |
| 31 | saved locals | 🟡 Post-MVP | |
| 38 | `ReservationResponse.status` | ✅ Done | live, workaround removed |
| 40 | back button | ✅ Done | built frontend-only |
| 41 | map geocoding | ✅ Done | Nominatim, no API key |
| 42 | 403 handling | ✅ Done | fixed in apiFetch |
| 43 | endpoint remapping | ✅ Done | all caller IDs removed |
| 44 | unlimited listing requests | ✅ Done | keyed by listing.id, deep-link |
| C/E | `RatingResponse` missing user fields | ❌ Open | blocks profile ratings section |
| D | `POST /api/auth/logout` | ❌ Open | workaround in place |

---

## Current state (as of 2026-04-23, session 2)
- **Dark/light mode** — `localStorage('theme')` drives `body.light-mode` (app pages) / `html.light-mode` (landing); sun/moon toggle button in topbar on every app page and in landing nav; theme-init IIFE runs before paint (no flash); `main.css` has `body.light-mode` overrides for all surface/border/text tokens; landing has its own inline `html.light-mode` block
- **Settings page** — `/settings` → `AccountViewController`; `settings.html` has: light mode toggle (synced with topbar icon), available-to-meet toggle (localStorage only), links to profile/edit/requests, sign out; accessible from Account page via new "Settings" button
- **Hamburger fix (landing)** — root cause was `toggleMobileMenu()` replacing `btn.innerHTML` which orphaned the clicked SVG child; then document outside-click handler saw the orphan as "not inside button" and closed menu immediately; fixed with `e.currentTarget` + `e.stopPropagation()` on the button listener
- **@import fix (landing)** — `@import` inside `<style>` after any other rule is silently ignored by browsers; moved to proper `<link>` tag; Syne/DM Mono fonts now actually load
- **New route** — `/settings` added to `AccountViewController`; template at `settings.html`

## Known pending issues (updated 2026-04-23 s2)
- **Topbar avatar broken** — shows empty gray circle; `user_initials` not in localStorage; topbar fetches user async but still doesn't render; needs browser devtools inspection
- **`ReservationResponse` missing `status` field** — two-layer fallback in place; waiting on David
- **`POST /api/auth/logout`** — server-side token invalidation pending David
- **Remove photo** — no DELETE endpoint yet
- **`GET /api/user/by-username/{username}`** — David said he'll implement it; workaround via `getAll()` in place
- `availableToMeet` — localStorage-only no-op (post-MVP)
- WebSocket `/ws/map` — reconnect disabled (post-MVP)

---

## Current state (as of 2026-04-23)
- **David's latest push** — profile photo upload/serve fully live; 403 on `GET /api/user/profile-photo/{filename}` fixed; `GET /api/user/search?query=` live returning `UserSearchResponse { searchResult, count }`
- **Issue #30 (VARCHAR vs TEXT) — OBSOLETE** — David switched to multipart file storage; `profilePhotoUrl` now stores just a short filename, VARCHAR(255) is fine; issue can be closed
- **Issue #26 (by-username)** — David confirmed he'll implement it; once pushed, swap `profile.html:178` from `getAll()` + filter to `UserAPI.getByUsername(_profileUsername)` — one line change
- **tours.html reservation filtering** — already has two-layer defense: `(!r.status || (r.status !== 'CANCELLED' && r.status !== 'EXPIRED'))` + `meetingDate >= now` for upcoming. When David adds `status` field it just starts working — zero frontend changes needed
- **Explore card photos** — already wired via `UserAPI.photoUrl(user.profilePhotoUrl)` in both `getAll()` and search paths; should now show real photos since 403 is fixed
- **GitHub issues filed this session** — Issue C (`ReservationResponse` needs `status` field), Issue D (`POST /api/auth/logout`), Issue E (`RatingResponse` missing `ratedUsername`/`raterUsername`)
- **New pages/assets built (2026-04-23)** — `landing.html` (preview at `/landing`), `error/404.html`, `manifest.json`, `icons/icon.svg`; PWA manifest link added to all 14 templates
- **Community page** — replaced coming-soon teaser with live listings from API grouped by city
- **Explore cards** — photo used as full-bleed card background when available; hue gradient fallback for users without photo
- **Share listing button** — added to listing modal in `tours.html`; copies `/tours?listing={id}` to clipboard
- **Profile nudge** — was already implemented; confirmed working
- **Ratings on profile** — blocked pending Issue E; `RatingResponse` has no `ratedUsername` field

## Known pending issues (updated 2026-04-23)
- **Topbar avatar broken** — shows empty gray circle; `user_initials` not in localStorage; topbar fetches user async but still doesn't render; root cause unknown — suspect `userId` not in localStorage at topbar render time, or fetch fails silently; needs browser devtools inspection to diagnose
- **`ReservationResponse` missing `status` field** — two-layer fallback is in place but CANCELLED future-dated reservations still show; waiting on David
- **`POST /api/auth/logout`** — server-side token invalidation pending David; current UX is correct (explicit_logout flag prevents re-login loop)
- **Remove photo** — no DELETE endpoint yet
- **`GET /api/user/by-username/{username}`** — David said he'll implement it; workaround via `getAll()` already in place
- `availableToMeet` — localStorage-only no-op (post-MVP)
- WebSocket `/ws/map` — reconnect disabled (post-MVP)

## GitHub issues for David — updated 2026-04-23

| # | Issue | Status | Notes |
|---|-------|--------|-------|
| 26 | `GET /api/user/by-username/{username}` | ❌ Open | David confirmed: implementing soon; frontend workaround in place |
| 27 | `availableToMeet` on User/UpdateUserDto/UserResponse | 🟡 Deferred | Agreed post-MVP; localStorage-only toggle |
| 30 | `profilePhotoUrl` column type TEXT | ✅ Obsolete | David switched to file storage — filename fits VARCHAR(255); close the issue |
| 31 | `GET/POST/DELETE /api/saved/{targetUserId}` | 🟡 Post-MVP | David confirmed post-MVP |
| C  | `ReservationResponse` missing `status` field | ❌ Open | Two-layer fallback in place; frontend ready to filter when field arrives |
| D  | `POST /api/auth/logout` | ❌ Open | UX workaround in place; server-side invalidation still needed |
| E  | `RatingResponse` missing `ratedUsername`/`raterUsername` | ❌ Open | `GET /api/rating` returns `{id, score, review}` only — can't filter by user; blocks ratings section on profile page |

---

## Current state (as of 2026-04-22)
- **local-test branch** — merged `vito/frontend-clean` + `david/backend`; missing shared event/exception classes were pulled manually from `origin/david/backend` (`UserDeletedEvent`, `TourListingsDeletedForHostEvent`, `ReservationsDeletedEvent`, `NotFoundException`, `ConflictException`); app runs with `mvn spring-boot:run` from `local-test` branch only (`vito/frontend-clean` has no `pom.xml`)
- **Map popup "See listing"** — now links to `/tours?listing={id}` instead of `/tours?host=...`; tours page detects `?listing=` param and auto-opens the modal for that listing
- **Tours listing modal** — wider (`min(740px, 92vw)`), bigger city title, split date/time, host profile card (avatar + name + handle, tappable → `/profile/{username}`)
- **Explore "View Profile"** — now links to `/profile/{username}` when user has no active tours; was incorrectly linking to `/tours?host=...`
- **`photoUrl` in api.js** — handles both bare filename and full path: `url.startsWith('/') ? url : /api/user/profile-photo/${url}`
- **Topbar avatar** — STILL BROKEN; topbar shows empty gray circle for both users (one with photo, one with initials); `user_initials` never ends up in localStorage despite multiple fix attempts; topbar now tries async fetch of `/api/user/{userId}` as last resort but still doesn't work — needs fresh investigation next session

## Known pending issues (updated 2026-04-22)
- **Topbar avatar broken** — shows empty gray circle; `user_initials` not in localStorage; topbar fetches user async but still doesn't render; root cause unknown — suspect `userId` not in localStorage at topbar render time, or fetch fails silently; needs browser devtools inspection to diagnose
- **`POST /api/auth/logout`** — David needs to add this endpoint to invalidate refresh token server-side
- **Remove photo** — no DELETE endpoint yet
- **`ReservationResponse` missing `status` field** — cancelled/expired tours still show in upcoming
- **`GET /api/user/by-username/{username}`** — Issue #1, still open
- `availableToMeet` — localStorage-only no-op
- WebSocket `/ws/map` — reconnect disabled

## Current state (as of 2026-04-21)
- **JWT refresh loop fix** — `Auth.logout()` and refresh-fail path in `apiFetch` set `sessionStorage.explicit_logout = '1'` before redirecting; login page IIFE bails immediately on that flag so a still-valid refresh cookie can't pull the user back to `/map` after explicit logout. **Partial fix only** — refresh token is never invalidated server-side; full fix requires David to add `POST /api/auth/logout` (see pending issues below)
- **Topbar profile photo** — login page now stores `photo_{userId}` in localStorage after resolving user data (both form submit and refresh auto-redirect paths); topbar avatar shows photo on first visit without needing to visit account.html first
- **Profile photos on tours page** — `UserAPI.getAll()` added to the main `Promise.all` on `tours.html` (with `.catch(() => [])` guard); `userAvatar(username, size)` helper renders photo or hue-matched initials; used in listing cards (22px), guest meet-cards (26px), host meet-cards (26px)
- **Profile photos on map** — `loadPins` uses `Promise.allSettled` to load users alongside listings; `buildPopup` renders a 28px host avatar (photo or initials) inline with host name
- **`TourListingResponse.host`** — only has `firstName, lastName, username` (no `profilePhotoUrl`); photos fetched separately via `UserAPI.getAll()` and cached in `_userCache` / `MapState.userCache`

## Current state (as of 2026-04-20)
- **Profile photo upload** — fully wired to backend; `UserAPI.uploadPhoto(id, blob)` calls `POST /api/user/{id}/profile-photo` (multipart); `account-edit.html` sends canvas-compressed blob, syncs returned `profilePhotoUrl` to localStorage; `profilePhotoUrl` removed from `UpdateUserDto` payload
- **`apiFetch`** — auto-skips `Content-Type: application/json` when body is `FormData` so browser sets multipart boundary
- **explore.js** — search calls `GET /api/user/search?query=` (David's endpoint) with 300ms debounce + skeleton loading state; empty state shows "No results for X"; sort pills apply to search results too; `renderExploreSorted` simplified
- **api.js** — added `UserAPI.search(query)`, `UserAPI.uploadPhoto(id, blob)`

## Known pending issues (updated 2026-04-21)
- **`POST /api/auth/logout`** — David needs to add this endpoint; it must invalidate the refresh token server-side and return `Set-Cookie: refresh_token=; maxAge=0; path=/api/auth; httpOnly` to clear the browser cookie. Once added, wire it in `Auth.logout()` in `api.js` with `await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' }).catch(() => {})` before the redirect
- **Remove photo** — UI resets to initials locally but no DELETE endpoint exists yet; localStorage cleared but server file stays
- **`ReservationResponse` missing `status` field** — cancelled/expired tours still show in upcoming (waiting on David)
- **`GET /api/user/by-username/{username}`** — Issue #1, still open; profile page can't load other users by username
- `availableToMeet` — toggle UI is localStorage-only no-op until David adds the field
- WebSocket `/ws/map` — reconnect disabled until David implements endpoint

## Current state (as of 2026-04-17)
- **profile.html** — full layout cleanup: CSS classes replace all inline styles, avatar 96px, bio section with "About" label, better spacing/hierarchy
- **api.js** — friendly fallback error messages by HTTP status (400 → "Check your input...", 500 → "Server error...", etc.); Spring's own `message` still shown if present
- **login.html** — redirect logic now tries refresh cookie even if userId missing from localStorage; full flow: JWT → refresh → /map or stay
- **register.html** — DOB input auto-formats with '/' as you type, leading zeros added on blur, digit-only keydown, isNaN date guard

## Current state (as of 2026-04-16, evening)
- Full local-test smoke tested — register, login, create tour, request, accept, rate all working
- Listing status dot has 3 states: Available (teal) / Requested (orange) / Booked (grey, reserved=true)
- map.js now uses apiFetch — pins load correctly with auth
- WebSocket reconnect disabled — backend endpoint not yet implemented
- Page transitions removed entirely
- Description minimum is 200 chars — counter turns teal at 200 (red below)
- Rating system works — Rate button appears on past tours in My Tours tab
- `RatingAPI` fully wired: `getAll`, `getById`, `create`, `update`, `delete`
- `profilePhotoUrl` now in `UserResponse` — account.html reads from API first, syncs to localStorage for topbar
- `tours.html` — tap any listing card to open full-description modal with Reserve/Map buttons
- `tours.html` — My Tours has 3 subtabs: As Guest / As Host / My Listings
- `tours.html` — "I'm here" button is LIVE and wired: calls `ReservationAPI.checkIn(resId)`, handles both-checked-in → tour takeover overlay, end-tour flow
- `tours.html` — cancel button opens modal with reason, calls `ReservationAPI.cancelTour(id)` (PATCH /cancel-tour)
- `tours.html` — host name on listing cards links to `/profile/{username}`
- `explore.js` — pre-fills search from `?q=` URL param
- `account.html` — booking requests moved to dedicated `/requests` page; badge shows pending count
- `requests.html` — dedicated page for host to view/accept/decline booking requests
- `experiences.html` and `tour-listings.html` — dead/unlinked templates, ignore them
- **`ReservationAPI`** wired: `getAll`, `getById`, `delete`, `checkIn`, `endTour`, `cancelTour`
- **`ReservationStatus` enum** (backend): CONFIRMED → ACTIVE → CLOSED → COMPLETED / CANCELLED / EXPIRED
- **Desktop layout** — all content pages centred at max 680px; sidebar brand shows "Experi**Mate**" with teal Mate
- **Responsiveness** — app-shell goes full-width 481–899px; no black margins between mobile and desktop breakpoints
- **Profile page** — content wrapped in `.profile-page` div, centred on desktop
- **Account page** — `.acc-section` and `.account-hero` centred with `margin: 0 auto`; nudge and bottom buttons also constrained
- **Sidebar brand** — replaced CSS `::before` with real HTML `<a class="navbar__brand">` so "Mate" can be teal

## Bugs fixed (2026-04-16)
- **`BookingRequestResponse.user` vs `.guest`** — fixed in `tours.html` and `requests.html`; both now use `r.user`
- **Cancel uses cancelTour not delete** — `confirmCancel()` calls `ReservationAPI.cancelTour(id)`
- **`profilePhotoUrl` VARCHAR(255) too short** — fixed on `local-test` with `@Column(columnDefinition = "TEXT")` on `User.java`; David needs same fix on `david/backend`

## Known pending issues (waiting on David) — updated 2026-04-16
- **`ReservationResponse` missing `status` field** — frontend can't distinguish CONFIRMED vs CANCELLED; cancelled tours still show in "My Tours" upcoming section
- `availableToMeet` — David questioned its value; agreed post-MVP. Toggle UI stays as localStorage-only no-op for now
- WebSocket `/ws/map` — reconnect disabled on frontend until David implements the endpoint

## GitHub issues for David — updated 2026-04-16 (superseded — see 2026-04-23 table above)

## Current state (as of 2026-04-05)
- All DTOs fully done and frontend synced — pushed to `vito/frontend-clean`
- Old `vito/frontend` branch — ignore it
- `idNumber` in register sends `crypto.randomUUID()` (backend rejects null)
- Full UX pass done — see session log 2026-04-04 below
- JWT refresh, booking requests section, and photo file picker added — see session log 2026-04-05 below

## Local testing setup (local-test branch)
- Branch `local-test` = `vito/frontend-clean` + `david/backend` merged together
- Missing Java files were pulled from `david/backend` manually (see session log below)
- `application.properties` on `local-test` needs:
  - `jwt.secret=ZXhwZXJpbWF0ZWxvY2FsZGV2c2VjcmV0a2V5Zm9ydGVzdGluZ29ubHkyMDI2` (Base64, ≥256 bit)
  - `spring.datasource.url=jdbc:h2:~/experimateDb;AUTO_SERVER=TRUE`
  - `spring.datasource.username=` (empty)
  - `spring.datasource.password=` (empty)
- David's `application.properties` uses a different H2 path + credentials — don't commit local-test's properties to either main branch
- H2 ENUM warning on startup is harmless — app still runs

## Response DTO status
| DTO | Status | Shape |
|-----|--------|-------|
| `UserResponse` | ✅ DONE | `id, username, firstName, lastName, bio, rating, profilePhotoUrl` |
| `TourListingResponse` | ✅ DONE | `id, city, lat, lng, meetingDate, postDate, tourDescription, reserved, host{firstName, lastName, username}` |
| `ReservationResponse` | ⚠️ MISSING STATUS | `id, dateOfReservation, tourListing{meetingDate, city, host{…}}, guest{firstName, lastName, username}` — needs `status` field |
| `BookingRequestResponse` | ⚠️ FIELD NAME BUG | `id, status, requestDate, tourListing{meetingDate, city, host{…}}, user{firstName, lastName, username}` — field is `user` not `guest`; frontend uses `r.guest` everywhere → broken |
| `CheckInResponse` | ✅ DONE | `reservationId, status, guestCheckedIn, hostCheckedIn, guestCheckInTimestamp, hostCheckInTimestamp, statusTimestamp` |
| `EndTourResponse` | ✅ DONE | `reservationId, status, endedByUsername, endTimestamp` |
| `CancelTourResponse` | ✅ DONE | `reservationId, status, cancelledByUsername, cancelTimestamp` |
| `RatingResponse` | ✅ DONE | `id, score, review` |
| `CreateRatingDto` | ✅ DONE | `raterId, ratedId, score, review` |

- JS uses `listing.lat` / `listing.lng` — matches `TourListingResponse` naming
- `CreateTourListingDto` uses full `latitude`/`longitude` — `listings-new.html` is correct
- `AuthResponse` only returns `{ token }` — no id, login falls back to `UserAPI.getAll()` to resolve userId
- `guest` in `ReservationResponse` has no `id` — "My Tours" tab filters by `guest.username` via `Auth.getUsername()`
- `BookingRequestAPI` wired up with all endpoints (create, accept, decline, getAll, getById, delete)
- `ReservationAPI` wired up with all endpoints (getAll, getById, delete, checkIn, endTour, cancelTour)
- `RatingAPI` wired up with all endpoints (getAll, getById, create, update, delete)
- Reserve button uses `POST /booking-request` with `{ guestId, listingId }`
- Rating submit resolves `ratedId` by calling `UserAPI.getAll()` and matching by username

## Available to meet toggle
- Toggle UI exists in `account.html` — currently localStorage only (calls `UserAPI.update` optimistically but field doesn't exist yet on backend)
- When David adds `availableToMeet` to `User` entity and `UpdateUserDto`, it will just start working

## Ready to test
- Needs David to merge both branches into `main`
- Smoke test: reserve, check-in flow (I'm here → both checked in → takeover → end tour → rate), booking requests accept/decline, My Listings delete, cancel reservation

---

## Session log — 2026-04-16 (David's 13-commit push — check-in/end-tour/cancel live)

### David pushed (2026-04-16)
- **Check-in flow** — `PATCH /api/reservation/check-in/{id}` returns `CheckInResponse` with `guestCheckedIn`, `hostCheckedIn`, timestamps; both checked in → status ACTIVE
- **End tour** — `PATCH /api/reservation/end-tour/{id}` → status CLOSED; 48h later auto-expires to EXPIRED
- **Cancel tour** — `PATCH /api/reservation/cancel-tour/{id}` → status CANCELLED (keeps row in DB)
- **`ReservationStatus` enum** — CONFIRMED, ACTIVE, COMPLETED, CLOSED, CANCELLED, EXPIRED
- **`BookingRequestResponse`** — now fully expanded with `tourListing` and `user` (guest) fields; `requestDate` also included
- **`ReservationService`** — now `@Transactional`, reservation creation triggered via `BookingRequestAcceptedEvent`
- **Reservation cascade** — deleting a user or listing cleans up associated reservations and booking requests
- **`DuplicateRatingException`** — backend now guards against rating the same person twice
- **Rating events** — `RatingCreatedEvent`, `RatingDeletedEvent`, `RatingRecalculatedEvent` — ratings now recalculate user's average score automatically

### Frontend already had (no changes needed in api.js)
- `ReservationAPI.checkIn`, `endTour`, `cancelTour` — all existed and are wired in `tours.html`
- `tours.html` — "I'm here" button, tour takeover overlay, end-tour flow all live

### Bugs found (need fixing)
- `BookingRequestResponse` field is `user`, not `guest` — `tours.html` and `requests.html` use `r.guest` → breaks request status tracking and request card rendering
- `confirmCancel()` calls `ReservationAPI.delete` (DELETE removes row) — should call `ReservationAPI.cancelTour` (PATCH sets CANCELLED status)

### Čeka David
- `ReservationResponse` needs `status` field so frontend can filter out CANCELLED/EXPIRED from "My Tours"
- `availableToMeet` field on User entity
- WebSocket `/ws/map`

---

## Session log — 2026-04-09 (RatingAPI, photo sync, UI polish pass)

### Što je napravljeno
- **`RatingAPI.getById`** — added missing method to `api.js`
- **Profile photo sync** — `account.html` now syncs `user.profilePhotoUrl` to `localStorage` on load so the topbar IIFE always reflects the latest photo; stale localStorage cleared if server has no photo
- **Description counter** — turns red below 200 chars, teal at or above (visual feedback for backend minimum)
- **My Listings tab** — `account.html` now has "Overview" / "My Listings" tab bar; My Listings tab renders own listings (reuses already-fetched data) with city, date, status dot, and a Delete button (`TourListingAPI.delete`)
- **Listing full-description modal** — tapping any listing card in `tours.html` opens a modal with the full (untruncated) description, host, date, status dot, and working Reserve/Map buttons; buttons inside cards use `stopPropagation` to not bubble to modal
- **"I'm here" placeholder** — disabled teal button added to upcoming meetup cards in My Tours tab, tooltip explains it's coming; ready to wire when David adds the backend endpoint
- **Host profile link** — host name on listing cards now links to `/explore?q={username}`
- **`explore.js` URL param** — `?q=` pre-fills search on load and filters to that user's card
- **Booking requests filter** — graceful fallback: shows all PENDING if `tourListing.host` isn't in the response yet; card renders guest name/handle/city/date when available, honest placeholder when not
- **Dead templates** — `experiences.html` and `tour-listings.html` confirmed unlinked (no routes); safe to ignore

### David pushed (2026-04-08)
- `RatingController` — `POST/GET/PATCH/DELETE /api/rating` all live
- `UserResponse` now includes `profilePhotoUrl`
- `TourListingReservedEvent` — internal backend event, no frontend action needed

### Čeka David
- `BookingRequestResponse` expand with `guest { firstName, lastName, username }` + `tourListing { id, city, meetingDate, host }`
- "I'm here" / meeting confirmation endpoint (both parties confirm → rating prompt)
- `availableToMeet` field on User entity
- WebSocket `/ws/map`

---

## Session log — 2026-04-05 (JWT refresh + booking requests + photo picker)

### Što je napravljeno
- **JWT refresh** — `apiFetch` now intercepts 401, calls `POST /api/auth/refresh` (browser sends httpOnly cookie automatically), saves new token, retries original request; if refresh fails → `Auth.logout()`
- **Booking requests section** — added to `account.html`; fetches all PENDING requests, renders cards with Accept/Decline buttons wired to `BookingRequestAPI`; pending count badge; skeleton loading; error state
- **Photo file picker** — `account-edit.html` URL field replaced with a styled file picker; tap avatar to open file explorer; previews image immediately; saves base64 to `localStorage` on form save; Remove photo button; full TODO comment marks where to swap for API call when David adds the endpoint

### Čeka David
- `BookingRequestResponse` treba proširiti s `guest { firstName, lastName, username }` i `tourListing { id, city, meetingDate }`
- `POST /api/user/{id}/photo` endpoint (multipart/form-data) + `profilePhotoUrl` na `UserResponse`

---

## Session log — 2026-04-04 (UX polish pass)

### Što je napravljeno
- **Brand rename** — "Experimate" → "ExperiMate" (capital M) across all pages: topbar, login, register, forgot-password
- **Map gem pin** — fixed color from lime to teal (`rgba(0,201,167,...)`)
- **Map FAB** — added `+` floating action button on map (`bottom: 108px; right: 14px`) linking to `/listings/new`
- **Explore page** — added shimmer skeleton cards while loading; initials avatar (hue from username hash); "View Listings" CTA now links to `/tours?host={username}` (filters that host's listings)
- **tours.html host filter** — `?host=` query param shows only that host's listings with a teal banner + clear link
- **tours.html empty state** — added `+ Create a listing` CTA button
- **listings-new.html** — fixed location marker color to teal; added description character counter (0/2000)
- **account.html** — host stats (Total tours + Active tours) loaded via `Promise.all([UserAPI.getById, TourListingAPI.getAll])`; skeleton loading for name/handle/rating; initials+hue cached to localStorage on load
- **account-edit.html** — profile photo URL field; saves to `localStorage` as `photo_{userId}`; prefills from localStorage
- **Topbar avatar** — replaced 👤 emoji with initials/photo from localStorage via inline IIFE (no flash); uses `user_initials` + `user_hue` keys
- **register.html** — saves `user_initials` + `user_hue` to localStorage after successful registration
- **Page transitions** — fade-in on load (`.app-shell` CSS animation), fade-out on navigate (`.app-shell--exit` class + JS click interceptor with setTimeout); final durations: ~25ms in, ~19ms out

### localStorage keys in use
| Key | Value |
|-----|-------|
| `jwt` | JWT token string |
| `userId` | numeric user id |
| `user_initials` | e.g. "VK" |
| `user_hue` | 0–359 (HSL hue from username hash) |
| `photo_{userId}` | profile photo URL (local workaround until backend supports it) |

---

## Session log — 2026-04-03 (evening — local testing + UX pass)

### Što je napravljeno
- **local-test branch** — pulled all missing Java files from `david/backend` (Reservation, DTOs, exceptions, events, repos)
- **JWT secret** — set Base64-encoded secret in `application.properties` (was empty → 500 on login/register)
- **UX overhaul** — login, register, forgot-password, tours, account, account-edit, listings-new, community all redesigned
  - Auth pages: dark glassmorphism card, animated teal/orange orbs, hardcoded colors (bypass CSS variable issue)
  - tours.html: shimmer skeleton cards, host initials avatar, glowing availability dot, staggered fade-in
  - account.html: teal hero gradient, skeleton loading for name/handle/rating
  - community.html: full teaser with feature preview cards
  - navbar.html: Account tab added (5th item)
  - main.css: skeleton shimmer keyframes + `.skeleton` classes, `fadeIn` animation
- **App confirmed running** at `localhost:8080` — register and login work end-to-end

---

## Session log — 2026-04-03 (afternoon chat with David)

### Što je rečeno
- David predložio gamification (achievements, ELO, tokeni, premium multiplier) — **post-MVP**
- David predložio community tab split (lokalni eventi vs travel sekcija) — **post-MVP**
- Dogovoreni roadmap: MVP → AI → gamification
- David potvrdio da je sve na frontendu usklađeno s Vitovim pushovima, čeka merge u `main`
- **Braun rekao da im je najvažniji UX i prototip UI** — fokus na polish za prezentaciju
- Nema konkretnog frontend posla iz ovog razgovora — čeka se David da mergea u `main`

---

## Session log — 2026-04-03 (morning — Claude session)

### Što je napravljeno
- **Pulled David's latest** (`david/backend`) — all 3 DTOs now confirmed done
- **`POST /booking-request`** — wired up in `tours.html`, switched from `ReservationAPI.create` to `BookingRequestAPI.create` with `{ guestId, listingId }`
- **`BookingRequestAPI`** — added to `api.js` with all endpoints (accept, decline, getAll, getById, delete)
- **`lat`/`lng` fix** — `tours.html` and `map.js` reverted to short names matching `TourListingResponse`
- **"My Tours" guest filter** — changed from `r.guest.id === currentUserId` to `r.guest.username === Auth.getUsername()` (UserDetails has no id)
- **Login userId** — improved to check `res.id` first, falls back to `getAll` lookup (AuthResponse only returns token)
- Committed and pushed to `vito/frontend-clean`

---

## Session log — 2026-04-02

### Što je napravljeno
- **Responsive layout fix** — `#tab-meetups` imao `display:flex` inline koji overridea `hidden` atribut → premješteno u CSS s `:not([hidden])` selektorom
- **"My Meetups" → "My Tours"** — tab preimenovan per David's prijedlog
- **Bio edit** — dodan "Edit" link pored Bio labela na account stranici
- **Available to meet toggle** — maknut (nema backend infrastrukture)
- **Zagreb topbar** — ostavljen kao hardcoded fallback, maknut iz staged izmjena nakon greške
- **lat/lng naming fix** — `tours.html` i `map.js` promijenjeni s `listing.lat/lng` na `listing.latitude/longitude`
- **listings-new.html** — `latitude`/`longitude` dodani u API create payload
- **register.html** — `idNumber: null` → `crypto.randomUUID()` (bio "fixan" ranije ali nije bio commitano)
- **View controller rute** — dodane sve rute koje su nedostajale: `/tours`, `/listings/new`, `/account/edit`, `/login`, `/register`, `/forgot-password`
- **Duplicate view controllers** — obrisani iz `controller/` paketa (bili su duplikati `view/` paketa, pucali Spring)
- **AuthViewController** — kreiran novi za auth rute

### Session log — 2026-04-01
- `api.js` — dodani `accept` i `decline` metode u `BookingRequestAPI`
- View controlleri (`view/` paket) — simplificirani; `AuthController` → `AuthViewController`
- `vito/frontend-clean` — očišćen od Java/backend fajlova
- Swagger/OpenAPI dodan — David dodao springdoc (`http://localhost:8080/swagger-ui/index.html`)
