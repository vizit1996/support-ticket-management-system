# Support Ticket Management System

A spec-driven support ticket application with a Java 21 / Spring Boot 3 backend and a Next.js / React frontend.

## Project layout

- `backend/` — Spring Boot API, domain model, JPA persistence, and tests
  - `ticket/controller/` — REST controllers and global HTTP exception handling
  - `ticket/dto/` — immutable request and response records
  - `ticket/service/`, `ticket/repository/`, `ticket/domain/` — application, persistence, and domain layers
- `frontend/` — Next.js App Router UI and API proxy
- `spec/` — functional requirements, state machine, API contract, and data model
- `.cursor/rules/` — project coding and testing conventions
- `docs/` — AI corrections audit and prompt history

## Toolchain

- **Java 21** — pinned via `.sdkmanrc`
- **Maven 3.8+**
- **Node 22** — pinned via `.nvmrc`

## Local development

### 1. Start the backend

For local development, use the in-memory H2 profile:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

The API is available at `http://localhost:8080/api/v1`.

### 2. Start the frontend

In another terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:3000`. The frontend proxies `/api/v1` to `BACKEND_URL`, which defaults to `http://localhost:8080`.

If Linux file-watch limits prevent development mode, use the existing production build:

```bash
npm run build
npm start
```

## PostgreSQL

The default backend profile uses PostgreSQL and Flyway. Set these variables before starting without the `h2` profile:

```bash
export DATABASE_HOST=localhost
export DATABASE_PORT=5432
export DATABASE_NAME=support_tickets
export DATABASE_USER=support
export DATABASE_PASSWORD='set-locally'
cd backend
mvn spring-boot:run
```

Do not commit database passwords or other credentials.

## Verification

```bash
cd backend && mvn test
cd ../frontend && npm run typecheck && npm test && npm run build
```
