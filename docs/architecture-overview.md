# OpenDebt - Architecture Overview and Implementation Status

## System Overview

OpenDebt is an open-source debt collection system for Danish public institutions (UFST), designed to replace legacy systems (EFI/DMI). It is built as a microservices architecture using Java 21, Spring Boot 3.3, and PostgreSQL 16, deployed on Kubernetes.

```mermaid
graph TB
    subgraph External["External Systems"]
        TastSelv["TastSelv<br/>(MitID)"]
        DigitalPost["Digital Post<br/>(e-Boks)"]
        SKB["Statens Koncernbetalinger<br/>(CREMUL/DEBMUL)"]
        ES["Creditor systems<br/>(OCES3 / DUPLA)"]
    end

    subgraph Gateway["Integration Layer"]
        IG["integration-gateway<br/>:8089<br/>DUPLA, CPR, CVR, SKB"]
    end

    subgraph Portals["User Portals"]
        CP["creditor-portal<br/>:8085<br/>Fordringshavere"]
        BP["citizen-portal<br/>:8086<br/>Borgere"]
    end

    subgraph Core["Core Services"]
        DS["debt-service<br/>:8082<br/>Debt registration"]
        CS["case-service<br/>:8081<br/>Flowable BPMN"]
        PS["payment-service<br/>:8083<br/>Bookkeeping"]
        CRS["creditor-service<br/>:8092<br/>Creditor master data"]
    end

    subgraph Collection["Collection Services"]
        LS["letter-service<br/>:8084<br/>Digital Post"]
        OS["offsetting-service<br/>:8087<br/>Modregning"]
        WG["wage-garnishment-service<br/>:8088<br/>Loenindeholdelse"]
    end

    subgraph Foundation["Foundation Services"]
        RE["rules-engine<br/>:8091<br/>Drools"]
        PR["person-registry<br/>:8090<br/>GDPR vault"]
    end

    TastSelv --> IG
    DigitalPost --> IG
    SKB --> IG
    ES --> IG

    IG --> PS
    IG --> DS
    IG --> CRS

    CP --> DS
    CP --> CS
    CP --> CRS
    BP --> DS
    BP --> CS
    BP --> PS

    CS --> DS
    CS --> PS
    CS --> LS
    CS --> OS
    CS --> WG
    CS --> IG

    DS --> RE
    DS --> PR
    DS --> CRS
    CRS --> PR
    CS --> PR
    PS --> DS
```

### Creditor Interaction Target Architecture (ADR-0020)

OpenDebt treats creditor interaction as two separate channels: **M2M/STP by default** and **portal/manual as a supplementary channel**. The target architecture is therefore:

- `integration-gateway` owns external M2M ingress via DUPLA
- `debt-service` owns `fordring` submission and lifecycle
- `creditor-service` owns non-PII `fordringshaver` master data and channel binding
- `creditor-portal` is a UI/BFF only, not a master-data system of record
- `person-registry` remains the source of organization identity and PII-like data

```mermaid
flowchart LR
    EXT[Creditor system] --> DUPLA[DUPLA]
    DUPLA --> IG[integration-gateway]
    IG --> CRS[creditor-service]
    IG --> DS[debt-service]

    CP[creditor-portal] --> CRS
    CP --> DS
    CP --> CS[case-service]

    DS --> CRS
    CRS --> PR[person-registry]
```

| Service | Target responsibility |
|---------|-----------------------|
| `creditor-portal` | Visualization, manual entry, and user interaction for creditor users |
| `integration-gateway` | External M2M ingress, protocol/security adaptation, routing, error mapping |
| `creditor-service` | Operational creditor master data, permissions, hierarchy, settlement setup, channel bindings |
| `debt-service` | Submission and lifecycle of `fordringer`, `restancer`, and transfer to collection |
| `person-registry` | Organization identity and PII-like reference data |

### Debt Collection Workflow (BPMN)

```mermaid
flowchart TD
    Start([Case Created]) --> Assess[Assess Case]
    Assess --> Gateway{Collection Strategy?}

    Gateway -->|VOLUNTARY_PAYMENT| SendReminder[Send Payment Reminder]
    Gateway -->|OFFSETTING| InitOffset[Initiate Offsetting]
    Gateway -->|WAGE_GARNISHMENT| InitGarnish[Initiate Wage Garnishment]

    SendReminder --> Wait30["Wait 30 days<br/>(Timer)"]
    Wait30 --> CheckPay[Check Payment Status]
    CheckPay --> PayGW{Paid?}
    PayGW -->|Yes| Close[Close Case]
    PayGW -->|No| Escalate

    InitOffset --> SendOffNotice[Send Offsetting Notice]
    SendOffNotice --> Wait14["Wait 14 days<br/>(Timer)"]
    Wait14 --> CheckOff[Check Offsetting Result]
    CheckOff --> OffGW{Fully Offset?}
    OffGW -->|Yes| Close
    OffGW -->|No| Escalate

    InitGarnish --> SendGarnNotice[Send Garnishment Notice]
    SendGarnNotice --> Monitor["Monitor Payments<br/>(User Task)"]
    Monitor --> GarnGW{Debt Cleared?}
    GarnGW -->|Yes| Close
    GarnGW -->|No| Monitor

    Monitor -.->|Appeal Signal| Appeal["Handle Appeal<br/>(User Task)"]
    Appeal --> AppealGW{Appeal Upheld?}
    AppealGW -->|Yes| Close
    AppealGW -->|No| Gateway

    Escalate["Review & Escalate<br/>(Supervisor Task)"] --> Gateway

    Close --> SendClosure[Send Closure Notice]
    SendClosure --> End([Case Closed])
```

### Database Architecture

