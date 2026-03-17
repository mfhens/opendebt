Feature: Creditor portal claim creation wizard

  # --- Wizard structure (FR 1-3) ---

  Scenario: Wizard provides a multi-step form for creating a new claim
    Given portal user "U1" is authenticated with role "CREDITOR_EDITOR"
    And user "U1" is bound to fordringshaver "K1"
    And the creditor agreement for "K1" allows portal actions and claim creation
    When user "U1" opens the claim creation wizard
    Then the wizard is displayed with four steps: debtor identification, claim details entry, review, and submission

  Scenario: Wizard guides the creditor through steps in correct order
    Given portal user "U1" has opened the claim creation wizard
    Then the wizard starts at Step 1: debtor identification
    And the creditor must complete each step before advancing to the next

  Scenario: Wizard requires CREDITOR_EDITOR role and agreement permissions
    Given portal user "U1" is authenticated with role "CREDITOR_EDITOR"
    And the creditor agreement for "K1" has allow_portal_actions and allow_create_recovery_claims enabled
    When user "U1" opens the claim creation wizard
    Then access is granted

  Scenario: User without CREDITOR_EDITOR role cannot access the wizard
    Given portal user "U2" is authenticated with role "CREDITOR_VIEWER"
    When user "U2" attempts to open the claim creation wizard
    Then access is denied

  Scenario: User whose agreement disallows portal actions cannot access the wizard
    Given portal user "U1" is authenticated with role "CREDITOR_EDITOR"
    And the creditor agreement for "K1" has allow_portal_actions disabled
    When user "U1" attempts to open the claim creation wizard
    Then access is denied

  Scenario: User whose agreement disallows claim creation cannot access the wizard
    Given portal user "U1" is authenticated with role "CREDITOR_EDITOR"
    And the creditor agreement for "K1" has allow_create_recovery_claims disabled
    When user "U1" attempts to open the claim creation wizard
    Then access is denied

  # --- Step 1: Debtor validation (FR 4-7) ---

  Scenario: Step 1 allows selecting debtor type CPR, CVR, SE, or AKR
    Given portal user "U1" is on Step 1 of the claim creation wizard
    Then the debtor type selection offers: CPR, CVR, SE, and AKR

  Scenario: CPR debtor verification succeeds when names match
    Given portal user "U1" is on Step 1 and selects debtor type "CPR"
    When user "U1" enters a CPR number and a name matching the person-registry record (case-insensitive, accent-stripped)
    Then the BFF calls person-registry to verify the CPR
    And verification succeeds because both first and last names match

  Scenario: CPR debtor verification fails when names do not match
    Given portal user "U1" is on Step 1 and selects debtor type "CPR"
    When user "U1" enters a CPR number and a name that does not match the person-registry record
    Then verification fails
    And the wizard displays an error indicating the name mismatch

  Scenario: CPR name matching is case-insensitive and accent-stripped
    Given portal user "U1" is on Step 1 and selects debtor type "CPR"
    And the person-registry record has name "Søren Ålborg"
    When user "U1" enters name "soeren aalborg"
    Then verification succeeds because name matching is case-insensitive and accent-stripped

  Scenario: CPR lookups are throttled per user per birth date
    Given portal user "U1" is on Step 1 and selects debtor type "CPR"
    When user "U1" performs multiple CPR lookups for the same birth date in rapid succession
    Then lookups are throttled to prevent abuse
    And the wizard displays a throttle warning if the limit is exceeded

  Scenario: CVR debtor verification succeeds with valid CVR number
    Given portal user "U1" is on Step 1 and selects debtor type "CVR"
    When user "U1" enters a valid CVR number
    Then the BFF calls person-registry or external CVR service to verify
    And company information is returned and displayed

  Scenario: SE debtor verification succeeds with valid SE number
    Given portal user "U1" is on Step 1 and selects debtor type "SE"
    When user "U1" enters a valid SE number
    Then the BFF calls person-registry or external CVR service to verify
    And company information is returned and displayed

  Scenario: CVR/SE debtor verification fails with invalid number
    Given portal user "U1" is on Step 1 and selects debtor type "CVR"
    When user "U1" enters an invalid CVR number
    Then verification fails
    And the wizard displays an error indicating the invalid number

  Scenario: AKR debtor entry is accepted
    Given portal user "U1" is on Step 1 and selects debtor type "AKR"
    When user "U1" enters an AKR number
    Then the debtor entry is accepted
    And the wizard advances to Step 2

  # --- Step 2: Claim data entry (FR 8-10) ---

  Scenario: Claim data entry form presents fields based on creditor agreement
    Given portal user "U1" has completed Step 1 and is on Step 2
    And the creditor agreement for "K1" allows specific claim types
    Then the form presents fordringstype options limited to those allowed by the agreement
    And the form displays fields: beloeb, hovedstol, fordringshaver-reference, beskrivelse, foraeldelsesdato, and bobehandling

  Scenario: Conditional fields are shown based on fordringstype configuration
    Given portal user "U1" is on Step 2
    And the selected fordringstype requires fordringsperiode and stiftelsesdato
    Then the form displays fordringsperiode (from-to dates) and stiftelsesdato
    And fields not required by the fordringstype are hidden

  Scenario: Optional fields are available when applicable
    Given portal user "U1" is on Step 2
    Then the form offers optional fields: domsdato, forligsdato, rentevalg, fordringsnote, and kundenote

  Scenario: Field visibility and requirement levels driven by fordringstype
    Given portal user "U1" is on Step 2
    When user "U1" selects a fordringstype
    Then field visibility and requirement levels change according to the fordringstype configuration in the creditor agreement

  Scenario: Interest rule options limited to creditor agreement
    Given portal user "U1" is on Step 2
    Then the rentevalg options are limited to interest rules allowed by the creditor agreement

  Scenario: Beskrivelse field enforces 100-character limit with no PII
    Given portal user "U1" is on Step 2
    Then the beskrivelse field has a maximum of 100 characters
    And a character counter is displayed showing remaining characters
    And a GDPR warning advises against entering PII

  # --- Step 3: Review and submission (FR 11-14) ---

  Scenario: Review step displays read-only summary of all entered data
    Given portal user "U1" has completed Step 2 and is on Step 3
    Then the wizard displays a read-only summary of all entered data
    And the summary is not editable

  Scenario: Creditor must explicitly confirm submission
    Given portal user "U1" is on Step 3 and has reviewed the summary
    When user "U1" clicks the submit button
    Then the creditor is required to confirm submission explicitly before data is sent

  Scenario: BFF submits the claim to debt-service
    Given portal user "U1" has confirmed submission on Step 3
    When the submission is processed
    Then the BFF submits the claim to debt-service
    And the portal does not call debt-service directly

  Scenario: debt-service evaluates the claim against indrivelsesparathed rules
    Given the BFF has submitted the claim to debt-service
    Then debt-service evaluates the claim against indrivelsesparathed rules (petitions 015-018)
    And the evaluation result is returned to the BFF

  # --- Submission outcomes (FR 15-17) ---

  Scenario: Accepted claim (UDFOERT) displays receipt page
    Given portal user "U1" has submitted a claim
    And debt-service accepts the claim with status UDFOERT
    Then the wizard displays a receipt page
    And the receipt shows the assigned fordrings-ID
    And the receipt shows the processing status
    And the receipt shows a summary of submitted data

  Scenario: Rejected claim (AFVIST) displays validation errors
    Given portal user "U1" has submitted a claim
    And debt-service rejects the claim with status AFVIST
    Then the wizard displays validation errors with error codes and Danish descriptions
    And the creditor can correct the entered data and resubmit

  Scenario: Claim in hearing (HOERING) informs the creditor
    Given portal user "U1" has submitted a claim
    And debt-service returns status HOERING
    Then the wizard informs the creditor that the claim is pending review
    And the wizard directs the creditor to the hearing list (petition 031)

  # --- Creditor agreement integration (FR 18-20) ---

  Scenario: Creditor agreement is loaded from creditor-service through the BFF
    Given portal user "U1" opens the claim creation wizard
    When the wizard initializes
    Then the creditor agreement is loaded from creditor-service through the BFF
    And the portal does not call creditor-service directly

  Scenario: Creditor agreement determines wizard behaviour
    Given the creditor agreement for "K1" is loaded
    Then the agreement determines allowed claim types
    And the agreement determines allowed debtor types
    And the agreement determines interest rules and rates
    And the agreement determines whether portal actions are allowed

  Scenario: Creditor agreement is cacheable with manual refresh
    Given the creditor agreement for "K1" has been loaded
    Then the agreement data is cached
    And the creditor can manually refresh the agreement data

  # --- Navigation visibility (FR 21) ---

  Scenario: Navigation item visible for authorized users
    Given portal user "U1" is authenticated with role "CREDITOR_EDITOR"
    And the creditor agreement allows portal actions and claim creation
    When the portal navigation is rendered
    Then the "Opret fordring" navigation item is visible

  Scenario: Navigation item hidden when user lacks role
    Given portal user "U2" is authenticated with role "CREDITOR_VIEWER"
    When the portal navigation is rendered
    Then the "Opret fordring" navigation item is not visible

  Scenario: Navigation item hidden when agreement disallows actions
    Given portal user "U1" is authenticated with role "CREDITOR_EDITOR"
    And the creditor agreement does not allow portal actions
    When the portal navigation is rendered
    Then the "Opret fordring" navigation item is not visible

  # --- Audit logging (FR 22) ---

  Scenario: Claim submission is logged to audit log
    Given portal user "U1" has submitted a claim
    Then the submission is logged to the audit log with the full payload
    And PII is excluded from the audit log entry

  # --- Layout and accessibility (FR 23-30) ---

  Scenario: Wizard uses the SKAT standardlayout
    Given portal user "U1" opens the claim creation wizard
    Then the page uses the SKAT standardlayout from layout/default.html
    And the page includes a skip link, header, breadcrumb, main content area, and footer

  Scenario: Breadcrumb shows Forside > Opret fordring
    Given portal user "U1" is on any step of the claim creation wizard
    Then the breadcrumb shows: Forside > Opret fordring

  Scenario: Step indicator shows current step and total steps
    Given portal user "U1" is on Step 2 of the claim creation wizard
    Then a step indicator is displayed showing "Step 2 of 4" (or equivalent)
    And the indicator is accessible to screen readers

  Scenario: Form steps use accessible patterns
    Given portal user "U1" is on a form step of the wizard
    Then all form fields have visible labels
    And all form fields have descriptive help text where applicable
    And inline validation errors are linked to fields via aria-describedby

  Scenario: CPR input field uses numeric inputmode and pattern
    Given portal user "U1" is on Step 1 and selects debtor type "CPR"
    Then the CPR input field has inputmode="numeric"
    And the CPR input field has a pattern attribute for numeric entry

  Scenario: Beskrivelse field displays a character counter
    Given portal user "U1" is on Step 2
    Then the beskrivelse field displays a character counter indicating the remaining characters out of 100

  Scenario: Review step is read-only
    Given portal user "U1" is on Step 3 (review)
    Then all data is displayed as read-only summary text
    And no form input elements are editable

  Scenario: All user-facing text uses message bundles with Danish and English
    Given the claim creation wizard is rendered
    Then all user-facing text is loaded from message bundles
    And Danish and English translations are available for all text
    And no hardcoded text appears in the templates
