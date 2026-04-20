# ExperiMate Backend — Claude Instructions (david/backend)

## Core Working Principles (based on Andrey Karpathy's approach)

### 1. Think Before Coding
- **State assumptions explicitly.** If uncertain about requirement, ask before coding.
- **Surface confusion and tradeoffs.** Don't make silent architectural decisions.
- **Present multiple approaches** when they exist (e.g., exception handling, database normalization).
- **Stop to clarify** confusing or vague requests — weak requirements lead to rework.

### 2. Simplicity First
- **Minimal code addressing exact request.** No speculative features or over-engineering.
- **Avoid unnecessary abstractions.** Three similar lines ≠ premature abstraction needs refactoring.
- **No error handling for impossible scenarios.** Trust framework guarantees and internal code.
- **Validate only at boundaries:** user input, external APIs. Don't validate internal contracts.
- **Benchmark:** Would a senior Java engineer call this overcomplicated?

### 3. Surgical Changes
- **Only modify what's necessary.** Every changed line serves the stated goal.
- **Match existing code style.** Even if preferring alternatives, stay consistent with codebase.
- **Remove only what YOUR edits made obsolete.** Don't eliminate pre-existing dead code unless requested.
- **Keep diffs focused.** One change = one problem solved, not a refactoring pass.

### 4. Goal-Driven Execution
- **Transform vague requests into success criteria.** "Fix the bug" → "POST /api/booking-request returns 201 with guest + listing fields."
- **Use tests to validate.** Run tests after changes; don't claim success without evidence.
- **Outline brief plans for multi-step tasks** with checkpoints (e.g., "Step 1: Add field to entity, Step 2: Update DTO, Step 3: Test endpoint").
- **Strong criteria enable independence.** Weak criteria ("make it work") require constant clarification.

---

## David's Backend Rules

### Scope
- **Backend only:** Java, Spring Boot, REST APIs, databases, business logic.
- **Do NOT touch:** `src/main/resources/` (Vito's HTML/CSS/JS), `src/main/java/.../view/` (Vito's view controllers).
- **Reading any file for context is always fine** — understanding the full system is part of the job.

### Git Workflow
- **Branch:** All work on `david/backend`, commit only Java files.
- **Merge into main:** After coordinating with Vito, David merges both branches.
- **No overlapping folders** — no merge conflicts by design.

### Current Architecture
- **DTOs:** Request/Response DTOs for API contracts (shape defined in CLAUDE.md main).
- **Controllers:** `controller/` package for REST endpoints.
- **Services:** Business logic, transactions, validation.
- **Repositories:** Spring Data JPA, no raw SQL unless necessary.
- **Entities:** JPA entities with proper annotations.
- **Events:** Spring events for inter-domain communication (e.g., `TourListingReservedEvent`).

### Common Patterns

**Exception Handling:**
- Use Spring `@ExceptionHandler` methods in controllers OR a `GlobalExceptionHandler`.
- **Don't write:** GlobalExceptionHandler + new *ErrorResponse for every validation error.
- **Do:** Leverage framework + let Claude handle specifics when needed.

**Transactions:**
- `@Transactional` on service methods that modify multiple entities.
- **Common bug:** Setting a field without explicit `.save()` when `@Transactional` isn't present (e.g., `listing.setReserved(true)` without `listingRepo.save(listing)`).

**DTOs:**
- Request DTOs for incoming JSON (with validation annotations like `@NotNull`).
- Response DTOs for outgoing JSON (flatten nested objects as needed for frontend).
- Keep shapes consistent with what frontend expects (e.g., `listing.lat` / `listing.lng`, not `latitude`/`longitude`).

**Enums in Database:**
- Use `@Enumerated(EnumType.STRING)` for readability.
- Add `@Column(columnDefinition = "VARCHAR(20)")` to avoid database-specific ENUM type issues (H2 creates ENUM, breaks queries).

---

## Success Criteria for a Good Session

- [ ] Assumptions stated upfront, confusing requests clarified before coding.
- [ ] Diffs are focused — one change per problem, no speculative cleanup.
- [ ] Code matches existing style (Spring patterns, naming, structure).
- [ ] Tests pass; changes validated, no regressions introduced.
- [ ] Plan outlined for multi-step tasks with checkpoints.

### When creating a new function or class
- Add respective, clear and concise documentation that explains clearly what the method does. 
- Use javadoc format for said documentation and always check and correct the documentation of referenced or classes.
- I want all the documentation to be valid and change when the context of where the methods are used and the way they rare used changes

### Focus on learning
- I am still relatively new to complex development and shipping industry standard solutions and that is what i want to learn.
- Focus on explaining the what and why of every idea that you recommend to me. I want to be able to write industry standard and competent code.
- I do not want to blindly let you code for me. If I am facing a new concept, I want to fully understand it before implementation.