# Petition 047 Outcome Contract

## Acceptance criteria

### REST API (BusinessConfigController)

1. OpenDebt exposes `GET /api/v1/config` that returns all config entries grouped by `config_key`, each with a computed `status` field (ACTIVE, PENDING_REVIEW, FUTURE, EXPIRED, APPROVED).
2. OpenDebt exposes `GET /api/v1/config/{key}?date={effectiveDate}` that returns the effective config value for the given key and date, or HTTP 404 if none is effective.
3. OpenDebt exposes `GET /api/v1/config/{key}/history` that returns all version entries for a key, ordered by `valid_from` descending.
4. OpenDebt exposes `POST /api/v1/config` that creates a new config version and automatically closes the previous open-ended entry for the same key by setting its `valid_to`.
5. OpenDebt exposes `PUT /api/v1/config/{id}` that updates only future or PENDING_REVIEW entries.
6. OpenDebt exposes `DELETE /api/v1/config/{id}` that deletes only future entries.

### Validation rules

7. `POST /api/v1/config` rejects entries with `valid_from` in the past unless `seedMigration=true` and the caller has ROLE_ADMIN.
8. `POST /api/v1/config` rejects entries with overlapping validity periods for the same `config_key`.
9. `POST /api/v1/config` validates `config_value` is parseable according to the declared `value_type` (DECIMAL, INTEGER, BOOLEAN, STRING).
10. `POST /api/v1/config` rejects entries missing `description` or `legal_basis` with HTTP 400.
11. `PUT` rejects modification of active or expired entries. `DELETE` rejects deletion of active or past entries.

### Status field and transitions

12. `BusinessConfigEntity` has a persisted `review_status` column; the full status (ACTIVE, FUTURE, EXPIRED, PENDING_REVIEW, APPROVED) is computed dynamically from `valid_from`, `valid_to`, and `review_status`.
13. PENDING_REVIEW entries transition to APPROVED on operator approval, or are deleted on rejection.
14. PENDING_REVIEW entries are not considered effective for config resolution — the previously active entry remains in effect.

### Derived rate auto-computation

15. Creating a `RATE_NB_UDLAAN` entry auto-generates three derived entries (RATE_INDR_STD = NB+4%, RATE_INDR_TOLD = NB+2%, RATE_INDR_TOLD_AFD = NB+1%) with `review_status=PENDING_REVIEW` and `created_by=SYSTEM`.
16. The `POST` response for an NB rate creation includes the list of auto-generated derived entries.
17. Auto-generated entries require explicit operator approval before activation.

### Caseworker portal — configuration pages

18. A "Konfiguration" menu item appears in the main navigation for ADMIN and CONFIGURATION_MANAGER roles.
19. CASEWORKER sees a read-only view (no create/edit/delete controls; read-only notice displayed).
20. The list page (`/konfiguration`) displays entries grouped by category (Renter, Gebyrer, Tærskler) with colour-coded status badges (green=active, yellow=future/pending, grey=expired).
21. Clicking a key navigates to a detail page (`/konfiguration/{key}`) showing version history as a timeline with value, dates, `created_by`, legal basis, and status badge.
22. The "Opret ny version" form includes value, date picker (min=tomorrow), description (pre-filled), and legal basis (pre-filled).
23. For `RATE_NB_UDLAAN`, the form displays a derived-rate preview panel where the operator can accept all, modify individually, or cancel.
24. A Danish confirmation dialog ("Er du sikker?") is shown before any create, update, or delete action.
25. PENDING_REVIEW entries show "Godkend" and "Afvis" buttons for ADMIN/CONFIGURATION_MANAGER.

### Portal controller and client

26. `ConfigurationController` delegates all operations to a `ConfigServiceClient` that calls the debt-service REST API.
27. `ConfigServiceClient` uses `@CircuitBreaker` and `@Retry` annotations per ADR-0026.
28. If no session identity is present, the controller redirects to `/demo-login`.
29. HTMX fragment responses are used for partial page updates (approve/reject/delete refresh timeline without full reload).
30. Backend unavailability displays the Danish error message "Konfigurationsservice er midlertidigt utilgængelig".

### Internationalisation

31. All UI text uses Danish `messages_da.properties` keys — no English UI strings appear on any configuration page.
32. Status labels: Aktiv, Fremtidig, Afventer godkendelse, Udløbet, Godkendt.
33. Validation error messages in Danish (past date, overlap, type mismatch, required fields).

### Audit logging

34. Every configuration change (CREATE, UPDATE, APPROVE, REJECT, DELETE) is recorded in a `business_config_audit` table with operator identity, timestamp, action, config key, old/new values, and details.
35. Auto-generated derived entries produce audit records with `performed_by=SYSTEM`.
36. The audit trail is viewable on the detail page as a collapsible section below the version timeline.

### Security and authorisation

37. ADMIN and CONFIGURATION_MANAGER have full CRUD access to config API endpoints.
38. CASEWORKER has read-only access (GET endpoints only; POST/PUT/DELETE return 403).
39. SERVICE role can read config entries and effective values but not history or write endpoints.
40. Portal controllers conditionally render write controls based on the operator's role.
41. `ROLE_CONFIGURATION_MANAGER` is introduced as a new role that grants config management without full ADMIN privileges.
42. In dev/demo mode, at least one demo user has the CONFIGURATION_MANAGER role.

## Definition of done

- All 6 REST API endpoints are functional and return correct HTTP status codes.
- Validation rules reject invalid input with Danish error messages and HTTP 400/403/409.
- Creating a `RATE_NB_UDLAAN` entry auto-generates three derived entries in PENDING_REVIEW status.
- The caseworker portal "Konfiguration" page renders correctly for ADMIN, CONFIGURATION_MANAGER (full access), and CASEWORKER (read-only).
- The version history timeline displays all versions for a key with correct ordering, colour-coded status badges, and audit trail.
- The "Opret ny version" form creates new config entries with confirmation dialog and NB derived-rate preview.
- PENDING_REVIEW entries can be approved or rejected via the portal UI with correct status transitions.
- Every config change is audit-logged in `business_config_audit`.
- `ConfigServiceClient` has circuit breaker and retry annotations; backend failure shows Danish error message.
- All UI text is in Danish via `messages_da.properties`.
- WCAG 2.1 AA compliance for all new UI elements per petition 013.
- Every acceptance criterion is covered by at least one Gherkin scenario.
- Flyway migration `V21__add_config_review_status_and_audit.sql` runs successfully.

## Failure conditions

- A config entry is created with `valid_from` in the past without the `seedMigration` bypass.
- Overlapping validity periods exist for the same `config_key`.
- An active or expired entry is modified or deleted.
- Auto-generated derived rates become effective without operator approval.
- CASEWORKER role can create, update, or delete config entries.
- SERVICE role can access version history or write endpoints.
- Config changes are not audit-logged, or audit records are missing operator identity.
- The portal displays English text on any configuration page.
- The portal allows creation/approval/deletion without a confirmation dialog.
- Backend unavailability causes an unhandled error (no circuit breaker fallback).
- The Flyway migration fails or creates an inconsistent schema.
