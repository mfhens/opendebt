Feature: Unified Case Timeline UI
  As a portal user (caseworker, citizen, or creditor)
  I want a unified chronological timeline of all case events
  So that I can understand the full history of a case in a single view

  # --- Data Aggregation ---

  Scenario: Timeline merges events from case-service and payment-service
    Given a case "C-1001" exists with 3 case events and 2 linked debts with 4 financial events
    When the caseworker opens the Tidslinje tab for case "C-1001"
    Then the timeline displays 7 entries
    And the entries are sorted newest-first by timestamp

  Scenario: Timeline normalises events into a common structure
    Given a case "C-1001" has a CASE_CREATED event and a PAYMENT_RECEIVED event
    When the timeline loads
    Then both entries have fields: id, timestamp, eventCategory, eventType, title, description
    And the PAYMENT_RECEIVED entry has a non-null amount
    And the CASE_CREATED entry has a null amount
    And the PAYMENT_RECEIVED entry has a non-null debtId
    And the CASE_CREATED entry has a null debtId

  Scenario: Duplicate events from overlapping sources are deduplicated
    Given a DEBT_REGISTERED event exists in both case-service and payment-service for the same debt
    When the timeline merges events
    Then only one DEBT_REGISTERED entry appears for that debt

  # --- Role-Based Visibility: Caseworker / Supervisor / Admin ---

  Scenario: Caseworker sees all event categories
    Given the current user has role "CASEWORKER"
    And the case has events in categories CASE, DEBT_LIFECYCLE, FINANCIAL, COLLECTION, CORRESPONDENCE, OBJECTION, JOURNAL
    When the timeline loads
    Then all 7 event categories are represented in the timeline

  Scenario: Supervisor sees all event categories
    Given the current user has role "SUPERVISOR"
    And the case has events in categories CASE, DEBT_LIFECYCLE, FINANCIAL, COLLECTION, CORRESPONDENCE, OBJECTION, JOURNAL
    When the timeline loads
    Then all 7 event categories are represented in the timeline

  Scenario: Admin sees all event categories
    Given the current user has role "ADMIN"
    And the case has events in categories CASE, DEBT_LIFECYCLE, FINANCIAL, COLLECTION, CORRESPONDENCE, OBJECTION, JOURNAL
    When the timeline loads
    Then all 7 event categories are represented in the timeline

  # --- Role-Based Visibility: Citizen ---

  Scenario: Citizen sees only citizen-appropriate events
    Given the current user has role "CITIZEN"
    And the case has events in categories CASE, DEBT_LIFECYCLE, FINANCIAL, COLLECTION, CORRESPONDENCE, OBJECTION, JOURNAL
    When the timeline loads
    Then only events in categories FINANCIAL, DEBT_LIFECYCLE, CORRESPONDENCE, COLLECTION are displayed
    And no events in categories CASE, OBJECTION, JOURNAL are displayed

  # --- Role-Based Visibility: Creditor ---

  Scenario: Creditor sees only creditor-appropriate events
    Given the current user has role "CREDITOR"
    And the case has events in categories CASE, DEBT_LIFECYCLE, FINANCIAL, COLLECTION, CORRESPONDENCE, OBJECTION, JOURNAL
    When the timeline loads
    Then only events in categories FINANCIAL, DEBT_LIFECYCLE, COLLECTION are displayed
    And no events in categories CASE, CORRESPONDENCE, OBJECTION, JOURNAL are displayed

  # --- Visibility Configuration ---

  Scenario: Visibility rules are read from external configuration, not templates
    Given the role-to-category visibility mapping is defined in application configuration properties
    When the caseworker portal renders the timeline for a CASEWORKER user
    Then the displayed events match the categories configured for the CASEWORKER role

  # --- Timeline UI Rendering ---

  Scenario: Hændelseslog tab is replaced by Tidslinje tab
    Given the case detail page for case "C-1001" is accessible
    When a user navigates to the case detail page
    Then the tab previously labelled "Hændelseslog" is labelled "Tidslinje"
    And the tab content is the unified timeline component

  Scenario: Financial entries display formatted amount
    Given the timeline contains a PAYMENT_RECEIVED event with amount 1234.56
    When the timeline renders
    Then the entry displays "1.234,56 DKK"

  Scenario: Debt-linked entries link to debt detail
    Given the timeline contains an event linked to debt "D-2001"
    When the timeline renders
    Then the event entry displays the debt reference "D-2001"
    And the reference is a clickable link to the debt detail page

  Scenario: Entries are visually categorised with badges
    Given the timeline contains events from categories CASE, FINANCIAL, and COLLECTION
    When the timeline renders
    Then each entry displays a colour-coded badge matching its event category
    And the badge colours are consistent with the SKAT design system

  Scenario: Timeline UI renders required fields for a non-financial entry
    Given the timeline contains a CASE_CREATED case event
    When the timeline renders
    Then the entry displays a formatted timestamp
    And the entry displays an event category badge
    And the entry displays a title
    And the entry displays a description

  Scenario: Posteringslog tab is preserved unchanged
    Given the case detail page for case "C-1001" is accessible
    When a user navigates to the case detail page
    Then the "Posteringslog" tab is present alongside the "Tidslinje" tab
    And the Posteringslog tab content displays the detailed financial ledger

  Scenario: All portals render FINANCIAL entries with identical formatting
    Given a PAYMENT_RECEIVED event with amount 1234.56 is visible to CASEWORKER, CITIZEN, and CREDITOR roles
    When the caseworker portal, citizen portal, and creditor portal each render that event in the Tidslinje tab
    Then all three portals display the amount as "1.234,56 DKK"
    And the entry structure (timestamp, category badge, title, description) is identical across all three portals

  # --- Filtering ---

  Scenario: Filter by single event category
    Given the timeline displays events from categories CASE, FINANCIAL, and COLLECTION
    When the user selects category filter "FINANCIAL"
    Then only FINANCIAL events are displayed
    And the filter is applied without full page reload

  Scenario: Filter by multiple event categories
    Given the timeline displays events from categories CASE, FINANCIAL, COLLECTION, and JOURNAL
    When the user selects category filters "FINANCIAL" and "COLLECTION"
    Then only FINANCIAL and COLLECTION events are displayed

  Scenario: Filter by date range
    Given the timeline contains events from 2025-01-01 through 2026-03-23
    When the user sets from-date "2026-01-01" and to-date "2026-03-01"
    Then only events with timestamps between 2026-01-01 and 2026-03-01 are displayed

  Scenario: Filter by specific debt
    Given the case has debts "D-2001" and "D-2002"
    And the timeline contains events linked to both debts plus case-level events
    When the user selects debt filter "D-2001"
    Then only events linked to "D-2001" and case-level events are displayed
    And events linked only to "D-2002" are hidden

  Scenario: Active filters are displayed as chips
    Given the user has applied a category filter "FINANCIAL" and a date from "2026-01-01"
    When the timeline renders
    Then two filter chips are displayed above the timeline

  Scenario: Removing a filter chip deactivates that filter
    Given category filter "FINANCIAL" and date filter "2026-01-01" are active
    When the user removes the "FINANCIAL" chip
    Then only the date filter "2026-01-01" remains active
    And the timeline refreshes to show all categories from 2026-01-01

  Scenario: Clear all filters resets the timeline
    Given multiple filters are active
    When the user clicks "Clear all filters"
    Then all filter chips are removed
    And the full unfiltered timeline is displayed

  # --- Lazy Loading / Pagination ---

  Scenario: Initial load displays 25 entries with load-more button
    Given a case has 60 timeline events
    When the timeline tab is opened
    Then 25 entries are displayed
    And a "Load more" button is visible below the entries

  Scenario: Load more appends next page of entries
    Given 25 entries are displayed and older entries exist
    When the user clicks "Load more"
    Then 25 additional entries are appended below the existing ones
    And the original 25 entries remain in place
    And the "Load more" button is still visible

  Scenario: Load more is hidden when all entries are loaded
    Given a case has 30 timeline events
    And the first 25 are displayed
    When the user clicks "Load more"
    Then 5 additional entries are appended
    And the "Load more" button is no longer visible

  Scenario: Filters apply to paginated results
    Given the user has a category filter "FINANCIAL" active
    And there are 40 FINANCIAL events
    When the user clicks "Load more"
    Then the next page contains only FINANCIAL events

  # --- Cross-Portal Consistency ---

  Scenario: Caseworker portal renders the timeline
    Given a caseworker navigates to case detail for case "C-1001"
    When the Tidslinje tab is selected
    Then the unified timeline component renders with all event categories

  Scenario: Citizen portal renders the timeline with restricted events
    Given a citizen navigates to their case detail page
    When the Tidslinje tab is selected
    Then the unified timeline renders showing only citizen-visible events

  Scenario: Creditor portal renders the timeline with restricted events
    Given a creditor navigates to a case detail page
    When the Tidslinje tab is selected
    Then the unified timeline renders showing only creditor-visible events

  # --- Graceful Degradation ---

  Scenario: Empty case shows no-events message
    Given a case "C-9999" has zero events
    When the timeline tab is opened
    Then the message "Ingen hændelser registreret" is displayed
    And no error is shown

  Scenario: Missing correspondence events handled gracefully
    Given the letter-service does not emit events
    When the timeline loads
    Then no CORRESPONDENCE entries appear
    And no error or empty category placeholder is shown

  Scenario: Partial data when one service is unavailable
    Given the payment-service is temporarily unavailable
    And the case-service returns 5 case events
    When the timeline loads
    Then 5 case events are displayed
    And a warning reads "Nogle hændelser kunne ikke hentes"
