# ADR 0007: No Direct Database Connections - API-Only Data Access

## Status
Accepted

## Context
The requirement states: "Avoid direct connections to the database, use API's defined via OpenAPI."

This aligns with:
1. **Fællesoffentlige Arkitekturprincipper**: Services should be loosely coupled
2. **UFST standards**: Data access via controlled APIs
3. **Security**: Reduced attack surface, audit logging
4. **DUPLA integration**: All external access via API gateway

## Decision
OpenDebt services do NOT directly connect to databases. Instead:

### Data Access Pattern
```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Service A  │────▶│   Service B  │────▶│   Database   │
│  (Consumer)  │ API │  (Provider)  │     │              │
└──────────────┘     └──────────────┘     └──────────────┘
```

Each bounded context owns its data and exposes it via APIs.

### Service Data Ownership
| Service | Data Owned |
|---------|------------|
| debt-service | Debts, debt types, readiness status |
| case-service | Cases, workflow state, assignments |
| payment-service | Payments, transactions |
| letter-service | Letters, templates, delivery status |
| offsetting-service | Offsetting rules, pending offsets |
| wage-garnishment-service | Garnishment orders, employer info |

### Data Store Strategy (Future Implementation)
When persistence is added:
- Each service has its own database schema/instance
- No shared databases between services
- Database migrations managed per service
- Backup/restore per service

### Cross-Service Data Access
- Synchronous: REST API calls
- Asynchronous: Event-driven (future consideration)
- Caching: Service-level caching with invalidation

### External Data Access
All external systems access data via:
1. DUPLA API Gateway (system-to-system)
2. Creditor Portal API (institution users)
3. Citizen Portal API (citizens via TastSelv)

## Consequences

### Positive
- Clear data ownership boundaries
- Independent service evolution
- Audit trail for all data access
- Security through API controls
- Easier compliance (GDPR, logging)

### Negative
- Network overhead for data access
- Eventual consistency challenges
- More complex queries across services
- API versioning complexity

### Mitigations
- Efficient API design (batch operations)
- CQRS pattern for complex queries (future)
- Event sourcing for audit trail (future)
- GraphQL aggregation layer (optional)

### Note
This ADR describes the inter-service data access pattern. Each service has its own PostgreSQL database (see ADR-0011), but services must not directly access other services' databases - only their own. Cross-service data access is via REST APIs.
