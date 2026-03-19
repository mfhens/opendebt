Feature: Citizen portal landing page

  Scenario: Landing page is served at the portal root
    When a citizen navigates to the portal root path
    Then the landing page is returned with status 200
    And the page contains a heading about debt overview

  Scenario: Landing page displays FAQ with 7 items
    When a citizen navigates to the portal root path
    Then the page contains 7 FAQ items

  Scenario: Landing page includes a link to MitID self-service
    When a citizen navigates to the portal root path
    Then the page contains a link to the MitID self-service portal

  Scenario: Landing page explains interest accrues daily
    When a citizen navigates to the portal root path
    Then the page contains a notice about daily interest accrual

  Scenario: Landing page displays debt errors section
    When a citizen navigates to the portal root path
    Then the page contains a section about possible errors in older debt

  Scenario: Landing page supports Danish and English
    When a citizen navigates to the portal root path with language "da"
    Then the page heading is in Danish
    When a citizen navigates to the portal root path with language "en"
    Then the page heading is in English

  Scenario: Accessibility statement page is served
    When a citizen navigates to the accessibility statement path
    Then the accessibility statement page is returned with status 200

  Scenario: Landing page includes skip link and landmark roles
    When a citizen navigates to the portal root path
    Then the page contains a skip link to main content
    And the page contains a main landmark

  Scenario: Landing page includes language selector
    When a citizen navigates to the portal root path
    Then the page contains a language selector

  Scenario: Footer links to accessibility statement
    When a citizen navigates to the portal root path
    Then the footer contains a link to the accessibility statement

  Scenario: External URLs are configurable
    When a citizen navigates to the portal root path
    Then external links use configured URLs not hardcoded values
