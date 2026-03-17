Feature: Creditor portal report browsing and downloading

  # --- List reports (FR 1-4) ---

  Scenario: Reports page displays a list of reports for the selected year and month
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And user "U1" is bound to fordringshaver "K1"
    And fordringshaver "K1" has reports available in the reporting service
    When user "U1" opens the reports page
    And user "U1" selects year "2025" and month "03"
    Then the portal displays a list of available reports for fordringshaver "K1" for 2025-03

  Scenario: Year/month selector allows browsing available periods
    Given portal user "U1" is on the reports page
    Then a year/month selector is displayed
    And user "U1" can select different years and months to browse reports

  Scenario: Each report entry shows name/type and availability status
    Given portal user "U1" is viewing the reports list for a selected period
    Then each report entry displays the report name or type
    And each report entry displays the availability status

  Scenario: Reconciliation summary files are filtered out from the report list
    Given portal user "U1" is viewing the reports list for a selected period
    And the reporting service returns reconciliation summary files among the reports
    Then reconciliation summary files are not displayed in the report list

  Scenario: Reports list is empty when no reports exist for the selected period
    Given portal user "U1" is on the reports page
    And no reports exist for the selected year and month
    When user "U1" selects that period
    Then the portal displays a message indicating no reports are available for the selected period

  # --- Download report (FR 5-8) ---

  Scenario: Creditor downloads an individual report
    Given portal user "U1" is viewing the reports list
    And a report is available for download
    When user "U1" clicks the download button for that report
    Then the report is downloaded

  Scenario: Downloaded file is served as application/zip with Content-Disposition
    Given portal user "U1" downloads a report
    Then the response content type is "application/zip"
    And the response includes a Content-Disposition header with an appropriate filename

  Scenario: Download progress is indicated to the user
    Given portal user "U1" initiates a report download
    Then download progress is indicated to the user
    And the indicator disappears once the download is complete

  Scenario: Report downloads are logged to the audit log
    Given portal user "U1" downloads a report
    Then the download event is logged to the audit log
    And the audit log entry includes the user identity, creditor identity, report identifier, and timestamp

  # --- Security (FR 9-11) ---

  Scenario: BFF enforces creditor-only access to reports
    Given portal user "U1" is bound to fordringshaver "K1"
    And a report belongs to fordringshaver "K2"
    When user "U1" attempts to access the report belonging to fordringshaver "K2"
    Then access is denied
    And user "U1" cannot download or view the report

  Scenario: Report access is validated against acting creditor context via channel binding
    Given portal user "U1" is authenticated and bound to fordringshaver "K1"
    When user "U1" requests the reports list
    Then the BFF validates the request against the acting creditor context resolved via channel binding

  Scenario: No client-side key encryption is used
    Given a report download request is made
    Then access control is enforced server-side by the BFF and backend service
    And no client-side key encryption scheme is involved

  # --- Access control (FR 12) ---

  Scenario: CREDITOR_VIEWER can access reports
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    When user "U1" opens the reports page
    Then access is granted

  Scenario: CREDITOR_EDITOR can access reports
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    When user "U2" opens the reports page
    Then access is granted

  Scenario: CREDITOR_RECONCILIATION can access reports
    Given portal user "U3" is authenticated with role "CREDITOR_RECONCILIATION"
    When user "U3" opens the reports page
    Then access is granted

  Scenario: CREDITOR_SUPPORT can access reports
    Given portal user "U4" is authenticated with role "CREDITOR_SUPPORT"
    When user "U4" opens the reports page
    Then access is granted

  Scenario: User without any creditor role is denied access to reports
    Given portal user "U5" is authenticated without any creditor role
    When user "U5" attempts to open the reports page
    Then access is denied

  Scenario: Unauthenticated user cannot access reports
    Given a user is not authenticated
    When the user attempts to open the reports page
    Then the user is redirected to the login page

  # --- Layout and accessibility (FR 13-18) ---

  Scenario: Reports page uses the SKAT standardlayout
    Given portal user "U1" opens the reports page
    Then the page uses the SKAT standardlayout from layout/default.html
    And the page includes a skip link, header, breadcrumb, main content area, and footer

  Scenario: Breadcrumb shows Forside > Rapporter
    Given portal user "U1" is on the reports page
    Then the breadcrumb shows: Forside > Rapporter

  Scenario: Year/month selector uses standard HTML select elements with proper labels
    Given portal user "U1" is on the reports page
    Then the year selector is a standard HTML select element with a visible label
    And the month selector is a standard HTML select element with a visible label

  Scenario: Download buttons are keyboard-accessible with aria-label
    Given portal user "U1" is viewing the reports list with available reports
    Then each download button is keyboard-accessible
    And each download button has an appropriate aria-label describing the report to download

  Scenario: Download status announced to screen readers via aria-live
    Given portal user "U1" initiates a report download
    Then the download status is communicated to screen readers via an aria-live region
    And completion is announced when the download finishes

  Scenario: All user-facing text uses message bundles with Danish and English
    Given the reports page is rendered
    Then all user-facing text is loaded from message bundles
    And Danish and English translations are available for all text
    And no hardcoded text appears in the template
