# OpenAPI Contract (v1)

This folder contains the frozen v1 REST API contract for the CLIMS backend.

`openapi-v1.json` was generated on the date in its file metadata using the live
running application and serves as the baseline for frontend integration and
external client onboarding.

## Export / Regenerate Steps

1. Start the backend (dev profile):
   - VS Code Task: **Backend: Run (dev)**
   - Or PowerShell (from repo root):
     ```powershell
     ./backend/backend/mvnw.cmd -f ./backend/backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
     ```
2. Export JSON spec:
   - VS Code Task: **Export OpenAPI v1** (downloads `/v3/api-docs`)
   - Or manual PowerShell:
     ```powershell
     Invoke-WebRequest -Uri 'http://localhost:8080/v3/api-docs' -OutFile './docs/openapi/openapi-v1.json'
     ```
3. (Optional) Export YAML:
   ```powershell
   Invoke-WebRequest -Uri 'http://localhost:8080/v3/api-docs.yaml' -OutFile './docs/openapi/openapi-v1.yaml'
   ```

## Contract Freeze Policy

Changes that modify response bodies, request schemas, URLs, or status codes are
considered breaking. To evolve the API:

* Additive changes (new endpoints/fields marked nullable or with defaults) are allowed in minor revisions (v1.x).
* Breaking changes require a new major version (e.g. `/api/v2/...`) while keeping v1 available until consumers migrate.
* Update this README with rationale when creating a new major version.

## Postman & Client Sync

After regenerating the spec:
1. Import / update the Postman collection from `openapi-v1.json`.
2. Regenerate typed clients (if used) for frontend or other services.
3. Bump any SDK package version that encapsulates the API.

## Troubleshooting

| Symptom | Likely Cause | Fix |
| ------- | ------------ | ---- |
| 401 on `/v3/api-docs` | Missing explicit matcher for `/v3/api-docs` | Ensure `SecurityConfig` permits `/v3/api-docs` and `/v3/api-docs/**` |
| 500 `NoSuchMethodError ControllerAdviceBean.<init>` | springdoc version incompatible with Spring Framework version | Upgrade `springdoc-openapi-starter-webmvc-ui` to >= 2.7.0 |
| Empty or truncated file | Backend not fully started when export ran | Wait for "Tomcat started" log then retry |
| CORS issues from frontend | Missing origin in `app.cors.allowed-origins` | Add origin (comma separated) in `application-*.properties` or env var |

## Dependency Reference

Current springdoc dependency declared in `pom.xml`:

```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.7.0</version>
</dependency>
```

## Versioning Checklist Before Bumping Major

* Provide migration notes.
* Supply parallel endpoints under new base path.
* Maintain dual-run period & deprecation timeline.
* Update ERD/docs if resource model changes.

---
Generated and maintained as part of the CLIMS backend delivery pipeline.
