# Petition 050 Outcome Contract: Unified Case Timeline UI

## Contract Header

| Field | Value |
|-------|-------|
| Petition ID | 050 |
| Title | Unified Case Timeline UI |
| Type | UI Component / Cross-Portal Feature |
| Scope | Reusable timeline fragment replacing Hændelseslog tab across all portals with aggregated, role-filtered events |
| Status | Not Started |
| Created | 2026-03-23 |

## Acceptance Criteria

### Category A: Data Aggregation

#### AC-A1: BFF merges case events and financial events
**Given** a case with ID `{caseId}` exists with case events and linked debts with financial events
**When** the BFF timeline endpoint is called for `{caseId}`
**Then** the response contains entries from both case-service events and payment-service events
**And** all entries are sorted in descending chronological order

#### AC-A2: Deduplication of overlapping events
**Given** both case-service and payment-service report an event for the same underlying action (e.g., debt registration)
**When** the BFF merges events
**Then** duplicate entries for the same logical action are not displayed

#### AC-A3: Normalised timeline entry structure
**Given** events from different services have different field structures
**When** the BFF maps them to `TimelineEntryDto`
**Then** each entry has: `id`, `timestamp`, `eventCategory`, `eventType`, `title`, `description`
**And** `amount` is populated for financial events and null for non-financial events
**And** `debtId` is populated for debt-specific events and null for case-level events

### Category B: Role-Based Visibility

#### AC-B1: Caseworker sees all event categories
**Given** the current user has the role `CASEWORKER`
**When** the timeline loads for a case
**Then** entries from all categories are displayed: `CASE`, `DEBT_LIFECYCLE`, `FINANCIAL`, `COLLECTION`, `CORRESPONDENCE`, `OBJECTION`, `JOURNAL`

#### AC-B2: Citizen sees only citizen-appropriate events
**Given** the current user has the role `CITIZEN`
**When** the timeline loads for a case
**Then** only entries in categories `FINANCIAL`, `DEBT_LIFECYCLE`, `CORRESPONDENCE`, `COLLECTION` are displayed
**And** no entries from categories `CASE` (internal), `JOURNAL`, or `OBJECTION` are displayed

#### AC-B3: Creditor sees only creditor-appropriate events
**Given** the current user has the role `CREDITOR`
**When** the timeline loads for a case
**Then** only entries in categories `FINANCIAL`, `DEBT_LIFECYCLE`, `COLLECTION` are displayed
**And** no entries from categories `CASE` (internal), `JOURNAL`, `OBJECTION`, or `CORRESPONDENCE` are displayed

#### AC-B4: Visibility rules are not hard-coded in templates
**Given** the role-to-category visibility mapping
**When** an administrator inspects the configuration
**Then** the mapping is defined in a central configuration source (properties file or configuration service)
**And** the Thymeleaf templates do not contain role-checking logic for event filtering

### Category C: Timeline UI Rendering

#### AC-C1: Timeline replaces Hændelseslog tab
**Given** a user navigates to the case detail page
**When** the page loads
**Then** the tab previously labelled "Hændelseslog" is labelled "Tidslinje"
**And** its content is the unified timeline component

#### AC-C2: Entries display required fields
**Given** the timeline is rendered with entries
**Then** each entry displays: formatted timestamp, event category badge, title, and description
**And** financial entries additionally display the amount in Danish locale (e.g., "1.234,56 DKK")
**And** debt-linked entries display the debt reference as a link to the debt detail page

#### AC-C3: Entries are visually categorised
**Given** the timeline contains entries from multiple categories
**Then** each category has a distinct colour-coded badge or icon
**And** the visual coding is consistent with the SKAT design system

#### AC-C4: Reverse chronological order
**Given** the timeline is loaded
**Then** the most recent event appears at the top
**And** scrolling down reveals progressively older events

### Category D: Filtering

#### AC-D1: Filter by event category
**Given** the timeline is displayed with events from multiple categories
**When** the user selects one or more categories from the category filter
**Then** only entries matching the selected categories are displayed
**And** the filter is applied via HTMX without full page reload

#### AC-D2: Filter by date range
**Given** the timeline is displayed
**When** the user sets a "from" date and/or "to" date
**Then** only entries within the specified date range are displayed

#### AC-D3: Filter by debt
**Given** the case has multiple linked debts
**When** the user selects a specific debt from the debt filter
**Then** only entries related to that debt (or case-level entries) are displayed

#### AC-D4: Active filters shown as chips
**Given** the user has applied one or more filters
**Then** each active filter is displayed as a removable chip/tag above the timeline
**And** clicking the remove icon on a chip removes that filter and refreshes the timeline

#### AC-D5: Clear all filters
**Given** multiple filters are active
**When** the user clicks "Clear all filters"
**Then** all filters are removed and the full unfiltered timeline is displayed

### Category E: Lazy Loading / Pagination

