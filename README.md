# ExperiMate

> **Connect with locals. Get real experiences.**

ExperiMate is a full-stack web platform that connects travellers with local hosts who offer authentic, in-person city tours. Hosts publish tour listings; guests discover them, send booking requests, check in on the day, and rate each other after — all without any messaging or intermediary. The premise is simple: real people, real places, no DMs.

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
- [Roadmap](#roadmap)
- [Team](#team)

---

## Overview

ExperiMate was built for students and travellers who want to go beyond tourist traps and experience a city through the eyes of someone who actually lives there. A local host creates a tour listing — a date, a location, and a description of what they'll show. A guest discovers it on the map or explore feed, sends a booking request, and if accepted, meets the host IRL. After the tour both parties rate each other, building a reputation system that rewards good hosting and respectful guests.

The application follows a **mobile-first, progressive web app** philosophy — it is designed to feel native on a phone while adapting gracefully to desktop via a sidebar layout.

---

## Features

### For Guests
- Browse and search available tour listings on an interactive map or card feed
- Filter listings by city, date, and availability
- Send booking requests to hosts
- Check in on the day with the "I'm here" button — tour activates when both parties confirm
- Rate and review hosts after the tour is completed
- View profile pages of prospective hosts including their bio and upcoming listings

### For Hosts
- Create tour listings with a location picker, date/time, and detailed description
- Manage incoming booking requests (accept or decline)
- Check in alongside guests to activate the tour
- End the tour to trigger the rating flow
- View hosted tours and listing status (Available / Requested / Booked)

### Platform
- JWT authentication with automatic silent token refresh via HTTP-only refresh cookie
- Snap-scroll TikTok-style explore feed with search and sort
- Dark-mode UI with teal/orange accent theme
- Fully responsive — phone layout below 900 px, sidebar grid above
- Profile photo upload with client-side canvas compression
- Profile completeness nudge, host stats, and booking request badge

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Security, Spring Data JPA |
| Database | H2 (embedded, file-based) |
| Auth | JWT (jjwt 0.12) + HTTP-only refresh token cookie |
| Frontend | Thymeleaf templates, Vanilla JS (no framework, no build step) |
| Map | Leaflet.js + Leaflet.markercluster |
| Geocoding | Nominatim (OpenStreetMap) |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Build | Maven (Maven Wrapper included) |

---

## Project Structure

```
ExperiMate/
├── src/
│   ├── main/
│   │   ├── java/hr/tvz/experimate/experimate/
│   │   │   ├── ExperiMateApplication.java          # Entry point
│   │   │   ├── config/
│   │   │   │   └── SecurityConfig.java             # Spring Security + JWT filter chain
│   │   │   ├── controller/                         # REST API controllers
│   │   │   │   ├── AuthController.java             # POST /api/auth/login, /refresh
│   │   │   │   ├── UserController.java             # CRUD /api/user
│   │   │   │   ├── TourListingController.java      # CRUD /api/tour-listing
│   │   │   │   ├── ReservationController.java      # /api/reservation + check-in/end/cancel
│   │   │   │   ├── BookingRequestController.java   # /api/booking-request + accept/decline
│   │   │   │   └── RatingController.java           # CRUD /api/rating
│   │   │   ├── model/                              # Domain model (entity + service + repo + DTOs)
│   │   │   │   ├── user/
│   │   │   │   ├── tour_listing/
│   │   │   │   ├── reservation/
│   │   │   │   ├── booking_request/
│   │   │   │   ├── rating/
│   │   │   │   ├── refresh_token/
│   │   │   │   └── shared/                         # DTOs, events, exceptions, utilities
│   │   │   ├── security/                           # JWT service, auth filter, UserDetails
│   │   │   └── view/                               # Thymeleaf view controllers (no business logic)
│   │   │       ├── AuthViewController.java
│   │   │       ├── MapController.java
│   │   │       ├── ExploreController.java
│   │   │       ├── TourListingViewController.java
│   │   │       ├── AccountViewController.java
│   │   │       └── CommunityViewController.java
│   │   └── resources/
│   │       ├── application.properties              # DB, JWT, refresh token config
│   │       ├── logback-spring.xml                  # Structured logging (console + rotating files)
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

| Requirement | Minimum version |
|---|---|
| Java (JDK) | 21 |
| Maven | 3.9+ (or use the included `mvnw` wrapper) |
| Browser | Any modern browser (Chrome, Firefox, Safari, Edge) |

No database installation is required — the app uses an embedded H2 file database.

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/your-org/experimate.git
cd experimate
```

### 2. Add required configuration

The `application.properties` file requires a JWT secret and database path before the app can start. Copy the example below into `src/main/resources/application.properties` (or add the missing keys to an existing file):

```properties
# JWT — must be a Base64-encoded secret of at least 256 bits
jwt.secret=ZXhwZXJpbWF0ZWxvY2FsZGV2c2VjcmV0a2V5Zm9ydGVzdGluZ29ubHkyMDI2

# H2 file database (created automatically on first run)
spring.datasource.url=jdbc:h2:~/experimateDb;AUTO_SERVER=TRUE
spring.datasource.username=
spring.datasource.password=
```

> ⚠️ The secret above is for local development only. Generate a new one for any shared or deployed environment.

### 3. Build and run

**Using the Maven Wrapper (recommended — no local Maven required):**

```bash
# macOS / Linux
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

**Using a locally installed Maven:**

```bash
mvn spring-boot:run
```

The application starts on **http://localhost:8080**.

### 4. Open the app

Navigate to [http://localhost:8080](http://localhost:8080) — you will be redirected to the login page.

- **Register** a new account (date of birth required, must be 18+)
- **Log in** with your credentials
- You will land on the **Map** page

### 5. Explore the app

| Page | URL | What to do |
|---|---|---|
| Map | `/map` | See all tour listings as pins; tap a pin for details |
| Tours | `/tours` | Browse listings; request a tour; manage your meetups |
| New listing | `/listings/new` | Create a tour as a host |
| Explore | `/explore` | Snap-scroll through user profiles; search by name |
| Requests | `/requests` | Accept or decline incoming booking requests (host) |
| Account | `/account` | View stats, bio, ratings; edit profile |
| API Docs | `/swagger-ui/index.html` | Interactive Swagger UI for all REST endpoints |
| H2 Console | `/h2-console` | Inspect the database directly |

---

## Configuration

All configurable values live in `src/main/resources/application.properties`.

| Property | Default | Description |
|---|---|---|
| `jwt.secret` | *(required)* | Base64-encoded HMAC-SHA256 signing key (≥ 256 bits) |
| `jwt.expiration` | `300000` | Access token lifetime in milliseconds (default: 5 min) |
| `refresh-token.expiration` | `604800000` | Refresh token lifetime in milliseconds (default: 7 days) |
| `spring.datasource.url` | *(required)* | H2 JDBC URL — use `~/experimateDb` for a home-directory file |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema strategy — `update` is safe for development |
| `spring.h2.console.enabled` | `true` | Enables the H2 web console at `/h2-console` |

---

## API Reference

The full API is documented via Swagger UI at `/swagger-ui/index.html` when the application is running.

### Summary of endpoints

| Resource | Endpoints |
|---|---|
| Auth | `POST /api/auth/login`, `POST /api/auth/refresh` |
| Users | `GET/POST /api/user`, `GET/PATCH/DELETE /api/user/{id}` |
| Tour Listings | `GET/POST /api/tour-listing`, `GET/PATCH/DELETE /api/tour-listing/{id}` |
| Booking Requests | `GET/POST /api/booking-request`, `PATCH /api/booking-request/accept/{id}`, `PATCH /api/booking-request/decline/{id}`, `DELETE /api/booking-request/{id}` |
| Reservations | `GET /api/reservation`, `PATCH /api/reservation/check-in/{id}`, `PATCH /api/reservation/end-tour/{id}`, `PATCH /api/reservation/cancel-tour/{id}`, `DELETE /api/reservation/{id}` |
| Ratings | `GET/POST /api/rating`, `GET/PATCH/DELETE /api/rating/{id}` |

All `/api/**` endpoints except `POST /api/auth/login` and `POST /api/user` require a valid `Authorization: Bearer <token>` header.

---

## Roadmap

The current version represents the **MVP**. Planned future iterations:

- **AI integration** — smart tour recommendations and personalised explore feed
- **Gamification** — ELO-style rating system, seasonal points, achievement badges, and a premium multiplier (no paywalled features)
- **Community tab** — local activity groups (running, cycling, dog walking) and a travel section for visiting users
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