```mermaid
erDiagram
    PERSON_REGISTRY ||--o{ DEBTS : "debtor_person_id"
    PERSON_REGISTRY ||--o{ DEBTS : "creditor_org_id"
    PERSON_REGISTRY ||--o{ CREDITORS : "creditor_org_id"
    PERSON_REGISTRY ||--o{ CASES : "debtor_person_id"
    CREDITORS ||--o{ DEBTS : "creditor_org_id"
    DEBTS ||--o{ CASES : "debt_ids"
    CASES ||--o{ PAYMENTS : "case_id"
    DEBTS ||--o{ PAYMENTS : "debt_id"
    DEBTS ||--o{ LEDGER_ENTRIES : "debt_id"
    DEBTS ||--o{ DEBT_EVENTS : "debt_id"
    DEBTS ||--o{ LIFECYCLE_EVENTS : "debt_id"
    DEBTS ||--o{ HOERING : "debt_id"
    DEBTS ||--o{ NOTIFICATIONS : "debt_id"
    DEBTS ||--o{ LIABILITIES : "debt_id"
    DEBTS ||--o{ OBJECTIONS : "debt_id"
    DEBTS ||--o{ COLLECTION_MEASURES : "debt_id"
    DEBTS ||--o{ INTEREST_JOURNAL : "debt_id"

    PERSON_REGISTRY {
        uuid id PK
        bytea identifier_encrypted
        varchar identifier_type "CPR/CVR"
        varchar identifier_hash "lookup index"
        varchar role "PERSONAL/BUSINESS"
        bytea name_encrypted
        bytea address_encrypted
        bytea email_encrypted
    }

    DEBTS {
        uuid id PK
        uuid debtor_person_id FK
        uuid creditor_org_id FK
        varchar debt_type_code
        numeric principal_amount
        numeric interest_amount
        numeric outstanding_balance
        varchar ocr_line UK
        date due_date
        varchar status
        varchar readiness_status
    }

    CREDITORS {
        uuid id PK
        uuid creditor_org_id FK
        uuid parent_creditor_id FK
        varchar external_creditor_id UK
        varchar activity_status
        varchar connection_type
    }

    CASES {
        uuid id PK
        varchar case_number UK
        uuid debtor_person_id FK
        varchar status
        varchar active_strategy
        varchar assigned_caseworker_id
        numeric total_debt
        numeric total_remaining
    }

    PAYMENTS {
        uuid id PK
        uuid case_id FK
        uuid debt_id FK
        numeric amount
        varchar payment_method
        varchar status
        varchar transaction_reference
        varchar ocr_line
    }

    LEDGER_ENTRIES {
        uuid id PK
        uuid transaction_id
        uuid debt_id FK
        varchar account_code
        varchar entry_type "DEBIT/CREDIT"
        numeric amount
        date effective_date
        date posting_date
        uuid reversal_of_transaction_id
        varchar entry_category
    }

    DEBT_EVENTS {
        uuid id PK
        uuid debt_id FK
        varchar event_type
        date effective_date
        numeric amount
        uuid corrects_event_id FK
        uuid ledger_transaction_id
    }

    CHART_OF_ACCOUNTS {
        varchar code PK
        varchar name
        varchar account_type
    }

    LIFECYCLE_EVENTS {
        uuid id PK
        uuid debt_id FK
        varchar previous_state
        varchar new_state
        uuid recipient_id
        timestamp event_time
    }

    HOERING {
        uuid id PK
        uuid debt_id FK
        varchar hoering_status
        varchar deviation_description
        timestamp sla_deadline
    }

    NOTIFICATIONS {
        uuid id PK
        uuid debt_id FK
        varchar type
        varchar channel
        varchar delivery_state
        varchar ocr_line
    }

    LIABILITIES {
        uuid id PK
        uuid debt_id FK
        uuid debtor_person_id
        varchar liability_type
        numeric share_percentage
        boolean active
    }

    OBJECTIONS {
        uuid id PK
        uuid debt_id FK
        uuid debtor_person_id
        varchar status
        varchar reason
        varchar resolution_note
    }

    COLLECTION_MEASURES {
        uuid id PK
        uuid debt_id FK
        varchar measure_type
        varchar status
        numeric amount
        timestamp completed_at
    }

    INTEREST_JOURNAL {
        uuid id PK
        uuid debt_id FK
        date accrual_date
        numeric balance_snapshot
        numeric rate
        numeric interest_amount
    }

    BATCH_JOB_EXECUTIONS {
        uuid id PK
        varchar job_name
        date execution_date
        varchar status
        int records_processed
        int records_failed
    }
```

### Bookkeeping - Storno and Retroactive Correction Flow

```mermaid
sequenceDiagram
    participant Court as Fogedret
    participant API as API
    participant Correction as RetroactiveCorrectionService
    participant Events as DebtEventRepository
    participant Ledger as LedgerEntryRepository
    participant Interest as InterestAccrualService

    Court->>API: Udlaeg reduced 50k to 30k<br/>effective 3 months ago
    API->>Correction: applyRetroactiveCorrection()

    Correction->>Events: Record UDLAEG_CORRECTED event<br/>amount=-20k, effective=3mo ago

    Note over Correction,Ledger: 1. Post principal correction
    Correction->>Ledger: DEBIT 3000 Indrivelsesindtaegter 20k
    Correction->>Ledger: CREDIT 1000 Fordringer 20k

    Note over Correction,Ledger: 2. Find and storno old interest
    Correction->>Ledger: findInterestAccrualsAfterDate()
    Ledger-->>Correction: 3 months of interest entries (500kr)

    loop Each affected transaction
        Correction->>Ledger: Post STORNO entries<br/>(reverse DEBIT/CREDIT)
    end

    Note over Correction,Interest: 3. Recalculate interest
    Correction->>Interest: calculatePeriodicInterest()
    Interest->>Events: findPrincipalAffectingEvents()
    Events-->>Interest: Balance timeline with correction
    Interest-->>Correction: New periods (300kr total)

    Note over Correction,Ledger: 4. Post recalculated interest
    loop Each new period
        Correction->>Ledger: DEBIT 1100 Renter 
        Correction->>Ledger: CREDIT 3100 Renteindtaegter
    end

    Correction-->>API: CorrectionResult<br/>delta=-200kr interest
```

### OCR-Based Payment Matching Flow (Petition 001)

```mermaid
sequenceDiagram
    participant SKB as SKB/CREMUL
    participant IG as integration-gateway
    participant PS as payment-service
    participant DS as debt-service
    participant RE as rules-engine

    SKB->>IG: CREMUL file
    IG->>IG: Parse CREMUL → CreditAdvice
    IG->>PS: POST /api/v1/payments/incoming<br/>(ocrLine, amount, valueDate, cremulRef)

    PS->>DS: GET /api/v1/debts/by-ocr?ocrLine=OCR-123
    DS-->>PS: [matched debt] or []

    alt Unique OCR match
        PS->>PS: Create COMPLETED PaymentEntity
        PS->>PS: Record bookkeeping (CREMUL)
        PS->>DS: POST /api/v1/debts/{id}/write-down?amount=600
        DS-->>PS: Updated debt

        opt Overpayment (paid > outstanding)
            PS->>RE: Resolve overpayment outcome<br/>(sagstype, frivillig indbetaling)
            RE-->>PS: PAYOUT or COVER_OTHER_DEBTS
        end

        PS-->>IG: PaymentMatchResult (autoMatched=true)
    else No unique match
        PS->>PS: Create PENDING PaymentEntity
        PS-->>IG: PaymentMatchResult (routedToManualMatching=true)
    end
```

### Communication Pattern (ADR-0019)