#### AC-E1: Initial load of 25 entries
**Given** a case with more than 25 timeline events
**When** the timeline tab is opened
**Then** the 25 most recent entries are displayed
**And** a "Load more" button is visible below the entries

#### AC-E2: Load more appends entries
**Given** 25 entries are displayed and older entries exist
**When** the user clicks "Load more"
**Then** the next 25 entries are fetched via HTMX and appended below the existing entries
**And** the previously displayed entries remain in place

#### AC-E3: Load more hidden when exhausted
**Given** all timeline entries have been loaded
**Then** the "Load more" button is not displayed

#### AC-E4: Filters apply to paginated results
**Given** a category filter is active
**When** the user clicks "Load more"
**Then** the next page of results respects the active filters

### Category F: Cross-Portal Consistency

#### AC-F1: Common fragment shared across portals
**Given** the timeline fragment resides in `opendebt-common`
**When** the caseworker, citizen, and creditor portals render the timeline
**Then** all three use the same Thymeleaf fragment and CSS
**And** the only difference is the event set (determined by role-based visibility)

#### AC-F2: Caseworker portal timeline functional
**Given** a caseworker navigates to a case detail page
**When** the Tidslinje tab is selected
**Then** the unified timeline renders with all event categories

#### AC-F3: Citizen portal timeline functional
**Given** a citizen navigates to their case detail page
**When** the Tidslinje tab is selected
**Then** the unified timeline renders with citizen-visible event categories only

#### AC-F4: Creditor portal timeline functional
**Given** a creditor navigates to a case detail page
**When** the Tidslinje tab is selected
**Then** the unified timeline renders with creditor-visible event categories only

### Category G: Graceful Degradation

#### AC-G1: Empty timeline
**Given** a case with zero events
**When** the timeline tab is opened
**Then** a message "Ingen hændelser registreret" (No events registered) is displayed
**And** no error is shown

#### AC-G2: Missing correspondence events
**Given** the letter-service does not yet emit events
**When** the timeline loads
**Then** the `CORRESPONDENCE` category is absent from the timeline
**And** no error or empty category placeholder is shown

#### AC-G3: Service unavailability
**Given** one of the backend services (case-service or payment-service) is temporarily unavailable
**When** the timeline loads
**Then** events from the available service are displayed
**And** a non-blocking warning indicates partial data: "Nogle hændelser kunne ikke hentes" (Some events could not be retrieved)

## Non-Functional Acceptance Criteria

| NFR | Acceptance Criterion |
|-----|---------------------|
| Performance | First 25 entries render in < 500 ms server-side processing time |
| Performance | "Load more" request completes in < 300 ms server-side |
| Accessibility | Timeline passes axe-core automated WCAG 2.1 AA checks |
| Accessibility | Screen readers announce each timeline entry with timestamp and title |
| i18n | All user-facing labels available in `da` and `en` via `messages.properties` |
| Testability | Timeline fragment renders correctly with mock data in an isolated Thymeleaf test |

## Implementation Dependencies

### Prerequisite Features
- Petition 048 (RBAC) — **Implemented** (role-based access control infrastructure)
- Petition 049 (Case Handler Assignment) — **In Progress** (assignment events feed timeline)

### Integration Points
1. **Case Service** — `GET /api/v1/cases/{id}/events` returns `List<CaseEventDto>`
2. **Payment Service** — `GET /api/v1/events/case/{caseId}` returns `List<DebtEventDto>`
3. **Debt Service** — `ClaimLifecycleEvent` entity exists but lacks REST endpoint (implementation task: expose or proxy)

## Risk and Mitigation

| Risk | Mitigation |
|------|------------|
| Debt-service lifecycle events have no REST endpoint | Expose via new endpoint or proxy through case-service; timeline degrades gracefully without them |
| Cross-service latency impacts timeline load time | Fetch events in parallel from both services; apply timeout with partial rendering |
| Event volume for long-running cases | Pagination (25 per page) limits initial payload; indexed queries on service side |
| Role-visibility matrix changes frequently | Externalise to configuration rather than hard-coding |

## Verification and Testing Strategy

### Unit Tests
- `TimelineEntryDto` mapping from `CaseEventDto` and `DebtEventDto`
- Role-based visibility filtering logic
- Merge and sort algorithm correctness (chronological ordering, deduplication)

### Integration Tests
- BFF controller returns correctly merged and filtered timeline for each role
- Pagination parameters produce correct page boundaries
- Filter combinations (category + date range + debt) work together

### E2E Tests
- Caseworker portal: timeline tab renders, filters work, load more works
- Citizen portal: only citizen-visible events shown
- Creditor portal: only creditor-visible events shown
- Empty case: "no events" message displayed

## Sign-Off

| Role | Name | Date | Status |
|------|------|------|--------|
| Product Owner | — | — | Pending |
| Technical Lead | — | — | Pending |
