Feature: Configuration administration UI for versioned business values

  # ==========================================================================
  # Petition 047 — Functional requirements extracted as Gherkin scenarios
  #
  # Covers: REST API CRUD (FR-1), Validation rules (FR-2), Status field (FR-3),
  #         Derived rate auto-computation (FR-4), Configuration UI (FR-5),
  #         Version history (FR-6), Create form (FR-7), Approve/reject (FR-8),
  #         Portal controller (FR-9), Internationalisation (FR-10),
  #         Audit logging (FR-11), Security & authorisation (FR-12)
  #
  # Depends on: Petition 046 (versioned business configuration)
  # ==========================================================================

  # --- FR-1: BusinessConfigController — REST API ---

  Scenario: 001 — GET /api/v1/config returns all entries grouped by config_key
    Given config entries exist for keys "RATE_NB_UDLAAN", "FEE_RYKKER", and "THRESHOLD_INTEREST_MIN"
    When the operator sends GET /api/v1/config
    Then the response status is 200
    And the response body contains entries grouped by config_key
    And each group contains the currently active entry and any future or pending entries
    And each entry includes a status field with value "ACTIVE", "PENDING_REVIEW", "FUTURE", "EXPIRED", or "APPROVED"

  Scenario: 002 — GET /api/v1/config/{key} returns effective value for a date
    Given a config entry for key "RATE_INDR_STD" with value "0.0575" valid from "2026-01-05" and valid_to null
    When the operator sends GET /api/v1/config/RATE_INDR_STD?date=2026-03-15
    Then the response status is 200
    And the response body contains config_key "RATE_INDR_STD" with value "0.0575"

  Scenario: 003 — GET /api/v1/config/{key} returns no entry when date is outside validity
    Given a config entry for key "RATE_INDR_STD" with value "0.0575" valid from "2026-01-05" and valid_to "2026-07-01"
    When the operator sends GET /api/v1/config/RATE_INDR_STD?date=2026-08-01
    Then the response status is 404

  Scenario: 004 — GET /api/v1/config/{key}/history returns full version history
    Given 3 config entries exist for key "RATE_INDR_STD" with different validity periods
    When the operator sends GET /api/v1/config/RATE_INDR_STD/history
    Then the response status is 200
    And the response body contains 3 version entries for key "RATE_INDR_STD"

  Scenario: 005 — POST /api/v1/config creates a new config version
    Given the operator has role ADMIN
    When the operator sends POST /api/v1/config with body:
      """
      {
        "configKey": "RATE_INDR_STD",
        "configValue": "0.0575",
        "valueType": "DECIMAL",
        "validFrom": "2026-07-05",
        "validTo": null,
        "description": "Inddrivelsesrente (NB + 4%)",
        "legalBasis": "Gældsinddrivelsesloven § 5, stk. 1-2"
      }
      """
    Then the response status is 201
    And the response body contains the created config entry with status "FUTURE"

  Scenario: 006 — POST /api/v1/config auto-closes previous open-ended entry for the same key
    Given an active config entry for key "RATE_INDR_STD" with valid_from "2026-01-05" and valid_to null
    And the operator has role ADMIN
    When the operator sends POST /api/v1/config with configKey "RATE_INDR_STD" and validFrom "2026-07-05"
    Then the previous entry for key "RATE_INDR_STD" has its valid_to set to "2026-07-05"

  Scenario: 007 — PUT /api/v1/config/{id} updates a future config entry
    Given a config entry with id "entry-1" for key "FEE_RYKKER" with status "FUTURE" and valid_from in the future
    And the operator has role CONFIGURATION_MANAGER
    When the operator sends PUT /api/v1/config/entry-1 with configValue "250.00"
    Then the response status is 200
    And the config entry "entry-1" has value "250.00"

  Scenario: 008 — DELETE /api/v1/config/{id} deletes a future config entry
    Given a config entry with id "entry-2" for key "FEE_RYKKER" with status "FUTURE" and valid_from in the future
    And the operator has role ADMIN
    When the operator sends DELETE /api/v1/config/entry-2
    Then the response status is 204
    And the config entry "entry-2" no longer exists

  Scenario: 009 — Validation failures return Danish-language error messages
    Given the operator has role ADMIN
    When the operator sends POST /api/v1/config with an invalid request body
    Then the response status is 400
    And the response body contains an error message in Danish

  # --- FR-2: Validation rules ---

  Scenario: 010 — POST rejects entry with valid_from in the past
    Given the operator has role ADMIN
    And today is "2026-06-15"
    When the operator sends POST /api/v1/config with configKey "FEE_RYKKER" and validFrom "2026-06-01"
    Then the response status is 400
    And the error message indicates valid_from cannot be in the past

  Scenario: 011 — POST allows past valid_from when seedMigration flag is true and caller is ADMIN
    Given the operator has role ADMIN
    And today is "2026-06-15"
    When the operator sends POST /api/v1/config with configKey "FEE_RYKKER", validFrom "2026-01-01", and seedMigration true
    Then the response status is 201

  Scenario: 012 — POST rejects past valid_from even with seedMigration flag if caller is not ADMIN
    Given the operator has role CONFIGURATION_MANAGER
    And today is "2026-06-15"
    When the operator sends POST /api/v1/config with configKey "FEE_RYKKER", validFrom "2026-01-01", and seedMigration true
    Then the response status is 403

  Scenario: 013 — POST rejects entry with overlapping validity period
    Given an existing config entry for key "RATE_INDR_STD" with valid_from "2026-01-05" and valid_to null
    And the operator has role ADMIN
    When the operator sends POST /api/v1/config with configKey "RATE_INDR_STD" and validFrom "2026-03-01" and validTo "2026-09-01"
    Then the response status is 400
    And the error message indicates an overlapping validity period exists

  Scenario: 014 — POST validates DECIMAL config_value is parseable as BigDecimal
    Given the operator has role ADMIN
    When the operator sends POST /api/v1/config with configKey "RATE_INDR_STD", configValue "not-a-number", and valueType "DECIMAL"
    Then the response status is 400
    And the error message indicates the value does not match the expected type DECIMAL

  Scenario: 015 — POST validates INTEGER config_value is parseable as Integer
    Given the operator has role ADMIN
    When the operator sends POST /api/v1/config with configKey "THRESHOLD_INTEREST_MIN", configValue "12.5", and valueType "INTEGER"
    Then the response status is 400
    And the error message indicates the value does not match the expected type INTEGER

  Scenario: 016 — POST validates BOOLEAN config_value is "true" or "false"
    Given the operator has role ADMIN
    When the operator sends POST /api/v1/config with configKey "SOME_FLAG", configValue "yes", and valueType "BOOLEAN"
    Then the response status is 400
    And the error message indicates the value does not match the expected type BOOLEAN

  Scenario: 017 — POST validates STRING config_value is non-empty
    Given the operator has role ADMIN
    When the operator sends POST /api/v1/config with configKey "SOME_TEXT", configValue "", and valueType "STRING"
    Then the response status is 400
    And the error message indicates the value must not be empty

  Scenario: 018 — POST rejects entry without description
    Given the operator has role ADMIN
    When the operator sends POST /api/v1/config with configKey "FEE_RYKKER" and no description
    Then the response status is 400
    And the error message indicates description is mandatory

  Scenario: 019 — POST rejects entry without legal_basis
    Given the operator has role ADMIN
    When the operator sends POST /api/v1/config with configKey "FEE_RYKKER" and no legalBasis
    Then the response status is 400
    And the error message indicates legal_basis is mandatory

  Scenario: 020 — PUT only allows modification of entries with future valid_from
    Given a config entry with id "entry-3" for key "FEE_RYKKER" with status "FUTURE" and valid_from in the future
    And the operator has role ADMIN
    When the operator sends PUT /api/v1/config/entry-3 with configValue "300.00"
    Then the response status is 200

  Scenario: 021 — PUT only allows modification of entries with PENDING_REVIEW status
    Given a config entry with id "entry-4" for key "RATE_INDR_STD" with review_status "PENDING_REVIEW"
    And the operator has role ADMIN
    When the operator sends PUT /api/v1/config/entry-4 with configValue "0.0600"
    Then the response status is 200

  Scenario: 022 — PUT rejects modification of currently active entries
    Given a config entry with id "entry-5" for key "FEE_RYKKER" that is currently active
    And the operator has role ADMIN
    When the operator sends PUT /api/v1/config/entry-5 with configValue "300.00"
    Then the response status is 409
    And the error message indicates active entries cannot be modified

  Scenario: 023 — PUT rejects modification of expired entries
    Given a config entry with id "entry-6" for key "FEE_RYKKER" that is expired
    And the operator has role ADMIN
    When the operator sends PUT /api/v1/config/entry-6 with configValue "300.00"
    Then the response status is 409
    And the error message indicates expired entries cannot be modified

  Scenario: 024 — DELETE rejects deletion of active entries
    Given a config entry with id "entry-7" for key "FEE_RYKKER" that is currently active
    And the operator has role ADMIN
    When the operator sends DELETE /api/v1/config/entry-7
    Then the response status is 409
    And the error message indicates active entries cannot be deleted

  Scenario: 025 — DELETE rejects deletion of past/expired entries
    Given a config entry with id "entry-8" for key "FEE_RYKKER" that is expired
    And the operator has role ADMIN
    When the operator sends DELETE /api/v1/config/entry-8
    Then the response status is 409
    And the error message indicates past entries cannot be deleted

  # --- FR-3: BusinessConfigEntity status field ---

  Scenario: 026 — Status is computed as ACTIVE when entry is currently effective
    Given a config entry for key "FEE_RYKKER" with valid_from "2026-01-01" and valid_to null and review_status null
    And today is "2026-06-15"
    When the system computes the status of the entry
    Then the computed status is "ACTIVE"

  Scenario: 027 — Status is computed as EXPIRED when valid_to has passed
    Given a config entry for key "FEE_RYKKER" with valid_from "2025-01-01" and valid_to "2026-01-01" and review_status null
    And today is "2026-06-15"
    When the system computes the status of the entry
    Then the computed status is "EXPIRED"

  Scenario: 028 — Status is computed as FUTURE when valid_from is in the future and no review_status
    Given a config entry for key "FEE_RYKKER" with valid_from "2026-12-01" and valid_to null and review_status null
    And today is "2026-06-15"
    When the system computes the status of the entry
    Then the computed status is "FUTURE"

  Scenario: 029 — Status is PENDING_REVIEW when review_status is PENDING_REVIEW regardless of dates
    Given a config entry for key "RATE_INDR_STD" with valid_from "2026-07-05" and review_status "PENDING_REVIEW"
    When the system computes the status of the entry
    Then the computed status is "PENDING_REVIEW"

  Scenario: 030 — Status is APPROVED when review_status is APPROVED and valid_from is in the future
    Given a config entry for key "RATE_INDR_STD" with valid_from "2026-07-05" and review_status "APPROVED"
    And today is "2026-06-15"
    When the system computes the status of the entry
    Then the computed status is "APPROVED"

  Scenario: 031 — PENDING_REVIEW transitions to APPROVED on operator approval
    Given a config entry with id "entry-p1" and review_status "PENDING_REVIEW"
    When the operator approves entry "entry-p1"
    Then the entry review_status changes to "APPROVED"

  Scenario: 032 — PENDING_REVIEW entry is deleted on operator rejection
    Given a config entry with id "entry-p2" and review_status "PENDING_REVIEW"
    When the operator rejects entry "entry-p2"
    Then the entry "entry-p2" no longer exists

  Scenario: 033 — APPROVED entry becomes ACTIVE when valid_from date arrives
    Given a config entry for key "RATE_INDR_STD" with valid_from "2026-07-05" and review_status "APPROVED"
    When the current date reaches "2026-07-05"
    Then the computed status of the entry is "ACTIVE"

  Scenario: 033b — FUTURE entry becomes ACTIVE when valid_from date arrives
    Given a config entry for key "FEE_RYKKER" with valid_from "2026-07-05" and review_status null
    And today is before "2026-07-05"
    When the current date reaches "2026-07-05"
    Then the computed status of the entry is "ACTIVE"

  Scenario: 034 — ACTIVE entry becomes EXPIRED when a newer entry takes effect
    Given an active config entry for key "RATE_INDR_STD" with valid_from "2026-01-05" and valid_to null
    When a new entry for key "RATE_INDR_STD" with valid_from "2026-07-05" becomes active
    Then the previous entry's valid_to is set to "2026-07-05"
    And the previous entry's computed status is "EXPIRED"

  Scenario: 035 — Only review_status column is persisted; ACTIVE, FUTURE, EXPIRED are derived
    Given a config entry for key "FEE_RYKKER" with valid_from "2026-12-01" and review_status null
    Then the persisted review_status column is null
    And the status "FUTURE" is computed dynamically from valid_from relative to the current date

  # --- FR-4: Derived rate auto-computation with approval workflow ---

  Scenario: 036 — Creating RATE_NB_UDLAAN auto-generates three derived rate entries
    Given the operator has role ADMIN
    When the operator sends POST /api/v1/config with configKey "RATE_NB_UDLAAN" and configValue "0.0175" and validFrom "2026-07-05"
    Then the system creates a config entry for "RATE_NB_UDLAAN" with value "0.0175"
    And the system auto-generates a derived entry for "RATE_INDR_STD" with value "0.0575"
    And the system auto-generates a derived entry for "RATE_INDR_TOLD" with value "0.0375"
    And the system auto-generates a derived entry for "RATE_INDR_TOLD_AFD" with value "0.0275"

  Scenario: 037 — Auto-generated derived entries use the same valid_from as the NB rate
    Given the operator creates a "RATE_NB_UDLAAN" entry with validFrom "2026-07-05"
    When the derived rate entries are auto-generated
    Then all 3 derived entries have valid_from "2026-07-05"

  Scenario: 038 — Auto-generated derived entries have review_status PENDING_REVIEW
    Given the operator creates a "RATE_NB_UDLAAN" entry with validFrom "2026-07-05"
    When the derived rate entries are auto-generated
    Then all 3 derived entries have review_status "PENDING_REVIEW"

  Scenario: 039 — Auto-generated entries include descriptive formula in description
    Given the operator creates a "RATE_NB_UDLAAN" entry with configValue "0.0175"
    When the derived rate entries are auto-generated
    Then the "RATE_INDR_STD" entry description notes it was auto-computed as NB + 4%
    And the "RATE_INDR_TOLD" entry description notes it was auto-computed as NB + 2%
    And the "RATE_INDR_TOLD_AFD" entry description notes it was auto-computed as NB + 1%

  Scenario: 040 — Auto-generated entries have created_by set to SYSTEM
    Given the operator creates a "RATE_NB_UDLAAN" entry
    When the derived rate entries are auto-generated
    Then all 3 derived entries have created_by "SYSTEM (auto-computed from RATE_NB_UDLAAN)"

  Scenario: 041 — Auto-generated entries do not become effective until approved
    Given auto-generated derived entries for "RATE_INDR_STD" with review_status "PENDING_REVIEW"
    And the valid_from date has arrived
    When the system evaluates which config entry is effective for key "RATE_INDR_STD"
    Then the PENDING_REVIEW entry is not considered effective
    And the previously active entry remains in effect

  Scenario: 042 — POST response for NB rate includes the list of auto-generated derived entries
    Given the operator has role ADMIN
    When the operator sends POST /api/v1/config with configKey "RATE_NB_UDLAAN" and configValue "0.0175"
    Then the response status is 201
    And the response body includes the created NB rate entry
    And the response body includes a list of 3 auto-generated derived entries

  # --- FR-5: Configuration management page in caseworker portal ---

  Scenario: 043 — Konfiguration menu item is visible to ADMIN
    Given the operator is logged in with role ADMIN
    When the operator views the caseworker portal main navigation
    Then the navigation contains a menu item "Konfiguration"

  Scenario: 044 — Konfiguration menu item is visible to CONFIGURATION_MANAGER
    Given the operator is logged in with role CONFIGURATION_MANAGER
    When the operator views the caseworker portal main navigation
    Then the navigation contains a menu item "Konfiguration"

  Scenario: 045a — Konfiguration menu item is not visible to CASEWORKER
    Given the operator is logged in with role CASEWORKER
    When the operator views the caseworker portal main navigation
    Then the navigation does not contain a menu item "Konfiguration"

  Scenario: 045b — CASEWORKER sees read-only configuration page when navigating directly
    Given the operator is logged in with role CASEWORKER
    When the operator navigates to /konfiguration
    Then the page displays current rates and history
    And no create, edit, or delete controls are visible
    And a read-only notice is displayed

  Scenario: 046 — Configuration list page groups entries by category Renter
    Given config entries exist for keys "RATE_NB_UDLAAN", "RATE_INDR_STD", "RATE_INDR_TOLD", "RATE_INDR_TOLD_AFD"
    When the operator navigates to /konfiguration
    Then the entries are displayed under the category heading "Renter"

  Scenario: 047 — Configuration list page groups entries by category Gebyrer
    Given config entries exist for keys "FEE_RYKKER", "FEE_UDLAEG_BASE", "FEE_UDLAEG_PCT", "FEE_LOENINDEHOLDELSE"
    When the operator navigates to /konfiguration
    Then the entries are displayed under the category heading "Gebyrer"

  Scenario: 048 — Configuration list page groups entries by category Tærskler
    Given config entries exist for keys "THRESHOLD_INTEREST_MIN", "THRESHOLD_FORAELDELSE_WARN"
    When the operator navigates to /konfiguration
    Then the entries are displayed under the category heading "Tærskler"

  Scenario: 049 — Each config row displays key, value, dates, legal basis, and status
    Given an active config entry for key "RATE_INDR_STD" with value "0.0575", valid_from "2026-01-05", valid_to null, and legal basis "Gældsinddrivelsesloven § 5, stk. 1-2"
    When the operator views the configuration list page
    Then the row for "RATE_INDR_STD" displays a human-readable Danish label
    And the row displays the current value formatted as a percentage
    And the row displays valid_from "2026-01-05" and valid_to as open-ended
    And the row displays the legal basis abbreviated with full text on hover
    And the row displays a green status badge indicating active

  Scenario: 050 — Rate values are formatted as percentages
    Given an active config entry for key "RATE_INDR_STD" with value "0.0575" and value_type DECIMAL
    When the operator views the configuration list page
    Then the value is displayed formatted as a percentage

  Scenario: 051 — Fee values are formatted as currency
    Given an active config entry for key "FEE_RYKKER" with value "250.00" and value_type DECIMAL
    When the operator views the configuration list page
    Then the value is displayed formatted as currency

  Scenario: 052 — Threshold values are formatted as days
    Given an active config entry for key "THRESHOLD_FORAELDELSE_WARN" with value "90" and value_type INTEGER
    When the operator views the configuration list page
    Then the value is displayed formatted as days

  Scenario: 053 — Active entries show a green status badge
    Given a config entry with computed status "ACTIVE"
    When the operator views the configuration list page
    Then the entry's status badge is green

  Scenario: 054 — Future and pending review entries show a yellow status badge
    Given a config entry with computed status "FUTURE"
    And a config entry with computed status "PENDING_REVIEW"
    When the operator views the configuration list page
    Then both entries' status badges are yellow

  Scenario: 055 — Expired entries show a grey status badge
    Given a config entry with computed status "EXPIRED"
    When the operator views the configuration list page
    Then the entry's status badge is grey

  # --- FR-6: Version history view ---

  Scenario: 056 — Clicking a config key navigates to the detail page with version history
    Given config entries exist for key "RATE_INDR_STD"
    When the operator clicks the key "RATE_INDR_STD" in the configuration list
    Then the browser navigates to /konfiguration/RATE_INDR_STD
    And the page displays a version history timeline

  Scenario: 057 — Version history timeline is ordered by valid_from descending
    Given 3 config entries for key "RATE_INDR_STD" with valid_from "2025-01-05", "2025-07-05", and "2026-01-05"
    When the operator views the detail page for "RATE_INDR_STD"
    Then the timeline displays entries in order "2026-01-05", "2025-07-05", "2025-01-05"

  Scenario: 058 — Timeline entries display value, dates, created_by, created_at, legal basis, and status
    Given a config entry for key "RATE_INDR_STD" with value "0.0575", valid_from "2026-01-05", valid_to null, created_by "admin-user", created_at "2025-12-20T10:30:00", and legal basis "Gældsinddrivelsesloven § 5"
    When the operator views the detail page for "RATE_INDR_STD"
    Then the timeline entry displays the value "0.0575"
    And the timeline entry displays valid_from "2026-01-05" and valid_to as open-ended
    And the timeline entry displays created_by "admin-user" and created_at "2025-12-20T10:30:00"
    And the timeline entry displays the legal basis
    And the timeline entry displays a colour-coded status badge

  Scenario: 059 — Rate keys display a line chart of rate values over time
    Given multiple config entries for key "RATE_INDR_STD" with value_type DECIMAL
    When the operator views the detail page for "RATE_INDR_STD"
    Then the page includes a line chart showing rate values over time

  Scenario: 060 — Non-rate keys do not display a line chart
    Given multiple config entries for key "THRESHOLD_INTEREST_MIN" with value_type INTEGER
    When the operator views the detail page for "THRESHOLD_INTEREST_MIN"
    Then the page does not include a rate line chart

  # --- FR-7: Create new version form ---

  Scenario: 061 — Detail page shows "Opret ny version" button for ADMIN
    Given the operator is logged in with role ADMIN
    When the operator views the detail page for "FEE_RYKKER"
    Then the page displays a button labelled "Opret ny version"

  Scenario: 062 — Detail page shows "Opret ny version" button for CONFIGURATION_MANAGER
    Given the operator is logged in with role CONFIGURATION_MANAGER
    When the operator views the detail page for "FEE_RYKKER"
    Then the page displays a button labelled "Opret ny version"

  Scenario: 063 — Detail page hides "Opret ny version" button for CASEWORKER
    Given the operator is logged in with role CASEWORKER
    When the operator views the detail page for "FEE_RYKKER"
    Then the page does not display a button labelled "Opret ny version"

  Scenario: 064 — Create form contains required fields with correct constraints
    Given the operator opens the create new version form for key "FEE_RYKKER"
    Then the form contains a config_value input field with validation matching the key's value_type
    And the form contains a valid_from date picker with minimum date set to tomorrow
    And the form contains a description text area
    And the form contains a legal_basis text field

  Scenario: 065 — Create form pre-fills description from previous entry
    Given an existing config entry for key "FEE_RYKKER" with description "Rykkergebyr"
    When the operator opens the create new version form for key "FEE_RYKKER"
    Then the description field is pre-filled with "Rykkergebyr"

  Scenario: 066 — Create form pre-fills legal_basis from previous entry
    Given an existing config entry for key "FEE_RYKKER" with legal basis "Inddrivelsesloven § 7"
    When the operator opens the create new version form for key "FEE_RYKKER"
    Then the legal_basis field is pre-filled with "Inddrivelsesloven § 7"

  Scenario: 067 — Submitting NB rate form shows derived rate preview panel
    Given the operator is on the create form for key "RATE_NB_UDLAAN"
    When the operator enters configValue "0.0175" and submits the form
    Then a preview panel is displayed showing auto-computed derived rates
    And the preview shows RATE_INDR_STD = "0.0575" (NB + 4%)
    And the preview shows RATE_INDR_TOLD = "0.0375" (NB + 2%)
    And the preview shows RATE_INDR_TOLD_AFD = "0.0275" (NB + 1%)

  Scenario: 068 — Operator accepts all derived rates from preview
    Given the operator sees the derived rate preview panel for "RATE_NB_UDLAAN"
    When the operator clicks "Godkend alle" (accept all)
    Then the NB rate entry is created
    And 3 derived rate entries are created with review_status "PENDING_REVIEW"

  Scenario: 069 — Operator modifies individual derived rates before confirming
    Given the operator sees the derived rate preview panel for "RATE_NB_UDLAAN"
    When the operator modifies the RATE_INDR_STD value to "0.0600" and confirms
    Then the RATE_INDR_STD entry is created with value "0.0600"
    And the RATE_INDR_TOLD and RATE_INDR_TOLD_AFD entries retain their auto-computed values

  Scenario: 070 — Operator cancels derived rate creation entirely
    Given the operator sees the derived rate preview panel for "RATE_NB_UDLAAN"
    When the operator cancels the operation
    Then no NB rate entry is created
    And no derived rate entries are created

  Scenario: 071 — Confirmation dialog is shown before creation
    Given the operator has filled in the create new version form
    When the operator clicks the submit button
    Then a confirmation dialog with the message "Er du sikker?" is displayed
    And the dialog has a "Ja, fortsæt" button and a "Nej, annullér" button

  Scenario: 072 — Confirmation dialog is shown before update
    Given the operator has modified a pending config entry
    When the operator clicks the save button
    Then a confirmation dialog with the message "Er du sikker?" is displayed

  Scenario: 073 — Confirmation dialog is shown before deletion
    Given the operator is about to delete a future config entry
    When the operator clicks the delete button
    Then a confirmation dialog with the message "Er du sikker?" is displayed

  Scenario: 074 — Successful creation shows success message and refreshes timeline
    Given the operator submits a valid create new version form
    When the creation is successful
    Then the page displays a success message "Ny konfigurationsversion oprettet"
    And the version history timeline is refreshed to include the new entry

  # --- FR-8: Approve/reject pending entries ---

  Scenario: 075 — Pending review entries display Godkend and Afvis buttons
    Given a config entry with status "PENDING_REVIEW" exists for key "RATE_INDR_STD"
    And the operator is logged in with role ADMIN
    When the operator views the detail page for "RATE_INDR_STD"
    Then the pending entry displays a "Godkend" (approve) button
    And the pending entry displays an "Afvis" (reject) button

  Scenario: 076 — Approving a pending entry sets review_status to APPROVED
    Given a config entry with id "entry-pr1" and review_status "PENDING_REVIEW"
    And the operator has role ADMIN
    When the operator clicks "Godkend" for entry "entry-pr1"
    Then the entry "entry-pr1" review_status changes to "APPROVED"

  Scenario: 077 — Approving a pending entry closes the previous open-ended entry
    Given an active config entry for key "RATE_INDR_STD" with valid_to null
    And a pending entry with id "entry-pr2" for key "RATE_INDR_STD" with valid_from "2026-07-05"
    When the operator approves entry "entry-pr2"
    Then the previous active entry's valid_to is set to "2026-07-05"

  Scenario: 078 — Approval logs the approving operator identity and timestamp
    Given a pending entry with id "entry-pr3" for key "RATE_INDR_STD"
    And the operator "admin-user" has role ADMIN
    When the operator approves entry "entry-pr3"
    Then an audit record is created with action "APPROVE", performed_by "admin-user", and a timestamp

  Scenario: 079 — Rejecting a pending entry deletes it
    Given a config entry with id "entry-pr4" and review_status "PENDING_REVIEW"
    And the operator has role ADMIN
    When the operator clicks "Afvis" for entry "entry-pr4"
    Then the entry "entry-pr4" is deleted

  Scenario: 080 — Rejection is audit-logged
    Given a pending entry with id "entry-pr5" for key "RATE_INDR_STD"
    And the operator "admin-user" has role ADMIN
    When the operator rejects entry "entry-pr5"
    Then an audit record is created with action "REJECT", performed_by "admin-user", and a timestamp

  # --- FR-9: ConfigurationController in caseworker portal ---

  Scenario: 081 — GET /konfiguration renders the config list page
    Given the operator is logged in with role ADMIN
    When the operator sends GET /konfiguration
    Then the response renders the configuration list page

  Scenario: 082 — GET /konfiguration/{key} renders the detail page with history
    Given the operator is logged in with role ADMIN
    When the operator sends GET /konfiguration/RATE_INDR_STD
    Then the response renders the detail page for key "RATE_INDR_STD" including version history

  Scenario: 083 — POST /konfiguration creates a new config version via form submission
    Given the operator is logged in with role CONFIGURATION_MANAGER
    When the operator submits the create form at POST /konfiguration with valid data
    Then the ConfigServiceClient delegates the creation to debt-service POST /api/v1/config
    And the page displays a success message

  Scenario: 084 — PUT /konfiguration/{id}/approve approves a pending entry
    Given the operator is logged in with role ADMIN
    When the operator sends PUT /konfiguration/entry-pr6/approve
    Then the ConfigServiceClient delegates the approval to debt-service PUT /api/v1/config/entry-pr6
    And the page displays the approval success message

  Scenario: 085 — DELETE /konfiguration/{id} deletes or rejects a future entry
    Given the operator is logged in with role ADMIN
    When the operator sends DELETE /konfiguration/entry-f1
    Then the ConfigServiceClient delegates the deletion to debt-service DELETE /api/v1/config/entry-f1
    And the page displays the deletion success message

  Scenario: 086 — ConfigServiceClient uses CircuitBreaker and Retry annotations
    Given the debt-service REST API is temporarily unavailable
    When the ConfigServiceClient attempts to call the config API
    Then the call is retried according to the retry policy
    And the circuit breaker opens after the failure threshold is reached

  Scenario: 087 — Controller redirects to /demo-login when session identity is missing
    Given no operator session identity is present
    When a request is made to /konfiguration
    Then the response redirects to /demo-login

  Scenario: 088 — Controller uses HTMX fragment responses for partial page updates
    Given the operator approves a pending entry via HTMX request
    When the approval is successful
    Then the response is an HTMX fragment that updates only the history timeline section

  Scenario: 089 — Backend unavailability displays Danish error message
    Given the debt-service REST API is unavailable
    And the circuit breaker is open
    When the operator navigates to /konfiguration
    Then the page displays the error message "Konfigurationsservice er midlertidigt utilgængelig"

  # --- FR-10: Internationalisation — messages_da.properties ---

  Scenario: 090 — Navigation menu item uses Danish label
    Given the messages_da.properties contains key "nav.konfiguration"
    When the operator views the main navigation
    Then the menu item text is "Konfiguration"

  Scenario: 091 — Configuration page title and subtitle use Danish labels
    Given the messages_da.properties contains keys "config.title" and "config.subtitle"
    When the operator navigates to /konfiguration
    Then the page title is "Konfiguration – Forretningsværdier"
    And the page subtitle is "Administrér renter, gebyrer og tærskler"

  Scenario: 092 — Category headings use Danish labels
    When the operator views the configuration list page
    Then the interest category heading is "Renter"
    And the fees category heading is "Gebyrer"
    And the thresholds category heading is "Tærskler"

  Scenario: 093 — Status labels use Danish text
    When the operator views config entries with different statuses
    Then active entries display status "Aktiv"
    And future entries display status "Fremtidig"
    And pending entries display status "Afventer godkendelse"
    And expired entries display status "Udløbet"
    And approved entries display status "Godkendt"

  Scenario: 094 — Validation error messages are in Danish
    When a config creation fails due to a past valid_from date
    Then the error message is "Gyldig fra-dato kan ikke være i fortiden"

  Scenario: 095 — Overlap validation error message is in Danish
    When a config creation fails due to overlapping validity
    Then the error message is "Gyldighedsperioden overlapper en eksisterende post"

  Scenario: 096 — Value format validation error message is in Danish
    When a config creation fails due to value type mismatch for type "DECIMAL"
    Then the error message is "Værdien matcher ikke den forventede type (DECIMAL)"

  Scenario: 097 — Required fields validation error message is in Danish
    When a config creation fails because required fields are missing
    Then the error message is "Alle felter skal udfyldes"

  Scenario: 098 — Read-only notice uses Danish text for CASEWORKER
    Given the operator is logged in with role CASEWORKER
    When the operator views the configuration page
    Then a notice is displayed reading "Du har læseadgang. Kontakt en administrator for at ændre konfiguration."

  Scenario: 099 — All UI strings are in Danish with no English text
    When the operator views any configuration page
    Then all visible text on the page is in Danish
    And no English UI strings are displayed

  # --- FR-11: Audit logging ---

  Scenario: 100 — Creating a config entry generates a CREATE audit record
    Given the operator "admin-user" with role ADMIN creates a config entry for key "FEE_RYKKER"
    When the creation is successful
    Then a record is inserted into business_config_audit with action "CREATE"
    And the audit record contains performed_by "admin-user" and a timestamp
    And the audit record contains config_key "FEE_RYKKER" and the new config_entry_id
    And the audit record old_value is null
    And the audit record new_value contains the created value

  Scenario: 101 — Updating a config entry generates an UPDATE audit record
    Given the operator "config-mgr" with role CONFIGURATION_MANAGER updates entry "entry-u1" value from "200.00" to "250.00"
    When the update is successful
    Then a record is inserted into business_config_audit with action "UPDATE"
    And the audit record contains old_value "200.00" and new_value "250.00"
    And the audit record contains performed_by "config-mgr"

  Scenario: 102 — Approving an entry generates an APPROVE audit record
    Given the operator "admin-user" approves entry "entry-a1" for key "RATE_INDR_STD"
    When the approval is successful
    Then a record is inserted into business_config_audit with action "APPROVE"
    And the audit record contains performed_by "admin-user"
    And the audit record contains config_key "RATE_INDR_STD"

  Scenario: 103 — Rejecting an entry generates a REJECT audit record
    Given the operator "admin-user" rejects entry "entry-r1" for key "RATE_INDR_STD"
    When the rejection is processed
    Then a record is inserted into business_config_audit with action "REJECT"
    And the audit record contains performed_by "admin-user"

  Scenario: 104 — Deleting a future entry generates a DELETE audit record
    Given the operator "admin-user" deletes future entry "entry-d1" for key "FEE_RYKKER" with value "300.00"
    When the deletion is successful
    Then a record is inserted into business_config_audit with action "DELETE"
    And the audit record contains old_value "300.00" and new_value null
    And the audit record contains performed_by "admin-user"

  Scenario: 105 — Auto-generated derived entries generate audit records with SYSTEM details
    Given the operator creates a "RATE_NB_UDLAAN" entry triggering auto-generation
    When 3 derived entries are auto-generated
    Then 3 CREATE audit records are inserted with performed_by "SYSTEM (auto-computed from RATE_NB_UDLAAN)"
    And each audit record details field notes the auto-computation formula

  Scenario: 106 — Audit record contains all required fields per schema
    Given any configuration change occurs
    Then the business_config_audit record contains id as UUID
    And the record contains config_entry_id as UUID FK to business_config
    And the record contains config_key as VARCHAR
    And the record contains action as one of "CREATE", "UPDATE", "APPROVE", "REJECT", "DELETE"
    And the record contains performed_by as the operator identity
    And the record contains performed_at as a timestamp
    And the record contains a details text field for additional context

  Scenario: 107 — Audit trail is viewable on the config detail page
    Given audit records exist for key "RATE_INDR_STD"
    And the operator is logged in with role ADMIN
    When the operator views the detail page for "RATE_INDR_STD"
    Then the page includes a collapsible audit trail section below the version timeline
    And the audit trail displays all audit entries for "RATE_INDR_STD"

  # --- FR-12: Security and authorisation ---

  Scenario: 108 — ADMIN can access all config API endpoints
    Given the caller has role ADMIN
    Then GET /api/v1/config returns 200
    And GET /api/v1/config/{key}?date={date} returns 200
    And GET /api/v1/config/{key}/history returns 200
    And POST /api/v1/config with valid body returns 201
    And PUT /api/v1/config/{id} with valid body returns 200
    And DELETE /api/v1/config/{id} for a future entry returns 204

  Scenario: 109 — CONFIGURATION_MANAGER can access all config API endpoints
    Given the caller has role CONFIGURATION_MANAGER
    Then GET /api/v1/config returns 200
    And GET /api/v1/config/{key}?date={date} returns 200
    And GET /api/v1/config/{key}/history returns 200
    And POST /api/v1/config with valid body returns 201
    And PUT /api/v1/config/{id} with valid body returns 200
    And DELETE /api/v1/config/{id} for a future entry returns 204

  Scenario: 110 — CASEWORKER can read config entries but cannot write
    Given the caller has role CASEWORKER
    Then GET /api/v1/config returns 200
    And GET /api/v1/config/{key}?date={date} returns 200
    And GET /api/v1/config/{key}/history returns 200
    And POST /api/v1/config returns 403
    And PUT /api/v1/config/{id} returns 403
    And DELETE /api/v1/config/{id} returns 403

  Scenario: 111 — SERVICE role can read config entries and effective values but not history
    Given the caller has role SERVICE
    Then GET /api/v1/config returns 200
    And GET /api/v1/config/{key}?date={date} returns 200
    And GET /api/v1/config/{key}/history returns 403
    And POST /api/v1/config returns 403
    And PUT /api/v1/config/{id} returns 403
    And DELETE /api/v1/config/{id} returns 403

  Scenario: 112 — Portal ConfigurationController hides write controls for CASEWORKER
    Given the operator is logged in with role CASEWORKER
    When the operator views the configuration pages
    Then create, edit, and delete controls are not rendered in the HTML
    And only read-only content is displayed

  Scenario: 113 — Portal ConfigurationController shows write controls for ADMIN
    Given the operator is logged in with role ADMIN
    When the operator views the configuration pages
    Then create, edit, delete, approve, and reject controls are visible

  Scenario: 114 — Portal ConfigurationController shows write controls for CONFIGURATION_MANAGER
    Given the operator is logged in with role CONFIGURATION_MANAGER
    When the operator views the configuration pages
    Then create, edit, delete, approve, and reject controls are visible

  Scenario: 115 — New role ROLE_CONFIGURATION_MANAGER is introduced
    Given the system's role definitions
    Then ROLE_CONFIGURATION_MANAGER exists as a valid role
    And ROLE_CONFIGURATION_MANAGER grants config management without full ADMIN privileges

  Scenario: 116 — Demo mode includes a user with CONFIGURATION_MANAGER role
    Given the system is running in dev/demo mode
    When the demo data seeder runs
    Then at least one demo user is created with role CONFIGURATION_MANAGER

  # --- Constraints and cross-cutting ---

  Scenario: 117 — WCAG 2.1 AA compliance for all configuration pages
    When the operator views any configuration page in the caseworker portal
    Then all UI elements meet WCAG 2.1 AA accessibility standards per petition 013

  # ⚠️ Needs clarification: Petition does not specify exact WCAG testing criteria;
  # assumed to follow the same approach as petition 013.

  Scenario: 118 — Flyway migration adds review_status column and audit table
    Given the system starts up
    When Flyway migration V21__add_config_review_status_and_audit.sql executes
    Then the business_config table has a review_status column
    And the business_config_audit table exists with all columns per FR-11

  Scenario: 119 — Operators can override auto-computed derived rate values
    Given the operator sees auto-computed derived rates in the preview panel
    When the operator modifies any derived rate value
    Then the modified value is used instead of the auto-computed value
    And the system does not prevent manual overrides

  # ⚠️ Implied requirement based on "Constraints and assumptions" section:
  # "The derived-rate auto-computation is a convenience feature.
  # Operators can always override auto-computed values or enter rates manually."

  Scenario: 120 — Config key not found returns Danish error message
    Given the operator navigates to /konfiguration/NONEXISTENT_KEY
    Then the page displays the error message "Konfigurationsnøgle ikke fundet"

  # -------------------------------------------------------------------
  # Summary
  # -------------------------------------------------------------------
  #
  # ✅ Total functional requirements extracted: 122
  # 🔍 Confidence in completeness: High
  # ⚠️ Outstanding clarifications needed: 2
  #
  #   1. Scenario 117 — WCAG 2.1 AA: petition references petition 013 but
  #      does not define specific testing criteria for config pages.
  #
  #   2. Scenario 119 — Manual override of derived rates: stated in
  #      constraints section, not in a numbered FR. Included as implied.
  #
  # Petition FR coverage:
  #   FR-1  (REST API):            Scenarios 001–009
  #   FR-2  (Validation):          Scenarios 010–025
  #   FR-3  (Status field):        Scenarios 026–033b, 034–035
  #   FR-4  (Derived rates):       Scenarios 036–042
  #   FR-5  (Config list UI):      Scenarios 043–044, 045a–045b, 046–055
  #   FR-6  (Version history):     Scenarios 056–060
  #   FR-7  (Create form):         Scenarios 061–074
  #   FR-8  (Approve/reject):      Scenarios 075–080
  #   FR-9  (Portal controller):   Scenarios 081–089
  #   FR-10 (i18n Danish):         Scenarios 090–099
  #   FR-11 (Audit logging):       Scenarios 100–107
  #   FR-12 (Security):            Scenarios 108–116
  #   Cross-cutting:               Scenarios 117–120
  # -------------------------------------------------------------------
