# Validation Contract — petition026

## VAL-P026-001: Unauthenticated citizen is redirected to MitID/TastSelv from the debt overview page

**Source**: `petition026-citizen-debt-overview-page.feature` — Scenario: "Unauthenticated citizen is redirected to MitID/TastSelv from the debt overview page"  
**Description**: A citizen opens `/min-gaeld` without an authenticated session and is sent into the MitID/TastSelv login flow instead of seeing the debt page.  
**Pass criteria**:
- Opening `/min-gaeld` without an authenticated session does not show the debt overview content.
- The browser is redirected into the MitID/TastSelv login flow.
**Fail criteria**: Any observable outcome that deviates from the pass criteria above.  
**Required evidence**: screenshots, network, console_errors

## VAL-P026-002: Landing page MitID call-to-action opens the internal debt overview after login

**Source**: `petition026-citizen-debt-overview-page.feature` — Scenario: "Landing page MitID call-to-action opens the internal debt overview after login"  
**Description**: A citizen starts from the landing page, activates the MitID call-to-action, completes login, and ends on `/min-gaeld`.  
**Pass criteria**:
- The landing page MitID call-to-action targets the internal debt-overview flow rather than an external overview URL.
- After successful login, the citizen lands on `/min-gaeld` and the debt overview page is visible.
**Fail criteria**: Any observable outcome that deviates from the pass criteria above.  
**Required evidence**: screenshots, network, console_errors

## VAL-P026-003: Debt overview loads debt data for the authenticated session person

**Source**: `petition026-citizen-debt-overview-page.feature` — Scenario: "Debt overview loads debt data for the authenticated session person"  
**Description**: An authenticated citizen opens `/min-gaeld`, the page requests the correct backend page slice, and the citizen sees only the debt snapshot belonging to the logged-in citizen.  
**Pass criteria**:
- The visible debt snapshot matches the authenticated citizen test fixture.
- The network evidence shows a `GET /api/v1/citizen/debts` request that includes both `pageNumber` and the portal-supplied `pageSize`.
- Debt belonging to another citizen is not shown on the page.
**Fail criteria**: Any observable outcome that deviates from the pass criteria above.  
**Required evidence**: screenshots, network, console_errors

## VAL-P026-004: Debt overview shows total outstanding amount and a semantic debt table

**Source**: `petition026-citizen-debt-overview-page.feature` — Scenario: "Debt overview shows total outstanding amount and a semantic debt table"  
**Description**: An authenticated citizen opens `/min-gaeld` and sees the total outstanding amount, a semantic table of debts, and page navigation whenever the dataset spans multiple pages.  
**Pass criteria**:
- The total outstanding debt amount is shown prominently at the top of the page.
- The debt table is visible with a caption, a table header, and column headers for debt type, creditor name, principal amount, outstanding balance, due date, and status.
- Each rendered debt row shows values for the required columns.
- When the fixture spans more than one page, accessible pagination controls are visible.
- Moving to another page updates the visible rows and active page state instead of repeating the first page or hiding remaining rows.
**Fail criteria**: Any observable outcome that deviates from the pass criteria above.  
**Required evidence**: screenshots, network, console_errors

## VAL-P026-005: Debt rows display the status returned for a debt

**Source**: `petition026-citizen-debt-overview-page.feature` — Scenario: "Debt rows display the status returned for a debt"  
**Description**: An authenticated citizen opens `/min-gaeld` and sees the expected status label for a returned debt.  
**Pass criteria**:
- The displayed status for the debt row matches the expected returned status fixture for that scenario.
- No contradictory status label or duplicate status text is shown for the same row.
**Fail criteria**: Any observable outcome that deviates from the pass criteria above.  
**Required evidence**: screenshots, network, console_errors

## VAL-P026-006: Citizen with no outstanding debt sees a clear no-debt message

**Source**: `petition026-citizen-debt-overview-page.feature` — Scenario: "Citizen with no outstanding debt sees a clear no-debt message"  
**Description**: An authenticated citizen with no debt opens `/min-gaeld` and sees a clear accessible no-debt message instead of an empty table.  
**Pass criteria**:
- A clear no-debt message is visible.
- The no-debt state is presented accessibly.
- An empty debt table is not shown.
**Fail criteria**: Any observable outcome that deviates from the pass criteria above.  
**Required evidence**: screenshots, console_errors

## VAL-P026-007: Debt overview presents interest, snapshot, and contact explanations

**Source**: `petition026-citizen-debt-overview-page.feature` — Scenario: "Debt overview presents interest, snapshot, and contact explanations"  
**Description**: An authenticated citizen opens `/min-gaeld` and sees the required explanatory text for interest, snapshot timing, and contact guidance.  
**Pass criteria**:
- The page explains that interest accrues daily.
- The page explains that payments reduce accrued interest before principal and shows the current interest-rate note.
- Each debt shows interest information.
- The page explains that the overview is a snapshot and that the actual balance may differ slightly.
- The configured phone number and contact guidance are visible.
**Fail criteria**: Any observable outcome that deviates from the pass criteria above.  
**Required evidence**: screenshots, network, console_errors

## VAL-P026-008: Debt overview shows payment, PDF placeholder, and navigation links