```mermaid
flowchart LR
    subgraph Orchestrator
        CS["case-service<br/>(Flowable BPMN)"]
    end

    CS -->|REST| DS[debt-service]
    CS -->|REST| RE[rules-engine]
    CS -->|REST| PS[payment-service]
    CS -->|REST| LS[letter-service]
    CS -->|REST| OS[offsetting-service]
    CS -->|REST| WG[wage-garnishment-service]
    CS -->|REST| IG[integration-gateway]

    IG -->|File poll| SKB[(SKB<br/>CREMUL/DEBMUL)]
    IG -->|REST| DUPLA[(DUPLA)]
    IG -->|REST| DP[(Digital Post)]
    IG -->|REST| CRS[creditor-service]

    DS -->|REST| RE
    DS -->|REST| PR[person-registry]
    DS -->|REST| CRS
```

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.3.0 |
| Database | PostgreSQL | 16 |
| Auth | Keycloak (OAuth2/OIDC) | 24.0 |
| Workflow | Flowable BPMN | 7.0.1 |
| Rules | Drools | 9.44.0 |
| EDIFACT | Smooks EDI Cartridge | 2.0.1 |
| Bookkeeping | double-entry-bookkeeping-api | 4.3.0 |
| Build | Maven | 3.9+ |
| Deployment | Kubernetes | - |
| Mapping | MapStruct | 1.5.5 |
| Code style | Spotless (Google Java Format) | 2.43.0 |
| Coverage | JaCoCo (80% line, 70% branch) | 0.8.12 |
| Portal templates | Thymeleaf | 3 (Spring Boot managed) |
| Progressive enhancement | HTMX | 2.x |
| Design system | DKFDS (Det Fælles Designsystem) | 11.x |
| Tracing | Micrometer Tracing + OpenTelemetry OTLP | Spring Boot managed |
| Structured logging | Logstash Logback Encoder | 8.0 |
| Metrics backend | Prometheus | latest (Docker) |
| Trace backend | Grafana Tempo | latest (Docker) |
| Log backend | Grafana Loki | latest (Docker) |
| Dashboards | Grafana OSS | latest (Docker) |
| Telemetry relay | OpenTelemetry Collector Contrib | latest (Docker) |

## Services

### person-registry (Port 8090)

**Purpose:** Centralized GDPR vault. The ONLY service that stores PII (CPR, CVR, names, addresses, email, phone). All other services reference persons by UUID only.

**Implementation status:** IMPLEMENTED

| Component | Status | Notes |
|-----------|--------|-------|
| PersonEntity (encrypted PII) | Done | AES encryption, hash-based lookup |
| OrganizationEntity | Done | CVR-based organizations |
| PersonController (REST API) | Done | lookup, CRUD, GDPR export/erase |
| EncryptionService | Done | Field-level encryption |
| PersonService | Done | Business logic |
| PersonRepository | Done | JPA + hash index for lookups |
| Flyway migration V1 | Done | Persons, organizations, audit |
| GDPR soft-delete | Done | `markAsDeleted()` clears encrypted fields |

**API endpoints (petition023 + existing):**
- `POST /api/v1/persons/lookup` - Lookup or create person by CPR (primary API for citizen auth and M2M ingress; returns UUID only, no PII in response)
- `GET /api/v1/persons/{id}` - Get person details (PII)
- `PUT /api/v1/persons/{id}` - Update person
- `GET /api/v1/persons/{id}/exists` - Existence check (no PII returned)
- `POST /api/v1/persons/{id}/gdpr/export` - GDPR data export
- `DELETE /api/v1/persons/{id}/gdpr/erase` - GDPR erasure request

---

### debt-service (Port 8082)

**Purpose:** Debt registration, lifecycle management, and downstream collection model for `fordringer`, `restancer`, readiness validation, notifications, liabilities, objections, collection measures, and batch processing.

**Implementation status:** IMPLEMENTED (88 Java files, 7 migrations)

| Component | Status | Notes |
|-----------|--------|-------|
| **Core entities** | | |
| DebtEntity | Done | Principal, interest, fees, status, readiness, ocrLine, outstandingBalance, PSRM stamdata fields, lifecycleState |
| DebtTypeEntity | Done | ~600 debt types with legal basis |
| ClaimLifecycleState | Done | Enum: REGISTERED, RESTANCE, HOERING, OVERDRAGET, TILBAGEKALDT, AFSKREVET, INDFRIET |
| ClaimLifecycleEvent | Done | Immutable audit of lifecycle state transitions with recipientId |
| ClaimArtEnum, ClaimCategory | Done | PSRM claim art and category enums |
| InterestSelectionEmbeddable | Done | Embeddable interest configuration |
| **Lifecycle (petition003)** | | |
| ClaimLifecycleService / Impl | Done | evaluateClaimState, transferForCollection, transitionToHearing, resolveHearing, withdraw, writeOff, markFullyPaid |
| **Hearing (petition003)** | | |
| HoeringEntity / HoeringStatus | Done | Hearing workflow with SLA deadline tracking |
| HoeringService / Impl | Done | Create, approve, reject, withdraw hearings |
| HoeringRepository | Done | JPA with status + SLA deadline queries |
| **Notifications (petition004)** | | |
| NotificationEntity | Done | Type (PAAKRAV/RYKKER/...), channel, deliveryState, ocrLine |
| NotificationService / Impl | Done | issueDemandForPayment, issueReminder, getNotificationHistory |
| NotificationController | Done | REST API for notification operations |
| **Liabilities (petition005)** | | |
| LiabilityEntity | Done | SOLE, JOINT_AND_SEVERAL, PROPORTIONAL with sharePercentage |
| LiabilityService / Impl | Done | Add/remove/query liabilities with validation |
| LiabilityController | Done | REST API for liability management |
| **Objections (petition006)** | | |
| ObjectionEntity | Done | ACTIVE, UPHELD, REJECTED with resolution notes |
| ObjectionService / Impl | Done | Register, resolve, hasActiveObjection, collection blocking |
| ObjectionController | Done | REST API for objection management |
| **Collection measures (petition007)** | | |
| CollectionMeasureEntity | Done | SET_OFF, WAGE_GARNISHMENT, ATTACHMENT with status tracking |
| CollectionMeasureService / Impl | Done | Initiate, complete, cancel measures (requires OVERDRAGET state) |
| CollectionMeasureController | Done | REST API for collection measures |
| **Citizen debt (petition024)** | | |
| CitizenDebtController | Done | Citizen-facing debt summary (person_id from JWT) |
| CitizenDebtService / Impl | Done | Pagination, status filter, no PII/creditor internals |
| **Batch processing (petition043)** | | |
| BatchJobExecutionEntity | Done | Job execution tracking for idempotency and audit |
| InterestJournalEntry | Done | Storno-compatible interest journal with balance snapshot |
| RestanceTransitionJob | Done | Daily REGISTERED->RESTANCE for expired payment deadlines |
| InterestAccrualJob | Done | Daily inddrivelsesrente at 5.75% for OVERDRAGET claims |
| DeadlineMonitoringJob | Done | Daily forældelsesfrist and hearing SLA monitoring |
| **Foundation** | | |
| DebtController | Done | Full CRUD + readiness + lifecycle + submit |
| DebtService / Impl | Done | Full CRUD + findByOcrLine + writeDown |
| ReadinessValidationService | Done | Calls rules-engine for evaluation |
| ClaimValidationService / Impl | Done | Drools-based claim validation |
| ClaimSubmissionService / Impl | Done | End-to-end claim submission flow |
| KvitteringService / Impl | Done | PSRM kvittering response (UDFOERT/AFVIST/HOERING) |
| ZeroClaimService / Impl | Done | 0-fordring pattern for interest claims |
| DebtRepository | Done | JPA with batch queries for lifecycle and interest |
| FordringMetrics | Done | `fordring_submissions_total` counter (ADR-0024) |
| logback-spring.xml | Done | Structured JSON logging with traceId/spanId (ADR-0024) |
| Flyway V1 | Done | Baseline: debts, debt_types, claim_lifecycle_events, hoering, audit |
| Flyway V2 | Done | Demo seed data |
| Flyway V3 | Done | Notifications table |
| Flyway V4 | Done | Liabilities table |
| Flyway V5 | Done | Objections table |
| Flyway V6 | Done | Collection measures table |
| Flyway V7 | Done | Batch job executions + interest journal entries |

