# OpenDebt - AI Agent Guidelines

This document provides guidelines for AI agents (GitHub Copilot, ChatGPT, Claude, etc.) working on the OpenDebt codebase.

## Project Overview

OpenDebt is an open-source debt collection system for Danish public institutions, designed to replace legacy systems like EFI/DMI with a modern, PSRM-compatible architecture.

### Key Characteristics
- **Language**: Java 21, Spring Boot 3.3
- **Architecture**: Microservices deployed on Kubernetes
- **API Style**: REST with OpenAPI 3.1 specifications
- **Authentication**: OAuth2/OIDC via Keycloak
- **Database**: PostgreSQL 16 (enterprise grade with audit and history)
- **Target Platform**: UFST Horizontale Driftsplatform

## Tools

### Diagrams
Use **Mermaid** for all diagrams (architecture, ER, flowcharts, sequence diagrams). Embed Mermaid blocks directly in Markdown files using ` ```mermaid ` fenced code blocks. Do NOT use ASCII art or draw.io.

### Documentation Maintenance (CRITICAL)
**Every time source code is changed, check and update the following documentation if affected:**
- `docs/architecture-overview.md` - Service inventory, implementation status, diagrams, endpoint lists
- `docs/development-process-rules-and-workflows.md` - Rules and workflow development process
- `agents.md` - ADR references, package structure, patterns
- Relevant ADR in `docs/adr/` if an architectural decision is affected

## Architecture Principles

### GDPR Data Isolation (CRITICAL)

**All personal data (PII) is isolated in the Person Registry service.** Other services store only technical UUIDs.

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

All code must align with Danish public sector architecture principles:

1. **Business-driven architecture** - Services map to business capabilities
2. **Flexibility and scalability** - Stateless services, horizontal scaling
3. **Coherence** - Consistent patterns across services
4. **Data reuse** - APIs for all data access, no direct DB connections
5. **Integrated security** - OAuth2, RBAC, audit logging
6. **Privacy by design** - GDPR compliance, data minimization
7. **Interoperability** - Standard protocols and formats
8. **Openness** - Open source, no vendor lock-in

### No Cross-Service Database Access

**CRITICAL**: Services must NOT directly connect to OTHER services' databases. Each service owns its database, but cross-service data access is via APIs.

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
Always use Spring Security annotations:
```java
@PreAuthorize("hasRole('CASEWORKER')")           // Role check
@PreAuthorize("hasAuthority('SCOPE_read:debts')") // Scope check
@PreAuthorize("@accessChecker.canAccess(#id)")   // Custom check
```

### Error Handling
Use the common exception hierarchy:
```java
throw new OpenDebtException(
    "Debt not found: " + id,
    "DEBT_NOT_FOUND",
    ErrorSeverity.WARNING
);
```

## Danish Domain Terminology

Use correct Danish terminology in code and documentation:

| Danish | English | Usage |
|--------|---------|-------|
| Fordringshaver | Creditor | Institution submitting debt |
| Skyldner | Debtor | Person/company owing debt |
| Gæld | Debt | The amount owed |
| Indrivelse | Collection | Debt collection process |
| Indrivelsesparat | Ready for collection | Debt validation status |
| Modregning | Offsetting | Deducting from payments due |
| Lønindeholdelse | Wage garnishment | Deducting from salary |
| Borger | Citizen | Individual person |
| Sag | Case | Collection case |

## Testing Requirements

### Unit Tests
- Minimum 80% line coverage, 70% branch coverage
- Use JUnit 5 and Mockito
- Test business logic in isolation

### Architecture Tests

**Shared rules** are defined in `opendebt-common/src/test/java/.../SharedArchRules.java` and enforced across all 12 services:

- **ADR-0014 (GDPR):** `ENTITIES_MUST_NOT_STORE_PII` — bans PII field names (cpr, cvr, name, address, email, phone) in `@Entity` classes. Applied to all services except person-registry (the GDPR vault).
- **ADR-0007 (DB isolation):** `noAccessToOtherServiceRepositories("myservice")` — prevents any service from importing another service's repository classes. Applied to all 12 services.

Each service has a thin `*ArchitectureTest.java` that references the shared rules:

```java
@AnalyzeClasses(packages = "dk.ufst.opendebt.myservice", importOptions = ImportOption.DoNotIncludeTests.class)
class MyServiceArchitectureTest {
    @ArchTest static final ArchRule pii = SharedArchRules.ENTITIES_MUST_NOT_STORE_PII;
    @ArchTest static final ArchRule db  = SharedArchRules.noAccessToOtherServiceRepositories("myservice");
}
```

**When adding a new service:** create this test class referencing `SharedArchRules`. If the service has a `..client..` package with WebClient, also add the trace propagation rule from `PortalArchitectureTest`.

**Trace propagation rule (ADR-0024):** Services with inter-service clients must prevent `WebClient.create()`:

```java
@ArchTest
static final ArchRule clients_must_use_injected_webclient_builder =
    noClasses()
        .that().resideInAPackage("..client..")
        .should().callMethod(WebClient.class, "create")
        .orShould().callMethod(WebClient.class, "create", String.class)
        .as("Service clients must inject WebClient.Builder for trace propagation (ADR-0024).");
