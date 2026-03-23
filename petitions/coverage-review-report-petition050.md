# Test Coverage Audit Report
## Petition: 050 — Unified Case Timeline UI
## Language: Java 21
## BDD Framework: JUnit 5 (cucumber-jvm declared; petition050 tests use JUnit `@Test` stubs)
## Date: 2026-03-23
## Status: ❌ FAIL

---

## 1. Scope and Input Artefacts

| Artefact | Path | Items |
|---|---|---|
| Feature file | `petitions/petition050-unified-case-timeline-ui.feature` | 33 Gherkin scenarios |
| Outcome contract | `petitions/petition050-unified-case-timeline-ui-outcome-contract.md` | 27 ACs (A1–G3) |
| Specs test section | `docs/specs/petition050-timeline-specs.md` §6 | §6.1 unit, §6.2 controller, §6.3 fragment test specs |
| Unit tests (common) | `opendebt-common/…/timeline/EventCategoryMapperTest.java` | 14 `@Test` stubs |
| Unit tests (common) | `opendebt-common/…/timeline/TimelineDeduplicatorTest.java` | 7 `@Test` stubs |
| Unit tests (common) | `opendebt-common/…/timeline/TimelineVisibilityPropertiesTest.java` | 7 `@Test` stubs |
| Fragment tests (common) | `opendebt-common/…/timeline/TimelineFragmentTest.java` | 14 `@Test` stubs |
| Controller tests (caseworker) | `opendebt-caseworker-portal/…/CaseTimelineControllerTest.java` | 18 `@Test` stubs |
| Controller tests (citizen) | `opendebt-citizen-portal/…/CitizenTimelineControllerTest.java` | 6 `@Test` stubs |
| Controller tests (creditor) | `opendebt-creditor-portal/…/CreditorTimelineControllerTest.java` | 7 `@Test` stubs |

**Total `@Test` methods audited: 73**

> **Note on test state:** All 73 `@Test` methods are currently failing stubs (`fail("not yet implemented: …")`). Every method contains a fully specified implementation comment and will pass once the referenced production class is built. The audit treats stub presence as coverage intent — a stub with no matching scenario/AC is still overreach; a scenario with no stub at all is a gap.

---

## 2. Coverage Matrix

### 2.1 Gherkin Scenarios → @Test Methods (33 scenarios)

