# Sprint Tasks -- Droid-Sized Implementation Breakdown

**Created:** 2026-03-19
**Purpose:** Break petitions into tasks small enough for a single droid session (~30-60 min)

---

## How to use with the orchestrator

Each task below is self-contained. To execute a task, tell the delivery-orchestrator:

> "Implement task P023-T1: Create PersonController with CPR lookup endpoint in person-registry"

The task description includes enough context for a droid to implement without reading the full petition.

---

## Petition 023: Person Registry CPR lookup API (3 tasks)

**Dependencies:** None (foundational)
**Service:** opendebt-person-registry

| Task ID | Description | Depends on |
|---------|-------------|------------|
| P023-T1 | **PersonController + PersonService**: Create `PersonController` with 3 endpoints: `POST /api/v1/persons/lookup` (CPR lookup-or-create, returns UUID only), `GET /api/v1/persons/{personId}` (returns person details), `GET /api/v1/persons/{personId}/exists` (boolean existence check). Create `PersonService` interface + `PersonServiceImpl` using existing `PersonRepository` and `PersonEntity`. The lookup-or-create must hash the CPR, check `identifier_hash` index, create if missing, return `personId` UUID. Secure with `@PreAuthorize("hasRole('SERVICE')")`. DTOs: `PersonLookupRequest` (cpr string + optional name/address), `PersonLookupResponse` (personId UUID only), `PersonDto`. No CPR in responses or logs. | -- |
| P023-T2 | **Unit tests for PersonService and PersonController**: Create `PersonServiceImplTest` (lookup existing, lookup-create-new, exists-true, exists-false, get-found, get-not-found). Create `PersonControllerTest` (all 3 endpoints, mock PersonService). Follow existing test patterns (Mockito + JUnit 5). | P023-T1 |
| P023-T3 | **BDD scenarios + update OpenAPI spec**: Create `petition023.feature` in test resources with scenarios from the petition. Create `Petition023Steps.java` step definitions. Update `openapi-person-registry-internal.yaml` with new Persons endpoints. | P023-T2 |

---

## Petition 024: Citizen-facing debt summary endpoint (3 tasks)

**Dependencies:** P023 (person_id in JWT)
**Service:** opendebt-debt-service

| Task ID | Description | Depends on |
|---------|-------------|------------|
| P024-T1 | **CitizenDebtController + CitizenDebtService**: Create `CitizenDebtController` at `/api/v1/citizen/debts` with `GET` endpoint secured with `@PreAuthorize("hasRole('CITIZEN')")`. Extract `person_id` from JWT SecurityContext. Create `CitizenDebtService` interface + impl that queries `DebtRepository` by `debtorPersonId`, computes `totalOutstandingAmount` and `totalDebtCount`. DTOs: `CitizenDebtSummaryResponse` (debts list, totalOutstandingAmount, totalDebtCount), `CitizenDebtItemDto` (debtId, debtTypeName, debtTypeCode, principalAmount, outstandingAmount, interestAmount, feesAmount, dueDate, status). Support pagination (page, size, default 20, max 100) and optional `status` filter. Do NOT include PII, creditor internals, or readinessStatus. | P023-T1 |
| P024-T2 | **Database migration for debtor_person_id index**: Add a Flyway migration to create an index on `debtor_person_id` column in the debts table if it doesn't already have one. Verify the column exists (it was added in petition003 work). Add a `findByDebtorPersonId` method to `DebtRepository`. | P024-T1 |
| P024-T3 | **Unit tests + BDD scenarios**: Create `CitizenDebtControllerTest` and `CitizenDebtServiceImplTest`. Create `petition024.feature` and `Petition024Steps.java`. Test person_id extraction from security context, pagination, status filtering, and that no PII is returned. | P024-T1 |

---

## Petition 025: MitID/TastSelv OAuth2 browser flow (4 tasks)

**Dependencies:** P022 (citizen portal layout), P023 (CPR lookup)
**Service:** opendebt-citizen-portal

