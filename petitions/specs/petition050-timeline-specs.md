# Petition 050 — Implementation Specifications

| Field | Value |
|---|---|
| Petition | petition050-unified-case-timeline-ui |
| Architecture ref | `docs/petition050-timeline-architecture.md` (SA-050) |
| Outcome contract | `petitions/petition050-unified-case-timeline-ui-outcome-contract.md` |
| Status | Draft |
| Modules in scope | opendebt-common · opendebt-caseworker-portal · opendebt-citizen-portal · opendebt-creditor-portal · opendebt-payment-service |

Design decisions are resolved in SA-050. This document specifies **what to build** at module and class level with enough detail to write and test code. It does not re-litigate architecture choices.

---

## 1. Prerequisites

These two tasks block everything else. Complete and release/merge them before implementing any portal changes.

> **OI-3 (TRACK):** Complete the `EventCategoryMapper` normalisation table from actual event type values produced by case-service and payment-service. The 16-entry starter set in §2.5 is provisional. `EventCategoryMapperTest` coverage must be expanded post-OI-3 discovery. OI-3 gates `EventCategoryMapper` finality.

### 1.1 OI-1 — Add `SERVICE` role to payment-service timeline endpoint

**File:** `opendebt-payment-service/src/main/java/dk/ufst/opendebt/payment/controller/LedgerController.java`

**Change:** Extend the `@PreAuthorize` expression on `GET /api/v1/events/case/{caseId}`.

| Current | New |
|---|---|
| `"hasRole('CASEWORKER') or hasRole('ADMIN')"` | `"hasRole('CASEWORKER') or hasRole('ADMIN') or hasRole('SERVICE')"` |

**Rationale:** Citizen and creditor portal BFFs call this endpoint using a service-account token. Without `SERVICE` role, the circuit breaker fires immediately, producing a permanently degraded timeline for those portals. See SA-050 §10, OI-1.

**Acceptance:** `@PreAuthorize` on `getEventsByCase` includes `hasRole('SERVICE')`. Existing `CASEWORKER`/`ADMIN` paths are unaffected.

---

### 1.2 OI-2 — Promote `DebtEventDto` to `opendebt-common`

**Current location:** `opendebt-payment-service/src/main/java/dk/ufst/opendebt/payment/dto/DebtEventDto.java`

**Target package:** `dk.ufst.opendebt.common.dto`

**Steps:**
1. Copy `DebtEventDto` to `opendebt-common/src/main/java/dk/ufst/opendebt/common/dto/DebtEventDto.java` — preserve all fields exactly (see table below) and retain `@Data @Builder @NoArgsConstructor @AllArgsConstructor` Lombok annotations.
2. In `opendebt-payment-service`: replace the local class with an import from `opendebt-common`. Remove the now-redundant local file.
3. Verify `opendebt-payment-service` already declares `opendebt-common` as a Maven dependency; if not, add it.

**Fields to preserve:**

| Field | Type | Nullable |
|---|---|---|
| `id` | `UUID` | No |
| `debtId` | `UUID` | No |
| `eventType` | `String` | No |
| `effectiveDate` | `LocalDate` | Yes |
| `amount` | `BigDecimal` | Yes |
| `correctsEventId` | `UUID` | Yes |
| `reference` | `String` | Yes |
| `description` | `String` | Yes |
| `ledgerTransactionId` | `UUID` | Yes |
| `createdAt` | `LocalDateTime` | No |

**Acceptance:** `opendebt-common` exports `dk.ufst.opendebt.common.dto.DebtEventDto`. `opendebt-payment-service` compiles with the common DTO. All three portal modules can import it via their existing `opendebt-common` dependency.

---

## 2. `opendebt-common` Changes

**Module path:** `opendebt-common/`  
**Source root:** `opendebt-common/src/main/java/`  
**Resources root:** `opendebt-common/src/main/resources/`

> **Prerequisite check (SA-050 §10, OI-6):** Verify `opendebt-common` has a `src/main/java/` and `src/main/resources/` directory and is declared as a `<module>` in the parent `pom.xml`. If `src/main/resources/` is absent, create it so JAR packaging includes templates and static assets.

---

### 2.1 `EventCategory` Enum

**File:** `dk.ufst.opendebt.common.timeline.EventCategory`

```
Package: dk.ufst.opendebt.common.timeline
Values (7, in declaration order):
  CASE, DEBT_LIFECYCLE, FINANCIAL, COLLECTION, CORRESPONDENCE, OBJECTION, JOURNAL
```

No fields or methods beyond the enum constants. No Lombok annotations needed.

**Acceptance:** `EventCategory.values().length == 7`.

---

### 2.2 `TimelineSource` Enum (internal, not rendered)

**File:** `dk.ufst.opendebt.common.timeline.TimelineSource`

```
Package: dk.ufst.opendebt.common.timeline
Values: CASE, PAYMENT
```

Used only inside `TimelineEntryDto` and `TimelineDeduplicator`. Never included in any HTTP response or Thymeleaf model exposed to the browser.

> **Rationale:** `TimelineSource` and `dedupeKey` are placed on the shared `TimelineEntryDto` (rather than a local wrapper) so that `TimelineDeduplicator` can operate as a stateless utility class that accepts only `List<TimelineEntryDto>` as input. This avoids creating a portal-scoped wrapper type in each of the three BFF controllers.

---

### 2.3 `TimelineEntryDto`

**File:** `dk.ufst.opendebt.common.timeline.TimelineEntryDto`

Annotations: `@Data @Builder @NoArgsConstructor @AllArgsConstructor`

| Field | Type | Nullable | Note |
|---|---|---|---|
| `id` | `UUID` | No | Source event `id` |
| `timestamp` | `LocalDateTime` | No | `CaseEventDto.performedAt` / `DebtEventDto.createdAt` (fallback: `effectiveDate.atStartOfDay()`) |
| `eventCategory` | `EventCategory` | No | Derived by `EventCategoryMapper` |
| `eventType` | `String` | No | Raw source event type string |
| `title` | `String` | No | i18n message key (e.g., `timeline.event.title.CASE_CREATED`) — resolved in Thymeleaf via `${#messages.msg(entry.title)}` |
| `description` | `String` | Yes | Source event description |
| `amount` | `BigDecimal` | Yes | `null` for `CaseEventDto`; `DebtEventDto.amount` |
| `debtId` | `UUID` | Yes | `null` for case-level events; `DebtEventDto.debtId` |
| `performedBy` | `String` | Yes | `CaseEventDto.performedBy`; `null` for payment events |
| `metadata` | `String` | Yes | `CaseEventDto.metadata`; `"ref:<reference>"` (+ `" corrects:<correctsEventId>"` if non-null) for debt events |
| `source` | `TimelineSource` | No | `CASE` or `PAYMENT` — **not rendered in templates** |
| `dedupeKey` | `String` | No | Computed at construction — **not rendered in templates** |

**`dedupeKey` computation** (must be set by the mapper, not lazily):
```
normalize(eventType) + "|" + (debtId != null ? debtId.toString() : "CASE") + "|" + timestamp.truncatedTo(ChronoUnit.MINUTES)
```
`normalize(eventType)` is delegated to `EventCategoryMapper.normalizeEventType(eventType)`.

**Mapping rules — from `CaseEventDto`:**