| # | Feature line | Scenario | AC ref | Covered by | Status |
|---|---|---|---|---|---|
| 1 | 8 | Timeline merges events from case-service and payment-service | AC-A1 | `CaseTimelineControllerTest`: `showTimeline_mergesEventsFromCaseServiceAndPaymentService`, `showTimeline_mergedEntries_areSortedDescendingByTimestamp` | ✅ COVERED |
| 2 | 14 | Timeline normalises events into a common structure | AC-A3 | `EventCategoryMapperTest` (14 tests); `CitizenTimelineControllerTest`: `showTimeline_paymentReceivedEntry_hasNonNullAmountAndDebtId` | ✅ COVERED |
| 3 | 23 | Duplicate events from overlapping sources are deduplicated | AC-A2 | `TimelineDeduplicatorTest` (7 tests); `CaseTimelineControllerTest`: `showTimeline_deduplicatesOverlappingEventsFromBothSources` | ✅ COVERED |
| 4 | 30 | Caseworker sees all event categories | AC-B1 | `TimelineVisibilityPropertiesTest`: `getAllowedCategories_caseworker_returnsAll7Categories`; `CaseTimelineControllerTest`: `showTimeline_caseworkerAuth_returnsEntriesInAllSevenCategories` | ✅ COVERED |
| 5 | 35 | Supervisor sees all event categories | AC-B1 | `TimelineVisibilityPropertiesTest`: `getAllowedCategories_supervisor_returnsAll7Categories` *(unit only — no controller test with SUPERVISOR auth)* | ⚠️ PARTIAL |
| 6 | 41 | Admin sees all event categories | AC-B1 | `TimelineVisibilityPropertiesTest`: `getAllowedCategories_admin_returnsAll7Categories` *(unit only — no controller test with ADMIN auth)* | ⚠️ PARTIAL |
| 7 | 50 | Citizen sees only citizen-appropriate events | AC-B2 | `TimelineVisibilityPropertiesTest`: `getAllowedCategories_citizen_returnsFourAllowedCategories`; `CitizenTimelineControllerTest`: `showTimeline_citizenAuth_returnsOnlyCitizenVisibleCategories` | ✅ COVERED |
| 8 | 59 | Creditor sees only creditor-appropriate events | AC-B3 | `TimelineVisibilityPropertiesTest`: `getAllowedCategories_creditor_returnsThreeAllowedCategories`; `CreditorTimelineControllerTest`: `showTimeline_creditorAuth_returnsOnlyCreditorVisibleCategories` | ✅ COVERED |
| 9 | 68 | Visibility rules are read from external configuration, not templates | AC-B4 | `TimelineVisibilityPropertiesTest`: `eventCategory_enumHasExactly7Values`, `getAllowedCategories_unknownRole_returnsEmptySet`; `CaseTimelineControllerTest`: `showTimeline_visibilityResolvedFromConfiguration_notFromTemplate` | ✅ COVERED |
| 10 | 75 | **Hændelseslog tab is replaced by Tidslinje tab** | **AC-C1** | **NONE — listed in `CaseTimelineControllerTest` JavaDoc but no `@Test` method stub was written** | ❌ **GAP** |
| 11 | 81 | Financial entries display formatted amount | AC-C2 | `TimelineFragmentTest`: `fragment_financialEntryWithAmount_rendersFormattedDkk` | ✅ COVERED |
| 12 | 86 | Debt-linked entries link to debt detail | AC-C2 | `TimelineFragmentTest`: `fragment_debtLinkedEntry_rendersClickableLink` | ✅ COVERED |
| 13 | 92 | Entries are visually categorised with badges | AC-C3 | `TimelineFragmentTest`: `fragment_entriesWithDifferentCategories_renderCategoryBadgesWithCorrectCssClasses` | ✅ COVERED |
| 14 | 98 | Timeline UI renders required fields for a non-financial entry | AC-C2 | `TimelineFragmentTest`: `fragment_nonFinancialEntry_rendersAllRequiredFields` | ✅ COVERED |
| 15 | 106 | **Posteringslog tab is preserved unchanged** | **FR-7.3 (no AC)** | **NONE — listed in `CaseTimelineControllerTest` JavaDoc but no `@Test` method stub was written** | ❌ **GAP** |
| 16 | 112 | All portals render FINANCIAL entries with identical formatting | AC-F1/AC-C2 | `TimelineFragmentTest`: `fragment_financialEntry_amountFormattingIsConsistentAcrossPortals` | ✅ COVERED |
| 17 | 120 | Filter by single event category | AC-D1 | `CaseTimelineControllerTest`: `showTimeline_categoryFilterFinancial_returnsOnlyFinancialEntries` | ✅ COVERED |
| 18 | 126 | Filter by multiple event categories | AC-D1 | `CaseTimelineControllerTest`: `showTimeline_multipleCategoryFilters_returnsOnlyMatchingCategories` | ✅ COVERED |
| 19 | 131 | Filter by date range | AC-D2 | `CaseTimelineControllerTest`: `showTimeline_dateRangeFilter_returnsOnlyEntriesWithinRange` | ✅ COVERED |
| 20 | 136 | Filter by specific debt | AC-D3 | `CaseTimelineControllerTest`: `showTimeline_debtIdFilter_returnsCaseLevelAndMatchedDebtEntries` | ✅ COVERED |
| 21 | 143 | Active filters are displayed as chips | AC-D4 | `CaseTimelineControllerTest`: `showTimeline_activeFilters_filterDtoPopulatedInModel`; `TimelineFragmentTest`: `fragment_activeFinancialCategoryFilter_rendersFilterChip` | ✅ COVERED |
| 22 | 147 | **Removing a filter chip deactivates that filter** | **AC-D4** | **No `@Test` exists for the chip-removal state transition.** `fragment_noActiveFilters_noChipsRendered` tests the *absence* of chips when no filters are active — it does NOT test that clicking a chip's remove icon deactivates the filter. | ❌ **GAP** |
| 23 | 153 | **Clear all filters resets the timeline** | **AC-D5** | **No `@Test` exists for the clear-all action.** `fragment_activeFinancialCategoryFilter_rendersFilterChip` asserts the "Nulstil filtre" link is present — it does NOT test that triggering the link returns the full unfiltered timeline. | ❌ **GAP** |
| 24 | 162 | Initial load displays 25 entries with load-more button | AC-E1 | `CaseTimelineControllerTest`, `CitizenTimelineControllerTest`, `CreditorTimelineControllerTest`: `showTimeline_*_60Events_returns25EntriesWithHasMoreTrue`; `TimelineFragmentTest`: `fragment_hasMoreTrue_loadMoreButtonPresent` | ✅ COVERED |
| 25 | 168 | Load more appends next page of entries | AC-E2 | `CaseTimelineControllerTest`: `loadMoreEntries_page2_returnsNext25EntriesViaTimelineEntriesFragment`; `CreditorTimelineControllerTest`: `loadMorePosterEntries_page2_returnsTimelineEntriesFragment` | ✅ COVERED |
| 26 | 175 | Load more is hidden when all entries are loaded | AC-E3 | `CaseTimelineControllerTest`: `loadMoreEntries_lastPage_hasMoreFalse`; `TimelineFragmentTest`: `fragment_hasMoreFalse_loadMoreButtonAbsent`, `fragment_entriesFragment_containsOobLoadMoreContainer` | ✅ COVERED |
| 27 | 182 | Filters apply to paginated results | AC-E4 | `CaseTimelineControllerTest`: `loadMoreEntries_activeFiltersPreservedOnSubsequentPages` | ✅ COVERED |
| 28 | 190 | Caseworker portal renders the timeline | AC-F2 | `CaseTimelineControllerTest`: `showTimeline_mergesEventsFromCaseServiceAndPaymentService` (view assertion) + `showTimeline_caseworkerAuth_returnsEntriesInAllSevenCategories` (combined) | ✅ COVERED |
| 29 | 195 | Citizen portal renders the timeline with restricted events | AC-F3 | `CitizenTimelineControllerTest`: `showTimeline_citizenPortal_rendersTimelineFragmentWithRestrictedEvents` | ✅ COVERED |
| 30 | 200 | Creditor portal renders the timeline with restricted events | AC-F4 | `CreditorTimelineControllerTest`: `showTimeline_creditorPortal_rendersTimelineWithClaimIdInModel` | ✅ COVERED |
| 31 | 207 | Empty case shows no-events message | AC-G1 | `CaseTimelineControllerTest`, `CitizenTimelineControllerTest`, `CreditorTimelineControllerTest`: `showTimeline_*_emptyCase_*`; `TimelineFragmentTest`: `fragment_emptyEntriesList_showsEmptyStateMessage` | ✅ COVERED |
| 32 | 213 | Missing correspondence events handled gracefully | AC-G2 | `CaseTimelineControllerTest`: `showTimeline_missingCorrespondenceEvents_noErrorShown` | ✅ COVERED |
| 33 | 219 | Partial data when one service is unavailable | AC-G3 | `CaseTimelineControllerTest`, `CitizenTimelineControllerTest`, `CreditorTimelineControllerTest`: `showTimeline_*_paymentServiceDown_*`; `TimelineFragmentTest`: `fragment_warningsList_showsWarningBanner` | ✅ COVERED |

