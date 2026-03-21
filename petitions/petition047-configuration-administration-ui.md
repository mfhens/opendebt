# Petition 047: Configuration administration UI for versioned business values

## Summary

OpenDebt shall provide a REST API and caseworker-portal interface that allows authorised operators to view, create, schedule, and audit-trail business configuration values defined by petition 046 (versioned business configuration). Today, the `BusinessConfigEntity`, `BusinessConfigService`, and `BusinessConfigRepository` exist in `opendebt-debt-service`, but there is no controller exposing CRUD operations and no user interface for operators to administer configuration. This petition closes the operational gap: rate changes, fee updates, and threshold adjustments currently require a developer to write SQL or Flyway migrations — after this petition, an ADMIN or CONFIGURATION_MANAGER can manage them through the caseworker portal.

## Context and motivation

Petition 046 introduced the `business_config` table with time-versioned configuration values — interest rates, fee amounts, thresholds, and regulatory parameters — with explicit `valid_from`/`valid_to` periods. The data model, service layer, and batch-processing integration are complete. However, the operational workflow for _maintaining_ these values is missing:

1. **No REST API**: `BusinessConfigController` does not exist. The only way to create or update configuration entries is via Flyway migration scripts or direct SQL — unacceptable in production.
2. **No operator UI**: Caseworkers and administrators have no portal page to inspect current rates, review version history, or schedule future rate changes.
3. **No derived-rate workflow**: Petition 046 FR-8 defined auto-computation of derived rates when the NB udlånsrente changes, but the approval workflow requires a UI with preview and confirmation.
4. **No audit visibility**: Configuration changes are audit-logged at the database level (`created_by`, `created_at`), but operators cannot view the audit trail through the portal.

Danish public debt collection rates change semi-annually (5th banking day after January 1 or July 1). The operator workflow for a rate change is:

1. Nationalbanken publishes a new udlånsrente.
2. Gældsstyrelsen calculates derived inddrivelsesrenter (NB + 4%, NB + 2%, NB + 1%).
3. An ADMIN enters the new NB rate with `valid_from` = effective date.
4. The system auto-generates derived rates as PENDING_REVIEW.
5. A second ADMIN reviews and approves (or modifies) the derived rates.
6. On the effective date, the new rates take effect automatically.

This petition provides the backend API and frontend UI to support this workflow.

## Functional requirements

### FR-1: BusinessConfigController — REST API

