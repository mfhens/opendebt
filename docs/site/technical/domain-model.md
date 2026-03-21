# Domain Model

OpenDebt follows the UFST begrebsmodel (concept model) for Danish public debt collection. All source code uses **English** terms; the mapping below is the canonical reference.

## Terminology mapping

| Danish (begrebsmodel) | English (code) | Example usage |
|----------------------|----------------|---------------|
| Fordringshaver | Creditor | `CreditorService`, `creditor_org_id` |
| Skyldner | Debtor | `DebtorPersonId`, `debtor_person_id` |
| Fordring | Claim / Debt | `DebtEntity`, `/api/v1/debts` |
| Restance | Overdue Claim | `ClaimLifecycleState.RESTANCE` |
| Fordringstype | Claim Type | `debtTypeCode` |
| Hovedstol | Principal | `principalAmount` |
| Betalingsfrist | Payment Deadline | `paymentDeadline` |
| Forældelse | Limitation | `limitationDate` |
| Overdragelse til inddrivelse | Transfer for Collection | `transferForCollection()` |
| Inddrivelsesskridt | Collection Measure | `CollectionMeasure` |
| Modregning | Set-off | `offsetting-service` |
| Lønindeholdelse | Wage Garnishment | `wage-garnishment-service` |
| Udlæg | Attachment | `AttachmentService` |
| Hæftelse | Liability | `LiabilityEntity` |
| Indsigelse | Objection | `ObjectionService` |
| Underretning | Notification | `NotificationService` |
| Påkrav | Demand for Payment | `PAAKRAV` |
| Rykker | Reminder Notice | `RYKKER` |
| Inddrivelsesrente | Recovery Interest | `recoveryInterestRate` |
| Regulering | Claim Adjustment | `ClaimAdjustmentEvent` |
| Opskrivning | Write-up | write-up endpoint |
| Nedskrivning | Write-down | `/debts/{id}/write-down` |
| Tilbagekald | Withdrawal | `TILBAGEKALDT` state |
| Høring | Hearing | `HOERING` state |
| Sag | Case | `CaseEntity` |
| Rentesats | Interest Rate | `interestRate`, `RATE_NB_UDLAAN` |
| Konfiguration | Business Configuration | `BusinessConfigEntity`, `/api/v1/config` |
| Konfigurationspost | Config Entry | `BusinessConfigEntity` instance |
| Gyldighedsperiode | Validity Period | `validFrom` / `validTo` on config entries |
| Godkendelse | Approval | `ReviewStatus.APPROVED` |
| Afventer godkendelse | Pending Review | `ReviewStatus.PENDING_REVIEW` |
| Afledt sats | Derived Rate | auto-computed from `RATE_NB_UDLAAN` |
| Rentejournal | Interest Journal | `InterestJournalEntry` |
| Rentegrænse | Rate Boundary | year-boundary split in interest recalculation |
| Dækning | Recovery / Payment applied | payment matching |
| Dækningsrækkefølge | Coverage Priority | interest before fees before principal |

## Entity relationships

```mermaid
erDiagram
    CREDITOR ||--o{ DEBT : "submits"
    DEBT ||--o{ LIABILITY : "has"
    LIABILITY }o--|| PERSON : "liable party"
    DEBT ||--o{ LIFECYCLE_EVENT : "tracks"
    DEBT ||--o{ NOTIFICATION : "generates"
    DEBT ||--o{ OBJECTION : "may have"
    DEBT ||--o{ COLLECTION_MEASURE : "subject to"
    DEBT ||--o{ PAYMENT : "receives"
    CASE ||--o{ DEBT : "contains"
    CASE }o--|| PERSON : "assigned caseworker"

    CREDITOR {
        UUID id PK
        UUID creditorOrgId
        string name
        string externalCreditorId
        boolean active
    }

    DEBT {
        UUID id PK
        string debtorId
        UUID debtorPersonId FK
        string creditorId
        string debtTypeCode
        decimal principalAmount
        decimal outstandingBalance
        date dueDate
        date paymentDeadline
        date limitationDate
        string lifecycleState
        string status
    }

    PERSON {
        UUID id PK
        string identifierType
        string role
        bytes identifierEncrypted
        string identifierHash
    }

    CASE {
        UUID id PK
        string caseNumber
        string status
        UUID assignedCaseworkerId
    }

    LIABILITY {
        UUID id PK
        UUID debtId FK
        UUID debtorPersonId FK
        string liabilityType
        decimal shareAmount
    }

    LIFECYCLE_EVENT {
        UUID id PK
        UUID debtId FK
        string fromState
        string toState
        string trigger
        timestamp occurredAt
    }

    PAYMENT {
        UUID id PK
        UUID debtId FK
        decimal amount
        string ocrLine
        date valueDate
    }
```

## Claim lifecycle states

```mermaid
stateDiagram-v2
    [*] --> REGISTERED : Claim submitted
    REGISTERED --> RESTANCE : Payment deadline expired
    RESTANCE --> HOERING : Under review
    RESTANCE --> OVERDRAGET : Transferred for collection
    HOERING --> OVERDRAGET : Hearing resolved
    HOERING --> TILBAGEKALDT : Hearing rejected
    OVERDRAGET --> INDFRIET : Fully paid
    OVERDRAGET --> AFSKREVET : Written off
    OVERDRAGET --> TILBAGEKALDT : Withdrawn
    REGISTERED --> INDFRIET : Paid before deadline
    TILBAGEKALDT --> [*]
    AFSKREVET --> [*]
    INDFRIET --> [*]
```

For the full begrebsmodel, see `docs/begrebsmodel/Inddrivelse-begrebsmodel-UFST-v3.md` in the repository.