**Source**: `petition026-citizen-debt-overview-page.feature` — Scenario: "Debt overview shows payment, PDF placeholder, and navigation links"  
**Description**: An authenticated citizen opens `/min-gaeld` and sees payment guidance, a PDF placeholder affordance, and a way back to the landing page.  
**Pass criteria**:
- A link or button to the configured external payment page is visible.
- A PDF affordance is visible and clearly indicates that the capability is a future enhancement.
- A link back to the landing page is visible.
- The interaction does not start payment processing or debt modification from this page.
**Fail criteria**: Any observable outcome that deviates from the pass criteria above.  
**Required evidence**: screenshots, console_errors

## VAL-P026-009: Debt overview uses localized message bundles and locale-aware currency formatting

**Source**: `petition026-citizen-debt-overview-page.feature` — Scenario Outline: "Debt overview uses localized message bundles and locale-aware currency formatting"  
**Description**: An authenticated citizen opens `/min-gaeld` in Danish and English and sees localized copy plus DKK values formatted for the selected locale.  
**Pass criteria**:
- In the `da` run, the page is rendered with Danish copy and DKK values formatted for the Danish locale.
- In the `en-GB` run, the page is rendered with English copy and DKK values formatted for the English locale.
- The rendered copy changes with the language selection rather than remaining fixed in one language.
**Fail criteria**: Any observable outcome that deviates from the pass criteria above.  
**Required evidence**: screenshots, console_errors

## VAL-P026-010: Debt overview is keyboard-navigable and screen-reader compatible

**Source**: `petition026-citizen-debt-overview-page.feature` — Scenario: "Debt overview is keyboard-navigable and screen-reader compatible"  
**Description**: An authenticated citizen can navigate the debt overview by keyboard and a screen reader can announce the table structure.  
**Pass criteria**:
- The page can be traversed with keyboard-only interaction.
- The screen reader can announce the debt table caption and column headers.
- No required information on the page is available only through pointer-specific interaction.
**Fail criteria**: Any observable outcome that deviates from the pass criteria above.  
**Required evidence**: screenshots, console_errors

## VAL-P026-011: Debt-service unavailability is communicated without exposing stack traces

**Source**: `petition026-citizen-debt-overview-page.feature` — Scenario: "Debt-service unavailability is communicated without exposing stack traces"  
**Description**: An authenticated citizen opens `/min-gaeld` while the backend is unavailable and sees a user-friendly accessible error state.  
**Pass criteria**:
- A user-friendly service-unavailable message is visible.
- The error state is communicated accessibly.
- No stack trace or raw exception details are visible to the citizen.
**Fail criteria**: Any observable outcome that deviates from the pass criteria above.  
**Required evidence**: screenshots, network, console_errors

## VAL-P026-012: Debt overview works without client-side scripting

**Source**: `petition026-citizen-debt-overview-page.feature` — Scenario: "Debt overview works without client-side scripting"  
**Description**: An authenticated citizen opens `/min-gaeld` in a browser with JavaScript disabled and can still read the debt snapshot.  
**Pass criteria**:
- The page renders successfully with client-side scripting disabled.
- The citizen can read the debt snapshot, total, and table content without JavaScript.
**Fail criteria**: Any observable outcome that deviates from the pass criteria above.  
**Required evidence**: screenshots, console_errors

## VAL-P026-013: Paused interest is explained when accrual is suspended for unclear debt

**Source**: `petition026-citizen-debt-overview-page.feature` — Scenario: "Paused interest is explained when accrual is suspended for unclear debt"  
**Description**: An authenticated citizen opens `/min-gaeld` for a debt with paused interest and sees that the interest accrual is suspended.  
**Pass criteria**:
- The affected debt visibly shows that interest accrual is paused.
- The pause explanation is shown on the page for that debt.
**Fail criteria**: Any observable outcome that deviates from the pass criteria above.  
**Required evidence**: screenshots, network, console_errors

## VAL-P026-014: Debt overview explains that recovery interest is not tax-deductible

**Source**: `petition026-citizen-debt-overview-page.feature` — Scenario: "Debt overview explains that recovery interest is not tax-deductible"  
**Description**: An authenticated citizen opens `/min-gaeld` for debts with recovery interest and sees the non-tax-deductible notice.  
**Pass criteria**:
- The page states that recovery interest is not tax-deductible.
- The notice is visible together with the debt overview content rather than hidden behind a separate flow.
**Fail criteria**: Any observable outcome that deviates from the pass criteria above.  
**Required evidence**: screenshots, network, console_errors

## VAL-P026-015: Written-off debt status explains why the debt was closed

**Source**: `petition026-citizen-debt-overview-page.feature` — Scenario Outline: "Written-off debt status explains why the debt was closed"  
**Description**: An authenticated citizen opens `/min-gaeld` for each written-off test fixture and sees both the written-off status and the matching reason text.  
**Pass criteria**:
- For each write-off reason example, the row shows status `WRITTEN_OFF`.
- For each write-off reason example, the page shows the matching explanatory sub-text for why the debt was closed.
- The explanation is shown at the same time as the debt row rather than requiring a separate drilldown page.
**Fail criteria**: Any observable outcome that deviates from the pass criteria above.  
**Required evidence**: screenshots, network, console_errors