1. OpenDebt shall expose the following REST endpoints in `opendebt-debt-service` under the base path `/api/v1/config`:

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/config` | List all config entries, grouped by `config_key` |
| `GET` | `/api/v1/config/{key}?date={effectiveDate}` | Get effective value for a given key and date |
| `GET` | `/api/v1/config/{key}/history` | Get full version history for a key |
| `POST` | `/api/v1/config` | Create a new config version |
| `PUT` | `/api/v1/config/{id}` | Update a pending or future config entry |
| `DELETE` | `/api/v1/config/{id}` | Delete a future (not yet effective) config entry |

2. The `GET /api/v1/config` endpoint shall return entries grouped by `config_key`, each group containing the currently active entry (if any) plus any future/pending entries. The response shall include a `status` field indicating whether each entry is `ACTIVE`, `PENDING_REVIEW`, `FUTURE`, or `EXPIRED`.

3. The `POST /api/v1/config` request body shall include:
    ```json
    {
      "configKey": "RATE_INDR_STD",
      "configValue": "0.0575",
      "valueType": "DECIMAL",
      "validFrom": "2026-01-05",
      "validTo": null,
      "description": "Inddrivelsesrente (NB + 4%)",
      "legalBasis": "Gældsinddrivelsesloven § 5, stk. 1-2"
    }
    ```

4. The `POST` endpoint shall automatically close the previous open-ended entry for the same `config_key` by setting its `valid_to` to the new entry's `valid_from`.

5. All endpoints shall return standard error responses with Danish-language error messages for validation failures.

### FR-2: Validation rules

6. The `POST /api/v1/config` endpoint shall reject entries where `valid_from` is in the past (before `LocalDate.now()`), unless the request includes a `seedMigration=true` flag and the caller has `ROLE_ADMIN`.

7. The endpoint shall reject entries where the validity period overlaps an existing entry for the same `config_key`. Overlap is defined as: existing entry's `valid_from < new entry's valid_to` AND (`existing entry's valid_to IS NULL` OR `existing entry's valid_to > new entry's valid_from`).

8. The endpoint shall validate that `config_value` is parseable according to the declared `value_type`:
    - `DECIMAL`: must parse as `BigDecimal`
    - `INTEGER`: must parse as `Integer`
    - `BOOLEAN`: must be `"true"` or `"false"`
    - `STRING`: any non-empty value

9. The `description` and `legal_basis` fields are mandatory — entries without them shall be rejected with HTTP 400.

10. The `PUT /api/v1/config/{id}` endpoint shall only allow modification of entries whose `valid_from` is in the future or whose `status` is `PENDING_REVIEW`. Entries that are currently active or expired shall not be modifiable.

11. The `DELETE /api/v1/config/{id}` endpoint shall only allow deletion of entries whose `valid_from` is in the future. Active or past entries shall not be deletable.

### FR-3: BusinessConfigEntity status field

12. The `BusinessConfigEntity` shall be extended with a `status` column:

| Status | Description |
|--------|-------------|
| `ACTIVE` | Currently effective (`valid_from <= today` AND (`valid_to IS NULL` OR `valid_to > today`)) |
| `PENDING_REVIEW` | Auto-generated derived rate awaiting operator approval |
| `FUTURE` | Manually created, `valid_from` is in the future |
| `EXPIRED` | `valid_to <= today` |
| `APPROVED` | Reviewed and approved, awaiting `valid_from` date |

13. Status transitions:
    - `PENDING_REVIEW` → `APPROVED` (operator approves) or `PENDING_REVIEW` → deleted (operator rejects)
    - `APPROVED` / `FUTURE` → `ACTIVE` (when `valid_from` arrives)
    - `ACTIVE` → `EXPIRED` (when `valid_to` passes or a newer entry takes effect)

14. The status shall be computed dynamically based on the `valid_from`, `valid_to`, and an explicit `review_status` column. The `review_status` column stores only `PENDING_REVIEW` or `APPROVED`; the `ACTIVE`, `FUTURE`, and `EXPIRED` statuses are derived from dates.

### FR-4: Derived rate auto-computation with approval workflow

15. When a new `RATE_NB_UDLAAN` entry is created via `POST /api/v1/config`, the system shall auto-generate derived rate entries:

| Derived key | Formula | Description |
|-------------|---------|-------------|
| `RATE_INDR_STD` | NB + 0.04 | Inddrivelsesrente (standard) |
| `RATE_INDR_TOLD` | NB + 0.02 | Toldrente (uden afdragsordning) |
| `RATE_INDR_TOLD_AFD` | NB + 0.01 | Toldrente (med afdragsordning) |

16. Auto-generated entries shall:
    - Use the same `valid_from` as the NB rate entry
    - Have `review_status` = `PENDING_REVIEW`
    - Include `description` noting the auto-computation formula
    - Include `created_by` = `"SYSTEM (auto-computed from RATE_NB_UDLAAN)"`

17. The auto-generated entries shall NOT become effective until an operator explicitly approves them via `PUT /api/v1/config/{id}` with `reviewStatus: "APPROVED"`.

18. The `POST /api/v1/config` response for an NB rate creation shall include the list of auto-generated derived entries so the UI can display them immediately.

### FR-5: Configuration management page in caseworker portal

19. The caseworker portal shall include a new menu item **"Konfiguration"** in the main navigation, visible only to users with `ROLE_ADMIN` or `ROLE_CONFIGURATION_MANAGER`.

20. Users with `ROLE_CASEWORKER` shall see a read-only version of the configuration page (view current rates and history, but no create/edit/delete controls).

21. The configuration list page (`/konfiguration`) shall display a table of all config entries, grouped by category:

| Category | Danish label | Config keys |
|----------|-------------|-------------|
| Renter | Renter (Interest) | `RATE_NB_UDLAAN`, `RATE_INDR_STD`, `RATE_INDR_TOLD`, `RATE_INDR_TOLD_AFD` |
| Gebyrer | Gebyrer (Fees) | `FEE_RYKKER`, `FEE_UDLAEG_BASE`, `FEE_UDLAEG_PCT`, `FEE_LOENINDEHOLDELSE` |
| Tærskler | Tærskler (Thresholds) | `THRESHOLD_INTEREST_MIN`, `THRESHOLD_FORAELDELSE_WARN` |

22. Each row in the table shall display:
    - Config key (human-readable Danish label)
    - Current value (formatted according to `value_type`: percentages for rates, currency for fees, days for thresholds)
    - `valid_from` and `valid_to` dates
    - Legal basis (abbreviated, full text on hover)
    - Status indicator:
      - **Green** badge: currently active
      - **Yellow** badge: future/pending review
      - **Grey** badge: expired

### FR-6: Version history view

23. Clicking a config key in the list shall navigate to a detail page (`/konfiguration/{key}`) showing the full version history as a timeline.

24. The timeline shall display all versions ordered by `valid_from` descending (newest first), with:
    - Value, `valid_from`, `valid_to`
    - `created_by` and `created_at`
    - Legal basis
    - Status badge (colour-coded as in FR-5)

25. For rate keys (`value_type = DECIMAL` with keys matching `RATE_*`), the timeline shall include a simple line chart showing rate values over time.

### FR-7: Create new version form

26. The detail page shall include a **"Opret ny version"** (Create new version) button, visible only to ADMIN / CONFIGURATION_MANAGER.

27. The form shall include:
    - `config_value` — input field with validation matching `value_type`
    - `valid_from` — date picker, minimum date = tomorrow
    - `description` — text area (pre-filled with previous entry's description)
    - `legal_basis` — text field (pre-filled with previous entry's legal basis)

28. For the `RATE_NB_UDLAAN` key, submitting the form shall trigger a preview panel showing the auto-computed derived rates (per FR-4). The operator can:
    - Accept all derived rates → creates NB rate + 3 derived entries in `PENDING_REVIEW`
    - Modify individual derived rates before confirming
    - Cancel the operation entirely

29. A confirmation dialog (`Er du sikker?` / Are you sure?) shall be displayed before any creation, update, or deletion.

30. After successful creation, the page shall display a success message and refresh the history timeline.

### FR-8: Approve/reject pending entries

31. Entries with `PENDING_REVIEW` status shall display **"Godkend"** (Approve) and **"Afvis"** (Reject) buttons.

32. Approving a pending entry shall:
    - Set `review_status` to `APPROVED`
    - Close the previous open-ended entry for the same key (set `valid_to`)
    - Log the approving operator's identity and timestamp

33. Rejecting a pending entry shall delete it and log the rejection.

### FR-9: ConfigurationController in caseworker portal

34. A new `ConfigurationController` in the caseworker portal shall handle:

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/konfiguration` | Config list page |
| `GET` | `/konfiguration/{key}` | Key detail + history page |
| `POST` | `/konfiguration` | Create new config version (form submission) |
| `PUT` | `/konfiguration/{id}/approve` | Approve pending entry |
| `DELETE` | `/konfiguration/{id}` | Delete/reject future entry |

