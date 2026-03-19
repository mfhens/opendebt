Feature: Explicit hæftelse and support for multiple skyldnere

  Scenario: A fordring with one liable party is registered as enehæftelse
    Given a debt exists in the system
    When a SOLE liability is added for one debtor
    Then the liability is created with type "SOLE"
    And the liability is active

  Scenario: A fordring can have multiple solidarisk hæftende skyldnere
    Given a debt exists in the system
    When a JOINT_AND_SEVERAL liability is added for debtor 1
    And a JOINT_AND_SEVERAL liability is added for debtor 2
    Then 2 active liabilities exist for the debt

  Scenario: A fordring can have delt hæftelse with defined shares
    Given a debt exists in the system
    When a PROPORTIONAL liability is added for debtor 1 with 60 percent
    And a PROPORTIONAL liability is added for debtor 2 with 40 percent
    Then 2 active liabilities exist for the debt
    And the shares sum to 100 percent

  Scenario: SOLE liability rejects second party
    Given a debt exists with a SOLE liability
    When a second SOLE liability is attempted
    Then the system rejects with "SOLE liability requires exactly one"

  Scenario: PROPORTIONAL shares cannot exceed 100 percent
    Given a debt exists with a PROPORTIONAL liability at 70 percent
    When a PROPORTIONAL liability of 40 percent is attempted
    Then the system rejects with "exceed 100%"

  Scenario: Liability types cannot be mixed on same debt
    Given a debt exists with a JOINT_AND_SEVERAL liability
    When a PROPORTIONAL liability is attempted on the same debt
    Then the system rejects with "Cannot mix liability types"

  Scenario: Removing a liability deactivates it
    Given a debt exists with a SOLE liability
    When the liability is removed
    Then the liability is no longer active

  Scenario: Communication can resolve all liable skyldnere from hæftelse
    Given a debt exists with liabilities for 2 debtors
    When liabilities are queried for the debt
    Then both debtors are returned as liable parties