| Task ID | Description | Depends on |
|---------|-------------|------------|
| P025-T1 | **SecurityConfig OAuth2 login flow**: Update `SecurityConfig` in citizen-portal to add `.oauth2Login()` using the `tastselv` client registration. Configure public pages (`/`, `/was`, `/css/**`, `/js/**`, `/fonts/**`, `/webjars/**`, `/actuator/health`) as permitAll. All other paths require authentication. Add logout endpoint `/logout` that clears session and redirects to `/`. Add `spring-boot-starter-oauth2-client` dependency to pom.xml if missing. | P022 |
| P025-T2 | **PersonRegistryClient + CPR resolution**: Create `PersonRegistryClient` in citizen-portal using injected `WebClient.Builder` (ADR-0024). Calls `POST /api/v1/persons/lookup` on person-registry. Create custom `OidcUserService` or authentication success handler that extracts CPR from configurable JWT claim (`opendebt.citizen.auth.cpr-claim-name`), calls PersonRegistryClient to resolve `person_id`, stores it as session attribute `CITIZEN_PERSON_ID`. CPR must NOT be logged or stored. Add person-registry URL to application.yml. | P025-T1, P023-T1 |
| P025-T3 | **Error page + dev mock config**: Create error page template for person-resolution failures (e.g., `/error/identity-not-found.html`). Create `application-dev.yml` with Keycloak mock pointing to localhost that mimics TastSelv with a CPR claim in tokens. Document the mock setup. | P025-T2 |
| P025-T4 | **Unit tests + BDD scenarios**: Create tests for SecurityConfig (public vs authenticated pages), PersonRegistryClient, OidcUserService/success handler. Create `petition025.feature` and `Petition025Steps.java`. Test login redirect, CPR extraction, person_id resolution, logout, and error handling. | P025-T3 |

---

## Petition 004: Underretning, paakrav, rykker (4 tasks)

**Dependencies:** P003 (lifecycle model, implemented)
**Service:** opendebt-debt-service (or new opendebt-notification-service)

| Task ID | Description | Depends on |
|---------|-------------|------------|
| P004-T1 | **NotificationEntity + migration**: Create `NotificationEntity` (extends AuditableEntity) with fields: `id` (UUID), `type` (enum: PAAKRAV, RYKKER, AFREGNING, UDLIGNING, ALLOKERING, RENTER, AFSKRIVNING, TILBAGESEND), `debtId` (UUID), `senderCreditorOrgId` (UUID), `recipientPersonId` (UUID), `channel` (enum: DIGITAL_POST, PHYSICAL_MAIL, PORTAL), `sentAt` (Instant), `deliveryState` (enum: PENDING, SENT, DELIVERED, FAILED), `ocrLine` (nullable String for paakrav), `relatedLifecycleEventId` (UUID, nullable). Create `NotificationRepository`. Create Flyway migration `V*__create_notifications_table.sql`. | -- |
| P004-T2 | **NotificationService**: Create `NotificationService` interface with: `issueDemandForPayment(UUID debtId, UUID creditorOrgId)` (creates PAAKRAV notification with OCR line), `issueReminder(UUID debtId, UUID creditorOrgId)` (creates RYKKER notification), `getNotificationHistory(UUID debtId)` (returns all notifications for a debt). Create `NotificationServiceImpl`. Create `NotificationDto`. | P004-T1 |
| P004-T3 | **NotificationController + REST endpoints**: Create `NotificationController` at `/api/v1/notifications` with: `POST /api/v1/debts/{debtId}/demand-for-payment` (issue paakrav), `POST /api/v1/debts/{debtId}/reminder` (issue rykker), `GET /api/v1/debts/{debtId}/notifications` (history). Secure with `@PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")`. | P004-T2 |
| P004-T4 | **Unit tests + BDD scenarios**: Create `NotificationServiceImplTest`, `NotificationControllerTest`. Create `petition004.feature` and `Petition004Steps.java`. Test paakrav creation with OCR, rykker creation, notification history retrieval, delivery state tracking. | P004-T3 |

---

## Petition 005: Haeftelse for multiple skyldnere (4 tasks)

**Dependencies:** P003 (lifecycle model, implemented)
**Service:** opendebt-debt-service

