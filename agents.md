# OpenDebt - AI Agent Guidelines

Guidelines for AI agents (GitHub Copilot, ChatGPT, Claude, etc.) on OpenDebt codebase.

## Project Overview

OpenDebt: open-source debt collection for Danish public institutions. Replaces EFI/DMI; PSRM-compatible architecture.

### Key Characteristics
- **Language**: Java 21, Spring Boot 3.3
- **Architecture**: Microservices on Kubernetes
- **API Style**: REST with OpenAPI 3.1
- **Authentication**: OAuth2/OIDC via Keycloak
- **Database**: PostgreSQL 16 (enterprise grade with audit and history)
- **Target Platform**: UFST Horizontale Driftsplatform

## Tools

### Diagrams
Use **Mermaid** for all diagrams (architecture, ER, flowcharts, sequence diagrams). Embed with ` ```mermaid ` blocks in Markdown. Do NOT use ASCII art or draw.io.

### External API and SDK Documentation
For third-party libs/SDKs/APIs, use local Claude skill `~/.claude/skills/get-api-docs/` and `chub` workflow — don't rely on remembered API shapes.

- Use `chub search "<library or API name>"` to find correct documentation ID
- Use `chub get <id> [--lang <lang>]` to fetch current documentation
- Use fetched documentation as implementation reference
- Save concise local notes with `chub annotate <id> "..."`

### Documentation Maintenance (CRITICAL)
**On every source change, check/update docs if affected:**
- `architecture/overview.md` - Service inventory, implementation status, diagrams, endpoint lists
- `docs/development-process-rules-and-workflows.md` - Rules and workflow development process
- `agents.md` - ADR references, package structure, patterns
- Relevant ADR in `architecture/adr/` if architectural decision affected
- `docs/site/technical/` - Developer guide, architecture, API reference, domain model (English)
- `docs/site/fordringshaver/` - Creditor user guide (Danish) if creditor-facing features change
- `docs/site/skyldner/` - Citizen user guide (Danish) if citizen-facing features change
- `docs/site/sagsbehandler/` - Caseworker user guide (Danish) if caseworker-facing features change

Doc site built with MkDocs (`mkdocs.yml` at root). Preview: `mkdocs serve`.

### Memory MCP Synchronisation
**When `petitions/program-status.yaml` or `architecture/adr/` updated, sync to memory MCP knowledge graph** (if available).

Update memory when:
- New ADR added or status changes → create or update entity for ADR
- TB-* item added, completed, or blocked → update relevant entity
- Petition status changes (e.g. `not_started` → `implemented`) → update petition entity

Use `memory-create_entities` for new items, `memory-add_observations` / `memory-delete_observations` for status changes. Keep observations concise — YAML is source of truth; memory is queryable index.

## Architecture Principles

### GDPR Data Isolation (CRITICAL)

**All PII isolated in Person Registry.** Other services store only technical UUIDs.

| Data Type | Storage Location | Other Services Store |
|-----------|------------------|---------------------|
| CPR numbers | Person Registry (encrypted) | `person_id` (UUID) |
| CVR numbers | Person Registry (encrypted) | `person_id` or `org_id` (UUID) |
| Names | Person Registry (encrypted) | Nothing |
| Addresses | Person Registry (encrypted) | Nothing |
| Email/Phone | Person Registry (encrypted) | Nothing |

**When creating new entities:**
```java
// CORRECT - reference by technical ID
@Column(name = "debtor_person_id", nullable = false)
private UUID debtorPersonId;

// WRONG - never store PII directly
private String cprNumber;  // NEVER DO THIS
private String name;       // NEVER DO THIS
```

### Fællesoffentlige Arkitekturprincipper Compliance

Code must align with Danish public sector architecture principles:

1. **Business-driven architecture** - Services map to business capabilities
2. **Flexibility and scalability** - Stateless services, horizontal scaling
3. **Coherence** - Consistent patterns across services
4. **Data reuse** - APIs for all data access, no direct DB connections
5. **Integrated security** - OAuth2, RBAC, audit logging
6. **Privacy by design** - GDPR compliance, data minimization
7. **Interoperability** - Standard protocols and formats
8. **Openness** - Open source, no vendor lock-in

### No Cross-Service Database Access

**CRITICAL**: Services must NOT directly connect to other services' databases. Each service owns its DB; cross-service data via APIs only.

```java
// CORRECT - Service owns its own repository
@Repository
public interface DebtRepository extends JpaRepository<DebtEntity, UUID> {}

