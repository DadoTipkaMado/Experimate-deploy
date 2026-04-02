# Experimate — Claude Instructions

## Team split
- **Vito** → frontend only (`vito/frontend-clean` branch)
- **David** → backend only (`david/backend` branch)
- Merge into `production` branch for testing

## Frontend rules (stupidly dumb frontend)
1. **UI + map only.** No business logic on the frontend.
2. **Never hash passwords on the frontend.** HTTPS handles transit, Spring handles hashing. Hashing on the frontend makes the hash the password and defeats the purpose.
3. **Age verification** — date of birth input, check 18+ client-side only as UX (not security). No ID scanning — that's a "pro+" feature for later if ever.
4. **Sign in with Google** — do not touch until David wires Spring backend to support it.
5. **Map is Vito's domain** — full freedom here, this is the one purely frontend feature.
6. **Send data to backend, don't think too much.** Forms submit, JS fetches, templates render.

## Current state (as of 2026-04-01)
- App compiles and runs. Pages load. Registration and login work.
- `vito/frontend-clean` is clean — only `src/main/resources/` (templates + static). No Java.
- `david/backend` has full backend + view controllers.
- Both branches merged into `main` by David for testing.
- JWT filter temporarily disabled by David for easier testing.
- Old `vito/frontend` branch — ignore it.

## Git workflow (agreed with David)
- Vito pushes only to `vito/frontend-clean` — only `src/main/resources/` files
- David pushes only to `david/backend` — only Java files
- No overlapping folders = no merge conflicts
- David merges both into `main` when ready to test
- For experimental testing: create a throwaway branch from main, mess around, delete if broken

## View controllers (view/ package) — DONE
- All view controllers simplified to just serve HTML templates — no model population, no session-based auth
- JS handles all data fetching and auth via JWT/localStorage
- `AuthController` renamed to `AuthViewController` to avoid conflict with REST `AuthController`
- Session-based guards removed — JS handles auth redirects

## Blocking backend issue (David's job)
- **Response DTOs are too stripped down** — JS gets `undefined`/`NaN` everywhere because:
  - `UserResponse` only has `id`, `username`, `rating` — missing `firstName`, `lastName`, `bio`, `profilePhotoUrl`, `availableToMeet`
  - `TourListingResponse` only has `id`, `city`, `longitude`, `latitude` — missing `meetingDate`, `tourDescription`, `reserved`, `host` (firstName, lastName, username)
  - `ReservationResponse` only has `id` — missing `dateOfReservation`, `tourListing` (meetingDate, city, host), `guestId`
  - JS uses `listing.lat` / `listing.lng` — DTO uses `latitude`/`longitude` (rename needed)

## What Vito can do now (no backend needed)
1. **Map page** — fully Vito's domain, zero backend dependency.
2. **Layout/UI polish** — any page that is pure HTML/CSS structure.

## Claude's role
- Help Vito with **frontend only**. Do not edit Java files unless Vito explicitly gives permission.
- Reading backend files for context is fine — editing is not by default.

## Repo structure
Stays as-is for now. Frontend/backend repo separation is a later problem.

---

## Session log — 2026-04-01

### Što je napravljeno
- `api.js` — dodani `accept` i `decline` metode u `BookingRequestAPI`
- `register.html` — fix: `idNumber: crypto.randomUUID()` umjesto `null` (backend odbijao null)
- View controlleri (`view/` paket) — simplificirani da samo serviraju HTML, nema model populacije ni session authova; `AuthController` → `AuthViewController` (ime konflikta s REST controllerom)
- `vito/frontend-clean` — očišćen od Java/backend fajlova, sad samo `src/main/resources/`
- Swagger/OpenAPI dodan (`http://localhost:8080/swagger-ui/index.html`) — David dodao springdoc

### Poznati problemi (čeka Davida)
- Response DTOs premali — `UserResponse`, `TourListingResponse`, `ReservationResponse` nemaju dovoljno polja (detalji u "Blocking backend issue" sekciji)
- `decline` endpoint u `requests.http` ima copy-paste bug — pokazuje `/accept/` umjesto `/decline/`
- Token rotacija nije implementirana (namjerno, za sad, David će proširiti)

### Što treba provjeriti kad David fixa Response DTOs
- Tours stranica → listing kartice pokazuju ime hosta, datum, opis (ne undefined/NaN)
- Account stranica → pokazuje ime, bio, rating
- Explore stranica → pokazuje korisnike s imenom i bio
- Map → pinovi se prikazuju (lat/lng polja moraju se zvati `lat`/`lng` u response-u)
