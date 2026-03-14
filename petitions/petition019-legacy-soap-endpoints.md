# Petition 019: Legacy SOAP Endpoints for External Creditors

## Summary

OpenDebt debt-service shall expose traditional SOAP-based integration endpoints for external creditor systems (SKAT, EFI/DMI legacy systems). Two namespaces with three services each must be supported, providing backward-compatible operations for claim reporting, receipt retrieval, and notification collection retrieval. These endpoints use OCES3 certificate authentication and log all calls to CLS (Central Logging Service).

## Context and motivation

OpenDebt is designed to replace legacy debt collection systems (EFI/DMI) used by Danish public institutions. While the new API architecture uses REST with DUPLA, many existing legacy creditor systems do not support modern REST APIs and require SOAP-based integration formats and protocols.

The legacy systems that continue to use SOAP include:

1. **SKAT legacy systems** - Internal Danish Tax Agency systems running on older OIO-based SOAP formats
2. **EFI/DMI creditor systems** - External institutional systems that have not been migrated to modern APIs
3. **Legacy integration partners** - Systems with hardcoded SOAP client implementations

These systems cannot be immediately replaced or upgraded due to:
- Long lifecycle of public sector enterprise systems
- Budget constraints
- Dependency on legacy vendor platforms
- Migration risks during transition periods

OpenDebt must therefore maintain SOAP endpoints alongside its modern REST APIs to ensure continuity of service during the migration period (estimated 5-10 years).

## Functional requirements

### OIO Namespace Endpoints (`urn:oio:skat:efi:ws:1.0.1`)

The debt-service shall expose the following SOAP services under the OIO namespace:

1. **OIOFordringIndberetService**
   - Operation: `MFFordringIndberet_I`
   - Purpose: Report debt claims in OIO format
   - Input: OIO-formatted fordring (claim) messages
   - Output: Acknowledgement and claim ID

2. **OIOKvitteringHentService**
   - Operation: `MFKvitteringHent_I`
   - Purpose: Fetch receipts for submitted claims in OIO format
   - Input: Claim ID or receipt ID
   - Output: OIO-formatted receipt with status and metadata

3. **OIOUnderretSamlingHentService**
   - Operation: `MFUnderretSamlingHent_I`
   - Purpose: Fetch notification collections (underretninger) in OIO format
   - Input: Claim ID, debtor ID, or notification ID
   - Output: OIO-formatted list of notifications related to the claim

### SKAT/PSRM Namespace Endpoints (`http://skat.dk/begrebsmodel/2009/01/15/`)

The debt-service shall expose the following SOAP services under the SKAT namespace:

1. **SkatFordringIndberetService**
   - Operation: `MFFordringIndberet_I`
   - Purpose: Report debt claims in SKAT PSRM format
   - Input: SKAT-formatted fordring (claim) messages
   - Output: Acknowledgement and claim ID

2. **SkatKvitteringHentService**
   - Operation: `MFKvitteringHent_I`
   - Purpose: Fetch receipts for submitted claims
   - Input: Claim ID or receipt ID
   - Output: SKAT-formatted receipt with status and metadata

3. **SkatUnderretSamlingHentService**
   - Operation: `MFUnderretSamlingHent_I`
   - Purpose: Fetch notification collections (underretninger)
   - Input: Claim ID, debtor ID, or notification ID
   - Output: SKAT-formatted list of notifications related to the claim

### Message Format Requirements

4. Each SOAP service shall accept and return messages formatted according to its respective namespace schema:
   - OIO services use OIO XML schema definitions (XSD version 1.0.1)
   - SKAT services use SKAT Begrebsmodel XML schema (version 2009/01/15)

5. All SOAP services shall use the same operation names across both namespaces for consistency:
   - `MFFordringIndberet_I` for claim reporting
   - `MFKvitteringHent_I` for receipt retrieval
   - `MFUnderretSamlingHent_I` for notification retrieval

### Authentication and Security Requirements

