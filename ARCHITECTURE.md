# CLIMS Asset Management System — Architecture (System Architect)

This document is written from the perspective of the System Architect and provides a concise but thorough overview of the system's architecture, design rationale, operational runbook, and developer guidance. It complements the SRS and SDD by explaining the "why" behind major design decisions and providing practical guidance for developers and operators.

## Goals and constraints

- Provide a secure, auditable, and maintainable Asset Management platform supporting CRUD operations, audit trails, scheduled maintenance, reporting (PDF and KPIs), and role-based access for administrators and users.
- Keep the architecture simple and standard: Spring Boot for backend, React/Vite for frontend, OpenAPI for contract-driven client/server integration.
- Support manageable local development (dev profiles), automated testing (unit/integration/e2e), and straightforward CI/CD to staging/production.

## High-level architecture

- Frontend: Single Page Application (React + TypeScript, Vite). Responsible for user interaction, local validations, and presenting KPI dashboards. Communicates with backend via REST over HTTPS and uses OpenAPI-generated typings for type safety.
- Backend: Spring Boot monolith exposing a REST API. Responsible for authentication/authorization, business rules, data persistence, auditing, reporting, and integrations.
- Persistence: Relational database (Postgres recommended for production). Use H2 in-memory for local development and fast tests.
- Observability: Centralized logging (Spring Boot logs), application metrics for KPIs, and structured logs for audit trails.

Diagram (textual)

Frontend (Vite/React)
  ↕ HTTPS/REST (OpenAPI)
Backend (Spring Boot)
  ├─ Controllers (REST endpoints)
  ├─ Services (business logic)
  ├─ Repositories (JPA/Hibernate)
  └─ Jobs/Schedulers (maintenance tasks, report generation)
  ↕ JDBC/ORM
Database (Postgres/H2)

## Component responsibilities

- API layer (Controllers): Validate input, enforce security annotations, and orchestrate service calls.
- Service layer: Implement business rules (department scoping, asset lifecycle, validation rules). Keep side-effects explicit and transactional boundaries clear.
- Repository layer: Use Spring Data repositories and explicit queries where needed (avoid leaking domain logic into queries).
- Security: Centralize security configuration. Use JWT or scoped tokens with refresh rotation. Apply method + endpoint security for defense-in-depth.
- Frontend: Keep UI stateless where possible; maintain auth state in `AuthContext`, use typed API client and centralized error handling.

## Data flow and contracts

- All client-server interactions must follow the OpenAPI contract located in `docs/openapi/openapi-v1.json`.
- For complex updates (batch or multi-entity modifications), prefer explicit endpoints rather than multiple client side calls to ensure transactional consistency.

## Design decisions and rationale

- Monolith backend (Spring Boot): Chosen for simplicity, developer productivity, and ease of testing. The project's scope and team size favor a monolith over microservices.
- OpenAPI-first client/server integration: Keeps contracts explicit, reduces integration friction, and enables typed clients for the frontend.
- Use of standard frameworks (Spring, React, Tailwind): Reduce technical debt by leveraging community-proven patterns and libraries.

Trade-offs & alternatives

- Microservices were considered but rejected because the project demands (asset management, admin UI) benefit from strong transactional integrity and fewer operational overheads.
- GraphQL considered for flexible querying; rejected due to the simplicity of REST-based resources and existing OpenAPI tooling.

## Security model

- Authentication: Token-based flows with short-lived access tokens + refresh tokens. Protect refresh tokens and rotate them when used.
- Authorization: Role-based access control (RBAC) applied at both endpoint and method levels. Admin-only operations must be gated and strongly tested.
- Data protection: Use HTTPS in transit and strong password hashing (BCrypt). Encrypt secrets at rest (production keys/certs via secrets manager).

## Operational runbook (developer & operator tasks)

Local development (quick start):

1. Start backend in dev profile (H2) to avoid DB setup:

```pwsh
cd backend
./mvnw.cmd -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

2. Start frontend dev server:

```pwsh
cd frontend
npm install
npm run dev
```

3. Use the frontend to exercise endpoints; OpenAPI is available at `http://localhost:8080/v3/api-docs`.

Deploy to production (recommended minimal steps):

1. Build backend with Maven and package the jar.
2. Build frontend production assets and host them on a static server or CDN.
3. Provision a Postgres instance and configure environment variables.
4. Deploy backend jar to a JVM host (or containerize in Docker) behind HTTPS and a reverse proxy (Nginx/Traefik).

Healthchecks & monitoring

- Use Spring Actuator endpoints for health and metrics in production (expose only to internal networks).
- Configure log aggregation and alerting for 5xx rates, authentication failures, and scheduler errors.

## Testing strategy (from architect)

- Unit tests: cover services and utility functions. Keep them fast and deterministic.
- Integration tests: use an in-memory H2 or testcontainers Postgres for realistic DB behavior. Run critical path flows (auth, asset lifecycle, security edge cases).
- End-to-end tests: Playwright scripts for crucial user journeys (login, create asset, report generation). Keep a small, stable e2e suite in CI.

CI/CD guidance

- Stages: build → unit tests → integration tests → e2e smoke → deploy to staging → deploy to prod.
- Keep OpenAPI generation/validation in CI: fail the build if API contract changes without an accompanying schema update or migration notes.

Developer guidelines

- Keep controllers thin. Put business logic in services.
- Write tests for all security-critical paths.
- Prefer explicit migrations (Flyway/Liquibase) for DB schema changes; store migration files under `backend/src/main/resources/db/migration`.
- Document API changes in `docs/openapi` and update the frontend `api/schema.ts` as part of the same change set.

## Observability & troubleshooting

- Ensure logs include correlation IDs (X-Request-Id) to trace requests across frontend/backend.
- Add audit logs for asset state transitions and user-admin actions. Keep audit logs append-only and immutable.

## Roadmap & evolution

- Short term: improve CI automation for OpenAPI sync, add a CONTRIBUTING guide, and automate local dev DB setup via Docker Compose.
- Medium term: split long-running background jobs into separate worker processes (if maintenance/reporting load grows) and consider introducing a message queue for heavy async tasks.

## Appendix: useful commands

Build backend (skip tests):

```pwsh
cd backend
./mvnw.cmd -f backend/pom.xml -DskipTests package
```

Run frontend dev server:

```pwsh
cd frontend
npm install
npm run dev
```

Export OpenAPI (one-off):

```pwsh
Invoke-WebRequest -Uri 'http://localhost:8080/v3/api-docs' -OutFile 'docs/openapi/openapi-v1.json'
```

---
Completion: `ARCHITECTURE.md` added with system-architect perspective, design rationale, operational runbook, and developer guidance.
