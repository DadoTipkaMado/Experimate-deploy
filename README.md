# ExperiMate

> **Meet new people. Share real experiences.**

ExperiMate is a full-stack social web platform that connects people who want to meet, hang out, and explore together — whether you're a traveller looking for a local guide, a student new to the city, or simply someone who wants to vibe with new people. Users create activity listings, others discover them, send booking requests, check in on the day, and rate each other after — all without any messaging or intermediary. The premise is simple: real people, real places, no DMs.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Testing](#testing)
- [Roadmap](#roadmap)
- [Team](#team)

---

## Overview

ExperiMate was built for anyone who wants to connect with new people and experience a city in an authentic, unscripted way. That includes travellers who want a local perspective, students freshly arrived in a new city, and locals who simply want to meet people and do things together. A host creates a listing — a date, a location, and a description of what they're offering, whether that's a city tour, a casual hangout, or any kind of shared activity. A guest discovers it on the map or explore feed, sends a booking request, and if accepted, meets the host IRL. After the meetup both parties rate each other, building a reputation system that rewards good hosts and respectful guests.

The application follows a **mobile-first, progressive web app** philosophy — it is designed to feel native on a phone while adapting gracefully to desktop via a sidebar layout.

---

## Features

### For Guests
- Browse and search available listings on an interactive map or card feed
- Filter listings by city, date, and availability
- Send booking requests to hosts
- Check in on the day with the "I'm here" button — meetup activates when both parties confirm
- Rate and review hosts after the meetup is completed
- View profile pages of prospective hosts including their bio and upcoming listings

### For Hosts
- Create listings with a location picker, date/time, and detailed description of the activity
- Manage incoming booking requests (accept or decline)
- Check in alongside guests to activate the meetup
- End the session to trigger the rating flow
- View hosted meetups and listing status (Available / Requested / Booked)

### Platform
- JWT authentication with automatic silent token refresh via HTTP-only refresh cookie
- Snap-scroll TikTok-style explore feed with search and sort
- Dark-mode UI with teal/orange accent theme
- Fully responsive — phone layout below 900 px, sidebar grid above
- Profile photo upload with client-side canvas compression
- Profile completeness nudge, host stats, and booking request badge

---

## Tech Stack

| Layer | Technology                                                    |
|---|---------------------------------------------------------------|
| Backend | Java 25, Spring Boot 4, Spring Security, Spring Data JPA      |
| Database | PostgreSQL 16 + Flyway migrations                             |
| Auth | JWT (jjwt 0.12) + HTTP-only refresh token cookie              |
| AI | Spring AI (Anthropic Claude)                                  |
| Frontend | Thymeleaf templates, Vanilla JS (no framework, no build step) |
| Map | Leaflet.js + Leaflet.markercluster                            |
| Geocoding | Nominatim (OpenStreetMap)                                     |
| API Docs | SpringDoc OpenAPI / Swagger UI                                |
| Build | Maven (Maven Wrapper included)                                |
| Testing | JUnit 5, Testcontainers, Maven Failsafe                       |

---

## Project Structure

```
ExperiMate/
├── src/
│   ├── main/
│   │   ├── java/hr/tvz/experimate/experimate/
│   │   │   ├── ExperiMateApplication.java          # Entry point
│   │   │   ├── config/                             # Spring configuration beans
│   │   │   │   ├── SecurityConfig.java             # Spring Security + JWT filter chain
│   │   │   │   ├── AiConfig.java                   # Spring AI / Anthropic setup
│   │   │   │   └── OpenApiConfig.java              # Swagger / SpringDoc config
│   │   │   ├── security/                           # JWT service, auth filter, UserDetails
│   │   │   │   ├── AuthController.java             # POST /api/auth/login, /refresh
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── JwtService.java
│   │   │   │   ├── JwtAuthFilter.java
│   │   │   │   └── AppUserDetailsService.java
│   │   │   ├── domain/                             # All business domains (package-by-feature)
│   │   │   │   ├── user/                           # Entity, service, repo, controller, dto, response, exception
│   │   │   │   ├── tour_listing/                   # Activity listings + host check-in / tour start
│   │   │   │   ├── reservation/                    # Booking lifecycle: check-in, end / cancel tour
│   │   │   │   ├── booking_request/                # Guest → host requests (accept / decline)
│   │   │   │   ├── rating/                         # Post-meetup mutual ratings
│   │   │   │   ├── refresh_token/                  # JWT refresh-token rotation
│   │   │   │   ├── token/                          # Email-verification + password-reset tokens
│   │   │   │   ├── onboarding/                     # Big Five (BFI-10) personality quiz
│   │   │   │   ├── match/                          # AI-powered user matching
│   │   │   │   ├── ai/                             # AI prompt / matching service
│   │   │   │   ├── feed/                           # Explore feed pagination
│   │   │   │   ├── email/                          # Transactional email content + delivery
│   │   │   │   ├── partner/                        # B2B partner profiles
│   │   │   │   ├── partner_event/                  # Partner-hosted events
│   │   │   │   ├── partner_pin/                    # Partner map pins + subscriptions
│   │   │   │   ├── premium/                        # Premium membership purchase
│   │   │   │   └── promoted_ad/                    # Paid promoted ads
│   │   │   ├── shared/                             # Cross-domain: events, exceptions, payment, mappers, rate limiting
│   │   │   ├── push/                               # Web-push notifications (VAPID) + schedulers
│   │   │   └── view/                               # Thymeleaf view controllers (no business logic)
│   │   │       ├── AuthViewController.java
│   │   │       ├── MapController.java
│   │   │       ├── ExploreController.java
│   │   │       ├── TourListingViewController.java
│   │   │       ├── AccountViewController.java
│   │   │       ├── MeetViewController.java
│   │   │       ├── CommunityViewController.java
│   │   │       └── LandingViewController.java
│   │   └── resources/
│   │       ├── application.properties              # Base config (JWT, JPA, Flyway)
│   │       ├── application-local.properties        # Local dev overrides (datasource, API keys)
│   │       ├── application-prod.properties         # Production overrides
│   │       ├── logback-spring.xml                  # Structured logging (console + rotating files)
│   │       ├── db/migration/                       # Flyway SQL migrations (V1__init.sql, ...)
│   │       ├── static/
│   │       │   ├── css/
│   │       │   │   └── main.css                    # Design tokens, components, responsive layout
│   │       │   └── js/
│   │       │       ├── api.js                      # All REST fetch wrappers + Auth helpers
│   │       │       ├── main.js                     # Global UI utilities (toast, tabs, sheet, etc.)
│   │       │       ├── map.js                      # Leaflet map init, clustering, popups
│   │       │       ├── explore.js                  # Snap-scroll feed, search, sort
│   │       │       ├── community.js
│   │       │       └── websocket.js                # WebSocket stub (backend endpoint pending)
│   │       └── templates/
│   │           ├── fragments/
│   │           │   ├── topbar.html
│   │           │   └── navbar.html
│   │           ├── layout/
│   │           │   └── base.html                   # Thymeleaf layout template
│   │           ├── login.html
│   │           ├── register.html
│   │           ├── map.html
│   │           ├── explore.html
│   │           ├── tours.html
│   │           ├── listings-new.html
│   │           ├── account.html
│   │           ├── account-edit.html
│   │           ├── profile.html
│   │           ├── requests.html
│   │           ├── ratings.html
│   │           └── community.html
│   └── test/
│       ├── java/hr/tvz/experimate/experimate/
│       │   ├── AbstractIntegrationTest.java        # Base class for IT tests (shared PostgreSQL container)
│       │   ├── ExperiMateApplicationIT.java        # Context-load smoke test
│       │   └── domain/ · security/ · push/ · ...   # 40+ unit (*Test) + Testcontainers IT (*IT) across all domains
│       └── resources/
│           └── application-test.properties         # Test profile (dummy keys, no datasource URL)
├── http/
│   └── requests.http                               # HTTP request scratch file for manual API testing
├── pom.xml
└── README.md
```

### Architecture Notes

- **No business logic on the frontend.** All data lives in the backend; the browser only renders and sends forms.
- **Event-driven internals.** Domain events (`BookingRequestAcceptedEvent`, `RatingRecalculatedEvent`, etc.) decouple services — for example, accepting a booking request automatically creates the reservation and declines all competing requests via Spring's `ApplicationEventPublisher`.
- **Scheduled cleanup.** Expired listings and closed reservations are automatically cleaned up on a 30-minute schedule.
- **JWT + refresh cookie.** The access token lives in `localStorage`; the refresh token is an HTTP-only cookie. `apiFetch` silently retries on 401, and redirects to `/login` only if the refresh also fails.

---

## Prerequisites

| Requirement | Minimum version                                    |
|---|----------------------------------------------------|
| Java (JDK) | 25                                                 |
| Maven | 3.9+ (or use the included `mvnw` wrapper)          |
| Docker Desktop | Latest                                             |
| Browser | Any modern browser (Chrome, Firefox, Safari, Edge) |

Docker Desktop is required for two things: running the local PostgreSQL container for development, and running integration tests (Testcontainers automatically spins up a separate PostgreSQL container during `mvn verify`).

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/DadoTipkaMado/Experimate-deploy.git
cd Experimate-deploy
```

### 2. Start the database

```bash
docker run --name experimate-db \
  -e POSTGRES_DB=<your-db-name> \
  -e POSTGRES_USER=<your-user> \
  -e POSTGRES_PASSWORD=<your-password> \
  -p 5432:5432 -d postgres:16
```

> Pick any values you like — you'll point the app at this database in step 3.

### 3. Add required configuration

The `application.properties` file requires a JWT secret and database path before the app can start. 

### 4. Build and run

```bash
mvn spring-boot:run
```

The application starts on **http://localhost:8080**.

### 5. Open the app

Navigate to [http://localhost:8080](http://localhost:8080) — you will be redirected to the login page.

- **Register** a new account (date of birth required, must be 18+)
- **Log in** with your credentials
- You will land on the **Map** page

### 6. Explore the app

| Page | URL | What to do |
|---|---|---|
| Map | `/map` | See all listings as pins; tap a pin for details |
| Tours | `/tours` | Browse listings; request a meetup; manage your schedule |
| New listing | `/listings/new` | Create a listing as a host |
| Explore | `/explore` | Snap-scroll through user profiles; search by name |
| Requests | `/requests` | Accept or decline incoming booking requests (host) |
| Account | `/account` | View stats, bio, ratings; edit profile |
| API Docs | `/swagger-ui/index.html` | Interactive Swagger UI for all REST endpoints |

---

## Configuration

All configurable values live in `src/main/resources/application.properties`.

Local development configuration lives in `src/main/resources/application-local.properties` (not committed — create it from the template below).

| Property | Description |
|---|---|
| `spring.datasource.url` | PostgreSQL JDBC URL — e.g. `jdbc:postgresql://localhost:5432/experimateDb` |
| `spring.datasource.username` | PostgreSQL username |
| `spring.datasource.password` | PostgreSQL password |
| `jwt.secret` | Base64-encoded HMAC-SHA256 signing key (≥ 256 bits / 32 bytes decoded) |
| `spring.ai.anthropic.api-key` | Anthropic API key — required for AI matching features |

Base config properties (in `application.properties`, safe to leave as-is for development):

| Property | Default | Description |
|---|---|---|
| `jwt.expiration` | `300000` | Access token lifetime in milliseconds (5 min) |
| `refresh-token.expiration` | `604800000` | Refresh token lifetime in milliseconds (7 days) |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Hibernate validates schema — Flyway manages all DDL |

---

## API Reference

The full API is documented via Swagger UI at `/swagger-ui/index.html` when the application is running.

### Summary of endpoints

| Resource | Key endpoints |
|---|---|
| Auth | `POST /api/auth/login`, `/refresh`, `/logout`, `/google`, `/google/complete-registration`, `/verify-email`, `/resend-verification`, `/password-reset/request`, `/password-reset/confirm`; `GET /api/auth/google-client-id` |
| Users | `POST /api/user`, `GET /api/user`, `GET /api/user/me`, `GET /api/user/{id}`, `GET /api/user/by-username/{username}`, `GET /api/user/search`, `PATCH/DELETE /api/user/{id}`, profile-photo upload / delete / serve |
| Tour Listings | `GET/POST /api/tour-listing`, `GET /api/tour-listing/mine`, `GET/PATCH/DELETE /api/tour-listing/{id}`, `POST /api/tour-listing/{id}/start-tour`, `POST /api/tour-listing/from-partner-event` |
| Booking Requests | `POST /api/booking-request`, `GET /api/booking-request/mine`, `GET /api/booking-request/{id}`, `PATCH /api/booking-request/accept/{id}`, `/decline/{id}`, `DELETE /api/booking-request/{id}` |
| Reservations | `GET /api/reservation/mine`, `GET /api/reservation/{id}`, `GET /api/reservation/{id}/presence`, `PATCH /api/reservation/check-in/{id}`, `/end-tour/{id}`, `/cancel-tour/{id}`, `DELETE /api/reservation/{id}` |
| Ratings | `GET/POST /api/rating`, `GET/PATCH/DELETE /api/rating/{id}` |
| Onboarding | `GET /api/onboarding/questions`, `/status`, `POST /api/onboarding/answers`, `/cancel`, `DELETE /api/onboarding/data` |
| Match (AI) | `GET /api/match`, `GET /api/match/{candidateId}/explain` *(PREMIUM_USER)* |
| Feed | `GET /api/feed` |
| Premium / Pricing | `POST /api/premium/purchase`, `GET /api/pricing` |
| Partner | `GET /api/partner/status`, `/profile`, `/stats`, `/events`, `POST /api/partner/apply`, `DELETE /api/partner` *(PARTNER)* |
| Partner Pins | `GET/POST /api/partner-pins`, `GET /api/partner-pins/mine`, `GET/PUT/DELETE /api/partner-pins/{id}`, logo upload / serve, `POST/GET/DELETE /api/partner-pins/{id}/subscription` *(PARTNER)* |
| Partner Events | `POST/GET /api/partner-pins/{pinId}/events`, `GET /api/partner-events/upcoming`, `GET/PUT/DELETE /api/partner-events/{id}`, `POST/DELETE /api/partner-events/{id}/promote` *(PARTNER)* |
| Promoted Ads | `POST /api/promoted-ads`, `GET /api/promoted-ads/mine`, `PUT/DELETE /api/promoted-ads/{id}`, `POST /api/promoted-ads/{id}/extend`, image upload / serve *(PARTNER)* |
| Push | `GET /api/push/vapid-public-key`, `POST /api/push/subscribe`, `/unsubscribe` |

All `/api/**` endpoints require a valid `Authorization: Bearer <token>` header, **except** the public ones: `POST /api/auth/**`, `GET /api/auth/google-client-id`, `POST /api/user`, and the file-serving GETs (`/api/user/profile-photo/*`, `/api/partner-pins/logo/*`, `/api/promoted-ads/image/*`). Endpoints tagged *(PARTNER)* / *(PREMIUM_USER)* additionally require that role.

---

## Testing

Integration tests use a real PostgreSQL instance via Testcontainers — no mocks, no in-memory database. Docker Desktop must be running.

```bash
mvn verify
```

Unit tests (`*Test.java`) run during the `test` phase. Integration tests (`*IT.java`) run during the `integration-test` phase via Maven Failsafe.

All integration tests extend `AbstractIntegrationTest`, which provides:
- A shared PostgreSQL container (started once per test class)
- A `TestRestTemplate` wired to the random server port
- A mocked `ChatClient` so Spring AI never calls the Anthropic API
- A `@BeforeEach` hook that truncates all tables so each test starts with a clean database

---

## CI/CD & Deployment

**Continuous Integration.** Every push and every pull request runs the full build and test suite through GitHub Actions (`.github/workflows/ci.yml` → `mvn verify`), spinning up a real PostgreSQL container via Testcontainers. The `main` and `develop` branches are protected: a pull request cannot be merged unless the CI check passes, so untested code never reaches `main`.

**Continuous Deployment.** The application is deployed on **Railway**, connected directly to the GitHub repository. Every merge to `main` automatically triggers a fresh deployment — Railway builds the image from the multi-stage `Dockerfile` and rolls it out with no manual steps. Because a green CI run is a required merge gate, only test-passing code is ever built and deployed, even though the production image build itself skips the test phase for speed.

**Production configuration.** The `prod` profile (`application-prod.properties`) reads every secret and connection detail from environment variables injected by Railway — datasource URL and credentials, `JWT_SECRET`, the Anthropic API key, SMTP settings, and the VAPID push keys. Nothing sensitive is committed to the repository.

---

## Roadmap

The current version represents the **MVP**. Planned future iterations:

- **AI integration** — smart tour recommendations and personalised explore feed
- **Gamification** — ELO-style rating system, seasonal points, achievement badges, and a premium multiplier (no paywalled features)
- **Community tab** — local activity groups (running, cycling, dog walking) and a social discovery section for people looking to hang out, not just tour
- **Trust & safety** — mutual blind reviews, user vouching, profile completeness score, verified phone/ID badge, response rate display
- **UX evolution** — feed-first layout, accessibility toolbar (dyslexia support), everything reachable in 3 taps

---

## Team

| Name | Role |
|---|---|
| **Vito** | Frontend, Thymeleaf templates, view controllers, map, UI/UX |
| **David** | Backend, REST API, security, database, domain events |

---

## License

This project was created for academic and competition purposes.