6. All SOAP endpoints shall require OCES3 certificate-based authentication for system-to-system communication.

7. Certificate validation shall verify that the calling system has a valid OCES3 certificate issued by an approved Certificate Authority (CA).

8. Each SOAP service shall map certificate subject or issuer to a fordringshaver (creditor) identifier for authorization.

9. SOAP fault messages shall be returned for:
   - Invalid or expired certificates
   - Certificates from unauthorized systems
   - Malformed SOAP messages
   - Schema validation errors

### Logging and Audit Requirements

10. All SOAP calls shall be logged to CLS (Central Logging Service) with the following information:
    - Timestamp
    - Calling system identifier (from certificate)
    - Service and operation called
    - Message ID (if available in SOAP header)
    - Request/response correlation ID
    - Success/failure status
    - Response time

11. SOAP request and response bodies (excluding PII) shall be logged for audit purposes.

12. SOAP faults shall be logged with full error details and stack traces for troubleshooting.

### Integration Pattern Requirements

13. SOAP endpoints shall internally delegate to the same business logic used by REST APIs.

14. Claim reporting via SOAP shall follow the same validation rules as REST claim submission (petition015, petition016, petition017, petition018).

15. SOAP endpoints shall return synchronous responses only; asynchronous processing shall not be supported.

16. SOAP services shall maintain backward compatibility with legacy systems and shall not change message formats without proper versioning and notification to consumers.

### Error Handling Requirements

17. SOAP faults shall use standard SOAP fault structure with:
   - Fault code (SOAP 1.1) or Code (SOAP 1.2)
   - Fault string (human-readable message in Danish)
   - Detail element with structured error information (namespace-specific error codes)

18. Error codes shall be mapped from the underlying OpenDebt exception system to SOAP fault codes.

19. Validation errors shall return detailed field-level error information in the SOAP fault detail element.

### WSDL Exposure Requirements

20. All SOAP services shall expose WSDL documents at standard endpoints:
   - `/soap/oio?wsdl` for OIO namespace services
   - `/soap/skat?wsdl` for SKAT/PSRM namespace services

21. WSDL documents shall include:
   - Service definitions
   - Port types and operations
   - Message structures
   - XSD schema references
   - Binding information (SOAP 1.1 and/or SOAP 1.2)

## Non-functional requirements

1. SOAP endpoints shall support both SOAP 1.1 and SOAP 1.2 protocols.

2. SOAP endpoints shall have a response time SLA of < 2 seconds for successful operations under normal load.

3. SOAP endpoints shall support at least 100 concurrent connections.

4. SOAP services shall be stateless to support horizontal scaling.

5. SOAP message validation (schema validation) shall complete within 500ms per message.

6. SOAP endpoints shall include request ID tracking for end-to-end correlation.

7. SOAP endpoints shall use HTTPS with TLS 1.3 for transport security.

## Constraints and assumptions

- SOAP endpoints are public-facing but protected by OCES3 certificate authentication, not exposed through DUPLA
- Legacy SOAP systems do not support modern OAuth2/OIDC authentication patterns
- Existing WSDL contracts from legacy EFI/DMI systems are not available and must be reconstructed from documentation and requirement specifications
- SOAP endpoints are maintained alongside REST APIs during the transition period (estimated 5-10 years)
- OCES3 certificates are issued by approved CAs (e.g., MITID) and include fordringshaver identifier
- CLS integration is already in place or planned for the integration-gateway service

## Out of scope

- DUPLA integration for SOAP endpoints (not required – SOAP uses direct OCES3 authentication)
- Asynchronous SOAP patterns (e.g., WS-Addressing, WS-ReliableMessaging)
- SOAP message transformation or protocol bridging (e.g., SOAP to REST)
- Legacy system migration assistance or SOAP client upgrades
- Detailed WSDL schema definitions (implementation detail)
- SOAP versioning strategy beyond initial 1.0/OIO and 2009/01/15 SKAT implementations
- SOAP-based event-driven or push notification mechanisms
- Legacy client library implementations