| `TimelineEntryDto` field | Rule |
|---|---|
| `id` | `CaseEventDto.id` |
| `timestamp` | `CaseEventDto.performedAt` |
| `eventType` | `CaseEventDto.eventType` |
| `eventCategory` | `EventCategoryMapper.fromCaseEventType(eventType)` |
| `title` | `"timeline.event.title." + EventCategoryMapper.normalizeEventType(eventType)` |
| `description` | `CaseEventDto.description` |
| `amount` | `null` |
| `debtId` | `null` |
| `performedBy` | `CaseEventDto.performedBy` |
| `metadata` | `CaseEventDto.metadata` |
| `source` | `TimelineSource.CASE` |

**Mapping rules — from `DebtEventDto`:**

| `TimelineEntryDto` field | Rule |
|---|---|
| `id` | `DebtEventDto.id` |
| `timestamp` | `DebtEventDto.createdAt` if non-null; else `DebtEventDto.effectiveDate.atStartOfDay()` |
| `eventType` | `DebtEventDto.eventType` |
| `eventCategory` | `EventCategoryMapper.fromDebtEventType(eventType)` |
| `title` | `"timeline.event.title." + EventCategoryMapper.normalizeEventType(eventType)` |
| `description` | `DebtEventDto.description` |
| `amount` | `DebtEventDto.amount` |
| `debtId` | `DebtEventDto.debtId` |
| `performedBy` | `null` |
| `metadata` | Compose: `"ref:" + reference` if `reference` is non-null; append `" corrects:" + correctsEventId` if non-null; `null` if both are null |
| `source` | `TimelineSource.PAYMENT` |

**Acceptance (AC-A3):** Mapped `TimelineEntryDto` from a `CaseEventDto` has non-null `id`, `timestamp`, `eventCategory`, `eventType`, `title`; `amount == null`; `debtId == null`. Mapped from `DebtEventDto` with non-null `amount` has non-null `amount` and `debtId`.

---

### 2.4 `TimelineFilterDto`

**File:** `dk.ufst.opendebt.common.timeline.TimelineFilterDto`

Annotations: `@Data @Builder @NoArgsConstructor @AllArgsConstructor`

| Field | Type | Nullable | Default |
|---|---|---|---|
| `eventCategories` | `Set<EventCategory>` | No | `new HashSet<>()` (empty = all role-allowed categories) |
| `fromDate` | `LocalDate` | Yes | `null` |
| `toDate` | `LocalDate` | Yes | `null` |
| `debtId` | `UUID` | Yes | `null` |

**Filter semantics (enforced in BFF controller):**
- `eventCategories` non-empty → include only entries whose `eventCategory` is in the set AND in the role's allowed categories.
- `fromDate` non-null → `entry.timestamp.toLocalDate() >= fromDate` (inclusive).
- `toDate` non-null → `entry.timestamp.toLocalDate() <= toDate` (inclusive).
- `debtId` non-null → include only entries where `entry.debtId.equals(debtId)`.
- All active filter conditions are ANDed together.

**Acceptance (AC-D1 through AC-D5):** Filter by single category, combined categories, date range, and debt all produce correctly bounded entry sets.

---

### 2.5 `EventCategoryMapper`

**File:** `dk.ufst.opendebt.common.timeline.EventCategoryMapper`

A non-Spring utility class (no `@Component`). All methods are `public static`.

**Responsibilities:**
1. Map a raw `CaseEventDto.eventType` string → `EventCategory`.
2. Map a raw `DebtEventDto.eventType` string → `EventCategory`.
3. Normalise an event type string to a canonical deduplication key.

**Method signatures:**

```java
public static EventCategory fromCaseEventType(String eventType)
public static EventCategory fromDebtEventType(String eventType)
public static String normalizeEventType(String eventType)
```

> **Note:** Title key construction is performed inline at the call site as `'timeline.event.title.' + EventCategoryMapper.normalizeEventType(eventType)`. There is no separate `titleKeyFor()` method. References to `titleKeyFor()` in the architecture document §4.3 are informal shorthand for this inline pattern.

**Normalisation and category mapping table** (starter set — complete from actual service event type data per OI-3):

| Raw event type (source) | `normalizeEventType()` output | `EventCategory` |
|---|---|---|
| `CASE_CREATED` (case) | `CASE_CREATED` | `CASE` |
| `CASE_STATUS_CHANGED` (case) | `CASE_STATUS_CHANGED` | `CASE` |
| `CASE_ASSIGNED` (case) | `CASE_ASSIGNED` | `CASE` |
| `DEBT_REGISTERED` (case) | `DEBT_REGISTERED` | `DEBT_LIFECYCLE` |
| `DEBT_REGISTRATION` (payment) | `DEBT_REGISTERED` | `DEBT_LIFECYCLE` |
| `WRITEOFF` (case) | `DEBT_WRITEOFF` | `DEBT_LIFECYCLE` |
| `DEBT_WRITEOFF` (payment) | `DEBT_WRITEOFF` | `DEBT_LIFECYCLE` |
| `PAYMENT_APPLIED` (case) | `PAYMENT_RECEIVED` | `FINANCIAL` |
| `PAYMENT_RECEIVED` (payment) | `PAYMENT_RECEIVED` | `FINANCIAL` |
| `REFUND` (payment) | `REFUND` | `FINANCIAL` |
| `PARTIAL_PAYMENT` (payment) | `PARTIAL_PAYMENT` | `FINANCIAL` |
| `COLLECTION_MEASURE_INITIATED` (case) | `COLLECTION_MEASURE_INITIATED` | `COLLECTION` |
| `OBJECTION_FILED` (case) | `OBJECTION_FILED` | `OBJECTION` |
| `OBJECTION_OUTCOME` (case) | `OBJECTION_OUTCOME` | `OBJECTION` |
| `JOURNAL_ENTRY_ADDED` (case) | `JOURNAL_ENTRY_ADDED` | `JOURNAL` |
| `JOURNAL_NOTE_ADDED` (case) | `JOURNAL_NOTE_ADDED` | `JOURNAL` |

**Unmapped event types:** Return `EventCategory.CASE` from `fromCaseEventType` and `EventCategory.FINANCIAL` from `fromDebtEventType` as safe defaults. Log a `WARN` when a default is used (to surface missing mappings during integration testing).

**Acceptance (OI-3):** Before shipping, all event type values produced by case-service and payment-service in the staging environment must appear in this table. The implementation team must enumerate them by querying both services' databases.

---

### 2.6 `TimelineDeduplicator`

**File:** `dk.ufst.opendebt.common.timeline.TimelineDeduplicator`

A non-Spring utility class. Single public static method:

```java
public static List<TimelineEntryDto> deduplicate(List<TimelineEntryDto> entries)
```

**Algorithm** (see SA-050 §4.4 for rationale):

1. Iterate `entries` in the order received (case-service entries first, payment-service entries second — caller is responsible for ordering).
2. Maintain a `LinkedHashMap<String, TimelineEntryDto> seen` keyed by `entry.dedupeKey`.
3. For each entry:
   - If `seen` does not contain the key → `seen.put(key, entry)`.
   - If `seen` already contains the key AND the stored entry has `amount == null` AND the incoming entry has `amount != null` → overwrite: `seen.put(key, entry)` (prefer PAYMENT source when it carries financial data).
   - Otherwise → discard the incoming entry (CASE source wins when both amounts are null).
4. Return `new ArrayList<>(seen.values())`.

**Pre-condition:** `entry.dedupeKey` must be non-null on all entries. Throw `IllegalArgumentException` if any entry has a null `dedupeKey`.

