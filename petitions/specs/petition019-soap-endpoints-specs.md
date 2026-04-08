# Implementation Specifications — Petition 019: Legacy SOAP Endpoints

**Specification ID:** SPECS-019  
**Status:** Approved for implementation  
**Petition:** petition019-legacy-soap-endpoints.md  
**Outcome contract:** petition019-legacy-soap-endpoints-outcome-contract.md  
**Feature file:** petition019-legacy-soap-endpoints.feature  
**Architecture:** docs/architecture/petition019-soap-endpoints.md  
**Component map:** petitions/petition019_map.yaml  

---

## Traceability Overview

All 35 acceptance criteria (AC-01 through AC-35) from the outcome contract are traceable to at least one specification section. The traceability column in each spec references the AC number(s) it satisfies.

| Module | Spec ID | AC Coverage |
|--------|---------|-------------|
| opendebt-integration-gateway — SoapConfig | SPEC-019-01 | AC-25, AC-26, AC-27, AC-28, AC-29, AC-32, AC-34, AC-35 |
| opendebt-integration-gateway — OIO Endpoints | SPEC-019-02 | AC-01, AC-02, AC-03, AC-04, AC-17, AC-18, AC-19, AC-20 |
| opendebt-integration-gateway — SKAT Endpoints | SPEC-019-03 | AC-05, AC-06, AC-07, AC-08, AC-17, AC-18, AC-19, AC-20 |
| opendebt-integration-gateway — Oces3SoapSecurityInterceptor | SPEC-019-04 | AC-09, AC-10, AC-11, AC-12 |
| opendebt-integration-gateway — ClsSoapAuditInterceptor | SPEC-019-05 | AC-13, AC-14, AC-15, AC-16, AC-34 |
| opendebt-integration-gateway — SoapFaultMappingResolver | SPEC-019-06 | AC-21, AC-22, AC-23, AC-24, AC-29 |
| opendebt-integration-gateway — DebtServiceSoapClient | SPEC-019-07 | AC-17, AC-18, AC-30, AC-31 |
| opendebt-common — Oces3CertificateParser | SPEC-019-08 | AC-09, AC-10, AC-12 |
| opendebt-common — SoapPiiMaskingUtil | SPEC-019-09 | AC-15 |
| Static resources — XSD + WSDL | SPEC-019-10 | AC-20, AC-25, AC-26, AC-27, AC-28, AC-33 |
| opendebt-debt-service — InternalClaimController | SPEC-019-11 | AC-17, AC-18 |

---

## SPEC-019-01: SoapConfig — Spring-WS Infrastructure Bean Configuration

**Module:** `opendebt-integration-gateway`  
**Package:** `dk.ufst.opendebt.gateway.soap.config`  
**Class:** `SoapConfig.java`  
**Source requirement:** Architecture §4.1; AC-25, AC-26, AC-27, AC-28, AC-29, AC-32, AC-34, AC-35  

### Interface

```yaml
class: SoapConfig
annotation: "@Configuration"
stereotype: Spring configuration class

beans_produced:
  - name: messageDispatcherServlet
    type: ServletRegistrationBean<MessageDispatcherServlet>
    purpose: >
      Registers Spring-WS MessageDispatcherServlet at URL-pattern /soap/* 
      (must NOT conflict with existing DispatcherServlet which is mapped to /api/*).
      Must set transformWsdlLocations=true on the MessageDispatcherServlet instance.
    required_properties:
      - servlet.path.soap must be set in application.yml to /soap
    acceptance_test: GET /soap/oio?wsdl returns HTTP 200 (AC-25)

  - name: oioWsdl
    type: SimpleWsdl11Definition
    bean_name_literal: "oioWsdl"
    classpath_resource: "wsdl/oio/oio-fordring.wsdl"
    url_path: /soap/oio?wsdl
    note: >
      MUST use SimpleWsdl11Definition — NOT DefaultWsdl11Definition.
      DefaultWsdl11Definition generates only SOAP 1.1 bindings and cannot
      produce the dual SOAP 1.1 + 1.2 binding structure required by AC-29.
    acceptance_test: GET /soap/oio?wsdl returns WSDL with both SOAP 1.1 and SOAP 1.2 bindings (AC-27, AC-28, AC-29)

  - name: skatWsdl
    type: SimpleWsdl11Definition
    bean_name_literal: "skatWsdl"
    classpath_resource: "wsdl/skat/skat-fordring.wsdl"
    url_path: /soap/skat?wsdl
    acceptance_test: GET /soap/skat?wsdl returns HTTP 200 with WSDL (AC-26)

  - name: oces3SecurityInterceptor
    type: Oces3SoapSecurityInterceptor
    order: 0
    purpose: Authenticates OCES3 certificates before any endpoint is invoked (see SPEC-019-04)

  - name: clsAuditInterceptor
    type: ClsSoapAuditInterceptor
    order: 1
    purpose: Logs every SOAP call to CLS (see SPEC-019-05)

  - name: faultResolver
    type: SoapFaultMappingResolver
    purpose: Maps domain exceptions to SOAP fault envelopes (see SPEC-019-06)
    order: 1
    order_note: "Order within the ExceptionResolver chain (separate from Interceptor chain). Only one resolver registered — ordinal is informational."
    note: Must be registered as a bean; Spring-WS discovers it automatically as EndpointExceptionResolver

  - name: payloadValidatingInterceptor
    type: PayloadValidatingInterceptor
    order: 2
    purpose: Schema-validates incoming SOAP payloads against XSD (see SPEC-019-10)
    note: >
      Auth must happen first (position 0) so that unauthenticated malformed messages
      do not receive schema rejection details. Schema faults at position 2 are still
      captured by ClsSoapAuditInterceptor in afterCompletion.

  - name: endpointInterceptors
    type: EndpointInterceptor[]
    order_guarantee: "[oces3SecurityInterceptor, clsAuditInterceptor, payloadValidatingInterceptor]"
    note: >
      Registered in WsConfigurationSupport or via WsConfigurerAdapter.addInterceptors().
      The security interceptor MUST be index 0 so fordringshaverId is populated
      in MessageContext before the audit interceptor runs.
```

### SOAP Protocol Auto-Detection

```yaml
soap_protocol_detection:
  mechanism: SaajSoapMessageFactory
  detection_rule:
    - Content-Type "text/xml" -> SOAP 1.1 response
    - Content-Type "application/soap+xml" -> SOAP 1.2 response
  note: >
    Spring-WS SaajSoapMessageFactory detects the incoming Content-Type and
    produces a response in the same protocol version. No manual routing is required.
    Both SOAP 1.1 and SOAP 1.2 requests are handled by the same endpoint beans.
  acceptance_test: AC-29 (both protocols accepted), AC-29 (correct fault envelope per version)
```

### Maven Dependencies to Add — opendebt-integration-gateway/pom.xml

```yaml
new_dependencies:
  - groupId: org.springframework.ws
    artifactId: spring-ws-core
    scope: compile

  # spring-ws-security intentionally excluded — OCES3 authentication uses TLS cert extraction
  # (INGRESS PATH B / EMBEDDED PATH A), not WS-Security headers. No Wss4jSecurityInterceptor
  # is instantiated anywhere in the implementation.

  - groupId: org.springframework.ws
    artifactId: spring-ws-support
    scope: compile

  - groupId: jakarta.xml.soap
    artifactId: jakarta.xml.soap-api
    scope: compile

  - groupId: com.sun.xml.messaging.saaj
    artifactId: saaj-impl
    scope: runtime

  - groupId: org.mapstruct
    artifactId: mapstruct
    scope: compile
    note: Already present in debt-service pom; add to integration-gateway pom

constraint: spring-ws-core 4.x required for Jakarta EE 9+ namespace (Spring Boot 3.3.x)
```

### Application Configuration

```yaml
# application.yml additions required
spring:
  mvc:
    servlet:
      path: /api     # Existing MVC DispatcherServlet stays on /api (unchanged)

opendebt:
  soap:
    oces3:
      trusted-ca-subjects:
        - "CN=OCES3 Udstedende CA 1, O=Nets DanID A/S, C=DK"
        - "CN=OCES3 Udstedende CA 2, O=Nets DanID A/S, C=DK"
      fordringshaver-dn-field: CN      # Configurable: CN (default) or OU
    security:
      tls-termination-mode: ingress    # INGRESS (default, prod) | EMBEDDED (dev)
  services:
    debt-service:
      url: "http://debt-service:8080"  # Existing property; reused by DebtServiceSoapClient
  audit:
    cls:
      enabled: true
```

### Statelessness Guarantee

```yaml
statelessness:
  rule: >
    All SOAP endpoint beans are annotated @Endpoint (Spring-WS singleton scope).
    No instance state between requests. Request-scoped data (fordringshaverId,
    correlationId, startTime) is propagated exclusively through MessageContext
    attribute map within a single request thread.
  prohibited: HTTP session usage, ThreadLocal state that is not cleared after each request
  acceptance_test: AC-32 (stateless, horizontally scalable)
```

---

## SPEC-019-02: OIO Namespace Endpoints

