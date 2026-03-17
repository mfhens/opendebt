Feature: Creditor portal write-up and write-down on existing claims

  # --- Update claim submission (FR 1-3) ---

  Scenario: Editor can initiate a write-up from the claim detail page
    Given portal user "U1" is authenticated with role "CREDITOR_EDITOR"
    And user "U1" is viewing the detail page for claim "FDR-100"
    And the creditor agreement allows write-up operations
    When user "U1" clicks the write-up action button
    Then the write-up form is displayed

  Scenario: Editor can initiate a write-down from the claim detail page
    Given portal user "U1" is authenticated with role "CREDITOR_EDITOR"
    And user "U1" is viewing the detail page for claim "FDR-100"
    And the creditor agreement allows write-down operations
    When user "U1" clicks the write-down action button
    Then the write-down form is displayed

  Scenario: Updates are submitted to debt-service through the BFF
    Given portal user "U1" has completed a write-up form for claim "FDR-100"
    When the update is submitted
    Then the update is sent to debt-service through the BFF
    And the portal does not call debt-service directly

  # --- Supported update types (FR 4-5) ---

  Scenario: Portal supports write-down type NEDSKRIV
    Given portal user "U1" is on the write-down form for claim "FDR-100"
    And the creditor agreement allows NEDSKRIV
    Then "NEDSKRIV" is available as an update type

  Scenario: Portal supports write-down type NEDSKRIVNING_INDBETALING
    Given portal user "U1" is on the write-down form for claim "FDR-100"
    And the creditor agreement allows NEDSKRIVNING_INDBETALING
    Then "NEDSKRIVNING_INDBETALING" is available as an update type

  Scenario: Portal supports write-down type NEDSKRIVNING_ANNULLERET_OPSKRIVNING_REGULERING
    Given portal user "U1" is on the write-down form for claim "FDR-100"
    And the creditor agreement allows NEDSKRIVNING_ANNULLERET_OPSKRIVNING_REGULERING
    Then "NEDSKRIVNING_ANNULLERET_OPSKRIVNING_REGULERING" is available as an update type

  Scenario: Portal supports write-down type NEDSKRIVNING_ANNULLERET_OPSKRIVNING_INDBETALING
    Given portal user "U1" is on the write-down form for claim "FDR-100"
    And the creditor agreement allows NEDSKRIVNING_ANNULLERET_OPSKRIVNING_INDBETALING
    Then "NEDSKRIVNING_ANNULLERET_OPSKRIVNING_INDBETALING" is available as an update type

  Scenario: Portal supports write-up type OPSKRIVNING_REGULERING
    Given portal user "U1" is on the write-up form for claim "FDR-100"
    And the creditor agreement allows OPSKRIVNING_REGULERING
    Then "OPSKRIVNING_REGULERING" is available as an update type

  Scenario: Portal supports write-up type OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING
    Given portal user "U1" is on the write-up form for claim "FDR-100"
    And the creditor agreement allows OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING
    Then "OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING" is available as an update type

  Scenario: Portal supports write-up type OPSKRIVNING_ANNULLERET_NEDSKRIVNING_INDBETALING
    Given portal user "U1" is on the write-up form for claim "FDR-100"
    And the creditor agreement allows OPSKRIVNING_ANNULLERET_NEDSKRIVNING_INDBETALING
    Then "OPSKRIVNING_ANNULLERET_NEDSKRIVNING_INDBETALING" is available as an update type

  Scenario: Portal supports write-up type FEJLAGTIG_HOVEDSTOL_INDBERETNING
    Given portal user "U1" is on the write-up form for claim "FDR-100"
    And the creditor agreement allows FEJLAGTIG_HOVEDSTOL_INDBERETNING
    Then "FEJLAGTIG_HOVEDSTOL_INDBERETNING" is available as an update type

  Scenario: Update types filtered by creditor agreement permission flags
    Given portal user "U1" is on the update form for claim "FDR-100"
    And the creditor agreement allows only NEDSKRIV and OPSKRIVNING_REGULERING
    Then only "NEDSKRIV" and "OPSKRIVNING_REGULERING" are available as update types
    And other update types are not shown

  # --- Update form fields (FR 6-7) ---

  Scenario: Update form requires beloeb, virkningsdato, and aarsag
    Given portal user "U1" is on the write-up form for claim "FDR-100"
    Then the form requires beloeb (amount)
    And the form requires virkningsdato (effective date)
    And the form requires aarsag/aarsagskode (reason)

  Scenario: Payment-related updates require debtor selection for multi-debtor claims
    Given portal user "U1" is on the write-down form for claim "FDR-200"
    And claim "FDR-200" has multiple debtors
    And user "U1" selects update type "NEDSKRIVNING_INDBETALING"
    Then the form additionally requires debtor selection

  Scenario: Payment-related updates do not require debtor selection for single-debtor claims
    Given portal user "U1" is on the write-down form for claim "FDR-201"
    And claim "FDR-201" has a single debtor
    And user "U1" selects update type "NEDSKRIVNING_INDBETALING"
    Then the form does not require debtor selection
    And the single debtor is used automatically

  # --- Debtor identification for payment write-downs (FR 8-10) ---

  Scenario: Creditor selects debtor from claim debtor list
    Given portal user "U1" is on the write-down form for claim "FDR-200"
    And claim "FDR-200" has multiple debtors
    And user "U1" selects a payment-related update type
    Then the debtor list from the claim is displayed for selection

  Scenario: Debtor identifiers are censored in the selection
    Given portal user "U1" is viewing the debtor selection for claim "FDR-200"
    And the claim has a debtor with CPR identifier
    Then the CPR number is displayed as the first 6 digits followed by "****"
    And other identifier types (CVR, SE, AKR) are displayed normally

  Scenario: BFF resolves actual debtor ID from haeftelsesstruktur
    Given portal user "U1" has selected a debtor for a payment-related update
    When the update is submitted
    Then the BFF resolves the actual debtor ID from the claim's haeftelsesstruktur
    And the resolved debtor ID is forwarded to debt-service

  # --- Receipt display (FR 11-12) ---

  Scenario: Successful update displays receipt with operation result
    Given portal user "U1" has submitted a write-up for claim "FDR-100"
    And the update is processed successfully
    Then a receipt is displayed showing: aktions-ID, status, and beloeb

  Scenario: Debtor identifiers in response are censored before display
    Given a receipt is displayed for a payment-related update
    Then debtor identifiers in the receipt are censored before display

  # --- Access control (FR 13-15) ---

  Scenario: CREDITOR_EDITOR with allow_portal_actions can access update forms
    Given portal user "U1" is authenticated with role "CREDITOR_EDITOR"
    And the creditor agreement has allow_portal_actions enabled
    When user "U1" opens the update form for claim "FDR-100"
    Then access is granted

  Scenario: User without CREDITOR_EDITOR role cannot access update forms
    Given portal user "U2" is authenticated with role "CREDITOR_VIEWER"
    When user "U2" attempts to open the update form for claim "FDR-100"
    Then access is denied

  Scenario: User without allow_portal_actions cannot access update forms
    Given portal user "U1" is authenticated with role "CREDITOR_EDITOR"
    And the creditor agreement has allow_portal_actions disabled
    When user "U1" attempts to open the update form for claim "FDR-100"
    Then access is denied

  Scenario: Specific update type requires corresponding permission flag
    Given portal user "U1" is authenticated with role "CREDITOR_EDITOR"
    And the creditor agreement has allow_portal_actions enabled
    But the creditor agreement does not have allow_write_up_adjustment
    When user "U1" attempts to use update type OPSKRIVNING_REGULERING
    Then the update type is not available

  Scenario: All update operations are logged to the audit log
    Given portal user "U1" has submitted a write-down for claim "FDR-100"
    Then the operation is logged to the audit log

  # --- Layout and accessibility (FR 16-20) ---

  Scenario: Update forms use the SKAT standardlayout
    Given portal user "U1" opens the update form for claim "FDR-100"
    Then the page uses the SKAT standardlayout from layout/default.html
    And the page includes a skip link, header, breadcrumb, main content area, and footer

  Scenario: Update form breadcrumb shows correct path
    Given portal user "U1" is on the write-up form for claim "FDR-100"
    Then the breadcrumb shows: Forside > Fordringer > FDR-100 > Opskriv/Nedskriv

  Scenario: Forms use accessible patterns with labels and validation feedback
    Given portal user "U1" is on the update form for claim "FDR-100"
    Then all form fields have visible labels
    And validation errors are displayed inline with feedback
    And the form requires confirmation before submission

  Scenario: Debtor selection uses radio button group with censored identifiers
    Given portal user "U1" is on the update form with debtor selection
    Then the debtor selection uses a radio button group
    And each option shows a censored debtor identifier

  Scenario: All user-facing text uses message bundles with Danish and English
    Given the update form is rendered for claim "FDR-100"
    Then all user-facing text is loaded from message bundles
    And Danish and English translations are available for all text
    And no hardcoded text appears in the templates
