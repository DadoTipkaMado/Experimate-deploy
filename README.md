# Experimate

Experimate is a web application designed to connect travelers with locals through real-life experiences in a city.  
The platform focuses on discovering authentic local experiences, meeting people **offline**, and exploring **hidden gems** recommended by locals.

Unlike traditional social platforms, Experimate does **not promote online chatting**.  
The main goal is to encourage people to meet **in person** and experience cities together.

---

# Tech Stack

## Backend
The backend application will be developed using **Spring Boot**, which will act as the web server and API layer responsible for handling:

- user accounts
- communities
- events
- map locations
- application logic

Spring Boot will serve both the application logic and the rendered frontend pages.

## Frontend
The frontend will be implemented using **Thymeleaf**, a template engine that generates HTML pages directly from the Java backend.

This approach keeps the architecture simple and avoids the complexity of modern JavaScript frameworks.  
Minimal JavaScript will be used only where necessary.

## Maps
The interactive map functionality will be implemented using **Leaflet**, a lightweight JavaScript library for working with maps based on **OpenStreetMap** data.

The map will allow users to:

- explore local experiences
- view events
- discover hidden gems added by locals

## Database
The application will use a relational database such as:

- **PostgreSQL**

The database will store:

- user accounts
- communities
- events
- hidden locations
- ratings

## Hosting
The application will run as a **Spring Boot web server** and will be deployed on a **VPS server**.

This architecture keeps the infrastructure simple and inexpensive while remaining scalable enough for the project's needs.

---

# Core Concept

Experimate aims to solve a simple problem:

**Tourists rarely experience cities the way locals do.**

The platform allows locals to create personalized experiences that travelers can join in real life.

---

# Main Features

## Local Experience Matching

Locals can create an account and describe:

> "How would you spend the perfect day in your city as a tourist?"

The platform will use **AI to standardize and structure the description** into a clear experience plan.

Users can also manually customize parts of the experience.

Travelers can then join a local on a selected day and experience the city according to that plan.

A **custom rating system** evaluates the authenticity and quality of the experience.

---

## Hidden Gems Finder

Locals can add locations directly on the embedded map.

These locations represent lesser-known places worth visiting, such as:

- cafés
- viewpoints
- parks
- local restaurants
- unique spots

Each hidden gem can be rated by visitors.

---

## No DM Policy

The platform intentionally **does not include private messaging**.

The goal is to encourage **real-life interaction instead of endless online chatting**.

Users connect through:

- events
- local experiences
- communities

---

## Communities (Potential Feature)

Users will be able to browse and join communities.

Communities can:

- organize events
- place events on the map

There will be **no internal chat**, but hovering over an event will display event details.

---

# UI Concept

The interface will follow a **map-first design**.

Main navigation will include:

- Map / Home
- Experiences
- Communities
- Account

The design inspiration is a **TikTok-like browsing experience**, adapted to discovering real-world locations and events.

---

# Project Goals

The project focuses on:

- simple architecture
- minimal frontend complexity
- fast development iteration
- real-world usability

The goal is to build a functional platform without relying on heavy frontend frameworks.

---

# Project Name

**Experimate**

*(Experience + Explore + Meet)*
