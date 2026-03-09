Feature: Accessibility statements and feedback for each UI

  Scenario: A web UI exposes a discoverable accessibility statement
    Given an OpenDebt web site is published
    When a user looks for accessibility information
    Then the user can find a link to the accessibility statement

  Scenario: The written contact path is accessible
    Given a user encounters inaccessible content in an OpenDebt UI
    When the user follows the written contact path from the accessibility statement
    Then the user can contact the responsible authority without using MitID login
    And the user is not blocked by inaccessible CAPTCHA

  Scenario: Accessibility statement maintenance is required after relevant change
    Given a UI change affects the accessibility information for an OpenDebt web site
    When the change is prepared for release
    Then the accessibility statement must be updated