35. The controller shall delegate all operations to a `ConfigServiceClient` (following the same pattern as `DebtServiceClient`) that calls the debt-service REST API. The client shall use `@CircuitBreaker` and `@Retry` annotations per ADR-0026.

36. All controller methods shall verify the operator's session identity (same pattern as `TransactionLogController`). If no identity is present, redirect to `/demo-login`.

37. The controller shall use HTMX fragment responses for partial page updates (e.g., refreshing the history timeline after creation) following the existing portal pattern.

### FR-10: Internationalisation — messages_da.properties

38. The following message keys shall be added to `messages_da.properties`:

```properties
# Configuration management
nav.konfiguration=Konfiguration
config.title=Konfiguration – Forretningsværdier
config.subtitle=Administrér renter, gebyrer og tærskler
config.category.renter=Renter
config.category.gebyrer=Gebyrer
config.category.taerskler=Tærskler
config.table.key=Nøgle
config.table.label=Beskrivelse
config.table.value=Aktuel værdi
config.table.validFrom=Gyldig fra
config.table.validTo=Gyldig til
config.table.legalBasis=Hjemmel
config.table.status=Status
config.status.active=Aktiv
config.status.future=Fremtidig
config.status.pending=Afventer godkendelse
config.status.expired=Udløbet
config.status.approved=Godkendt
config.history.title=Historik for {0}
config.history.empty=Ingen historik fundet
config.history.createdBy=Oprettet af
config.history.createdAt=Oprettet den
config.create.title=Opret ny version
config.create.value=Værdi
config.create.validFrom=Gyldig fra
config.create.description=Beskrivelse
config.create.legalBasis=Hjemmel
config.create.submit=Opret
config.create.cancel=Annullér
config.create.success=Ny konfigurationsversion oprettet
config.create.error.past=Gyldig fra-dato kan ikke være i fortiden
config.create.error.overlap=Gyldighedsperioden overlapper en eksisterende post
config.create.error.format=Værdien matcher ikke den forventede type ({0})
config.create.error.required=Alle felter skal udfyldes
config.confirm.title=Bekræft handling
config.confirm.message=Er du sikker på, at du vil fortsætte?
config.confirm.yes=Ja, fortsæt
config.confirm.no=Nej, annullér
config.derived.title=Afledte renter
config.derived.preview=Følgende afledte renter genereres automatisk:
config.derived.accept=Godkend alle
config.derived.modify=Tilpas individuelt
config.approve.button=Godkend
config.approve.success=Konfigurationsværdi godkendt
config.reject.button=Afvis
config.reject.success=Afvist konfigurationsværdi slettet
config.delete.button=Slet
config.delete.success=Fremtidig konfigurationsværdi slettet
config.readonly.notice=Du har læseadgang. Kontakt en administrator for at ændre konfiguration.
config.error.backend=Konfigurationsservice er midlertidigt utilgængelig
config.error.notfound=Konfigurationsnøgle ikke fundet
```

