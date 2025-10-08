# API Error Contract

This document defines the standardized error response format returned by the backend.

## JSON Schema

```
{
  "timestamp": "2025-10-07T20:15:30.123Z",   // ISO-8601 instant
  "path": "/api/assets/123",                 // Request path
  "status": 404,                               // HTTP status code
  "error": "Not Found",                      // Standard HTTP reason phrase
  "code": "NOT_FOUND",                       // Application-specific error code (from ErrorCode enum)
  "message": "Asset not found with id 123",   // Human-readable message
  "details": {                                 // Optional; structure varies by error type
    "fields": {                                // Validation: field -> violation message
      "name": "must not be blank"
    }
  }
}
```

All fields except `details` are always present. `details` is omitted when there is no additional structured information.

## Error Codes
| Code | HTTP | Usage |
|------|------|-------|
| NOT_FOUND | 404 | Requested resource does not exist. |
| VALIDATION_FAILED | 400 | Request body/parameters failed Bean Validation constraints. |
| BUSINESS_RULE_VIOLATION | 409 | Domain invariant or state machine rule violated. |
| DATA_INTEGRITY_VIOLATION | 409 | Persistence layer constraint violation (unique/FK/etc). |
| ACCESS_DENIED | 403 | Authenticated user lacks required authority. |
| AUTHENTICATION_FAILED | 401 | Authentication failed or missing credentials. |
| CONFLICT | 409 | Generic conflict (reserved for future refinement). |
| INTERNAL_ERROR | 500 | Unhandled/unexpected server error. |

## Validation Error Details
When validation fails (`VALIDATION_FAILED`), `details.fields` maps each invalid field name to its first message.

Example:
```
{
  "code": "VALIDATION_FAILED",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": {
    "fields": {
      "serialNumber": "must not be blank",
      "name": "size must be between 3 and 64"
    }
  },
  "path": "/api/assets"
}
```

## Data Integrity Violations
These originate from the database layer (e.g. unique index, foreign key). The message is intentionally generic to avoid leaking internal schema details.

## Business Rule Violations
Thrown via `BusinessRuleException` (or `IllegalStateException` where transitional). Use for domain logic such as invalid asset assignment transitions.

## Best Practices for New Endpoints
1. Throw `ResourceNotFoundException` instead of returning empty 404 bodies.
2. Use Bean Validation annotations on request DTOs to leverage standardized validation handling.
3. For complex multi-field logical validation, perform checks in the service layer and throw `BusinessRuleException` with concise messages.
4. Do not expose raw exception messages for low-level errors—let the global handler wrap them.

## Extending Error Codes
Add new values to `ErrorCode` and handle them in `GlobalExceptionHandler`. Avoid reusing existing codes for semantically distinct cases.

## Versioning Considerations
The contract is additive. New optional properties may appear; existing fields will not be renamed or removed without a major version increment of the API.

---
_Last updated: 2025-10-07_