| Task ID | Description | Depends on |
|---------|-------------|------------|
| P005-T1 | **LiabilityEntity + migration**: Create `LiabilityEntity` (extends AuditableEntity) with fields: `id` (UUID), `debtId` (UUID, FK to debts), `debtorPersonId` (UUID), `liabilityType` (enum: SOLE, JOINT_AND_SEVERAL, PROPORTIONAL), `shareAmount` (BigDecimal, nullable -- only for PROPORTIONAL), `sharePercentage` (BigDecimal, nullable -- only for PROPORTIONAL), `active` (boolean). Create `LiabilityRepository`. Create Flyway migration. Add unique constraint on (debtId, debtorPersonId). | -- |
| P005-T2 | **LiabilityService**: Create `LiabilityService` interface with: `addLiability(UUID debtId, UUID debtorPersonId, LiabilityType type, BigDecimal share)`, `removeLiability(UUID liabilityId)`, `getLiabilities(UUID debtId)`, `getDebtorLiabilities(UUID debtorPersonId)`. Create `LiabilityServiceImpl` with validation (e.g., SOLE liability means exactly one liable party, PROPORTIONAL shares must sum to 100%). Create `LiabilityDto`. | P005-T1 |
| P005-T3 | **LiabilityController + REST endpoints**: Create `LiabilityController` at `/api/v1/debts/{debtId}/liabilities` with CRUD endpoints. Secure with `@PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")`. | P005-T2 |
| P005-T4 | **Unit tests + BDD scenarios**: Create `LiabilityServiceImplTest`, `LiabilityControllerTest`. Create `petition005.feature` and `Petition005Steps.java`. Test SOLE/JOINT_AND_SEVERAL/PROPORTIONAL creation, share validation, multi-debtor queries. | P005-T3 |

---

## Petition 006: Indsigelse workflow (4 tasks)

**Dependencies:** P003 (lifecycle model, implemented)
**Service:** opendebt-debt-service (or opendebt-case-service)

| Task ID | Description | Depends on |
|---------|-------------|------------|
| P006-T1 | **ObjectionEntity + migration**: Create `ObjectionEntity` (extends AuditableEntity) with fields: `id` (UUID), `debtId` (UUID), `debtorPersonId` (UUID), `reason` (String), `status` (enum: ACTIVE, UPHELD, REJECTED), `registeredAt` (Instant), `resolvedAt` (Instant, nullable), `resolutionNote` (String, nullable). Create `ObjectionRepository`. Create Flyway migration. | -- |
| P006-T2 | **ObjectionService + collection blocking**: Create `ObjectionService` interface with: `registerObjection(UUID debtId, UUID debtorPersonId, String reason)`, `resolveObjection(UUID objectionId, ObjectionOutcome outcome, String note)`, `hasActiveObjection(UUID debtId)`, `getObjections(UUID debtId)`. Create `ObjectionServiceImpl`. When an active objection exists, `hasActiveObjection` returns true. Integrate with `ClaimLifecycleService` to prevent collection progression (transferForCollection) when `hasActiveObjection` is true. Create `ObjectionDto`. | P006-T1 |
| P006-T3 | **ObjectionController + REST endpoints**: Create `ObjectionController` at `/api/v1/debts/{debtId}/objections` with: `POST` (register), `PUT /{objectionId}/resolve` (resolve with outcome), `GET` (list). Secure with `@PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")`. | P006-T2 |
| P006-T4 | **Unit tests + BDD scenarios**: Create `ObjectionServiceImplTest`, `ObjectionControllerTest`. Create `petition006.feature` and `Petition006Steps.java`. Test objection registration, collection blocking, upheld/rejected resolution, resume after rejection. | P006-T3 |

---

## Petition 007: Inddrivelsesskridt (4 tasks)

**Dependencies:** P003 (lifecycle model, implemented)
**Service:** opendebt-debt-service

| Task ID | Description | Depends on |
|---------|-------------|------------|
| P007-T1 | **CollectionMeasureEntity + migration**: Create `CollectionMeasureEntity` (extends AuditableEntity) with fields: `id` (UUID), `debtId` (UUID), `measureType` (enum: SET_OFF, WAGE_GARNISHMENT, ATTACHMENT), `status` (enum: INITIATED, IN_PROGRESS, COMPLETED, CANCELLED), `initiatedBy` (String), `initiatedAt` (Instant), `completedAt` (Instant, nullable), `amount` (BigDecimal, nullable), `note` (String, nullable). Create `CollectionMeasureRepository`. Create Flyway migration. | -- |
| P007-T2 | **CollectionMeasureService**: Create `CollectionMeasureService` interface with: `initiateSetOff(UUID debtId, BigDecimal amount)`, `initiateWageGarnishment(UUID debtId)`, `initiateAttachment(UUID debtId)`, `completeMeasure(UUID measureId)`, `cancelMeasure(UUID measureId, String reason)`, `getMeasures(UUID debtId)`. Create `CollectionMeasureServiceImpl` with validation: debt must be in OVERDRAGET state (transferred for collection). Create `CollectionMeasureDto`. | P007-T1 |
| P007-T3 | **CollectionMeasureController + REST endpoints**: Create `CollectionMeasureController` at `/api/v1/debts/{debtId}/collection-measures` with endpoints for each measure type initiation + completion/cancellation + list. Secure with `@PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")`. | P007-T2 |
| P007-T4 | **Unit tests + BDD scenarios**: Create `CollectionMeasureServiceImplTest`, `CollectionMeasureControllerTest`. Create `petition007.feature` and `Petition007Steps.java`. Test each measure type initiation, state validation (must be OVERDRAGET), completion, cancellation, audit trail. | P007-T3 |

