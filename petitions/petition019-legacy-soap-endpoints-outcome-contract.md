# Petition 019 Outcome Contract

## Acceptance criteria

### OIO Namespace Endpoints
1. A SOAP request to `OIOFordringIndberetService` operation `MFFordringIndberet_I` with valid OIO-formatted claim returns an acknowledgement with claim ID.
2. A SOAP request to `OIOKvitteringHentService` operation `MFKvitteringHent_I` with valid claim ID returns OIO-formatted receipt.
3. A SOAP request to `OIOUnderretSamlingHentService` operation `MFUnderretSamlingHent_I` with valid claim ID returns OIO-formatted notification collection.
4. OIO namespace services are exposed under namespace `urn:oio:skat:efi:ws:1.0.1`.

### SKAT/PSRM Namespace Endpoints
5. A SOAP request to `SkatFordringIndberetService` operation `MFFordringIndberet_I` with valid SKAT-formatted claim returns an acknowledgement with claim ID.
6. A SOAP request to `SkatKvitteringHentService` operation `MFKvitteringHent_I` with valid claim ID returns SKAT-formatted receipt.
7. A SOAP request to `SkatUnderretSamlingHentService` operation `MFUnderretSamlingHent_I` with valid claim ID returns SKAT-formatted notification collection.
8. SKAT namespace services are exposed under namespace `http://skat.dk/begrebsmodel/2009/01/15/`.

### Authentication and Security
9. A SOAP request without a valid OCES3 certificate returns a SOAP fault with authentication error.
10. A SOAP request with an expired or revoked OCES3 certificate returns a SOAP fault with authentication error.
11. A SOAP request with a certificate from an unauthorized system returns a SOAP fault with authorization error.
12. Certificate subject or issuer is mapped to fordringshaver identifier for authorization decisions.

### Logging and Audit
13. Each successful SOAP call is logged to CLS with timestamp, calling system identifier, service/operation, and correlation ID.
14. Each SOAP fault is logged to CLS with full error details and stack trace.
15. SOAP request and response bodies (excluding PII) are logged for audit.
16. CLS logging includes response time for performance monitoring.

### Integration Patterns
17. Claim reporting via SOAP executes the same validation rules as REST claim submission.
18. SOAP endpoints delegate to the same business logic as REST APIs for all operations.
19. SOAP services return synchronous responses only; no asynchronous processing is supported.
20. SOAP message validation uses the same XSD schemas as legacy EFI/DMI systems.

### Error Handling
21. SOAP validation errors include field-level error information in the fault detail element.
22. SOAP faults use standard SOAP fault structure with fault code, fault string (Danish), and detail element.
23. Error codes from OpenDebt exceptions are mapped to SOAP fault codes appropriately.
24. Malformed SOAP messages return a SOAP fault with schema validation error code.

### WSDL Exposure
25. WSDL for OIO namespace services is accessible at `/soap/oio?wsdl`.
26. WSDL for SKAT namespace services is accessible at `/soap/skat?wsdl`.
27. WSDL documents include service definitions, port types, operations, message structures, schema references, and bindings.
28. WSDL references the correct XSD schemas for message validation (OIO or SKAT format).

### Non-functional Requirements
29. SOAP endpoints support both SOAP 1.1 and SOAP 1.2 protocols.
30. SOAP endpoint response time is less than 2 seconds for successful operations under normal load.
31. SOAP endpoints support at least 100 concurrent connections without degradation.
32. SOAP services are stateless and support horizontal scaling.
33. SOAP message schema validation completes within 500ms per message.
34. SOAP endpoints include request ID tracking for end-to-end correlation.
35. SOAP endpoints use HTTPS with TLS 1.3 for transport security.

## Definition of done

- All 6 SOAP services (3 OIO + 3 SKAT) are implemented and deployed
- All SOAP services conform to namespace specifications (`urn:oio:skat:efi:ws:1.0.1` and `http://skat.dk/begrebsmodel/2009/01/15/`)
- OCES3 certificate authentication is implemented and tested for all services
- CLS logging is configured and verified for all SOAP calls
- WSDL documents are generated and accessible at the specified endpoints
- All acceptance criteria are covered by integration tests
- Performance benchmarks confirm 2-second response time SLA under 100 concurrent connections
- Security audit confirms TLS 1.3 enforcement and certificate validation
- Error handling is tested with valid certificates, invalid certificates, and malformed messages
- SOAP services reuse the same business logic as REST APIs (verified through code review)
- All SOAP faults include Danish human-readable messages

## Failure conditions

- SOAP services are not exposed under the correct namespaces
- WSDL documents are not accessible or incomplete
- SOAP requests without certificates are accepted
- SOAP requests with invalid certificates are not rejected with appropriate faults
- CLS logging is missing or incomplete for SOAP calls
- SOAP endpoints perform schema validation slower than 500ms
- SOAP endpoint response time exceeds 2-second SLA under 100 concurrent connections
- SOAP services cannot handle 100 concurrent connections
- SOAP errors do not include field-level validation details in fault detail
- SOAP faults do not include Danish error messages
- SOAP services are not stateless (state is maintained between requests)
- SOAP services do not support both SOAP 1.1 and SOAP 1.2 protocols
- SOAP endpoints do not use TLS 1.3
- SOAP business logic differs from REST API business logic
- SOAP messages are not logged to CLS