// CORRECT - Cross-service access via API client
@Component
public class PersonRegistryClient {
    public PersonDto getPerson(UUID personId) {
        return webClient.get()
            .uri(personRegistryUrl + "/api/v1/persons/{id}", personId)
            .retrieve()
            .bodyToMono(PersonDto.class)
            .block();
    }
}

// WRONG - Accessing another service's database directly
@Repository
public interface PersonRepository extends JpaRepository<Person, UUID> {} // In debt-service - WRONG!
```

## Code Standards

### Implementation Language and Domain Terminology (CRITICAL)

**All source code (Java, JS, SQL), comments, logs, API contracts: English only.** No Danish in identifiers, string literals, or comments — use English equivalents.

**`docs/begrebsmodel/` is single source for domain terminology.** Section 2.1: canonical Danish→English mapping. Use English equivalent for classes, fields, methods, columns, REST resources, DTOs. For i18n UI text, use Danish column as source concept.

| Begrebsmodel Danish | English for code | Example usage |
|---------------------|-----------------|---------------|
| Fordringshaver | Creditor | `CreditorService`, `creditor_org_id` |
| Skyldner | Debtor | `DebtorPersonId`, `debtor_person_id` |
| Fordring | Claim | `ClaimEntity`, `/api/v1/claims` |
| Restance | Overdue Claim | `OverdueClaimDto` |
| Fordringstype | Claim Type | `ClaimType`, `claim_type_code` |
| Fordringsart | Claim Art | `ClaimArt` |
| Hovedstol | Principal | `principalAmount`, `principal_amount` |
| Betalingsfrist | Payment Deadline | `paymentDeadline` |
| Forældelse | Limitation | `limitationDate` |
| Overdragelse til inddrivelse | Transfer for Collection | `TransferForCollectionEvent` |
| Inddrivelsesskridt | Collection Measure | `CollectionMeasure` |
| Modregning | Set-off | `SetOffService` |
| Lønindeholdelse | Wage Garnishment | `WageGarnishmentService` |
| Udlæg | Attachment | `AttachmentService` |
| Afdragsordning | Instalment Arrangement | `InstalmentArrangementDto` |
| Hæftelse | Liability | `LiabilityEntity`, `liability_type` |
| Indsigelse | Objection | `ObjectionService` |
| Underretning | Notification | `NotificationService` |
| Påkrav | Demand for Payment | `DemandForPaymentDto` |
| Rykker | Reminder Notice | `ReminderNoticeDto` |
| Dækning | Recovery | `RecoveryEntity` |
| Inddrivelsesrente | Recovery Interest | `recoveryInterestRate` |
| Regulering | Claim Adjustment | `ClaimAdjustmentEvent` |
| Opskrivning | Write-up | `WriteUpDto` |
| Nedskrivning | Write-down | `WriteDownDto` |
| Tilbagekald | Withdrawal | `WithdrawalService` |
| Genindsendelse | Resubmission | `ResubmissionDto` |
| Høring | Hearing | `HearingEntity` |
| Fordringskompleks | Claim Complex | `claimComplexId` |
| Sag | Case | `CaseEntity` |

**For i18n message bundles** (`messages_*.properties`): each locale file written entirely in its target language. Begrebsmodel Danish terms are source concepts. New locale bundles must include header comment mapping terms back to begrebsmodel.

### Package Structure
```
dk.ufst.opendebt.<service>/
├── config/          # Spring configuration
├── controller/      # REST controllers
├── service/         # Business logic interfaces
│   └── impl/        # Service implementations
├── client/          # API clients for other services
├── dto/             # Data transfer objects
├── mapper/          # MapStruct mappers
└── exception/       # Custom exceptions
```

`integration-gateway` has `soap/` sub-package for Spring-WS SOAP logic (petition019, ADR-0030):

```
dk.ufst.opendebt.integrationgateway.soap/
├── config/          # SoapConfig (MessageDispatcherServlet, WSDL beans, SAAJ factory)
│                    # SoapMessageReceiverHandlerAdapter
├── fault/           # SoapFaultMappingResolver, domain SOAP exceptions
├── filter/          # SoapHttpStatusFilter, SoapParseErrorFilter, WsdlServingFilter
├── interceptor/     # Oces3SoapSecurityInterceptor, ClsSoapAuditInterceptor
├── oio/             # OIO endpoints, OioClaimMapper, generated JAXB
└── skat/            # SKAT endpoints, SkatClaimMapper, generated JAXB
```

`payment-service` has `daekning/` sub-package for GIL § 4 payment application order module (petition057):

```
dk.ufst.opendebt.payment.daekning/
├── PrioritetKategori.java       # Enum — 5 GIL § 4 priority categories
├── RenteKomponent.java          # Enum — 6 interest component sub-positions
├── InddrivelsesindsatsType.java # Enum — 4 inddrivelsesindsats types
├── dto/                         # DaekningsraekkefoelgePositionDto, SimulatePositionDto, SimulateRequestDto
├── entity/                      # DaekningFordringEntity (daekning_fordring), DaekningRecord (daekning_record)
├── repository/                  # DaekningFordringRepository, DaekningRecordRepository
└── service/
    ├── DaekningsRaekkefoeigenService.java      # Interface
    └── impl/
        └── DaekningsRaekkefoeigenServiceImpl.java  # 8-step GIL § 4 algorithm