**Acceptance (AC-A2):** Given a list containing both a `DEBT_REGISTERED` (case-source, `amount=null`) and a `DEBT_REGISTRATION` (payment-source, `amount=BigDecimal(5000)`) entry for the same `debtId` within the same minute, `deduplicate()` returns exactly one entry with `amount` non-null and `source == PAYMENT`.

---

### 2.7 `TimelineVisibilityProperties`

**File:** `dk.ufst.opendebt.common.timeline.TimelineVisibilityProperties`

**Annotations:** `@ConfigurationProperties(prefix = "opendebt.timeline.visibility")`  
**Lombok:** `@Getter @Setter` (no `@Component` — each portal enables it via `@EnableConfigurationProperties`)

```java
@ConfigurationProperties(prefix = "opendebt.timeline.visibility")
public class TimelineVisibilityProperties {

    /** Maps role name → allowed event categories. Role names must match Spring Security role strings (without ROLE_ prefix). */
    private Map<String, Set<EventCategory>> roleCategories = new HashMap<>();

    /**
     * Returns the set of event categories allowed for the given role.
     * Returns an empty set if the role is not configured (yields empty timeline).
     */
    public Set<EventCategory> getAllowedCategories(String role) {
        return roleCategories.getOrDefault(role, Set.of());
    }
}
```

**Acceptance (AC-B4):** No Thymeleaf template contains any role-checking logic. All role-to-category mapping is resolved by this class in the BFF controller before the model is passed to the fragment.

---

### 2.8 `fragments/timeline.html`

**File:** `opendebt-common/src/main/resources/templates/fragments/timeline.html`

This fragment is resolved at runtime from all three portal classpaths via Spring Boot's default `ClassPathTemplateResolver`. Portals must **not** create their own `templates/fragments/timeline.html`.

#### Fragment selectors

| Selector | Used by | HTMX swap mode |
|---|---|---|
| `timeline-panel` | Tab load (`hx-trigger="revealed"`) and filter form submit | `innerHTML` on `#panel-events` (tab load) or `outerHTML` on `#timeline-panel` (filter) |
| `timeline-entries` | "Load more" click | `beforeend` on `#timeline-entries`; plus OOB update of `#load-more-container` |

#### Model contract (all model attributes consumed by the fragment)

| Attribute name | Type | Notes |
|---|---|---|
| `entries` | `List<TimelineEntryDto>` | Current page — already filtered, deduplicated, role-filtered, sorted descending |
| `page` | `int` | Current 1-based page number |
| `size` | `int` | Page size |
| `hasMore` | `boolean` | Whether more pages exist |
| `totalCount` | `int` | Total entries matching active filters (for display) |
| `filters` | `TimelineFilterDto` | Active filter state (chip rendering + form pre-population) |
| `warnings` | `List<String>` | i18n message keys for partial-data warnings; empty if all services available |
| `caseId` | `UUID` | For HTMX URL construction |
| `availableDebts` | `List<CaseDebtDto>` | Debt selector options (`debtId`: UUID; `transferReference`: `@Nullable String` — falls back to UUID string in the debt selector dropdown when null) |

#### Required structure (logical — implement using SKAT design-system classes)

```
#timeline-panel (th:fragment="timeline-panel")
  ├── Warning banner (rendered if !warnings.isEmpty())
  │     Each warning key resolved via #{warningKey}
  ├── Filter bar (form with GET action pointing to /cases/{caseId}/tidslinje)
  │     - Category multi-select (populated from EventCategory.values() ∩ allowed by role — role filter applied server-side; template shows only what's in entries)
  │     - From-date input (type="date")
  │     - To-date input (type="date")
  │     - Debt selector (populated from availableDebts)
  │     - Submit: hx-get, hx-target="#timeline-panel", hx-swap="outerHTML"
  ├── Active filter chips (one per active filter in TimelineFilterDto)
  │     Each chip has a remove link (single filter cleared, GET re-submit)
  │     "Clear all" link (clears all, GET re-submit)
  ├── <ol id="timeline-entries" aria-live="polite" th:fragment="timeline-entries">
  │     Empty state (th:if="${entries.isEmpty()}"): <li>#{timeline.empty}</li>
  │     <li th:each="entry : ${entries}" role="article"
  │           th:aria-label="${#temporals.format(entry.timestamp,'dd-MM-yyyy HH:mm')} + ' - ' + ${#messages.msg(entry.title)}">
  │       - Timestamp: #temporals.format(entry.timestamp, 'dd-MM-yyyy HH:mm')
  │       - Category badge: CSS class skat-badge skat-badge--timeline-{entry.eventCategory.name().toLowerCase()}
  │         badge text: #{timeline.category.{entry.eventCategory.name()}}
  │       - Title: ${#messages.msg(entry.title)} (entry.title IS the i18n key, resolved at runtime)
  │       - Description: entry.description (nullable — omit if null)
  │       - Amount (th:if="${entry.amount != null}"):
  │           #numbers.formatDecimal(entry.amount, 1, 'COMMA', 2, 'POINT') + ' DKK'
  │       - Debt link (th:if="${entry.debtId != null}"):
  │           <a th:href="@{/cases/{cId}/debts/{dId}(cId=${caseId},dId=${entry.debtId})}">
  │             th:text="${entry.debtId}"
  │           </a>
  │       - PerformedBy (th:if="${entry.performedBy != null}"):
  │           entry.performedBy
  │     </li>
  └── #load-more-container (th:fragment="load-more-button")
        <button th:if="${hasMore}"
                th:attr="hx-get=@{/cases/{id}/tidslinje/entries(
                            id=${caseId}, page=${page + 1}, size=${size},
                            eventCategory=${filters.eventCategories},
                            fromDate=${filters.fromDate}, toDate=${filters.toDate},
                            debtId=${filters.debtId})}"
                hx-target="#timeline-entries"
                hx-swap="beforeend"
                aria-controls="timeline-entries"
                class="skat-btn skat-btn--secondary"
                th:text="#{timeline.load.more}">
          Indlæs flere
        </button>
```

**`/tidslinje/entries` response** must include **both**:
1. The `<li>` entry elements (appended via `beforeend`).
2. `<div id="load-more-container" hx-swap-oob="true">` wrapping the conditionally rendered button — this replaces the existing button OOB to hide it when `hasMore=false`.

**No role-checking logic in the template.** The template renders whatever is in `entries` without inspecting roles.

**Accessibility requirements** (WCAG 2.1 AA — AC from NFR):
- `<ol>` has `aria-live="polite"`.
- Each `<li>` has `role="article"` and `aria-label` with timestamp and title.
- Category badge uses both colour class AND text label (colour is never the sole category indicator).
- Filter form inputs have explicit `<label for="...">` associations.
- Date inputs use `type="date"`.
- "Load more" button has `aria-controls="timeline-entries"`.

---

### 2.9 `static/css/timeline.css`

**File:** `opendebt-common/src/main/resources/static/css/timeline.css`

Defines CSS modifier classes for category badges. One class per `EventCategory` value:

| CSS class | Category |
|---|---|
| `.skat-badge--timeline-case` | `CASE` |
| `.skat-badge--timeline-debt_lifecycle` | `DEBT_LIFECYCLE` |
| `.skat-badge--timeline-financial` | `FINANCIAL` |
| `.skat-badge--timeline-collection` | `COLLECTION` |
| `.skat-badge--timeline-correspondence` | `CORRESPONDENCE` |
| `.skat-badge--timeline-objection` | `OBJECTION` |
| `.skat-badge--timeline-journal` | `JOURNAL` |

