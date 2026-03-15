Feature: UI webtilgængelighed compliance

  Scenario: A core portal flow can be completed by keyboard only
    Given a user opens an OpenDebt portal
    When the user navigates a core flow using only the keyboard
    Then the user can complete the flow without requiring a mouse

  Scenario: Form validation is accessible
    Given a user submits an invalid form in an OpenDebt UI
    When validation errors are shown
    Then the errors are programmatically associated with the relevant fields
    And the user can perceive the errors without relying on color alone

  Scenario: Accessibility is part of release readiness
    Given a UI change is ready for release
    When release readiness is evaluated
    Then accessibility verification is included in the release gate
