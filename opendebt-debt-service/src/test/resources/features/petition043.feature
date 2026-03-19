Feature: Batch processing for daily lifecycle transitions, interest accrual, and deadline monitoring

  Scenario: Batch transitions overdue unpaid claims to RESTANCE
    Given 3 claims in state REGISTERED with expired payment deadline and positive balance
    When the daily RESTANCE transition batch job runs
    Then 3 claims are transitioned to RESTANCE

  Scenario: Batch does not transition fully paid claims
    Given a claim in state REGISTERED with expired payment deadline and zero balance
    When the daily RESTANCE transition batch job runs
    Then the claim remains in state REGISTERED

  Scenario: RESTANCE transition batch is idempotent
    Given 2 claims in state REGISTERED with expired payment deadline and positive balance
    When the daily RESTANCE transition batch job runs
    And the daily RESTANCE transition batch job runs again for the same date
    Then the batch skips on the second run

  Scenario: Batch accrues daily interest on OVERDRAGET claims
    Given a claim in state OVERDRAGET with outstanding balance 100000.00
    When the daily interest accrual batch job runs
    Then an interest journal entry is created for the claim with amount 15.75

  Scenario: Interest accrual batch is idempotent
    Given a claim in state OVERDRAGET with outstanding balance 100000.00
    When the daily interest accrual batch job runs
    And the daily interest accrual batch job runs again for the same date
    Then the claim has exactly 1 interest journal entry

  Scenario: Batch detects approaching limitation deadline
    Given a claim with limitation date within 90 days
    When the daily deadline monitoring batch job runs
    Then the batch flags 1 approaching deadline

  Scenario: Batch job records execution metadata
    Given 2 claims eligible for RESTANCE transition
    When the daily RESTANCE transition batch job runs
    Then a batch execution record is created with 2 records processed
