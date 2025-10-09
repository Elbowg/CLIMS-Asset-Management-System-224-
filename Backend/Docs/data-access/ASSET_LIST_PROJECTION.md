# Asset List Projection Evaluation

This document explains the motivation, implementation details, validation method, and expected efficiency impact of using a JPA interface projection for the assets listing endpoint (`GET /api/assets`).

## 1. Motivation
Previously the endpoint returned fully materialized `Asset` entities. This caused:
- Unnecessary column selection (location_id, vendor_id, purchase_date, serial_number, version, etc.)
- Higher heap allocation (entity state + enhancement proxies)
- Larger JSON payloads (even if some fields were later hidden at the controller layer)

The majority of list views (grid/table) need only: `id`, `name`, `status`, `assignedUserId`.

## 2. Implementation Summary
- Interface projection `AssetListProjection` declared with getters for the four required fields.
- Repository method (simplified signature):
  ```java
  @Query("SELECT a.id AS id, a.name AS name, a.status AS status, a.assignedUser.id AS assignedUserId " +
         "FROM Asset a WHERE (:status IS NULL OR a.status = :status) " +
         "AND (:assignedUserId IS NULL OR a.assignedUser.id = :assignedUserId)")
  Page<AssetListProjection> findFiltered(@Param("status") AssetStatus status,
                                         @Param("assignedUserId") Long assignedUserId,
                                         Pageable pageable);
  ```
- Controller enforces a page size cap (100) and applies a whitelisted sort (id|name|status|assignedUserId). Unsupported sorts fall back to `id,asc`.

## 3. Verifying Column Reduction
### Option A: Enable SQL Logging (dev / test profile)
Set in `application-local.properties` or via env:
```
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE
```
Observe an example query (projection path):
```
select a1_0.asset_id,a1_0.name,a1_0.status,a1_0.assigned_user_id from asset a1_0 where (? is null or a1_0.status=?) and (? is null or a1_0.assigned_user_id=?) order by a1_0.asset_id asc limit ?
```
Only 4 columns are fetched.

Compare to (hypothetical) full entity selection (if we used `findAll(Pageable)`):
```
select a1_0.asset_id,a1_0.assigned_user_id,a1_0.location_id,a1_0.name,a1_0.purchase_date,a1_0.serial_number,a1_0.status,a1_0.type,a1_0.vendor_id,a1_0.version from asset a1_0 ...
```
### Option B: Hibernate Statistics
Enable statistics:
```
spring.jpa.properties.hibernate.generate_statistics=true
logging.level.org.hibernate.stat=DEBUG
```
Look for reduced entity load counts (fewer materializations) when using projection vs full entity listing.

### Option C: Integration Test Snippet (Manual)
Add a temporary assertion test to log the SQL or to count returned field names in the `ResultSetMetaData` via a datasource proxy (see future N+1 guard). Remove after manual verification.

## 4. Estimated Savings
| Dimension | Full Entity (approx) | Projection | Notes |
|-----------|----------------------|------------|-------|
| Selected columns | 10 | 4 | 60% reduction |
| Bytes over wire (ResultSet) | ~X (depends on data) | ~40% of full | Proportional to column count & data types |
| JSON serialization fields | (if all exposed) 10 | 4 | Less CPU + bandwidth |
| Heap allocations | Entity + proxies | Lightweight proxy | Each entity may include additional state & lazy handlers |

(Replace X with measured values if a profiling pass is added later.)

## 5. Behavioral Semantics
- Read-only: Projection rows are not managed entities; mutating them has no persistence effect.
- Sorting: Performed at DB level on whitelisted properties; custom mapping handles nested `assignedUserId`.
- Null Filters: Passing an invalid `status` or omitting parameters treats them as `null` => predicate neutral.

## 6. Edge Cases & Considerations
| Case | Handling |
|------|----------|
| Invalid enum value (e.g. `status=BOGUS`) | Ignored (treated as no status filter) |
| Large page size request (e.g. `size=5000`) | Clamped to 100 |
| Sort on non-whitelisted field | Fallback to `id,asc` |
| `assignedUserId` references non-existent user | Returns empty page (no join fetch performed) |
| Future need for additional list column | Add to projection & query; backward compatible if additive |

## 7. Future Enhancements
- Keyset pagination (seek method) for stable performance at large offsets.
- Second-level or Caffeine cache layering (pending caching task) for repeated small pages.
- DataLoader-style batching if future GraphQL layer is introduced.
- Automatic explain-plan sampling in CI for regression detection.

## 8. Migration Notes
Existing clients must already adapt to `Page` wrapper (see backend README). No additional changes required—projection is transparent at JSON level.

## 9. Validation Checklist (Operator Runbook)
1. Enable SQL DEBUG.
2. Hit `/api/assets?page=0&size=5`.
3. Confirm only the 4 columns appear in SELECT.
4. (Optional) Temporarily switch repository call to entity-returning method and observe expanded column list.
5. Revert any temporary code changes.

## 10. Summary
Interface projections deliver immediate efficiency wins (reduced columns, memory, and serialization cost) while keeping API shape stable. This sets the foundation for subsequent performance work (N+1 detection, caching, keyset pagination).
