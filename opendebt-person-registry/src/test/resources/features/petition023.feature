Feature: Person Registry CPR Lookup API
  As an internal service
  I need to lookup or create person records by CPR
  So that I can reference citizens by technical UUID only

  Background:
    Given the person registry is operational
    And encryption service is configured

  Scenario: Lookup existing person by CPR
    Given a person exists with CPR "1234567890" and role "PERSONAL"
    When I lookup person by CPR "1234567890" with role "PERSONAL"
    Then the response should contain a person UUID
    And the response should indicate the person was not created
    And no CPR should be present in the response

  Scenario: Create new person when CPR not found
    Given no person exists with CPR "9876543210"
    When I lookup person by CPR "9876543210" with role "PERSONAL" and name "John Doe"
    Then the response should contain a person UUID
    And the response should indicate the person was created
    And a new person record should exist in the database
    And the CPR should be stored encrypted
    And the identifier hash should be computed correctly

  Scenario: Idempotent lookup - concurrent requests do not create duplicates
    Given no person exists with CPR "5555555555"
    When I lookup person by CPR "5555555555" with role "PERSONAL" concurrently 3 times
    Then exactly one person record should exist for CPR "5555555555"

  Scenario: Get person details by UUID
    Given a person exists with UUID and CPR "1112223330" and name "Jane Smith"
    When I get person details by UUID
    Then the response should contain decrypted name "Jane Smith"
    And the response should contain identifier type "CPR"
    And the response should contain role "PERSONAL"
    And the person access count should be incremented

  Scenario: Get person returns 404 when person not found
    Given a random non-existent person UUID
    When I get person details by that UUID
    Then the response should be 404 Not Found

  Scenario: Get deleted person returns 404
    Given a person exists with CPR "9998887770" and has been marked as deleted
    When I get person details by that person UUID
    Then the response should indicate the person is deleted

  Scenario: Check person existence - person exists
    Given a person exists with CPR "3334445550"
    When I check if that person UUID exists
    Then the response should be true

  Scenario: Check person existence - person does not exist
    Given a random non-existent person UUID
    When I check if that person UUID exists
    Then the response should be false

  Scenario: Lookup with optional metadata stores encrypted fields
    Given no person exists with CPR "7778889990"
    When I lookup person by CPR "7778889990" with metadata:
      | name           | email               | phone       | addressStreet  | addressCity | addressPostalCode | addressCountry |
      | Alice Anderson | alice@example.com   | +4512345678 | Main Street 1  | Copenhagen  | 1000              | Denmark        |
    Then the person should be created with encrypted metadata
    And retrieving the person should return decrypted metadata

  Scenario: No CPR in logs
    Given logging is captured
    When I lookup person by CPR "1231231230" with role "PERSONAL"
    Then no CPR number should appear in any log messages
    And log messages should reference person UUID only
