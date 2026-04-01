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

## Current state (as of 2026-03-31)
- `david/backend` compiles. `test-branch` does not (David left controllers mid-refactor).
- Backend MVP is ~2-3 days out (David's estimate).
- David will generate Swagger/OpenAPI docs and send them to Vito as the API contract — wait for those before wiring uncertain endpoints.
- `vito/frontend-clean` is the active frontend branch (clean, only templates + JS).
- Old `vito/frontend` branch — ignore it.

## Blocking backend issue (David's job)
- There is **no `@Controller`** serving the HTML routes. Without it, all pages return 404.
- `tour-listings.html` uses Thymeleaf server-side bindings (`th:each`, `${listings}`) — David needs to decide: MVC view controller that populates model, or Vito rewrites it to pure JS fetches.
- `api.js` is missing `BookingRequestAPI` and `RatingAPI` wrappers — add after David sends API docs.

## What Vito can do now (no backend needed)
1. **Map page** — fully Vito's domain, zero backend dependency. Leaflet, pins, search, filters.
2. **Layout/UI polish** — any page that is pure HTML/CSS structure.
3. **`listings-new.html`** — build the form UI now, wire submit later once API contract is confirmed.

## Claude's role
- Help Vito with **frontend only**. Do not edit any Java files.
- Reading backend files for context (to understand endpoints before building forms) is fine — editing is not.

## Repo structure
Stays as-is for now. Frontend/backend repo separation is a later problem.

---

## Session log — 2026-03-31

### Što je napravljeno
- `api.js` — dodani `BookingRequestAPI` i `RatingAPI` wrapperi
- `map.html` — dodan bottom-sheet fragment (pokazuje fallback lokalne korisnike dok nema stvarnih podataka)
- `tours.html` — FAB gumb "+" sakriven za nelogirane korisnike
- `application.properties` — dodan `jwt.secret` i `jwt.expiration` (backend se sad pokreće)
- JWT strategija dogovorena s Davidom: localStorage + Bearer header, token se čita svježe pri svakom requestu, David šalje novi token kad se refresha, Vito zove `Auth.saveToken()`

### Što se čeka od Davida
1. **View controller** — BLOKER. Bez njega sve stranice vraćaju 404. Treba `@Controller` klasa koja mapira URL-ove na Thymeleaf template-e (npr. `@GetMapping("/login") { return "login"; }`). Nije mu potrebno injectati model — JS sve fetchea.
   - Stari view controlleri postojali su u `vito/frontend` branchu (`view/` paket: `AccountViewController`, `AuthController`, `MeetViewController`, `TourListingViewController`) ali su koristili session-based auth koji je izbačen. Vjerojatno su pregaženi pri mergeu — David ih treba napisati iznova, jednostavno.
2. **Swagger/OpenAPI dokumentacija** — da se provjeri jesu li svi endpointi u `api.js` točni
3. **`POST /api/reservation`** — ne postoji u `ReservationController`! `ReservationAPI.create()` u frontendu postoji ali će baciti 404. David mora dodati endpoint.
4. **Push na `david/backend`** — Vito će pullati kad David završi

### Što treba provjeriti kad view controller bude gotov
- Sve stranice se učitavaju (`/login`, `/register`, `/map`, `/tours`, `/explore`, `/account`, `/account/edit`, `/listings/new`, `/community`)
- Login → sprema JWT i userId → redirect na `/map`
- Register → kreira usera → logira → redirect na `/map`
- Tours stranica → lista listinga se učitava, rezervacija radi (čeka `POST /api/reservation`)
- Account stranica → učitava korisničke podatke
- Edit profile → PATCH user radi
- New listing → kreira tour listing s datumom i gradom
- Map → pinovi se prikazuju, filteri i search rade, bottom sheet se otvara

### Poznati problemi
- `tour-listings.html` koristi server-side Thymeleaf bindinge (`th:each`, `${listings}`) — ili treba view controller koji popunjava model ILI prepisati na JS fetch (preporučeno jer je konzistentno s ostatkom). Odluka na Davidu/Viti.
- Token rotacija nije implementirana (namjerno, za sad 5 min session, David će proširiti kad zatreba)
