# OpenDebt Consolidated Execution Plan

**Last updated:** 2026-03-19
**Supersedes:** execution-plan-2026-03-14.md, execution-plan-2026-03-15.md, execution-plan-2026-03-16.md, execution-plan-wave7-psrm-collection.md, execution-plan-skyldnerportal.md

---

## Current Status Summary

| Metric | Count |
|--------|-------|
| Total petitions | 40 |
| Validated | 6 |
| Implemented | 29 |
| In progress | 1 |
| Ready for implementation | 1 |
| Not started | 3 |

### By phase

| Phase | Name | Status |
|-------|------|--------|
| Phase 0 | Foundation (petition001-002) | petition001 done, petition002 in progress |
| Phase 1 | Core domain / creditor (petition003, 008-010) | All implemented |
| Phase 2 | Creditor channels / UI (petition011-014, 019) | petition011 implemented, petition012-014 validated, petition019 ready |
| Phase 3 | Downstream collection model (petition004-007) | **All implemented** |
| Phase 4 | Fordring validation rules (petition015-018) | All implemented |
| Phase 5 | Cross-cutting quality (petition020-021) | All validated |
| Phase 6 | Citizen portal landing (petition022) | **Implemented** |
| Phase 7 | Citizen auth / APIs (petition023-025) | **All implemented** |
| Phase 8 | Citizen self-service (petition026-028) | Not started (unblocked) |
| Phase 9 | Creditor portal features (petition029-038) | All implemented |
| Phase 10 | Batch processing / scale (petition043) | **Implemented** |
| Phase 11 | Documentation (petition044) | **Implemented** |

### Recent work (2026-03-17 to 2026-03-19)

- Implemented end-to-end claim submission flow (validation, submit endpoint, case auto-assignment)
- Built caseworker-portal with demo-login, case/debt views, ledger timeline
- Implemented crossing transaction detection in payment-service
- Extended case-service with OIO-Sag data model and assign-debt endpoint
- Created unified `start-demo.ps1` replacing two separate demo scripts
- Added creditor-service `/agreement` endpoint for portal claim wizard
- Removed duplicate FordringController and dashboard shortcuts from creditor portal
- Changed license from Apache 2.0 to source-available (read-only)
- **Implemented petition003** (fordring lifecycle model): evaluateClaimState, transferForCollection with recipient audit trail, REST endpoints, 4 BDD scenarios
- **Implemented petition011** (M2M ingress via integration-gateway): CreditorM2mController, CreditorServiceClient, DebtServiceClient, 3 BDD scenarios + 13 unit tests
- **Implemented petition022** (citizen portal landing page): Thymeleaf layout, FAQ, accessibility, i18n, SecurityConfig
- **Implemented petition023** (person registry CPR lookup API): PersonController, PersonService, 3 endpoints
- **Implemented petition024** (citizen debt summary endpoint): CitizenDebtController, CitizenDebtService, pagination, status filter
- **Implemented petition025** (MitID/TastSelv OAuth2 flow): SecurityConfig OAuth2, PersonRegistryClient, CPR resolution
- **Implemented petition004** (notification system): NotificationService, paakrav/rykker, OCR lines, 8 unit tests + 10 BDD scenarios
- **Implemented petition005** (liability): LiabilityService, SOLE/JOINT_AND_SEVERAL/PROPORTIONAL, 12 unit tests + 8 BDD scenarios
- **Implemented petition006** (objection workflow): ObjectionService, collection blocking, 11 unit tests + 6 BDD scenarios
- **Implemented petition007** (collection measures): CollectionMeasureService, SET_OFF/WAGE_GARNISHMENT/ATTACHMENT, 11 unit tests + 7 BDD scenarios
- **Implemented petition043** (batch processing): RestanceTransitionJob, InterestAccrualJob, DeadlineMonitoringJob, 12 unit tests + 7 BDD scenarios, all 258 debt-service tests pass
- **Implemented petition044** (comprehensive documentation): MkDocs site with technical, fordringshaver, skyldner, sagsbehandler guides

---

## What can be worked on now (unblocked)

### Priority 1: petition019 -- Legacy SOAP endpoints

