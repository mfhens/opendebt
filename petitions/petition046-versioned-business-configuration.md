# Petition 046: Versioned business configuration with validity periods

## Summary

OpenDebt shall support time-versioned business configuration values — such as interest rates, fee amounts, thresholds, and regulatory parameters — with explicit validity periods (`valid_from`, `valid_to`). This replaces the current approach of hardcoding rates in `application.yml` properties, which cannot represent semi-annual rate changes, historical rate lookups, or audit trails for regulatory values.

## Context and motivation

Danish public debt collection interest rates change semi-annually. The inddrivelsesrente is recalculated every time Nationalbanken changes its official udlånsrente, taking effect on the **5th banking day after January 1 or July 1** each year. Between 2023 and 2026 alone, the rate has changed five times:

| Effective date | Rate | NB udlånsrente | Spread |
|---------------|------|----------------|--------|
| 2023-01-01 | 7.25% | -0.75% | +8.0% |
| 2023-07-01 | 5.90% | 1.90% | +4.0% (reduced by Gældsaftalen 2022) |
| 2024-01-08 | 7.75% | 3.75% | +4.0% |
| 2025-01-06 | 6.30% | 2.30% | +4.0% |
| 2025-07-07 | 5.75% | 1.75% | +4.0% |

The current OpenDebt implementation uses:
```yaml
opendebt:
  interest:
    annual-rate: 0.0575
```

This single property value has three critical limitations:

1. **No historical accuracy**: When interest is calculated for a past period (e.g., crossing transactions replaying from March to December), the system applies today's rate to all days, even if the rate was different in March.
2. **No scheduled rate changes**: An operator must manually update `application.yml` and restart/redeploy the service on each rate change date.
3. **No audit trail**: There is no record of when a rate changed, who changed it, or what the previous value was.

These limitations also apply to other configurable business values:
- Fee amounts (rykkergebyr, udlægsafgift, lønindeholdelsesgebyr)
- Thresholds (e.g., minimum amount for interest, forældelsesfrist warning days)
- Regulatory parameters (e.g., NB diskontosats, kassekreditrente for opkrævning)
- Told-specific rates (NB+2% / NB+1%)

## Functional requirements

### FR-1: Business configuration table

1. OpenDebt shall maintain a `business_config` table in the debt-service database:

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `config_key` | VARCHAR(100) | Unique key within validity period (e.g., `RATE_INDR_STD`, `FEE_RYKKER`) |
| `config_value` | VARCHAR(500) | The value (stored as string, parsed by consumers) |
| `value_type` | VARCHAR(20) | `DECIMAL`, `INTEGER`, `STRING`, `BOOLEAN` — for type-safe parsing |
| `valid_from` | DATE | Start of validity period (inclusive) |
| `valid_to` | DATE | End of validity period (exclusive), NULL means open-ended |
| `description` | TEXT | Human-readable description including legal basis |
| `legal_basis` | VARCHAR(500) | Law reference (e.g., "Gældsinddrivelsesloven § 5, stk. 1-2") |
| `created_at` | TIMESTAMP | When the row was inserted |
| `created_by` | VARCHAR(100) | Who created the entry |
| `version` | BIGINT | Optimistic lock |

2. The combination of (`config_key`, `valid_from`) shall be unique. This prevents overlapping validity periods for the same key.
3. When a new rate takes effect, a new row is inserted with the new `valid_from` and the previous row's `valid_to` is set to the new row's `valid_from`.

### FR-2: Configuration resolution by date

4. OpenDebt shall provide a `BusinessConfigService` that resolves configuration values for a given date:
    ```java
    BigDecimal getDecimalValue(String configKey, LocalDate effectiveDate);
    Optional<BusinessConfigEntry> getEntry(String configKey, LocalDate effectiveDate);
    List<BusinessConfigEntry> getHistory(String configKey);
    ```
5. Resolution logic: find the row where `config_key = ?` AND `valid_from <= effectiveDate` AND (`valid_to IS NULL` OR `valid_to > effectiveDate`).
6. If no matching row exists, the service shall throw a `ConfigurationNotFoundException` with a descriptive message including the key and date.

### FR-3: Caching with TTL

