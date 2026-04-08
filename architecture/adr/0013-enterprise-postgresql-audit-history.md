# ADR 0013: Enterprise PostgreSQL with Audit and History

## Status
Accepted

## Context
OpenDebt handles sensitive financial data for Danish public debt collection. Enterprise requirements mandate:

1. **Full audit trail** - Track who changed what, when, and how (including direct DB access)
2. **Temporal history** - Ability to query data "as of" any point in time
3. **Compliance** - Meet GDPR, Rigsarkivet, and public sector audit requirements
4. **Data integrity** - Prevent unauthorized modifications
5. **Forensics** - Support investigation of data issues

## Decision
We implement enterprise-grade PostgreSQL features across all services:

### 1. Audit Logging
Every table modification is logged to an `audit_log` table:

```sql
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    record_id UUID NOT NULL,
    operation VARCHAR(10) NOT NULL,  -- INSERT, UPDATE, DELETE
    old_values JSONB,                -- Previous state (UPDATE/DELETE)
    new_values JSONB,                -- New state (INSERT/UPDATE)
    changed_fields TEXT[],           -- List of modified columns
    db_user VARCHAR(100) NOT NULL,   -- PostgreSQL user
    application_user VARCHAR(100),   -- Application-level user
    client_ip INET,                  -- Client IP address
    client_application VARCHAR(200), -- Application name
    transaction_id BIGINT,           -- PostgreSQL transaction ID
    timestamp TIMESTAMPTZ NOT NULL
);
```

### 2. Audit Context from Application
Applications set audit context before operations:

```java
@Component
public class AuditContextSetter {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public void setAuditContext(String userId, String clientIp, String appName) {
        jdbcTemplate.execute(
            "SELECT set_audit_context(?, ?::inet, ?)",
            ps -> {
                ps.setString(1, userId);
                ps.setString(2, clientIp);
                ps.setString(3, appName);
            }
        );
    }
}
```

### 3. Temporal Tables (System-Versioned)
Each main entity table has a corresponding history table:

```
┌─────────────────┐          ┌─────────────────────┐
│     debts       │          │   debts_history     │
├─────────────────┤          ├─────────────────────┤
│ id              │──────────│ id                  │
│ ...             │          │ ...                 │
│ sys_period      │          │ sys_period          │
│ (current)       │          │ (closed range)      │
└─────────────────┘          └─────────────────────┘
```

**sys_period**: `TSTZRANGE` column tracking validity period
- Current records: `[2024-01-15, NULL)` (open-ended)
- Historical records: `[2024-01-01, 2024-01-15)` (closed)

### 4. Automatic Versioning Trigger
```sql
CREATE TRIGGER debts_versioning_trigger
    BEFORE UPDATE OR DELETE ON debts
    FOR EACH ROW
    EXECUTE FUNCTION versioning_trigger_function();
```

On UPDATE:
1. Copy OLD record to history table
2. Close the sys_period in history
3. Update current record with new sys_period
4. Increment version number

### 5. Point-in-Time Queries
```sql
-- Get debt as it was on 2024-01-10
SELECT * FROM get_debt_as_of('uuid-here', '2024-01-10'::timestamptz);

-- Get complete history of a debt
SELECT * FROM get_debt_history('uuid-here');
```

### 6. Database Security
```sql
-- Separate users for application vs. direct access
CREATE ROLE opendebt_app LOGIN PASSWORD 'xxx';
CREATE ROLE opendebt_admin LOGIN PASSWORD 'xxx';

-- Application role: limited permissions
GRANT SELECT, INSERT, UPDATE ON debts TO opendebt_app;
GRANT SELECT, INSERT ON audit_log TO opendebt_app;

-- Admin role: requires explicit audit
GRANT ALL ON ALL TABLES TO opendebt_admin;
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Application Layer                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │ Set Audit   │→ │   JPA       │→ │   Entity    │          │
│  │ Context     │  │  Operation  │  │   Change    │          │
│  └─────────────┘  └─────────────┘  └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     PostgreSQL                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │   debts     │→ │ Versioning  │→ │   debts_    │          │
│  │  (current)  │  │  Trigger    │  │  history    │          │
│  └─────────────┘  └──────┬──────┘  └─────────────┘          │
│                          │                                   │
│                          ▼                                   │
│                   ┌─────────────┐                            │
│                   │  audit_log  │                            │
│                   └─────────────┘                            │
└─────────────────────────────────────────────────────────────┘
```

## Consequences

### Positive
- Complete audit trail for compliance
- Point-in-time queries for debugging/forensics
- Tracks both application and direct DB changes
- Immutable history (append-only)
- Standard PostgreSQL features (no extensions needed)

### Negative
- Storage overhead (history tables grow)
- Slight write performance impact (triggers)
- More complex queries for historical data

### Mitigations
- Partition history tables by time period
- Archive old history to cold storage
- Index history tables appropriately
- Regular maintenance (VACUUM, ANALYZE)

## Retention Policy
- **Audit logs**: 10 years (legal requirement)
- **History tables**: 7 years active, then archive
- **Archived data**: According to Rigsarkivet guidelines

## Monitoring
- Alert on high audit log growth
- Monitor trigger execution time
- Track history table sizes