**Why:** All validation rule dependencies (petition015-018) are implemented. Can proceed independently.

### Priority 2: Phase 8 -- Citizen self-service (petitions 026-028)

**Why:** All dependencies are now met: petition022 (landing page), petition024 (debt summary), petition025 (MitID auth) are all implemented. Phase 8 is fully unblocked.

### Also unblocked (lower priority)

- W1-ACC-03/04: Wire shared access resolution into integration-gateway, creditor-portal, and debt-service
- TB-008: Replace readiness validation stub with Drools rules engine call (petition015 is implemented)
- petition002: Complete end-to-end demo validation

---

## Wave 7: Downstream Collection Model (sprints 12-14, 89 SP)

**Blocked by:** petition003 (lifecycle model)

Once petition003 is done, Wave 7 executes in 3 sprints:

### Sprint 12 (31 SP) -- Fordring lifecycle, stamdata, hearing

| Ticket | Description | SP | Depends on |
|--------|-------------|-----|------------|
| W7-STAM-01 | 22 PSRM stamdata fields (partially done) | 8 | -- |
| W7-LIFE-01 | Lifecycle state machine | 8 | W7-STAM-01 |
| W7-ZERO-01 | 0-fordring pattern | 3 | W7-LIFE-01 |
| W7-HEAR-01 | HOERING workflow + portal UI | 8 | W7-STAM-01 |
| W7-KVIT-01 | Kvittering response model | 4 | W7-STAM-01, W7-HEAR-01 |

### Sprint 13 (35 SP) -- Notifications, liability, withdrawal

| Ticket | Description | SP | Depends on |
|--------|-------------|-----|------------|
| W7-UND-01 | 6 PSRM underretning types | 8 | Sprint 12 |
| W7-HAFT-01 | Solidarisk haeftelse + HAFT | 8 | W7-LIFE-01 |
| W7-HAFT-02 | I/S and PEF skyldner rules | 5 | W7-HAFT-01 |
| W7-INDS-01 | KLAG/HENS tilbagekald workflows | 8 | W7-LIFE-01 |
| W7-INDS-02 | GenindsendFordring | 6 | W7-INDS-01 |

### Sprint 14 (23 SP) -- Collection steps, write-off, acceptance

| Ticket | Description | SP | Depends on |
|--------|-------------|-----|------------|
| W7-INDR-01 | Inddrivelsesskridt domain model | 5 | W7-LIFE-01 |
| W7-INDR-02 | 100K threshold + daekningsraekkefoelge | 5 | W7-INDR-01 |
| W7-AFSK-01 | Afskrivning reason codes | 5 | W7-LIFE-01 |
| W7-ACC-01 | BDD acceptance tests (all Wave 7) | 8 | All above |

---

## Wave 6: Citizen Portal (sprints 10-11, 12 tickets)

**Unblocked now.** No backend dependencies for the landing page.

### Sprint 10 -- Infrastructure and landing page

| Ticket | Description | Depends on |
|--------|-------------|------------|
| W6-CP-01 | Thymeleaf + HTMX + layout dependencies | -- |
| W6-CP-02 | i18n infrastructure | W6-CP-01 |
| W6-CP-03 | Citizen portal layout template | W6-CP-02, W6-CP-04 |
| W6-CP-04 | Static resources (CSS, JS, fonts) | W6-CP-01 |
| W6-CP-05 | Landing page controller + template | W6-CP-03 |
| W6-CP-06 | FAQ section (7 items) | W6-CP-05 |

### Sprint 11 -- i18n, security, acceptance

| Ticket | Description | Depends on |
|--------|-------------|------------|
| W6-CP-07 | Accessibility statement (/was) | W6-CP-03 |
| W6-CP-08 | messages_da.properties | W6-CP-05-07 |
| W6-CP-09 | English translations (translator-droid) | W6-CP-08 |
| W6-CP-10 | SecurityConfig (public access) | W6-CP-01 |
| W6-CP-11 | BDD acceptance tests | W6-CP-05-10 |
| W6-CP-12 | Convergence + reuse verification | W6-CP-11 |

---

## Petition status detail