Each class sets a `background-color` using a SKAT design-system CSS variable or colour token. Colour selection must be distinct across all 7 categories (AC-C3). Icons or glyphs supplement colour so colour is not the sole category indicator (WCAG 2.1 AA, §8.3).

---

### 2.10 i18n Message Keys (required in every portal)

These keys are referenced by `fragments/timeline.html` using Thymeleaf `#{...}` expressions. They must be added to **each portal's** `messages_da.properties` and `messages_en_GB.properties`.

#### Tab and navigation keys

| Key | Danish (`messages_da.properties`) | English (`messages_en_GB.properties`) |
|---|---|---|
| `case.detail.tab.timeline` | `Tidslinje` | `Timeline` |
| `timeline.loading` | `Indlæser tidslinje…` | `Loading timeline…` |
| `timeline.load.more` | `Indlæs flere` | `Load more` |
| `timeline.empty` | `Ingen hændelser registreret` | `No events registered` |
| `timeline.warning.partial` | `Nogle hændelser kunne ikke hentes` | `Some events could not be retrieved` |

#### Filter bar keys

| Key | Danish | English |
|---|---|---|
| `timeline.filter.category` | `Kategori` | `Category` |
| `timeline.filter.fromDate` | `Fra dato` | `From date` |
| `timeline.filter.toDate` | `Til dato` | `To date` |
| `timeline.filter.debt` | `Fordring` | `Debt` |
| `timeline.filter.clear` | `Nulstil filtre` | `Clear all filters` |
| `timeline.filter.all.categories` | `Alle kategorier` | `All categories` |

#### Event category display names

| Key | Danish | English |
|---|---|---|
| `timeline.category.CASE` | `Sag` | `Case` |
| `timeline.category.DEBT_LIFECYCLE` | `Fordringens livscyklus` | `Debt lifecycle` |
| `timeline.category.FINANCIAL` | `Finansiel` | `Financial` |
| `timeline.category.COLLECTION` | `Inddrivelse` | `Collection` |
| `timeline.category.CORRESPONDENCE` | `Korrespondance` | `Correspondence` |
| `timeline.category.OBJECTION` | `Indsigelse` | `Objection` |
| `timeline.category.JOURNAL` | `Journal` | `Journal` |

#### Event title keys (per normalised event type)

Pattern: `timeline.event.title.<NORMALISED_EVENT_TYPE>`. Each maps to a human-readable title. The full set must be populated for all entries in the normalisation table (§2.5). Starter set:

| Key | Danish | English |
|---|---|---|
| `timeline.event.title.CASE_CREATED` | `Sag oprettet` | `Case created` |
| `timeline.event.title.CASE_STATUS_CHANGED` | `Sagtilstand ændret` | `Case status changed` |
| `timeline.event.title.CASE_ASSIGNED` | `Sag tildelt sagsbehandler` | `Case assigned to caseworker` |
| `timeline.event.title.DEBT_REGISTERED` | `Fordring registreret` | `Debt registered` |
| `timeline.event.title.DEBT_WRITEOFF` | `Fordring afskrevet` | `Debt written off` |
| `timeline.event.title.PAYMENT_RECEIVED` | `Betaling modtaget` | `Payment received` |
| `timeline.event.title.REFUND` | `Tilbagebetaling` | `Refund` |
| `timeline.event.title.PARTIAL_PAYMENT` | `Delbetaling modtaget` | `Partial payment received` |
| `timeline.event.title.COLLECTION_MEASURE_INITIATED` | `Inddrivelsesskridt iværksat` | `Collection measure initiated` |
| `timeline.event.title.OBJECTION_FILED` | `Indsigelse modtaget` | `Objection filed` |
| `timeline.event.title.OBJECTION_OUTCOME` | `Indsigelse afgjort` | `Objection resolved` |
| `timeline.event.title.JOURNAL_ENTRY_ADDED` | `Journalpost tilføjet` | `Journal entry added` |
| `timeline.event.title.JOURNAL_NOTE_ADDED` | `Note tilføjet` | `Note added` |

---

## 3. `opendebt-caseworker-portal` Changes

**Base package:** `dk.ufst.opendebt.caseworker`

---

### 3.1 `bffFetchExecutor` Bean

**Add to:** `dk.ufst.opendebt.caseworker.config.WebClientConfig` (or a new `TimelineConfig` class in the same package)

```java
@Bean(destroyMethod = "shutdown")
public ExecutorService bffFetchExecutor() {
    ThreadFactory factory = Thread.ofPlatform()
        .name("bff-fetch-", 0)
        .factory();
    return Executors.newFixedThreadPool(10, factory);
}
```

The bean name `bffFetchExecutor` must match the injection point in `CaseworkerTimelineController`.

**Rationale:** Parallel `CompletableFuture.supplyAsync()` calls must not use `ForkJoinPool.commonPool()` which is unsuitable for blocking I/O. See SA-050 DD-2.

---

### 3.2 `CaseServiceClient` — No Change Required

`dk.ufst.opendebt.caseworker.client.CaseServiceClient` already provides:

```java
@CircuitBreaker(name = "case-service", fallbackMethod = "getEventsFallback")
@Retry(name = "case-service")
public List<CaseEventDto> getEvents(UUID caseId)
```

with a fallback returning `List.of()`. No changes needed.

---

### 3.3 `PaymentServiceClient` — Add `getDebtEventsByCase`

**File:** `dk.ufst.opendebt.caseworker.client.PaymentServiceClient`

Add the following method and fallback. The return type is `List<DebtEventDto>` from `opendebt-common` (after OI-2), **not** `List<PortalDebtEventDto>` (the existing ledger-oriented method continues to use `PortalDebtEventDto` unchanged).

```java
private static final String CIRCUIT_BREAKER_NAME = "payment-service"; // already defined in class

/**
 * Retrieves all debt events for all debts linked to a case.
 * Used exclusively by the timeline BFF aggregation.
 */
@CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getDebtEventsByCaseFallback")
@Retry(name = CIRCUIT_BREAKER_NAME)
public List<DebtEventDto> getDebtEventsByCase(UUID caseId) {
    return webClient.get()
        .uri("/api/v1/events/case/{caseId}", caseId)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<DebtEventDto>>() {})
        .block();
}

private List<DebtEventDto> getDebtEventsByCaseFallback(UUID caseId, Throwable t) {
    if (t instanceof WebClientResponseException wcre) {
        throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for getDebtEventsByCase: {}", t.getMessage());
    return List.of();
}
```

**Import:** `dk.ufst.opendebt.common.dto.DebtEventDto` (from common, after OI-2).

---

### 3.4 `TimelineVisibilityProperties` — Configuration

Add `@EnableConfigurationProperties(TimelineVisibilityProperties.class)` to `dk.ufst.opendebt.caseworker.config.WebClientConfig` (or any `@Configuration` class).

Add to `application.properties` (or `application.yml`):

```properties
# Caseworker portal — all 7 categories visible to CASEWORKER, SUPERVISOR, ADMIN
opendebt.timeline.visibility.role-categories.CASEWORKER=CASE,DEBT_LIFECYCLE,FINANCIAL,COLLECTION,CORRESPONDENCE,OBJECTION,JOURNAL
opendebt.timeline.visibility.role-categories.SUPERVISOR=CASE,DEBT_LIFECYCLE,FINANCIAL,COLLECTION,CORRESPONDENCE,OBJECTION,JOURNAL
opendebt.timeline.visibility.role-categories.ADMIN=CASE,DEBT_LIFECYCLE,FINANCIAL,COLLECTION,CORRESPONDENCE,OBJECTION,JOURNAL
```

