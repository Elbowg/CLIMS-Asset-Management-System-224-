
# 📑 Documentation — CLIMS

This folder contains all **project deliverables** and design artifacts.

## 📂 Structure
- **SPMP/** → Software Project Management Plan
- **SRS/** → Software Requirements Specification
- **SDD/** → Software Design Document
- **Diagrams/** → UML, ERD, Sequence, Component diagrams

## � API Documentation

- Swagger UI (local dev): http://localhost:8080/swagger-ui.html
- OpenAPI artifacts (CI): See workflow run artifacts named `openapi-spec` (JSON and YAML). Tagged releases (`v*`) also attach these specs.
- Standardized Error Contract: `Backend/backend/Docs/API_ERRORS.md` — canonical error schema and examples. Swagger responses include examples for common errors (400/401/403/404/409/500).

## �📌 Responsibilities
- Store all official project documentation
- Maintain version history of deliverables
- Ensure consistency between SRS → SDD → Implementation

