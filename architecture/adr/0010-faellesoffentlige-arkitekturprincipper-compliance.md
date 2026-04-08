# ADR 0010: Compliance with Fællesoffentlige Arkitekturprincipper

## Status
Accepted

## Context
OpenDebt must comply with the Fællesoffentlige Arkitekturprincipper (Danish Common Public Sector Architecture Principles) as mandated by Digitaliseringsstyrelsen. These principles guide digital architecture across the Danish public sector.

Reference: https://arkitektur.digst.dk/

## Decision
We align OpenDebt architecture with each principle:

### Principle 1: Architecture is Based on Business Needs
**Implementation:**
- Domain-driven design based on debt collection processes
- Services aligned with business capabilities (case management, payment, etc.)
- Business stakeholder involvement in API design

### Principle 2: Architecture is Flexible and Scalable
**Implementation:**
- Microservices allow independent scaling
- Kubernetes horizontal pod autoscaling
- API versioning for evolution
- Feature flags for gradual rollout

### Principle 3: Architecture is Coherent
**Implementation:**
- Consistent API design patterns
- Shared common library (`opendebt-common`)
- Standard error handling
- Unified logging and monitoring

### Principle 4: Data is Reused and Shared
**Implementation:**
- APIs expose data for reuse via DUPLA
- No data silos between services
- Standard data formats (JSON)
- Reference data management

### Principle 5: Security is Integrated
**Implementation:**
- OAuth2/OIDC authentication
- Role-based access control
- Encryption in transit (TLS)
- OWASP security scanning
- Audit logging to CLS

### Principle 6: Privacy is Built-In
**Implementation:**
- GDPR compliance by design
- Data minimization in APIs
- Purpose limitation enforcement
- Consent management where applicable
- Right to access/deletion support

### Principle 7: Solutions Support Interoperability
**Implementation:**
- OpenAPI specifications
- Standard protocols (REST, OAuth2)
- DUPLA integration
- Standard data formats

### Principle 8: IT Solutions are Open
**Implementation:**
- Open source (Apache 2.0 license)
- OpenAPI public specifications
- Standard technologies (Java, Spring Boot)
- No vendor lock-in

## Compliance Matrix

| Principle | OpenDebt Implementation | Status |
|-----------|------------------------|--------|
| Business-driven | Domain services | ✅ |
| Flexible/Scalable | Microservices + K8s | ✅ |
| Coherent | Common patterns | ✅ |
| Data reuse | DUPLA APIs | ✅ |
| Security | OAuth2 + RBAC | ✅ |
| Privacy | GDPR by design | ✅ |
| Interoperability | OpenAPI + standards | ✅ |
| Openness | Open source | ✅ |

## Consequences

### Positive
- Regulatory compliance
- Interoperability with other public systems
- Future-proof architecture
- Trust from stakeholders

### Negative
- Additional design constraints
- Documentation overhead
- Compliance verification effort

### Verification
- Architecture reviews against principles
- Compliance checklist in pull requests
- Regular audits
