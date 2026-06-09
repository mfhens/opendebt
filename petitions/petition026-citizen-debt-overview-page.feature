Feature: Authenticated citizen debt overview page
  Citizens use the internal "/min-gaeld" page after MitID/TastSelv authentication to
  view a localized and accessible snapshot of their debt, interest information,
  payment guidance, and contact information.
  Out of scope: payment processing, PDF generation, instalment plan management,
  debt objections, and detailed debt drilldown pages.

  Scenario: Unauthenticated citizen is redirected to MitID/TastSelv from the debt overview page
    Given a citizen is not authenticated
    When the citizen opens "/min-gaeld"
    Then the citizen is redirected to the MitID/TastSelv login flow

  Scenario: Landing page MitID call-to-action opens the internal debt overview after login
    Given the citizen portal landing page is available
    And the landing page MitID call-to-action points to "/min-gaeld"
    And the citizen is not authenticated
    When the citizen clicks the MitID call-to-action
    And the citizen completes the MitID/TastSelv login flow successfully
    Then the citizen arrives at "/min-gaeld"
    And the debt overview page is displayed

  Scenario: Debt overview loads debt data for the authenticated session person
    Given a citizen is authenticated
    And the citizen session contains person_id "person-123"
    And debt-service has a citizen debt summary for person_id "person-123"
    When the citizen opens "/min-gaeld"
    Then the page loads debt data by using session person_id "person-123"
    And only debt data for person_id "person-123" is displayed

  Scenario: Debt overview shows total outstanding amount and a semantic debt table
    Given a citizen is authenticated
    And debt-service returns one or more debts for the citizen
    When the citizen opens "/min-gaeld"
    Then the total outstanding debt amount is displayed prominently at the top of the page
    And the page displays a debt table with a caption
    And the debt table header contains debt type, creditor name, principal amount, outstanding balance, due date, and status
    And each debt row displays debt type, creditor name, principal amount, outstanding balance, due date, and status
    And the debt table uses a thead element
    And each column header uses a th element with scope "col"

  Scenario: Debt rows display the status returned for a debt
    Given a citizen is authenticated
    And debt-service returns a debt with a status value
    When the citizen opens "/min-gaeld"
    Then the debt row shows the returned status for that debt

  Scenario: Citizen with no outstanding debt sees a clear no-debt message
    Given a citizen is authenticated
    And debt-service returns no outstanding debt for the citizen
    When the citizen opens "/min-gaeld"
    Then the page displays a clear message that no debt was found
    And the no-debt message is communicated accessibly
    And an empty debt table is not displayed

  Scenario: Debt overview presents interest, snapshot, and contact explanations
    Given a citizen is authenticated
    And debt-service returns debts with accrued interest
    When the citizen opens "/min-gaeld"
    Then the page explains that interest accrues daily
    And the page explains that payments reduce accrued interest before principal
    And the page shows the current interest rate note
    And each debt displays interest information
    And the page explains that the overview is a snapshot taken at page load
    And the page explains that the actual balance may differ slightly from the displayed amount
    And the page displays the configured phone number and contact information for debt questions

  Scenario: Debt overview shows payment, PDF placeholder, and navigation links
    Given a citizen is authenticated
    When the citizen opens "/min-gaeld"
    Then the page contains a link or button to the configured external payment page
    And the page contains a placeholder affordance for PDF download or viewing
    And the PDF affordance indicates that the capability is a future enhancement
    And the page contains a link back to the landing page
    And the page does not initiate payment processing or debt modification

  Scenario Outline: Debt overview uses localized message bundles and locale-aware currency formatting
    Given a citizen is authenticated
    And debt-service returns a total outstanding amount of "12345.67" DKK
    When the citizen opens "/min-gaeld" with language "<language>"
    Then all user-facing text is loaded from message bundles for "<language>"
    And the page is rendered in language "<language>"
    And debt amounts are formatted as DKK for "<language>"

    Examples:
      | language |
      | da       |
      | en-GB    |

  Scenario: Debt overview is keyboard-navigable and screen-reader compatible
    Given a citizen is authenticated
    And debt-service returns one or more debts for the citizen
    When the citizen opens "/min-gaeld"
    Then the page is keyboard-navigable
    And the page is screen-reader compatible
    And the debt table is announced with its caption and column headers

  Scenario: Debt-service unavailability is communicated without exposing stack traces
    Given a citizen is authenticated
    And debt-service is temporarily unavailable
    When the citizen opens "/min-gaeld"
    Then the page displays a user-friendly service unavailable message
    And the error message is communicated accessibly
    And no stack trace is visible to the citizen

  Scenario: Debt overview works without client-side scripting
    Given a citizen is authenticated
    And the citizen browser does not execute JavaScript
    And debt-service returns one or more debts for the citizen
    When the citizen opens "/min-gaeld"
    Then the debt overview page is rendered successfully
    And the citizen can read the debt snapshot without JavaScript

  Scenario: Paused interest is explained when accrual is suspended for unclear debt
    Given a citizen is authenticated
    And debt-service returns a debt where interest accrual is paused because the claim is unclear and the debtor cannot pay
    When the citizen opens "/min-gaeld"
    Then the page shows that interest accrual is paused for that debt

  Scenario: Debt overview explains that recovery interest is not tax-deductible
    Given a citizen is authenticated
    And debt-service returns debts with recovery interest
    When the citizen opens "/min-gaeld"
    Then the page informs the citizen that recovery interest is not tax-deductible

  Scenario Outline: Written-off debt status explains why the debt was closed
    Given a citizen is authenticated
    And debt-service returns a debt that was written off because of "<writeOffReason>"
    When the citizen opens "/min-gaeld"
    Then the debt row shows status "WRITTEN_OFF"
    And the page shows explanatory sub-text for "<writeOffReason>"

    Examples:
      | writeOffReason                      |
      | limitation expired                  |
      | bankruptcy                          |
      | estate of deceased                  |
      | debt restructuring                  |
      | recovery deemed obviously futile    |
      | recovery costs disproportionate     |