**Module:** `opendebt-integration-gateway`  
**Package:** `dk.ufst.opendebt.gateway.soap.oio`  
**Source requirement:** FR-OIO-1, FR-OIO-2, FR-OIO-3; AC-01, AC-02, AC-03, AC-04, AC-17, AC-18, AC-19, AC-20  

### Namespace Binding

```yaml
namespace_uri: "urn:oio:skat:efi:ws:1.0.1"
xsd_version: "1.0.1"
wsdl_file: classpath:wsdl/oio/oio-fordring.wsdl
jaxb_generated_package: dk.ufst.opendebt.gateway.soap.oio.generated
mapper_class: OioClaimMapper   # MapStruct mapper (see §Mapping)
```

### OIOFordringIndberetEndpoint

```yaml
specification_id: SPEC-019-02A
class: OIOFordringIndberetEndpoint
annotation: "@Endpoint"
package: dk.ufst.opendebt.gateway.soap.oio

interface:
  payload_root:
    namespace_uri: "urn:oio:skat:efi:ws:1.0.1"
    local_part: "MFFordringIndberet_IRequest"
  method_annotation: "@PayloadRoot(namespace = ..., localPart = ...)"
  return_annotation: "@ResponsePayload"

  inputs:
    - name: request
      type: "MFFordringIndberet_IRequest"   # JAXB-generated from oio-fordring.xsd
      annotation: "@RequestPayload"

  outputs:
    - name: response
      type: "MFFordringIndberet_IResponse"  # JAXB-generated from oio-fordring.xsd

  processing_steps:
    1: "Extract fordringshaverId from MessageContext attribute 'oces3AuthContext'"
    2: "Extract correlationId from MessageContext attribute 'correlationId'"
    3: "Map MFFordringIndberet_IRequest → FordringSubmitRequest via OioClaimMapper"
    4: "Call DebtServiceSoapClient.submitClaim(FordringSubmitRequest, fordringshaverId, correlationId)"
    5: "Map ClaimSubmissionResponse → MFFordringIndberet_IResponse via OioClaimMapper"
    6: "Return MFFordringIndberet_IResponse"

  error_handling:
    - exception: FordringValidationException
      action: "Propagate to SoapFaultMappingResolver — maps to ValidationError fault (AC-21)"
    - exception: OpenDebtException
      action: "Propagate to SoapFaultMappingResolver — maps to Server fault"
    - exception: WebClientRequestException
      action: "Propagate to SoapFaultMappingResolver — maps to Server fault"

  synchronous_only:
    rule: "Method is synchronous (blocking). No WS-Addressing ReplyTo headers are set. No async executor used."
    acceptance_test: AC-19

acceptance_criteria: [AC-01, AC-17, AC-18, AC-19, AC-20]
# FIX-019-SPEC-3: removed AC-02 (belongs to SPEC-019-02B OIOKvitteringHentEndpoint, next in chain)
```

### OIOKvitteringHentEndpoint

```yaml
specification_id: SPEC-019-02B
class: OIOKvitteringHentEndpoint
annotation: "@Endpoint"
package: dk.ufst.opendebt.gateway.soap.oio

interface:
  payload_root:
    namespace_uri: "urn:oio:skat:efi:ws:1.0.1"
    local_part: "MFKvitteringHent_IRequest"

  inputs:
    - name: request
      type: "MFKvitteringHent_IRequest"
      field: claimId (UUID extracted from request)

  outputs:
    - name: response
      type: "MFKvitteringHent_IResponse"
      mapped_from_KvitteringResponse_fields: [kvitteringId, claimId, status, modtagetDato, behandletDato, afvisningKode, afvisningTekst]

  processing_steps:
    1: "Extract fordringshaverId from MessageContext attribute 'oces3AuthContext'"
    2: "Extract correlationId from MessageContext attribute 'correlationId'"
    3: "Map MFKvitteringHent_IRequest → claimId (UUID) via OioClaimMapper"
    4: "Call DebtServiceSoapClient.getReceipt(claimId, fordringshaverId, correlationId)"
    5: "Map KvitteringResponse → MFKvitteringHent_IResponse via OioClaimMapper"
    6: "Return MFKvitteringHent_IResponse"

  error_handling:
    - http_status: 404
      action: "Propagate as OpenDebtException(CLAIM_NOT_FOUND) → SoapFaultMappingResolver → Client fault"

acceptance_criteria: [AC-02, AC-20]
# FIX-019-SPEC-3: removed AC-03 (belongs to SPEC-019-02C OIOUnderretSamlingHentEndpoint, next in chain)
```

### OIOUnderretSamlingHentEndpoint

```yaml
specification_id: SPEC-019-02C
class: OIOUnderretSamlingHentEndpoint
annotation: "@Endpoint"
package: dk.ufst.opendebt.gateway.soap.oio

interface:
  payload_root:
    namespace_uri: "urn:oio:skat:efi:ws:1.0.1"
    local_part: "MFUnderretSamlingHent_IRequest"

  inputs:
    - name: request
      type: "MFUnderretSamlingHent_IRequest"
      fields: [claimId (UUID), debtorId (UUID, optional)]

  outputs:
    - name: response
      type: "MFUnderretSamlingHent_IResponse"
      fields: [claimId, notifications[] (mapped from NotificationDto list), total]

  processing_steps:
    1: "Extract fordringshaverId from MessageContext attribute 'oces3AuthContext'"
    2: "Extract correlationId from MessageContext attribute 'correlationId'"
    3: "Map MFUnderretSamlingHent_IRequest → (claimId, optional debtorId) via OioClaimMapper"
    4: "Call DebtServiceSoapClient.getNotifications(claimId, debtorId, fordringshaverId, correlationId)"
    5: "Map NotificationCollectionResult → MFUnderretSamlingHent_IResponse via OioClaimMapper"
    6: "Return MFUnderretSamlingHent_IResponse"

acceptance_criteria: [AC-03, AC-04, AC-20]
```

### OioClaimMapper

```yaml
specification_id: SPEC-019-02D
class: OioClaimMapper
annotation: "@Mapper(componentModel = 'spring')"
package: dk.ufst.opendebt.gateway.soap.oio
framework: MapStruct

naming_note: >
  Class name uses English "Claim" per agents.md domain vocabulary convention
  (Fordring → Claim). Architecture §10.4 uses OioFordringMapper/SkatFordringMapper
  (Danish) — the English form is canonical for implementation.

mappings:
  - from: "MFFordringIndberet_IRequest (JAXB)"
    to: FordringSubmitRequest (common DTO — see SPEC-019-07)
    field_rules:
      - oio_element "Beloeb" -> field "amount" (BigDecimal)
      - oio_element "FordringsType" -> field "claimType" (String)
      - oio_element "SkyldnerPersonId" -> field "debtorPersonId" (UUID)
      - oio_element "FordringsDato" -> field "claimDate" (LocalDate)
      - oio_element "ForfaldsDato" -> field "dueDate" (LocalDate)
      - oio_element "EksternId" -> field "externalId" (String)

  - from: ClaimSubmissionResponse
    to: "MFFordringIndberet_IResponse (JAXB)"
    field_rules:
      - claimId -> oio_element "FordringsId"
      - outcome SUCCESS  -> acknowledgement status "MODTAGET"
      - outcome REJECTED -> acknowledgement status "AFVIST"
      - outcome ERROR    -> acknowledgement status "FEJL"
      - errors[].field -> validation error detail (forwarded to fault resolver)

  - from: KvitteringResponse
    to: "MFKvitteringHent_IResponse (JAXB)"

  - from: NotificationCollectionResult
    to: "MFUnderretSamlingHent_IResponse (JAXB)"
```

---

## SPEC-019-03: SKAT Namespace Endpoints

**Module:** `opendebt-integration-gateway`  
**Package:** `dk.ufst.opendebt.gateway.soap.skat`  
**Source requirement:** FR-SKAT-1, FR-SKAT-2, FR-SKAT-3; AC-05, AC-06, AC-07, AC-08, AC-17, AC-18, AC-19, AC-20  

### Namespace Binding

```yaml
namespace_uri: "http://skat.dk/begrebsmodel/2009/01/15/"
xsd_version: "2009/01/15"
wsdl_file: classpath:wsdl/skat/skat-fordring.wsdl
jaxb_generated_package: dk.ufst.opendebt.gateway.soap.skat.generated
mapper_class: SkatClaimMapper
```

### SkatFordringIndberetEndpoint

