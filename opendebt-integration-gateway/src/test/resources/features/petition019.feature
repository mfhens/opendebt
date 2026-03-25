Feature: Legacy SOAP Endpoints for External Creditors

  The debt-service exposes SOAP-based integration endpoints for external creditor systems
  (SKAT, EFI/DMI legacy systems) that require SOAP for backward compatibility.

  Background:
    Given integration-gateway is running and listening for SOAP requests
    And CLS logging is configured and accessible
    And SOAP endpoint security is configured for OCES3 certificate validation
    And fordringshaver "CREDITOR-001" has a valid OCES3 certificate
    And fordringshaver "CREDITOR-001" is authorized to submit claims

  # OIO Namespace Endpoint Tests

  Scenario: OIOFordringIndberetService accepts valid OIO claim submission
    Given fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request is addressed to OIOFordringIndberetService
    And the SOAP request is in OIO namespace "urn:oio:skat:efi:ws:1.0.1"
    And the SOAP request operation is "MFFordringIndberet_I"
    And the SOAP request body contains a valid OIO-formatted claim
    When the SOAP request is processed
    Then SOAP response is successful with HTTP 200
    And SOAP response contains an acknowledgement
    And SOAP response contains claim ID "CLAIM-001"
    And CLS logging records the SOAP call with calling system "CREDITOR-001"
    And CLS logging records service "OIOFordringIndberetService" and operation "MFFordringIndberet_I"

  Scenario: OIOFordringIndberetService applies same validation rules as REST API
    Given fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request is addressed to OIOFordringIndberetService
    And the SOAP request operation is "MFFordringIndberet_I"
    And the SOAP request body contains an OIO-formatted claim with invalid amount (0 DKK)
    When the SOAP request is processed
    Then SOAP response is a fault with HTTP 500
    And SOAP fault contains error code corresponding to claim validation failure
    And SOAP fault detail contains field-level error information for the amount field

  Scenario: OIOKvitteringHentService returns receipt for valid claim ID
    Given claim "CLAIM-001" exists with status "SUBMITTED"
    And claim "CLAIM-001" has receipt "RECEIPT-001"
    And fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request is addressed to OIOKvitteringHentService
    And the SOAP request is in OIO namespace "urn:oio:skat:efi:ws:1.0.1"
    And the SOAP request operation is "MFKvitteringHent_I"
    And the SOAP request body contains claim ID "CLAIM-001"
    When the SOAP request is processed
    Then SOAP response is successful with HTTP 200
    And SOAP response contains OIO-formatted receipt "RECEIPT-001"
    And SOAP response includes claim status "SUBMITTED"
    And CLS logging records the SOAP call with calling system "CREDITOR-001"

  Scenario: OIOUnderretSamlingHentService returns notification collection for valid claim
    Given claim "CLAIM-001" exists with debtor "DEBTOR-001"
    And claim "CLAIM-001" has 3 notifications
    And fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request is addressed to OIOUnderretSamlingHentService
    And the SOAP request is in OIO namespace "urn:oio:skat:efi:ws:1.0.1"
    And the SOAP request operation is "MFUnderretSamlingHent_I"
    And the SOAP request body contains claim ID "CLAIM-001"
    When the SOAP request is processed
    Then SOAP response is successful with HTTP 200
    And SOAP response contains OIO-formatted notification collection
    And SOAP response includes 3 notifications
    And CLS logging records the SOAP call with calling system "CREDITOR-001"

  # SKAT/PSRM Namespace Endpoint Tests

  Scenario: SkatFordringIndberetService accepts valid SKAT claim submission
    Given fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request is addressed to SkatFordringIndberetService
    And the SOAP request is in SKAT namespace "http://skat.dk/begrebsmodel/2009/01/15/"
    And the SOAP request operation is "MFFordringIndberet_I"
    And the SOAP request body contains a valid SKAT-formatted claim
    When the SOAP request is processed
    Then SOAP response is successful with HTTP 200
    And SOAP response contains an acknowledgement
    And SOAP response contains claim ID "CLAIM-002"
    And CLS logging records the SOAP call with calling system "CREDITOR-001"
    And CLS logging records service "SkatFordringIndberetService" and operation "MFFordringIndberet_I"

  Scenario: SkatFordringIndberetService applies same validation rules as REST API
    Given fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request is addressed to SkatFordringIndberetService
    And the SOAP request operation is "MFFordringIndberet_I"
    And the SOAP request body contains a SKAT-formatted claim with missing required field
    When the SOAP request is processed
    Then SOAP response is a fault with HTTP 500
    And SOAP fault contains error code corresponding to claim validation failure
    And SOAP fault detail contains field-level error information for the missing field

  Scenario: SkatKvitteringHentService returns receipt for valid claim ID
    Given claim "CLAIM-002" exists with status "SUBMITTED"
    And claim "CLAIM-002" has receipt "RECEIPT-002"
    And fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request is addressed to SkatKvitteringHentService
    And the SOAP request is in SKAT namespace "http://skat.dk/begrebsmodel/2009/01/15/"
    And the SOAP request operation is "MFKvitteringHent_I"
    And the SOAP request body contains claim ID "CLAIM-002"
    When the SOAP request is processed
    Then SOAP response is successful with HTTP 200
    And SOAP response contains SKAT-formatted receipt "RECEIPT-002"
    And SOAP response includes claim status "SUBMITTED"
    And CLS logging records the SOAP call with calling system "CREDITOR-001"

  Scenario: SkatUnderretSamlingHentService returns notification collection for valid claim
    Given claim "CLAIM-002" exists with debtor "DEBTOR-002"
    And claim "CLAIM-002" has 2 notifications
    And fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request is addressed to SkatUnderretSamlingHentService
    And the SOAP request is in SKAT namespace "http://skat.dk/begrebsmodel/2009/01/15/"
    And the SOAP request operation is "MFUnderretSamlingHent_I"
    And the SOAP request body contains claim ID "CLAIM-002"
    When the SOAP request is processed
    Then SOAP response is successful with HTTP 200
    And SOAP response contains SKAT-formatted notification collection
    And SOAP response includes 2 notifications
    And CLS logging records the SOAP call with calling system "CREDITOR-001"

  # Authentication and Security Tests

  Scenario: SOAP request without certificate returns authentication fault
    Given no OCES3 certificate is provided with the SOAP request
    And the SOAP request is addressed to OIOFordringIndberetService
    And the SOAP request contains a valid OIO-formatted claim
    When the SOAP request is processed
    Then SOAP response is a fault with HTTP 401
    And SOAP fault code indicates authentication failure
    And SOAP fault message (in Danish) describes certificate requirement
    And CLS logging records the failed SOAP call with authentication error

  Scenario: SOAP request with expired certificate returns authentication fault
    Given fordringshaver "CREDITOR-001" has expired OCES3 certificate "CERT-EXPIRED"
    And the SOAP request is addressed to OIOFordringIndberetService
    And the SOAP request contains a valid OIO-formatted claim
    When the SOAP request is processed
    Then SOAP response is a fault with HTTP 403
    And SOAP fault code indicates authentication failure
    And SOAP fault message (in Danish) describes certificate expiration
    And CLS logging records the failed SOAP call with authentication error

  Scenario: SOAP request from unauthorized system returns authorization fault
    Given fordringshaver "CREDITOR-UNAUTHORIZED" has valid OCES3 certificate "CERT-999"
    And fordringshaver "CREDITOR-UNAUTHORIZED" is not authorized to submit claims
    And the SOAP request is addressed to OIOFordringIndberetService
    And the SOAP request contains a valid OIO-formatted claim
    When the SOAP request is processed
    Then SOAP response is a fault with HTTP 403
    And SOAP fault code indicates authorization failure
    And SOAP fault message (in Danish) describes authorization requirement
    And CLS logging records the failed SOAP call with authorization error

  Scenario: Certificate subject is mapped to fordringshaver identifier
    Given OCES3 certificate "CERT-001" has subject "CN=CREDITOR-001,O=UFST"
    And the SOAP request is addressed to OIOFordringIndberetService
    And the SOAP request body contains a valid OIO-formatted claim
    When the SOAP request is processed
    Then the fordringshaver identifier is extracted from certificate subject
    And the fordringshaver identifier "CREDITOR-001" is used for authorization

  # Logging and Audit Tests

  Scenario: Successful SOAP call is logged to CLS
    Given fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request is addressed to OIOFordringIndberetService
    And the SOAP request contains a valid OIO-formatted claim
    When the SOAP request is processed
    Then CLS logging contains timestamp of the SOAP call
    And CLS logging contains calling system identifier "CREDITOR-001"
    And CLS logging contains service name "OIOFordringIndberetService"
    And CLS logging contains operation name "MFFordringIndberet_I"
    And CLS logging contains success status "SUCCESS"
    And CLS logging contains correlation ID from SOAP headers
    And CLS logging contains response time in milliseconds

  Scenario: SOAP fault is logged to CLS with error details
    Given fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request is addressed to OIOFordringIndberetService
    And the SOAP request contains an invalid OIO-formatted claim
    When the SOAP request is processed
    Then CLS logging contains timestamp of the SOAP call
    And CLS logging contains calling system identifier "CREDITOR-001"
    And CLS logging contains service name "OIOFordringIndberetService"
    And CLS logging contains operation name "MFFordringIndberet_I"
    And CLS logging contains failure status "FAULT"
    And CLS logging contains error code from SOAP fault
    And CLS logging contains error message from SOAP fault
    And CLS logging contains stack trace for troubleshooting

  Scenario: SOAP request and response bodies are logged excluding PII
    Given fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request is addressed to OIOFordringIndberetService
    And the SOAP request contains a valid OIO-formatted claim with debtor CPR
    When the SOAP request is processed
    Then CLS logging contains SOAP request body
    And CLS logging does not contain debtor CPR number
    And CLS logging does not contain debtor name
    And CLS logging contains SOAP response body
    And CLS logging contains debtor identifier as UUID reference only

  # Error Handling Tests

  Scenario: SOAP validation error includes field-level details
    Given fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request is addressed to OIOFordringIndberetService
    And the SOAP request contains an OIO-formatted claim with amount -100 DKK
    When the SOAP request is processed
    Then SOAP response is a fault with HTTP 500
    And SOAP fault contains fault code
    And SOAP fault contains Danish human-readable message
    And SOAP fault detail element contains field "amount"
    And SOAP fault detail element contains error message "Amount must be positive"

  Scenario: Malformed SOAP message returns schema validation fault
    Given fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request is addressed to OIOFordringIndberetService
    And the SOAP request does not conform to SOAP 1.1 or SOAP 1.2 specification
    When the SOAP request is processed
    Then SOAP response is a fault with HTTP 400
    And SOAP fault code indicates client error
    And SOAP fault message (in Danish) describes SOAP schema validation error

  # Protocol Compatibility Tests

  Scenario: SOAP endpoint accepts requests using SOAP 1.1 protocol
    Given fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request uses SOAP 1.1 protocol
    And the SOAP request is addressed to OIOFordringIndberetService
    When the SOAP request is processed
    Then SOAP response uses SOAP 1.1 protocol
    And SOAP response is successful with HTTP 200

  Scenario: SOAP endpoint accepts requests using SOAP 1.2 protocol
    Given fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request uses SOAP 1.2 protocol
    And the SOAP request is addressed to OIOFordringIndberetService
    When the SOAP request is processed
    Then SOAP response uses SOAP 1.2 protocol
    And SOAP response is successful with HTTP 200

  # WSDL Exposure Tests

  Scenario: WSDL for OIO services is accessible
    When a GET request is made to "/soap/oio?wsdl"
    Then response is successful with HTTP 200
    And response content type is "application/wsdl+xml"
    And WSDL contains service definitions for OIOFordringIndberetService
    And WSDL contains service definitions for OIOKvitteringHentService
    And WSDL contains service definitions for OIOUnderretSamlingHentService
    And WSDL contains port type for each service
    And WSDL contains operation "MFFordringIndberet_I"
    And WSDL contains operation "MFKvitteringHent_I"
    And WSDL contains operation "MFUnderretSamlingHent_I"
    And WSDL references XSD schema for OIO namespace
    And WSDL contains message structure definitions for all operations
    And WSDL contains binding definitions for SOAP 1.1
    And WSDL contains binding definitions for SOAP 1.2

  Scenario: WSDL for SKAT services is accessible
    When a GET request is made to "/soap/skat?wsdl"
    Then response is successful with HTTP 200
    And response content type is "application/wsdl+xml"
    And WSDL contains service definitions for SkatFordringIndberetService
    And WSDL contains service definitions for SkatKvitteringHentService
    And WSDL contains service definitions for SkatUnderretSamlingHentService
    And WSDL contains port type for each service
    And WSDL contains operation "MFFordringIndberet_I"
    And WSDL contains operation "MFKvitteringHent_I"
    And WSDL contains operation "MFUnderretSamlingHent_I"
    And WSDL references XSD schema for SKAT namespace
    And WSDL contains message structure definitions for all operations
    And WSDL contains binding definitions for SOAP 1.1
    And WSDL contains binding definitions for SOAP 1.2

  # Statelessness and Scalability Tests

  Scenario: SOAP services are stateless
    Given a SOAP request to OIOFordringIndberetService contains correlation ID "CORR-001"
    When the SOAP request is processed
    Then SOAP response contains correlation ID "CORR-001"
    And no session state is maintained between SOAP requests
    And SOAP service can handle requests from any connection


  # SOAP Version Compatibility Tests

  Scenario: SOAP 1.1 fault response uses correct SOAP 1.1 fault envelope structure
    Given fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request uses SOAP 1.1 protocol
    And the SOAP request body contains an OIO-formatted claim with invalid amount
    When the SOAP request is processed
    Then SOAP response is a fault with HTTP 500
    And SOAP fault envelope contains element "faultcode"
    And SOAP fault envelope contains element "faultstring"
    And SOAP fault envelope contains element "detail"

  Scenario: SOAP 1.2 fault response uses correct SOAP 1.2 fault envelope structure
    Given fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request uses SOAP 1.2 protocol
    And the SOAP request body contains an OIO-formatted claim with invalid amount
    When the SOAP request is processed
    Then SOAP response is a fault with HTTP 500
    And SOAP fault envelope contains element "Code"
    And SOAP fault envelope contains element "Reason"
    And SOAP fault envelope contains element "Detail"

  # Synchronous Processing and Schema Validation Tests

  Scenario: SOAP services return synchronous responses only
    Given fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request is addressed to OIOFordringIndberetService
    And the SOAP request body contains a valid OIO-formatted claim
    When the SOAP request is processed
    Then SOAP response is returned in the same HTTP connection
    And SOAP response does not contain WS-Addressing ReplyTo headers
    And SOAP response does not contain asynchronous callback references

  Scenario: OIO SOAP messages are validated against OIO XSD schema version 1.0.1
    Given fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request is addressed to OIOFordringIndberetService
    And the SOAP request body is a valid instance of OIO XSD schema "urn:oio:skat:efi:ws:1.0.1"
    When the SOAP request is processed
    Then SOAP response is successful with HTTP 200
    And SOAP message validation used OIO XSD schema "urn:oio:skat:efi:ws:1.0.1"

  Scenario: SKAT SOAP messages are validated against SKAT XSD schema version 2009/01/15
    Given fordringshaver "CREDITOR-001" has OCES3 certificate "CERT-001"
    And the SOAP request is addressed to SkatFordringIndberetService
    And the SOAP request body is a valid instance of SKAT XSD schema "http://skat.dk/begrebsmodel/2009/01/15/"
    When the SOAP request is processed
    Then SOAP response is successful with HTTP 200
    And SOAP message validation used SKAT XSD schema "http://skat.dk/begrebsmodel/2009/01/15/"
