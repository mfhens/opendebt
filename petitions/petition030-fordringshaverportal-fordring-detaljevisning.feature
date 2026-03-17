Feature: Creditor portal claim detail view

  # --- Claim information section (FR 1-3) ---

  Scenario: Detail view displays claim information fields
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And user "U1" navigates to the detail view for claim "FDR-100"
    Then the detail view displays: fordringstype, fordringskategori, fordringshaver-beskrivelse, modtagelsesdato, periode (from-to dates), stiftelsesdato, fordrings-ID, obligations-ID, and fordringshaver-reference

  Scenario: Single-debtor claim displays additional date fields
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And claim "FDR-101" has a single debtor
    When user "U1" navigates to the detail view for claim "FDR-101"
    Then the detail view additionally displays: forfaldsdato, foraeldelsesdato, and sidste rettidige betalingsdag

  Scenario: Single-debtor claim displays retsdato when applicable
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And claim "FDR-102" has a single debtor and a court date
    When user "U1" navigates to the detail view for claim "FDR-102"
    Then the detail view displays retsdato

  Scenario: Multi-debtor claim does not display single-debtor-only fields
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And claim "FDR-103" has multiple debtors
    When user "U1" navigates to the detail view for claim "FDR-103"
    Then the detail view does not display forfaldsdato, foraeldelsesdato, or sidste rettidige betalingsdag

  Scenario: Detail view displays fordringskategori and related obligations-ID
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And claim "FDR-104" is a sub-claim with fordringskategori and a related obligations-ID
    When user "U1" navigates to the detail view for claim "FDR-104"
    Then the detail view displays the fordringskategori (HOVEDFORDRING or sub-category)
    And the detail view displays the related obligations-ID

  # --- Financial information section (FR 4-6) ---

  Scenario: Detail view displays financial breakdown table with debt category rows
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And user "U1" navigates to the detail view for claim "FDR-100"
    Then the financial breakdown table shows rows per debt category
    And each debt category row shows: original amount, write-off amount, payment amount, and balance

  Scenario: Financial breakdown table includes recovery interest row
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    Then the financial breakdown table includes a row for inddrivelsesrenter (recovery interest)

  Scenario: Financial breakdown table includes collection charges row
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    Then the financial breakdown table includes a row for inddrivelsesomkostninger (collection charges) from related claims

  Scenario: Financial breakdown table includes collection interest sent for recovery
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    Then the financial breakdown table includes a row for opkraevningsrenter sendt til inddrivelse (collection interest sent for recovery)

  Scenario: Financial breakdown table includes total current balance row
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    Then the financial breakdown table includes a total current balance row

  Scenario: Detail view displays interest rule and rate information
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    Then the detail view displays renteregel (interest rule) and rentesats (interest rate)

  Scenario: Detail view displays extra interest rate when applicable
    Given portal user "U1" is viewing the detail view for a claim with an extra interest rate
    Then the detail view displays ekstra rentesats (extra interest rate)

  Scenario: Detail view displays aggregated financial fields
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    Then the detail view displays: total gaeld (total debt), seneste rentetilskrivningsdato (latest interest accrual date), oprindelig hovedstol (original principal), modtaget beloeb (received amount), fordringssaldo (claim balance), samlet fordringshaver-saldo (total claimant balance), and beloeb indsendt til inddrivelse (with and without write-ups)

  # --- Write-ups section (FR 7-9) ---

  Scenario: Detail view displays write-ups list with all required columns
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    And claim "FDR-100" has write-ups
    Then the detail view displays a list of all write-ups
    And each write-up shows: aktions-ID, reference-aktions-ID, formtype, aarsag (reason), beloeb (amount), virkningsdato (effective date), and skyldner-ID

  Scenario: Annulled write-ups are visually flagged
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    And claim "FDR-100" has annulled write-ups
    Then annulled write-ups are visually flagged in the write-ups list

  Scenario: Write-ups are sorted by aktions-ID
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    And claim "FDR-100" has multiple write-ups
    Then the write-ups list is sorted by aktions-ID

  # --- Write-downs section (FR 10) ---

  Scenario: Detail view displays write-downs list with all required columns
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    And claim "FDR-100" has write-downs
    Then the detail view displays a list of all write-downs
    And each write-down shows: aktions-ID, reference-aktions-ID, formtype, aarsagskode (reason code), beloeb, virkningsdato, and skyldner-ID

  # --- Related claims section (FR 11-12) ---

  Scenario: Detail view lists related claims when present
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    And claim "FDR-100" has related claims (underfordringer)
    Then the detail view lists related claims with summary information

  Scenario: Clicking a related claim navigates to its detail view
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    And the related claims section lists claim "FDR-200"
    When user "U1" clicks on related claim "FDR-200"
    Then the portal navigates to the detail view for claim "FDR-200"

  Scenario: Detail view without related claims does not show related claims section
    Given portal user "U1" is viewing the detail view for claim "FDR-105"
    And claim "FDR-105" has no related claims
    Then the related claims section is not displayed

  # --- Debtor information section (FR 13-14) ---

  Scenario: Detail view lists all debtors from haeftelsesstruktur
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    And claim "FDR-100" has debtors associated via the haeftelsesstruktur
    Then the detail view lists all debtors associated with the claim

  Scenario: Each debtor shows their identifier type
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    And the claim has debtors with CPR, CVR, SE, and AKR identifiers
    Then each debtor displays their identifier (CPR censored, CVR, SE, or AKR)

  Scenario: CPR numbers are always censored in the debtor section
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    And a debtor has a CPR identifier
    Then the CPR number is displayed as the first 6 digits followed by "****"
    And the full CPR number is never shown

  # --- Decisions section (FR 15-16) ---

  Scenario: Single-debtor claim displays court decisions and settlements
    Given portal user "U1" is viewing the detail view for claim "FDR-101"
    And claim "FDR-101" has a single debtor
    And the claim has court decisions and settlements
    Then the detail view displays court decisions (dom) with their dates
    And the detail view displays settlements (forlig) with their dates

  Scenario: Multi-debtor claim does not display the decisions section
    Given portal user "U1" is viewing the detail view for claim "FDR-103"
    And claim "FDR-103" has multiple debtors
    Then the decisions section is not displayed

  # --- Receipt retrieval (FR 17-18) ---

  Scenario: User can fetch a receipt for a claim operation
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    And claim "FDR-100" has a receipt with delivery ID "DLV-001"
    When user "U1" requests the receipt for delivery ID "DLV-001"
    Then the portal retrieves the receipt from debt-service using the delivery ID
    And the receipt is displayed or downloaded

  # --- Error handling (FR 19-20) ---

  Scenario: Zero-balance claim past 60 days shows data unavailable message
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And claim "FDR-106" has had zero balance for more than 60 days
    When user "U1" navigates to the detail view for claim "FDR-106"
    Then the detail view displays a message informing the user that detailed data is no longer available

  Scenario: Service error from debt-service displays user-friendly Danish message
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And debt-service returns a service error when fetching claim details
    When user "U1" navigates to the detail view for the claim
    Then the detail view displays a user-friendly error message in Danish

  # --- Access control (FR 21) ---

  Scenario: CREDITOR_VIEWER can access the claim detail view
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    When user "U1" navigates to the detail view for claim "FDR-100"
    Then access is granted
    And the detail view is displayed

  Scenario: CREDITOR_EDITOR can access the claim detail view
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    When user "U2" navigates to the detail view for claim "FDR-100"
    Then access is granted
    And the detail view is displayed

  Scenario: Unauthenticated user cannot access the claim detail view
    Given a user is not authenticated
    When the user attempts to navigate to the detail view for claim "FDR-100"
    Then the user is redirected to the login page

  Scenario: User without CREDITOR_VIEWER or CREDITOR_EDITOR role is denied access
    Given portal user "U3" is authenticated without "CREDITOR_VIEWER" or "CREDITOR_EDITOR" roles
    When user "U3" attempts to navigate to the detail view for claim "FDR-100"
    Then access is denied

  # --- Data loading (FR 26) ---

  Scenario: Claim detail data is loaded from debt-service through the BFF
    Given portal user "U1" navigates to the detail view for claim "FDR-100"
    When the portal loads claim detail data
    Then the data is retrieved from debt-service through the creditor-portal BFF
    And the portal does not query debt-service directly

  # --- Layout and accessibility (FR 22-27) ---

  Scenario: Detail page uses the SKAT standardlayout with correct breadcrumb
    Given portal user "U1" navigates to the detail view for claim "FDR-100"
    Then the page uses the SKAT standardlayout from layout/default.html
    And the breadcrumb shows: Forside > Fordringer > FDR-100

  Scenario: Write-ups section is collapsible
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    And the claim has write-ups
    Then the write-ups section uses a collapsible details/summary element

  Scenario: Write-downs section is collapsible
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    And the claim has write-downs
    Then the write-downs section uses a collapsible details/summary element

  Scenario: Related claims section is collapsible
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    And the claim has related claims
    Then the related claims section uses a collapsible details/summary element

  Scenario: Financial tables use semantic HTML with scope attributes
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    Then the financial breakdown table uses semantic HTML elements: table, thead, tbody, th with scope attributes
    And the table has a proper caption or aria-label

  Scenario: Monetary amounts are formatted in Danish locale
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    Then all monetary amounts are formatted with 2 decimal places
    And the comma is used as the decimal separator

  Scenario: Dates are formatted as dd.MM.yyyy
    Given portal user "U1" is viewing the detail view for claim "FDR-100"
    Then all dates are formatted as dd.MM.yyyy

  Scenario: All user-facing text uses message bundles with Danish and English
    Given the claim detail page is rendered for claim "FDR-100"
    Then all user-facing text is loaded from message bundles
    And Danish and English translations are available for all text
    And no hardcoded text appears in the template