```

`debt-service` has `offsetting/` sub-package for Modregning og Korrektionspulje module (petition058, ADR-0027):

```
dk.ufst.opendebt.debtservice.offsetting/
├── batch/
│   └── KorrektionspuljeSettlementJob.java  # @Scheduled monthly + annual settlement sweep
├── client/
│   └── DaekningsRaekkefoeigenServiceClient.java  # HTTP stub for P057 in payment-service
├── controller/
│   └── ModregningController.java           # POST tier2-waiver, GET modregning-events
├── entity/
│   ├── ModregningEvent.java                # GIL § 16 stk. 1 set-off decision; written to immudb (ADR-0029)
│   ├── KorrektionspuljeEntry.java          # Pool entry for reversal/gendaenkning credit
│   └── RenteGodtgoerelseRateEntry.java     # Rate table for GIL § 8b computation
├── repository/
│   ├── ModregningEventRepository.java
│   ├── KorrektionspuljeEntryRepository.java
│   └── RenteGodtgoerelseRateEntryRepository.java
└── service/
    ├── FordringQueryPort.java                     # Internal JPA adapter for active-fordringer queries
    ├── ModregningService.java                     # @Service, @Transactional — three-tier orchestrator
    ├── ModregningsRaekkefoeigenEngine.java        # GIL § 7 stk. 1 allocation algorithm
    ├── KorrektionspuljeService.java               # @Service, @Transactional — reversal/pool processor
    ├── RenteGodtgoerelseService.java              # GIL § 8b rate + start-date calculator
    ├── DanishBankingCalendar.java                 # 5-banking-day utility
    ├── PublicDisbursementEvent.java               # Inbound DTO from integration-gateway (Nemkonto)
    └── [result/decision records]                  # ModregningResult, KorrektionspuljeResult, etc.
```

Key invariants in `offsetting/`:
- `renteGodtgoerelseNonTaxable` is ALWAYS `true` (GIL SS 8b; hardcoded, never configurable)
- No CPR/PII in entities — `UUID debtorPersonId` only (ADR-0014)
- Idempotency via `nemkontoReferenceId` unique constraint (AC-5)
- `@Transactional` on `initiateModregning` and `settleEntry` (NFR-1)
- CLS audit per allocation with `gilParagraf` annotation (NFR-2)

`caseworker-portal` has `daekning/` view sub-package for GIL § 4 view (petition057):

```
dk.ufst.opendebt.caseworkerportal.daekning/
└── DaekningsRaekkefoeigenViewController.java   # GET /debtors/{debtorId}/daekningsraekkefoelge
                                                 # — calls PaymentServiceClient.getDaekningsraekkefoelge()
                                                 #   and renders daekningsraakkefoelge.html