**Acceptance (AC-B1):** `TimelineVisibilityProperties.getAllowedCategories("CASEWORKER")` returns a set of all 7 `EventCategory` values.

---

### 3.5 `CaseworkerTimelineController`

**File:** `dk.ufst.opendebt.caseworker.controller.CaseworkerTimelineController`

**Annotations:** `@Slf4j @Controller @RequiredArgsConstructor`

**Constructor injection:**
```java
private final CaseServiceClient caseServiceClient;
private final PaymentServiceClient paymentServiceClient;
private final TimelineVisibilityProperties visibilityProperties;
private final ExecutorService bffFetchExecutor;
```

#### Endpoint 1: `GET /cases/{caseId}/tidslinje`

Returns the full `timeline-panel` fragment. Called on tab reveal and on filter form submission.

```
@GetMapping("/cases/{caseId}/tidslinje")
@PreAuthorize("hasAnyRole('CASEWORKER', 'SUPERVISOR', 'ADMIN')")
public String showTimeline(
    @PathVariable UUID caseId,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "25") int size,
    @RequestParam(required = false) String[] eventCategory,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
    @RequestParam(required = false) UUID debtId,
    Authentication auth,
    Model model)
```

**Controller logic:**

1. Resolve allowed categories: `Set<EventCategory> allowed = visibilityProperties.getAllowedCategories(primaryRole(auth))`.
2. Build `TimelineFilterDto` from query params; constrain `eventCategory` to intersection with `allowed`.
3. Launch parallel fetch (4-second join timeout):
   ```
   List<String> warnings = Collections.synchronizedList(new ArrayList<>());
   CompletableFuture<List<CaseEventDto>> caseFuture = CompletableFuture.supplyAsync(
       () -> caseServiceClient.getEvents(caseId), bffFetchExecutor)
       .exceptionally(t -> { warnings.add("timeline.warning.partial"); return List.of(); });
   CompletableFuture<List<DebtEventDto>> paymentFuture = CompletableFuture.supplyAsync(
       () -> paymentServiceClient.getDebtEventsByCase(caseId), bffFetchExecutor)
       .exceptionally(t -> { warnings.add("timeline.warning.partial"); return List.of(); });
   CompletableFuture.allOf(caseFuture, paymentFuture).get(4, TimeUnit.SECONDS);
   ```
4. Map: `List<TimelineEntryDto> caseEntries` (from `CaseEventDto` per §2.3 mapping rules); `List<TimelineEntryDto> paymentEntries` (from `DebtEventDto` per §2.3 mapping rules).
5. Concatenate case entries first, then payment entries.
6. Deduplicate: `List<TimelineEntryDto> merged = TimelineDeduplicator.deduplicate(concatenated)`.
7. Role-filter: retain only entries where `allowed.contains(entry.eventCategory)`.
8. Apply `TimelineFilterDto` filters (see §2.4 filter semantics).
9. Sort descending by `timestamp`.
10. `int totalCount = filtered.size()`.
11. Paginate: `List<TimelineEntryDto> page_entries = filtered.subList(offset, min(offset+size, filtered.size()))` where `offset = (page-1)*size`.
12. `boolean hasMore = totalCount > page * size`.
13. Fetch `availableDebts`: `caseServiceClient.getCase(caseId)?.getDebtIds()` mapped to `List<CaseDebtDto>` (one per UUID, `debtId` field set, `transferReference` explicitly `@Nullable String` — the template renders UUID string as fallback display label when null). If `getCase` returns null (fallback), use `List.of()`. **Note:** the architecture §4.7 defines `transferReference` as non-null; that is incorrect — the field is nullable per this fallback logic.
14. Populate model: `entries`, `page`, `size`, `hasMore`, `totalCount`, `filters`, `warnings` (deduplicated list), `caseId`, `availableDebts`.
15. Return `"fragments/timeline :: timeline-panel"`.

**`primaryRole(Authentication auth)` helper:** Extract the first granted authority matching any of `[CASEWORKER, SUPERVISOR, ADMIN]` (strip `ROLE_` prefix if present). Return the matching role name as String.

#### Endpoint 2: `GET /cases/{caseId}/tidslinje/entries`

Returns only entry `<li>` elements plus the OOB load-more container. Called by "Load more" button.

```
@GetMapping("/cases/{caseId}/tidslinje/entries")
@PreAuthorize("hasAnyRole('CASEWORKER', 'SUPERVISOR', 'ADMIN')")
public String loadMoreEntries(
    @PathVariable UUID caseId,
    @RequestParam int page,
    @RequestParam(defaultValue = "25") int size,
    @RequestParam(required = false) String[] eventCategory,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
    @RequestParam(required = false) UUID debtId,
    Authentication auth,
    Model model)
```

**Same aggregation pipeline as above** (steps 1–12). Steps 13 (availableDebts) and warnings are included in model but not rendered by `timeline-entries` fragment selector. Return `"fragments/timeline :: timeline-entries"`.

---

### 3.6 Case Detail Page — Tab Change

**File:** `opendebt-caseworker-portal/src/main/resources/templates/cases/detail.html`

#### Change 1 — Tab button (around line 75)

Replace:
```html
<button role="tab" id="tab-events" aria-controls="panel-events" aria-selected="false"
        class="skat-btn skat-btn--sm skat-btn--secondary"
        onclick="showTab('events')" th:text="#{case.detail.tab.events}">H&#230;ndelseslog</button>
```

With:
```html
<button role="tab" id="tab-events" aria-controls="panel-events" aria-selected="false"
        class="skat-btn skat-btn--sm skat-btn--secondary"
        onclick="showTab('events')"
        th:attr="hx-get=@{/cases/{id}/tidslinje(id=${caseDto.id})}"
        hx-target="#panel-events"
        hx-swap="innerHTML"
        hx-trigger="click once"
        th:text="#{case.detail.tab.timeline}">Tidslinje</button>
```

#### Change 2 — Events tab panel body (around line 186)

Replace the entire `<!-- Events tab panel -->` `<div>` block (currently contains a `<table>` with 4 columns rendered from `${events}`) with:

```html
<!-- Events tab panel — unified timeline -->
<div role="tabpanel" id="panel-events" aria-labelledby="tab-events" style="display: none;">
  <p th:text="#{timeline.loading}">Indlæser tidslinje…</p>
</div>
```

HTMX replaces the `<p>` loading text with the `timeline-panel` fragment content on first tab activation. The panel body must not pre-render any events server-side on page load.

**Remove from `CaseDetailController`:** The existing model attribute `events` (the `List<CaseEventDto>` currently fetched and added to the model in the case detail controller) is no longer needed by the detail page. Remove the `CaseServiceClient.getEvents()` call and `model.addAttribute("events", ...)` from `CaseDetailController` to eliminate a redundant upstream call on every case detail page load.

**Acceptance (AC-C1, FR-7.1, FR-7.2):** Tab button reads "Tidslinje" (da) / "Timeline" (en). Clicking it triggers HTMX load of the unified timeline fragment.

**i18n addition (caseworker-portal `messages_da.properties` and `messages_en_GB.properties`):** Add all keys from §2.10. Additionally, the existing `case.detail.tab.events` key value can remain unchanged (key is no longer referenced by the template after this change, but removing it is optional housekeeping).

---