7. Configuration values shall be cached in-memory with a configurable TTL (default: 5 minutes) to avoid per-debt database lookups during batch processing.
8. The cache shall be keyed by (`config_key`, `effectiveDate`) and shall be invalidated when TTL expires.
9. For batch processing (InterestAccrualJob), a bulk pre-load method shall be available:
    ```java
    Map<String, BigDecimal> preloadRatesForDate(LocalDate effectiveDate, List<String> configKeys);
    ```

### FR-4: Seed data for initial configuration

10. The following configuration keys shall be seeded with initial values:

| config_key | value | valid_from | valid_to | description |
|-----------|-------|------------|----------|-------------|
| `RATE_NB_UDLAAN` | 0.0175 | 2025-07-07 | NULL | Nationalbankens officielle udlånsrente |
| `RATE_INDR_STD` | 0.0575 | 2025-07-07 | NULL | Inddrivelsesrente (NB + 4%) |
| `RATE_INDR_TOLD` | 0.0375 | 2025-07-07 | NULL | Toldrente (NB + 2%) |
| `RATE_INDR_TOLD_AFD` | 0.0275 | 2025-07-07 | NULL | Toldrente med afdragsordning (NB + 1%) |
| `FEE_RYKKER` | 65.00 | 2024-01-01 | NULL | Rykkergebyr per erindringsskrivelse |
| `FEE_UDLAEG_BASE` | 300.00 | 2024-01-01 | NULL | Udlægsafgift basisbeløb |
| `FEE_UDLAEG_PCT` | 0.005 | 2024-01-01 | NULL | Udlægsafgift procent over 3000 kr |
| `FEE_LOENINDEHOLDELSE` | 100.00 | 2024-01-01 | NULL | Lønindeholdelsesgebyr |
| `THRESHOLD_INTEREST_MIN` | 100.00 | 2024-01-01 | NULL | Minimum beløb for renteberegning |
| `THRESHOLD_FORAELDELSE_WARN` | 90 | 2024-01-01 | NULL | Forældelsesfrist warning days |

11. Historical rate entries shall also be seeded for correct retroactive calculations:

| config_key | value | valid_from | valid_to |
|-----------|-------|------------|----------|
| `RATE_NB_UDLAAN` | 0.0375 | 2024-01-08 | 2025-01-06 |
| `RATE_NB_UDLAAN` | 0.0230 | 2025-01-06 | 2025-07-07 |
| `RATE_INDR_STD` | 0.0775 | 2024-01-08 | 2025-01-06 |
| `RATE_INDR_STD` | 0.0630 | 2025-01-06 | 2025-07-07 |

### FR-5: Integration with InterestAccrualJob

12. The InterestAccrualJob shall use `BusinessConfigService.getDecimalValue("RATE_INDR_STD", accrualDate)` instead of reading `opendebt.interest.annual-rate` from `application.yml`.
13. When processing debts with different interest rules (petition 045), the job shall resolve the appropriate `config_key` based on the debt's `interest_rule`:

| interest_rule | config_key lookup |
|---------------|-------------------|
| `INDR_STD` | `RATE_INDR_STD` |
| `INDR_TOLD` | `RATE_INDR_TOLD` |
| `INDR_TOLD_AFD` | `RATE_INDR_TOLD_AFD` |
| `INDR_CONTRACT` | Use `additional_interest_rate` from InterestSelectionEmbeddable (no config lookup) |
| `INDR_EXEMPT` | Skip (no interest) |

### FR-6: Integration with crossing transaction replay (petition 039)

14. When petition 039's timeline replay recalculates interest for a past date range, it shall resolve the rate that was effective on each accrual date.
15. If a rate changed mid-period (e.g., rate changed on July 7 during a replay from June 1 to August 31), the replay shall split the calculation at the rate boundary:
    - June 1 – July 6: old rate
    - July 7 – August 31: new rate
16. InterestJournalEntry already records the `rate` used per day — this ensures auditability even when rates change.

### FR-7: REST API for configuration management

17. OpenDebt shall expose a REST endpoint for configuration CRUD:
    ```
    GET  /api/v1/config/{key}?date={effectiveDate}  → current value
    GET  /api/v1/config/{key}/history                → all versions
    POST /api/v1/config                              → create new version
    ```
18. The POST endpoint shall validate:
    - `valid_from` is not in the past (except for seed data migration)
    - No overlapping validity period exists for the same key
    - `value_type` matches the provided `config_value` format
