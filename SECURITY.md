# Security & Authentication Overview

This document describes the current security model of the CLIMS Asset Management backend: authentication, authorization, user/role seeding, JWT design, Flyway migrations that shape the model, and recommended next steps.

## 1. Authentication Flow
- Stateless JWT-based authentication using Spring Security (Spring Boot 3.5.x).
- Endpoint: `POST /api/auth/login` accepts `{ "username", "password" }`.
- On success returns JSON containing:
  - `accessToken` (short-lived, contains roles claim)
  - `refreshToken` (long-lived, no roles claim, typ=REFRESH)
  - `tokenType` (Bearer)
  - `expiresIn` (seconds; access token TTL)
  - `refreshExpiresIn` (seconds)
- Refresh Endpoint: `POST /api/auth/refresh` accepts `{ "refreshToken" }`.
  - Validates token type (must have `typ=REFRESH`).
  - Issues a new pair (access, refresh) if valid.

## 2. JWT Structure
| Claim | Access Token | Refresh Token | Notes |
|-------|--------------|---------------|-------|
| `sub` | Yes | Yes | Username (unique) |
| `iat` | Yes | Yes | Issued-at epoch seconds |
| `exp` | Yes | Yes | Expiration epoch seconds |
| `typ` | `ACCESS` | `REFRESH` | Used to enforce token purpose |
| `roles` | Array<String> | (omitted) | Present only in access token |

Algorithm: HS256 (shared secret configured in application properties).  
Role claim drives `GrantedAuthority` reconstruction server-side for access tokens; refresh tokens never contain roles (minimizes surface & prevents role confusion).

## 3. Authorization Rules
- Method & endpoint protection enforced via Spring Security filter chain + method annotations (@PreAuthorize where used).
- Unauthorized vs Access Denied:
  - Missing / invalid / expired token returns HTTP 401 with JSON body via `RestAuthenticationEntryPoint`.
  - Authenticated principal lacking authority receives HTTP 403 via `RestAccessDeniedHandler`.
  - This replaced earlier 403-on-expired behavior; tests updated accordingly.

## 4. User & Role Data Model
Tables (simplified):
- `user_account` (includes legacy `role` column; JOIN model is authoritative going forward)
- `role` (`id`, `name`)
- `user_roles` (join table `user_id` + `role_id`)

The legacy `user_account.role` remains to avoid a destructive migration mid-stream; future migration can drop it once all code paths rely solely on `user_roles`.

## 5. Seeded Users & Deterministic IDs
Two baseline users are inserted with explicit primary keys for portability across MySQL & H2:
| Username | Raw Password | user_id | Roles | Introduced / Updated In |
|----------|--------------|---------|-------|--------------------------|
| admin | admin | 1 | ROLE_ADMIN | V2 (seed), password corrected V11 |
| user  | user  | 2 | ROLE_USER  | V12 (seed), password normalized V14 |

Deterministic assignment prevents auto-increment divergence and removes the need for sequence bump hacks.

## 6. Relevant Flyway Migrations (Chronological Highlights)
| Version | Purpose | Key Notes |
|---------|---------|-----------|
| V0 | Core tables | Created `user_account` with legacy `role` column |
| V2 | Seed admin | Explicit user_id=1; initial hash later superseded |
| V4 | Roles & join | Adds `role`, `user_roles`; seeds ROLE_USER & ROLE_ADMIN |
| V10 | Backfill mapping | Ensures admin has ROLE_ADMIN in `user_roles` |
| V11 | Admin password fix | Sets admin password hash matching raw `admin` |
| V12 | Seed standard user | Inserts user_id=2 + join mapping for ROLE_USER |
| V13 | Sequence bump (removed) | Converted to NO-OP; replaced with comment + `SELECT 1` |
| V14 | Standard user hash fix | Aligns user password with raw `user` |

## 7. Password Hashing
- BCrypt (strength 10) via `BCryptPasswordEncoder` bean.
- Hashes embedded in migrations for deterministic reproducibility.
- To rotate, generate with same cost and add a new migration updating the row (avoid editing historical files already applied in production).

## 8. Test Strategy
Integration & negative tests cover:
- Successful login (admin & user).
- User forbidden from creating admin-only resources (403).
- Refresh endpoint rejects access token (401/Unauthorized).
- Expired access token now produces 401 (after entry point refinement).

Short-lifetime access token in tests enforced via property override: `jwt.access-expiration=1000` (1 second) for expiry scenario.

## 9. Known Technical Debt / Future Improvements
| Item | Description | Suggested / Status |
|------|-------------|--------------------|
| Legacy `user_account.role` | Redundant with `user_roles` join | Dropped in V15 |
| V13 noop | Historical artifact | Leave as-is; only remove if performing a baseline reset |
| Expired token status | Now unified 401 for unauthenticated cases | Implemented via custom entry point (2025-10-08) |
| Hard-coded hashes in SQL | Less flexible for rotation | Consider moving to Java initializer (profile-gated) for future rotation |
| Roles claim absence in refresh | By design | No change planned |

## 10. Operational Notes
- Flyway history integrity preserved (no rewrites) — production-friendly.
- Explicit numeric user IDs simplify reproducible test database creation.
- Idempotent inserts use `WHERE NOT EXISTS` to avoid duplicate creation on reruns (dev/test).

## 11. How to Add a New Role
1. Create a new Flyway migration (e.g., V15__add_role_X.sql) inserting into `role` if not exists.  
2. Optionally backfill user_roles mappings.  
3. Update any @PreAuthorize annotations or authority checks.  
4. Add tests validating both access (allowed) and denial (other roles).

## 12. Expired Token Behavior (Implemented)
- Custom `RestAuthenticationEntryPoint` maps missing/malformed/expired tokens to 401.
- `RestAccessDeniedHandler` preserves 403 for authenticated-but-forbidden.
- Tests assert 401 for expiry, 403 for authorization failures.

## 13. Refresh Token Misuse Handling
- Access token used at `/api/auth/refresh` → 401 due to `typ` mismatch check.
- Consider rate limiting refresh attempts per user (future enhancement).

## 14. Recommended Next Migration Set
| Version | Purpose | Outline |
|---------|---------|---------|
| V16 | Add unique index (optional) | Enforce uniqueness on `username` if not already (depends on schema) |
| V17 | Add audit columns | e.g., `last_login_at`, `password_changed_at` |

## 15. Quick Reference
| Item | Value |
|------|-------|
| Access token typ | ACCESS |
| Refresh token typ | REFRESH |
| Access roles claim | Present (array) |
| Refresh roles claim | Omitted |
| Seed admin creds | admin / admin |
| Seed user creds | user / user |
| Current migration head | V15 |

---
_Last updated: 2025-10-08 (post 401/403 refinement & V15 column drop)_
