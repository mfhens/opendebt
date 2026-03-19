Feature: Debt Notification System (Underretning, Paakrav, Rykker)

  As a caseworker (sagsbehandler)
  I want to issue demand-for-payment and reminder notifications to debtors
  So that debtors are properly notified about their outstanding debts

  Background:
    Given a debt exists in the system with a known debtor

  Scenario: Issue demand for payment (paakrav) with OCR line
    When a caseworker issues a demand for payment for the debt
    Then a PAAKRAV notification is created
    And the notification includes a structured OCR payment reference line
    And the notification delivery state is PENDING
    And the notification channel is DIGITAL_POST

  Scenario: Issue reminder notice (rykker)
    When a caseworker issues a reminder for the debt
    Then a RYKKER notification is created
    And the notification does not include an OCR line
    And the notification delivery state is PENDING

  Scenario: Retrieve notification history for a debt
    Given a demand for payment was previously issued for the debt
    And a reminder was previously issued for the debt
    When the caseworker retrieves the notification history
    Then both notifications are returned in reverse chronological order

  Scenario: Notification records sender and recipient
    When a caseworker issues a demand for payment for the debt
    Then the notification records the creditor as sender
    And the notification records the debtor as recipient via person_id

  Scenario: Demand for payment for non-existent debt fails
    When a caseworker tries to issue a demand for a non-existent debt
    Then the request is rejected with an error

  Scenario: Reminder for non-existent debt fails
    When a caseworker tries to issue a reminder for a non-existent debt
    Then the request is rejected with an error

  Scenario: OCR line format follows FI71 standard
    When a caseworker issues a demand for payment for the debt
    Then the OCR line starts with "+71<" and ends with "+"
    And the OCR line contains a 16-character debt reference

  Scenario: Notification history is empty for new debt
    Given no notifications have been issued for the debt
    When the caseworker retrieves the notification history
    Then an empty list is returned

  Scenario: Only authorized users can issue notifications
    Given an unauthenticated user
    When they try to issue a demand for payment
    Then the request is rejected with 401 or 403

  Scenario: Notification does not contain PII
    When a caseworker issues a demand for payment for the debt
    Then the notification references debtor by person_id only
    And no CPR number appears in the notification or logs
