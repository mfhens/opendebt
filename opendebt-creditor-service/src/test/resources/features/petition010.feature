# Petition 010: Fordringshaver channel binding and access resolution
# Outcome Contract: Centralized access resolution for fordringshaver channels
# Requirement traceability: petition010-fordringshaver-channel-binding-and-access-resolution-outcome-contract.md

Feature: Fordringshaver channel binding and access resolution

  Scenario: A bound M2M identity resolves to a fordringshaver
    Given M2M identity "CERT-123" is bound to fordringshaver "K1"
    When OpenDebt resolves access for identity "CERT-123"
    Then the acting fordringshaver is "K1"

  Scenario: An umbrella parent may act on behalf of a child fordringshaver
    Given fordringshaver "K_PARENT" may act on behalf of fordringshaver "K_CHILD"
    And portal user "U1" is bound to fordringshaver "K_PARENT"
    When OpenDebt resolves access for user "U1" acting for fordringshaver "K_CHILD"
    Then the represented fordringshaver is "K_CHILD"
    And the request is allowed

  Scenario: An unbound identity is rejected
    Given identity "UNKNOWN" is not bound to any fordringshaver
    When OpenDebt resolves access for identity "UNKNOWN"
    Then the request is rejected
