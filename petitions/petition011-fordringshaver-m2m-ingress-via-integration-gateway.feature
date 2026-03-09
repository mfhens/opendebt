Feature: Fordringshaver M2M ingress via integration-gateway

  Scenario: A creditor system submits a fordring through the gateway
    Given creditor system "SYS1" is authorized for fordringshaver "K1"
    When system "SYS1" submits a fordring through DUPLA and integration-gateway
    Then integration-gateway resolves acting fordringshaver "K1"
    And the request is routed to debt-service

  Scenario: An unauthorized creditor system is rejected before routing
    Given creditor system "SYS2" is not authorized for the requested operation
    When system "SYS2" calls the creditor M2M API
    Then integration-gateway rejects the request
    And the request is not routed to debt-service

  Scenario: Gateway propagates audit context to the owning service
    Given creditor system "SYS3" is authorized for fordringshaver "K3"
    When system "SYS3" submits a request through integration-gateway
    Then the routed request contains correlation and audit context
