Feature: Creditor portal rejected claims list and detail view

  # --- Rejected claims list (FR 1-3) ---

  Scenario: Rejected claims list displays paginated rejected claims for the acting creditor
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And user "U1" is bound to fordringshaver "K1"
    And fordringshaver "K1" has rejected claims in debt-service
    When user "U1" opens the rejected claims list
    Then the portal displays a paginated table of rejected claims for fordringshaver "K1"

  Scenario: Rejected claims list uses the same patterns as other list views
    Given portal user "U1" is viewing the rejected claims list
    Then the list uses the same tabular, search, sort, and pagination patterns as the petition 029 list views

  Scenario: Rejected claims list supports server-side sorting by any column
    Given portal user "U1" is viewing the rejected claims list
    When user "U1" clicks a column header to sort
    Then the list is sorted by that column on the server side
    And sorting can be toggled between ascending and descending order

  Scenario: Rejected claims list supports search by Fordrings-ID
    Given portal user "U1" is viewing the rejected claims list
    When user "U1" searches by Fordrings-ID "FDR-60001"
    Then only claims matching Fordrings-ID "FDR-60001" are displayed

  Scenario: Rejected claims list supports search by CPR number
    Given portal user "U1" is viewing the rejected claims list
    When user "U1" searches by CPR number
    Then only claims matching the CPR number are displayed

  Scenario: Rejected claims list supports search by CVR number
    Given portal user "U1" is viewing the rejected claims list
    When user "U1" searches by CVR number
    Then only claims matching the CVR number are displayed

  Scenario: Rejected claims list supports search by SE number
    Given portal user "U1" is viewing the rejected claims list
    When user "U1" searches by SE number
    Then only claims matching the SE number are displayed

  Scenario: Rejected claims list supports date range filtering
    Given portal user "U1" is viewing the rejected claims list
    When user "U1" applies a date range filter from "01.01.2025" to "31.03.2025"
    Then only claims within that date range are displayed

  Scenario: Clicking a rejected claim row navigates to the detail view
    Given portal user "U1" is viewing the rejected claims list
    When user "U1" clicks a rejected claim row
    Then the portal navigates to the rejected claim detail page for that claim

  # --- Rejected claim detail view (FR 4) ---

  Scenario: Detail view displays aktionsstatus and afvisningsaarsag
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And user "U1" navigates to the rejected claim detail view for claim "FDR-60001"
    Then the detail view displays the aktionsstatus (action status)
    And the detail view displays the afvisningsaarsag (rejection reason text)

  Scenario: Detail view displays ID-numre section
    Given portal user "U1" is viewing the rejected claim detail view for claim "FDR-60001"
    Then the detail view displays: fordrings-ID and fordringshaver-reference

  Scenario: Detail view displays fordringsinformation section
    Given portal user "U1" is viewing the rejected claim detail view for claim "FDR-60001"
    Then the detail view displays: fordringstype, fordringshaver-beskrivelse, indberetningstidspunkt, periode, and stiftelsesdato

  Scenario: Detail view displays renteinformation section
    Given portal user "U1" is viewing the rejected claim detail view for claim "FDR-60001"
    Then the detail view displays: renteregelnummer (interest rule number) and rentesatskode (interest rate code)

  Scenario: Detail view displays fordringshaver-info section
    Given portal user "U1" is viewing the rejected claim detail view for claim "FDR-60001"
    Then the detail view displays: fordringshaver-ID and fordringshaver-navn

  Scenario: Detail view displays beloeb section
    Given portal user "U1" is viewing the rejected claim detail view for claim "FDR-60001"
    Then the detail view displays: oprindeligt beloeb (original amount) and fordringsbeloeb (claim amount)

  Scenario: Detail view displays fejlbeskrivelser with error codes
    Given portal user "U1" is viewing the rejected claim detail view for claim "FDR-60001"
    And claim "FDR-60001" has validation errors
    Then the detail view displays a list of fejlbeskrivelser (validation error descriptions)
    And each error displays both the numeric error code and the Danish description

  Scenario: Detail view displays sagsbehandler-bemaerkning when present
    Given portal user "U1" is viewing the rejected claim detail view for claim "FDR-60002"
    And claim "FDR-60002" has a sagsbehandler-bemaerkning (caseworker remark)
    Then the detail view displays the sagsbehandler-bemaerkning

  Scenario: Detail view hides sagsbehandler-bemaerkning when absent
    Given portal user "U1" is viewing the rejected claim detail view for claim "FDR-60003"
    And claim "FDR-60003" does not have a sagsbehandler-bemaerkning
    Then the sagsbehandler-bemaerkning section is not displayed

  # --- Debtor information in rejected claims (FR 5-7) ---

  Scenario: Detail view displays debtor list with required fields
    Given portal user "U1" is viewing the rejected claim detail view for claim "FDR-60001"
    And claim "FDR-60001" has debtors
    Then the detail view displays a debtor list
    And each debtor shows: skyldner-ID, forfaldsdato, sidste rettidige betalingsdato, and foraeldelsesdato

  Scenario: Debtor list displays domsdato when applicable
    Given portal user "U1" is viewing the rejected claim detail view for claim "FDR-60001"
    And a debtor has a court date (domsdato)
    Then the debtor entry displays the domsdato

  Scenario: Debtor list displays forligsdato when applicable
    Given portal user "U1" is viewing the rejected claim detail view for claim "FDR-60001"
    And a debtor has a settlement date (forligsdato)
    Then the debtor entry displays the forligsdato

  Scenario: Debtor list displays bobehandling flag
    Given portal user "U1" is viewing the rejected claim detail view for claim "FDR-60001"
    And a debtor has a bobehandling (estate processing) flag
    Then the debtor entry displays the bobehandling flag

  Scenario: Debtor list displays skyldner-note
    Given portal user "U1" is viewing the rejected claim detail view for claim "FDR-60001"
    And a debtor has a skyldner-note
    Then the debtor entry displays the skyldner-note

  Scenario: CPR numbers are censored in the debtor list
    Given the debtor list contains a debtor with a CPR identifier
    Then the CPR number is displayed as the first 6 digits followed by "****"
    And the full CPR number is never shown

  Scenario: Debtor list displays CVR, SE, and AKR identifiers uncensored
    Given the debtor list contains debtors with CVR, SE, and AKR identifiers
    Then CVR, SE, and AKR identifiers are displayed in full

  Scenario: Configurable flag controls debtor detail visibility
    Given the configurable debtor detail visibility flag is set to false
    When portal user "U1" views the rejected claim detail view
    Then the debtor details section is not displayed

  Scenario: Debtor details shown when configurable flag is true
    Given the configurable debtor detail visibility flag is set to true
    When portal user "U1" views the rejected claim detail view for a claim with debtors
    Then the debtor details section is displayed with all debtor information

  # --- Error code display (FR 8-9) ---

  Scenario: Error code displays numeric code and Danish description
    Given the rejected claim detail view displays validation errors
    Then each error shows the numeric error code followed by the Danish description
    # Example: "152 - Ugyldig valuta"

  Scenario: Error codes correspond to validation rules in petitions 015-018
    Given the rejected claim detail view displays validation errors
    Then the error codes map to the validation rules defined in petitions 015-018
    # Example: 152 = ugyldig valuta, 411 = forkert fordringsart

  # --- Access control (FR 10) ---

  Scenario: CREDITOR_VIEWER can access the rejected claims list and detail view
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    When user "U1" opens the rejected claims list
    Then access is granted
    And user "U1" can navigate to any rejected claim detail view

  Scenario: CREDITOR_EDITOR can access the rejected claims list and detail view
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    When user "U2" opens the rejected claims list
    Then access is granted
    And user "U2" can navigate to any rejected claim detail view

  Scenario: Unauthenticated user cannot access the rejected claims list
    Given a user is not authenticated
    When the user attempts to open the rejected claims list
    Then the user is redirected to the login page

  Scenario: User without CREDITOR_VIEWER or CREDITOR_EDITOR role is denied access
    Given portal user "U3" is authenticated without "CREDITOR_VIEWER" or "CREDITOR_EDITOR" roles
    When user "U3" attempts to open the rejected claims list
    Then access is denied

  # --- Data loading ---

  Scenario: Rejected claims data is loaded from debt-service through the BFF
    Given portal user "U1" opens the rejected claims list
    When the portal loads rejected claim data
    Then the data is retrieved from debt-service through the creditor-portal BFF
    And the portal does not query debt-service directly

  # --- Layout and accessibility (FR 11-14) ---

  Scenario: Rejected claims list page uses the SKAT standardlayout
    Given portal user "U1" opens the rejected claims list
    Then the page uses the SKAT standardlayout from layout/default.html
    And the page includes a skip link, header, breadcrumb, main content area, and footer

  Scenario: Rejected claim detail page uses the SKAT standardlayout with correct breadcrumb
    Given portal user "U1" navigates to the rejected claim detail view for claim "FDR-60001"
    Then the page uses the SKAT standardlayout from layout/default.html
    And the breadcrumb shows: Forside > Afviste fordringer > FDR-60001

  Scenario: Error descriptions use error alert styling
    Given portal user "U1" is viewing the rejected claim detail view
    And the claim has validation errors
    Then error descriptions are displayed using "skat-alert skat-alert--error" styling or similar error component

  Scenario: Data tables use semantic HTML with accessible structure
    Given the rejected claims list table is displayed
    Then the table uses semantic HTML elements: table, thead, tbody, th with scope="col"
    And the table has a proper caption or aria-label

  Scenario: CPR numbers are censored in the rejected claims list
    Given the rejected claims list contains claims with CPR-type debtors
    Then CPR numbers are displayed as the first 6 digits followed by "****"
    And the full CPR number is never shown in the list view

  Scenario: Monetary amounts are formatted in Danish locale
    Given the rejected claim detail view is displayed
    Then all monetary amounts are formatted with 2 decimal places
    And the comma is used as the decimal separator

  Scenario: Dates are formatted as dd.MM.yyyy
    Given the rejected claims list or detail view is displayed
    Then all dates are formatted as dd.MM.yyyy

  Scenario: All user-facing text uses message bundles with Danish and English
    Given a rejected claims page is rendered
    Then all user-facing text is loaded from message bundles
    And Danish and English translations are available for all text
    And no hardcoded text appears in the template
