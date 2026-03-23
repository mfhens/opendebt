# Petition 050: Unified Case Timeline UI

## Summary

Replace the existing Hændelseslog (event log) tab on the case detail page with a unified, chronological timeline that merges events from multiple backend services — case events, debt lifecycle transitions, and financial events — into a single visual event stream. The timeline component shall be implemented as a reusable Thymeleaf + HTMX fragment deployed across all three portals (caseworker, citizen, creditor) with role-based event visibility filtering.

## Context and Motivation

The caseworker portal currently displays case history across two separate tabs: **Hændelseslog** (case-level events from case-service) and **Posteringslog** (financial ledger entries from payment-service). This fragmented view forces caseworkers to mentally reconstruct the chronological sequence of a case by switching between tabs. Citizens and creditors have no event visibility at all.

A unified timeline provides a single chronological view of everything that has happened to a case, improving situational awareness for caseworkers and transparency for citizens and creditors. Each portal renders only the event types appropriate for the user's role, respecting data minimisation under GDPR.

### Domain Terms

| Danish | English | Definition |
|--------|---------|------------|
| Hændelseslog | Event log | Chronological record of case lifecycle events |
| Posteringslog | Transaction log | Financial ledger entries for a debt |
| Tidslinje | Timeline | Unified chronological view of all case events |
| Sagsbehandler | Caseworker | Internal user processing debt collection cases |
| Fordringshaver | Creditor | Public institution that owns the debt claim |
| Skyldner / Borger | Citizen / Debtor | Person owing the debt |
| Hændelsestype | Event type | Classification of a timeline entry |

## Functional Requirements

### FR-1: Unified Timeline Data Model

- **FR-1.1**: Define a portal-level `TimelineEntryDto` that normalises events from all sources into a common structure: `id`, `timestamp`, `eventCategory`, `eventType`, `title`, `description`, `amount` (nullable), `debtId` (nullable), `performedBy` (nullable), `metadata` (nullable).
- **FR-1.2**: `eventCategory` shall classify entries into: `CASE`, `DEBT_LIFECYCLE`, `FINANCIAL`, `COLLECTION`, `CORRESPONDENCE`, `OBJECTION`, `JOURNAL`.
- **FR-1.3**: The DTO shall include a `visibleTo` set derived from role-based visibility rules (not exposed to the frontend; used server-side for filtering).

### FR-2: BFF Aggregation Layer

- **FR-2.1**: Each portal's BFF controller shall fetch events from existing service APIs: `GET /api/v1/cases/{id}/events` (case-service) and `GET /api/v1/events/case/{caseId}` (payment-service).
- **FR-2.2**: The BFF shall merge, deduplicate, and sort events into a single descending-chronological list.
- **FR-2.3**: The BFF shall apply role-based filtering before rendering — only events matching the current user's role are included.
- **FR-2.4**: The BFF shall support pagination parameters: `page` (default 1) and `size` (default 25).
- **FR-2.5**: The BFF shall support optional query filters: `eventCategory`, `fromDate`, `toDate`, `debtId`.

### FR-3: Role-Based Event Visibility

- **FR-3.1**: **Caseworker / Supervisor / Admin** — all event categories visible.
- **FR-3.2**: **Citizen** — visible categories: `FINANCIAL` (payment received, refund), `DEBT_LIFECYCLE` (state changes), `CORRESPONDENCE` (letters sent), `COLLECTION` (collection measures initiated). Internal assignment, journal entries, and notes are hidden.
- **FR-3.3**: **Creditor** — visible categories: `FINANCIAL` (debt registration, payment received), `DEBT_LIFECYCLE` (state changes), `COLLECTION` (collection measures). Internal assignment, journal entries, objection details, and notes are hidden.
- **FR-3.4**: The visibility matrix shall be configurable via a central mapping (not hard-coded in templates).

### FR-4: Timeline UI Component

- **FR-4.1**: Implement the timeline as a Thymeleaf fragment (`fragments/timeline.html`) reusable across all three portals.
- **FR-4.2**: Each timeline entry shall display: timestamp, event type icon/badge, title, description, and amount (if applicable).
- **FR-4.3**: Entries shall be rendered in reverse chronological order (newest first).
- **FR-4.4**: Each entry shall be visually distinguished by `eventCategory` using colour-coded badges or icons consistent with the SKAT design system.
- **FR-4.5**: Financial entries shall display the amount with correct formatting (Danish locale, DKK).
- **FR-4.6**: Entries linked to a specific debt shall display the debt reference and link to the debt detail page.