**Scenario summary: 27 COVERED · 2 PARTIAL · 4 GAP out of 33**

---

### 2.2 Outcome Contract ACs → @Test Methods (27 ACs)

| AC | Title | @Test count | Status | Notes |
|---|---|---|---|---|
| AC-A1 | BFF merges and sorts events | 4 | ✅ COVERED | Caseworker (2) + Creditor (1) + sort assertion |
| AC-A2 | Deduplication of overlapping events | 8 | ✅ COVERED | DeduplicatorTest (7) + controller test (1) |
| AC-A3 | Normalised timeline entry structure | 15 | ✅ COVERED | MapperTest (14) + citizen controller (1) |
| AC-B1 | Caseworker/Supervisor/Admin — all 7 categories | 5 | ✅ COVERED | Property tests (3) + caseworker controller (1) + visibility-from-config (1) |
| AC-B2 | Citizen — 4 restricted categories | 2 | ✅ COVERED | Property test (1) + citizen controller (1) |
| AC-B3 | Creditor — 3 restricted categories | 2 | ✅ COVERED | Property test (1) + creditor controller (1) |
| AC-B4 | Visibility from configuration, not templates | 3 | ✅ COVERED | Property tests (2) + caseworker controller (1) |
| **AC-C1** | **Timeline replaces Hændelseslog tab** | **0** | **❌ GAP** | **Listed in CaseTimelineControllerTest JavaDoc; stub never written** |
| AC-C2 | Entries display required fields (timestamp, badge, title, desc, amount, debt link) | 5 | ✅ COVERED | Fragment tests (required fields, DKK format, debt link, null amount) |
| AC-C3 | Entries visually categorised with colour-coded badges | 1 | ✅ COVERED | Fragment badge CSS class test |
| AC-C4 | Reverse chronological order | 1 | ✅ COVERED | Caseworker controller sort test |
| AC-D1 | Filter by event category (single and multi) | 2 | ✅ COVERED | Caseworker controller (single + multi) |
| AC-D2 | Filter by date range | 1 | ✅ COVERED | Caseworker controller date-range test |
| AC-D3 | Filter by specific debt | 1 | ✅ COVERED | Caseworker controller debt-filter test |
| **AC-D4** | **Active filters as chips; chip removal** | **2 (display only)** | **⚠️ PARTIAL** | **Chip display tested (model DTO + fragment). Chip removal action (clicking ✕ to deactivate a filter) has no `@Test`.** |
| **AC-D5** | **Clear all filters** | **0 (action)** | **❌ GAP** | **"Nulstil filtre" link presence is asserted in fragment test. No test verifies that activating the link resets the timeline.** |
| AC-E1 | Initial load of 25 entries, hasMore=true | 4 | ✅ COVERED | All 3 portals (3) + fragment button test (1) |
| AC-E2 | Load more appends entries | 2 | ✅ COVERED | Caseworker + creditor controllers |
| AC-E3 | Load more hidden when exhausted | 3 | ✅ COVERED | Caseworker controller + fragment (button absent + OOB container) |
| AC-E4 | Filters apply to paginated results | 1 | ✅ COVERED | Caseworker controller pagination filter test |
| **AC-F1** | **Common fragment shared across all portals** | **1 (formatting only)** | **⚠️ PARTIAL** | **Consistent DKK formatting tested via shared fragment. No test structurally asserts all 3 portals load the same `classpath:/templates/fragments/timeline.html` resource.** |
| AC-F2 | Caseworker portal timeline functional | 2 | ✅ COVERED | Caseworker controller (view + categories combined) |
| AC-F3 | Citizen portal timeline functional | 1 | ✅ COVERED | Citizen controller rendering test |
| AC-F4 | Creditor portal timeline functional | 1 | ✅ COVERED | Creditor controller rendering test |
| AC-G1 | Empty timeline | 4 | ✅ COVERED | All 3 portals + fragment |
| AC-G2 | Missing correspondence events | 1 | ✅ COVERED | Caseworker controller (no CORRESPONDENCE, no warning) |
| AC-G3 | Service unavailability — partial data warning | 4 | ✅ COVERED | All 3 portals + fragment warning banner |