### FR-11: Audit logging

39. Every configuration change (create, update, approve, reject, delete) shall be audit-logged with:
    - Operator identity (name, role)
    - Timestamp
    - Action performed (CREATE, UPDATE, APPROVE, REJECT, DELETE)
    - Config key and affected entry ID
    - Old value and new value (for updates)

40. The audit log shall be stored in a `business_config_audit` table:

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `config_entry_id` | UUID | FK to `business_config.id` |
| `config_key` | VARCHAR(100) | Config key (denormalised for query convenience) |
| `action` | VARCHAR(20) | `CREATE`, `UPDATE`, `APPROVE`, `REJECT`, `DELETE` |
| `old_value` | VARCHAR(500) | Previous value (NULL for CREATE) |
| `new_value` | VARCHAR(500) | New value (NULL for DELETE) |
| `performed_by` | VARCHAR(100) | Operator identity |
| `performed_at` | TIMESTAMP | When the action occurred |
| `details` | TEXT | Additional context (e.g., "Auto-generated from RATE_NB_UDLAAN") |

41. The audit trail for a config key shall be viewable on the detail page (FR-6), as a collapsible section below the version timeline.

### FR-12: Security and authorisation

42. Configuration API endpoints (`/api/v1/config/**`) shall enforce role-based access:

| Endpoint | ADMIN | CONFIGURATION_MANAGER | CASEWORKER | SERVICE |
|----------|-------|-----------------------|------------|---------|
| `GET /api/v1/config` | ✅ | ✅ | ✅ (read-only) | ✅ |
| `GET /api/v1/config/{key}` | ✅ | ✅ | ✅ (read-only) | ✅ |
| `GET /api/v1/config/{key}/history` | ✅ | ✅ | ✅ (read-only) | ❌ |
| `POST /api/v1/config` | ✅ | ✅ | ❌ | ❌ |
| `PUT /api/v1/config/{id}` | ✅ | ✅ | ❌ | ❌ |
| `DELETE /api/v1/config/{id}` | ✅ | ✅ | ❌ | ❌ |

43. The caseworker portal `ConfigurationController` shall check the operator's role from the session and conditionally render or hide create/edit/delete controls accordingly.

44. A new role `ROLE_CONFIGURATION_MANAGER` shall be introduced (in addition to `ROLE_ADMIN`) to allow configuration management without full administrative privileges.

## PSRM reference context

### Semi-annual rate update process
> Inddrivelsesrenten fastsættes halvårligt og svarer til Nationalbankens officielle udlånsrente pr. 1. januar og 1. juli, tillagt 4 procentpoint. Ændringer offentliggøres af Skatteministeriet og træder i kraft den 5. bankdag efter halvårsskiftet.
_Source: Gældsinddrivelsesloven § 5, stk. 1-2; Renteloven § 5_

