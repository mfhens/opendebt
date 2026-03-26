# Architecture

OpenDebt is a microservices-based debt collection system built on Java 21, Spring Boot 3.3, and PostgreSQL 16.

## System overview

```mermaid
graph TB
    subgraph External["External Systems"]
        TastSelv["TastSelv (MitID)"]
        DigitalPost["Digital Post (e-Boks)"]
        SKB["Statens Koncernbetalinger"]
        ES["Creditor systems"]
    end

    subgraph Gateway["Integration Layer"]
        IG["integration-gateway :8089"]
    end

    subgraph Portals["User Portals"]
        CP["creditor-portal :8085"]
        BP["citizen-portal :8086"]
        CW["caseworker-portal :8093"]
    end

    subgraph Core["Core Services"]
        DS["debt-service :8082"]
        CS["case-service :8081"]
        PS["payment-service :8083"]
        CRS["creditor-service :8092"]
    end

    subgraph Foundation["Foundation Services"]
        RE["rules-engine :8091"]
        PR["person-registry :8090"]
    end

    ES --> IG
    SKB --> IG
    IG --> DS
    IG --> CRS
    IG --> PS

    subgraph LegacySoap["Legacy SOAP"]
        OIO["EFI/DMI systems<br/>(OIO protocol)"]
        SKAT2["SKAT systems<br/>(SKAT protocol)"]
    end
    OIO --> IG
    SKAT2 --> IG

    CP --> DS
    CP --> CRS
    CP --> PS
    BP --> DS
    BP --> CS
    BP --> PS
    CW --> DS
    CW --> CS
    CW --> PS

    DS --> RE
    DS --> CRS
    CS --> DS
```

## Service inventory

| Service | Port | Responsibility |
|---------|------|----------------|
| debt-service | 8082 | Claim registration, lifecycle management, validation |
| case-service | 8081 | Case management with Flowable BPMN workflows |
| payment-service | 8083 | Payment matching (OCR), bookkeeping (double-entry), debt event log, immudb tamper-evidence (ADR-0029) |
| creditor-service | 8092 | Creditor master data, channel binding, access resolution |
| person-registry | 8090 | GDPR vault for personal data (CPR/CVR encryption) |
| rules-engine | 8091 | Drools-based validation rules |
| integration-gateway | 8089 | DUPLA, SKB CREMUL/DEBMUL, M2M creditor ingress, legacy SOAP (OIO/SKAT, petition019) |
| creditor-portal | 8085 | Fordringshaver web portal (Thymeleaf + HTMX); timeline at `/fordring/{id}/tidslinje` |
| citizen-portal | 8086 | Skyldner web portal (Thymeleaf + HTMX); case detail + timeline at `/cases/{id}/tidslinje` |
| caseworker-portal | 8093 | Sagsbehandler web portal; unified timeline at `/cases/{id}/tidslinje` |
| letter-service | 8084 | Digital Post integration |
| offsetting-service | 8087 | Modregning (set-off) |
| wage-garnishment-service | 8088 | Loenindeholdelse (wage garnishment) |
| opendebt-common | JAR | Shared library: audit infrastructure, DTOs, timeline components (petition050) |
| **immudb** | **3322 (gRPC)** | **Cryptographic tamper-evidence KV store for financial ledger entries (ADR-0029)** |

## Technology stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL 16 |
| Authentication | Keycloak (OAuth2/OIDC) |
| Rules engine | Drools |
| Workflow engine | Flowable BPMN |
| Tamper-evidence ledger | immudb 1.10 + immudb4j 1.0.1 |
| API gateway | DUPLA (external), integration-gateway (internal) |
| Frontend | Thymeleaf + HTMX |
| Observability | Grafana + Prometheus + Loki + Tempo |
| Deployment | Kubernetes |
| Build | Maven |

## Data flow: Legacy SOAP claim submission (petition019)

```mermaid
sequenceDiagram
    participant L as Legacy System (OIO/SKAT)
    participant IG as Integration Gateway (/soap/*)
    participant SEC as Oces3SoapSecurityInterceptor
    participant AUD as ClsSoapAuditInterceptor
    participant EP as OIO/SkatFordringIndberetEndpoint
    participant DS as Debt Service

    L->>IG: POST /soap/oio (SOAP 1.1/1.2, OCES3 mTLS cert)
    IG->>SEC: Validate OCES3 certificate → extract fordringshaverId
    SEC-->>IG: Authorized
    IG->>AUD: Record start time + correlationId
    IG->>EP: Unmarshal OIO/SKAT XML → FordringSubmitRequest DTO
    EP->>DS: POST /internal/fordringer (X-Fordringshaver-Id, X-Correlation-Id)
    DS-->>EP: 201 Created (claimId)
    EP-->>IG: Marshal result → SOAP response XML
    IG->>AUD: PII-mask bodies → ship SoapAuditEvent to CLS
    IG-->>L: HTTP 200 SOAP response
```

## Data flow: Claim submission

```mermaid
sequenceDiagram
    participant C as Creditor System
    participant IG as Integration Gateway
    participant CRS as Creditor Service
    participant DS as Debt Service
    participant RE as Rules Engine
    participant CS as Case Service

    C->>IG: POST /creditor-m2m/claims/submit
    IG->>CRS: POST /creditors/access/resolve
    CRS-->>IG: Access resolution (creditorOrgId)
    IG->>DS: POST /debts/submit
    DS->>RE: Validate claim rules
    RE-->>DS: Validation result
    DS->>CS: Create case + assign debt
    DS-->>IG: Submission response
    IG-->>C: Gateway response
```

## Business configuration (petition 046/047)

Time-versioned business values (interest rates, fees, thresholds) are stored in the `business_config` table in **debt-service** and accessed via `BusinessConfigService`. No configuration lives in `application.yml` for business values.

```mermaid
stateDiagram-v2
    [*] --> PENDING_REVIEW : createEntry()
    PENDING_REVIEW --> APPROVED : approveEntry()
    PENDING_REVIEW --> REJECTED : rejectEntry()
    APPROVED --> [*] : entry expires (valid_to reached)
    REJECTED --> [*]

    note right of PENDING_REVIEW
        4-øjne-princip:
        opretteren ≠ godkenderen
    end note
```

When `RATE_NB_UDLAAN` is created or updated, three derived rate entries are automatically computed and created as `PENDING_REVIEW`:

| Config key | Derivation |
|------------|------------|
| `RATE_INDR_STD` | NB + 4 pp |
| `RATE_INDR_TOLD` | NB + 2 pp |
| `RATE_INDR_TOLD_AFD` | NB + 1 pp |

The `InterestAccrualJob` and `InterestRecalculationService` resolve the effective rate per day, splitting interest periods at rate-change boundaries (see petition 045/046 implementation).

## Key architectural decisions

See the [ADR Index](adr-index.md) for all decisions. The most impactful are:

- **ADR-0007**: No cross-service database connections
- **ADR-0014**: GDPR data isolation in person-registry
- **ADR-0018**: Double-entry bookkeeping for payments
- **ADR-0019**: Orchestration over event-driven architecture
- **ADR-0024**: Observability with Grafana stack
- **ADR-0029**: immudb for cryptographic financial ledger integrity (conditionally accepted; pending TB-028-a HDP validation)
- **ADR-0030**: SOAP legacy gateway (OIO/SKAT protocols via `integration-gateway`)
