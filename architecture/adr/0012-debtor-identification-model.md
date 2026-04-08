# ADR 0012: Debtor Identification Model (CPR/CVR with Role)

## Status
Accepted

## Context
In the Danish debt collection system, debtors can be identified in multiple ways:

1. **Natural persons** are identified by CPR (personnummer) - 10 digits
2. **Companies** are identified by CVR (virksomhedsnummer) - 8 digits
3. **Sole proprietors** (enkeltmandsvirksomheder) are natural persons running a business, identified by CPR

Critical insight: **A natural person (CPR) can have debt in two distinct capacities**:
- **Personal debt** - incurred as a private individual (e.g., unpaid taxes, traffic fines)
- **Business debt** - incurred as a sole proprietor (e.g., VAT debt, business-related fees)

These must be tracked separately because:
- Different collection rules may apply
- Different payment abilities (personal vs. business assets)
- Different legal implications
- Separate accounting requirements

## Decision
We implement a **composite debtor identifier** consisting of three parts:

```
┌─────────────────────────────────────────────────────────────┐
│                    Debtor Identifier                        │
├─────────────────┬─────────────────┬─────────────────────────┤
│   identifier    │ identifier_type │        role             │
│  (CPR or CVR)   │   (CPR/CVR)     │  (PERSONAL/BUSINESS)    │
├─────────────────┼─────────────────┼─────────────────────────┤
│   1234567890    │      CPR        │      PERSONAL           │
│   1234567890    │      CPR        │      BUSINESS           │ ← Same person, different role
│   12345678      │      CVR        │      BUSINESS           │
└─────────────────┴─────────────────┴─────────────────────────┘
```

### Database Schema
```sql
-- Debtor columns in all relevant tables
debtor_identifier VARCHAR(10) NOT NULL,
debtor_identifier_type VARCHAR(3) NOT NULL CHECK (identifier_type IN ('CPR', 'CVR')),
debtor_role VARCHAR(10) NOT NULL CHECK (role IN ('PERSONAL', 'BUSINESS')),

-- Constraints
CONSTRAINT chk_cpr_format CHECK (
    debtor_identifier_type != 'CPR' OR debtor_identifier ~ '^[0-9]{10}$'
),
CONSTRAINT chk_cvr_format CHECK (
    debtor_identifier_type != 'CVR' OR debtor_identifier ~ '^[0-9]{8}$'
),
CONSTRAINT chk_cvr_must_be_business CHECK (
    debtor_identifier_type != 'CVR' OR debtor_role = 'BUSINESS'
)
```

### Valid Combinations

| identifier_type | role | Example Use Case |
|-----------------|------|------------------|
| CPR | PERSONAL | Private person's unpaid taxes, fines |
| CPR | BUSINESS | Sole proprietor's VAT debt |
| CVR | BUSINESS | Company's unpaid fees |
| CVR | PERSONAL | **Invalid** - CVR cannot have personal debt |

### Composite Key
The composite key `{identifier_type}:{identifier}:{role}` uniquely identifies a debtor in a specific capacity:
- `CPR:1234567890:PERSONAL` - Hans Hansen's personal debts
- `CPR:1234567890:BUSINESS` - Hans Hansen's business debts (enkeltmandsvirksomhed)
- `CVR:12345678:BUSINESS` - ABC ApS company debts

### Java Implementation
```java
public class DebtorIdentifier {
    private String identifier;        // CPR or CVR number
    private IdentifierType identifierType;  // CPR or CVR
    private DebtorRole role;          // PERSONAL or BUSINESS
    
    public String toCompositeKey() {
        return String.format("%s:%s:%s", identifierType, identifier, role);
    }
}
```

## Consequences

### Positive
- Clear separation between personal and business debt for same person
- Accurate tracking for sole proprietors
- Supports all Danish identification scenarios
- Enables correct legal treatment per debt type
- Proper audit trail per role

### Negative
- More complex queries (3-column lookups)
- Need to specify role when creating debts
- UI must clearly indicate which role debt applies to

### Mitigations
- Composite index on (identifier, identifier_type, role)
- Default role inference where possible (CVR → BUSINESS)
- Clear UI labels: "Privat gæld" vs "Erhvervsgæld"

## Examples

### Scenario: Hans Hansen (CPR: 1234567890)
Hans is both a private person and runs a small consulting business (enkeltmandsvirksomhed).

**Personal debts:**
- Traffic fine: `CPR:1234567890:PERSONAL`
- Unpaid personal tax: `CPR:1234567890:PERSONAL`

**Business debts:**
- Unpaid VAT: `CPR:1234567890:BUSINESS`
- Business registration fee: `CPR:1234567890:BUSINESS`

Both can be collected, but with different rules and asset considerations.

### Scenario: ABC ApS (CVR: 12345678)
Company with only business debts:
- Corporate tax: `CVR:12345678:BUSINESS`
- Environmental fee: `CVR:12345678:BUSINESS`
