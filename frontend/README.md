# CLIMS Frontend

This README documents the frontend portion of the CLIMS (Campus/Corporate/Client Laboratory Information Management System) Asset Management System. It summarizes the architecture, features, implementation details, developer workflows, tests, and links to API/OpenAPI artifacts. The contents were informed by the project's SRS and SDD documents and reflect the current repository layout.

## Project overview

- Location: `frontend/`
- Tech stack: Vite + React (TypeScript), Tailwind CSS, Playwright for e2e, Vitest for unit tests.
- Purpose: Single-page application used by administrators and users to view and manage assets, vendors, locations, maintenance, departments, and user profiles. It consumes the backend REST API documented by OpenAPI (v1).

## Architecture

- Entry point: `index.html` and `src/main.tsx` which bootstraps the React app.
- UI composition: component-driven React structure under `src/` with features grouped by domain in `components/` and `features/`.
- Routing/layout: top-level layout component is `layouts/AppLayout.tsx` used by pages in `features/*`.
- API client: `api/client.ts` centralizes HTTP calls; `api/schema.ts` contains OpenAPI-generated or hand-maintained typings for requests/responses.
- Styling: Tailwind CSS via `tailwind.config.cjs` and `postcss.config.cjs`; global styles in `src/styles.css`.
- Tests: unit tests with Vitest in `src/__tests__`; Playwright e2e tests under `tests/` and `src/__tests__/e2e`.

## Features and how they are implemented

Below is a feature-by-feature mapping describing how each major feature is implemented in the frontend and where to find the relevant code.

- Authentication
  - Pages/components: `features/auth/LoginPage.tsx`, `features/auth/ProfilePage.tsx`, `src/components` for UI pieces.
  - Implementation: Uses `AuthContext` in `features/auth/AuthContext.tsx` to store tokens and user info. Login flows call backend auth endpoints through `api/client.ts`.
  - Notes: Token storage helpers and `tmp-token.txt` artifacts exist for local testing scripts in `scripts/`.

- Assets CRUD and search
  - Pages/components: `features/assets/AssetsPage.tsx`, `AssetFormPage.tsx`, `AssetDetailPage.tsx`.
  - Implementation: Forms use controlled React components, client-side validation (see `feat/add-asset-validation-tests` branch). Calls to backend endpoints go through `api/client.ts` using typed request/response shapes from `api/schema.ts`.
  - Special behaviors: Department scoping, type validation, and audit/history views are driven by backend filters and parameters passed from UI.

- Users and admin management
  - Pages/components: `features/users/UsersPage.tsx`, admin management controllers that surface in UI tables and modals.
  - Implementation: Admin-only UI flows rely on `AuthContext` role checks and call secure backend endpoints.

- Vendors, Locations, Departments
  - Pages/components: `features/vendors/VendorsPage.tsx`, `features/locations/LocationsPage.tsx`, `features/departments/DepartmentsPage.tsx`.
  - Implementation: Simple CRUD pages that reuse shared list and form components. API interactions go through the centralized client.

- Maintenance & reporting
  - Pages/components: `features/maintenance/MaintenancePage.tsx`, report generation links/buttons in dashboards.
  - Implementation: Maintenance scheduling operations call backend endpoints; reporting may request PDF generation endpoints and download responses.

- Dashboard and KPIs
  - Pages/components: `features/dashboard/DashboardPage.tsx` and `src/App.tsx` integration.
  - Implementation: Fetches aggregated metrics from backend API; UI charts or KPI widgets based on returned data.

- Misc
  - NotFound and routing fallbacks: `features/misc/NotFoundPage.tsx`.

## Data contracts and OpenAPI

- The frontend uses typed schemas (OpenAPI-derived) located in `api/schema.ts` and `frontend/docs/openapi/openapi-v1.json` (copied for frontend reference).
- The canonical backend OpenAPI is in `docs/openapi/openapi-v1.json`. Use this file to regenerate client typings if needed.

## Development

Prerequisites:
- Node.js (16+ recommended) and npm/yarn