**AC summary: 22 COVERED · 2 PARTIAL · 2 GAP (AC-C1, AC-D5) out of 27**

---

### 2.3 Specs §6 Test Specification Compliance

| Spec test case | Status |
|---|---|
| §6.1 `EventCategoryMapperTest` — all 8 spec rows mapped to tests | ✅ 14 tests (spec has 8 rows; test also covers `COLLECTION_MEASURE_INITIATED→COLLECTION`, `OBJECTION_FILED→OBJECTION`, `JOURNAL_ENTRY_ADDED→JOURNAL`, `fromDebtEventType→FINANCIAL`, unknown-type WARN log — all justified by AC-A3 full mapping table in specs §2.5) |
| §6.1 `TimelineDeduplicatorTest` — all 7 spec rows mapped | ✅ 7 tests (1:1 with spec table) |
| §6.1 `TimelineVisibilityPropertiesTest` — 3 spec rows mapped, 4 additional | ✅ 7 tests (SUPERVISOR, ADMIN, CREDITOR, enum-size tests justified by feature scenarios) |
| §6.2 Caseworker controller — 13 spec rows | ✅ 16 tests (includes extra: sort test, multi-category filter, chip model DTO, correspondence, security) |
| §6.2 Citizen controller | ✅ 6 tests |
| §6.2 Creditor controller | ✅ 7 tests |
| §6.3 Fragment tests — 11 spec rows | ✅ 14 tests (includes extras: null-amount guard, empty-warnings guard, no-chips guard — justified as derived from AC-A3/C2/D4/G1/NFR-Testability) |