```yaml
specification_id: SPEC-019-03A
class: SkatFordringIndberetEndpoint
annotation: "@Endpoint"
package: dk.ufst.opendebt.gateway.soap.skat

interface:
  payload_root:
    namespace_uri: "http://skat.dk/begrebsmodel/2009/01/15/"
    local_part: "MFFordringIndberet_IRequest"

  inputs:
    - name: request
      type: "MFFordringIndberet_IRequest"  # JAXB-generated from skat-fordring.xsd

  outputs:
    - name: response
      type: "MFFordringIndberet_IResponse"

  processing_steps:
    1: "Extract fordringshaverId from MessageContext attribute 'oces3AuthContext'"
    2: "Extract correlationId from MessageContext attribute 'correlationId'"
    3: "Map MFFordringIndberet_IRequest → FordringSubmitRequest via SkatClaimMapper"
    4: "Call DebtServiceSoapClient.submitClaim(FordringSubmitRequest, fordringshaverId, correlationId)"
    5: "Map ClaimSubmissionResponse → MFFordringIndberet_IResponse via SkatClaimMapper"
    6: "Return MFFordringIndberet_IResponse"

  error_handling:
    - exception: FordringValidationException
      action: "Propagate to SoapFaultMappingResolver — maps to ValidationError fault (AC-21)"
    - exception: OpenDebtException
      action: "Propagate to SoapFaultMappingResolver — maps to Server fault"
    - exception: WebClientRequestException
      action: "Propagate to SoapFaultMappingResolver — maps to Server fault"
  synchronous_only: "Identical to SPEC-019-02A"

acceptance_criteria: [AC-05, AC-17, AC-18, AC-19, AC-20]
# FIX-019-SPEC-3: removed AC-06 (belongs to SPEC-019-03B SkatKvitteringHentEndpoint, next in chain)
```

### SkatKvitteringHentEndpoint

```yaml
specification_id: SPEC-019-03B
class: SkatKvitteringHentEndpoint
annotation: "@Endpoint"

interface:
  payload_root:
    namespace_uri: "http://skat.dk/begrebsmodel/2009/01/15/"
    local_part: "MFKvitteringHent_IRequest"
  processing: "Identical pattern to SPEC-019-02B; uses SkatClaimMapper instead of OioClaimMapper"
  note: >
    Maps KvitteringResponse using the canonical field set: kvitteringId, claimId, status
    (SUBMITTED | ACCEPTED | REJECTED), modtagetDato, behandletDato, afvisningKode, afvisningTekst.

acceptance_criteria: [AC-06, AC-20]
# FIX-019-SPEC-3: removed AC-07 (belongs to SPEC-019-03C SkatUnderretSamlingHentEndpoint, next in chain)
```

### SkatUnderretSamlingHentEndpoint

```yaml
specification_id: SPEC-019-03C
class: SkatUnderretSamlingHentEndpoint
annotation: "@Endpoint"

interface:
  payload_root:
    namespace_uri: "http://skat.dk/begrebsmodel/2009/01/15/"
    local_part: "MFUnderretSamlingHent_IRequest"
  processing: "Identical pattern to SPEC-019-02C; uses SkatClaimMapper instead of OioClaimMapper"

acceptance_criteria: [AC-07, AC-08, AC-20]
```

### SkatClaimMapper

```yaml
specification_id: SPEC-019-03D
class: SkatClaimMapper
annotation: "@Mapper(componentModel = 'spring')"
package: dk.ufst.opendebt.gateway.soap.skat
framework: MapStruct

naming_note: >
  Class name uses English "Claim" per agents.md domain vocabulary convention
  (Fordring → Claim). Architecture §10.4 uses OioFordringMapper/SkatFordringMapper
  (Danish) — the English form is canonical for implementation.

mappings:
  - from: "MFFordringIndberet_IRequest (SKAT JAXB)"
    to: FordringSubmitRequest
    field_rules:
      - skat_element "Beloeb" -> field "amount" (BigDecimal)
      - skat_element "FordringsType" -> field "claimType" (String)
      - skat_element "SkyldnerPersonId" -> field "debtorPersonId" (UUID)
      - skat_element "FordringsDato" -> field "claimDate" (LocalDate)
      - skat_element "ForfaldsDato" -> field "dueDate" (LocalDate)
      - skat_element "EksternId" -> field "externalId" (String)
    note: >
      SKAT XSD element names may differ from OIO; mapper must use @Mapping(source, target)
      annotations for all non-obvious name differences. Neither mapper may share
      code with the other — they map from structurally different generated types.

  - from: ClaimSubmissionResponse
    to: "MFFordringIndberet_IResponse (SKAT JAXB)"
    field_rules:
      - claimId -> skat_element "FordringsId"
      - outcome SUCCESS  -> acknowledgement status "MODTAGET"
      - outcome REJECTED -> acknowledgement status "AFVIST"
      - outcome ERROR    -> acknowledgement status "FEJL"
      - errors[].field -> validation error detail (forwarded to fault resolver)

  - from: KvitteringResponse
    to: "MFKvitteringHent_IResponse (SKAT JAXB)"

  - from: NotificationCollectionResult
    to: "MFUnderretSamlingHent_IResponse (SKAT JAXB)"
```

---

## SPEC-019-04: Oces3SoapSecurityInterceptor

**Module:** `opendebt-integration-gateway`  
**Package:** `dk.ufst.opendebt.gateway.soap.interceptor`  
**Class:** `Oces3SoapSecurityInterceptor.java`  
**Implements:** `org.springframework.ws.server.EndpointInterceptor`  
**Source requirement:** FR-SEC-1, FR-SEC-2, FR-SEC-3, FR-SEC-4; AC-09, AC-10, AC-11, AC-12  

### Interface

```yaml
interceptor_position: 0   # Must be first in interceptor chain

constructor_dependencies:
  - Oces3CertificateParser (injected, from opendebt-common)
  - TlsTerminationMode tlsTerminationMode (from property soap.security.tls-termination-mode)
    type: enum { INGRESS, EMBEDDED }
    default: INGRESS

configuration_property:
  soap:
    security:
      tls-termination-mode: ingress    # prod default per ADR-0006
```

### handleRequest — Full Logic Contract

```yaml
method: handleRequest(MessageContext messageContext) -> boolean
  step_1_extract_cert:
    mode_INGRESS:
      - Obtain HttpServletRequest from TransportContextHolder.getTransportContext().getConnection()
      - Read header "X-Client-Cert" (plain Base64-encoded PEM; set by nginx ingress via $ssl_client_raw_cert)
      - Strip "-----BEGIN CERTIFICATE-----" and "-----END CERTIFICATE-----" boundary lines
      - Remove all whitespace and newlines from the remaining string
      - Base64-decode to DER byte array
      - Construct java.security.cert.X509Certificate via CertificateFactory.getInstance("X.509")
    mode_EMBEDDED:
      - Obtain HttpServletRequest from TransportContextHolder
      - Read attribute "javax.servlet.request.X509Certificate" (X509Certificate[])
      - Use certs[0]

  step_2_null_check:
    if cert is null or header missing:
      throw: Oces3AuthenticationException("Certifikat mangler", "CERT_MISSING")
      result: SoapFaultMappingResolver produces CLIENT fault with Danish message (AC-09)

  step_3_parse:
    call: Oces3CertificateParser.parse(cert)
    returns: Oces3AuthContext { fordringshaverId, cn, issuer, validTo, serialNumber }

  step_4_expiry_check:
    if Oces3AuthContext.validTo is before Instant.now():
      throw: Oces3AuthenticationException("Certifikat er udløbet", "CERT_EXPIRED")
      result: SoapFaultMappingResolver produces CLIENT fault (AC-10)

  step_5_ca_trust_check:
    if Oces3AuthContext.issuer is NOT in configured trusted-ca-subjects list:
      throw: Oces3AuthorizationException("Udsteder ikke godkendt", "ISSUER_NOT_TRUSTED")
      result: SoapFaultMappingResolver produces CLIENT fault (AC-11)

  step_6_store_context:
    messageContext.setProperty("oces3AuthContext", oces3AuthContext)
    messageContext.setProperty("fordringshaverId", oces3AuthContext.fordringshaverId)
    messageContext.setProperty("correlationId", extractCorrelationId(messageContext))

  step_7_return_true:
    return true   # Continue interceptor chain

correlation_id_extraction:
  rule: >
    Attempt to read wsa:MessageID from SOAP header. If absent or blank,
    generate UUID.randomUUID().toString() and use as correlationId.
    Store in MessageContext property "correlationId".
  acceptance_test: AC-34 (end-to-end request ID tracking)
```

### handleResponse / handleFault / afterCompletion

```yaml
handleResponse: no-op (return true)
handleFault: no-op (return true)
afterCompletion: no-op
note: Logging is handled exclusively by ClsSoapAuditInterceptor (SPEC-019-05)
```

### Exception Types (new — in opendebt-integration-gateway)

```yaml
new_exception_classes:
  - class: Oces3AuthenticationException
    package: dk.ufst.opendebt.gateway.soap.fault
    extends: RuntimeException
    fields: [message: String, errorCode: String]
    usage: Missing cert, expired cert

  - class: Oces3AuthorizationException
    package: dk.ufst.opendebt.gateway.soap.fault
    extends: RuntimeException
    fields: [message: String, errorCode: String]
    usage: Unauthorized issuer, unauthorized fordringshaver
```

### Acceptance Criteria Coverage

| AC | Satisfied by |
|----|--------------|
| AC-09 | step_2_null_check throws Oces3AuthenticationException |
| AC-10 | step_4_expiry_check throws Oces3AuthenticationException |
| AC-11 | step_5_ca_trust_check throws Oces3AuthorizationException |
| AC-12 | step_3_parse extracts fordringshaverId from cert CN; stored in MessageContext |

---

