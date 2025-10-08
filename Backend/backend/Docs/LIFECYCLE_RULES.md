# Lifecycle Rules

## Asset Status Transitions
```
AVAILABLE  -> ASSIGNED | MAINTENANCE | RETIRED
ASSIGNED   -> AVAILABLE | MAINTENANCE | RETIRED
MAINTENANCE-> AVAILABLE | RETIRED
RETIRED    -> (no transitions)
```
Invalid transitions raise `BusinessRuleException`.

## Maintenance Status Transitions
```
REPORTED    -> IN_PROGRESS | CANCELLED
IN_PROGRESS -> RESOLVED | CANCELLED
RESOLVED    -> (terminal)
CANCELLED   -> (terminal)
```

## Principles
1. All transitions validated centrally.
2. Terminal states (`RETIRED`, `RESOLVED`, `CANCELLED`) cannot transition further.
3. Service methods enforce rules before mutating entities.

## Extension
Add new statuses by updating the lifecycle graph classes and creating tests that cover new transitions (positive + negative paths).

_Last updated: 2025-10-07_