**API endpoints (28):**
- `GET/POST /api/v1/debts` - List/create debts
- `GET/PUT /api/v1/debts/{id}` - Get/update debt
- `POST /api/v1/debts/submit` - Submit claim (full validation + kvittering)
- `GET /api/v1/debts/debtor/{debtorId}` - Debts by debtor
- `GET /api/v1/debts/by-ocr?ocrLine={ocrLine}` - Find debts by OCR-linje
- `POST /api/v1/debts/{id}/validate-readiness` - Validate indrivelsesparathed
- `POST /api/v1/debts/{id}/approve-readiness` - Manual approval
- `POST /api/v1/debts/{id}/reject-readiness` - Rejection with reason
- `POST /api/v1/debts/{id}/evaluate-state` - Evaluate lifecycle state
- `POST /api/v1/debts/{id}/transfer-for-collection` - Transfer for collection (RESTANCE->OVERDRAGET)
- `POST /api/v1/debts/{id}/write-down?amount={amount}` - Write down outstanding balance
- `DELETE /api/v1/debts/{id}` - Cancel (soft delete)
- `POST /api/v1/debts/{debtId}/demand-for-payment` - Issue paakrav notification
- `POST /api/v1/debts/{debtId}/reminder` - Issue rykker notification
- `GET /api/v1/debts/{debtId}/notifications` - Notification history
- `POST /api/v1/debts/{debtId}/liabilities` - Add liability
- `DELETE /api/v1/debts/{debtId}/liabilities/{id}` - Remove liability
- `GET /api/v1/debts/{debtId}/liabilities` - List liabilities
- `POST /api/v1/debts/{debtId}/objections` - Register objection
- `PUT /api/v1/debts/{debtId}/objections/{id}/resolve` - Resolve objection
- `GET /api/v1/debts/{debtId}/objections` - List objections
- `POST /api/v1/debts/{debtId}/collection-measures` - Initiate measure
- `POST /api/v1/debts/{debtId}/collection-measures/{id}/complete` - Complete measure
- `POST /api/v1/debts/{debtId}/collection-measures/{id}/cancel` - Cancel measure
- `GET /api/v1/debts/{debtId}/collection-measures` - List measures
- `GET /api/v1/citizen/debts` - Citizen debt summary (CITIZEN role, person_id from JWT)

---

### case-service (Port 8081)

**Purpose:** Case management, workflow orchestration via Flowable BPMN, caseworker assignment.

**Implementation status:** IMPLEMENTED

| Component | Status | Notes |
|-----------|--------|-------|
| CaseEntity | Done | Status, strategy, debts, assignment |
| CaseController | Done | Full CRUD + assign + strategy + close |
| CaseService | Done | Interface defined |
| CaseRepository | Done | JPA |
| CaseWorkflowService | Done | Flowable start/complete/signal/cancel |
| CaseWorkflowServiceImpl | Done | Full Flowable RuntimeService integration |
| debt-collection-case.bpmn20.xml | Done | 3 paths: voluntary, offsetting, garnishment |
| CaseAssessmentDelegate | Done | Auto-assessment at case start |
| SendLetterDelegate | Done | Letter service integration (TODO: wire client) |
| CheckPaymentDelegate | Done | Payment check (TODO: wire client) |
| CloseCaseDelegate | Done | Case closure |
| Flyway migration V1 | Done | Cases, case_debt_ids, audit, history |

**Workflow paths (BPMN):**
1. **Voluntary Payment:** Send reminder -> wait 30 days -> check payment -> paid/escalate
2. **Offsetting (Modregning):** Initiate -> send notice -> wait 14 days -> check result -> paid/escalate
3. **Wage Garnishment (Loenindeholdelse):** Initiate -> send notice -> monitor (user task) -> cleared/continue

**API endpoints:**
- `GET/POST /api/v1/cases` - List/create cases
- `GET/PUT /api/v1/cases/{id}` - Get/update case
- `GET /api/v1/cases/debtor/{debtorId}` - Cases by debtor
- `POST /api/v1/cases/{id}/assign` - Assign caseworker
- `POST /api/v1/cases/{id}/strategy` - Set collection strategy
- `POST /api/v1/cases/{id}/close` - Close case

---

### payment-service (Port 8083)

**Purpose:** Payment processing, reconciliation, double-entry bookkeeping, retroactive corrections.

**Implementation status:** PARTIALLY IMPLEMENTED

| Component | Status | Notes |
|-----------|--------|-------|
| PaymentEntity | Done | Amount, method, status, transaction ref |
| Flyway V1 (payments) | Done | Payments, audit, history |
| **Bookkeeping module** | **Done** | |
| AccountCode (kontoplan) | Done | 7 accounts aligned with statsligt regnskab |
| LedgerEntryEntity (bi-temporal) | Done | effective_date + posting_date + storno |
| DebtEventEntity (timeline) | Done | Immutable event log for replay |
| BookkeepingService | Done | 6 operations with bi-temporal dates |
| BookkeepingServiceImpl | Done | Double-entry posting + event recording |
| InterestAccrualService | Done | Period-based interest calculation |
| RetroactiveCorrectionService | Done | Storno + recalculate + re-post |
| LedgerEntryRepository | Done | Queries for storno, interest, active entries |
| DebtEventRepository | Done | Timeline queries, principal-affecting events |
| Flyway V2 (ledger) | Done | ledger_entries, debt_events, chart_of_accounts, views |
| **Unit tests** | **Done** | BookkeepingServiceImplTest, RetroactiveCorrectionServiceImplTest, InterestAccrualServiceImplTest |
| **Payment matching module** | **Done** | |
| IncomingPaymentDto | Done | DTO for incoming CREMUL payments |
| PaymentMatchResult | Done | Result of OCR-based matching |
| OverpaymentOutcome | Done | Enum: PAYOUT, COVER_OTHER_DEBTS |
| PaymentMatchingService | Done | Interface for OCR matching logic |
| PaymentMatchingServiceImpl | Done | Auto-match, write-down, overpayment rules |
| OverpaymentRulesService | Done | Interface for rule-driven overpayment handling |
| OverpaymentRulesServiceImpl | Done | Placeholder (defaults to PAYOUT, Drools rules TBD) |
| DebtServiceClient | Done | REST client for debt-service (ADR-0007) |
| PaymentRepository | Done | JPA repository for PaymentEntity |
| PaymentController | Done | REST API for incoming payment processing |
| Flyway V3 (ocr_line, nullable case) | Done | OCR-linje column, nullable case_id/debtor fields |
| **Unit tests** | **Done** | PaymentMatchingServiceImplTest (10 tests) |
| ReconciliationService | Not started | Match CREMUL entries against ledger |