## 4. `opendebt-citizen-portal` Changes

**Base package:** `dk.ufst.opendebt.citizen`

> **Prerequisite: No citizen case detail page exists.** The citizen portal currently has no `cases/detail.html` template and no case detail controller. Implementing the Tidslinje for citizen users requires creating a minimal case detail page. This page must be scoped and created as part of this petition. At minimum it must include the Tidslinje tab panel; other tabs (overview, debts) may be added as stub panels or deferred.

---

### 4.1 `bffFetchExecutor` Bean

**Add to:** `dk.ufst.opendebt.citizen.config.WebClientConfig`

Identical declaration to §3.1. Bean name: `bffFetchExecutor`.

---

### 4.2 `CaseServiceClient` (new)

**File:** `dk.ufst.opendebt.citizen.client.CaseServiceClient`

**Annotations:** `@Slf4j @Component`

Follow the exact Resilience4j pattern from caseworker-portal `CaseServiceClient`. Implement:

```java
public CaseServiceClient(
    WebClient.Builder webClientBuilder,
    @Value("${opendebt.services.case-service.url:http://localhost:8081}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
}

@CircuitBreaker(name = "case-service", fallbackMethod = "getEventsFallback")
@Retry(name = "case-service")
public List<CaseEventDto> getEvents(UUID caseId) { ... }

private List<CaseEventDto> getEventsFallback(UUID caseId, Throwable t) {
    if (t instanceof WebClientResponseException wcre) { throw wcre; }
    log.warn("Circuit breaker fallback triggered for getEvents: {}", t.getMessage());
    return List.of();
}

@CircuitBreaker(name = "case-service", fallbackMethod = "getCaseFallback")
@Retry(name = "case-service")
public CaseDto getCase(UUID caseId) { ... }

private CaseDto getCaseFallback(UUID caseId, Throwable t) {
    if (t instanceof WebClientResponseException wcre) { throw wcre; }
    log.warn("Circuit breaker fallback triggered for getCase: {}", t.getMessage());
    return null;
}
```

**Resilience4j config** in citizen portal `application.properties`: Add `case-service` circuit breaker and retry config following the same parameter values as caseworker portal (50% failure threshold, 30s wait, 3 retries, 500ms × 2 backoff).

---

### 4.3 `PaymentServiceClient` (new)

**File:** `dk.ufst.opendebt.citizen.client.PaymentServiceClient`

**Annotations:** `@Slf4j @Component`

Implement only `getDebtEventsByCase` (no ledger methods — the citizen timeline does not need ledger data):

```java
public PaymentServiceClient(
    WebClient.Builder webClientBuilder,
    @Value("${opendebt.services.payment-service.url:http://localhost:8083}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
}

@CircuitBreaker(name = "payment-service", fallbackMethod = "getDebtEventsByCaseFallback")
@Retry(name = "payment-service")
public List<DebtEventDto> getDebtEventsByCase(UUID caseId) { ... }

private List<DebtEventDto> getDebtEventsByCaseFallback(UUID caseId, Throwable t) {
    if (t instanceof WebClientResponseException wcre) { throw wcre; }
    log.warn("Circuit breaker fallback triggered for getDebtEventsByCase: {}", t.getMessage());
    return List.of();
}
```

**Resilience4j config** in citizen portal `application.properties`: Add `payment-service` circuit breaker and retry entries.

---

### 4.4 `TimelineVisibilityProperties` — Configuration

Add `@EnableConfigurationProperties(TimelineVisibilityProperties.class)` to a citizen portal `@Configuration` class.

Add to `application.properties`:

```properties
opendebt.timeline.visibility.role-categories.CITIZEN=FINANCIAL,DEBT_LIFECYCLE,CORRESPONDENCE,COLLECTION
```

**Acceptance (AC-B2):** `getAllowedCategories("CITIZEN")` returns `{FINANCIAL, DEBT_LIFECYCLE, CORRESPONDENCE, COLLECTION}`. No entries from `CASE`, `OBJECTION`, or `JOURNAL` reach the template.

---

### 4.5 `CitizenTimelineController`

**File:** `dk.ufst.opendebt.citizen.controller.CitizenTimelineController`

**Annotations:** `@Slf4j @Controller @RequiredArgsConstructor`

**Constructor injection:**
```java
private final CaseServiceClient caseServiceClient;       // dk.ufst.opendebt.citizen.client
private final PaymentServiceClient paymentServiceClient; // dk.ufst.opendebt.citizen.client
private final TimelineVisibilityProperties visibilityProperties;
private final ExecutorService bffFetchExecutor;
```

**Endpoint 1:** `GET /cases/{caseId}/tidslinje`  
`@PreAuthorize("hasRole('CITIZEN')")`

**Endpoint 2:** `GET /cases/{caseId}/tidslinje/entries`  
`@PreAuthorize("hasRole('CITIZEN')")`

**Controller logic is identical to §3.5** with the following difference in `primaryRole()`: return `"CITIZEN"` (citizen portal has exactly one role).

**Acceptance (AC-F3, AC-B2):** Citizen user receives only entries in `{FINANCIAL, DEBT_LIFECYCLE, CORRESPONDENCE, COLLECTION}`. No `CASE`, `OBJECTION`, or `JOURNAL` entries appear.

---

### 4.6 Citizen Case Detail Page

**New file:** `opendebt-citizen-portal/src/main/resources/templates/cases/detail.html`

Create a minimal case detail page using `layout:decorate="~{layout/default}"`. The page must include at minimum:
- Case reference/number in the page heading.
- A tab for "Tidslinje" wired identically to §3.6 Change 1 (HTMX `hx-trigger="click once"` on tab button, `hx-get` pointing to `/cases/{caseId}/tidslinje`).
- The `#panel-events` tab panel div with loading placeholder (§3.6 Change 2 pattern).

#### `CaseDetailController` specification

- **Class:** `dk.ufst.opendebt.citizen.controller.CaseDetailController`
- **Endpoint:** `@GetMapping("/cases/{caseId}")`
- **Calls:** `CaseServiceClient.getCase(caseId)` returning a `CaseDto` (reuse existing type from `opendebt-common` if available, otherwise define a minimal record `CaseDto(UUID caseId, String caseReference)`)
- **Model attributes populated:**
  - `caseId` (UUID) — from path variable
  - `caseReference` (String) — from `CaseDto.caseReference()`, used in page heading `<h1>`
  - `serviceError` (String, optional) — set if `CaseServiceClient.getCase()` call fails
- **View:** `cases/detail`
- The page renders a `<h1>` containing the case reference and a Tidslinje tab as the sole initial tab.

**i18n additions to citizen portal `messages_da.properties` and `messages_en_GB.properties`:** Add all keys from §2.10 plus any page-level keys needed for the case detail page heading and breadcrumb.

---

## 5. `opendebt-creditor-portal` Changes

**Base package:** `dk.ufst.opendebt.creditor`

> **Note on URL conventions:** The creditor portal uses "claims" (fordringer) and `claims/detail.html`. The confirmed URL base for claim navigation is `/fordring/{id}`, matching the existing `ClaimDetailController.java` which maps `@GetMapping("/fordring/{id}")`. Timeline endpoints follow the same base path.

---

### 5.1 `bffFetchExecutor` Bean

**Add to:** `dk.ufst.opendebt.creditor.config.WebClientConfig`

Identical to §3.1. Bean name: `bffFetchExecutor`.

---

### 5.2 `CaseServiceClient` — Extend with `getEvents`

