# CLIMS Backend

This README documents the backend portion of the CLIMS Asset Management System. It summarizes the architecture, features, implementation details, environment configuration, build/run instructions, tests, and links to API/OpenAPI artifacts. The contents are guided by the project's SRS and SDD and reflect the current repository layout under `backend/`.

## Project overview

- Location: `backend/`
- Tech stack: Java (OpenJDK 11+), Spring Boot, Maven. The project uses the Maven wrapper (`mvnw`) included in the repository.
- Purpose: Provide REST API endpoints for authentication, asset management, maintenance, reporting, users/roles, and supporting domain entities (vendors, locations, departments). The backend secures endpoints with role-based access control, produces OpenAPI v1 documentation under `/v3/api-docs`, and contains unit and integration tests.

## Architecture

- Entry: Spring Boot application (main class under `src/main/java`).
- Layers: Typical layered structure — controllers (REST endpoints), services (business logic), repositories/DAOs (persistence), and domain models.
- Security: Spring Security is used for authentication/authorization. Token-based flows are present (access/refresh) and role checks protect admin endpoints.
- Configuration: Profiles for `dev`, `prod`, etc. Configuration files are in `src/main/resources` and in `target/classes` for the built artifact.
- API contract: OpenAPI JSON is generated and available at `/v3/api-docs` and committed snapshot in `docs/openapi/openapi-v1.json`.

## Features and how they are implemented

This section maps each major SRS feature to the implementation and relevant files.

- Authentication & Authorization
  - Controllers: Auth-related endpoints implemented in authentication controller classes under `src/main/java/.../controller`.
  - Implementation: Uses Spring Security for password handling, JWT or token-based access/refresh flows, and refresh/rotate mechanisms tested in `AuthControllerChangeAndRefreshTests` and `AuthControllerRegisterTests`.

- Asset management (CRUD, search, audit)
  - Controllers: AssetController (REST endpoints for assets).
  - Services: `AssetService` implements business rules such as department-scoping and validations (see `AssetServiceTests` and `AssetServiceDepartmentScopeTests`).
  - Validations: Type validation and field validation tested in `AssetControllerTypeValidationTests` and `AssetControllerValidationTests`.
  - Auditing: Asset audit endpoints and tests (`AssetAuditEndpointTests`); persistence of change history handled at service/repository layers.

- Users and admin management
  - Controllers: UserController, AdminUserManagementController.
  - Implementation: User management functionality supports role assignments, lockouts (`UserLockoutTests`), and admin-specific operations. Role enforcement is tested across several security-focused test classes.

- Maintenance and scheduling
  - Controllers/Services: MaintenanceController and MaintenanceService. Tests in `MaintenanceServiceTests` and controller security in `MaintenanceControllerSecurityTests`.

- Reporting (PDF and metrics)
  - Reporting endpoints exist to produce exports (PDF) and aggregated KPIs. PDF generation endpoints have tests in `ReportControllerPdfTests`.

- Other domain entities (vendors, locations, departments)
  - Each domain entity has controllers, services, and persistence layers. CRUD operations follow standard Spring Boot patterns and reuse validation annotations.

## Data model and persistence

- The project uses JPA/Hibernate (likely) with entities in `src/main/java` under domain packages. Repositories interface with the database via Spring Data.
- DB configuration and profiles are in `src/main/resources/application-*.properties`.

## API documentation and OpenAPI

- The backend exposes OpenAPI docs at `/v3/api-docs` (when running) and an example snapshot is in `docs/openapi/openapi-v1.json`.
- Use this OpenAPI spec to generate client code or keep the `frontend/docs/openapi/openapi-v1.json` in sync.

## Build & run

Prerequisites: Java 11+, Maven (or use bundled `mvnw`).

Build (skip tests):

```pwsh
cd backend
./mvnw.cmd -f backend/pom.xml -DskipTests package
```

Run (development profile):

```pwsh
cd backend
./mvnw.cmd -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

Run tests:

```pwsh
cd backend
./mvnw.cmd -f backend/pom.xml test
```

The VS Code tasks in `.vscode/tasks.json` include convenient tasks for building and running the backend.

## Testing

- The repository contains a comprehensive test suite under `target/surefire-reports` for unit and integration tests. Notable test classes include security-focused integration tests, asset validation tests, and service layer unit tests.
- To run a focused test, use Maven's `-Dtest=` option:

```pwsh
./mvnw.cmd -Dtest=AssetServiceTests test
```

## Configuration & environment

- Example environment variables and sample `.env.example` are present at the project root. Use these to configure database URLs, mail servers (if used), and other environment-specific properties.
- Profiles: `dev`, `prod`. Keep secrets and production configs outside source control.

## Observability and logging

- Logs are produced under `logs/` and Spring Boot logging configuration can be adjusted in `application-*.properties`.

## Security considerations

- Use HTTPS in production. Ensure strong password hashing and rotate secret keys used for token signing.
- Review permission checks for admin-only endpoints. Several tests already verify the expected access controls.

## Files of interest

- `pom.xml` — Maven config and dependencies
- `mvnw`, `mvnw.cmd` — Maven wrapper
- `src/main/java` — Controllers, services, entities, repositories
- `src/main/resources` — Application config files and OpenAPI generation config
- `docs/openapi/openapi-v1.json` — Committed OpenAPI snapshot

## Recommendations and next steps

- Keep the OpenAPI snapshot in `docs/openapi` in sync with the running API. You can export it via the provided VS Code task or a curl/Invoke-WebRequest call to `http://localhost:8080/v3/api-docs`.
- Add a short CONTRIBUTING.md describing how to run the app locally with a dev DB (H2 or Dockerized Postgres) for new developers.
- If migrating or regenerating clients, add automation in `scripts/` to update `frontend/api/schema.ts` and run tests.

---
Completion: Backend README added to describe architecture, features, and developer guidance aligned to SRS/SDD.
