Feature: Batch processing for daily lifecycle transitions, interest accrual, and deadline monitoring

  # --- Daily RESTANCE transition ---

  Scenario: Batch transitions overdue unpaid claims to RESTANCE
    Given 3 claims in state REGISTERED with betalingsfrist before today and outstanding balance greater than zero
    And 2 claims in state REGISTERED with betalingsfrist in the future
    When the daily RESTANCE transition batch job runs
    Then 3 claims are transitioned to RESTANCE
    And 2 claims remain in state REGISTERED
    And 3 lifecycle events are recorded with previous state REGISTERED and new state RESTANCE

  Scenario: Batch does not transition fully paid claims
    Given a claim in state REGISTERED with betalingsfrist before today and outstanding balance equal to zero
    When the daily RESTANCE transition batch job runs
    Then the claim remains in state REGISTERED
    And no lifecycle event is recorded for the claim

  Scenario: RESTANCE transition batch is idempotent
    Given 2 claims in state REGISTERED with betalingsfrist before today and outstanding balance greater than zero
    When the daily RESTANCE transition batch job runs
    And the daily RESTANCE transition batch job runs again for the same date
    Then each claim has exactly 1 lifecycle event with new state RESTANCE

  # --- Daily interest accrual ---

  Scenario: Batch accrues daily interest on OVERDRAGET claims
    Given a claim in state OVERDRAGET with outstanding balance 100000.00 and received at 2026-02-15
    And the annual interest rate is 5.75 percent
    When the daily interest accrual batch job runs for date 2026-03-19
    Then an interest journal entry is created for the claim
    And the interest amount is 15.75

  Scenario: Batch does not accrue interest on terminal state claims
    Given a claim in state TILBAGEKALDT with outstanding balance 50000.00
    When the daily interest accrual batch job runs
    Then no interest journal entry is created for the claim

  Scenario: Interest journal entries are storno-compatible for crossing transactions
    Given a claim in state OVERDRAGET with outstanding balance 100000.00 and received at 2026-02-15
    And the annual interest rate is 5.75 percent
    When the daily interest accrual batch job runs for date 2026-03-19
    Then the interest journal entry contains the accrual date 2026-03-19
    And the interest journal entry contains the effective date 2026-03-19
    And the interest journal entry contains the balance snapshot 100000.00
    And the interest journal entry contains the rate 5.75

  Scenario: Interest accrual batch is idempotent
    Given a claim in state OVERDRAGET with outstanding balance 100000.00 and received at 2026-02-15
    When the daily interest accrual batch job runs for date 2026-03-19
    And the daily interest accrual batch job runs again for date 2026-03-19
    Then the claim has exactly 1 interest journal entry for date 2026-03-19

  # --- Deadline monitoring ---

  Scenario: Batch detects approaching foraeldelsesfrist
    Given a claim with limitation date 90 days from today
    And the foraeldelsesfrist warning threshold is 90 days
    When the daily deadline monitoring batch job runs
    Then the claim is flagged as approaching foraeldelsesfrist

  Scenario: Batch detects expired hoering SLA deadline
    Given a hoering with sla deadline before today and status AFVENTER_FORDRINGSHAVER
    When the daily deadline monitoring batch job runs
    Then the hoering is flagged as SLA expired

  # --- Operational ---

  Scenario: Batch job records execution metadata
    Given 5 claims eligible for RESTANCE transition
    When the daily RESTANCE transition batch job runs
    Then a batch execution record is created
    And the execution record contains the start time and end time
    And the execution record shows 5 records processed and 0 records failed