Common commands (run from `frontend/`):

For development (hot reload):

```pwsh
# install deps
npm install
# run dev server
npm run dev
```

Run unit tests (Vitest):

```pwsh
npm test
```

Run Playwright e2e checks (if configured):

```pwsh
npm run test:e2e
```

Build for production:

```pwsh
npm run build
```

## Testing strategy

- Unit tests: Vitest for components and API logic. See `src/__tests__/` for examples.
- Integration/E2E: Playwright tests in `tests/` and `src/__tests__/e2e` simulate flows such as create-asset and smoke tests. CLI helpers like `scripts/smoke-check.js` exist.

## Security and best practices

- Keep secrets out of the repo. Use environment variables for API base URL and auth secrets.
- Follow the backend's auth token expiry/refresh flow implemented in `features/auth/AuthContext.tsx`.

## Files of interest

- `src/` — All application source code
- `api/client.ts` — REST client used across the app
- `api/schema.ts` — Type definitions for API messages
- `vite.config.ts`, `tsconfig.json` — build and type config
- `docs/openapi/openapi-v1.json` — OpenAPI snapshot for frontend reference

## Notes and next steps

- If you regenerate the OpenAPI client on the frontend, update `api/schema.ts` and ensure tests still pass.
- Consider centralizing feature flags and environment config via `.env` files compatible with Vite.

---
Completion: Frontend README created to reflect the project's current structure and SRS/SDD-informed implementation guidance.
# CLIMS Frontend

Initial scaffold for the Computer and Laptop Information Management System web client.

## Stack Choices

- Build Tool: Vite (fast dev server, native ESM)
- Language: TypeScript
- UI Library: React 18
- Routing: React Router v6
- Styling: Tailwind CSS (utility-first, rapid prototyping)
- State: React Query (server state) + lightweight Context for auth
- Forms: React Hook Form (accessible, performant) – to be added later
- API Client: Generated from `docs/openapi/openapi-v1.json` using `openapi-typescript` for types + a thin fetch wrapper

## Folder Structure (proposed)

```
frontend/
  src/
    api/            # Generated types + API client wrapper
    components/     # Reusable presentational components
    features/       # Feature-based pages and hooks
      auth/
      assets/
      maintenance/
      reports/
      admin/
    layouts/        # Layout shells (AuthLayout, AppLayout)
    routes/         # Route element definitions
    lib/            # Utilities (fetch wrapper, storage, config)
    styles/         # Global styles & Tailwind entry
    App.tsx
    main.tsx
  public/
  index.html
```

## Environment Variables

Create `.env.local` (not committed) for local dev overrides:
```
VITE_API_BASE=http://localhost:8080
```
Defaults fallback to `/` if not provided.

## Commands (to add after package.json)
```
npm install
npm run dev
npm run build
npm run preview
npm run api:gen   # Regenerate API typings from backend OpenAPI JSON
```

## API Typing Generation

We will use `openapi-typescript` to generate a types file:
```
npx openapi-typescript ../docs/openapi/openapi-v1.json -o src/api/schema.ts
```
Then build lightweight functions in `src/api/client.ts` that leverage those types.

## Next Steps
- Add package.json, tsconfig, and Vite config
- Add Tailwind config & PostCSS
- Generate initial API schema types
- Implement auth token storage & interceptor fetch wrapper
- Flesh out feature pages

## Current Temporary Stubs / Expected Dev Warnings

- `src/api/schema.ts` is a temporary stub so TypeScript builds before you install Node and generate real OpenAPI types. After installing Node run `npm run api:gen` to overwrite it.
- Until you run `npm install`, TypeScript in the editor will report missing module errors for React Router, React Query, etc. This is expected because `node_modules` is absent. Resolve by installing Node.js (LTS) and running the install command below.

### After Installing Node.js

```
npm install
npm run api:gen
npm run dev
```

If the backend is running on a non-default port, set `VITE_API_BASE` accordingly in `.env.local`.

---
This scaffold will evolve as the frontend develops; update this README accordingly.

http://localhost:5173/assets/new