---

## Petition 019: Legacy SOAP endpoints (5 tasks)

**Dependencies:** P015-018 (validation rules, all implemented)
**Service:** opendebt-integration-gateway (or opendebt-debt-service)

| Task ID | Description | Depends on |
|---------|-------------|------------|
| P019-T1 | **SOAP infrastructure**: Add `spring-boot-starter-web-services` and Apache CXF (or Spring WS) dependencies to pom.xml. Create WSDL files for OIO namespace (`urn:oio:skat:efi:ws:1.0.1`) and SKAT namespace (`http://skat.dk/begrebsmodel/2009/01/15/`). Define 3 operations each: `MFFordringIndberet_I`, `MFKvitteringHent_I`, `MFUnderretSamlingHent_I`. Generate Java stubs from WSDL (jaxws-maven-plugin or cxf-codegen). | -- |
| P019-T2 | **OIO SOAP endpoints**: Create `OioFordringIndberetEndpoint`, `OioKvitteringHentEndpoint`, `OioUnderretSamlingHentEndpoint` implementing the OIO namespace operations. Each endpoint delegates to the same business logic as REST (DebtService, ClaimSubmissionService). Map OIO XML request/response to internal DTOs. Return SOAP faults for validation errors. | P019-T1 |
| P019-T3 | **SKAT SOAP endpoints**: Create `SkatFordringIndberetEndpoint`, `SkatKvitteringHentEndpoint`, `SkatUnderretSamlingHentEndpoint` implementing the SKAT namespace operations. Same delegation pattern as OIO but with SKAT XML schema mapping. | P019-T1 |
| P019-T4 | **OCES3 authentication + CLS logging**: Create OCES3 certificate authentication filter for SOAP endpoints. Map certificate subject to fordringshaver identifier. Create CLS logging interceptor that records timestamp, caller, service, operation, messageId, correlationId, status, response time. | P019-T2 |
| P019-T5 | **Unit tests + BDD scenarios**: Create tests for each SOAP endpoint (mock business services, verify XML mapping). Create `petition019.feature` and `Petition019Steps.java`. Test claim submission via SOAP, receipt retrieval, notification retrieval, auth rejection for invalid certificates, SOAP fault generation. | P019-T4 |

---

## Petition 043: Batch processing (5 tasks)

**Dependencies:** P003 (lifecycle model, implemented)
**Service:** opendebt-debt-service