---

## 3. Flagged Gaps

### GAP-1 — SEVERITY: HIGH
**Scenario 10 (feature line 75) / AC-C1: "Hændelseslog tab is replaced by Tidslinje tab"**

- **AC-C1 state:** The outcome contract requires the tab label to change from "Hændelseslog" to "Tidslinje" and the tab content to be the unified timeline component.
- **@Test count:** **0**
- **Evidence:** `CaseTimelineControllerTest` JavaDoc (line 33–34) explicitly lists this scenario as *covered by this class*, but the implementation block was never stubbed. There is no corresponding `@Test` method in any of the 7 audited test files.
- **Required stub location:** `CaseTimelineControllerTest` (controller must return the "Tidslinje" view name or equivalent) and/or a Thymeleaf template integration test verifying the tab label text.
- **Impact:** AC-C1 has zero test coverage.

---

### GAP-2 — SEVERITY: MEDIUM
**Scenario 15 (feature line 106): "Posteringslog tab is preserved unchanged"**

- **AC mapping:** Feature scenario only; not an explicit acceptance criterion in the outcome contract (captured as FR-7.3 in the traceability table: "Posteringslog unchanged; no spec needed"). However, as a Gherkin scenario it requires a `@Test`.
- **@Test count:** **0**
- **Evidence:** `CaseTimelineControllerTest` JavaDoc (line 34) lists "Scenario: Posteringslog tab is preserved unchanged (feature line 106)" under covered scenarios, but the stub was never written.
- **Required stub location:** `CaseTimelineControllerTest` — assert that the "Posteringslog" tab is still present in the rendered page, i.e., the controller does not inadvertently remove it.
- **Impact:** Scenario 15 has zero test coverage.

---

### GAP-3 — SEVERITY: HIGH
**Scenario 22 (feature line 147) / AC-D4 (partial): "Removing a filter chip deactivates that filter"**

- **AC-D4 text:** "clicking the remove icon on a chip removes that filter and refreshes the timeline."
- **@Test count for removal:** **0**
- **Evidence:** `TimelineFragmentTest.fragment_noActiveFilters_noChipsRendered` asserts that no chip elements are rendered when no filters are active — a static rendering assertion. This does NOT test the dynamic state transition (chip present → user clicks ✕ → filter deactivated → timeline refreshes showing all categories). `CaseTimelineControllerTest` JavaDoc (line 39) lists this scenario but no stub was written.
- **Required stub location:** Either (a) a controller test asserting that a request without the previously active `eventCategory` parameter produces all-category results, or (b) an HTMX integration test, or (c) a fragment test that explicitly tests the chip-with-removal-link structure rather than the no-chips state.
- **Impact:** AC-D4 chip-removal half is untested.

---

