# ADR 0014: GDPR Data Isolation - Person Registry

## Status
Accepted

## Context
GDPR-sensitive personal data (PII) must be carefully managed:

**PII Data includes:**
- CPR numbers (personnummer)
- CVR numbers (can identify sole proprietors)
- Names
- Addresses
- Email addresses
- Phone numbers
- Bank account numbers

**Problems with PII proliferation:**
1. **Data minimization violation** - GDPR requires storing PII only where necessary
2. **Right to erasure complexity** - Must find and delete PII across all databases
3. **Access control difficulty** - Hard to enforce who can see PII
4. **Audit complexity** - Must track PII access across all services
5. **Data breach risk** - More copies = more exposure

## Decision
We implement a **centralized Person Registry** that is the **single source of truth** for all PII data. All other services reference persons by a **technical key** (UUID) only.

### Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        PERSON REGISTRY SERVICE                          │
│                     (Single source of PII data)                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  person_id (UUID)  │  CPR/CVR  │  Name  │  Address  │  Email   │   │
│  │  pk-123-456...     │ encrypted │  enc.  │   enc.    │   enc.   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  Features:                                                              │
│  - Encrypted at rest (column-level AES-256-GCM)                        │
│  - Strict access control (SERVICE, CASEWORKER, GDPR_OFFICER roles)     │
│  - Full audit logging (all access tracked)                             │
│  - GDPR operations (export, delete)                                     │
│  - Temporal tables (full history)                                       │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                         UUID references only
                    ┌───────────────┼───────────────┐
                    │               │               │
                    ▼               ▼               ▼
            ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
            │ debt-service │ │ case-service │ │payment-serv. │
            │              │ │              │ │              │
            │ person_id    │ │ person_id    │ │ person_id    │
            │ (UUID only)  │ │ (UUID only)  │ │ (UUID only)  │
            │              │ │              │ │              │
            │ NO PII DATA  │ │ NO PII DATA  │ │ NO PII DATA  │
            └──────────────┘ └──────────────┘ └──────────────┘
                    │               │               │
                    └───────────────┴───────────────┘
                                    │
                         1:N relationship
                    (One person can have MANY debts/cases)
```

**Note**: Use draw.io MCP to generate visual diagrams for presentations.

### Person Registry Data Model

```sql
-- Core person table (GDPR data isolated here)
CREATE TABLE persons (
    id UUID PRIMARY KEY,                          -- Technical key used everywhere
    
    -- Encrypted identification
    identifier_encrypted BYTEA NOT NULL,          -- CPR or CVR (encrypted)
    identifier_type VARCHAR(3) NOT NULL,          -- 'CPR' or 'CVR'
    identifier_hash VARCHAR(64) NOT NULL UNIQUE,  -- SHA-256 for lookups
    
    -- Role context
    role VARCHAR(10) NOT NULL,                    -- 'PERSONAL' or 'BUSINESS'
    
    -- Encrypted PII
    name_encrypted BYTEA,
    address_encrypted BYTEA,
    email_encrypted BYTEA,
    phone_encrypted BYTEA,
    
    -- Metadata (not PII)
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    
    -- GDPR tracking
    consent_given_at TIMESTAMPTZ,
    data_retention_until DATE,
    deletion_requested_at TIMESTAMPTZ,
    
    UNIQUE(identifier_hash, role)
);
```

### Lookup Flow

```
1. Creditor submits debt with CPR: 1234567890
                    │
                    ▼
2. Person Registry: Hash CPR → lookup/create person
   - If exists: return person_id (UUID)
   - If new: create record, return person_id
                    │
                    ▼
3. Debt Service stores: person_id = "pk-123-456..."
   - NO CPR stored
   - NO name stored
   - Only technical reference
```

### API Design

```yaml
# Person Registry API (internal only)
POST /api/v1/persons/lookup
  Request:  { identifier: "1234567890", identifierType: "CPR", role: "PERSONAL" }
  Response: { personId: "uuid-here" }

GET /api/v1/persons/{personId}
  Response: { personId, name, address, ... }  # Only for authorized services

POST /api/v1/persons/{personId}/gdpr/export
  Response: { all PII data for the person }

DELETE /api/v1/persons/{personId}/gdpr/erase
  Response: { confirmation of erasure }
```

### What Other Services Store

| Service | Stores | Does NOT Store |
|---------|--------|----------------|
| debt-service | person_id (UUID), creditor_id (UUID) | CPR, CVR, names |
| case-service | person_id (UUID) | CPR, CVR, names, addresses |
| payment-service | person_id (UUID) | CPR, bank accounts |
| letter-service | person_id (UUID), letter content | Addresses (fetched on-demand) |

### Creditor Registry
Similarly, creditors (institutions) are stored in Person Registry:

```sql
CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    cvr_encrypted BYTEA NOT NULL,
    cvr_hash VARCHAR(64) NOT NULL UNIQUE,
    name_encrypted BYTEA,
    address_encrypted BYTEA,
    ...
);
```

## Consequences

### Positive
- **GDPR compliance** - PII isolated, easy to audit, export, delete
- **Data minimization** - Services only have what they need
- **Single point of control** - Access control in one place
- **Reduced breach impact** - Compromised service has no PII
- **Right to erasure** - Delete in one place, references become orphaned

### Negative
- **Additional service** - More infrastructure
- **Network dependency** - Services need Person Registry for PII
- **Caching complexity** - Can't cache PII in other services
- **Join complexity** - Must call API to get names for display

### Mitigations
- Person Registry is highly available (replicated)
- Batch API for bulk lookups
- Short-lived in-memory caching with strict TTL
- Display services aggregate data at presentation layer

## GDPR Operations

### Right to Access (Data Export)
```java
// All PII for a person in one API call
PersonDataExport export = personRegistry.exportPersonData(personId);
```

### Right to Erasure
```java
// Erase PII, other services keep orphaned UUIDs
personRegistry.erasePerson(personId);
// Cascading: debts remain but person_id points to deleted record
```

### Data Retention
```sql
-- Automatic cleanup of expired data
DELETE FROM persons WHERE data_retention_until < CURRENT_DATE;
```

## Security

- Person Registry database encrypted at rest
- Column-level encryption for PII fields
- TLS for all API calls
- Strict RBAC - only authorized services can access
- All access logged to audit_log
- No PII in logs (only person_id references)
