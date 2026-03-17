Feature: Creditor portal notification search and download

  # --- Notification search (FR 1-3) ---

  Scenario: Notification search page is available to creditors
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    And user "U1" is bound to fordringshaver "K1"
    When user "U1" opens the notification search page
    Then the notification search page is displayed

  Scenario: Search parameters include date range and notification types
    Given portal user "U1" is on the notification search page
    Then the search form includes a date range picker with from and to fields
    And the search form includes checkboxes for each notification type the creditor is configured to receive

  Scenario: Search result displays count of matching notifications
    Given portal user "U1" is on the notification search page
    When user "U1" enters a date range and selects notification types
    And user "U1" submits the search
    Then the search result displays the count of matching notifications

  Scenario: Search with no matching notifications returns zero count
    Given portal user "U1" is on the notification search page
    When user "U1" enters a date range with no matching notifications
    And user "U1" submits the search
    Then the search result displays a count of 0 matching notifications

  # --- Notification download (FR 4-7) ---

  Scenario: Creditor can download matching notifications
    Given portal user "U1" has performed a notification search with matching results
    When user "U1" initiates a download
    Then the matching notifications are downloaded

  Scenario: Download format options include PDF and XML
    Given portal user "U1" has performed a notification search with matching results
    Then download format options are available: PDF and XML
    And each format is selectable via a checkbox

  Scenario: Download with PDF selected produces zip containing PDF files
    Given portal user "U1" has performed a notification search with matching results
    And user "U1" selects PDF format
    When user "U1" initiates the download
    Then a zip file is produced containing the matching notifications in PDF format

  Scenario: Download with XML selected produces zip containing XML files
    Given portal user "U1" has performed a notification search with matching results
    And user "U1" selects XML format
    When user "U1" initiates the download
    Then a zip file is produced containing the matching notifications in XML format

  Scenario: Download with both PDF and XML selected produces zip containing both formats
    Given portal user "U1" has performed a notification search with matching results
    And user "U1" selects both PDF and XML formats
    When user "U1" initiates the download
    Then a zip file is produced containing the matching notifications in both PDF and XML formats

  Scenario: Download progress is indicated to the user
    Given portal user "U1" initiates a notification download
    Then download progress is indicated to the user via HTMX polling or a progress indicator
    And the indicator disappears once the download is complete

  # --- Notification types (FR 8) ---

  Scenario: Available notification types are determined by creditor agreement
    Given portal user "U1" is on the notification search page
    And the creditor agreement for "K1" includes notification preferences
    Then only notification types configured in the agreement are shown as checkboxes

  Scenario: Interest notifications (Renteunderretninger) available when configured
    Given portal user "U1" is on the notification search page
    And the creditor agreement includes Renteunderretninger
    Then "Renteunderretninger" is available as a notification type checkbox

  Scenario: Detailed interest notifications (Detaljerede renteunderretninger) available when configured
    Given portal user "U1" is on the notification search page
    And the creditor agreement includes Detaljerede renteunderretninger
    Then "Detaljerede renteunderretninger" is available as a notification type checkbox

  Scenario: Equalisation notifications (Udligningsunderretninger) available when configured
    Given portal user "U1" is on the notification search page
    And the creditor agreement includes Udligningsunderretninger
    Then "Udligningsunderretninger" is available as a notification type checkbox

  Scenario: Allocation notifications (Allokeringsunderretninger) available when configured
    Given portal user "U1" is on the notification search page
    And the creditor agreement includes Allokeringsunderretninger
    Then "Allokeringsunderretninger" is available as a notification type checkbox

  Scenario: Settlement notifications (Afregningsunderretninger) available when configured
    Given portal user "U1" is on the notification search page
    And the creditor agreement includes Afregningsunderretninger
    Then "Afregningsunderretninger" is available as a notification type checkbox

  Scenario: Return notifications (Returunderretninger) available when configured
    Given portal user "U1" is on the notification search page
    And the creditor agreement includes Returunderretninger
    Then "Returunderretninger" is available as a notification type checkbox

  Scenario: Write-off notifications (Afskrivningsunderretninger) available when configured
    Given portal user "U1" is on the notification search page
    And the creditor agreement includes Afskrivningsunderretninger
    Then "Afskrivningsunderretninger" is available as a notification type checkbox

  Scenario: Notification type not configured in agreement is not shown
    Given portal user "U1" is on the notification search page
    And the creditor agreement does not include Returunderretninger
    Then "Returunderretninger" is not shown as a notification type checkbox

  # --- Access control (FR 9) ---

  Scenario: CREDITOR_VIEWER can access the notification search page
    Given portal user "U1" is authenticated with role "CREDITOR_VIEWER"
    When user "U1" opens the notification search page
    Then access is granted

  Scenario: CREDITOR_EDITOR can access the notification search page
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    When user "U2" opens the notification search page
    Then access is granted

  Scenario: User without CREDITOR_VIEWER or CREDITOR_EDITOR role is denied access
    Given portal user "U3" is authenticated without "CREDITOR_VIEWER" or "CREDITOR_EDITOR" roles
    When user "U3" attempts to open the notification search page
    Then access is denied

  Scenario: Unauthenticated user cannot access the notification search page
    Given a user is not authenticated
    When the user attempts to open the notification search page
    Then the user is redirected to the login page

  # --- Layout and accessibility (FR 10-15) ---

  Scenario: Notifications page uses the SKAT standardlayout
    Given portal user "U1" opens the notification search page
    Then the page uses the SKAT standardlayout from layout/default.html
    And the page includes a skip link, header, breadcrumb, main content area, and footer

  Scenario: Breadcrumb shows Forside > Underretninger
    Given portal user "U1" is on the notification search page
    Then the breadcrumb shows: Forside > Underretninger

  Scenario: Date range picker uses standard HTML date inputs with proper labels
    Given portal user "U1" is on the notification search page
    Then the date range picker uses standard HTML date input elements
    And each date input has a visible, associated label

  Scenario: Notification type checkboxes grouped in fieldset with legend
    Given portal user "U1" is on the notification search page
    Then the notification type checkboxes are grouped in a fieldset element
    And the fieldset has a legend element describing the group

  Scenario: Download status communicated to screen readers via aria-live
    Given portal user "U1" initiates a notification download
    Then the download status is communicated to screen readers via an aria-live region

  Scenario: All user-facing text uses message bundles with Danish and English
    Given the notification search page is rendered
    Then all user-facing text is loaded from message bundles
    And Danish and English translations are available for all text
    And no hardcoded text appears in the template