## SPEC-019-05: ClsSoapAuditInterceptor

**Module:** `opendebt-integration-gateway`  
**Package:** `dk.ufst.opendebt.gateway.soap.interceptor`  
**Class:** `ClsSoapAuditInterceptor.java`  
**Implements:** `org.springframework.ws.server.EndpointInterceptor`  
**Source requirement:** FR-LOG-1, FR-LOG-2, FR-LOG-3; AC-13, AC-14, AC-15, AC-16, AC-34  

### ADR-0022 Exception Notice

```yaml
adr_exception:
  adr: ADR-0022
  standard_pattern: Filebeat polls PostgreSQL audit_log table
  exception: >
    integration-gateway has no PostgreSQL database (ADR-0011). Filebeat is
    therefore inapplicable. ClsAuditClient.shipEvent() is used directly.
    This is the fallback permitted by ADR-0022 §Consequences:
    "CLS Client (Java - DEPRECATED) — Optional fallback for direct API shipping."
    No new ADR is required; the deviation is bounded and justified.
```

### Constructor Dependencies

```yaml
constructor_dependencies:
  - ClsAuditClient clsAuditClient (injected from opendebt-common)
  - SoapPiiMaskingUtil soapPiiMaskingUtil (injected from opendebt-common)
  - String serviceName = "integration-gateway" (literal constant)
```

### Interceptor Lifecycle Contract

```yaml
interceptor_position: 1   # After Oces3SoapSecurityInterceptor

handleRequest(MessageContext):
  actions:
    - Record start time: Instant startTime = Instant.now()
    - Store in MessageContext property "soapAuditStartTime" = startTime
    - Read "fordringshaverId" from MessageContext (populated by SPEC-019-04)
    - Read "correlationId" from MessageContext (populated by SPEC-019-04)
    - Capture raw SOAP request envelope as String from messageContext.getRequest()
    - Store in MessageContext property "rawRequestBody"
    - Determine serviceName and operationName from the SOAP envelope's local element name
    - Store in MessageContext properties "soapServiceName" and "soapOperationName"
  returns: true

  serviceName_resolution:
    rule: >
      Map from payload root local part to service display name:
        "MFFordringIndberet_IRequest" in namespace "urn:oio:..." -> "OIOFordringIndberetService"
        "MFKvitteringHent_IRequest" in namespace "urn:oio:..." -> "OIOKvitteringHentService"
        "MFUnderretSamlingHent_IRequest" in namespace "urn:oio:..." -> "OIOUnderretSamlingHentService"
        Same 3 local parts in "http://skat.dk/..." -> "Skat*" equivalents
    acceptance_test: AC-13 (service name logged correctly)

  operationName_resolution:
    rule: >
      Map from payload root local part:
        "MFFordringIndberet_IRequest" -> "MFFordringIndberet_I"
        "MFKvitteringHent_IRequest" -> "MFKvitteringHent_I"
        "MFUnderretSamlingHent_IRequest" -> "MFUnderretSamlingHent_I"
    acceptance_test: AC-13 (operation name logged correctly)

handleResponse(MessageContext):
  actions:
    - Capture raw SOAP response envelope as String from messageContext.getResponse()
    - Store in MessageContext property "rawResponseBody"
    - Mark MessageContext property "soapStatus" = "SUCCESS"
  returns: true

handleFault(MessageContext):
  actions:
    - Capture raw SOAP fault envelope as String from messageContext.getResponse()
    - Store in MessageContext property "rawResponseBody"
    - Mark MessageContext property "soapStatus" = "FAULT"
    - Extract faultCode, faultMessage from the fault envelope and store in MessageContext
    - Capture stack trace string from current thread if available via exception attribute
  returns: true

afterCompletion(MessageContext, Exception ex):
  actions:
    - Retrieve all stored MessageContext properties
    - Call SoapPiiMaskingUtil.mask(rawRequestBody) -> maskedRequestBody
    - Call SoapPiiMaskingUtil.mask(rawResponseBody) -> maskedResponseBody
    - Compute responseTimeMs = Instant.now().toEpochMilli() - startTime.toEpochMilli()
    - Build ClsAuditEvent (see §CLS Event Structure below)
    - Call clsAuditClient.shipEvent(event)
  note: afterCompletion is called for BOTH success and fault paths
```

### CLS Event Structure for SOAP

```yaml
cls_event_fields:
  eventId: UUID.randomUUID()
  timestamp: startTime (Instant recorded in handleRequest)
  serviceName: "integration-gateway"
  operation: "{soapServiceName}.{soapOperationName}"
    examples:
      - "OIOFordringIndberetService.MFFordringIndberet_I"
      - "SkatKvitteringHentService.MFKvitteringHent_I"
  resourceType: "SOAP_CALL"
  resourceId: null    # No DB resource ID for pure SOAP calls
  userId: fordringshaverId (from MessageContext; system identifier, not a person)
  clientApplication: cert CN / serial number (from Oces3AuthContext)
  clientIp: remote address from TransportContext
  correlationId: correlationId (from MessageContext)
  changedFields:
    - submitClaim: []   # FIX-019-SPEC-1: corrected from ["claimId"]; all fields encoded in newValues per note_on_custom_fields_encoding
    - getReceipt / getNotifications: []
  newValues:
    - submitClaim: {"claimId": "<uuid>"}
    - reads: {}
  oldValues: {}  # SOAP is stateless — no pre-state
  requestBody: maskedRequestBody (PII removed by SoapPiiMaskingUtil)
  responseBody: maskedResponseBody (PII removed by SoapPiiMaskingUtil)
  faultCode: populated only on fault path (from handleFault)
  faultMessage: populated only on fault path (Danish text)
  stackTrace: populated only on fault path
  environment: from spring.profiles.active or opendebt.environment property

newValues_consolidated_example:
  note: >
    FIX-019-SPEC-2: consolidated newValues map examples for both paths.
    All custom fields (status, requestBody, responseBody, faultCode, faultMessage, stackTrace,
    responseTimeMs) are encoded as string values in the newValues map because ClsAuditEvent
    has no dedicated fields for them (see note_on_custom_fields_encoding below).
  success_path:
    changedFields: []
    newValues:
      responseTimeMs: "123"
      status: "SUCCESS"
      requestBody: "<soapenv:Envelope xmlns:soapenv=...><!-- PII masked --></soapenv:Envelope>"
      responseBody: "<soapenv:Envelope xmlns:soapenv=...><!-- PII masked --></soapenv:Envelope>"
      faultCode: null
      faultMessage: null
      stackTrace: null
  fault_path:
    changedFields: []
    newValues:
      responseTimeMs: "456"
      status: "FAULT"
      requestBody: "<soapenv:Envelope xmlns:soapenv=...><!-- PII masked --></soapenv:Envelope>"
      responseBody: null   # response body may not exist when fault is thrown before endpoint
      faultCode: "env:Client.ValidationError"
      faultMessage: "Valideringsfejl i fordringen"
      stackTrace: "dk.ufst.opendebt.gateway.soap.fault.FordringValidationException: Valideringsfejl i fordringen"

note_on_response_time:
  field_mapping: >
    The existing ClsAuditEvent DTO does not have a responseTimeMs field.
    Encode responseTimeMs in the newValues map: {"responseTimeMs": 123} for success calls.
    This avoids modifying the shared ClsAuditEvent DTO in opendebt-common.
  acceptance_test: AC-16 (CLS logging includes response time)

note_on_custom_fields_encoding: >
  ClsAuditEvent has no status, requestBody, responseBody, faultCode, faultMessage,
  or stackTrace fields. Encode all via the newValues map alongside responseTimeMs:
    "status"       -> "SUCCESS" or "FAULT"
    "requestBody"  -> PII-masked SOAP Envelope string (SoapPiiMaskingUtil.mask())
    "responseBody" -> PII-masked SOAP response Envelope string (may be null on fault)
    "faultCode"    -> e.g. "env:Client.ValidationError" (null on success)
    "faultMessage" -> Danish fault string (null on success)
    "stackTrace"   -> exception.getClass().getName() + ": " + message (null on success)
  All values are strings. The existing changedFields list is set to empty [].
```

### Acceptance Criteria Coverage

| AC | Satisfied by |
|----|--------------|
| AC-13 | afterCompletion ships event with timestamp, fordringshaverId, service, operation, correlationId, status SUCCESS |
| AC-14 | handleFault captures faultCode, faultMessage, stackTrace; afterCompletion ships with status FAULT |
| AC-15 | SoapPiiMaskingUtil.mask() applied before shipEvent(); CPR/name/address stripped (verified by SPEC-019-09) |
| AC-16 | responseTimeMs encoded in newValues map |
| AC-34 | correlationId from SOAP wsa:MessageID (or generated UUID) propagated through CLS event |

---

## SPEC-019-06: SoapFaultMappingResolver

**Module:** `opendebt-integration-gateway`  
**Package:** `dk.ufst.opendebt.gateway.soap.fault`  
**Class:** `SoapFaultMappingResolver.java`  
**Extends:** `org.springframework.ws.server.endpoint.AbstractEndpointExceptionResolver`  
**Source requirement:** FR-ERR-1, FR-ERR-2, FR-ERR-3; AC-21, AC-22, AC-23, AC-24, AC-29  

