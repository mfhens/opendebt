# ADR-0022: Shared Audit Infrastructure

## Status

Accepted

## Date

2026-03-14

## Context

OpenDebt is a microservices-based debt collection system where each service owns its own PostgreSQL database (per ADR-0007, ADR-0013). All services require comprehensive audit logging for:

1. **Regulatory compliance** - Danish public sector systems must maintain audit trails
2. **Security monitoring** - Tracking who accessed/modified what data
3. **Debugging and support** - Understanding state changes over time
4. **UFST CLS integration** - Shipping audit events to the Common Logging System

The initial implementation duplicated audit infrastructure across services:
- Each service's Flyway migration created identical `audit_log` tables
- Each service defined identical `audit_trigger_function()` PostgreSQL functions
- Each entity class duplicated `createdAt`, `updatedAt`, `createdBy`, `version` fields
- No shared mechanism for CLS integration

SonarCloud flagged 3% code duplication, primarily in SQL migrations and entity audit fields.

## Decision

We will consolidate audit infrastructure into `opendebt-common` while respecting microservice database isolation:

### 1. Shared Java Components (in opendebt-common)

**AuditableEntity** - `@MappedSuperclass` providing standard audit fields:
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @CreatedBy
    private String createdBy;
    
    @LastModifiedBy
    private String updatedBy;
    
    @Version
    private Long version;
}
```

**AuditContextService** - Sets PostgreSQL session variables for audit triggers (already exists)

**AuditContextFilter** - Captures user/IP from HTTP requests (already exists)

**AuditingConfig** - Configures Spring Data JPA auditing with security context integration

**CLS Client (Java - DEPRECATED)** - Optional fallback for direct API shipping:
- `ClsAuditClient` interface
- `ClsAuditClientImpl` - Async batched shipping with retry logic
- `NoOpClsAuditClient` - For development/testing
- `ClsAuditEventMapper` - Maps DB audit records to CLS format with PII masking

> **Note:** The Java CLS client is retained for edge cases but the preferred approach
> is Filebeat (see section 4 below).

### 4. CLS Integration via Filebeat (Recommended)

Since CLS is an ELK stack (Elasticsearch, Logstash, Kibana), the most robust approach
is using **Filebeat with SQL input** to poll `audit_log` tables directly:

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ PostgreSQL      │     │ Filebeat        │     │ CLS (ELK)       │
│                 │     │                 │     │                 │
│ audit_log table │ ──▶ │ SQL input       │ ──▶ │ Elasticsearch   │
│ (per service)   │     │ (polls every    │     │                 │
│                 │     │  10 seconds)    │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

**Why Filebeat over Java client:**
- **Decoupled** - Works even when Java services are down
- **Standard ELK pattern** - Ops teams already know this
- **No application latency** - Shipping doesn't impact request handling
- **Cursor-based** - Tracks last shipped ID, handles restarts gracefully
- **Built-in PII masking** - JavaScript processor masks sensitive fields

**Configuration:** See `config/filebeat/filebeat-audit.yml`

**Deployment options:**
1. Sidecar container alongside each service
2. Single Filebeat instance with access to all databases
3. DaemonSet on Kubernetes nodes

### 2. Database Audit Infrastructure (per service - NOT shared)

Each service's database MUST maintain its own:
- `audit_log` table
- `audit_trigger_function()` 
- History tables and triggers

This is architecturally correct because:
- Each microservice owns its database (ADR-0007)
- PostgreSQL functions/triggers cannot be shared across databases
- Database-level audit captures ALL changes, including direct DB access

### 3. PII Masking for CLS

The `ClsAuditEventMapper` masks sensitive fields before transmission:
- CPR/CVR numbers
- Names, addresses, contact info
- Bank account details

This ensures CLS receives audit metadata without exposing person registry data.

## Consequences

### Positive

1. **DRY Java code** - Entity audit fields defined once
2. **Single CLS integration** - One client implementation for all services
3. **Consistent behavior** - All services audit identically
4. **Reduced SonarCloud duplication** - Java duplication eliminated
5. **PII protection** - Automatic masking before CLS transmission

### Negative

1. **SQL duplication remains** - Required for database isolation
2. **Additional dependency** - Services depend on opendebt-common for auditing
3. **Migration coordination** - Entities must be updated to extend AuditableEntity

### Neutral

1. **No runtime coupling** - CLS client operates asynchronously
2. **Gradual adoption** - Existing entities can be migrated incrementally

## Implementation Notes

### Entity Migration Pattern

```java
// Before
@Entity
public class CreditorEntity {
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp  
    private LocalDateTime updatedAt;
    @Column(name = "created_by")
    private String createdBy;
    @Version
    private Long version;
    // ... other fields
}

// After
@Entity
public class CreditorEntity extends AuditableEntity {
    // Audit fields inherited
    // ... other fields only
}
```

### CLS Configuration (Filebeat - Recommended)

```yaml
# config/filebeat/filebeat-audit.yml
filebeat.inputs:
  - type: sql
    driver: postgres
    dsn: "postgres://user:pass@postgres:5432/opendebt_creditor"
    sql: |
      SELECT * FROM audit_log 
      WHERE id > :cursor.id
      ORDER BY id ASC LIMIT 1000
    cursor:
      id:
        initial_value: 0
    period: 10s
    
output.elasticsearch:
  hosts: ["https://cls.ufst.dk:9200"]
  index: "opendebt-audit-%{[fields.service]}-%{+yyyy.MM}"
```

### CLS Configuration (Java Client - Fallback)

```yaml
opendebt:
  audit:
    cls:
      enabled: false  # Use Filebeat instead
      endpoint: https://cls.ufst.dk/api/v1/events
      batch-size: 100
      flush-interval-ms: 5000
      retry-attempts: 3
```

## Related ADRs

- ADR-0007: No Cross-Service Database Connections
- ADR-0013: Enterprise PostgreSQL with Audit and History
- ADR-0014: GDPR Data Isolation - Person Registry

## References

- UFST Common Logging System (CLS) Integration Guide
- Danish Public Sector Audit Requirements
- Spring Data JPA Auditing Documentation
