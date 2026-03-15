Feature: Fordringshaverportal as BFF and manual submission channel

  Scenario: The portal reads creditor profile from the backend service
    Given portal user "U1" is bound to fordringshaver "K1"
    When user "U1" opens the fordringshaver portal
    Then the portal reads creditor profile data from the creditor master data service

  Scenario: Manual fordring creation is submitted to debt-service
    Given portal user "U2" is allowed to create fordringer for fordringshaver "K2"
    When user "U2" submits a manual fordring in the portal
    Then the portal sends the request to debt-service
    And the portal does not persist the fordring as its own domain data

  Scenario: A portal user cannot act for an unrelated fordringshaver
    Given portal user "U3" is bound to fordringshaver "K3"
    And fordringshaver "K3" may not act on behalf of fordringshaver "K4"
    When user "U3" attempts to act for fordringshaver "K4"
    Then the request is rejected