**API endpoints:**
- `POST /api/v1/payments/incoming` - Process incoming CREMUL payment (OCR-based matching)

---

### rules-engine (Port 8091)

**Purpose:** Centralized business rules evaluation using Drools. Called by other services via REST.

**Implementation status:** IMPLEMENTED

| Component | Status | Notes |
|-----------|--------|-------|
| DroolsConfig | Done | Auto-loads .drl and .xlsx from classpath |
| RulesService / RulesServiceImpl | Done | evaluateReadiness, calculateInterest, determinePriority |
| FordringValidationService / Impl | Done | Fordring action validation (petition015-018) |
| RulesController | Done | REST API for all rule types |
| debt-readiness.drl | Done | 9 rules including manual review triggers |
| interest-calculation.drl | Done | Standard rate, small amount exempt, not-due |
| collection-priority.drl | Done | 5 priority levels (child support > tax > fines > court > other) |
| fordring-validation.drl | Done | 114 rules: 23 core (petition015) + 14 authorization (petition016) + 32 lifecycle/reference (petition017) + 45 content (petition018) |
| Flyway migration V1 | Done | Rules audit tables |

**Fordring Validation Rules (petition015-018):**
| Category | Rules | Error Codes |
|----------|-------|-------------|
| Structure validation | 10 | 403, 404, 406, 407, 412, 444, 447, 448, 458, 505 |
| Currency validation | 1 | 152 |
| Art type validation | 1 | 411 |
| Interest rate validation | 1 | 438 |
| Date validation | 6 | 409, 464, 467, 548, 568, 569 |
| Agreement validation | 3 | 2, 151, 156 |
| Debtor validation | 1 | 5 |
| Authorization validation | 14 | 400, 416, 419, 420, 421, 437, 465, 466, 480, 497, 501, 508, 511, 543 |
| Genindsend validation | 5 | 539, 540, 541, 542, 544 |
| Tilbagekald validation | 5 | 434, 538, 546, 547, 570 |
| Action reference validation | 5 | 418, 429, 526, 527, 530 |
| Opskrivning/Nedskrivning | 13 | 469, 470, 471, 473, 474, 477, 493, 494, 502, 503, 504, 506 |
| State validation | 4 | 428, 488, 496, 498 |
| Document/Note validation | 6 | 164, 181, 220, 413, 415, 516 |
| Claim amount validation | 5 | 201, 215, 227, 408, 425 |
| Sub-claim validation | 4 | 270, 423, 459, 461 |
| Interest validation | 4 | 436, 441, 442, 443 |
| Nedskriv reason validation | 4 | 410, 433, 519, 571 |
| Hovedstol validation | 4 | 510, 512, 517, 518 |
| Hæftelse validation | 6 | 528, 531, 532, 533, 557, 559 |
| Routing validation | 4 | 422, 426, 565, 572 |
| Claim type validation | 5 | 509, 537, 550, 574, 575 |
| Identifier validation | 3 | 486, 602, 603 |

**API endpoints:**
- `POST /api/v1/rules/readiness/evaluate` - Debt readiness check
- `POST /api/v1/rules/interest/calculate` - Interest calculation
- `POST /api/v1/rules/priority/evaluate` - Collection priority
- `POST /api/v1/rules/priority/sort` - Sort debts by priority

---

### integration-gateway (Port 8089)

**Purpose:** External system integration via DUPLA. Owns M2M ingress for creditor systems (petition011) and SKB CREMUL/DEBMUL processing.

**Implementation status:** PARTIALLY IMPLEMENTED (22 Java files)

| Component | Status | Notes |
|-----------|--------|-------|
| IntegrationGatewayApplication | Done | Spring Boot app |
| WebClientConfig | Done | WebClient.Builder bean (ADR-0024 trace propagation) |
| **Creditor M2M ingress (petition011)** | **Done** | |
| CreditorM2mController | Done | `POST /api/v1/creditor/m2m/claims/submit` |
| CreditorIngressService / Impl | Done | Orchestrates access resolution + claim forwarding |
| CreditorServiceClient | Done | REST client for creditor-service (access resolution) |
| DebtServiceClient | Done | REST client for debt-service (claim submission) |
| ClaimSubmissionRequest / Result | Done | DTOs for M2M claim flow |
| GatewayClaimResponse / GatewayErrorResponse | Done | Response DTOs |
| AccessResolutionRequest / Response | Done | Channel binding resolution DTOs |
| CreditorAction / ChannelType | Done | Enums |
| **SKB adapter** | **Done** | |
| CreditAdvice / DebitAdvice / SkbMessage | Done | EDIFACT model classes |
| SkbEdifactService / Impl | Done | Smooks-based CREMUL parsing, DEBMUL generation |
| SkbController | Done | REST API for CREMUL upload + DEBMUL download |
| cremul-config.xml (Smooks) | Done | Template (TODO: map to SKB directory version) |
| **Unit tests** | **Done** | CreditorM2mControllerTest, CreditorIngressServiceImplTest, SkbEdifactServiceImplTest, 3 BDD scenarios |
| DUPLA client | Not started | OCES3 certificate integration |
| CPR/CVR register client | Not started | External lookups |
| Digital Post client | Not started | Letter delivery |
| File polling for CREMUL | Not started | Scheduled pickup from SKB directory |

**API endpoints:**
- `POST /api/v1/creditor/m2m/claims/submit` - M2M claim submission (creditor access resolution + forwarding to debt-service)
- `POST /api/v1/skb/cremul/parse` - Upload and parse CREMUL file
- `POST /api/v1/skb/debmul/generate` - Generate DEBMUL file

---

### creditor-service (Port 8092)

**Purpose:** Operational `fordringshaver` master data service. Owns non-PII creditor configuration, hierarchy, permissions, settlement setup, and channel access resolution.

**Implementation status:** IMPLEMENTED

| Component | Status | Notes |
|-----------|--------|-------|
| CreditorEntity / model | Done | Implements Petition 008 operational data model |
| ChannelBindingEntity / model | Done | Binds M2M and portal identities to creditors |
| CreditorController | Done | Lookup and action-validation APIs |
| AccessResolutionController | Done | POST /api/v1/creditors/access/resolve (petition010) |
| CreditorService / CreditorServiceImpl | Done | Creditor lookup, hierarchy, action validation |
| ChannelBindingService / ChannelBindingServiceImpl | Done | Channel binding CRUD, access resolution |
| CreditorMapper / ChannelBindingMapper | Done | MapStruct mappers |
| CreditorRepository | Done | JPA with filtering indexes |
| ChannelBindingRepository | Done | JPA repository |
| SecurityConfig | Done | OAuth2 resource server, actuator/swagger permitted |
| AccessResolutionMetrics | Done | `creditor_access_resolution_total` counter (tags: result=allowed/denied) |
| logback-spring.xml | Done | Structured JSON logging with traceId/spanId (ADR-0024) |
| Flyway migration V1 | Done | `creditors`, audit, history |
| Flyway migration V2 | Done | `channel_bindings`, audit, history |

