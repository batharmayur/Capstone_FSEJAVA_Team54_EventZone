# EventZone Monorepo

This project is organized into two separate services:

- `eventzone-frontend/` — React user interface for attendee, organiser, and admin dashboard
- `eventzone-backend/` — Spring Boot API service for event catalog, ticket booking, and business logic

## Tech Stack

- Frontend: React 18 (hooks, Context API, React Router), Vite, TypeScript, Tailwind CSS
- Backend: Java 17+, Spring Boot 3.3, Spring Web, Spring Data JPA, Bean Validation, Spring Security (stateless JWT, BCrypt)
- Database: PostgreSQL (production profile), H2 in-memory (local and tests)
- API docs and testing: springdoc OpenAPI / Swagger UI, JUnit 5, Mockito, MockMvc, Postman collection (runnable with newman)
- Dev tooling: Docker Compose (PostgreSQL), Maven, npm

## Project Structure

- `eventzone-frontend/` — React UI (`src/pages`, `src/components`, `src/context`, `src/services`)
- `eventzone-backend/` — Spring Boot API (`controller`, `service`, `repository`, `model`, `dto`, `security`, `exception`, `config`)
- `postman/` — Postman collection covering every endpoint (49 requests, 82 assertions)
- `docker-compose.yml` — PostgreSQL instance for local development

## Frontend Setup

```bash
cd eventzone-frontend
npm install
npm run dev
```

Frontend runs at:

```bash
http://localhost:3000
```

Frontend reads the backend API from `VITE_API_BASE_URL` when provided. During local development, `/api/*` requests are proxied to `http://localhost:8080` by Vite.

## Backend Setup

Run order: 1) start a database, 2) run Spring Boot (sample data is seeded automatically when the database is empty), 3) run the UI.

**Option A: in-memory H2 (no Docker needed, data resets on restart)**

```bash
cd eventzone-backend
DB_URL="jdbc:h2:mem:eventzone;MODE=PostgreSQL" DB_USERNAME=sa DB_PASSWORD= \
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver \
SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.H2Dialect \
mvn spring-boot:run
```

**Option B: PostgreSQL**

```bash
docker compose up -d
cd eventzone-backend
mvn spring-boot:run
```

The defaults in `eventzone-backend/src/main/resources/application.properties` match `docker-compose.yml`. Override with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` and `JWT_EXPIRATION_MINUTES` as needed.

Backend runs at `http://localhost:8080` (health check: `http://localhost:8080/api/health`).

## API Documentation and Postman

- **Swagger UI:** `http://localhost:8080/swagger-ui.html` (use *Authorize* and paste the token from `POST /api/auth/login`).
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
- **Postman:** import `postman/EventZone.postman_collection.json` and run the collection in order. Test scripts store tokens and ids in collection variables. Command line: `npx newman run postman/EventZone.postman_collection.json` (backend must be running with the seed data).

## Testing

```bash
cd eventzone-backend && mvn test       # 71 tests: unit (JUnit 5 + Mockito) and API (MockMvc) tests on H2
cd eventzone-frontend && npm test      # 12 tests: Vitest + Testing Library (booking form, role-gated routes)
cd eventzone-frontend && npm run build # type-checks and builds the UI
```

Backend tests cover the services' business rules, role and ownership checks, validation, the error format, and a concurrency test that fires 10 simultaneous bookings at 3 seats. Frontend tests cover the booking form (available categories, quantity limits, total, success, server error, expired session, sold out) and the role-based route guard.

## Seed Data

Loaded by `DataSeeder` on first start (only into an empty database):

| Account | Role | Password |
| --- | --- | --- |
| `admin@eventzone.com` | ADMIN | `Password@123` |
| `org1@eventzone.com` (Arjun Events) | ORGANISER | `Password@123` |
| `user1@eventzone.com` (Divya) | ATTENDEE | `Password@123` |

Categories: Concert, Sports, Workshop, Conference. Six upcoming events (dates are relative to the first start) owned by `org1`, each with 1-2 ticket categories: Rock Night, Indie Sunset Sessions, Championship Weekend, City Marathon, Spring Boot Hands-on Workshop, AI Product Meetup. These demo credentials are for local development only; remove them before any real deployment.

## Implemented Modules

### Event Listing and Detail (Sprint 1)

- `GET /api/events` — active events ordered by date, with ticket price range (`minPrice`/`maxPrice`)
- `GET /api/events?category=Sports` — filter by category name (case-insensitive)
- `GET /api/events/{id}` — event detail with ticket categories (price, total and available seats); 404 if missing or deactivated
- `GET /api/categories` — event categories used by the filter tabs
- Errors use the spec format: `{timestamp, path, error, message}` (`NOT_FOUND`, `VALIDATION_ERROR`).
- Sample categories, events, and ticket categories are seeded automatically on first start when the database is empty.
- Frontend: event list at `/` (category tabs, event cards) and event detail at `/events/:id`.

### Authentication (Sprint 2, part 1)

- `POST /api/auth/register` `{email, password, name}` → 201 with the new user. Always creates an `ATTENDEE`; a role in the request is ignored. Duplicate email → 409.
- `POST /api/auth/login` `{email, password}` → 200 `{token, expiresAt, user}`. Wrong email or password → 401 with one generic message.
- `POST /api/auth/logout` → 204. Tokens are stateless, so the client discards its token.
- Send the token as `Authorization: Bearer <token>`. Public endpoints: `GET /api/events`, `/api/events/{id}`, `/api/categories`, `/api/health`, and the auth endpoints above. Everything else requires a valid token (401 otherwise).
- Passwords are hashed with BCrypt (8-72 characters). Emails are stored lowercase.
- Frontend: `/login` and `/register` pages, `AuthContext` (session kept in `localStorage`, cleared on expiry), and a navbar that shows the signed-in user.

