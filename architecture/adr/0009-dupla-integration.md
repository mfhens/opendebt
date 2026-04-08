# ADR 0009: DUPLA Integration for External APIs

## Status
Accepted

## Context
UFST DUPLA (Dataudvekslingsplatformen) is the standard API gateway for exposing and consuming APIs in the Danish tax administration ecosystem. It provides:

- Centralized API management
- Authentication (OCES3 certificates, OAuth2)
- Authorization via agreement module
- Audit logging to CLS (Central Logging Service)
- Rate limiting and throttling
- API documentation and discovery

All external integrations must go through DUPLA.

## Decision
We integrate with DUPLA for all external API exposure and consumption:

### Exposed APIs via DUPLA
| API | Consumers | Purpose |
|-----|-----------|---------|
| Debt Registration API | 1,200 creditor systems | Submit debts |
| Debt Status API | Creditors | Query debt status |
| Payment API | Payment providers | Record payments |
| Case Status API | External systems | Query case status |

### Consumed APIs via DUPLA
| API | Provider | Purpose |
|-----|----------|---------|
| CPR Register | CPR Office | Validate citizen identity |
| CVR Register | Erhvervsstyrelsen | Validate business identity |
| Income Data | eIndkomst | Wage garnishment calculations |
| Address Data | DAWA | Letter delivery addresses |

### Integration Architecture
```
┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│  External System │────▶│      DUPLA       │────▶│  Integration     │
│  (Creditor)      │     │   (API Gateway)  │     │  Gateway Service │
└──────────────────┘     └──────────────────┘     └──────────────────┘
                                │
                                ▼
                         ┌──────────────────┐
                         │ CLS (Logging)    │
                         └──────────────────┘
```

### Authentication Flow
1. **System-to-system**: OCES3 certificate → DUPLA validates → Access token
2. **User context**: OAuth2 token from Keycloak → DUPLA forwards

### Agreement Module
- Each data exchange requires a formal agreement
- Agreements specify: data fields, purpose, legal basis, certificates
- Agreements are versioned and auditable

### Integration Gateway Service
The `opendebt-integration-gateway` service handles:
- Routing to internal services
- Protocol translation if needed
- Error mapping to DUPLA standards
- Additional logging/metrics

## Consequences

### Positive
- Standardized API management
- Built-in audit logging
- Security handled at gateway level
- API discovery for consumers
- Compliance with UFST standards

### Negative
- Additional latency through gateway
- Dependency on DUPLA availability
- Agreement bureaucracy for new integrations
- Learning curve for DUPLA specifics

### Mitigations
- Caching where appropriate
- Circuit breakers for resilience
- Early engagement with DUPLA team
- Documentation of integration patterns
