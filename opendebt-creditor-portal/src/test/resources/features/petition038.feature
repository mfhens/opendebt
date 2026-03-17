Feature: Fordringshaverportal Dashboard, Navigation, and Settings

  Background:
    Given the creditor portal is running

  # --- Dashboard homepage ---

  Scenario: Dashboard displays summary claim counts
    Given a creditor user with acting creditor context
    When the user opens the dashboard homepage
    Then the page contains an HTMX-loadable claim counts section
    And the claim counts section references the claims count endpoint

  Scenario: Dashboard retains existing creditor profile card
    Given a creditor user with acting creditor context
    When the user opens the dashboard homepage
    Then the creditor profile card is displayed

  # --- Portal navigation ---

  Scenario: Portal pages include a secondary navigation component
    Given a creditor user with acting creditor context
    When the user opens the dashboard homepage
    Then a secondary navigation element is present with an aria-label
    And the navigation contains links to portal sections

  Scenario: External links are configurable and open in new tab
    Given the portal is configured with external link URLs
    When the user views the navigation
    Then external links open in a new tab
    And external link URLs come from application configuration

  Scenario: Navigation labels use message bundles
    Given a creditor user with acting creditor context
    When the user opens the dashboard homepage
    Then all navigation labels are rendered from i18n message bundles

  # --- Settings page ---

  Scenario: Settings page displays creditor agreement configuration
    Given a creditor user with acting creditor context
    When the user navigates to the settings page
    Then the settings page displays agreement configuration details
    And a contact email management section is shown

  # --- Claimant selection ---

  Scenario: Umbrella-user sees claimant selection page
    Given an umbrella-user is authenticated
    When the umbrella-user opens the claimant selection page
    Then a list of creditors is displayed for selection

  # --- System status ---

  Scenario: Navigation shows a system status indicator
    Given a creditor user with acting creditor context
    When the user opens the dashboard homepage
    Then a system status indicator is visible in the navigation area