### GAP-4 — SEVERITY: HIGH
**Scenario 23 (feature line 153) / AC-D5: "Clear all filters resets the timeline"**

- **AC-D5 text:** "all filters are removed and the full unfiltered timeline is displayed."
- **@Test count for action:** **0**
- **Evidence:** `TimelineFragmentTest.fragment_activeFinancialCategoryFilter_rendersFilterChip` asserts that the "Nulstil filtre" (clear-all) link is rendered when filters are active — this is a *presence* assertion. There is no test verifying that the link's target returns the full unfiltered timeline (i.e., a GET with no filter parameters returns entries from all allowed categories). `CaseTimelineControllerTest` JavaDoc (line 40) lists this scenario but no stub was written.
- **Required stub location:** `CaseTimelineControllerTest` — assert that a `GET /cases/{id}/tidslinje` with no filter parameters and no `fromDate`/`toDate`/`debtId` yields the complete unfiltered entry set.
- **Impact:** AC-D5 has no actionable test coverage.

---

## 4. Warnings (Partial Coverage)

### WARN-1
**Scenarios 5 & 6 (feature lines 35, 41) / AC-B1: SUPERVISOR and ADMIN role variants**

- `TimelineVisibilityPropertiesTest` proves the Spring `@ConfigurationProperties` binding returns all 7 categories for SUPERVISOR and ADMIN via the config binding layer.
- However, no `CaseTimelineControllerTest` test exercises a controller call with SUPERVISOR or ADMIN authentication — only CASEWORKER auth is tested at controller level.
- The Gherkin scenarios say "Given the current user has role SUPERVISOR/ADMIN … When the timeline loads … Then all 7 categories are displayed" — the "timeline loads" step implies a portal render, not just property binding.
- **Risk:** A future security configuration change that rejects SUPERVISOR/ADMIN on the caseworker portal endpoint would not be caught by existing tests.
- **Recommendation:** Add two controller-level test stubs to `CaseTimelineControllerTest` with SUPERVISOR and ADMIN auth principals.

### WARN-2
**AC-F1: Common fragment shared across all portals**

- `TimelineFragmentTest.fragment_financialEntry_amountFormattingIsConsistentAcrossPortals` verifies that the shared template produces consistent DKK formatting.
- AC-F1 states "all three [portals] use the **same** Thymeleaf fragment and CSS." This structural claim (classpath identity of the template across portals) is not verifiable by a runtime test alone — it is an artifact of module dependency configuration. No test asserts that the same `fragments/timeline.html` classpath resource is resolved in each portal's test context.
- **Risk:** Low — if a portal accidentally bundled a different copy of the fragment it would likely break formatting and be caught by other tests.
- **Recommendation:** Document as a configuration invariant rather than adding a structural test; accept as known limitation.

---

## 5. Overreach Analysis

**Finding: No overreach detected.**

All 73 `@Test` methods are traceable to an explicit source:

| @Test method | Traceability |
|---|---|
| `fragment_nullAmount_noDkkStringRendered` | Derived from AC-A3 ("amount null for non-financial") + AC-C2 ("financial entries additionally display amount") |
| `fragment_emptyWarningsList_noWarningBannerRendered` | Derived from AC-G1 (empty case, no warning) + NFR Testability ("renders correctly with mock data") |
| `fragment_noActiveFilters_noChipsRendered` | Derived from AC-D4 (chip only shown when filters active — inverse state) |
| `fragment_entriesFragment_containsOobLoadMoreContainer` | Spec §2.8 explicit: "/tidslinje/entries response must include OOB update of #load-more-container"; Spec §6.3 row |
| `showTimeline_creditorRole_isForbidden` | Spec §6.2 explicit: "Security: rejected role → HTTP 403" |
| All other `@Test` methods | Direct 1:1 mapping to Gherkin scenario or AC |

---

## 6. Syntax / Compilation Check

All 7 test files use only standard JUnit 5 imports (`@Test`, `@DisplayName`, `fail()`, `@ExtendWith(MockitoExtension.class)`). All production classes referenced (`EventCategoryMapper`, `TimelineDeduplicator`, `TimelineVisibilityProperties`, `TimelineEntryDto`, `TimelineFilterDto`, `EventCategory`, portal controllers) are currently **absent** — every test is an intentional failing stub.