### Validated (6)

| Petition | Title |
|----------|-------|
| 009 | Dedicated fordringshaver master data service |
| 012 | Fordringshaverportal as BFF |
| 013 | UI webtilgaengelighed compliance |
| 014 | Accessibility statements |
| 020 | OpenTelemetry-based observability |
| 021 | Internationalization (i18n) |

### Implemented (29)

| Petition | Title |
|----------|-------|
| 001 | OCR-based payment matching |
| 003 | Fordring lifecycle model |
| 004 | Underretning, paakrav, rykker |
| 005 | Haeftelse for multiple skyldnere |
| 006 | Indsigelse workflow |
| 007 | Inddrivelsesskridt (collection measures) |
| 008 | Fordringshaver data model |
| 010 | Channel binding and access resolution |
| 011 | M2M ingress via integration-gateway |
| 015 | Fordring core validation rules |
| 016 | Fordring authorization rules |
| 017 | Fordring lifecycle/reference rules |
| 018 | Fordring content validation rules |
| 022 | Citizen portal landing page |
| 023 | Person Registry CPR lookup API |
| 024 | Citizen-facing debt summary endpoint |
| 025 | MitID/TastSelv OAuth2 auth flow |
| 029 | Portal -- claims lists |
| 030 | Portal -- claim detail |
| 031 | Portal -- claims in hearing |
| 032 | Portal -- rejected claims |
| 033 | Portal -- claim creation wizard |
| 034 | Portal -- write-up/write-down |
| 035 | Portal -- notifications |
| 036 | Portal -- reconciliation |
| 037 | Portal -- monthly reports |
| 038 | Portal -- dashboard/navigation/settings |
| 043 | Batch processing (daily lifecycle and interest) |
| 044 | Comprehensive documentation |

### In progress (1)

| Petition | Title | Blocker |
|----------|-------|---------|
| 002 | Creditor creation of new fordring | Needs petition011 (M2M) and end-to-end demo validation |

### Ready for implementation (1)

| Petition | Title | Why ready |
|----------|-------|-----------|
| 019 | Legacy SOAP endpoints | petition015-018 implemented |

### Not started (3) -- all unblocked

| Petition | Title | Blocked by |
|----------|-------|------------|
| 026 | Mit gaeldsoverblik page | -- (all deps implemented) |
| 027 | Citizen payment initiation | petition026 |
| 028 | Digital Post integration | petition026 |

---

## Technical backlog (15 items)

| ID | Title | Priority | Status |
|----|-------|----------|--------|
| TB-001 | Migrate entities to AuditableEntity | Medium | In progress |
| TB-002 | Enable CLS integration | High | Blocked (UFST endpoint) |
| TB-003 | SonarCloud SQL duplication | Low | Won't fix |
| TB-004 | Smooks CREMUL/DEBMUL pipeline | High | Blocked (sample files) |
| TB-005 | Partial unique index on ocr_line | Medium | Not started |
| TB-006 | Load debt types from config table | Medium | Not started |
| TB-007 | Refactor ReadinessValidationService | Low | Not started |
| TB-008 | Replace readiness stub with Drools | High | Unblocked (petition015 done) |
| TB-009 | Inject RulesService in overpayment | Medium | Not started |
| TB-010 | Bookkeeping balance validation | Medium | Not started |
| TB-011 | Emit event to case-service for payment | Medium | Not started |
| TB-012 | Saga/outbox for payment matching | High | Not started |
| TB-013 | Resilience4j for payment-service client | Medium | Not started |
| TB-014 | Backend reconciliation module | Medium | Not started |
| TB-015 | Backend reporting/storage module | Medium | Not started |

---

## Recommended next actions

1. **Implement petition019** (Legacy SOAP endpoints) -- all dependencies met, last Phase 2 item
2. **Implement Phase 8** (petitions 026-028) -- citizen self-service, all unblocked
3. **Wire Drools rules** into readiness validation (TB-008) -- low-hanging fruit since rules exist
4. **Complete petition002** end-to-end demo validation
5. **Wire shared access resolution** (W1-ACC-03/04) into integration-gateway, creditor-portal, debt-service
