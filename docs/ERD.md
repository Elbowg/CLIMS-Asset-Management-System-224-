# CLIMS ERD (Logical)

Entities and key relations:

- Department (1) ── (0..*) Users
- User (1) ── (0..*) Assets (as assignedUser)
- Location (1) ── (0..*) Assets
- Vendor (1) ── (0..*) Assets
- Asset (1) ── (0..*) Maintenance
- User (1) ── (0..*) Maintenance (reportedBy, optional)
- User (1) ── (0..*) AuditLog (optional)
- User (1) ── (0..*) Report (generatedBy, optional)

Notes and rules:
- Unique constraints: departments.name, users.username, users.email, assets.asset_tag.
- Enums (strings): users.role, assets.status, maintenance.status.
- On delete:
  - departments → users: SET NULL
  - users → assets.assigned_user: SET NULL
  - locations/vendors → assets: SET NULL
  - assets → maintenance: CASCADE
  - users → maintenance.reported_by, audit_logs.user, reports.generated_by: SET NULL
- Indexes for performance:
  - assets: (status), (assigned_user_id), (location_id), (vendor_id)
  - maintenance: (asset_id), (status), (scheduled_date)
  - audit_logs: (entity_name, entity_id), (created_at)
