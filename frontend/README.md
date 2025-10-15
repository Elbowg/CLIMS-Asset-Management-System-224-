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
