# ADR 0030: SOAP Legacy Gateway (OIO/SKAT Protocols)

## Status
Accepted

## Context

External creditor systems (EFI/DMI and SKAT) communicate using legacy SOAP-based protocols that predate the OpenDebt REST architecture. Two distinct namespaces are in use:

- **OIO protocol** (`urn:oio:skat:efi:ws:1.0.1`): Used by ~1,200 creditor systems that were integrated via EFI/DMI for claim submission (`FordringIndberet`), receipt retrieval (`KvitteringHent`), and notification collection retrieval (`UnderretSamlingHent`).
- **SKAT protocol** (`http://skat.dk/begrebsmodel/2009/01/15/`): Used by SKAT systems for the same three operations.

These systems cannot be migrated to REST in the near term. OpenDebt must accept their SOAP traffic while keeping the internal architecture REST-first (ADR-0004).

Both protocols require OCES3 X.509 mutual TLS client certificate authentication (the Danish public-sector standard for system-to-system trust) rather than OAuth2 bearer tokens.

Additional constraints:
- Both SOAP 1.1 (`text/xml`) and SOAP 1.2 (`application/soap+xml`) must be supported for each protocol.
- Malformed SOAP messages must return a SOAP fault response, not the default Spring Boot JSON error.
- Every SOAP call must be audit-logged to CLS with PII masking, consistent with ADR-0022.
- Downstream calls to `debt-service` must be protected by a circuit breaker (ADR-0026).

## Decision

Extend `opendebt-integration-gateway` with a dedicated Spring-WS `MessageDispatcherServlet` mounted at `/soap/*`. This servlet is entirely separate from the Spring MVC REST dispatcher at `/api`.

### Servlet separation

```
/api/*   → Spring MVC DispatcherServlet  (REST, OAuth2)
/soap/*  → Spring-WS MessageDispatcherServlet  (SOAP, OCES3 mTLS)
```

### Dual SOAP 1.1/1.2 support

A ThreadLocal in `SaajSoapMessageFactory` detects the incoming `Content-Type` and selects either the SOAP 1.1 or SOAP 1.2 message factory accordingly. The response protocol always matches the request protocol.

### Static WSDL files

`DefaultWsdl11Definition` (Spring-WS auto-generation) can only produce SOAP 1.1 bindings. To expose dual-binding WSDLs, static WSDL files are placed on the classpath and served via `SimpleWsdl11Definition`:

| Endpoint | WSDL resource |
|----------|---------------|
| `/soap/oio?wsdl` | `wsdl/oio/oio-fordring.wsdl` |
| `/soap/skat?wsdl` | `wsdl/skat/skat-fordring.wsdl` |

### Authentication

`Oces3SoapSecurityInterceptor` validates the OCES3 X.509 client certificate on every SOAP request before any endpoint processing occurs. It supports three operational modes:

| Mode | Description |
|------|-------------|
| `TEST` | Accept any cert; extract `fordringshaverId` from subject CN |
| `INGRESS` | Validate cert chain against trust store; extract `fordringshaverId` |
| `EMBEDDED` | Full mTLS — cert delivered by TLS terminator via header |

### Interceptor chain

1. `Oces3SoapSecurityInterceptor` — cert validation + `fordringshaverId` extraction
2. `ClsSoapAuditInterceptor` — CLS audit logging with XPath PII masking
3. Endpoint (OIO or SKAT)

### SOAP fault mapping

`SoapFaultMappingResolver` maps domain exceptions to SOAP faults with correct HTTP status codes:

| Exception | HTTP | SOAP 1.1 `faultcode` | SOAP 1.2 `Code/Value` |
|-----------|------|----------------------|-----------------------|
| Malformed XML / SAAJ parse error | 400 | `env:Client` | `env:Sender` |
| `FordringValidationException` | 422 | `env:Client` | `env:Sender` |
| `Oces3AuthenticationException` | 401 | `env:Client` | `env:Sender` |
| `Oces3AuthorizationException` | 403 | `env:Client` | `env:Sender` |
| `ServiceException` (generic) | 500 | `env:Server` | `env:Receiver` |

`SoapHttpStatusFilter` preserves the custom HTTP status code after Spring-WS resets it to 500. `SoapParseErrorFilter` catches SAAJ parse failures at the filter level so they return a proper SOAP fault instead of a JSON error page.

### Internal delegation

`DebtServiceSoapClient` (WebClient, `@CircuitBreaker("debtService")`) translates SOAP payloads into internal REST calls to `debt-service`, consistent with the orchestration pattern (ADR-0019) and resilience requirements (ADR-0026).

### Package layout

```
dk.ufst.opendebt.integrationgateway.soap/
├── config/          # SoapConfig, SoapMessageReceiverHandlerAdapter
├── fault/           # SoapFaultMappingResolver, domain SOAP exceptions
├── filter/          # SoapHttpStatusFilter, SoapParseErrorFilter, WsdlServingFilter
├── interceptor/     # Oces3SoapSecurityInterceptor, ClsSoapAuditInterceptor
├── oio/             # OIO endpoints, OioClaimMapper, oio/generated/ (JAXB)
├── skat/            # SKAT endpoints, SkatClaimMapper, skat/generated/ (JAXB)
└── DebtServiceSoapClient.java
```

## Consequences

### Positive
- Legacy creditor systems (OIO/SKAT) can submit claims without protocol migration.
- SOAP traffic is fully isolated from REST traffic via separate servlet mapping.
- OCES3 certificate authentication is consistent with the existing DUPLA integration pattern (ADR-0009).
- CLS audit logging and circuit breakers apply uniformly to both REST and SOAP ingress paths.
- Static WSDLs allow dual SOAP 1.1/1.2 bindings without runtime generation.

### Negative
- Spring-WS adds a second web framework alongside Spring MVC; developers must understand both dispatch models.
- JAXB code generation for OIO and SKAT XSDs must be maintained when schemas change.
- ThreadLocal SOAP version detection in `SaajSoapMessageFactory` requires care under virtual-thread (Project Loom) migration.

### Mitigations
- All SOAP configuration is isolated in the `soap/` sub-package; REST code is unaffected.
- JAXB generation is automated via the `maven-jaxb2-plugin` in the `integration-gateway` POM.
- The ThreadLocal pattern is documented; migration to a scope-neutral approach can be addressed if Loom adoption requires it.
