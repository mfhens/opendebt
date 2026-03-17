Feature: Creditor portal claims in recovery, zero-balance claims, and counts (petition 029)

  Background:
    Given the creditor portal is running

  # --- Claims in recovery list ---

  Scenario: Recovery list page exists with HTMX progressive loading
    Given a creditor user with acting creditor context
    When the user opens the claims in recovery list page
    Then the recovery list page uses the SKAT standardlayout
    And the recovery list table body is loaded asynchronously via HTMX

  Scenario: Recovery list table fragment contains all 13 required columns
    Given the claims table fragment template exists
    Then the table fragment contains columns for claim-id, received-date, debtor-type, debtor-id, debtor-count, creditor-reference, claim-type, claim-status, incorporation-date, period, amount-sent, balance, and balance-with-interest

  Scenario: Recovery list table uses semantic HTML
    Given the claims table fragment template exists
    Then the table uses semantic HTML elements including table, thead, tbody, and th with scope col
    And the table has an aria-label for accessibility

  # --- Zero-balance claims list ---

  Scenario: Zero-balance list page exists with HTMX progressive loading
    Given a creditor user with acting creditor context
    When the user opens the zero-balance claims list page
    Then the zero-balance list page uses the SKAT standardlayout
    And the zero-balance list table body is loaded asynchronously via HTMX

  Scenario: Zero-balance list uses the same column layout as the recovery list
    Given the claims table fragment template exists
    Then both recovery and zero-balance lists use the same claims table fragment

  # --- Search and filtering ---

  Scenario: Search controls support search by Fordrings-ID, CPR, CVR, and SE
    Given a creditor user with acting creditor context
    When the user opens the claims in recovery list page
    Then the search form includes options for claim-id, CPR, CVR, and SE search types

  Scenario: Date range filtering controls are available
    Given a creditor user with acting creditor context
    When the user opens the claims in recovery list page
    Then the page includes date range filter inputs for modtagelsesdato

  # --- Claims counts ---

  Scenario: Claims counts page displays recovery and zero-balance counts
    Given the claims counts template exists
    Then the counts page shows a recovery count card and a zero-balance count card
    And the counts page includes a date range filter form

  # --- Pagination ---

  Scenario: Pagination controls use HTMX for content swap
    Given the claims table fragment template exists
    Then the pagination controls use HTMX to load pages without full page reload
    And the pagination controls are keyboard accessible

  # --- CPR censoring ---

  Scenario: CPR numbers are censored in display
    Given the claims table fragment template exists
    Then the table is designed to display pre-censored debtor identifiers

  # --- Formatting ---

  Scenario: Monetary amounts use Danish locale formatting
    Given the claims table fragment template exists
    Then monetary amounts in the table use comma as decimal separator with 2 decimal places

  Scenario: Dates are formatted as dd.MM.yyyy
    Given the claims table fragment template exists
    Then dates in the table are formatted as dd.MM.yyyy

  # --- i18n ---

  Scenario: All claims list text uses message bundles with Danish and English
    Given the creditor portal is running
    Then Danish translations exist for all claims list message keys
    And English translations exist for all claims list message keys

  # --- Click-through ---

  Scenario: Clicking a claim row navigates to the claim detail page
    Given the claims table fragment template exists
    Then each claim row contains a link to the claim detail page