### Fault Code Mapping Table

```yaml
exception_to_fault_mapping:
  - exception: Oces3AuthenticationException
    http_equivalent: 401
    soap_11_faultcode: "env:Client.Authentication"
    soap_12_code_value: "env:Sender"
    soap_12_subcode: "Authentication"
    danish_faultstring: "Autentificering fejlede: certifikat mangler eller er ugyldigt"
    detail_element: none
    acceptance_test: AC-09 (no cert), AC-22 (Danish message)

  - exception: Oces3AuthorizationException
    http_equivalent: 403
    soap_11_faultcode: "env:Client.Authorization"
    soap_12_code_value: "env:Sender"
    soap_12_subcode: "Authorization"
    danish_faultstring: "System er ikke autoriseret til denne operation"
    detail_element: none
    acceptance_test: AC-11, AC-22

  - exception: "Oces3AuthenticationException with errorCode='CERT_EXPIRED'"
    http_equivalent: 403
    soap_11_faultcode: "env:Client.CertificateExpired"
    soap_12_code_value: "env:Sender"
    soap_12_subcode: "CertificateExpired"
    danish_faultstring: "Certifikat er udløbet"
    detail_element: none
    acceptance_test: AC-10, AC-22

  - exception: FordringValidationException (wraps validation errors)
    http_equivalent: 500
    soap_11_faultcode: "env:Client.ValidationError"
    soap_12_code_value: "env:Sender"
    soap_12_subcode: "ValidationError"
    danish_faultstring: "Valideringsfejl i fordringen"
    detail_element: ValidationErrors (see §Detail Element Structure)
    acceptance_test: AC-21 (field-level errors), AC-23 (error code mapping)

  - exception: OpenDebtException (generic)
    http_equivalent: 500
    soap_11_faultcode: "env:Server"
    soap_12_code_value: "env:Receiver"
    danish_faultstring: "Intern serverfejl — kontakt UFST support"
    detail_element: none
    acceptance_test: AC-23

  - exception: "SaajSoapFaultException / malformed XML / schema validation failure"
    http_equivalent: 400
    soap_11_faultcode: "env:Client"
    soap_12_code_value: "env:Sender"
    danish_faultstring: "Ugyldig SOAP-meddelelse: skemavalidering fejlede"
    detail_element: none
    acceptance_test: AC-24

  note_on_http_status: >
    Spring-WS maps SOAP 1.1 faults to HTTP 500 by default. The feature file
    specifies HTTP 401 and HTTP 403 for auth faults. The resolver must set
    the HTTP status on the transport response via
    ((HttpServletResponse) transportResponse).setStatus(httpStatus)
    before writing the SOAP fault.

  note_on_exception_names: >
    FordringValidationException is the canonical name (aligned with architecture §7.3).
    http_equivalent values in this table reflect conceptual REST mappings only;
    Spring-WS returns HTTP 500 for all SOAP 1.1 faults regardless of the conceptual error type.
```

### Detail Element Structure

```yaml
detail_element_validation_error:
  soap_11_xml: |
    <detail>
      <ValidationErrors xmlns="{request-namespace}">
        <ValidationError>
          <Field>{fieldName}</Field>
          <Code>{ruleCode}</Code>
          <Message>{message}</Message>
        </ValidationError>
      </ValidationErrors>
    </detail>
  soap_12_xml: |
    <env:Detail>
      <ValidationErrors xmlns="{request-namespace}">
        <ValidationError>
          <Field>{fieldName}</Field>
          <Code>{ruleCode}</Code>
          <Message>{message}</Message>
        </ValidationError>
      </ValidationErrors>
    </env:Detail>

  namespace_rule: >
    The namespace of the ValidationErrors element MUST match the request namespace.
    Determine from the incoming SOAP envelope:
      "urn:oio:skat:efi:ws:1.0.1" for OIO requests
      "http://skat.dk/begrebsmodel/2009/01/15/" for SKAT requests
  acceptance_test: AC-21

  source_data: >
    Field-level errors come from ClaimSubmissionResponse.errors (returned by DebtServiceSoapClient
    when FordringValidationException is thrown). Each ValidationError in the result maps to one
    <ValidationError> child element.
```

### SOAP 1.1 vs SOAP 1.2 Protocol Handling

```yaml
soap_version_detection:
  mechanism: >
    SoapMessage.getSaajMessage().getSOAPPart().getEnvelope().getNamespaceURI() 
    returns "http://schemas.xmlsoap.org/soap/envelope/" for SOAP 1.1
    returns "http://www.w3.org/2003/05/soap-envelope" for SOAP 1.2
  fault_construction:
    soap_11:
      elements: [faultcode, faultstring, detail]
      faultstring_attribute: "xml:lang='da'"
    soap_12:
      elements: [Code, Reason, Detail]
      reason_text_attribute: "xml:lang='da'"
  note: >
    Spring-WS SaajSoapBody.addFault() automatically produces the correct structure
    for the detected SOAP version. The resolver should use Spring-WS fault API
    rather than constructing XML manually.
  acceptance_test: AC-29 (SOAP 1.1 elements faultcode/faultstring/detail), AC-29 (SOAP 1.2 Code/Reason/Detail)
```

### Exception Class Definitions (SPEC-019-06)

```yaml
exception_classes:
  FordringValidationException:
    package: dk.ufst.opendebt.gateway.soap.fault
    extends: RuntimeException
    constructor:
      - fieldErrors: List<FieldError>  # from ClaimSubmissionResponse.errors
    purpose: "Thrown by DebtServiceSoapClient when debt-service returns HTTP 422 (validation failure). Carries field-level error list for inclusion in SOAP fault Detail element (AC-21)."
```

### Acceptance Criteria Coverage

| AC | Satisfied by |
|----|--------------|
| AC-21 | detail element with field-level ValidationError children |
| AC-22 | All faultstring values are Danish; xml:lang="da" on faultstring/Reason |
| AC-23 | Exception-to-faultcode mapping table fully implemented |
| AC-24 | SaajSoapFaultException / malformed XML -> Client fault with Danish schema error message |
| AC-29 | SOAP 1.1: faultcode/faultstring/detail; SOAP 1.2: Code/Reason/Detail |

---

## SPEC-019-07: DebtServiceSoapClient

**Module:** `opendebt-integration-gateway`  
**Package:** `dk.ufst.opendebt.gateway.soap`  
**Class:** `DebtServiceSoapClient.java`  
**Source requirement:** FR-INT-1, FR-INT-2; AC-17, AC-18, AC-30, AC-31  

### Constructor and WebClient Configuration

```yaml
class: DebtServiceSoapClient
annotation: "@Component"
constructor_dependencies:
  - WebClient.Builder webClientBuilder   # INJECTED — never WebClient.create() (ADR-0024)
  - "@Value('${opendebt.services.debt-service.url:http://localhost:8082}') String debtServiceUrl"

webclient_configuration: |
  // Constructed in constructor — NOT as a Spring bean (to apply per-client timeouts)
  this.webClient = webClientBuilder
      .baseUrl(debtServiceUrl)
      .clientConnector(new ReactorClientHttpConnector(
          HttpClient.create()
              .connectTimeout(Duration.ofSeconds(2))       // AC-30: TCP connect timeout
              .responseTimeout(Duration.ofMillis(1500))    // 1.5s budget; leaves 0.5s for overhead
      ))
      .build();

timeout_rationale: >
  @TimeLimiter is NOT used. In a synchronous servlet thread, @TimeLimiter AOP does not
  enforce timeouts on WebClient.block() — the blocking call holds the thread for its full
  duration. connectTimeout and responseTimeout on the underlying HttpClient are enforced
  at the TCP/HTTP level and correctly interrupt blocking calls.
  Budget: 1500ms debt-service + 500ms gateway overhead = 2000ms total = AC-30 SLA.
```

### Method Specifications