**API endpoints:**
- `GET /api/v1/creditors/{creditorOrgId}` - Resolve creditor master data by organization reference
- `GET /api/v1/creditors/by-external-id/{externalCreditorId}` - Resolve creditor by legacy external ID
- `POST /api/v1/creditors/{creditorOrgId}/validate-action` - Validate creditor status and permissions
- `POST /api/v1/creditors/access/resolve` - Resolve acting and represented creditor for a channel request (petition010)

---

### letter-service (Port 8084)

**Purpose:** Letter generation and delivery via Digital Post.

**Implementation status:** SCAFFOLD ONLY

| Component | Status | Notes |
|-----------|--------|-------|
| LetterServiceApplication | Done | Spring Boot app |
| Flyway migration V1 | Done | Letters, templates, audit |
| LetterController | Not started | |
| LetterService | Not started | |
| Digital Post integration | Not started | Via integration-gateway |

---

### offsetting-service (Port 8087)

**Purpose:** Modregning (offsetting) processing.

**Implementation status:** SCAFFOLD ONLY

| Component | Status | Notes |
|-----------|--------|-------|
| OffsettingServiceApplication | Done | Spring Boot app |
| application.yml | Done | Priority rules config |
| Controllers/Services | Not started | |

---

### wage-garnishment-service (Port 8088)

**Purpose:** Loenindeholdelse (wage garnishment) processing.

**Implementation status:** SCAFFOLD ONLY

| Component | Status | Notes |
|-----------|--------|-------|
| WageGarnishmentServiceApplication | Done | Spring Boot app |
| application.yml | Done | |
| Controllers/Services | Not started | |

---

### creditor-portal (Port 8085)

**Purpose:** UI/BFF for fordringshavere (creditors) to view data and perform manual interactions. It is not the system of record for creditor master data and not the primary M2M entry point.

**Accessibility requirement:** Must comply with ADR-0021 and the applicable requirements from EN 301 549 / WCAG 2.1 AA, and must have its own accessibility statement.

**Implementation status:** PARTIALLY IMPLEMENTED

| Component | Status | Notes |
|-----------|--------|-------|
| CreditorPortalApplication | Done | Spring Boot app |
| application.yml | Done | References debt-service, case-service, creditor-service |
| SKAT design tokens CSS | Done | `static/css/skat-tokens.css` — color palette, spacing, typography tokens from skat.dk design language (ADR-0023) |
| WebClientConfig | Done | WebClient.Builder bean with JSON defaults |
| CreditorServiceClient | Done | REST client for creditor-service (getByCreditorOrgId, resolveAccess) |
| DebtServiceClient | Done | REST client for debt-service (listDebts, createDebt) |
| CaseServiceClient | Done | REST client for case-service (listCases) |
| Portal DTOs | Done | PortalCreditorDto, PortalDebtDto, PortalCaseDto, FordringFormDto, AccessResolutionRequest/Response, RestPage |
| FordringMapper | Done | Maps FordringFormDto + creditorOrgId → PortalDebtDto for DebtServiceClient |
| PortalSessionService | Done | Resolves acting creditor for session; acting-on-behalf-of enforcement via CreditorServiceClient.resolveAccess(); session caching; graceful fallback |
| AccessibilityController | Done | `GET /was` → accessibility statement page (petition014) |
| Accessibility statement | Done | `templates/was.html` — Danish WCAG 2.1 AA statement with contact info, enforcement link, last-updated date |
| Form-field fragment | Done | `templates/fragments/form-field.html` — reusable accessible form pattern with aria-describedby |
| Focus management JS | Done | `static/js/a11y.js` — HTMX afterSwap focus management for screen readers |
| **Unit tests** | **Done** | 39 unit tests across 9 test classes |
| **BDD acceptance tests** | **Done** | 9 Cucumber scenarios: petition012 (3), petition013 (3), petition014 (3) |
| SecurityConfig (permissive) | Done | Permits all requests for dev/demo without Keycloak (ADR-0023 browser flow TBD) |
| logback-spring.xml | Done | Structured JSON logging with traceId/spanId (ADR-0024) |
| Thymeleaf layout + HTMX | Done | `spring-boot-starter-thymeleaf`, `thymeleaf-layout-dialect`, `htmx.org` WebJar (ADR-0023) |
| SKAT layout template | Done | `templates/layout/default.html` — skip link, header, breadcrumb, main, footer with `/was` link |
| DashboardController | Done | `GET /` → index.html with creditor profile card, shortcuts, acting-on-behalf-of selector (`?actAs=`); graceful fallback when backend unavailable |
| FordringController | Done | `GET /fordring/ny` → fordring-ny.html form; `POST /fordring/ny` → validate + submit to DebtServiceClient; success redirect with flash; backend error display |
| PortalPagesController | Done | `GET /fordringer`, `GET /sager` → placeholder pages |
| Dashboard template | Done | SKAT-style creditor profile card (status badge), shortcuts to fordring/sager, acting-on-behalf-of selector/indicator |
| Fordring form template | Done | `templates/fordring-ny.html` — accessible form with skat-tokens.css, Bean Validation error display, breadcrumb |
| Fordringer placeholder | Done | `templates/fordringer.html` — "Dine fordringer" with placeholder table, success flash alert |
| Sager placeholder | Done | `templates/sager.html` — "Dine sager" with placeholder table |
| Error page | Done | `templates/error.html` extending SKAT layout |

---

### citizen-portal (Port 8086)

**Purpose:** UI/BFF for borgere (citizens) to view debts and make payments. TastSelv/MitID integration via OAuth2/OIDC.

**Accessibility requirement:** Must comply with ADR-0021 and the applicable requirements from EN 301 549 / WCAG 2.1 AA, and must have its own accessibility statement.

**Implementation status:** PARTIALLY IMPLEMENTED (15 Java files)