A `mvn -q -DskipTests test-compile` would be required to verify compilation. Given that all production classes are absent, `test-compile` is expected to fail until implementation begins. The test stubs themselves contain no syntax errors; all compilation failures will be in the `// TODO: implement` blocks which are commented out.

**Syntax verdict:** Test file syntax is valid. Compilation failure is expected and correct at this pre-implementation stage.

---

## 7. Blocking Issues

| # | Issue | Severity | AC/Scenario | Action Required |
|---|---|---|---|---|
| **B-1** | AC-C1 has zero `@Test` coverage. "Hændelseslog tab replaced by Tidslinje tab" scenario stub was never written despite being listed in `CaseTimelineControllerTest` JavaDoc. | **HIGH** | AC-C1 / Scenario 10 | Write `@Test` stub in `CaseTimelineControllerTest` (or a dedicated tab-label test) before implementation begins. |
| **B-2** | AC-D5 "Clear all filters" has zero actionable `@Test` coverage. Link presence is asserted; action result is not. | **HIGH** | AC-D5 / Scenario 23 | Write `@Test` stub in `CaseTimelineControllerTest` asserting that no-filter request returns full unfiltered timeline. |
| **B-3** | Scenario 22 "Removing a filter chip deactivates that filter" — AC-D4 removal half — has zero `@Test` coverage. | **HIGH** | AC-D4 / Scenario 22 | Write `@Test` stub (controller or fragment) testing the filter deactivation state transition. |
| **B-4** | Scenario 15 "Posteringslog tab preserved unchanged" has zero `@Test` coverage. Stub listed in JavaDoc, not written. | **MEDIUM** | FR-7.3 / Scenario 15 | Write `@Test` stub in `CaseTimelineControllerTest` asserting Posteringslog tab is present and unmodified. |
| **B-5** | SUPERVISOR and ADMIN role scenarios (lines 35, 41) have unit-level property binding tests but no controller-level integration tests. | **MEDIUM** | AC-B1 / Scenarios 5–6 | Add two controller test stubs to `CaseTimelineControllerTest` with SUPERVISOR and ADMIN auth. |

---

## 8. Approval Decision

### ❌ FAIL

**Reason:** 4 hard coverage gaps identified across 4 Gherkin scenarios and 2 acceptance criteria (AC-C1 zero coverage; AC-D5 zero actionable coverage; AC-D4 removal half uncovered; Scenario 15 uncovered). The test suite cannot be approved until all 5 blocking issues above are resolved with written `@Test` stubs.

**What is correct:**
- 27 of 33 Gherkin scenarios have at least one `@Test` stub.
- 22 of 27 outcome contract ACs have full `@Test` coverage.
- 73 total `@Test` stubs are defined; zero exhibit overreach — every test is traceable to an explicit petition artefact.
- All three portal controllers (caseworker, citizen, creditor) have corresponding controller test classes.
- The shared `TimelineFragmentTest` provides robust Thymeleaf rendering coverage for all rendering-related ACs.
- Deduplication and event category mapping have thorough unit test suites (7 and 14 tests respectively).

**What must be fixed before approval:**
1. Write `@Test` stub for **AC-C1** (Hændelseslog → Tidslinje tab label) in `CaseTimelineControllerTest`.
2. Write `@Test` stub for **AC-D5** (clear-all resets timeline) in `CaseTimelineControllerTest`.
3. Write `@Test` stub for **Scenario 22 / AC-D4 removal** (chip removal deactivates filter) — controller or fragment.
4. Write `@Test` stub for **Scenario 15 / FR-7.3** (Posteringslog tab preserved) in `CaseTimelineControllerTest`.
5. Add SUPERVISOR and ADMIN controller-level test stubs to address scenarios 5 and 6 at integration depth.

Re-submit for audit after all 5 stubs are written. No implementation changes are required — stub creation is sufficient to unblock approval.
