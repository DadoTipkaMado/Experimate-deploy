# Experimate — Claude Instructions

## Team split
- **Vito** → frontend + view controllers (`vito/frontend` branch)
- **David** → backend only (`david/backend` branch)

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

## Git workflow (updated 2026-05-09)
- **Repo:** ExperiMate-deploy (stari Experimate repo je legacy/deprecated)
- **Flow:** `feature/ime-featurea` → `vito/frontend` → `develop` → `main`
  1. Napravi `feature/ime-featurea` branch iz `vito/frontend` za svaki feature
  2. Razvij i testiraj tamo
  3. Kad feature radi → merge nazad u `vito/frontend`, obriši lokalni feature branch
  4. Kad je feature stabilan → merge `vito/frontend` u `develop`, push `develop` na remote
  5. `main` je samo za službene releaseove — David mergea `develop` u `main` (zaštićen, rijetko)
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

### Arhitektura po tabovima (finalno stanje)
- **Map** — pin filter (typing), geo fly-to (Enter na zasebnom inputu), FAB "List a Day"
- **Explore** — listing feed, client-side filter (typing), AI Match (Enter s chipom aktivnim)
- **Community** — coming soon hero (orbs, feature preview kartice)
- **Meets/Tours** — My Meets (Joined/Hosting), My Listings, Requests
- **Account** — profil, Memories tab, edit/settings linkovi

### Lokalni dev setup (samo na Vitovom računalu)
- `application-local.properties` — gitignoriran, koristi H2 + placeholder JWT/AI key
- `pom.xml` ima H2 dependency lokalno, zaštićen s `git update-index --skip-worktree pom.xml` — nikad se ne pusha
- Pokretanje: `mvn spring-boot:run`

### Sljedeće (polufinale i dalje)
- **AI matching na Explore** — čeka `POST /api/match` od Davida (trenutno seeded placeholder)
- **`POST /api/personality`** — persistanje quiz rezultata (David)
- **`POST /api/auth/logout`** — server-side token invalidacija (David)
- **`RatingResponse` nema `raterUsername`/`ratedUsername`** — blokira ratings na profile stranici (David)
- **Za finale:** Profile completion badge u sidebaru za desktop


