Feature: Creditor portal claim detail view (petition 030)

  # --- Template structure and layout ---

  Scenario: Claim detail template exists and uses SKAT standardlayout
    Given the claim detail template exists
    Then the detail page uses the SKAT standardlayout with breadcrumb

  # --- Claim information section ---

  Scenario: Claim detail template contains a claim information section with all required fields
    Given the claim detail template exists
    Then the claim detail template contains a claim information section
    And the template contains single-debtor-only fields conditionally displayed
    And the template conditionally displays the related obligation ID

  # --- Financial information section ---

  Scenario: Claim detail template contains financial breakdown table with semantic HTML
    Given the claim detail template exists
    Then the claim detail template contains a financial breakdown table
    And the financial table uses semantic HTML with scope attributes
    And the template displays additional financial summary fields
    And the extra interest rate is conditionally displayed

  # --- Write-ups section ---

  Scenario: Claim detail template contains collapsible write-ups section with annulled flag
    Given the claim detail template exists
    Then the claim detail template contains a collapsible write-ups section
    And annulled write-ups have a visual flag

  # --- Write-downs section ---

  Scenario: Claim detail template contains collapsible write-downs section
    Given the claim detail template exists
    Then the claim detail template contains a collapsible write-downs section

  # --- Related claims section ---

  Scenario: Claim detail template contains collapsible related claims section with clickable links
    Given the claim detail template exists
    Then the claim detail template contains a collapsible related claims section
    And each related claim is clickable to its own detail view

  # --- Debtor information section ---

  Scenario: Claim detail template contains debtor information section
    Given the claim detail template exists
    Then the claim detail template contains a debtors section

  # --- Decisions section ---

  Scenario: Claim detail template contains decisions section for single-debtor claims
    Given the claim detail template exists
    Then the claim detail template contains a decisions section for single-debtor claims

  # --- Zero-balance and error handling ---

  Scenario: Claim detail template shows zero-balance expired message and service errors
    Given the claim detail template exists
    Then the claim detail template shows a zero-balance expired message when applicable
    And the claim detail template displays service errors

  # --- Formatting ---

  Scenario: Claim detail uses Danish monetary and date formatting
    Given the claim detail template exists
    Then monetary amounts in the claim detail use Danish locale formatting
    And dates in the claim detail are formatted as dd.MM.yyyy

  # --- i18n ---

  Scenario: Danish and English translations exist for all claim detail message keys
    Given the claim detail template exists
    Then Danish translations exist for all claim detail message keys
    And English translations exist for all claim detail message keys

  # --- Message bundles ---

  Scenario: All user-facing text uses message bundles
    Given the claim detail template exists
    Then the claim detail template uses message bundles for all user-facing text
