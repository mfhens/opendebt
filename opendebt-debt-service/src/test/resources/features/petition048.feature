Feature: Petition048 RBAC authorization convergence
  As OpenDebt security controls
  I need role-scoped access and downstream re-validation
  So that unauthorized cross-service access is blocked per ADR-0007

  Background:
    Given RBAC test debts are seeded for multiple debtors and creditor organizations

  Scenario: Citizen can access own debt and is denied other citizen debt
    Given a citizen auth context for debtor person A
    When the citizen requests access to a debt owned by person A
    Then debt-service should grant debt access
    When the citizen requests access to a debt owned by person B
    Then debt-service should deny debt access

  Scenario: Creditor can access own claim and is denied other creditor claim
    Given a creditor auth context for organization A
    When the creditor requests access to a claim owned by organization A
    Then debt-service should grant claim access
    When the creditor requests access to a claim owned by organization B
    Then debt-service should deny claim access

  Scenario: Downstream service re-validates authorization independently of upstream assumptions
    Given upstream case-service context claims debtor person A
    And a citizen auth context for debtor person B
    When debt-service re-validates access to a debt owned by person A
    Then debt-service should deny debt access

  Scenario: Admin override remains unrestricted
    Given an admin auth context
    When the admin requests access to a debt owned by person B
    Then debt-service should grant debt access
    When the admin requests access to a claim owned by organization B
    Then debt-service should grant claim access