### Operational requirement
> Gældsstyrelsen har ansvar for at sikre, at inddrivelsesrentesatser, gebyrer og tærskler er korrekt konfigureret og opdateret i henhold til gældende lovgivning. Ændringer skal dokumenteres med hjemmel og være sporbare i et revisionslog.
_Source: Gældsstyrelsens forretningsgange; Forvaltningsloven § 22 (begrundelsespligt)_

### Four-eyes principle for rate changes
> Ændringer af rentesatser med væsentlig økonomisk konsekvens bør følge et fire-øjne-princip, hvor én medarbejder opretter ændringen og en anden godkender den, inden den træder i kraft.
_Source: Gældsstyrelsens it-sikkerhedspolitik; Rigsrevisionens anbefalinger for interne kontroller_

## Constraints and assumptions

- This petition builds exclusively on top of petition 046's `business_config` table and `BusinessConfigService`. No schema changes beyond adding `review_status` and the `business_config_audit` table.
- The `status` field (ACTIVE, FUTURE, EXPIRED) is derived at query time from `valid_from`/`valid_to` relative to the current date. Only `review_status` (PENDING_REVIEW, APPROVED) is persisted.
- The caseworker portal in dev/demo mode uses session-based identity (no OAuth). The `ConfigurationController` follows the same pattern as existing controllers (`DemoLoginController` session check).
- The derived-rate auto-computation is a convenience feature. Operators can always override auto-computed values or enter rates manually.
- The `ROLE_CONFIGURATION_MANAGER` role is additive — it grants config management without requiring full `ROLE_ADMIN` privileges. In dev/demo mode, one demo user shall have this role.
- HTMX is used for partial page updates (approve/reject/delete actions update fragments without full page reload).
- All UI text is in Danish, using `messages_da.properties` for i18n. No English UI strings.
- WCAG 2.1 AA compliance is required per petition 013.

## Existing system building blocks

| Component | Status | Change needed |
|-----------|--------|---------------|
| `BusinessConfigEntity` | Done (P046) | Add `review_status` column |
| `BusinessConfigService` | Done (P046) | Add create/update/delete/approve methods |
| `BusinessConfigRepository` | Done (P046) | Add queries for listing, overlap detection, status filtering |
| `BusinessConfigController` | **New** | REST API for config CRUD |
| `BusinessConfigAuditEntity` | **New** | JPA entity for audit log |
| `BusinessConfigAuditRepository` | **New** | Repository for audit queries |
| `ConfigServiceClient` (portal) | **New** | WebClient-based BFF client for config API |
| `ConfigurationController` (portal) | **New** | Thymeleaf controller for config pages |
| `config/list.html` (portal) | **New** | Config table view template |
| `config/detail.html` (portal) | **New** | Key history + create form template |
| `messages_da.properties` (portal) | Extend | Add config-related message keys |
| `DebtServiceClient` (portal) | Done (P041) | No change — separate client for config |
| Flyway migration | **New** | `V21__add_config_review_status_and_audit.sql` |
| Demo data seeder (portal) | Extend | Add CONFIGURATION_MANAGER demo user |

## Dependencies

- **Petition 046** (versioned business configuration) — provides `business_config` table, `BusinessConfigEntity`, `BusinessConfigService`, and `BusinessConfigRepository` that this petition builds upon.
- **Petition 041** (sagsbehandlerportal demo access) — provides the caseworker portal infrastructure, `DemoLoginController`, session management, and SKAT standardlayout that this petition extends.
- **ADR-0026** (resilience) — the `ConfigServiceClient` shall use `@CircuitBreaker` and `@Retry` annotations for fault tolerance on debt-service API calls.
- **Petition 013** (WCAG accessibility) — all new UI elements must meet WCAG 2.1 AA compliance.

## Out of scope

- Real-time NB rate API feed from Nationalbanken (rates are entered manually by operators).
- Per-fordringshaver configuration overrides (handled by `InterestSelectionEmbeddable.additional_interest_rate`).
- Feature flags or application-level toggles (different concern from business configuration).
- Non-debt-service configuration values (portal UI settings, integration gateway timeouts).
- Bulk import/export of configuration values (CSV/Excel upload).
- Configuration approval notifications (email, Teams) — the approval workflow is purely within the portal.
- Multi-language UI support beyond Danish (English portal text is not required).