```

`PaymentServiceClient` in `caseworker-portal` extended with `getDaekningsraekkefoelge(UUID debtorId)` → `GET /api/v1/debtors/{debtorId}/daekningsraekkefoelge` on payment-service (petition057). i18n: 12 new keys/locale (5 priority category labels, 6 interest component labels, 1 view title).

Shared code in `opendebt-common` base package `dk.ufst.opendebt.common`:

```
dk.ufst.opendebt.common/
├── audit/           # AuditableEntity, ClsAuditClient, audit infrastructure
├── dto/             # Cross-service shared DTOs (CaseDto, DebtDto, DebtEventDto, …)
├── exception/       # OpenDebtException, ErrorResponse, GlobalExceptionHandler
└── timeline/        # Petition050 unified timeline: EventCategory, TimelineSource,
                     #   TimelineEntryDto, TimelineFilterDto, EventCategoryMapper,
                     #   TimelineDeduplicator, TimelineEntryMapper,
                     #   TimelineVisibilityProperties
```

### Naming Conventions
- **Classes**: PascalCase (`DebtController`, `CaseService`)
- **Methods**: camelCase (`createDebt`, `validateReadiness`)
- **Constants**: SCREAMING_SNAKE_CASE (`MAX_DEBT_AMOUNT`)
- **Packages**: lowercase (`dk.ufst.opendebt.debtservice`)
- **REST endpoints**: kebab-case (`/api/v1/debt-types`)
- **JSON fields**: camelCase (`debtorId`, `principalAmount`)

### API Design
```java
@RestController
@RequestMapping("/api/v1/debts")
@Tag(name = "Debts", description = "Debt management operations")
public class DebtController {
    
    @GetMapping("/{id}")
    @Operation(summary = "Get debt by ID")
    @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
    public ResponseEntity<DebtDto> getDebt(@PathVariable UUID id) {
        // Implementation
    }
}
```

### Security Annotations
Use Spring Security annotations:
```java
@PreAuthorize("hasRole('CASEWORKER')")           // Role check
@PreAuthorize("hasAuthority('SCOPE_read:debts')") // Scope check
@PreAuthorize("@accessChecker.canAccess(#id)")   // Custom check
```

### Error Handling
Use common exception hierarchy:
```java
throw new OpenDebtException(
    "Debt not found: " + id,
    "DEBT_NOT_FOUND",
    ErrorSeverity.WARNING
);
```

## Domain Terminology Reference

**`docs/begrebsmodel/` single source of truth.** See mapping table in "Implementation Language and Domain Terminology" above. Code: **English only**; Danish only in `da` i18n bundles and domain docs. Consult `docs/begrebsmodel/Inddrivelse-begrebsmodel-UFST-v3.md` section 2.1.

## Testing Requirements

### Unit Tests
- Minimum 80% line coverage, 70% branch coverage
- Use JUnit 5 and Mockito
- Test business logic in isolation

### Architecture Tests

**Shared rules** in `opendebt-common/src/test/java/.../SharedArchRules.java`, enforced across all 12 services:

- **ADR-0014 (GDPR):** `ENTITIES_MUST_NOT_STORE_PII` — bans PII field names (cpr, cvr, name, address, email, phone) in `@Entity` classes. Applied to all services except person-registry (the GDPR vault).
- **ADR-0007 (DB isolation):** `noAccessToOtherServiceRepositories("myservice")` — prevents any service from importing another service's repository classes. Applied to all 12 services.

Each service has thin `*ArchitectureTest.java` referencing shared rules:

```java
@AnalyzeClasses(packages = "dk.ufst.opendebt.myservice", importOptions = ImportOption.DoNotIncludeTests.class)
class MyServiceArchitectureTest {
    @ArchTest static final ArchRule pii = SharedArchRules.ENTITIES_MUST_NOT_STORE_PII;
    @ArchTest static final ArchRule db  = SharedArchRules.noAccessToOtherServiceRepositories("myservice");
}
```

**New service:** create test class referencing `SharedArchRules`. If service has `..client..` with WebClient, add trace propagation rule from `PortalArchitectureTest`.

**Trace propagation rule (ADR-0024):** Services with inter-service clients must not use `WebClient.create()`:

```java
@ArchTest
static final ArchRule clients_must_use_injected_webclient_builder =
    noClasses()
        .that().resideInAPackage("..client..")
        .should().callMethod(WebClient.class, "create")
        .orShould().callMethod(WebClient.class, "create", String.class)
        .as("Service clients must inject WebClient.Builder for trace propagation (ADR-0024).");