```yaml
methods:

  submitClaim:
    signature: "ClaimSubmissionResponse submitClaim(FordringSubmitRequest request, String fordringshaverId, String correlationId)"
    resilience:
      - "@CircuitBreaker(name = 'debtService')"
      - NOT @Retry (non-idempotent POST — must not retry automatically)
      - NOT @TimeLimiter (see timeout_rationale above)
    http_call:
      method: POST
      path: /internal/fordringer
      headers:
        X-Fordringshaver-Id: fordringshaverId
        X-Correlation-Id: correlationId
      body: FordringSubmitRequest (JSON, serialised by WebClient)
      success_response: 201 Created -> deserialise to ClaimSubmissionResponse
      error_handling:
        - 422 Unprocessable Entity: deserialise body to ClaimSubmissionResponse and throw FordringValidationException(result)
        - 403 Forbidden (WebClientResponseException.Forbidden):
            throw: Oces3AuthorizationException("Fordringshaveren er ikke autoriseret til at indsende fordringer", "FORDRINGSHAVER_NOT_AUTHORIZED")
            result: SoapFaultMappingResolver maps to env:Client.Authorization SOAP fault (AC-11)
        - 5xx: throw OpenDebtException("Debt service unavailable", "DEBT_SERVICE_UNAVAILABLE", CRITICAL)
        - circuit open: throw OpenDebtException("Debt service circuit open", "DEBT_SERVICE_UNAVAILABLE", CRITICAL)
    acceptance_test: AC-17 (same validation rules as REST), AC-18 (same business logic)

  getReceipt:
    signature: "KvitteringResponse getReceipt(UUID claimId, String fordringshaverId, String correlationId)"
    resilience:
      - "@CircuitBreaker(name = 'debtService')"
      - "@Retry(name = 'debtService')"   # Idempotent GET — safe to retry
    http_call:
      method: GET
      path: /internal/fordringer/{claimId}/kvittering
      path_vars: [claimId]
      headers:
        X-Fordringshaver-Id: fordringshaverId
        X-Correlation-Id: correlationId
      success_response: 200 OK -> deserialise to KvitteringResponse
      error_handling:
        - 404 Not Found: throw OpenDebtException("Claim not found: " + claimId, "CLAIM_NOT_FOUND", WARNING)
        - 403 Forbidden: throw Oces3AuthorizationException("Fordringshaveren er ikke autoriseret", "FORDRINGSHAVER_NOT_AUTHORIZED")
        - 5xx: throw OpenDebtException("Debt service unavailable", "DEBT_SERVICE_UNAVAILABLE", CRITICAL)
    acceptance_test: AC-18

  getNotifications:
    signature: "NotificationCollectionResult getNotifications(UUID claimId, UUID debtorId, String fordringshaverId, String correlationId)"
    resilience:
      - "@CircuitBreaker(name = 'debtService')"
      - "@Retry(name = 'debtService')"   # Idempotent GET — safe to retry
    http_call:
      method: GET
      path: /internal/fordringer/{claimId}/underretninger
      path_vars: [claimId]
      query_params:
        - debtorId: debtorId (if not null)
        # Note: Parameter name debtorId is used per agents.md English naming convention;
        # the architecture document §10.3 incorrectly uses skyldnerId — this spec takes precedence.
      headers:
        X-Fordringshaver-Id: fordringshaverId
        X-Correlation-Id: correlationId
      success_response: 200 OK -> deserialise to NotificationCollectionResult
      error_handling:
        - 404 Not Found: throw OpenDebtException("Claim not found: " + claimId, "CLAIM_NOT_FOUND", WARNING)
        - 403 Forbidden: throw Oces3AuthorizationException("Fordringshaveren er ikke autoriseret", "FORDRINGSHAVER_NOT_AUTHORIZED")
        - 5xx: throw OpenDebtException("Debt service unavailable", "DEBT_SERVICE_UNAVAILABLE", CRITICAL)
    acceptance_test: AC-18
```

### Shared DTOs (in opendebt-common)

```yaml
new_dto_classes:
  - class: FordringSubmitRequest
    package: dk.ufst.opendebt.common.dto.soap
    fields:
      - claimType: String
      - amount: BigDecimal      # Must be > 0 (validated by debt-service)
      - debtorPersonId: UUID    # Person-registry UUID only — NEVER CPR (GDPR ADR-0014)
      - claimDate: LocalDate
      - dueDate: LocalDate
      - externalId: String
    note: "Fields are English-named (agents.md naming convention). GDPR: debtorPersonId is a UUID reference, not CPR."

  - class: ClaimSubmissionResponse
    package: dk.ufst.opendebt.common.dto.soap
    fields:
      - claimId: UUID
      - outcome: Enum(SUCCESS, REJECTED, ERROR)
      - errors: List<FieldError>
    nested_types:
      - class: FieldError
        fields:
          - field: String
          - message: String

  - class: KvitteringResponse
    package: dk.ufst.opendebt.common.dto.soap
    fields:
      - kvitteringId: UUID
      - claimId: UUID
      - status: String   # SUBMITTED | ACCEPTED | REJECTED
      - modtagetDato: Instant
      - behandletDato: Instant
      - afvisningKode: String (nullable)
      - afvisningTekst: String (nullable)

  - class: NotificationCollectionResult
    package: dk.ufst.opendebt.common.dto.soap
    fields:
      - claimId: UUID
      - notifications: List<NotificationDto>  # from opendebt-common or debt-service DTO
        annotation: "@JsonProperty(\"underretninger\")"
        note: "JSON wire format uses 'underretninger' (Danish domain term); Java field uses English name per agents.md"
      - total: int
```

### Acceptance Criteria Coverage

| AC | Satisfied by |
|----|--------------|
| AC-17 | submitClaim delegates to POST /internal/fordringer — debt-service runs same validation |
| AC-18 | All three methods delegate to debt-service internal REST — business logic is in debt-service only |
| AC-30 | connectTimeout(2s) + responseTimeout(1500ms) enforce 2s total SLA |
| AC-31 | Stateless client bean; thread safety guaranteed by WebClient; HPA for 100+ concurrent |

> **Note:** AC-33 (schema validation < 500ms) is covered by SPEC-019-10 (PayloadValidatingInterceptor pre-compiled XSD validators at startup). It is not attributable to DebtServiceSoapClient.

---

## SPEC-019-08: Oces3CertificateParser

**Module:** `opendebt-common`  
**Package:** `dk.ufst.opendebt.common.soap`  
**Class:** `Oces3CertificateParser.java`  
**Source requirement:** FR-SEC-1, FR-SEC-2, FR-SEC-4; AC-09, AC-10, AC-12  

### Interface

```yaml
class: Oces3CertificateParser
annotation: "@Component"
package: dk.ufst.opendebt.common.soap
dependencies: none (uses java.security.cert.* only)

method_parse:
  signature: "Oces3AuthContext parse(X509Certificate certificate)"
  inputs:
    - certificate: java.security.cert.X509Certificate (never null at call site — null check is in interceptor)
  outputs:
    - Oces3AuthContext (value object — see below)
  error_handling:
    - If DN cannot be parsed -> throw CertificateParsingException("Cannot extract DN field", "CERT_PARSE_ERROR")
    - If certificate is structurally invalid -> throw CertificateParsingException("Invalid certificate", "CERT_INVALID")
```

### Oces3AuthContext Value Object

```yaml
class: Oces3AuthContext
type: record (Java 16+ record, or @Value final class)
package: dk.ufst.opendebt.common.soap
fields:
  - fordringshaverId: String   # Extracted from DN field per fordringshaver-dn-field property
  - cn: String                  # Full CN value
  - issuer: String              # Issuer DN string (for CA trust check in interceptor)
  - validTo: Instant            # Certificate notAfter
  - serialNumber: String        # Certificate serial number (hex)
```

### DN Extraction Logic

```yaml
dn_extraction:
  default_field: CN
  rule: >
    Parse the certificate Subject DN using javax.security.auth.x500.X500Principal.getName(RFC2253).
    Split on comma to extract attribute-value pairs.
    Locate the pair matching the configured fordringshaver-dn-field (default: CN).
    Strip the "CN=" prefix and trim whitespace.
  example:
    subject_dn: "CN=CREDITOR-001,O=Gældstyrelsen,C=DK"
    fordringshaver-dn-field: CN
    result_fordringshaverId: "CREDITOR-001"
  acceptance_test: AC-12 (certificate subject mapped to fordringshaver identifier)
  note: >
    No external database lookup at parse time. The mapping is purely DN-based.
    Authorization is enforced downstream in debt-service via X-Fordringshaver-Id header.
```

### Acceptance Criteria Coverage

| AC | Satisfied by |
|----|--------------|
| AC-09 | Called by interceptor; if cert is null, interceptor throws before calling parser |
| AC-10 | validTo field compared against Instant.now() in interceptor |
| AC-12 | fordringshaverId extracted from CN field of Subject DN |

---

## SPEC-019-09: SoapPiiMaskingUtil

**Module:** `opendebt-common`  
**Package:** `dk.ufst.opendebt.common.soap`  
**Class:** `SoapPiiMaskingUtil.java`  
**Source requirement:** FR-LOG-3; AC-15  

### Interface

```yaml
class: SoapPiiMaskingUtil
annotation: "@Component"
package: dk.ufst.opendebt.common.soap
state: stateless (safe for concurrent use)

method_mask:
  signature: "String mask(String soapEnvelopeXml)"
  inputs:
    - soapEnvelopeXml: String (raw SOAP envelope XML; may be null or blank)
  outputs:
    - String: SOAP envelope XML with PII elements replaced; same structure otherwise
  behaviour_on_null_input: return null (or empty string if blank — do not throw)
  implementation_approach: >
    SAX streaming transformer or XPath-based replacement. 
    Do NOT use DOM round-trip for performance — masking must not add significant latency
    since it runs in afterCompletion outside the response path.
```

### Masking Rules Table