**Demo accounts:** see [Seed Data](#seed-data).

**Configuration:** `JWT_SECRET` (at least 32 bytes) and `JWT_EXPIRATION_MINUTES` (default 60). The built-in default secret is for local development only.

**JWT vs session cookie (trade-offs):** a stateless JWT needs no server session store and suits a separate SPA and API, but it cannot be revoked before it expires, and the role in the token is not refreshed until the next login. Storing it in `localStorage` is simple but readable by any script on the page (XSS); an `HttpOnly` cookie would avoid that at the cost of CSRF protection. Tokens are short-lived (60 minutes) to limit exposure.

### Ticket Booking (Sprint 2, part 2)

All booking endpoints require a signed-in user; the user always comes from the token, never from the request body.

- `POST /api/bookings` `{ticketCategoryId, quantity}` (quantity 1-5) → 201 with `bookingRef` (e.g. `EZ-7KQ2M9XA`), status `CONFIRMED` and the total price at booking time. The booked quantity is deducted from `availableSeats`.
- `GET /api/bookings/mine` → the signed-in user's bookings, newest first.
- `PUT /api/bookings/{id}/cancel` → status `CANCELLED` and the seats are restored. Cancelling twice → 409. Another user's booking → 404 (so booking ids are not leaked).
- Booking is rejected with 409 when the category is sold out, fewer seats remain than requested, or the event is inactive or already past.
- **Concurrency:** seats are taken with one atomic conditional `UPDATE ... WHERE available_seats >= qty`, so simultaneous requests can never oversell (covered by a 10-parallel-request test against 3 seats). Cancellation locks the booking row so seats cannot be restored twice.
- Frontend: "Book tickets" on the event detail page opens a modal (category, quantity, total) and shows the booking reference; signed-out visitors see "Log in to book tickets" and return to the event after login. `/bookings` (My Bookings, protected) lists bookings with Cancel.

### Organiser Dashboard and Admin (Sprint 3)

Roles are checked in the security configuration (by endpoint) and ownership is checked in the service layer. Violations return 403 in the standard error format.

**Organiser** (`ORGANISER`; an organiser can only touch their own events, an admin can edit or delete any event):
- `POST /api/events` `{title, description, eventDate, venue, coverImageUrl, categoryId}` → 201. The event is in the public catalog immediately. `eventDate` must be in the future; `coverImageUrl` must be an http(s) URL or a site-relative path.
- `PUT /api/events/{id}`, `DELETE /api/events/{id}` (organiser owner or admin). An event that has bookings cannot be deleted (409); an admin can deactivate it instead.
- `POST /api/events/{id}/ticket-categories` `{name, price, totalSeats}`, `PUT` and `DELETE /api/ticket-categories/{id}`. Editing the total adjusts available seats atomically and cannot drop below the seats already booked (409). A category with bookings cannot be deleted.
- `GET /api/organiser/events` → the organiser's events (including deactivated ones) with, per ticket category, `bookedSeats` and `bookingCount` (confirmed bookings).
- Frontend: **My Events** (`/organiser`) with add/edit/delete event, add/edit/delete ticket category, and booking figures.

**Admin** (`ADMIN`, everything under `/api/admin`):
- `GET /api/admin/events` → all events with organiser and status.
- `PUT /api/admin/events/{id}/active` `{active}` → deactivate or reactivate. Inactive events vanish from the public catalog and cannot be booked; existing bookings are kept.
- `POST`, `PUT`, `DELETE /api/admin/categories[/{id}]` → category CRUD. Names are unique (case-insensitive); a category used by events cannot be deleted. New categories are immediately available when an organiser creates an event.
- Frontend: **Admin** (`/admin`) with category management and event activation. Pages for the wrong role show "Access denied".

Not implemented: deactivating users (the spec lists it under the admin role but not under the in-scope features), and organiser accounts are seeded rather than promoted through the UI. Because the role is read from the token, a role change applies at the next login.

## Constraints and Trade-offs

- Scope is the Foundation version: no payment gateway or QR codes. Deactivating users is not built (the spec lists it under the admin role but not in the in-scope features).
- Roles come from the JWT, so a role change or a deactivated account takes effect at the next login or token expiry (see the JWT trade-offs above).
- Events with bookings cannot be deleted, so booking history stays intact; admins deactivate them instead.
- Prices are shown in INR and event times are local date-times without a time zone.
- Sample cover images are local SVGs served by the frontend; organisers can supply any http(s) URL or site-relative path.
- Frontend tests are limited to the two key components (booking form, route guard), as the spec asks; there are no end-to-end browser tests.

## Generative AI Usage

The project was built with GitHub Copilot in agent mode inside VS Code, working incrementally from the requirement document. Examples of the prompts given:

- "as per requirement document, frontend should be in react so please update the frontend project accordingly"
- "As we need to start with the event listing module as per mentioned in requirement document so please start incrementally implementation"
- "image are not displaying" (diagnosed as blocked external image hosts; replaced by local images with a fallback)
- "as per requirement document go with next step implementation" (used for the booking, organiser and admin modules)
- "remove unwanted files as whatever not needed"

AI output was reviewed, and each step was checked by running the unit and API tests, exercising the endpoints with curl and Postman/newman, and trying the UI in a browser. Design points settled during review: seat counts change only through atomic SQL updates to prevent overselling under concurrency, and Spring's auto-generated default user is disabled.

## Production Notes

- Use strong secrets for authentication and database credentials.
- Deploy frontend and backend separately to managed hosting services.
- Add CI/CD, environment-based config, and monitoring before production release.