| Component | Status | Notes |
|-----------|--------|-------|
| CitizenPortalApplication | Done | Spring Boot app |
| application.yml | Done | OAuth2, person-registry, i18n config |
| **Landing page (petition022)** | **Done** | |
| LandingPageController | Done | `GET /` landing page with FAQ |
| AccessibilityController | Done | `GET /was` accessibility statement |
| I18nConfig / I18nProperties / I18nModelAdvice | Done | i18n infrastructure with locale switching |
| CitizenLinksProperties / CitizenLinksModelAdvice | Done | Portal link configuration |
| Thymeleaf templates | Done | index.html, was.html, layout/default.html, language-selector fragment |
| messages_da.properties / messages_en_GB.properties | Done | 72 i18n keys per locale |
| SecurityConfig (public pages) | Done | Permits landing, /was, static resources; requires auth elsewhere |
| **MitID/TastSelv auth (petition025)** | **Done** | |
| SecurityConfig (OAuth2) | Done | `.oauth2Login()` with tastselv client registration |
| CitizenOidcUserService / CitizenOidcUser | Done | Custom OIDC user with CPR extraction and person_id resolution |
| CitizenAuthProperties | Done | Configurable CPR claim name |
| PersonRegistryClient | Done | REST client for person-registry CPR lookup (WebClient.Builder, ADR-0024) |
| WebClientConfig | Done | WebClient.Builder bean with JSON defaults |
| DashboardController | Done | `GET /dashboard` for authenticated citizens |
| **Tests** | **Done** | LandingPageControllerTest, DashboardControllerTest, SecurityConfigTest, CitizenOidcUserServiceTest, PersonRegistryClientTest, ArchitectureTest |

**Page routes:**
- `GET /` - Landing page (public)
- `GET /was` - Accessibility statement (public)
- `GET /dashboard` - Authenticated citizen dashboard

---

### opendebt-common (Shared library)

**Purpose:** Shared DTOs, exceptions, audit infrastructure, and CLS integration.

**Implementation status:** IMPLEMENTED

| Component | Status | Notes |
|-----------|--------|-------|
| CaseDto, DebtDto, PaymentDto, LetterDto | Done | Shared DTOs |
| DebtorIdentifier | Done | CPR/CVR identifier model |
| ErrorResponse | Done | Standard error format |
| OpenDebtException | Done | Base exception with error code + severity |
| GlobalExceptionHandler | Done | @ControllerAdvice |
| AuditableEntity | Done | @MappedSuperclass with createdAt/updatedAt/createdBy/updatedBy/version |
| AuditingConfig | Done | JPA auditing with security context integration |
| AuditContextFilter | Done | Extracts user context for audit |
| AuditContextService | Done | Sets PostgreSQL audit context |
| ClsAuditClient | Done | Interface for CLS event shipping (fallback) |
| ClsAuditClientImpl | Done | Async batched CLS client with retry (fallback) |
| NoOpClsAuditClient | Done | No-op client for dev/test |
| ClsAuditEventMapper | Done | Maps audit records to CLS format with PII masking |
| **Filebeat config** | Done | `config/filebeat/filebeat-audit.yml` - recommended CLS integration |
| FordringValidationService | Done | 114 Drools rules for fordring validation |

## Database Architecture

Each service owns its own PostgreSQL database (no cross-service DB access, ADR-0007):

| Database | Service | Key Tables |
|----------|---------|------------|
| opendebt_person | person-registry | persons, organizations |
| opendebt_case | case-service | cases, case_debt_ids, ACT_* (Flowable) |
| opendebt_debt | debt-service | debts, debt_types, claim_lifecycle_events, hoering, notifications, liabilities, objections, collection_measures, batch_job_executions, interest_journal_entries |
| opendebt_creditor | creditor-service | creditors, channel_bindings, creditor_permissions |
| opendebt_payment | payment-service | payments, ledger_entries, debt_events, chart_of_accounts |
| opendebt_letter | letter-service | letters, letter_templates |
| opendebt_rules | rules-engine | rule_audit |

All databases include:
- `audit_log` table with trigger-based audit logging
- `*_history` tables for temporal versioning (sys_period)
- UUID primary keys
- Flyway-managed migrations

## Communication Pattern

**Explicit orchestration via Flowable BPMN + synchronous REST** (ADR-0019). No message broker.

See the Communication Pattern diagram above.

## Security Model

- **Authentication:** OAuth2/OIDC via Keycloak (ADR-0005)
- **Authorization:** Role-based (`@PreAuthorize`) per endpoint
- **Roles:** ADMIN, SUPERVISOR, CASEWORKER, CREDITOR, SERVICE, GDPR_OFFICER
- **PII isolation:** All personal data encrypted in person-registry only (ADR-0014)
- **Audit:** PostgreSQL trigger-based audit on all tables

## Accessibility and digital inclusion

- **Compliance baseline:** Public UIs must comply with ADR-0021 and applicable EN 301 549 v3.2.1 requirements; WCAG 2.1 AA is the practical baseline for web UI implementation.
- **Accessibility statements:** Each web site and future mobile application must have its own accessibility statement created in WAS-Tool, updated on material change and at least annually.
- **Engineering expectation:** Keyboard access, visible focus, semantic structure, accessible forms/error handling, sufficient contrast, and accessible documents are mandatory qualities for UI delivery.
- **Operational expectation:** Each UI should expose a discoverable link to its accessibility statement, preferably in the footer and, where practical, via `/was`.

## Deployment

- **Local:** Docker Compose with all services, PostgreSQL 16, Keycloak 24
- **Observability stack (ADR-0024):** Separate `docker-compose.observability.yml` (merged with `-f`), containing OTel Collector, Grafana Tempo, Grafana Loki, Prometheus, and Grafana OSS. All 12 services are instrumented with Micrometer Tracing + OpenTelemetry OTLP export, structured JSON logging (logback-spring.xml with traceId/spanId), and Prometheus metrics. Prometheus scrapes all 12 service `/actuator/prometheus` endpoints. Config files under `config/otel/`, `config/tempo/`, `config/loki/`, `config/prometheus/`, `config/grafana/`.
- **Kubernetes:** Kustomize-based with base + staging/production overlays
  - Namespace: `opendebt`
  - Service discovery via internal DNS
  - ConfigMap for service URLs and JVM options
  - Only case-service has full K8s manifests (deployment + service); other services pending

## OpenAPI Specifications

Pre-defined API specs (API-first, ADR-0004):
- `api-specs/openapi-debt-service.yaml` - Debt management APIs
- `api-specs/openapi-case-service.yaml` - Case management and workflow APIs

## Architecture Decision Records (ADRs)

| ADR | Decision |
|-----|----------|
| 0001 | Record Architecture Decisions |
| 0002 | Microservices Architecture |
| 0003 | Java/Spring Boot Technology Stack |
| 0004 | API-First Design with OpenAPI |
| 0005 | Keycloak Authentication |
| 0006 | Kubernetes Deployment |
| 0007 | No Cross-Service Database Connections |
| 0008 | Letter Management Strategy |
| 0009 | DUPLA Integration |
| 0010 | Faellesoffentlige Arkitekturprincipper Compliance |
| 0011 | PostgreSQL Database |
| 0012 | Debtor Identification Model (CPR/CVR with Role) |
| 0013 | Enterprise PostgreSQL with Audit and History |
| 0014 | GDPR Data Isolation - Person Registry |
| 0015 | Drools Rules Engine |
| 0016 | Flowable Workflow Engine |
| 0017 | Smooks EDIFACT CREMUL/DEBMUL (SKB Integration) |
| 0018 | Double-Entry Bookkeeping (Bi-Temporal with Storno) |
| 0019 | Orchestration over Event-Driven Architecture |
| 0020 | Creditor Channel and Master Data Architecture |
| 0021 | UI Accessibility and Webtilgængelighed Compliance |
| 0022 | Shared Audit Infrastructure |
| 0023 | Creditor Portal Frontend Technology (Thymeleaf + HTMX) |
| 0024 | Observability Backend Stack (Grafana + Prometheus + Loki + Tempo) |
| 0025 | Maven Build Tool |