### FR-5: Filtering

- **FR-5.1**: Provide a filter bar above the timeline with: event category dropdown (multi-select), date range picker (from/to), and debt selector (populated from debts linked to the case).
- **FR-5.2**: Filters shall apply via HTMX without full page reload, replacing the timeline fragment content.
- **FR-5.3**: Active filters shall be displayed as removable chips/tags.
- **FR-5.4**: A "clear all filters" action shall reset to the default unfiltered view.

### FR-6: Lazy Loading / Pagination

- **FR-6.1**: Initially load the 25 most recent timeline entries.
- **FR-6.2**: Display a "Load more" button at the bottom of the timeline when older entries exist.
- **FR-6.3**: Clicking "Load more" shall fetch the next page of entries via HTMX and append them to the existing timeline (no full replacement).
- **FR-6.4**: When no more entries exist, the "Load more" button shall be hidden.

### FR-7: Replace Hændelseslog Tab

- **FR-7.1**: On the case detail page, replace the existing Hændelseslog tab content with the unified timeline component.
- **FR-7.2**: The tab label shall change from "Hændelseslog" to "Tidslinje" (Danish) in all portals.
- **FR-7.3**: The existing Posteringslog tab shall remain unchanged; the timeline is a complementary view, not a replacement for the detailed financial ledger.

### FR-8: Common Component Reuse

- **FR-8.1**: The timeline Thymeleaf fragment, CSS, and `TimelineEntryDto` shall reside in `opendebt-common` so all portals share the same component code.
- **FR-8.2**: Each portal shall provide its own BFF controller that wires service clients and role-visibility rules to the common fragment.

## Non-Functional Requirements

| NFR | Specification |
|-----|---------------|
| Performance | Timeline page load (first 25 entries) shall complete in < 500 ms (server-side, excluding network) |
| Performance | "Load more" incremental fetch shall complete in < 300 ms |
| Accessibility | Timeline shall meet WCAG 2.1 AA, including screen-reader-friendly semantic markup |
| Internationalisation | All labels shall use Spring `messages.properties`; support `da` and `en` locales |
| Testability | Timeline fragment shall be testable in isolation with mock data |

## Constraints and Assumptions

- The timeline aggregates events at the **BFF layer** (portal controllers), not via a new backend aggregation microservice.
- Event data is read-only; the timeline does not create or modify events.
- The letter-service does not currently emit events; `CORRESPONDENCE` entries will only appear once letter-service event tracking is implemented (future petition). The timeline shall gracefully handle zero entries in this category.
- The existing `CaseEventDto` and `DebtEventDto` in `opendebt-common` are the source DTOs; the new `TimelineEntryDto` maps from these.
- Claim lifecycle events from debt-service are currently internal-only (no REST endpoint). They will need to be exposed or proxied via case-service. This is an implementation dependency.

## Dependencies

| Dependency | Status | Impact |
|------------|--------|--------|
| Petition 049 (Case Handler Assignment) | In progress | Case assignment events feed into the timeline |
| Petition 047 (Configuration Administration UI) | Implemented | Visibility matrix could use configuration service |
| Petition 048 (RBAC) | Implemented | Role-based filtering depends on RBAC infrastructure |
| Case-service events API | Implemented | `GET /api/v1/cases/{id}/events` exists |
| Payment-service events API | Implemented | `GET /api/v1/events/case/{caseId}` exists |
| Debt-service lifecycle API | Not implemented | `ClaimLifecycleEvent` exists but has no REST endpoint — needs exposure (TB-024) |

## Out of Scope

- Letter-service event tracking (no events exist yet; separate petition)
- Wage garnishment service events (future petition)
- Offsetting service events (future petition)
- Real-time event streaming / WebSocket push (events are fetched on page load and explicit user action)
- Modification or replacement of the Posteringslog tab (kept as-is for detailed financial drill-down)
- Event aggregation microservice (BFF aggregation is sufficient for current scale)
- Debt-detail-level timeline (this petition covers case-level timeline only; debt-detail may be a follow-up)
