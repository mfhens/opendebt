Feature: Creditor portal claims in recovery, zero-balance claims, and counts

  # --- Claims in recovery list (FR 1-7) ---

  Scenario: Recovery list displays paginated claims for the acting creditor
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And user "U1" is bound to fordringshaver "K1"
    And fordringshaver "K1" has claims in recovery in debt-service
    When user "U1" opens the claims in recovery list
    Then the portal displays a paginated table of claims in recovery for fordringshaver "K1"
    And zero-balance claims are not included in the list

  Scenario: Each claim row displays all 13 required columns
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And the claims in recovery list is displayed
    Then each claim row displays: Fordrings-ID, modtagelsesdato, skyldner-type, skyldner-ID, antal skyldnere, fordringshaver-reference, fordringstype, fordringsstatus, stiftelsesdato, periode, amount sent for recovery, saldo, and saldo with interest and fees

  Scenario: Recovery list supports server-side sorting by any column
    Given portal user "U1" is viewing the claims in recovery list
    When user "U1" clicks a column header to sort
    Then the list is sorted by that column on the server side
    And sorting can be toggled between ascending and descending order

  Scenario: Recovery list supports search by Fordrings-ID
    Given portal user "U1" is viewing the claims in recovery list
    When user "U1" searches by Fordrings-ID "FDR-12345"
    Then only claims matching Fordrings-ID "FDR-12345" are displayed

  Scenario: Recovery list supports search by CPR number
    Given portal user "U1" is viewing the claims in recovery list
    When user "U1" searches by CPR number
    Then only claims matching the CPR number are displayed

  Scenario: Recovery list supports search by CVR number
    Given portal user "U1" is viewing the claims in recovery list
    When user "U1" searches by CVR number
    Then only claims matching the CVR number are displayed

  Scenario: Recovery list supports search by SE number
    Given portal user "U1" is viewing the claims in recovery list
    When user "U1" searches by SE number
    Then only claims matching the SE number are displayed

  Scenario: Recovery list supports date range filtering on modtagelsesdato
    Given portal user "U1" is viewing the claims in recovery list
    When user "U1" applies a date range filter from "01.01.2025" to "31.03.2025"
    Then only claims with modtagelsesdato within that range are displayed

  Scenario: Clicking a claim row navigates to the claim detail page
    Given portal user "U1" is viewing the claims in recovery list
    When user "U1" clicks a claim row
    Then the portal navigates to the claim detail page for that claim

  # --- Zero-balance claims list (FR 8-11) ---

  Scenario: Zero-balance list displays paginated zero-balance claims
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And user "U1" is bound to fordringshaver "K1"
    And fordringshaver "K1" has zero-balance claims in debt-service
    When user "U1" opens the zero-balance claims list
    Then the portal displays a paginated table of zero-balance claims for fordringshaver "K1"
    And the table uses the same column layout as the recovery list

  Scenario: Zero-balance list supports server-side sorting
    Given portal user "U1" is viewing the zero-balance claims list
    When user "U1" clicks a column header to sort
    Then the list is sorted by that column on the server side
    And sorting can be toggled between ascending and descending order

  Scenario: Zero-balance list supports search and date range filtering
    Given portal user "U1" is viewing the zero-balance claims list
    When user "U1" searches by Fordrings-ID, CPR, CVR, or SE number
    Then the list is filtered to matching claims
    And user "U1" can also filter by modtagelsesdato date range

  Scenario: Clicking a zero-balance claim navigates to the detail page
    Given portal user "U1" is viewing the zero-balance claims list
    When user "U1" clicks a zero-balance claim row
    Then the portal navigates to the claim detail page for that claim

  Scenario: Zero-balance claim older than 60 days shows appropriate message
    Given portal user "U1" is viewing the zero-balance claims list
    And a zero-balance claim reached zero balance more than 60 days ago
    When user "U1" navigates to the detail page for that claim
    Then the detail page displays an appropriate message about the claim age

  # --- Claims counts (FR 12-14) ---

  Scenario: Portal displays count of active claims in recovery
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And user "U1" is bound to fordringshaver "K1"
    When user "U1" views the claims count page
    Then the portal displays the count of active claims in recovery for fordringshaver "K1"

  Scenario: Portal displays count of zero-balance claims
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And user "U1" is bound to fordringshaver "K1"
    When user "U1" views the claims count page
    Then the portal displays the count of zero-balance claims for fordringshaver "K1"

  Scenario: Claims counts are filterable by date range
    Given portal user "U1" is viewing the claims count page
    When user "U1" selects a date range
    Then both the recovery count and the zero-balance count reflect the selected date range

  # --- Data loading (FR 15-17) ---

  Scenario: Claims data is loaded from debt-service through the BFF
    Given portal user "U1" opens the claims in recovery list
    When the portal loads claim data
    Then the data is retrieved from debt-service through the creditor-portal BFF
    And the portal does not query debt-service directly

  Scenario: HTMX progressive loading with loading indicator
    Given portal user "U1" opens a claims list page
    When the page shell renders
    Then the table body is loaded asynchronously via HTMX
    And a loading indicator is shown while the table data is being fetched
    And the loading indicator disappears once data is loaded

  Scenario: Pagination uses HTMX without full page reload
    Given portal user "U1" is viewing a claims list
    When user "U1" clicks a pagination control to go to the next page
    Then the table content is swapped via HTMX
    And a full page reload does not occur

  # --- Access control (FR 18-19) ---

  Scenario: CREDITOR_VIEWER can access claims lists and counts
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    When user "U1" opens the claims in recovery list
    Then access is granted
    And user "U1" can also access the zero-balance list and claims counts

  Scenario: CREDITOR_EDITOR can access claims lists and counts
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    When user "U2" opens the claims in recovery list
    Then access is granted
    And user "U2" can also access the zero-balance list and claims counts

  Scenario: CREDITOR_RECONCILIATION cannot access claims lists
    Given portal user "U3" is authenticated with only role "CREDITOR_RECONCILIATION"
    When user "U3" attempts to open the claims in recovery list
    Then access is denied
    And user "U3" cannot access the zero-balance list or claims counts

  Scenario: Unauthenticated user cannot access claims lists
    Given a user is not authenticated
    When the user attempts to open the claims in recovery list
    Then the user is redirected to the login page

  # --- Layout and accessibility (FR 20-26) ---

  Scenario: Claims list pages use the SKAT standardlayout
    Given portal user "U1" opens a claims list page
    Then the page uses the SKAT standardlayout from layout/default.html
    And the page includes a skip link, header, breadcrumb, main content area, and footer

  Scenario: Data tables use semantic HTML with accessible structure
    Given a claims list table is displayed
    Then the table uses semantic HTML elements: table, thead, tbody, th with scope="col"
    And the table has a proper caption or aria-label

  Scenario: Pagination controls are keyboard-accessible and screen-reader friendly
    Given a claims list with pagination is displayed
    When a user navigates pagination controls using the keyboard
    Then the controls are focusable and operable by keyboard
    And page changes are announced to screen readers

  Scenario: Monetary amounts are formatted in Danish locale
    Given a claims list is displayed
    Then monetary amounts are formatted with 2 decimal places
    And the comma is used as the decimal separator

  Scenario: Dates are formatted as dd.MM.yyyy
    Given a claims list is displayed
    Then all dates are formatted as dd.MM.yyyy

  Scenario: CPR numbers are censored in display
    Given a claims list contains claims with CPR-type debtors
    Then CPR numbers are displayed as the first 6 digits followed by "****"
    And the full CPR number is never shown in the list view

  Scenario: All user-facing text uses message bundles with Danish and English
    Given a claims list page is rendered
    Then all user-facing text is loaded from message bundles
    And Danish and English translations are available for all text
    And no hardcoded text appears in the template
