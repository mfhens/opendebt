# ADR 0011: PostgreSQL as Database

## Status
Accepted

## Context
OpenDebt services require persistent data storage for:
- Debt records (~600 types, potentially millions of records)
- Case management workflow state
- Payment transactions
- Letter templates and delivery tracking
- Offsetting and wage garnishment records

Requirements:
- ACID compliance for financial data integrity
- Scalability for high transaction volumes
- JSON support for flexible data structures
- Strong ecosystem and tooling support
- Alignment with UFST technology standards

## Decision
We adopt **PostgreSQL 16** as the standard database for all OpenDebt services.

### Database per Service
Each microservice owns its database (database-per-service pattern):

| Service | Database |
|---------|----------|
| case-service | opendebt_case |
| debt-service | opendebt_debt |
| payment-service | opendebt_payment |
| letter-service | opendebt_letter |
| offsetting-service | opendebt_offsetting |
| wage-garnishment-service | opendebt_wage_garnishment |

### Schema Management
- **Flyway** for versioned migrations
- Migrations stored in `src/main/resources/db/migration/`
- Naming convention: `V{version}__{description}.sql`
- Validate mode in production (`ddl-auto: validate`)

### Connection Configuration
```yaml
spring:
  datasource:
    url: jdbc:postgresql://host:5432/database
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

### Key PostgreSQL Features Used
- **UUID** primary keys (gen_random_uuid())
- **JSONB** for flexible metadata (template variables, etc.)
- **Constraints** for data integrity
- **Indexes** for query performance
- **Triggers** for audit timestamps

### Data Isolation
- Services access **only their own database**
- Cross-service data access is via **REST APIs only**
- No shared tables or cross-database queries

## Consequences

### Positive
- UFST standard technology with existing expertise
- Excellent performance for complex queries
- Strong data integrity with ACID compliance
- Rich feature set (JSONB, full-text search, etc.)
- Mature tooling (pgAdmin, pg_dump, etc.)
- Open source with no licensing costs

### Negative
- Operational overhead of multiple databases
- Need for separate backup strategies per service
- Connection pooling configuration per service

### Mitigations
- Use managed PostgreSQL in production (platform-provided)
- Standardized backup procedures
- Monitoring of connection pools via metrics

## Alternatives Considered

| Option | Reason Not Chosen |
|--------|-------------------|
| Oracle | Licensing costs, not UFST standard |
| MySQL | Less feature-rich, UFST prefers PostgreSQL |
| MongoDB | Not suitable for financial ACID requirements |
| SQL Server | Licensing, not open source |

## Security Considerations
- Credentials stored in Kubernetes Secrets
- TLS for database connections in production
- Principle of least privilege for service accounts
- No direct database access from external systems
