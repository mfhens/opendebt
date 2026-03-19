Feature: Citizen Debt Summary Endpoint
  As an authenticated citizen
  I need to view my debt summary
  So that I can see what I owe and make payment decisions

  Background:
    Given the debt service is operational
    And person registry has resolved my CPR to a person_id

  Scenario: Citizen retrieves debt summary with authenticated person_id
    Given I am authenticated as a citizen with person_id
    And I have 3 active debts totaling 25000 kr
    When I request my debt summary
    Then I should receive a list of 3 debts
    And the total outstanding amount should be 25000 kr
    And each debt should include debt type, amounts, due date, and status
    And no PII should be present in the response
    And no creditor internal fields should be present

  Scenario: Citizen filters debts by status
    Given I am authenticated as a citizen with person_id
    And I have 2 active debts and 1 paid debt
    When I request my debt summary with status filter "ACTIVE"
    Then I should receive a list of 2 debts
    And all debts should have status "ACTIVE"

  Scenario: Citizen paginates through debt list
    Given I am authenticated as a citizen with person_id
    And I have 25 debts
    When I request my debt summary with page 0 and size 10
    Then I should receive 10 debts on the current page
    And the response should indicate page 0 of 3 total pages
    And the total debt count should be 25

  Scenario: Citizen with no debts sees empty list
    Given I am authenticated as a citizen with person_id
    And I have no debts
    When I request my debt summary
    Then I should receive an empty debt list
    And the total outstanding amount should be 0 kr
    And the total debt count should be 0

  Scenario: Unauthenticated user cannot access citizen debts
    Given I am not authenticated
    When I request the citizen debt summary endpoint
    Then I should receive a 401 Unauthorized response

  Scenario: Non-citizen role cannot access citizen debts
    Given I am authenticated with role "CASEWORKER"
    When I request the citizen debt summary endpoint
    Then I should receive a 403 Forbidden response

  Scenario: Person_id extraction from JWT works correctly
    Given I am authenticated via MitID OAuth2 flow
    And the authentication success handler has stored my person_id in the security context
    When I request my debt summary
    Then the service should extract my person_id from the authentication details
    And retrieve debts for that person_id only

  Scenario: Page size is limited to maximum 100
    Given I am authenticated as a citizen with person_id
    When I request my debt summary with page size 200
    Then the actual page size should be capped at 100

  Scenario: Invalid status filter returns bad request
    Given I am authenticated as a citizen with person_id
    When I request my debt summary with invalid status "INVALID_STATUS"
    Then I should receive a 400 Bad Request response

  Scenario: No PII leakage in citizen response
    Given I am authenticated as a citizen with person_id
    And my debts have associated creditor and debtor information
    When I request my debt summary
    Then the response should not contain any CPR numbers
    And the response should not contain any creditor organization IDs
    And the response should not contain any readiness status fields
    And the response should not contain any internal creditor references