```

See `CreditorArchitectureTest` (layered architecture + shared rules) and `PortalArchitectureTest` (WebClient guard) for reference.

### Integration Tests
- Use Testcontainers for external dependencies
- Test API contracts against OpenAPI specs

## CI/CD Pipeline

### Required Checks
1. `spotless:check` - Code formatting
2. `test` - Unit tests
3. `verify` - Integration tests + coverage
4. `sonar:sonar` - Static analysis
5. `catala typecheck --language en --no-stdlib` — Catala compliance artefacts (for petitions with `legal_footprint: true`)

**Scheduled / manual:** `dependency-check:check` (OWASP) runs weekly + workflow dispatch — see `.github/workflows/owasp-dependency-check.yml`. Not on every push (too slow).

### Before Committing
```bash
mvn spotless:apply    # Fix formatting
mvn verify            # Run all checks
```

## External Integrations

### DUPLA (API Gateway)
All external APIs via DUPLA:
- Define OpenAPI spec first
- Use OCES3 certificates for system-to-system
- Log all calls to CLS

### TastSelv Integration
Citizen portal integrates with TastSelv for authentication:
- MitID login flow
- Token exchange for internal services

### Digital Post
Letters sent via Digital Post:
- Check recipient status before sending
- Fallback to physical mail if needed

## Common Patterns

### Service Client Pattern

**CRITICAL (ADR-0024 / Trace Propagation):** Inject `WebClient.Builder` — never use `WebClient.create()`. Spring-managed builder carries Micrometer Tracing filters that propagate W3C `traceparent` headers. Without this, distributed traces break. Enforced by ArchUnit.

```java
@Component
@RequiredArgsConstructor
public class DebtServiceClient {
    private final WebClient.Builder webClientBuilder;
    
    @Value("${opendebt.services.debt-service.url}")
    private String debtServiceUrl;
    
    public DebtDto getDebt(UUID id) {
        return webClientBuilder.build()
            .get()
            .uri(debtServiceUrl + "/api/v1/debts/{id}", id)
            .retrieve()
            .bodyToMono(DebtDto.class)
            .block();
    }
}
```

```java
// WRONG — breaks distributed tracing (no trace context propagation)
WebClient client = WebClient.create(baseUrl);

// CORRECT — Spring-managed builder with Micrometer tracing filters
private final WebClient.Builder webClientBuilder; // injected via constructor
WebClient client = webClientBuilder.build();
```

### Pagination
```java
@GetMapping
public ResponseEntity<Page<DebtDto>> listDebts(
    @RequestParam(required = false) String status,
    Pageable pageable) {
    // Implementation
}
```

### Audit Infrastructure
Entities should extend `AuditableEntity` from opendebt-common:
```java
@Entity
public class MyEntity extends AuditableEntity {
    @Id
    private UUID id;
    // ... other fields (audit fields inherited)
}
```

`AuditableEntity` provides: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `version`.

For CLS integration, events shipped via `ClsAuditClient`:
```yaml
opendebt:
  audit:
    cls:
      enabled: true  # false for dev/test
