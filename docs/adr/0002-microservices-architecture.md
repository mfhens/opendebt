# ADR 0002: Microservices Architecture

## Status
Accepted

## Context
OpenDebt is a complex debt collection system that needs to handle:
- ~600 debt types from ~1,200 public institutions
- Multiple user interfaces (creditor portal, citizen portal)
- Various collection mechanisms (offsetting, wage garnishment)
- Integration with external systems (TastSelv, Digital Post, DUPLA)
- High availability and scalability requirements

The system must comply with Fællesoffentlige Arkitekturprincipper (Danish public sector architecture principles), which emphasize:
- Loosely coupled services
- Reusable components
- Clear service boundaries
- Interoperability

## Decision
We adopt a microservices architecture with the following services:

| Service | Responsibility |
|---------|---------------|
| `case-service` | Case management, workflow, assignment |
| `debt-service` | Debt registration, readiness validation |
| `payment-service` | Payment processing, reconciliation |
| `letter-service` | Letter generation, Digital Post integration |
| `offsetting-service` | Modregning (offsetting) processing |
| `wage-garnishment-service` | Loenindeholdelse processing |
| `creditor-portal` | UI/API for fordringshavere |
| `citizen-portal` | UI/API for borgere, TastSelv integration |
| `integration-gateway` | External integrations via DUPLA |

Services communicate via REST APIs defined in OpenAPI specifications.

## Consequences

### Positive
- Independent deployment and scaling of services
- Technology flexibility per service
- Fault isolation
- Easier to understand individual service boundaries
- Aligns with Fællesoffentlige Arkitekturprincipper

### Negative
- Increased operational complexity
- Network latency between services
- Distributed transaction challenges
- Need for robust service discovery and monitoring
- More complex testing (integration tests)

### Mitigations
- Use Kubernetes for orchestration and service discovery
- Implement circuit breakers and retries
- Comprehensive monitoring with Prometheus/Grafana
- Contract testing with OpenAPI specifications