19. The endpoint shall require `ROLE_ADMIN` or `ROLE_CONFIGURATION_MANAGER` authorization.
20. All changes shall be audit-logged (who, when, what changed, old value, new value).

### FR-8: Derived rate auto-computation

21. When the NB udlånsrente (`RATE_NB_UDLAAN`) is updated, the system shall optionally auto-compute derived rates:
    - `RATE_INDR_STD` = NB + 0.04
    - `RATE_INDR_TOLD` = NB + 0.02
    - `RATE_INDR_TOLD_AFD` = NB + 0.01
22. Auto-computation shall create new config entries as `PENDING_REVIEW` status, requiring manual approval before activation.
23. This is a convenience feature — operators can also manually enter derived rates.

## PSRM reference context

### Semi-annual rate updates
> Inddrivelsesrenten fastsættes halvårligt og svarer til Nationalbankens officielle udlånsrente pr. 1. januar og 1. juli, tillagt 4 procentpoint. Nationalbankens renteændringer slår igennem på inddrivelsesrenten med virkning fra 5. bankdag efter 1/1 eller 1/7.
_Source: Gældsinddrivelsesloven § 5, stk. 1-2; Renteloven § 5_

### Told rate
> Forsinket told opkræves med simpel morarente efter EUTK art. 114: NB's udlånsrente + 2%-point (uden afdragsordning) eller + 1%-point (med afdragsordning).
_Source: EU-toldkodeks art. 114; Toldloven § 30a_

### Rate change history (2023–2026)
> 2023 H1: 7.25%, 2023 H2: 5.90% (post Gældsaftalen), 2024 H1: 7.75%, 2025 H1: 6.30%, 2025 H2: 5.75%.
_Source: Gældsstyrelsens hjemmeside, Skatteministeriets udmeldinger_

## Constraints and assumptions

- The `business_config` table resides in the debt-service database. Other services that need configuration values (e.g., payment-service for dækningsrækkefølge) access them via the debt-service API or maintain their own cached copy.
- This design uses a simple key-value table with validity periods rather than a full temporal database pattern, balancing complexity with the actual number of configurable values (~20-30).
- The `valid_from` / `valid_to` pattern is sufficient because rate changes are infrequent (2-4 per year) and always known in advance.
- The `application.yml` property `opendebt.interest.annual-rate` shall be retained as a fallback during the migration period but shall be deprecated once petition 045 and 046 are fully implemented.
- Rate auto-computation is a convenience feature and NOT a hard requirement — in production, rates would be verified against Skatteministeriet's official announcements before activation.
- The configuration API is internal (not exposed to fordringshavere or borgere).

## Existing system building blocks

| Component | Status | Change needed |
|-----------|--------|---------------|
| `application.yml` rate property | Done | Deprecate; retain as fallback |
| `InterestAccrualJobHelper` | Done | Replace hardcoded rate lookup with `BusinessConfigService` |
| `InterestJournalEntry.rate` | Done | Already stores rate per entry — no change needed |
| Petition 039 timeline replay | In backlog | Must use date-resolved rates |
| Petition 045 multi-regime | In backlog | Depends on this petition for rate storage |
| `BusinessConfigEntity` | **New** | JPA entity for `business_config` table |
| `BusinessConfigService` | **New** | Service for date-resolved config lookup |
| `BusinessConfigRepository` | **New** | Spring Data JPA repository |
| `BusinessConfigController` | **New** | REST API for config management |
| Flyway migration | **New** | `V20__create_business_config_table.sql` with seed data |

## Dependencies

- **Petition 045** (multi-regime interest) — consumes the configuration values stored by this petition.
- **Petition 043** (batch processing) — the InterestAccrualJob that will be refactored to use `BusinessConfigService`.
- **Petition 039** (crossing transactions) — timeline replay needs historical rate lookups.

## Out of scope

- Feature flags or application-level toggles (different concern from business configuration).
- Multi-tenant configuration (all values are system-wide; per-fordringshaver overrides are handled by InterestSelectionEmbeddable's `additional_interest_rate`).
- Real-time NB rate feeds or automatic fetching from Nationalbanken's API (manual entry by operator).
- Configuration for non-debt-service properties (e.g., portal UI settings, integration gateway timeouts).
- Full temporal database / bi-temporal configuration pattern (over-engineered for the ~20 values we need to version).