## Unit Tests

| Test Class | Service | Tests | Coverage |
|------------|---------|-------|----------|
| BookkeepingServiceImplTest | payment-service | 9 | Bi-temporal posting, all 6 operations, event recording |
| RetroactiveCorrectionServiceImplTest | payment-service | 5 | Storno, recalculation, delta verification |
| InterestAccrualServiceImplTest | payment-service | 6 | Period-based calculation, corrections, edge cases |
| PaymentMatchingServiceImplTest | payment-service | 10 | OCR matching, write-down, overpayment rules, manual routing |
| SkbEdifactServiceImplTest | integration-gateway | 6 | CREMUL parsing, DEBMUL generation, UTF-8 |
| DebtServiceImplTest | debt-service | 8 | findByOcrLine, writeDown, balance clamping, status transitions |
| ClaimLifecycleServiceImplTest | debt-service | 20 | Lifecycle transitions, evaluateClaimState, transferForCollection, hearing, withdraw, writeOff |
| NotificationServiceImplTest | debt-service | 8 | Paakrav/rykker creation, OCR line generation, notification history |
| LiabilityServiceImplTest | debt-service | 12 | SOLE/JOINT_AND_SEVERAL/PROPORTIONAL, share validation, deactivation |
| ObjectionServiceImplTest | debt-service | 11 | Register, resolve (UPHELD/REJECTED), hasActiveObjection, collection blocking |
| CollectionMeasureServiceImplTest | debt-service | 11 | SET_OFF/WAGE_GARNISHMENT/ATTACHMENT, state validation, complete, cancel |
| CitizenDebtServiceImplTest | debt-service | 6 | Citizen debt summary, pagination, status filter |
| HoeringServiceImplTest | debt-service | 7 | Create, approve, reject, withdraw hearings |
| KvitteringServiceImplTest | debt-service | 3 | UDFOERT/AFVIST/HOERING kvittering responses |
| ZeroClaimServiceImplTest | debt-service | 9 | 0-fordring pattern for interest references |
| RestanceTransitionJobTest | debt-service | 4 | Batch RESTANCE transition, idempotency, failure handling |
| InterestAccrualJobTest | debt-service | 4 | Interest calculation accuracy, idempotency, skip existing |
| DeadlineMonitoringJobTest | debt-service | 4 | Approaching limitation, expired SLA, idempotency |
| NotificationControllerTest | debt-service | 4 | REST endpoint tests for notification operations |
| LiabilityControllerTest | debt-service | 5 | REST endpoint tests for liability operations |
| ObjectionControllerTest | debt-service | 4 | REST endpoint tests for objection operations |
| CollectionMeasureControllerTest | debt-service | 5 | REST endpoint tests for collection measure operations |
| CitizenDebtControllerTest | debt-service | 6 | REST endpoint tests for citizen debt summary |
| RunCucumberTest (petition002-007,024,043) | debt-service | 59 | BDD scenarios across 8 petitions |
| OverpaymentRulesServiceImplTest | payment-service | 1 | Placeholder default outcome |
| CreditorM2mControllerTest | integration-gateway | 5 | M2M claim submission, validation, error handling |
| CreditorIngressServiceImplTest | integration-gateway | 8 | Access resolution + claim forwarding orchestration |
| RunCucumberTest (petition011) | integration-gateway | 3 | BDD: M2M claim submission, access denied, correlation propagation |
| LandingPageControllerTest | citizen-portal | 3 | Landing page model attributes, FAQ |
| DashboardControllerTest | citizen-portal | 3 | Authenticated dashboard |
| SecurityConfigTest | citizen-portal | 3 | Public vs authenticated page access |
| CitizenOidcUserServiceTest | citizen-portal | 4 | CPR extraction, person_id resolution |
| PersonRegistryClientTest | citizen-portal | 3 | CPR lookup, error handling |
| CreditorServiceClientTest | creditor-portal | 5 | Creditor lookup, access resolution, error handling |
| DebtServiceClientTest | creditor-portal | 5 | Debt listing, creation, validation errors, server errors |
| CaseServiceClientTest | creditor-portal | 2 | Case listing, server error handling |
| DashboardControllerTest | creditor-portal | 6 | Dashboard with creditor profile, acting creditor resolution, fallback on backend unavailable |
| PortalPagesControllerTest | creditor-portal | 2 | Fordringer and sager placeholder view names |
| FordringControllerTest | creditor-portal | 4 | Form display, validation errors, successful submission, backend error handling |
| PortalSessionServiceTest | creditor-portal | 11 | Acting creditor resolution, access allowed/denied, backend fallback, session management |
| FordringMapperTest | creditor-portal | 2 | Full field mapping, null description handling |
| AccessibilityControllerTest | creditor-portal | 1 | Accessibility statement view name |
| RunCucumberTest (petition012) | creditor-portal | 3 | Portal reads creditor profile, manual fordring submission to debt-service, unrelated fordringshaver rejection |
| RunCucumberTest (petition013) | creditor-portal | 3 | Keyboard-only navigation structure, accessible form validation with aria, accessibility release gate |
| RunCucumberTest (petition014) | creditor-portal | 3 | Discoverable accessibility statement link, accessible contact path without MitID, statement maintenance date |
| AccessResolutionMetricsTest | creditor-service | 4 | Counter registration, allowed/denied increments, independence |
| FordringMetricsTest | debt-service | 2 | Counter registration, submission increments |

## Implementation Summary

| Service | Status | Java Files | Endpoints / Routes | DB Migrations |
|---------|--------|-----------|-----------|---------------|
| opendebt-common | Done | 32 | - | - |
| person-registry | Done | 9 | 6 | V1 |
| debt-service | Done | 88 | 28 | V1-V7 |
| case-service | Done | 10 | 8 | V1 |
| rules-engine | Done | 13 | 4 (+ 114 validation rules) | V1 |
| payment-service | Partial | 22 | 1 | V1, V2, V3 |
| integration-gateway | Partial | 22 | 3 | - |
| creditor-service | Done | 35 | 4 | V1, V2 |
| letter-service | Scaffold | 1 | 0 | V1 |
| offsetting-service | Scaffold | 1 | 0 | - |
| wage-garnishment-service | Scaffold | 1 | 0 | - |
| creditor-portal | Partial | 72 | 48 | - |
| citizen-portal | Partial | 15 | 3 | - |
| **Total** | | **321** | **105** | **16** |
