Feature: Creditor portal claims in hearing list and detail view

  # --- Hearing claims list (FR 1-4) ---

  Scenario: Hearing list displays paginated claims in hearing for the acting creditor
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And user "U1" is bound to fordringshaver "K1"
    And fordringshaver "K1" has claims in hearing in debt-service
    When user "U1" opens the claims in hearing list
    Then the portal displays a paginated table of claims in hearing for fordringshaver "K1"

  Scenario: Each hearing claim row displays all 10 required columns
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And the claims in hearing list is displayed
    Then each claim row displays: Fordrings-ID, indberetningstidspunkt, skyldner-type and skyldner-ID (CPR censored), antal skyldnere, fordringshaver-reference, fordringstype, fejl (error description), hoeringsstatus, sags-ID, and aktionskode

  Scenario: Fejl column shows single error text when one error exists
    Given portal user "U1" is viewing the claims in hearing list
    And a claim has exactly one validation error
    Then the fejl column for that claim displays the single error description text

  Scenario: Fejl column shows count when multiple errors exist
    Given portal user "U1" is viewing the claims in hearing list
    And a claim has 3 validation errors
    Then the fejl column for that claim displays "3 fejl"

  Scenario: Hearing list supports server-side sorting by any column
    Given portal user "U1" is viewing the claims in hearing list
    When user "U1" clicks a column header to sort
    Then the list is sorted by that column on the server side
    And sorting can be toggled between ascending and descending order

  Scenario: Hearing list supports search by Fordrings-ID
    Given portal user "U1" is viewing the claims in hearing list
    When user "U1" searches by Fordrings-ID "FDR-50001"
    Then only claims matching Fordrings-ID "FDR-50001" are displayed

  Scenario: Hearing list supports search by CPR number
    Given portal user "U1" is viewing the claims in hearing list
    When user "U1" searches by CPR number
    Then only claims matching the CPR number are displayed

  Scenario: Hearing list supports search by CVR number
    Given portal user "U1" is viewing the claims in hearing list
    When user "U1" searches by CVR number
    Then only claims matching the CVR number are displayed

  Scenario: Hearing list supports search by SE number
    Given portal user "U1" is viewing the claims in hearing list
    When user "U1" searches by SE number
    Then only claims matching the SE number are displayed

  Scenario: Hearing list supports date range filtering on indberetningstidspunkt
    Given portal user "U1" is viewing the claims in hearing list
    When user "U1" applies a date range filter from "01.01.2025" to "31.03.2025"
    Then only claims with indberetningstidspunkt within that range are displayed

  Scenario: Clicking a hearing claim row navigates to the hearing detail view
    Given portal user "U1" is viewing the claims in hearing list
    When user "U1" clicks a claim row
    Then the portal navigates to the hearing detail page for that claim

  # --- Hearing detail view (FR 5) ---

  Scenario: Hearing detail view displays fordringsstatus
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And user "U1" navigates to the hearing detail view for claim "FDR-50001"
    Then the detail view displays fordringsstatus mapped from status code to Danish text

  Scenario: Hearing detail view displays ID-numre section
    Given portal user "U1" is viewing the hearing detail view for claim "FDR-50001"
    Then the detail view displays: fordrings-ID, sags-ID, aktions-ID, fordringshaver-reference, and hovedfordrings-ID

  Scenario: Hearing detail view displays fordringsinformation section
    Given portal user "U1" is viewing the hearing detail view for claim "FDR-50001"
    Then the detail view displays: fordringstype, fordringshaver-beskrivelse, indberetningstidspunkt, periode, and stiftelsesdato

  Scenario: Hearing detail view displays fordringshaver-info section
    Given portal user "U1" is viewing the hearing detail view for claim "FDR-50001"
    Then the detail view displays: fordringshaver-ID and fordringshaver-navn

  Scenario: Hearing detail view displays beloeb section
    Given portal user "U1" is viewing the hearing detail view for claim "FDR-50001"
    Then the detail view displays: oprindelig hovedstol (original principal) and modtaget beloeb (received amount)

  Scenario: Hearing detail view displays aktionskode
    Given portal user "U1" is viewing the hearing detail view for claim "FDR-50001"
    Then the detail view displays the aktionskode (action code)

  Scenario: Hearing detail view displays skyldnerliste with fejltyper per skyldner
    Given portal user "U1" is viewing the hearing detail view for claim "FDR-50001"
    And claim "FDR-50001" has multiple debtors with errors
    Then the detail view displays a skyldnerliste (debtor list) with fejltyper (error types) per skyldner

  # --- Write-up hearing claims (FR 6-7) ---

  Scenario: Write-up fields are displayed when action code indicates opskrivning
    Given portal user "U1" is viewing the hearing detail view for claim "FDR-50002"
    And claim "FDR-50002" has action code "OPSKRIVNING_REGULERING"
    Then the detail view additionally displays: opskrivningsbeloeb (write-up amount), opskrivningsaarsag (write-up reason), and reference-aktions-ID

  Scenario: Write-up fields are hidden when action code does not indicate opskrivning
    Given portal user "U1" is viewing the hearing detail view for claim "FDR-50001"
    And claim "FDR-50001" does not have a write-up action code
    Then the detail view does not display opskrivningsbeloeb, opskrivningsaarsag, or reference-aktions-ID

  Scenario: FEJLAGTIG_HOVEDSTOL_INDBERETNING shows aendret oprindelig hovedstol
    Given portal user "U1" is viewing the hearing detail view for claim "FDR-50003"
    And claim "FDR-50003" has action code "FEJLAGTIG_HOVEDSTOL_INDBERETNING"
    Then the detail view displays: opskrivningsbeloeb, opskrivningsaarsag, reference-aktions-ID, and aendret oprindelig hovedstol (changed original principal)

  Scenario: OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING is recognised as write-up
    Given portal user "U1" is viewing the hearing detail view for a claim
    And the claim has action code "OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING"
    Then the detail view displays write-up fields: opskrivningsbeloeb, opskrivningsaarsag, and reference-aktions-ID

  Scenario: OPSKRIVNING_ANNULLERET_NEDSKRIVNING_INDBETALING is recognised as write-up
    Given portal user "U1" is viewing the hearing detail view for a claim
    And the claim has action code "OPSKRIVNING_ANNULLERET_NEDSKRIVNING_INDBETALING"
    Then the detail view displays write-up fields: opskrivningsbeloeb, opskrivningsaarsag, and reference-aktions-ID

  Scenario: Non-FEJLAGTIG action codes do not show aendret oprindelig hovedstol
    Given portal user "U1" is viewing the hearing detail view for a claim
    And the claim has action code "OPSKRIVNING_REGULERING"
    Then the detail view does not display aendret oprindelig hovedstol

  # --- Approve or withdraw hearing case (FR 8-12) ---

  Scenario: Editor approves a hearing claim with justification
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" permission from the creditor agreement
    And user "U2" is viewing the hearing detail view for claim "FDR-50001"
    When user "U2" approves the hearing claim with justification "Stamdata verified manually"
    Then the action is submitted to debt-service through the BFF
    And the claim status changes to "Afventer Gaeldsstyrelsen"

  Scenario: Editor withdraws a hearing claim with reason
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" permission from the creditor agreement
    And user "U2" is viewing the hearing detail view for claim "FDR-50001"
    When user "U2" withdraws the hearing claim with reason "Claim submitted in error"
    Then the action is submitted to debt-service through the BFF

  Scenario: Approve action triggers confirmation dialog before submission
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" permission
    And user "U2" has filled in the approval justification
    When user "U2" clicks the approve button
    Then a confirmation dialog is displayed before the action is submitted

  Scenario: Withdraw action triggers confirmation dialog before submission
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" permission
    And user "U2" has filled in the withdrawal reason
    When user "U2" clicks the withdraw button
    Then a confirmation dialog is displayed before the action is submitted

  Scenario: Approve action is logged to the audit log
    Given portal user "U2" approves a hearing claim
    Then the approval action is logged to the audit log
    And the audit log entry includes the user, claim ID, and justification

  Scenario: Withdraw action is logged to the audit log
    Given portal user "U2" withdraws a hearing claim
    Then the withdrawal action is logged to the audit log
    And the audit log entry includes the user, claim ID, and reason

  Scenario: Approval without justification is rejected with validation error
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" permission
    When user "U2" attempts to approve a hearing claim without providing a justification
    Then the form displays a validation error indicating that a justification is required

  Scenario: Withdrawal without reason is rejected with validation error
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" permission
    When user "U2" attempts to withdraw a hearing claim without providing a reason
    Then the form displays a validation error indicating that a reason is required

  # --- Hearing workflow context (FR 13-15) ---

  Scenario: Claim in hearing is not received for inddrivelse
    Given a claim is in hearing status
    Then the claim is not received for inddrivelse
    And the creditor's own foraeldelsesregler apply during the hearing period

  Scenario: Gaeldsstyrelsen treats approved claims within 14 days
    Given an editor has approved a hearing claim
    And the claim status is "Afventer Gaeldsstyrelsen"
    Then Gaeldsstyrelsen shall treat the claim within 14 days of approval

  Scenario: Gaeldsstyrelsen review results in godkendt outcome
    Given Gaeldsstyrelsen has reviewed an approved hearing claim
    When the outcome is godkendt (accepted)
    Then the claim status changes to accepted

  Scenario: Gaeldsstyrelsen review results in afvist outcome
    Given Gaeldsstyrelsen has reviewed an approved hearing claim
    When the outcome is afvist (rejected)
    Then the claim status changes to rejected

  Scenario: Gaeldsstyrelsen review results in tilpas indgangsfilter outcome
    Given Gaeldsstyrelsen has reviewed an approved hearing claim
    When the outcome is tilpas indgangsfilter (adjusted)
    Then the claim status changes to adjusted

  # --- Access control (FR 16-17) ---

  Scenario: CREDITOR_VIEWER can access the hearing list and detail view
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    When user "U1" opens the claims in hearing list
    Then access is granted
    And user "U1" can navigate to any hearing detail view

  Scenario: CREDITOR_EDITOR can access the hearing list and detail view
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    When user "U2" opens the claims in hearing list
    Then access is granted
    And user "U2" can navigate to any hearing detail view

  Scenario: CREDITOR_VIEWER cannot perform approve or withdraw actions
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And user "U1" is viewing the hearing detail view for claim "FDR-50001"
    Then the approve and withdraw action buttons are not displayed

  Scenario: CREDITOR_EDITOR without allow_portal_actions cannot approve or withdraw
    Given portal user "U4" is authenticated with role "CREDITOR_EDITOR"
    And user "U4" does not have "allow_portal_actions" permission from the creditor agreement
    When user "U4" is viewing the hearing detail view for claim "FDR-50001"
    Then the approve and withdraw action buttons are not displayed

  Scenario: CREDITOR_EDITOR with allow_portal_actions sees approve and withdraw buttons
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" permission from the creditor agreement
    When user "U2" is viewing the hearing detail view for claim "FDR-50001"
    Then the approve and withdraw action buttons are displayed

  Scenario: Unauthenticated user cannot access the hearing list
    Given a user is not authenticated
    When the user attempts to open the claims in hearing list
    Then the user is redirected to the login page

  Scenario: User without CREDITOR_VIEWER or CREDITOR_EDITOR role is denied access
    Given portal user "U3" is authenticated without "CREDITOR_VIEWER" or "CREDITOR_EDITOR" roles
    When user "U3" attempts to open the claims in hearing list
    Then access is denied

  # --- Data loading (FR 35-36) ---

  Scenario: Hearing claims data is loaded from debt-service through the BFF
    Given portal user "U1" opens the claims in hearing list
    When the portal loads hearing claim data
    Then the data is retrieved from debt-service through the creditor-portal BFF
    And the portal does not query debt-service directly

  Scenario: Approve and withdraw actions are submitted through the BFF
    Given portal user "U2" performs an approve or withdraw action
    Then the action is submitted to debt-service through the BFF
    And the portal does not call debt-service directly

  # --- Layout and accessibility (FR 18-22) ---

  Scenario: Hearing list page uses the SKAT standardlayout
    Given portal user "U1" opens the claims in hearing list
    Then the page uses the SKAT standardlayout from layout/default.html
    And the page includes a skip link, header, breadcrumb, main content area, and footer

  Scenario: Hearing detail page uses the SKAT standardlayout with correct breadcrumb
    Given portal user "U1" navigates to the hearing detail view for claim "FDR-50001"
    Then the page uses the SKAT standardlayout from layout/default.html
    And the breadcrumb shows: Forside > Fordringer i hoering > FDR-50001

  Scenario: Error descriptions are displayed in alert components
    Given portal user "U1" is viewing the hearing detail view for a claim with errors
    Then error descriptions are displayed in a clearly visible alert component

  Scenario: Approve/withdraw form has accessible labels and validation feedback
    Given portal user "U2" is viewing the approve/withdraw form on the hearing detail page
    Then the form fields have associated labels
    And validation errors display accessible feedback messages

  Scenario: CPR numbers are censored in the hearing list
    Given the hearing claims list contains claims with CPR-type debtors
    Then CPR numbers are displayed as the first 6 digits followed by "****"
    And the full CPR number is never shown in the list view

  Scenario: CPR numbers are censored in the hearing detail skyldnerliste
    Given the hearing detail view displays a skyldnerliste with CPR-type debtors
    Then CPR numbers are displayed as the first 6 digits followed by "****"
    And the full CPR number is never shown

  Scenario: Monetary amounts are formatted in Danish locale
    Given the hearing detail view is displayed
    Then all monetary amounts are formatted with 2 decimal places
    And the comma is used as the decimal separator

  Scenario: Dates are formatted as dd.MM.yyyy
    Given the hearing list or detail view is displayed
    Then all dates are formatted as dd.MM.yyyy

  Scenario: All user-facing text uses message bundles with Danish and English
    Given a hearing claims page is rendered
    Then all user-facing text is loaded from message bundles
    And Danish and English translations are available for all text
    And no hardcoded text appears in the template

  Scenario: Data tables use semantic HTML with accessible structure
    Given the hearing claims list table is displayed
    Then the table uses semantic HTML elements: table, thead, tbody, th with scope="col"
    And the table has a proper caption or aria-label