**File:** `dk.ufst.opendebt.creditor.client.CaseServiceClient` (existing — currently has only `listCases()`)

Add:

```java
@CircuitBreaker(name = "case-service", fallbackMethod = "getEventsFallback")
@Retry(name = "case-service")
public List<CaseEventDto> getEvents(UUID caseId) {
    return webClient.get()
        .uri("/api/v1/cases/{id}/events", caseId)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<CaseEventDto>>() {})
        .block();
}

private List<CaseEventDto> getEventsFallback(UUID caseId, Throwable t) {
    if (t instanceof WebClientResponseException wcre) { throw wcre; }
    log.warn("Circuit breaker fallback triggered for getEvents: {}", t.getMessage());
    return List.of();
}
```

Also add `getCase(UUID caseId)` with `getCaseFallback` returning `null` (same pattern as §4.2), for `availableDebts` population.

---

### 5.3 `PaymentServiceClient` (new)

**File:** `dk.ufst.opendebt.creditor.client.PaymentServiceClient`

**Annotations:** `@Slf4j @Component`

Implement only `getDebtEventsByCase` — same structure as §4.3, pointing to:

```java
@Value("${opendebt.services.payment-service.url:http://localhost:8083}")
```

Resilience4j `payment-service` config in creditor portal `application.properties`.

---

### 5.4 `TimelineVisibilityProperties` — Configuration

Add `@EnableConfigurationProperties(TimelineVisibilityProperties.class)` to a creditor portal `@Configuration` class.

Add to `application.properties`:

```properties
opendebt.timeline.visibility.role-categories.CREDITOR=FINANCIAL,DEBT_LIFECYCLE,COLLECTION
```

**Acceptance (AC-B3):** `getAllowedCategories("CREDITOR")` returns `{FINANCIAL, DEBT_LIFECYCLE, COLLECTION}`. No `CASE`, `CORRESPONDENCE`, `OBJECTION`, or `JOURNAL` entries reach the template.

---

### 5.5 `CreditorTimelineController`

**File:** `dk.ufst.opendebt.creditor.controller.CreditorTimelineController`

**Annotations:** `@Slf4j @Controller @RequiredArgsConstructor`

**Constructor injection:**
```java
private final CaseServiceClient caseServiceClient;
private final PaymentServiceClient paymentServiceClient;
private final TimelineVisibilityProperties visibilityProperties;
private final ExecutorService bffFetchExecutor;
```

**Endpoint 1:** `GET /fordring/{id}/tidslinje`
`@PreAuthorize("hasRole('CREDITOR')")`

**Endpoint 2:** `GET /fordring/{id}/tidslinje/poster`
`@PreAuthorize("hasRole('CREDITOR')")`

**Model variable:** `claimId` (UUID, from path variable `{id}`) — used in model and HTMX URL construction. Use `claimId` consistently; do not use `caseId` in the creditor portal controller or template.

`primaryRole()` returns `"CREDITOR"`. All other logic identical to §3.5.

---

### 5.6 Claim Detail Page — Timeline Card Addition

**File:** `opendebt-creditor-portal/src/main/resources/templates/claims/detail.html`

The actual `claims/detail.html` uses a flat `skat-card` layout — there are no tabs in the creditor portal claim detail page. The Tidslinje is therefore added as a **new `skat-card` section** appended after the last existing card.

#### `CreditorTimelineController` — full page endpoint

`GET /fordring/{id}/tidslinje` renders the full `claims/detail.html` view (not a tab fragment). The controller adds a `timelineEntries` model attribute containing the first page of timeline entries, plus all standard model attributes required by `claims/detail.html`. The HTMX "Load more" endpoint (`GET /fordring/{id}/tidslinje/poster`) returns only the `timeline-entries` fragment for `beforeend` swap.

#### New `skat-card` block

Add the following block after the last existing `skat-card` closing tag in `claims/detail.html`:

```html
<!-- Tidslinje -->
<div class="skat-card">
  <div class="skat-card__header">
    <h2 th:text="#{case.detail.tab.timeline}">Tidslinje</h2>
  </div>
  <div class="skat-card__body" id="panel-events">
    <div th:replace="~{fragments/timeline :: timeline-panel}"></div>
  </div>
</div>
```

The `timeline-panel` fragment is server-side rendered on page load (no deferred HTMX tab trigger). The HTMX "Load more" button within the fragment targets `#timeline-entries` using `beforeend` swap exactly as specified in §2.8. The `GET /fordring/{id}/tidslinje/poster` endpoint returns the `timeline-entries` fragment OOB response.

**i18n additions** to creditor portal `messages_da.properties` and `messages_en_GB.properties`: Add all keys from §2.10.

---

## 6. Test Specifications

### 6.1 Unit Tests

#### `EventCategoryMapperTest`

**Location:** `opendebt-common` test sources

| Test case | Input | Expected output |
|---|---|---|
| Maps known case event type | `fromCaseEventType("CASE_CREATED")` | `EventCategory.CASE` |
| Maps known case event type | `fromCaseEventType("DEBT_REGISTERED")` | `EventCategory.DEBT_LIFECYCLE` |
| Maps known payment event type | `fromDebtEventType("PAYMENT_RECEIVED")` | `EventCategory.FINANCIAL` |
| Normalises alias pair | `normalizeEventType("DEBT_REGISTRATION")` | `"DEBT_REGISTERED"` |
| Normalises alias pair | `normalizeEventType("PAYMENT_APPLIED")` | `"PAYMENT_RECEIVED"` |
| Normalises alias pair | `normalizeEventType("WRITEOFF")` | `"DEBT_WRITEOFF"` |
| Unknown type returns default | `fromCaseEventType("UNKNOWN_TYPE")` | `EventCategory.CASE` (default) |
| Unknown type logs WARN | Any unmapped type | Log message at WARN level |

#### `TimelineDeduplicatorTest`

**Location:** `opendebt-common` test sources

| Test case | Setup | Expected |
|---|---|---|
| No duplicates — returns all | 3 entries with distinct dedupeKeys | Returns all 3 |
| Dedup: CASE wins when both amounts null | `DEBT_REGISTERED` (CASE, amount=null) + `DEBT_REGISTERED` (PAYMENT, amount=null) same key | Returns 1 entry, source=CASE |
| Dedup: PAYMENT wins when it has amount | `DEBT_REGISTERED` (CASE, amount=null) + `DEBT_REGISTRATION` (PAYMENT, amount=5000) same key | Returns 1 entry, source=PAYMENT, amount=5000 |
| Dedup: timestamp window | Same eventType+debtId, timestamps 30s apart (same minute bucket) | Deduplicated to 1 entry |
| No dedup: different debt | Same eventType, different debtId | 2 entries retained |
| No dedup: different minute | Same eventType+debtId, timestamps 61s apart (different minute bucket) | 2 entries retained |
| Null dedupeKey throws | Entry with `dedupeKey=null` | `IllegalArgumentException` |

#### `TimelineVisibilityPropertiesTest`

**Location:** Any portal test sources (test the binding)

| Test case | Setup | Expected |
|---|---|---|
| Caseworker gets all 7 | `roleCategories = {CASEWORKER: [all 7]}` | `getAllowedCategories("CASEWORKER").size() == 7` |
| Citizen gets 4 | `roleCategories = {CITIZEN: [FINANCIAL, DEBT_LIFECYCLE, CORRESPONDENCE, COLLECTION]}` | Returns exactly those 4 |
| Unknown role returns empty | `roleCategories = {}` | `getAllowedCategories("UNKNOWN").isEmpty()` |