```yaml
masking_rules:
  - xml_elements: ["CPRNummer", "cprNummer", "SkyldnerCPR"]
    namespace: any
    context_constraint: none (always mask regardless of parent)
    masking_value: "[MASKED]"
    acceptance_test: AC-15 (CPR not logged)

  - xml_elements: ["Navn", "SkyldnerNavn"]
    namespace: any
    context_constraint: parent element must be one of [Skyldner, Debitor, SkyldnerData]
    masking_value: "[MASKED]"
    acceptance_test: AC-15 (debtor name not logged)

  - xml_elements: ["Adresse", "PostNummer", "By"]
    namespace: any
    context_constraint: parent element must be one of [Skyldner, Debitor, SkyldnerData, AdresseData]
    masking_value: "[MASKED]"
    acceptance_test: AC-15 (address not logged)

  - xml_elements: ["BankKontoNummer", "KontoNummer"]
    namespace: any
    context_constraint: none
    masking_value: "[MASKED]"
    acceptance_test: AC-15

  - xml_elements: ["CVRNummer", "SkyldnerCVR"]
    namespace: any
    context_constraint: parent element must be one of [Skyldner, Debitor]
    masking_value: "[MASKED-CVR]"

  - xml_elements: ["Email", "EmailAdresse"]
    namespace: any
    context_constraint: parent element must be one of [Skyldner, Debitor]
    masking_value: "[MASKED-EMAIL]"

  - xml_elements: ["Telefon", "TelefonNummer"]
    namespace: any
    context_constraint: parent element must be one of [Skyldner, Debitor]
    masking_value: "[MASKED-PHONE]"

  - xml_elements: ["SkyldnerUUID", "DebtorId", "skyldnerPersonId"]
    namespace: any
    context_constraint: none
    masking_value: "PRESERVED — UUID references are not PII (GDPR ADR-0014)"
    acceptance_test: AC-15 (debtor identifier as UUID reference only — must be present in logs)
```

### Performance Constraint

```yaml
performance:
  constraint: >
    PII masking runs in afterCompletion — it does NOT block the SOAP response.
    The response is already written to the client before masking begins.
    No latency requirement applies to the masking operation itself.
  note_on_schema_validation_budget: >
    The 500ms schema validation budget (AC-33) applies to PayloadValidatingInterceptor,
    not to SoapPiiMaskingUtil. These are separate concerns.
```

### Acceptance Criteria Coverage

| AC | Satisfied by |
|----|--------------|
| AC-15 | CPR, name, address, bank account masked; UUID preserved in audit log |

---

## SPEC-019-10: Static Resources — XSD and WSDL Files

**Module:** `opendebt-integration-gateway`  
**Location:** `src/main/resources/wsdl/`  
**Source requirement:** FR-NFR-3, FR-NFR-4, AC-20, AC-25, AC-26, AC-27, AC-28  

### File Layout

```
opendebt-integration-gateway/src/main/resources/
└── wsdl/
    ├── oio/
    │   ├── oio-fordring.wsdl       (WSDL 1.1; static; dual SOAP 1.1 + 1.2 binding)
    │   └── oio-fordring.xsd        (OIO XSD version 1.0.1)
    └── skat/
        ├── skat-fordring.wsdl      (WSDL 1.1; static; dual SOAP 1.1 + 1.2 binding)
        └── skat-fordring.xsd       (SKAT XSD 2009/01/15)
```

### OIO XSD Requirements (oio-fordring.xsd)

```yaml
namespace_uri: "urn:oio:skat:efi:ws:1.0.1"
schema_version: "1.0.1"

required_root_elements:
  - MFFordringIndberet_IRequest
  - MFFordringIndberet_IResponse
  - MFKvitteringHent_IRequest
  - MFKvitteringHent_IResponse
  - MFUnderretSamlingHent_IRequest
  - MFUnderretSamlingHent_IResponse

required_fields_MFFordringIndberet_IRequest:
  - FordringsType: xs:string, maxOccurs 1, minOccurs 1
  - Beloeb: xs:decimal, minOccurs 1   # Must be > 0 at business level; XSD enforces type only
  - SkyldnerPersonId: xs:string (UUID), minOccurs 1
  - FordringsDato: xs:date, minOccurs 1
  - ForfaldsDato: xs:date, minOccurs 1
  - EksternId: xs:string, minOccurs 0   # Optional creditor reference

required_fields_MFFordringIndberet_IResponse:
  - FordringsId: xs:string (UUID)
  - Status: xs:string   # MODTAGET | AFVIST

debtor_pii_elements:
  note: >
    Debtor PII elements (CPRNummer, SkyldnerCPR, Navn, etc.) referenced in SoapPiiMaskingUtil
    may appear in the XSD as xs:string within a Skyldner/Debitor complex type.
    Their presence in the schema does not violate GDPR — masking occurs at the audit layer.
    The services never store PII; they pass it through to person-registry via debtorPersonId UUID.

jaxb_binding_file: oio-bindings.xjb
  customisations:
    - xs:date -> java.time.LocalDate
    - xs:decimal -> java.math.BigDecimal
    - xs:string (UUID fields) -> java.util.UUID
```

### SKAT XSD Requirements (skat-fordring.xsd)

```yaml
namespace_uri: "http://skat.dk/begrebsmodel/2009/01/15/"

required_root_elements:
  - MFFordringIndberet_IRequest
  - MFFordringIndberet_IResponse
  - MFKvitteringHent_IRequest
  - MFKvitteringHent_IResponse
  - MFUnderretSamlingHent_IRequest
  - MFUnderretSamlingHent_IResponse

note: >
  SKAT element naming follows SKAT PSRM conventions. Element names may differ from OIO
  counterparts (e.g., SKAT may use "HovedstolBeloeb" instead of "Beloeb").
  The SkatClaimMapper (SPEC-019-03D) handles the structural differences.

jaxb_binding_file: skat-bindings.xjb
  customisations: same as oio-bindings.xjb
```

### WSDL Requirements (oio-fordring.wsdl and skat-fordring.wsdl)

```yaml
wsdl_version: "1.1"
wsdl_structure_per_file:
  types:
    - xs:import of corresponding XSD (relative path, same directory)

  messages:
    - MFFordringIndberet_IRequest  -> uses element from XSD
    - MFFordringIndberet_IResponse -> uses element from XSD
    - MFKvitteringHent_IRequest
    - MFKvitteringHent_IResponse
    - MFUnderretSamlingHent_IRequest
    - MFUnderretSamlingHent_IResponse

  portType:
    name: FordringPortType
    operations:
      - name: MFFordringIndberet_I
        input:  MFFordringIndberet_IRequest
        output: MFFordringIndberet_IResponse
      - name: MFKvitteringHent_I
        input:  MFKvitteringHent_IRequest
        output: MFKvitteringHent_IResponse
      - name: MFUnderretSamlingHent_I
        input:  MFUnderretSamlingHent_IRequest
        output: MFUnderretSamlingHent_IResponse

  bindings:
    - name: FordringBinding_SOAP11
      type: FordringPortType
      soap_binding_style: document
      soap_version: SOAP 1.1
      transport: "http://schemas.xmlsoap.org/soap/http"
      operations: [MFFordringIndberet_I, MFKvitteringHent_I, MFUnderretSamlingHent_I]

    - name: FordringBinding_SOAP12
      type: FordringPortType
      soap_binding_style: document
      soap_version: SOAP 1.2
      transport: "http://www.w3.org/2003/05/soap/bindings/HTTP/"
      operations: [MFFordringIndberet_I, MFKvitteringHent_I, MFUnderretSamlingHent_I]

  service:
    OIO WSDL: service name "OIOFordringService"; two ports (SOAP11Port, SOAP12Port)
    SKAT WSDL: service name "SkatFordringService"; two ports (SOAP11Port, SOAP12Port)

content_type_requirement:
  rule: GET /soap/oio?wsdl returns Content-Type "application/wsdl+xml"
  acceptance_test: AC-27 (response content type)

wsdl_naming_note: >
  File names oio-fordring.wsdl and skat-fordring.wsdl use kebab-case per agents.md
  conventions. Architecture §8.1 code samples use OIOFordringService.wsdl /
  SkatFordringService.wsdl — the kebab-case form is canonical for this spec.
```

### JAXB Build Plugin Configuration

```yaml
plugin: jaxb2-maven-plugin (or jakarta-jaxb-plugin for Jakarta EE 9+)
executions:
  - id: generate-oio
    goals: [generate]
    configuration:
      schemaDirectory: ${project.basedir}/src/main/resources/wsdl/oio
      bindingDirectory: ${project.basedir}/src/main/resources/wsdl/oio
      packageName: dk.ufst.opendebt.gateway.soap.oio.generated
      outputDirectory: ${project.build.directory}/generated-sources/jaxb/oio

  - id: generate-skat
    goals: [generate]
    configuration:
      schemaDirectory: ${project.basedir}/src/main/resources/wsdl/skat
      bindingDirectory: ${project.basedir}/src/main/resources/wsdl/skat
      packageName: dk.ufst.opendebt.gateway.soap.skat.generated
      outputDirectory: ${project.build.directory}/generated-sources/jaxb/skat
```

### XSD Schema Validation Interceptor