```

See `CreditorArchitectureTest` (full layered architecture + shared rules) and `PortalArchitectureTest` (WebClient guard) for reference.

### Integration Tests
- Use Testcontainers for external dependencies
- Test API contracts against OpenAPI specs

## CI/CD Pipeline

### Required Checks
1. `spotless:check` - Code formatting
2. `test` - Unit tests
3. `verify` - Integration tests + coverage
4. `dependency-check:check` - Security vulnerabilities
5. `sonar:sonar` - Static analysis

### Before Committing
```bash
mvn spotless:apply    # Fix formatting
mvn verify            # Run all checks
```

## External Integrations

### DUPLA (API Gateway)
All external APIs must be exposed via DUPLA:
- Define OpenAPI spec first
- Use OCES3 certificates for system-to-system
- Log all calls to CLS

### TastSelv Integration
Citizen portal integrates with TastSelv for authentication:
- MitID login flow
- Token exchange for internal services

### Digital Post
Letters are sent via Digital Post:
- Check recipient status before sending
- Fallback to physical mail if needed

## Common Patterns

### Service Client Pattern

**CRITICAL (ADR-0024 / Trace Propagation):** Always inject `WebClient.Builder` — never use `WebClient.create()`. The Spring-managed builder carries Micrometer Tracing filters that automatically propagate W3C `traceparent` headers across inter-service calls. Without this, distributed traces break and requests cannot be correlated across services. This is enforced by ArchUnit tests.

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
All entities should extend `AuditableEntity` from opendebt-common:
```java
@Entity
public class MyEntity extends AuditableEntity {
    @Id
    private UUID id;
    // ... other fields (audit fields inherited)
}
```

The `AuditableEntity` provides: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `version`.

For CLS (Common Logging System) integration, events are shipped via `ClsAuditClient`:
```yaml
opendebt:
  audit:
    cls:
      enabled: true  # false for dev/test
```

## ADR References

When making architectural decisions, reference existing ADRs:
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

## Standard Components

### Rules Engine (Drools)
Use Drools for all business rules:
- Debt readiness validation (indrivelsesparathed)
- Interest calculation
- Collection priority for offsetting
- Thresholds and limits

```java
// Call rules engine for evaluation
DebtReadinessResult result = rulesService.evaluateReadiness(request);
```

Rules are defined in:
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
- Follow existing patterns in the codebase
- Write OpenAPI specs before implementing endpoints
- Use Lombok for boilerplate reduction
- Add proper validation annotations
- Include security annotations on all endpoints
- Write meaningful commit messages
- Update ADRs for significant decisions
- Use Mermaid for all diagrams in documentation
- Store PII only in Person Registry
- Use technical UUIDs to reference persons/organizations
- Update `docs/architecture-overview.md` when adding/changing services, endpoints, entities, or migrations

### Don't
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

When AI agents (droids) or developers identify technical debt, refactoring opportunities, or
improvements that are not blocking but should be tracked, use `AIDEV-` prefixed comments:

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
2. `backlog-planner` can scan for AIDEV comments and propose technical_backlog items
3. `tech-debt-executor` implements items from the technical_backlog

These comments are collected into `petitions/program-status.yaml` under `technical_backlog`.
