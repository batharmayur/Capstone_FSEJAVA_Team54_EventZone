# EventZone

This project is organized into two separate services:

- `eventzone-frontend/` — React user interface for attendee, organiser, and admin dashboard
- `eventzone-backend/` — Spring Boot API service for event catalog, ticket booking, and business logic

## Tech Stack

- Frontend: React, Vite, TypeScript, Tailwind CSS
- Backend: Java 17, Spring Boot 3, Spring Web, Spring Data JPA
- Database: PostgreSQL
- Dev tooling: Docker Compose, Maven, Spring Boot

## Project Structure

- `eventzone-frontend/` — UI application and project pages
- `eventzone-backend/` — Java Spring Boot API server and database configuration
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

```bash
cd eventzone-backend
mvn clean install
mvn spring-boot:run
```

Backend runs at:

```bash
http://localhost:8080/api/health
```

## Database

Start PostgreSQL locally:

```bash
docker compose up -d
```

Then configure your environment variables or use the default values in `eventzone-backend/src/main/resources/application.properties`:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/eventzone
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
```

Then run the Spring Boot app:

```bash
cd eventzone-backend
mvn spring-boot:run
```

## Production Notes

- Use strong secrets for authentication and database credentials.
- Deploy frontend and backend separately to managed hosting services.
- Add CI/CD, environment-based config, and monitoring before production release.
