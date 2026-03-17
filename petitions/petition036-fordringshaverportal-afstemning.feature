Feature: Creditor portal reconciliation list, detail, and response submission

  # --- Reconciliation list (FR 1-3) ---

  Scenario: Reconciliation list displays reconciliation periods for the acting creditor
    Given portal user "U1" is authenticated with role "CREDITOR_RECONCILIATION"
    And user "U1" is bound to fordringshaver "K1"
    And fordringshaver "K1" has reconciliation periods in debt-service
    When user "U1" opens the reconciliation list page
    Then the portal displays a list of reconciliation periods for fordringshaver "K1"

  Scenario: Reconciliation list supports filtering by status
    Given portal user "U1" is viewing the reconciliation list
    When user "U1" filters by status "ACTIVE"
    Then only reconciliation periods with status "ACTIVE" are displayed

  Scenario: Reconciliation list supports filtering by period end date range
    Given portal user "U1" is viewing the reconciliation list
    When user "U1" filters by period end date from "01.01.2025" to "31.03.2025"
    Then only reconciliation periods with period end date within that range are displayed

  Scenario: Reconciliation list supports filtering by reconciliation start date range
    Given portal user "U1" is viewing the reconciliation list
    When user "U1" filters by reconciliation start date from "01.01.2025" to "28.02.2025"
    Then only reconciliation periods with reconciliation start date within that range are displayed

  Scenario: Reconciliation list supports filtering by reconciliation end date range
    Given portal user "U1" is viewing the reconciliation list
    When user "U1" filters by reconciliation end date from "01.02.2025" to "31.03.2025"
    Then only reconciliation periods with reconciliation end date within that range are displayed

  Scenario: Each reconciliation entry shows summary information
    Given portal user "U1" is viewing the reconciliation list
    Then each reconciliation entry displays status, period, and whether a response has been submitted

  # --- Reconciliation detail view (FR 4-5) ---

  Scenario: Detail view displays reconciliation status, year/month, and previous response
    Given portal user "U1" is authenticated with role "CREDITOR_RECONCILIATION"
    And a reconciliation period exists for fordringshaver "K1"
    When user "U1" opens the reconciliation detail view
    Then the detail view displays reconciliation status
    And the detail view displays the year and month of the reconciliation period
    And the detail view displays the previous response if one has been submitted

  Scenario: Detail view for ACTIVE reconciliation displays basis data
    Given portal user "U1" is viewing the detail view for an ACTIVE reconciliation
    Then the detail view displays tilgang (influx amount)
    And the detail view displays tilbagekaldt (recall amount)
    And the detail view displays opskrevet (write-up amount)
    And the detail view displays nedskrevet (write-down amount)

  Scenario: Detail view for CLOSED reconciliation does not show basis data
    Given portal user "U1" is viewing the detail view for a CLOSED reconciliation
    Then the detail view does not display basis data fields
    And the detail view displays the previous response

  # --- Basis data (FR 6-8) ---

  Scenario: Basis data is retrieved from OpenDebt storage backend
    Given portal user "U1" opens the detail view for an ACTIVE reconciliation
    When basis data is loaded
    Then the data is retrieved from OpenDebt's storage backend through the BFF

  Scenario: Amounts are displayed in DKK with 2 decimal places
    Given portal user "U1" is viewing the detail view for an ACTIVE reconciliation
    Then all basis data amounts are displayed in DKK with 2 decimal places

  Scenario: No basis data exists for the period
    Given portal user "U1" is viewing the detail view for an ACTIVE reconciliation
    And no basis data exists for that period
    Then the view displays zero amounts for tilgang, tilbagekaldt, opskrevet, and nedskrevet

  # --- Reconciliation response submission (FR 9-14) ---

  Scenario: Creditor submits a reconciliation response for an ACTIVE reconciliation
    Given portal user "U1" is authenticated with role "CREDITOR_RECONCILIATION"
    And user "U1" is viewing the detail view for an ACTIVE reconciliation
    When user "U1" fills in forklaret difference as "1000.00"
    And user "U1" fills in uforklaret difference as "500.00"
    And user "U1" fills in total difference as "1500.00"
    And user "U1" submits the reconciliation response
    Then the response is submitted to debt-service through the BFF
    And the response is recorded successfully

  Scenario: Validation enforces forklaret + uforklaret == total
    Given portal user "U1" is viewing the response form for an ACTIVE reconciliation
    When user "U1" fills in forklaret difference as "1000.00"
    And user "U1" fills in uforklaret difference as "500.00"
    And user "U1" fills in total difference as "2000.00"
    And user "U1" attempts to submit the reconciliation response
    Then the form displays a validation error indicating forklaret + uforklaret must equal total
    And the response is not submitted

  Scenario: Validation passes when forklaret + uforklaret equals total
    Given portal user "U1" is viewing the response form for an ACTIVE reconciliation
    When user "U1" fills in forklaret difference as "750.00"
    And user "U1" fills in uforklaret difference as "250.00"
    And user "U1" fills in total difference as "1000.00"
    And user "U1" submits the reconciliation response
    Then the validation passes
    And the response is submitted successfully

  Scenario: Basis data is tamper-protected
    Given portal user "U1" submits a reconciliation response
    When the BFF receives the response
    Then the BFF verifies the basis data has not been modified client-side
    And if the basis data has been tampered with the response is rejected

  Scenario: Only ACTIVE reconciliations accept response submissions
    Given portal user "U1" is viewing the detail view for a CLOSED reconciliation
    Then the response submission form is not available
    And user "U1" cannot submit a response

  Scenario: Response submitted through BFF to debt-service
    Given portal user "U1" submits a valid reconciliation response
    Then the response is forwarded by the BFF to debt-service
    And the portal does not submit directly to debt-service

  Scenario: Service errors are propagated as Danish messages
    Given portal user "U1" submits a reconciliation response
    And debt-service returns an error
    Then the error is propagated to the user as a Danish message

  Scenario: Confirmation step before response submission
    Given portal user "U1" has filled in the response form for an ACTIVE reconciliation
    When user "U1" clicks the submit button
    Then a confirmation step is displayed summarizing the response
    And user "U1" must confirm before the response is actually submitted

  # --- Access control (FR 15-16) ---

  Scenario: CREDITOR_RECONCILIATION can access reconciliation list and detail
    Given portal user "U1" is authenticated with role "CREDITOR_RECONCILIATION"
    When user "U1" opens the reconciliation list page
    Then access is granted
    And user "U1" can also access the reconciliation detail view

  Scenario: CREDITOR_SUPPORT can access reconciliation list and detail
    Given portal user "U2" is authenticated with role "CREDITOR_SUPPORT"
    When user "U2" opens the reconciliation list page
    Then access is granted
    And user "U2" can also access the reconciliation detail view

  Scenario: CREDITOR_SUPPORT cannot submit a reconciliation response
    Given portal user "U2" is authenticated with only role "CREDITOR_SUPPORT"
    And user "U2" is viewing the detail view for an ACTIVE reconciliation
    Then the response submission form is not available to user "U2"

  Scenario: CREDITOR_RECONCILIATION can submit a reconciliation response
    Given portal user "U1" is authenticated with role "CREDITOR_RECONCILIATION"
    And user "U1" is viewing the detail view for an ACTIVE reconciliation
    Then the response submission form is available to user "U1"

  Scenario: User without CREDITOR_RECONCILIATION or CREDITOR_SUPPORT is denied access
    Given portal user "U3" is authenticated with only role "CREDITOR_VIEWER"
    When user "U3" attempts to open the reconciliation list page
    Then access is denied

  Scenario: Unauthenticated user cannot access reconciliation pages
    Given a user is not authenticated
    When the user attempts to open the reconciliation list page
    Then the user is redirected to the login page

  # --- Layout and accessibility (FR 17-21) ---

  Scenario: Reconciliation pages use the SKAT standardlayout
    Given portal user "U1" opens the reconciliation list page
    Then the page uses the SKAT standardlayout from layout/default.html
    And the page includes a skip link, header, breadcrumb, main content area, and footer

  Scenario: Breadcrumb shows Forside > Afstemning on the list page
    Given portal user "U1" is on the reconciliation list page
    Then the breadcrumb shows: Forside > Afstemning

  Scenario: Breadcrumb shows Forside > Afstemning > [Period] on the detail page
    Given portal user "U1" is viewing the detail view for a reconciliation period "2025-03"
    Then the breadcrumb shows: Forside > Afstemning > 2025-03

  Scenario: Response form uses accessible patterns with labels and validation feedback
    Given portal user "U1" is viewing the response form for an ACTIVE reconciliation
    Then each input field has a visible, associated label
    And validation errors are displayed as inline feedback next to the relevant fields
    And error messages are associated with inputs via aria-describedby

  Scenario: Financial tables use semantic HTML with proper scope attributes
    Given the reconciliation detail page displays basis data
    Then the financial table uses semantic HTML elements: table, thead, tbody, th with scope attributes
    And the table has a proper caption or aria-label

  Scenario: All user-facing text uses message bundles with Danish and English
    Given a reconciliation page is rendered
    Then all user-facing text is loaded from message bundles
    And Danish and English translations are available for all text
    And no hardcoded text appears in the template
