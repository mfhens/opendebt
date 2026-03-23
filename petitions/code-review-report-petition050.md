# Code Review Report — Petition 050: Unified Case Timeline UI

**Petition**: 050 — Unified Case Timeline UI  
**Language**: Java 21 / Spring Boot 3.3 / Thymeleaf + HTMX  
**Review Date**: 2026-03-23 18:43  
**Overall Decision**: ❌ **REJECTED**  
**Quality Score**: **48 / 100**

---

## Summary

| Metric | Count |
|--------|-------|
| Total Items Reviewed | 18 |
| KEEP | 9 |
| DISCARD / FIX | 9 |
| ESCALATE | 0 |
| Critical Bugs | 5 |
| Significant Bugs | 4 |

Six of the nine acceptance criteria categories (AC-A2, AC-C1, AC-D4, AC-E4, NFR Accessibility, and cross-portal consistency) are broken by the defects documented below. The implementation is not shippable.

---

## Technical Validation

### Compilation / Type Check Status

The codebase compiles. `opendebt-payment-service` has a local `dk.ufst.opendebt.payment.dto.DebtEventDto` class that should have been removed by OI-2 and replaced by the promoted common class. Both coexist, but since `LedgerController` imports from the local package and the portal clients import from common, there is no compile failure — only a latent structural violation of OI-2 that will surface as a JSON deserialization divergence if the two classes ever drift.

### Test Discovery Status

Tests are not run as part of this review pass. Per `project.yaml`, run `mvn -q test` to execute the full suite. The bugs documented below (especially the deduplication key mismatch and NPE in sorting) are expected to produce test failures if unit tests for `TimelineDeduplicator` and the BFF pipeline exist.

---

## Detailed Review

### ✅ KEEP Items

| # | File / Class | Item | Mapped Requirement |
|---|---|---|---|
| K1 | `EventCategory.java` | 7-value enum in declaration order | FR-1.2, spec §2.1 |
| K2 | `TimelineSource.java` | Internal-only; correct values | Spec §2.2 |
| K3 | `TimelineVisibilityProperties.java` | ConfigurationProperties; no @Component; `getAllowedCategories` safe-default | FR-3.4, AC-B4, spec §2.7 |
| K4 | `EventCategoryMapper.java` | 16-entry normalisation table matches spec; WARN logging on unknown types; correct dual-path lookup | Spec §2.5 |
| K5 | `TimelineDeduplicator.java` | LinkedHashMap insertion-order algorithm; null-key guard; PAYMENT-wins logic | AC-A2, spec §2.6 |
| K6 | `CaseworkerTimelineController` — security | `@PreAuthorize("hasAnyRole('CASEWORKER','SUPERVISOR','ADMIN')")` on both endpoints | FR-3.1, spec §3.5 |
| K7 | `CitizenTimelineController` — security | `@PreAuthorize("hasRole('CITIZEN')")` on both endpoints | AC-B2, spec §4.5 |
| K8 | `CreditorTimelineController` — security | `@PreAuthorize("hasRole('CREDITOR')")` on both endpoints | AC-B3, spec §5.5 |
| K9 | `LedgerController.getEventsByCase` | `@PreAuthorize` now includes `hasRole('SERVICE')` | OI-1, spec §1.1 |
| K10 | All portal `CaseServiceClient`/`PaymentServiceClient` | `@CircuitBreaker` + `@Retry` on `getEvents` and `getDebtEventsByCase`; correct fallback returning `List.of()` | AC-G3, spec §3.2–3.3, §4.2–4.3, §5.2–5.3 |
| K11 | `messages_da.properties` / `messages_en_GB.properties` | All 26 required keys present and correctly translated | FR-8.1, spec §2.10 |
| K12 | `timeline.css` | All 7 category badge modifier classes with distinct background + white text (dual-channel: colour AND text label) | AC-C3, spec §2.9, WCAG 2.1 AA §1.4.1 |

---

### ❌ DISCARD / FIX Items

---

#### BUG-1 [CRITICAL] — Cross-Service Deduplication Key Is Structurally Incompatible

**File**: `opendebt-common/src/main/java/dk/ufst/opendebt/common/timeline/TimelineEntryMapper.java`  
**Lines**: 22–24 (`fromCaseEvent`), 66–71 (`fromDebtEvent`)

**Problem**: The `dedupeKey` format is not uniform across the two source types:

- **Case events** (`fromCaseEvent`): `normalize(eventType) + "|CASE|" + timestamp.truncatedTo(MINUTES)`
  - For `DEBT_REGISTERED` from case-service on 2024-01-15 at 14:30, the key is:  
    `DEBT_REGISTERED|CASE|2024-01-15T14:30`
- **Payment events** (`fromDebtEvent`): `normalize(eventType) + "|" + debtId.toString() + "|" + timestamp.truncatedTo(MINUTES)`
  - For `DEBT_REGISTRATION` (normalised → `DEBT_REGISTERED`) from payment-service, same time:  
    `DEBT_REGISTERED|550e8400-e29b-41d4-a716-446655440000|2024-01-15T14:30`

These two keys are **never equal**. `TimelineDeduplicator.deduplicate()` will never match them. Both entries will appear in the output — the primary accepted scenario in AC-A2 ("debt registration appears once with the payment-source amount") is broken in all three portals.

**Spec says** (§2.3): The dedupeKey for `CaseEventDto` should also incorporate `debtId` from the case event (or from the event's context), not unconditionally use the string `"CASE"`. Alternatively, the spec's intended design is that cross-service dedup keys must agree — the current implementation of `fromCaseEvent` always embeds `"CASE"` in the segment that should hold the debt UUID.

**Fix required**: Either (a) populate `debtId` on case events when the event is debt-related (e.g., `DEBT_REGISTERED`) and use it in the key, or (b) use a format that both sources can produce identically — such as keying only on `normalize(eventType) + "|" + timestamp.truncatedTo(MINUTES)` for event types known to be 1:1 across services. This requires a design decision but the current state fails the deduplication guarantee.

**Violated**: AC-A2, spec §2.3, §2.6 acceptance.

---

#### BUG-2 [CRITICAL] — `aria-label` Exposes Raw i18n Key String to Screen Readers

**File**: `opendebt-common/src/main/resources/templates/fragments/timeline.html`  
**Lines**: 104, 160

**Current code** (both `timeline-panel` and `timeline-entries` fragments):
```html
th:aria-label="${#temporals.format(entry.timestamp,'dd-MM-yyyy HH:mm')} + ' - ' + ${entry.title}"
```

**Problem**: `entry.title` is an i18n message key such as `timeline.event.title.CASE_CREATED` — not a human-readable string. Screen readers will announce:
> "2024-01-15 14:30 - timeline.event.title.CASE_CREATED"

**Spec requires** (§2.8 Accessibility requirements):
```html
th:aria-label="${#temporals.format(entry.timestamp,'dd-MM-yyyy HH:mm')} + ' - ' + ${#messages.msg(entry.title)}"
```

**Fix required**: Replace `${entry.title}` with `${#messages.msg(entry.title)}` on both lines 104 and 160.

**Violated**: NFR Accessibility (WCAG 2.1 AA 2.4.6 Headings and Labels; 4.1.3 Status Messages), spec §2.8.

---

#### BUG-3 [CRITICAL] — Shared Template Uses `${caseId}` Which Is Null in the Creditor Portal

**File**: `opendebt-common/src/main/resources/templates/fragments/timeline.html`  
**Lines**: 24, 80, 83, 84, 90, 121, 133, 177, 187

**File**: `opendebt-creditor-portal/.../CreditorTimelineController.java`  
**Line**: 183

**Problem**: `CreditorTimelineController.buildTimeline()` adds `claimId` to the model (line 183, with a comment explicitly saying "do NOT add caseId"). The shared `timeline.html` fragment references `${caseId}` in:

| Location | Effect |
|---|---|
| Filter form `th:action` (line 24) | Form posts to `/cases/null/tidslinje` — 404 |
| Chip remove links (lines 80, 83, 84, 90) | All link to `/cases/null/tidslinje` — 404 |
| "Load more" button `hx-get` (lines 133, 187) | Fetches from `/cases/null/tidslinje/entries` — 404 |
| Debt detail `th:href` (lines 121, 177) | Links to `/cases/null/debts/{debtId}` — 404 |

The creditor portal also uses the path `/fordring/{id}/tidslinje/poster` for the load-more endpoint (spec §5.5), but the template hard-codes `/cases/{id}/tidslinje/entries`. **All interactive elements in the creditor portal are dead on arrival.**

**Fix required**: The template must be parameterised to support different URL roots, either by accepting a `baseTimelinePath` model attribute or by maintaining separate creditor-specific URL segments. All uses of `${caseId}` in URL templates must be guarded or made portal-agnostic.

**Violated**: AC-F4, AC-D1 through AC-D5, AC-E2, spec §5.5.

---

#### BUG-4 [CRITICAL] — Individual Filter Chip Removal Clears ALL Active Filters

**File**: `opendebt-common/src/main/resources/templates/fragments/timeline.html`  
**Lines**: 78–92

**Current code** (chip remove links, all three variants — category chip, fromDate chip, toDate chip):
```html
<a th:href="@{/cases/{id}/tidslinje(id=${caseId})}" class="skat-chip__remove">&#xD7;</a>
```

**Problem**: Every chip's remove link navigates to the base URL with **no query parameters**. Clicking the `×` on the `FINANCIAL` category chip when `fromDate`, `toDate`, and `debtId` are also active will clear ALL filters, not just `FINANCIAL`.

**Spec requires** (AC-D4): "clicking the remove icon on a chip removes that filter and refreshes the timeline." The remove link must carry all other currently active filter parameters except the one being removed.

**Fix required**: Each chip's remove link must reconstruct the URL carrying all active filters **minus** the one it represents. For a category chip, all other categories must be re-appended. For the `fromDate` chip, `fromDate` is omitted but `toDate`, `debtId`, and `eventCategory` are preserved.

**Violated**: AC-D4, spec §2.8 filter chip semantics.

---

#### BUG-5 [CRITICAL] — "Load More" Button Does Not Carry Active Filter Parameters

**File**: `opendebt-common/src/main/resources/templates/fragments/timeline.html`  
**Lines**: 133–134, 187–188

**Current code** (`timeline-panel` and `timeline-entries` OOB button):
```html
th:attr="hx-get=@{/cases/{id}/tidslinje/entries(id=${caseId},page=${page + 1},size=${size})}"
```

**Problem**: Only `page` and `size` are forwarded. The active filter state (`eventCategory`, `fromDate`, `toDate`, `debtId`) is not included. When a user is viewing a category-filtered view and clicks "Load more", the next-page request fetches **unfiltered** results. Filter state is silently discarded across paginations.

**Spec requires** (AC-E4, spec §2.8):
```
hx-get=@{/cases/{id}/tidslinje/entries(
    id=${caseId}, page=${page + 1}, size=${size},
    eventCategory=${filters.eventCategories},
    fromDate=${filters.fromDate}, toDate=${filters.toDate},
    debtId=${filters.debtId})}
```

**Fix required**: Add all four filter parameters to both the `timeline-panel` and `timeline-entries` OOB load-more button `hx-get` attribute.

**Violated**: AC-E4, spec §2.8.

---

#### BUG-6 [SIGNIFICANT] — Submit Button Carries Wrong i18n Key

**File**: `opendebt-common/src/main/resources/templates/fragments/timeline.html`  
**Line**: 73

**Current code**:
```html
<button type="submit" class="skat-btn skat-btn--primary"
        th:text="#{timeline.filter.category}">Søg</button>
```

**Problem**: `timeline.filter.category` resolves to "Kategori" (da) / "Category" (en). The fallback text "Søg" in the HTML is never displayed (Thymeleaf always renders the `th:text`). The submit button is labelled "Kategori" in production — a misleading, incorrect label.

No `timeline.filter.submit` (or equivalent) key was added to either properties file.

**Fix required**: Add `timeline.filter.submit=Filtrer` (da) and `timeline.filter.submit=Apply filter` (en) to the message bundles and use that key on the button.

**Violated**: FR-4 (UI quality), NFR i18n, spec §2.8 and §2.10.

---

#### BUG-7 [SIGNIFICANT] — NullPointerException in Sort Step When `timestamp` Is Null

**Files**: `CaseworkerTimelineController.java` line 182, `CitizenTimelineController.java` line 158, `CreditorTimelineController.java` line 164

**Current code** (same in all three controllers):
```java
.sorted(Comparator.comparing(TimelineEntryDto::getTimestamp).reversed())
```

**Problem**: `TimelineEntryMapper.fromDebtEvent()` sets `timestamp` to null when both `event.getCreatedAt()` and `event.getEffectiveDate()` are null (lines 51–53 of `TimelineEntryMapper.java`). `Comparator.comparing(...)` on a null value throws `NullPointerException` at runtime.

The spec states `timestamp` is non-null (table, §2.3), but imposes no guard at the sort site and the mapper path can produce null. A corrupted or incomplete `DebtEventDto` would crash the BFF controller for the entire case view, returning a 500 to the user with no graceful degradation.

**Fix required**: Either (a) add `Comparator.nullsLast(...)` around the comparator, or (b) throw a mapped exception in the mapper when timestamp is null, or (c) add an assertion/null-filter before the sort step. Option (c) is safest: filter out entries with null timestamps before sorting, with a WARN log.

**Violated**: NFR resilience (AC-G3), correctness.

---

#### BUG-8 [SIGNIFICANT] — `CaseDetailController` Still Fetches Old Events (FR-7.1 Incomplete)

**File**: `opendebt-caseworker-portal/.../CaseDetailController.java`  
**Lines**: 87, 122–129

**Problem**: `CaseDetailController.caseDetail()` still calls `loadEvents(caseId, model)` on every page load, which invokes `caseServiceClient.getEvents(caseId)` and adds the result to the model as `events`. Spec §3.6 explicitly mandates:

> "Remove from `CaseDetailController`: The existing model attribute `events` (the `List<CaseEventDto>` currently fetched...) is no longer needed by the detail page. Remove the `CaseServiceClient.getEvents()` call and `model.addAttribute("events", ...)` from `CaseDetailController`."

**Impact**: Every case detail page load now makes a redundant upstream call to case-service for events that no longer serve the page (since the timeline is loaded lazily on tab click via HTMX). This doubles the load on case-service for every page view and wastes BFF resources. If the events are still being rendered by the old `cases/detail.html` template (which cannot be confirmed from this review since the template was not listed as a changed file), it means FR-7.1 is **not implemented** — the tab content is unchanged.

**Fix required**: Remove `loadEvents(caseId, model)` and its helper method from `CaseDetailController`. Confirm `cases/detail.html` tab button and panel have been updated per spec §3.6.

**Violated**: FR-7.1, AC-C1, spec §3.6 (remove stale event load); performance NFR.

---

#### BUG-9 [SIGNIFICANT] — OI-2 Not Fully Implemented: Payment-Service Retains Local `DebtEventDto`

**File**: `opendebt-payment-service/src/main/java/dk/ufst/opendebt/payment/dto/DebtEventDto.java`

**Problem**: OI-2 (spec §1.2) requires the local `DebtEventDto` to be **removed** from payment-service and replaced with an import from `opendebt-common`. Instead, both coexist:
- `dk.ufst.opendebt.payment.dto.DebtEventDto` — still present, still used by `LedgerController`
- `dk.ufst.opendebt.common.dto.DebtEventDto` — added to common, used by portal clients

As long as both classes have identical fields, JSON serialisation/deserialisation works. But:
1. OI-2 explicitly requires removal of the local class — this is a stated acceptance criterion.
2. If fields diverge in a future change (e.g., `createdAt` timezone handling), the portal's `TimelineEntryMapper` will silently receive stale data.
3. The `LedgerController` (which serialises to JSON for API responses) still uses the payment-local class. Portal clients deserialise using the common class. Any discrepancy will cause Jackson `UnrecognizedPropertyException` or silently drop fields.

**Fix required**: Replace `import dk.ufst.opendebt.payment.dto.DebtEventDto` with `import dk.ufst.opendebt.common.dto.DebtEventDto` in `LedgerController.java` and delete the local file.

**Violated**: OI-2 acceptance criterion, spec §1.2.

---

## Coding Principles Violations

| # | File | Line | Principle | Description |
|---|---|---|---|---|
| V1 | `timeline.html` | 104, 160 | Correctness | Unresolved i18n key in `aria-label` (BUG-2) |
| V2 | `timeline.html` | 24, 80, 83, 90, 121, 133, 177, 187 | DRY / Correctness | URL fragments use `${caseId}` which is portal-specific; should be abstracted |
| V3 | `timeline.html` | 73 | Correctness | Wrong message key on submit button (BUG-6) |
| V4 | `TimelineEntryMapper.java` | 22–24 | Correctness | `dedupeKey` format for case events incompatible with payment event format (BUG-1) |
| V5 | All three BFF controllers | 182 / 158 / 164 | Defensive programming | `Comparator.comparing(timestamp)` without null guard (BUG-7) |
| V6 | `CaseDetailController.java` | 87, 122–129 | KISS / Performance | Dead model attribute `events` still fetched and populated (BUG-8) |
| V7 | `EventCategoryMapper.java` | 47 | DRY | `PAYMENT_APPLIED` appears in both NORMALISE (maps to `PAYMENT_RECEIVED`) and in `CASE_CATEGORIES` (direct entry to `FINANCIAL`). The direct entry is dead code; after normalisation, the lookup will always match via `PAYMENT_RECEIVED`. |
| V8 | `timeline.html` | 133, 187 | Correctness | Active filter state not propagated on load-more (BUG-5) |

---

## Minimality Analysis

All reviewed classes and methods are mapped to petition requirements. No speculative or unmapped functionality was introduced. The `EventCategoryMapper.PAYMENT_APPLIED` direct entry in `CASE_CATEGORIES` (violation V7) is a minor duplication — not speculative, just unreachable dead code that should be removed for clarity.

---

## Quality Score Breakdown

| Category | Score | Notes |
|---|---|---|
| Compilation / Type Check | 18 / 20 | OI-2 incomplete: local DebtEventDto not removed (–2) |
| Test Discovery | 20 / 20 | Compile compiles; test files not listed in scope |
| Minimality | 18 / 20 | PAYMENT_APPLIED dead entry in CASE_CATEGORIES (–2) |
| Coding Principles | 12 / 30 | 8 violations × –3 = –24 (capped, see violations table) |
| Test Quality | 10 / 10 | N/A — tests not in scope of this review pass |
| **Deductions** | –30 | 5 critical bugs × –5 = –25; 4 significant bugs × –5 = –20; capped at –30 |
| **Final Score** | **48 / 100** | |

---

## Technical Debt Identified

| File | Type | Description | Suggested TB-ID |
|---|---|---|---|
| `EventCategoryMapper.java` line 47 | `AIDEV-REFACTOR` | `PAYMENT_APPLIED` entry in `CASE_CATEGORIES` is dead code after normalisation — remove or document as explicit alias | TB-025 |
| All three BFF controllers | `AIDEV-PERF` | In-flight futures are NOT cancelled after the 4-second `allOf` timeout. Threads remain occupied in `bffFetchExecutor` until the upstream call completes or WebClient timeout fires. Add explicit cancellation or ensure WebClient read timeout is < 4s. | TB-026 |
| `CaseDetailController.java` | `AIDEV-TODO` | `loadEvents()` method still present; remove after confirming `cases/detail.html` template is updated | TB-027 |

---

## Recommendations — Ordered by Priority

1. **[P1] Fix dedupeKey format** (`TimelineEntryMapper.fromCaseEvent`) — Either extract the debtId from case events where semantically appropriate (DEBT_REGISTERED, WRITEOFF, PAYMENT_APPLIED), or adopt an event-type-scoped key that both sources can match. This requires a confirmed design decision before coding. BUG-1.

2. **[P1] Fix `aria-label` in `timeline.html`** — Replace `${entry.title}` with `${#messages.msg(entry.title)}` on lines 104 and 160. One-line fix. BUG-2.

3. **[P1] Fix creditor portal URL incompatibility** — The shared template must accept a model attribute (e.g., `timelineBaseUrl`) injected by each portal's controller, replacing all hard-coded `/cases/{id}/tidslinje` references. The creditor portal passes `claimId` and uses `/fordring/{id}/tidslinje`; the template must not assume the caseworker/citizen URL structure. BUG-3.

4. **[P1] Fix filter chip individual removal** — Each chip's remove link must reconstruct the URL with all remaining active filters. BUG-4.

5. **[P1] Fix load-more button filter pass-through** — Add `eventCategory`, `fromDate`, `toDate`, `debtId` to the `hx-get` attribute on both load-more buttons. BUG-5.

6. **[P2] Add `timeline.filter.submit` i18n key and fix the submit button** — Add key to both property files; correct `th:text` on the `<button>`. BUG-6.

7. **[P2] Add null-guard in sort step** — All three controllers: wrap comparator with `Comparator.nullsLast(...)` or pre-filter null-timestamp entries. BUG-7.

8. **[P2] Remove `loadEvents()` from `CaseDetailController`** — And confirm `cases/detail.html` has been updated with the new Tidslinje tab markup per spec §3.6. BUG-8.

9. **[P2] Complete OI-2** — Delete `opendebt-payment-service/.../payment/dto/DebtEventDto.java`; update `LedgerController.java` to import from `opendebt-common`. BUG-9.

10. **[P3] Remove dead `PAYMENT_APPLIED` entry from `CASE_CATEGORIES`** in `EventCategoryMapper`. Technical debt V7.
