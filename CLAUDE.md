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
  - `/login` → AuthViewController
  - `/register` → AuthViewController
  - `/forgot-password` → AuthViewController
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

## Current state (as of 2026-04-06)
- Full local-test smoke tested — register, login, create tour, request, accept, rate all working
- Listing status dot has 3 states: Available (teal) / Requested (orange, sessionStorage) / Booked (grey, reserved=true)
- map.js now uses apiFetch — pins load correctly with auth
- WebSocket reconnect disabled — backend endpoint not yet implemented
- Page transitions removed entirely
- Description minimum is 200 chars (backend enforces via IllegalArgumentException → 400)
- `BookingRequest.status` column fixed to VARCHAR(20) in local-test — David needs same fix on his branch
- Rating system works — Rate button appears on past tours in My Tours tab

## Known pending issues (waiting on David) — updated 2026-04-06
- **`ReservationService.createReservation`** — `validatedListing.setReserved(true)` is called but method is not `@Transactional` and listing is not explicitly saved. Fix: add `@Transactional` to `createReservation` or call `tourListingRepo.save(validatedListing)` after setting reserved.
- **`BookingRequest.status`** — needs `@Column(columnDefinition = "VARCHAR(20)")` alongside `@Enumerated(EnumType.STRING)` in `BookingRequest.java` (H2 creates ENUM type otherwise, breaks queries)
- Profile photo — file picker built, saves base64 to localStorage; needs `POST /api/user/{id}/photo` + `profilePhotoUrl` on `UserResponse`
- `availableToMeet` toggle removed — needs backend field
- WebSocket `/ws/map` — reconnect disabled on frontend until David implements the endpoint

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
| `UserResponse` | ✅ DONE | `id, username, firstName, lastName, bio, rating` |
| `TourListingResponse` | ✅ DONE | `id, city, lat, lng, meetingDate, postDate, tourDescription, reserved, host{firstName, lastName, username}` |
| `ReservationResponse` | ✅ DONE | `id, dateOfReservation, tourListing{meetingDate, city, host{firstName, lastName, username}}, guest{firstName, lastName, username}` |

- JS uses `listing.lat` / `listing.lng` — matches `TourListingResponse` naming
- `CreateTourListingDto` uses full `latitude`/`longitude` — `listings-new.html` is correct
- `AuthResponse` only returns `{ token }` — no id, login falls back to `UserAPI.getAll()` to resolve userId
- `guest` in `ReservationResponse` has no `id` — "My Tours" tab filters by `guest.username` via `Auth.getUsername()`
- `BookingRequestAPI` wired up with all endpoints (create, accept, decline, getAll, getById, delete)
- Reserve button uses `POST /booking-request` with `{ guestId, listingId }`

## Available to meet toggle — REMOVED (temporarily)
- Removed from `account.html` because `User` entity and `UpdateUserDto` have no `availableToMeet` field
- When David adds the field to backend, add back the toggle UI and `toggleAvailability()` JS function

## Known pending issues (waiting on David)
- Profile photo — file picker built, saves base64 to `localStorage` as workaround; needs `POST /api/user/{id}/photo` (multipart) + `profilePhotoUrl` on `UserResponse`; TODO comment in `account-edit.html` submit handler marks the swap point
- `BookingRequestResponse` only returns `{ id, status }` — needs `guest { firstName, lastName, username }` and `tourListing { id, city, meetingDate }` to show useful data in booking requests section
- `availableToMeet` needs backend infrastructure before toggle can be re-added
- JWT refresh wired — David's `/api/auth/refresh` uses httpOnly cookie, frontend handles 401 → refresh → retry automatically

## Ready to test
- Needs David to merge both branches into `main`
- Smoke test: reserve button, My Tours tab, map pins, JWT refresh on expiry, booking requests accept/decline

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