---

### 6.2 Integration Tests — `TimelineController` (per portal, MockMvc)

Use `@WebMvcTest` with `@MockBean` for `CaseServiceClient`, `PaymentServiceClient`, `TimelineVisibilityProperties`, and `ExecutorService`.

Run equivalent tests in each portal's test module against the portal's controller (`CaseworkerTimelineController`, `CitizenTimelineController`, `CreditorTimelineController`).

#### Caseworker portal controller tests

| Scenario | Mock setup | Request | Expected |
|---|---|---|---|
| AC-A1: Merges sources | `getEvents` → 3 CaseEventDto; `getDebtEventsByCase` → 2 DebtEventDto | `GET /cases/{id}/tidslinje` | Model `entries` contains 5 entries (no overlap) |
| AC-A2: Deduplication | Both clients return `DEBT_REGISTERED`/`DEBT_REGISTRATION` for same debt within same minute | `GET /cases/{id}/tidslinje` | Model `entries` contains 1 entry for that event |
| AC-A4: Sorted descending | Entries with 3 different timestamps | `GET /cases/{id}/tidslinje` | Model `entries` in descending timestamp order |
| AC-B1: Caseworker sees all | Entries in all 7 categories | `GET /cases/{id}/tidslinje` with CASEWORKER auth | All 7 categories in `entries` |
| AC-E1: Page size default | 60 entries total | `GET /cases/{id}/tidslinje` | `entries.size() == 25`, `hasMore == true` |
| AC-E2: Page 2 appends | 60 entries, page 1 already displayed | `GET /cases/{id}/tidslinje/entries?page=2` | Returns entries 26–50; existing 25 unaffected |
| AC-E3: Load more hidden | 30 entries total, page 2 requested (size=25) | `GET /cases/{id}/tidslinje/entries?page=2` | `hasMore == false` |
| AC-D1: Category filter | 10 FINANCIAL + 10 CASE entries | `GET /cases/{id}/tidslinje?eventCategory=FINANCIAL` | `entries` all FINANCIAL; `totalCount == 10` |
| AC-D2: Date range | Entries from 2025-01-01 and 2026-03-01 | `GET /cases/{id}/tidslinje?fromDate=2026-01-01&toDate=2026-12-31` | Only entries in 2026 returned |
| AC-D3: Filter by specific debt | Mixed entries with different `debtId` values | `GET /cases/{id}/tidslinje?debtId={D-2001}` with role CASEWORKER | Only entries where `debtId = D-2001` returned |
| AC-E4: Filters preserved across pages | 60 FINANCIAL entries, page 1 loaded | `GET /cases/{id}/tidslinje/poster?page=2&eventCategory=FINANCIAL` with role CASEWORKER | Only FINANCIAL category entries on page 2 |
| AC-G1: Empty case | Both clients → `List.of()` | `GET /cases/{id}/tidslinje` | `entries.isEmpty()`; no warning |
| AC-G3: Payment service down | `getDebtEventsByCase` throws | `GET /cases/{id}/tidslinje` | Case events returned; `warnings` contains `"timeline.warning.partial"` |
| Security: rejected role | Request with `CREDITOR` auth | `GET /cases/{id}/tidslinje` | HTTP 403 |

---

### 6.3 Fragment Rendering Tests

**Purpose:** Verify the Thymeleaf fragment renders correctly with mock data (NFR — Testability).

**Location:** `opendebt-common` test sources using `SpringTemplateEngine` with `ClassPathTemplateResolver`.

| Test case | Model | Assertion |
|---|---|---|
| Entry renders required fields | Single `TimelineEntryDto` with all fields populated | Output contains formatted timestamp, category badge class, resolved title text (e.g., asserts `"Sag oprettet"` for a `CASE_CREATED` entry), description |
| Financial entry shows amount | `amount = 1234.56` | Output contains `"1.234,56 DKK"` |
| Debt link rendered | `debtId` non-null | Output contains `<a href="...">` with debt UUID |
| Amount omitted when null | `amount = null` | No DKK string in output |
| Empty state message | `entries = []` | Output contains `"Ingen hændelser registreret"` (da locale) |
| Load more shown | `hasMore = true` | Output contains button element |
| Load more hidden | `hasMore = false` | No button element in output |
| Warning banner shown | `warnings = ["timeline.warning.partial"]` | Output contains resolved warning text |
| No warning banner | `warnings = []` | Warning div absent from output |
| Active filter chips rendered | `TimelineFilterDto` with `eventCategories=[FINANCIAL]` | Chip element with text "Finansielle begivenheder" rendered; clear-all link present |
| No chips when no active filters | `TimelineFilterDto` with all fields null/empty | No chip elements rendered; clear-all link absent |
| OOB container present in entries fragment | `hasMore = false` on `/entries` response | Output contains `<div id="load-more-container" hx-swap-oob="true">` |

---

## Traceability Summary

| Requirement | Spec section |
|---|---|
| FR-1.1 TimelineEntryDto | §2.3 |
| FR-1.2 EventCategory (7 values) | §2.1 |
| FR-1.3 visibleTo (server-side only) | §2.7, §3.5 step 1 |
| FR-2.1 Fetch from both APIs | §3.3, §4.2, §4.3, §5.2, §5.3 |
| FR-2.2 Merge, deduplicate, sort | §2.6, §3.5 steps 4–9 |
| FR-2.3 Role-based filtering | §2.7, §3.4, §4.4, §5.4 |
| FR-2.4 Pagination (page, size) | §3.5 endpoints, §2.8 |
| FR-2.5 Optional filters | §2.4, §3.5 endpoints |
| FR-3.1–3.3 Role visibility | §3.4, §4.4, §5.4 |
| FR-3.4 Configurable visibility | §2.7 |
| FR-4.1 Reusable fragment | §2.8 |
| FR-4.2 Entry fields in UI | §2.8 structure |
| FR-4.3 Reverse chronological | §3.5 step 9 |
| FR-4.4 Category badges | §2.9 |
| FR-4.5 DKK formatting | §2.8 amount rendering |
| FR-4.6 Debt reference link | §2.8 debt link |
| FR-5.1–5.4 Filter bar | §2.4, §2.8 filter bar |
| FR-6.1 Initial 25 entries | §3.5 default size=25 |
| FR-6.2–6.3 Load more append | §2.8 load-more, §3.5 endpoint 2 |
| FR-6.4 Hide load more | §2.8 `hasMore` flag |
| FR-7.1–7.2 Replace Hændelseslog | §3.6, §4.6, §5.6 |
| FR-7.3 Posteringslog unchanged | Not touched; no spec needed |
| FR-8.1 Common fragment/DTO | §2 |
| FR-8.2 Portal BFF controllers | §3.5, §4.5, §5.5 |
| OI-1 SERVICE role | §1.1 |
| OI-2 DebtEventDto promotion | §1.2 |
| OI-3 EventType normalisation table | §2.5 (starter set; must be completed) |
| NFR Performance < 500ms | §3.1 executor, §3.5 parallel fetch |
| NFR WCAG 2.1 AA | §2.8 accessibility requirements |
| NFR i18n da/en | §2.10 |
| AC-G1 Empty timeline | §2.8 empty state, §6.2 |
| AC-G2 No CORRESPONDENCE entries | §2.5 defaults to empty; no placeholder rendered |
| AC-G3 Service unavailability | §3.5 step 3 exceptionally handler |