| Task ID | Description | Depends on |
|---------|-------------|------------|
| P043-T1 | **BatchJobExecutionEntity + migration**: Create `BatchJobExecutionEntity` with fields: `id` (UUID), `jobName` (String), `executionDate` (LocalDate), `startedAt` (Instant), `completedAt` (Instant), `recordsProcessed` (int), `recordsFailed` (int), `status` (enum: RUNNING, COMPLETED, FAILED). Create `BatchJobExecutionRepository`. Create Flyway migration. This entity tracks batch job runs for idempotency and auditing. | -- |
| P043-T2 | **Daily RESTANCE transition job**: Create `RestanceTransitionJob` with `@Scheduled(cron)` that: queries debts in REGISTERED state with expired `paymentDeadline` and `outstandingBalance > 0`, transitions them to RESTANCE in bulk (batch UPDATE), records `ClaimLifecycleEvent` per transition, logs batch execution. Must be idempotent (skip already-transitioned). Process in configurable page sizes (default 1000). Record execution in `BatchJobExecutionEntity`. | P043-T1 |
| P043-T3 | **Daily interest accrual job**: Create `InterestAccrualJob` with `@Scheduled(cron)` that: queries debts in OVERDRAGET state where `receivedAt` + 1 month has passed, calculates daily interest (`outstandingBalance * 0.0575 / 365`), inserts interest journal entries (NOT modifying principalAmount) with accrual_date, effective_date, balance_snapshot, rate for storno compatibility. Must be idempotent (check if today's interest already recorded). Skip terminal states. | P043-T1 |
| P043-T4 | **Deadline monitoring job**: Create `DeadlineMonitoringJob` with `@Scheduled(cron)` that: identifies debts with `limitationDate` within 90 days (configurable), identifies hoering records with expired SLA deadlines, logs warnings and creates monitoring events for caseworker review. | P043-T1 |
| P043-T5 | **Unit tests + BDD scenarios**: Create `RestanceTransitionJobTest`, `InterestAccrualJobTest`, `DeadlineMonitoringJobTest`. Create `petition043.feature` and `Petition043Steps.java`. Test idempotency, bulk processing, interest calculation accuracy, deadline detection, terminal state exclusion. | P043-T2, P043-T3, P043-T4 |

---

## Technical backlog tasks

| Task ID | Description | Depends on |
|---------|-------------|------------|
| TB008-T1 | **Wire Drools rules into ReadinessValidationService**: Replace the stub implementation in `ReadinessValidationServiceImpl` with actual Drools rules engine call. The rules are already implemented (petition015). Create `RulesServiceClient` or inject `RulesService` to evaluate readiness rules. Update existing tests to verify Drools integration. | -- |
| TB001-T1 | **Migrate remaining entities to AuditableEntity**: Scan for entities that don't extend `AuditableEntity` and add the extension. Verify audit fields (`createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `version`) are populated. | -- |

---

## Recommended execution order

### Sprint A (unblocked now, no dependencies)

1. P023-T1 -- PersonController (foundational, unblocks citizen auth)
2. P023-T2 -- PersonService tests
3. P023-T3 -- BDD + OpenAPI
4. P004-T1 -- NotificationEntity (foundational for Wave 7)
5. P004-T2 -- NotificationService
6. TB008-T1 -- Wire Drools rules (quick win)

### Sprint B (after Sprint A)

7. P004-T3 -- NotificationController
8. P004-T4 -- Notification tests + BDD
9. P005-T1 -- LiabilityEntity
10. P005-T2 -- LiabilityService
11. P005-T3 -- LiabilityController
12. P005-T4 -- Liability tests + BDD

### Sprint C (after Sprint B, or parallel with Sprint B)

13. P006-T1 -- ObjectionEntity
14. P006-T2 -- ObjectionService
15. P006-T3 -- ObjectionController
16. P006-T4 -- Objection tests + BDD
17. P007-T1 -- CollectionMeasureEntity
18. P007-T2 -- CollectionMeasureService

### Sprint D (after P023, citizen auth chain)

19. P025-T1 -- SecurityConfig OAuth2 login
20. P025-T2 -- PersonRegistryClient + CPR resolution
21. P025-T3 -- Error page + dev mock
22. P025-T4 -- Auth tests + BDD
23. P024-T1 -- CitizenDebtController
24. P024-T2 -- Database migration
25. P024-T3 -- Citizen debt tests + BDD

### Sprint E (batch processing, independent)

26. P043-T1 -- BatchJobExecutionEntity
27. P043-T2 -- RESTANCE transition job
28. P043-T3 -- Interest accrual job
29. P043-T4 -- Deadline monitoring job
30. P043-T5 -- Batch tests + BDD

### Sprint F (SOAP, can be parallel)

31. P019-T1 -- SOAP infrastructure
32. P019-T2 -- OIO SOAP endpoints
33. P019-T3 -- SKAT SOAP endpoints
34. P019-T4 -- OCES3 auth + CLS logging
35. P019-T5 -- SOAP tests + BDD

### Sprint G (config API, after P046 foundation -- already done)

36. P046-T4 -- Deprecate application.yml rate fallback
37. P047-T1 -- BusinessConfigEntity status + audit migration
38. P047-T2 -- BusinessConfigService CRUD extension
39. P047-T3 -- Derived rate auto-computation
40. P047-T4 -- BusinessConfigController REST API
41. P047-T5 -- Config API unit tests

### Sprint H (config operator UI, after Sprint G)

42. P047-T6 -- ConfigServiceClient + ConfigurationController
43. P047-T7 -- Thymeleaf templates + i18n
44. P047-T8 -- BDD scenarios + integration tests
45. P046-T5 -- Timeline replay rate splitting

---

## Petition 044: Comprehensive documentation (7 tasks)

**Dependencies:** None (can be written for implemented features progressively)
**Location:** docs/ directory + mkdocs.yml at repo root

| Task ID | Description | Depends on |
|---------|-------------|------------|
| P044-T1 | **MkDocs setup + site structure**: Add `mkdocs.yml` at repo root using Material for MkDocs theme. Create directory structure: `docs/site/technical/`, `docs/site/fordringshaver/`, `docs/site/skyldner/`, `docs/site/sagsbehandler/`. Add nav sections for all four audiences. Add `mkdocs` and `mkdocs-material` to a `requirements-docs.txt`. Verify `mkdocs build` succeeds with placeholder index pages. | -- |
| P044-T2 | **Developer onboarding guide (English)**: Create `docs/site/technical/developer-guide.md` covering: repo structure, prerequisites (Java 21, Maven, Docker), build instructions (`mvn verify`, `mvn spotless:apply`), local dev setup (docker-compose for PostgreSQL + Keycloak), running tests, coding conventions summary, contribution workflow. Link to AGENTS.md for full conventions. | P044-T1 |
| P044-T3 | **Architecture + API + domain model reference (English)**: Create `docs/site/technical/architecture.md` (consolidate from existing architecture-overview.md with Mermaid diagrams), `docs/site/technical/adr-index.md` (summary table linking to each ADR), `docs/site/technical/api-reference.md` (link to each service's OpenAPI spec), `docs/site/technical/domain-model.md` (begrebsmodel terminology table + ER diagrams). | P044-T1 |
| P044-T4 | **Fordringshaver-guide (Danish)**: Create `docs/site/fordringshaver/index.md` (introduction), `docs/site/fordringshaver/oprettelse-fordringer.md` (claim creation and submission), `docs/site/fordringshaver/livscyklus.md` (lifecycle states), `docs/site/fordringshaver/regulering.md` (write-up, write-down, withdrawal), `docs/site/fordringshaver/underretninger.md` (notification types), `docs/site/fordringshaver/afstemning.md` (reconciliation and reports), `docs/site/fordringshaver/faq.md`. All in Danish using begrebsmodel terminology. | P044-T1 |
| P044-T5 | **M2M-integrationsvejledning (Danish)**: Create `docs/site/fordringshaver/m2m-integration.md` covering: authentication (OCES3 for SOAP, OAuth2 for REST), endpoint catalogue (SOAP + REST), request/response examples, error handling, test environment setup. In Danish. | P044-T4 |
| P044-T6 | **Skyldner-guide (Danish)**: Create `docs/site/skyldner/index.md` (introduction), `docs/site/skyldner/login.md` (MitID login), `docs/site/skyldner/gaeldsoverblik.md` (debt overview), `docs/site/skyldner/betaling.md` (payment options), `docs/site/skyldner/indsigelse.md` (objection and appeal), `docs/site/skyldner/renter.md` (interest rules), `docs/site/skyldner/kontakt.md` (contact and help), `docs/site/skyldner/tilgaengelighed.md` (accessibility). All in Danish. | P044-T1 |
| P044-T7 | **Sagsbehandler-guide (Danish)**: Create `docs/site/sagsbehandler/index.md` (introduction), `docs/site/sagsbehandler/sagsoversigt.md` (case management), `docs/site/sagsbehandler/fordringer.md` (claim details), `docs/site/sagsbehandler/inddrivelse.md` (collection measures), `docs/site/sagsbehandler/haeftelse.md` (liability), `docs/site/sagsbehandler/indsigelse.md` (objection handling), `docs/site/sagsbehandler/bogfoering.md` (bookkeeping timeline). All in Danish. | P044-T1 |

---

## Petition 046: Versioned business configuration (5 tasks)

**Dependencies:** P043 (batch processing, implemented)
**Services:** opendebt-debt-service (primary), opendebt-common (shared enum)
**Status:** Foundation implemented (entity, service, repository, migration, seed data). Remaining: deprecate application.yml fallback, REST API for timeline replay, full config key coverage.

| Task ID | Description | Depends on |
|---------|-------------|------------|
| P046-T1 | ✅ **BusinessConfigEntity + BusinessConfigService + migration** *(DONE)*: Created `BusinessConfigEntity` (UUID PK, configKey, configValue, valueType, validFrom, validTo, description, legalBasis, createdBy, createdAt, version). Created `BusinessConfigRepository` with `findEffective(key, date)` query. Created `BusinessConfigService` with `getDecimalValue()`, `preloadRatesForDate()`, `clearCache()`, `getHistory()`. Flyway V8 migration creates `business_config` table with unique constraint on (config_key, valid_from), seeds 18 config entries (NB rates, INDR_STD/TOLD/TOLD_AFD across 3 periods, fee amounts, thresholds). | -- |
| P046-T2 | ✅ **InterestRuleCode enum + per-debt rate resolution** *(DONE)*: Created `InterestRuleCode` enum (INDR_STD, INDR_TOLD, INDR_TOLD_AFD, INDR_EXEMPT, INDR_CONTRACT, OPK_STD) mapping each rule to a `RATE_*` config key. InterestAccrualJobHelper resolves rate per debt via `resolveRate()` / `resolveRuleCode()`. INDR_EXEMPT returns ZERO, INDR_CONTRACT uses embeddable's `additionalInterestRate`. Batch cache preloads all needed rates in single pass. | P046-T1 |
| P046-T3 | ✅ **FeeEntity + fee-inclusive interest** *(DONE)*: Created `FeeEntity` (RYKKER, UDLAEG, LOENINDEHOLDELSE, OTHER), `FeeRepository`, fees table in V8 migration. InterestAccrualJobHelper includes `feesAmount` in interest base per gældsinddrivelsesloven. | P046-T1 |
| P046-T4 | **Deprecate application.yml rate fallback**: Remove the `opendebt.interest.annual-rate` property from `application.yml` and all `@Value` injections that read it. Update any remaining code that falls back to this property. Ensure the `BusinessConfigService` is the sole source of rate values. Add a startup check that logs a warning if the `business_config` table has no entries for required keys. Update `application-test.yml` and test fixtures. | P046-T1, P046-T2 |
| P046-T5 | **Timeline replay rate splitting (petition 039 integration)**: Update `TimelineReplayServiceImpl` to use `BusinessConfigService.getDecimalValue()` for each accrual date instead of a single rate. When a rate changes mid-replay period (e.g., rate changed July 7 during June 1 – August 31 replay), the replay must split at the boundary: apply old rate before the change date, new rate on and after. Add unit tests for rate boundary splitting. | P046-T4 |

---

## Petition 047: Configuration administration UI (8 tasks)

**Dependencies:** P046 (versioned business config), P041 (caseworker portal)
**Services:** opendebt-debt-service (REST API), opendebt-caseworker-portal (UI)

| Task ID | Description | Depends on |
|---------|-------------|------------|
| P047-T1 | **BusinessConfigEntity status extension + audit table migration**: Extend `BusinessConfigEntity` with `review_status` column (VARCHAR(20), values `PENDING_REVIEW` or `APPROVED`, nullable — NULL means manually created/active). Add a computed `getStatus()` method that derives ACTIVE/FUTURE/EXPIRED from `validFrom`/`validTo` dates and combines with `reviewStatus`. Create `BusinessConfigAuditEntity` with fields: id, configEntryId, configKey, action (CREATE/UPDATE/APPROVE/REJECT/DELETE), oldValue, newValue, performedBy, performedAt, details. Create `BusinessConfigAuditRepository`. Create Flyway V9 migration adding `review_status` column to `business_config` and creating `business_config_audit` table. | P046-T1 |
| P047-T2 | **BusinessConfigService CRUD extension**: Extend `BusinessConfigService` with write methods: `createEntry(CreateConfigRequest)` — validates `validFrom` not in past, no overlap, type-parseable value, required fields; auto-closes previous open-ended entry's `validTo`. `updateEntry(UUID id, UpdateConfigRequest)` — only allows modification of PENDING_REVIEW or future entries. `deleteEntry(UUID id)` — only future entries. `approveEntry(UUID id, String operatorName)` — sets `reviewStatus = APPROVED`, closes previous entry. `rejectEntry(UUID id, String operatorName)` — deletes PENDING_REVIEW entry. All write operations create `BusinessConfigAuditEntity` records. Add overlap detection query to `BusinessConfigRepository`. Create `CreateConfigRequest`, `UpdateConfigRequest`, `ConfigEntryDto` DTOs. | P047-T1 |
| P047-T3 | **Derived rate auto-computation**: When a new `RATE_NB_UDLAAN` entry is created, auto-generate entries for `RATE_INDR_STD` (NB+0.04), `RATE_INDR_TOLD` (NB+0.02), `RATE_INDR_TOLD_AFD` (NB+0.01) with same `validFrom`, `reviewStatus = PENDING_REVIEW`, `createdBy = "SYSTEM (auto-computed from RATE_NB_UDLAAN)"`. Return the list of generated entries in the creation response so the UI can display them. Add unit tests for derivation logic and edge cases (NB rate already has derived entries). | P047-T2 |
| P047-T4 | **BusinessConfigController REST API**: Create `BusinessConfigController` in opendebt-debt-service at `/api/v1/config`. Implement 6 endpoints: `GET /` (list all grouped by key with derived status), `GET /{key}?date=` (effective value), `GET /{key}/history`, `POST /` (create + auto-derive for NB), `PUT /{id}` (update pending/future), `DELETE /{id}` (delete future). Role-based access: GET endpoints allow ADMIN, CONFIGURATION_MANAGER, CASEWORKER, SERVICE; write endpoints require ADMIN or CONFIGURATION_MANAGER. Return Danish error messages for validation failures. Add `@PreAuthorize` annotations. | P047-T2 |
| P047-T5 | **Unit tests for config API + service**: Create `BusinessConfigControllerTest` (mock service, test all 6 endpoints, role-based access, validation error responses). Create `BusinessConfigServiceCrudTest` (create with overlap rejection, create with auto-close, update restrictions, delete restrictions, approve flow, reject flow, audit record creation). Create `DerivedRateComputationTest` (NB → 3 derived rates, edge cases). Target: ≥80% line coverage on new classes. | P047-T4 |
| P047-T6 | **ConfigServiceClient + ConfigurationController in caseworker portal**: Create `ConfigServiceClient` in opendebt-caseworker-portal (same pattern as `DebtServiceClient`) calling debt-service `/api/v1/config` endpoints. Add `@CircuitBreaker` and `@Retry` per ADR-0026. Create `ConfigurationController` with routes: `GET /konfiguration` (list page), `GET /konfiguration/{key}` (detail/history page), `POST /konfiguration` (create form submit), `PUT /konfiguration/{id}/approve`, `DELETE /konfiguration/{id}`. All routes check session identity; read-only view for CASEWORKER role, full CRUD for ADMIN/CONFIGURATION_MANAGER. Add "Konfiguration" menu item to `caseworker-nav.html` fragment (visible to ADMIN/CONFIGURATION_MANAGER). | P047-T4 |
| P047-T7 | **Thymeleaf templates + i18n**: Create `config/list.html` — table grouped by category (Renter, Gebyrer, Tærskler) with status badges (green=active, yellow=pending, grey=expired), formatted values (% for rates, kr for fees, dage for thresholds). Create `config/detail.html` — version history timeline (newest first), collapsible audit trail section, "Opret ny version" form (date picker, value input, description, legal basis). For `RATE_NB_UDLAAN`, add derived-rate preview panel. Create `config/fragments/` for HTMX partials (history refresh, approve/reject actions). Add confirmation dialog (`Er du sikker?`). Add all `config.*` message keys to `messages_da.properties`. SKAT standardlayout, WCAG 2.1 AA compliant. | P047-T6 |
| P047-T8 | **BDD scenarios + integration tests**: Create `petition047.feature` with key scenarios: list config entries grouped by category, view version history, create new rate version, create NB rate with auto-derived preview, approve pending entry, reject pending entry, delete future entry, prevent past-date creation, prevent overlap, CASEWORKER read-only access, audit trail display. Create `Petition047Steps.java` step definitions. Create `ConfigurationControllerTest` for caseworker-portal (mock client, test role-based rendering, HTMX fragments). | P047-T7 |

---

## Petition 046 + 047 sprint schedule

### Sprint G: Config API foundation (builds on implemented foundation)

| # | Task | Est. |
|---|------|------|
| 1 | P046-T4 — Deprecate application.yml fallback | 30 min |
| 2 | P047-T1 — Status extension + audit table migration | 45 min |
| 3 | P047-T2 — BusinessConfigService CRUD extension | 60 min |
| 4 | P047-T3 — Derived rate auto-computation | 45 min |
| 5 | P047-T4 — BusinessConfigController REST API | 45 min |
| 6 | P047-T5 — Unit tests for config API + service | 45 min |

### Sprint H: Config operator UI

| # | Task | Est. |
|---|------|------|
| 7 | P047-T6 — ConfigServiceClient + ConfigurationController | 60 min |
| 8 | P047-T7 — Thymeleaf templates + i18n | 60 min |
| 9 | P047-T8 — BDD scenarios + integration tests | 45 min |
| 10 | P046-T5 — Timeline replay rate splitting | 60 min |

---

## Task count summary

| Petition | Tasks | Status |
|----------|-------|--------|
| P023 | 3 | Ready |
| P024 | 3 | Blocked by P023 |
| P025 | 4 | Blocked by P022, P023 |
| P004 | 4 | Ready |
| P005 | 4 | Ready |
| P006 | 4 | Ready |
| P007 | 4 | Ready |
| P019 | 5 | Ready |
| P043 | 5 | Ready |
| P044 | 7 | Ready |
| P046 | 5 | Partially done (foundation implemented) |
| P047 | 8 | Blocked by P046 |
| TB-008 | 1 | Ready |
| TB-001 | 1 | Ready |
| **Total** | **58** | |