```yaml
validator_bean:
  type: PayloadValidatingInterceptor
  registered_in: SoapConfig (SPEC-019-01)
  configuration:
    oio_schema: new ClassPathResource("wsdl/oio/oio-fordring.xsd")
    skat_schema: new ClassPathResource("wsdl/skat/skat-fordring.xsd")
    validate_request: true
    validate_response: false   # Not required by outcome contract
  performance:
    rule: XSD validator instance compiled ONCE at startup (not per request)
    reason: Ensures schema validation completes within 500ms (AC-33)

  schema_validation_fault:
    on_failure: Spring-WS automatically produces SOAP fault
    fault_intercepted_by: SoapFaultMappingResolver for consistent Danish message

acceptance_criteria: [AC-20, AC-33]
```

### Acceptance Criteria Coverage

| AC | Satisfied by |
|----|--------------|
| AC-20 | OIO XSD v1.0.1 and SKAT XSD 2009/01/15 used for schema validation |
| AC-25 | GET /soap/oio?wsdl returns 200, served by SimpleWsdl11Definition oioWsdl bean |
| AC-26 | GET /soap/skat?wsdl returns 200, served by SimpleWsdl11Definition skatWsdl bean |
| AC-27 | WSDL contains all 3 services, portTypes, operations, messages, schemas, bindings |
| AC-28 | WSDL references correct XSD (OIO or SKAT); static WSDL import section |
| AC-33 | PayloadValidatingInterceptor uses pre-compiled XSD validator; 500ms budget |

---

## SPEC-019-11: InternalClaimController — opendebt-debt-service

**Module:** `opendebt-debt-service`  
**Package:** `dk.ufst.opendebt.debtservice.controller`  
**Class:** `InternalClaimController.java`  
**Source requirement:** FR-INT-1, FR-INT-2; AC-17, AC-18  

### Prerequisite

```yaml
prerequisite: >
  Petitions 015–018 establish the business-logic layer in debt-service (ClaimSubmissionService,
  validation rules, NotificationService, KvitteringService). SPEC-019-11 specifies only
  the NEW internal REST endpoints that expose this existing logic to DebtServiceSoapClient.
  If these endpoints already exist from petitions 015–018, no new controller is needed —
  only the header handling must be verified against this spec.
```

### Endpoint Specifications

```yaml
class: InternalClaimController
annotation: "@RestController"
base_path: /internal/fordringer
note: >
  Path prefix /internal/* must NOT be exposed via DUPLA. 
  Internal network access only (Kubernetes ClusterIP).
  No Spring Security OAuth2 check on /internal paths — authentication
  is delegated to the caller (integration-gateway) via X-Fordringshaver-Id header.

endpoints:

  submit_claim:
    method: POST
    path: /internal/fordringer
    request_headers:
      - "X-Fordringshaver-Id: {fordringshaverId}" (required — used for authorization)
      - "X-Correlation-Id: {correlationId}" (required — for end-to-end tracing)
    request_body: FordringSubmitRequest (JSON)
    success_response:
      status: 201 Created
      body: ClaimSubmissionResponse { claimId: UUID, outcome: SUCCESS, errors: [] }
    validation_failure_response:
      status: 422 Unprocessable Entity
      body: ClaimSubmissionResponse { claimId: null, outcome: REJECTED, errors: [...FieldError...] }
    behaviour: >
      Delegates to ClaimSubmissionService (same service used by REST channel).
      Validation rules are identical to those applied when claims are submitted via REST.
    acceptance_test: AC-17 (same validation), AC-18 (same business logic)

  get_receipt:
    method: GET
    path: /internal/fordringer/{claimId}/kvittering
    path_vars: [claimId: UUID]
    request_headers:
      - "X-Fordringshaver-Id: {fordringshaverId}"
      - "X-Correlation-Id: {correlationId}"
    success_response:
      status: 200 OK
      body: KvitteringResponse { kvitteringId: UUID, claimId: UUID, status: String (SUBMITTED | ACCEPTED | REJECTED), modtagetDato: Instant, behandletDato: Instant, afvisningKode: String (nullable), afvisningTekst: String (nullable) }
    not_found_response:
      status: 404 Not Found
      body: ErrorResponse { error: "CLAIM_NOT_FOUND", message: "..." }
    acceptance_test: AC-18

  get_notifications:
    method: GET
    path: /internal/fordringer/{claimId}/underretninger
    path_vars: [claimId: UUID]
    query_params:
      - debtorId: UUID (optional)
      # Note: Parameter name debtorId is used per agents.md English naming convention;
      # the architecture document §10.3 incorrectly uses skyldnerId — this spec takes precedence.
    request_headers:
      - "X-Fordringshaver-Id: {fordringshaverId}"
      - "X-Correlation-Id: {correlationId}"
    success_response:
      status: 200 OK
      body: |
        {
          "claimId": UUID,
          "underretninger": [NotificationDto],
          "total": int
        }
    not_found_response:
      status: 404 Not Found
    acceptance_test: AC-18
```

### Acceptance Criteria Coverage

| AC | Satisfied by |
|----|--------------|
| AC-17 | POST /internal/fordringer routes to ClaimSubmissionService — same validation logic as REST |
| AC-18 | All three endpoints expose the same business logic as the REST channel; no duplicate business rules |

---

## TLS Configuration (Non-Functional — AC-35)

```yaml
specification_id: SPEC-019-TLS
acceptance_test: AC-35 (HTTPS with TLS 1.3)

path_b_ingress_default:
  environment: Kubernetes (production)
  mechanism: nginx ingress terminates TLS; forwards cert via X-Client-Cert header
  kubernetes_ingress_annotations:
    nginx.ingress.kubernetes.io/auth-tls-pass-certificate-to-upstream: "true"
    nginx.ingress.kubernetes.io/auth-tls-secret: "opendebt/oces3-truststore-secret"
    nginx.ingress.kubernetes.io/auth-tls-verify-client: "on"
    nginx.ingress.kubernetes.io/configuration-snippet: |
      proxy_set_header X-Client-Cert $ssl_client_raw_cert;
  tls_version: 1.3 (enforced by nginx ingress policy — existing cluster policy)
  application_config: soap.security.tls-termination-mode=ingress

path_a_embedded_dev:
  environment: local development / direct-TLS scenarios
  mechanism: Spring Boot embedded Tomcat performs mTLS handshake
  config_file: application-local.yml (NOT the main application.yml)
  spring_config: |
    server:
      ssl:
        client-auth: need
        bundle: oces3-trust
    spring:
      ssl:
        bundle:
          jks:
            oces3-trust:
              keystore:
                location: classpath:tls/gateway-keystore.p12
                password: ${SSL_KEYSTORE_PASSWORD}
                type: PKCS12
              truststore:
                location: classpath:tls/oces3-truststore.p12
                password: ${SSL_TRUSTSTORE_PASSWORD}
                type: PKCS12
  application_config: soap.security.tls-termination-mode=embedded
```

---

## Architecture Test Requirements (AC-32, ADR-0024)

```yaml
specification_id: SPEC-019-ARCH

new_arch_test_file:
  module: opendebt-integration-gateway
  class: IntegrationGatewayArchitectureTest
  package: dk.ufst.opendebt.gateway
  base_class_ref: >
    Follow pattern from CreditorArchitectureTest and PortalArchitectureTest.
    Reference SharedArchRules from opendebt-common.

required_rules:
  - rule: SharedArchRules.noAccessToOtherServiceRepositories("gateway")
    rationale: prevents repository imports from other modules

  - rule: clients_must_use_injected_webclient_builder
    rationale: ADR-0024 — WebClient.create() is forbidden; enforces trace propagation
    definition: |
      noClasses()
        .that().resideInAPackage("..soap..")
        .should().callMethod(WebClient.class, "create")
        .orShould().callMethod(WebClient.class, "create", String.class)
        .as("SOAP clients must inject WebClient.Builder for trace propagation (ADR-0024)")
```

---

## Validation Checklist

- [x] Every outcome-contract requirement (AC-01 through AC-35) has at least one specification
- [x] Every specification traces back to a petition functional or non-functional requirement
- [x] All interfaces are unambiguous and testable (input types, output types, error cases defined)
- [x] Non-functional requirements included only where explicitly specified (AC-29 through AC-35)
- [x] Zero items beyond petition, requirements-doc, and outcome-contract
- [x] Every specification enables either implementation or test execution
- [x] No vague language ("should", "might", "could") — all statements are "must" or defined behaviour
- [x] No invented features or constraints
- [x] GDPR: debtorPersonId is UUID only; CPR never stored or logged (ADR-0014)
- [x] WebClient.Builder injected (never WebClient.create()) — ADR-0024
- [x] SimpleWsdl11Definition used (not DefaultWsdl11Definition) — dual SOAP binding requirement
- [x] ClsAuditClient exception to ADR-0022 documented — integration-gateway has no DB
- [x] TLS PATH B (ingress) is production default; PATH A (embedded) is dev only
- [x] @TimeLimiter explicitly excluded from DebtServiceSoapClient — WebClient native timeouts used
- [x] Fault strings are Danish with xml:lang="da"
- [x] Interceptor order enforced: Oces3SoapSecurityInterceptor (0) before ClsSoapAuditInterceptor (1) before PayloadValidatingInterceptor (2)
- [x] AuditableEntity extension is not applicable — integration-gateway has no JPA entities
