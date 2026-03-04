# OpenDebt Person Registry

**The single source of truth for all personal data (PII) in the OpenDebt system.**

## Purpose

The Person Registry is a critical GDPR compliance component that isolates all personal identifiable information (PII) in one place. Other services in OpenDebt store only technical UUIDs that reference this registry.

## Data Model

### Persons (Debtors)

```
┌─────────────────────────────────────────────────────────────────┐
│                         persons                                  │
├─────────────────────────────────────────────────────────────────┤
│ id (UUID)                 ← Technical key shared with services  │
│ identifier_encrypted      ← CPR/CVR (AES-256-GCM encrypted)     │
│ identifier_type           ← 'CPR' or 'CVR'                      │
│ identifier_hash           ← SHA-256 for lookups                 │
│ role                      ← 'PERSONAL' or 'BUSINESS'            │
│ name_encrypted            ← Full name (encrypted)               │
│ address_*_encrypted       ← Address fields (encrypted)          │
│ email_encrypted           ← Email (encrypted)                   │
│ phone_encrypted           ← Phone (encrypted)                   │
│ digital_post_enabled      ← Communication preference            │
│ consent_given_at          ← GDPR consent tracking               │
│ data_retention_until      ← Retention policy date               │
│ deletion_requested_at     ← GDPR erasure request                │
└─────────────────────────────────────────────────────────────────┘
```

### Organizations (Creditors)

```
┌─────────────────────────────────────────────────────────────────┐
│                       organizations                              │
├─────────────────────────────────────────────────────────────────┤
│ id (UUID)                 ← Technical key for creditors         │
│ cvr_encrypted             ← CVR number (encrypted)              │
│ cvr_hash                  ← SHA-256 for lookups                 │
│ name_encrypted            ← Organization name (encrypted)       │
│ address_encrypted         ← Address (encrypted)                 │
│ organization_type         ← MUNICIPALITY, REGION, STATE_AGENCY  │
│ active                    ← Whether creditor is active          │
└─────────────────────────────────────────────────────────────────┘
```

## Debtor Identification

A natural person (CPR) can have debt in two capacities:

| Identifier Type | Role | Use Case |
|-----------------|------|----------|
| CPR | PERSONAL | Private person's debts (fines, taxes) |
| CPR | BUSINESS | Sole proprietor's debts (VAT, business fees) |
| CVR | BUSINESS | Company debts |

**Same CPR, different roles = different person records:**
```
person_id: abc-123  (CPR:1234567890, PERSONAL)  → Personal debts
person_id: def-456  (CPR:1234567890, BUSINESS)  → Business debts
```

## API Endpoints

### Lookup/Create Person
```http
POST /person-registry/api/v1/persons/lookup
Authorization: Bearer <service-token>
Content-Type: application/json

{
  "identifier": "1234567890",
  "identifierType": "CPR",
  "role": "PERSONAL",
  "name": "Hans Hansen",           // Optional
  "addressStreet": "Vestergade 1"  // Optional
}

Response:
{
  "personId": "abc-123-def-456",   // Use this UUID everywhere
  "created": false,
  "role": "PERSONAL"
}
```

### Get Person Details
```http
GET /person-registry/api/v1/persons/{personId}
Authorization: Bearer <service-token>

Response:
{
  "id": "abc-123-def-456",
  "identifierType": "CPR",
  "role": "PERSONAL",
  "name": "Hans Hansen",
  "addressStreet": "Vestergade 1",
  "addressCity": "København",
  ...
}
```

### GDPR Export
```http
POST /person-registry/api/v1/persons/{personId}/gdpr/export
Authorization: Bearer <gdpr-officer-token>

Response: Complete data export for the person
```

### GDPR Erasure
```http
DELETE /person-registry/api/v1/persons/{personId}/gdpr/erase?reason=GDPR%20request
Authorization: Bearer <gdpr-officer-token>

Response: 202 Accepted
```

## Security

### Encryption
- **Algorithm**: AES-256-GCM (authenticated encryption)
- **Key Management**: 256-bit key via environment variable
- **At Rest**: All PII columns encrypted in database

### Access Control
| Role | Permissions |
|------|-------------|
| SERVICE | Lookup, create, read persons |
| CASEWORKER | Read person details |
| GDPR_OFFICER | Export, erase person data |
| ADMIN | Full access |

### Audit Logging
All access is logged to `audit_log` table:
- Who accessed (db_user, application_user)
- What was accessed (table, record_id)
- When (timestamp)
- From where (client_ip)

**Note**: Encrypted field values are redacted from audit logs.

## Integration

### How Other Services Use Person Registry

```java
@Service
@RequiredArgsConstructor
public class DebtService {
    private final PersonRegistryClient personRegistryClient;
    
    public DebtDto createDebt(CreateDebtRequest request) {
        // 1. Lookup/create person in registry
        PersonLookupResponse person = personRegistryClient.lookupOrCreate(
            PersonLookupRequest.builder()
                .identifier(request.getCpr())
                .identifierType(IdentifierType.CPR)
                .role(PersonRole.PERSONAL)
                .build()
        );
        
        // 2. Store only the UUID
        DebtEntity debt = DebtEntity.builder()
            .debtorPersonId(person.getPersonId())  // UUID only!
            .creditorOrgId(request.getCreditorOrgId())
            .principalAmount(request.getAmount())
            .build();
        
        return debtRepository.save(debt);
    }
}
```

### Displaying Person Information

When UI needs to show person details:
```java
public DebtWithPersonDto getDebtWithPerson(UUID debtId) {
    DebtEntity debt = debtRepository.findById(debtId).orElseThrow();
    
    // Fetch person details on-demand (not cached)
    PersonDto person = personRegistryClient.getPerson(debt.getDebtorPersonId());
    
    return DebtWithPersonDto.builder()
        .debtId(debt.getId())
        .amount(debt.getPrincipalAmount())
        .debtorName(person.getName())  // From Person Registry
        .build();
}
```

## Configuration

```yaml
spring:
  application:
    name: opendebt-person-registry

opendebt:
  encryption:
    key: ${ENCRYPTION_KEY}  # Base64-encoded 256-bit key
  gdpr:
    default-retention-years: 10
    deletion-grace-period-days: 30
```

## Database

### Tables
- `persons` - Natural persons and sole proprietors
- `persons_history` - Temporal history (point-in-time queries)
- `organizations` - Creditor institutions
- `organizations_history` - Temporal history
- `audit_log` - All access and modifications

### Key Features
- **Temporal tables**: Full history of all changes
- **Audit triggers**: Automatic logging of all modifications
- **Encrypted columns**: PII encrypted at rest
- **Hash indexes**: Fast lookups without exposing PII

## GDPR Compliance

### Right to Access (Article 15)
Use `/gdpr/export` endpoint to get all data for a person.

### Right to Erasure (Article 17)
Use `/gdpr/erase` endpoint to delete person data. The UUID remains but all PII is cleared.

### Data Minimization (Article 5)
Other services store only UUIDs, minimizing PII exposure.

### Retention Policy
`data_retention_until` field enforces automatic cleanup of expired data.