```

## ADR References

Reference existing ADRs for architectural decisions:
- ADR-0002: Microservices Architecture
- ADR-0003: Java/Spring Boot Stack
- ADR-0004: API-First Design
- ADR-0005: Keycloak Authentication
- ADR-0006: Kubernetes Deployment
- ADR-0007: No Cross-Service Database Connections
- ADR-0008: Letter Management
- ADR-0009: DUPLA Integration
- ADR-0010: Architecture Principles Compliance
- ADR-0011: PostgreSQL Database
- ADR-0012: Debtor Identification Model (CPR/CVR with Role)
- ADR-0013: Enterprise PostgreSQL with Audit and History
- ADR-0014: GDPR Data Isolation - Person Registry
- ADR-0015: Drools Rules Engine
- ADR-0016: Flowable Workflow Engine
- ADR-0017: Smooks EDIFACT CREMUL/DEBMUL (SKB Integration)
- ADR-0018: Double-Entry Bookkeeping
- ADR-0019: Orchestration over Event-Driven Architecture
- ADR-0020: Creditor Channel and Master Data Architecture
- ADR-0021: UI Accessibility and Webtilgaengelighed Compliance
- ADR-0022: Shared Audit Infrastructure
- ADR-0023: Creditor Portal Frontend Technology (Thymeleaf + HTMX)
- ADR-0024: Observability Backend Stack (Grafana + Prometheus + Loki + Tempo)
- ADR-0025: Maven Build Tool
- ADR-0026: Inter-Service Resilience (Resilience4j Circuit Breaker + Retry)
- ADR-0027: Offsetting merged into debt-service
- ADR-0028: Backup and Disaster Recovery Strategy (RTO 4h / RPO 4h)
- ADR-0029: immudb for Financial Ledger Integrity (payment-service + debt-service offsetting records; see P058 amendment)
- ADR-0030: SOAP Legacy Gateway (OIO/SKAT endpoints, petition019)
- ADR-0031: Statutory Codes as Enums not Configuration
- ADR-0032: Catala Formal Compliance Layer

## Standard Components

### Rules Engine (Drools)
Use Drools for business rules:
- Debt readiness validation (indrivelsesparathed)
- Interest calculation
- Collection priority for offsetting
- Thresholds and limits

```java
// Call rules engine for evaluation
DebtReadinessResult result = rulesService.evaluateReadiness(request);
```

Rules in:
- `.drl` files for complex rules (developers)
- `.xlsx` Excel decision tables for simple rules (business analysts)

### Workflow Engine (Flowable)
Use Flowable for case management workflow:
- BPMN 2.0 process definitions in `processes/*.bpmn20.xml`
- Service tasks for automated actions (delegates)
- User tasks for caseworker/supervisor actions
- Timer events for deadlines
- Signal events for external triggers (payments, appeals)

```java
// Start workflow for a case
workflowService.startCaseWorkflow(caseId, strategy, caseworker);

// Complete a user task
workflowService.completeTask(taskId, variables);
```

## Do's and Don'ts

### Do
- Follow existing patterns
- **Financial transactions (ADR-0018):** On changes with financial effect (balances, payments, interest, offsetting, write-offs, refunds, corrections), ensure **double-entry postings** land in payment-service (`BookkeepingService` / ledger) or document explicit **ADR exception**. Service-local journals alone insufficient for statutory accounting.
- Write OpenAPI specs before implementing endpoints
- Use Lombok for boilerplate
- Add validation annotations
- Include security annotations on all endpoints
- Write conventional commits: `type(scope): description` (types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`; always include `Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>` trailer)
- Update ADRs for significant decisions
- Use Mermaid for all diagrams
- Store PII only in Person Registry
- Use technical UUIDs to reference persons/organizations
- Update `architecture/overview.md` when adding/changing services, endpoints, entities, or migrations

### Don't
- **Record financial effects only in service-local tables** (e.g. interest journals) **without** corresponding ledger posting plan to payment-service — see ADR-0018 amendment #3
- Store CPR, CVR, names, addresses outside Person Registry
- Access other services' databases directly
- Skip security annotations
- Create endpoints without OpenAPI documentation
- Use hardcoded configuration values
- Ignore code formatting rules
- Commit without running tests
- Add dependencies without security review
- Log PII data (use person_id instead)
- Use ASCII art or draw.io for diagrams (use Mermaid)
- Change source code without checking if documentation needs updating

## AIDEV Comments for Technical Backlog

Track non-blocking tech debt/refactoring/improvements with `AIDEV-` comments:

```java
// AIDEV-TODO: Migrate this entity to extend AuditableEntity (TB-001)
// AIDEV-REFACTOR: Extract duplicate validation logic to shared service
// AIDEV-PERF: Consider caching this lookup - called frequently
// AIDEV-SECURITY: Add rate limiting before production
```

**Comment types:**
- `AIDEV-TODO` - Work that should be done but isn't blocking
- `AIDEV-REFACTOR` - Code that works but should be restructured
- `AIDEV-PERF` - Performance improvement opportunities
- `AIDEV-SECURITY` - Security hardening suggestions
- `AIDEV-TEST` - Missing test coverage

**Workflow:**
1. Reviewer droids (`code-reviewer-strict`, `solution-architecture-reviewer`) add AIDEV comments
2. `backlog-planner` can scan for AIDEV comments and propose technical_backlog items; also posts new TB items to wasteland `wanted` table (see **Wasteland Integration → New items**)
3. `tech-debt-executor` implements items from technical_backlog

Collected into `petitions/program-status.yaml` under `technical_backlog`.


## C4 Architecture Governance

Architecture governed via Structurizr DSL. Key files:

| File | Purpose |
|---|---|
| `architecture/workspace.dsl` | Canonical C4 model — updated by `solution-architect`, maintained by `implementation-doc-sync` |
| `architecture/policies.yaml` | Architecture policy set — evaluated by `c4-model-validator` and `c4-architecture-governor` |

ADRs in `architecture/adr/` — one file per ADR, numbered sequentially (0001, 0002, …). Index: `docs/site/technical/adr-index.md`.

On new accepted ADR, INSERT into wasteland `decisions` table (see **Wasteland Integration → New items**).

## Wasteland Integration

**Wasteland** (`mfhens/ufst` on DoltHub): federated work registry for UFST Modernization. Petition/TB granularity; visible to external contractors and agent rigs.

**Beads and wasteland coexist.** Beads: inner tracker (sprint subtasks, fine-grained). Wasteland: outer federated registry (petition-level bounty board with evidence and trust stamps).

Local clone: `~/.hop/commons/mfhens/ufst`

### Claim

Before starting petition or TB item, mark claimed:

```bash
cd ~/.hop/commons/mfhens/ufst
dolt pull origin main
dolt sql -q "UPDATE wanted SET status='in_progress', claimed_by='mfhens' \
  WHERE id='<petition-id>' AND status='open'"
dolt add . && dolt commit -m 'claim: <petition-id>' && dolt push origin main
```

If item not in `wanted` yet, INSERT first — see **New items**.

### Complete

When petition or TB item marked `implemented`/`done`, post completion and close wanted item:

```bash
cd ~/.hop/commons/mfhens/ufst
GIT_SHA=$(git -C /home/markus/GitHub/opendebt rev-parse --short HEAD)
dolt sql -q "INSERT IGNORE INTO completions \
  (id, wanted_id, completed_by, evidence, completed_at) \
  VALUES ('<petition-id>-cmp', '<petition-id>', 'mfhens', 'commit:${GIT_SHA}', NOW())"
dolt sql -q "UPDATE wanted SET status='done' WHERE id='<petition-id>'"
dolt add . && dolt commit -m 'complete: <petition-id>' && dolt push origin main
```

### Stamp

Reviewer agents (`code-reviewer-strict`, `scrutiny-feature-reviewer`) stamp external workers' completions. `stamps` table enforces `author != subject` — no self-stamps; only meaningful for external rigs.

```bash
cd ~/.hop/commons/mfhens/ufst
STAMP_ID=$(python3 -c "import uuid; print(str(uuid.uuid4())[:16])")
dolt sql -q "INSERT INTO stamps \
  (id, author, subject, valence, confidence, skill_tags, message, context_id, context_type, created_at) \
  VALUES ('${STAMP_ID}', '<reviewer-handle>', '<worker-handle>', \
  '{\"quality\":\"high\",\"correctness\":\"verified\"}', 0.9, \
  '[\"java\",\"spring-boot\",\"opendebt\"]', '<one-line review summary>', \
  '<completion-id>', 'completion', NOW())"
dolt add . && dolt commit -m 'stamp: <worker-handle> on <completion-id>' && dolt push origin main
```

### New items

**New TB item** (`backlog-planner` posting to wasteland after updating `program-status.yaml`):

```bash
cd ~/.hop/commons/mfhens/ufst
dolt sql -q "INSERT IGNORE INTO wanted \
  (id, title, project, type, priority, tags, posted_by, status, effort_level, sandbox_required, created_at, updated_at) \
  VALUES ('<TB-id>', '<title>', 'opendebt', 'technical_backlog', 3, \
  '[\"java\",\"tech-debt\",\"opendebt\"]', 'mfhens', 'open', 'medium', TRUE, NOW(), NOW())"
dolt add . && dolt commit -m 'post: <TB-id>' && dolt push origin main
```

**New ADR** (`solution-architect` after writing an accepted ADR):

```bash
cd ~/.hop/commons/mfhens/ufst
dolt sql -q "INSERT IGNORE INTO decisions \
  (id, number, title, status, summary, skill_tags, project, created_at) \
  VALUES ('adr-<NNNN>', <N>, '<ADR title>', 'Accepted', '<one-sentence decision>', \
  '[\"architecture\",\"adr\"]', 'opendebt', NOW())"
dolt add . && dolt commit -m 'decision: adr-<NNNN>' && dolt push origin main
```

<!-- BEGIN BEADS INTEGRATION v:1 profile:minimal hash:ca08a54f -->
## Beads Issue Tracker

Project uses **bd (beads)** for issue tracking. Run `bd prime` for workflow context and commands.

### Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --claim  # Claim work
bd close <id>         # Complete work
```

### Rules

- Use `bd` for ALL task tracking — do NOT use TodoWrite, TaskCreate, or markdown TODO lists
- Run `bd prime` for detailed command reference and session close protocol
- Use `bd remember` for persistent knowledge — do NOT use MEMORY.md files

<!-- dgc-policy-v11 -->
## Dual-Graph Context Policy

Project uses local dual-graph MCP server for efficient context retrieval.

### MANDATORY: Adaptive graph_continue rule

**Call `graph_continue` ONLY when NOT already knowing relevant files.**

**Call when:**
- First message of new task / conversation
- Task shifts to different codebase area
- Need files not yet read this session

**SKIP when:**
- Already identified relevant files this conversation
- Follow-up work on already-read files (verify, refactor, test, docs, cleanup, commit)
- Pure text task (commit message, summary, explanation)

**If skipping** → go directly to `graph_read` on already-known `file::symbol`.

### When you DO call graph_continue

1. `needs_project=true` → call `graph_scan` with `pwd`. Do NOT ask user.
2. `skip=true` → fewer than 5 files — read only specifically named files.
3. Read `recommended_files` via `graph_read`:
   - Always use `file::symbol` notation (e.g. `src/auth.ts::handleLogin`) — never whole files.
   - Entries already containing `::` passed verbatim.
4. Obey confidence caps:
   - `confidence=high` → Stop. No grep/explore.
   - `confidence=medium` → `fallback_rg` at most `max_supplementary_greps` times, then `graph_read` at most `max_supplementary_files` more. Stop.
   - `confidence=low` → same as medium. Stop.

### Session State

Maintain short JSON in working memory. Update every turn:

```json
{
  "files_identified": ["path/to/file.py"],
  "symbols_changed": ["module::function"],
  "fix_applied": true,
  "features_added": ["description"],
  "open_issues": ["one-line note"]
}
```

### Token Usage

`token-counter` MCP tracks live token usage.

- Before large file read: `count_tokens({text: "<content>"})`
- Running session cost: `get_session_stats()`
- Log completed task: `log_usage({input_tokens: N, output_tokens: N, description: "task"})`

### Rules

- Do NOT use `rg`, `grep`, or file exploration before `graph_continue` (when required)
- No broad/recursive exploration at any confidence level
- `max_supplementary_greps` and `max_supplementary_files` are hard caps — never exceed
- Do NOT call `graph_continue` more than once per turn
- Always use `file::symbol` with `graph_read` — never bare filenames
- After edits, call `graph_register_edit` with changed files using `file::symbol`

### Context Store

Append decisions/tasks/blockers to `.dual-graph/context-store.json`.

```json
{"type": "decision|task|next|fact|blocker", "content": "one sentence max 15 words", "tags": ["topic"], "files": ["relevant/file.ts"], "date": "YYYY-MM-DD"}
```

Read → add entry → write back → call `graph_register_edit` on `.dual-graph/context-store.json`.

Rules:
- Only log things worth remembering across sessions
- `content` max 15 words
- Log immediately — not at session end

### Session End (Dual-Graph)

When user signals done (e.g. "bye", "done", "wrap up"), update `CONTEXT.md` in project root:
- **Current Task**: one sentence
- **Key Decisions**: bullet list, max 3
- **Next Steps**: bullet list, max 3

Keep `CONTEXT.md` under 20 lines. Only what's needed to resume next session.

## Session Completion

**End session**: complete ALL steps. Work NOT done until `git push` succeeds.

**MANDATORY WORKFLOW:**

1. **File issues for remaining work** - Create issues for anything needing follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **PUSH TO REMOTE** - MANDATORY:
   ```bash
   git pull --rebase
   bd dolt push
   cd ~/.hop/commons/mfhens/ufst && dolt push origin main  # sync wasteland
   git push
   git status  # MUST show "up to date with origin"
   ```
5. **Clean up** - Clear stashes, prune remote branches
6. **Verify** - All changes committed AND pushed
7. **Hand off** - Provide context for next session

**CRITICAL RULES:**
- Work NOT complete until `git push` succeeds
- NEVER stop before pushing - leaves work stranded locally
- NEVER say "ready to push when you are" - YOU must push
- If push fails, resolve and retry until success
<!-- END BEADS INTEGRATION -->
