# Test Coverage Audit Report
## Petition: petition059
## Language: java (Java 21)
## BDD Framework: cucumber-jvm via Maven/JUnit Platform
## Date: 2026-05-16T21:49:06.0032583+02:00
## Status: PASS

### Scope
- Petition: `petitions\petition059-foraeldelse.md`
- Outcome contract: `petitions\petition059-foraeldelse-outcome-contract.md`
- Canonical feature: `petitions\petition059-foraeldelse.feature`
- Specs: `petitions\specs\petition059-specs.yaml`
- Architecture: `design\solution-architecture-p059-foraeldelse.md`
- Audited generated artefacts:
  - `opendebt-debt-service\src\test\resources\features\petition059.feature`
  - `opendebt-debt-service\src\test\java\dk\ufst\opendebt\debtservice\steps\Petition059Steps.java`
  - `opendebt-caseworker-portal\src\test\resources\features\petition059.feature`
  - `opendebt-caseworker-portal\src\test\java\dk\ufst\opendebt\caseworker\steps\Petition059PortalSteps.java`

### Validation evidence
- Resolved stack from `.factory\project.yaml`: Java 21, Maven, `cucumber-jvm`, dry-run `mvn test -Dcucumber.execution.dry-run=true`.
- Targeted dry-runs passed:
  - `mvn -pl opendebt-debt-service "-Dtest=RunCucumberTest" "-Dcucumber.execution.dry-run=true" "-Dcucumber.filter.tags=@petition059" "-DfailIfNoTests=false" --no-transfer-progress test`
  - `mvn -pl opendebt-caseworker-portal "-Dtest=RunCucumberTest" "-Dcucumber.execution.dry-run=true" "-Dcucumber.filter.tags=@petition059" "-DfailIfNoTests=false" --no-transfer-progress test`
- Discovery result:
  - Canonical petition feature: 49 scenarios
  - Generated debt-service feature: 47 scenarios
  - Generated caseworker-portal feature: 7 scenarios
  - All 49 canonical scenarios are still covered.
  - 5 focused retry scenarios were added beyond the canonical feature and are explicitly justified by petition/spec requirements:
    - `FR-5.3 Ukendt fordringId ved tillægsfrist returnerer HTTP 404`
    - `FR-6.1c Ukendt fordringId ved indsigelsesregistrering returnerer HTTP 404`
    - `FR-6.2b Ukendt fordringId ved indsigelsesevaluering returnerer HTTP 404`
    - `NFR-1.1 Forældelsesberegning bruger injicerbart Clock til deterministisk LocalDate-evaluering`
    - `NFR-5.1 GET /foraeldelse/{fordringId} holder p99 under 200 ms med 50 historikposter`

### Prior gap resolution
| Prior gap | Revised evidence | Status |
|---|---|---|
| Deterministic Clock coverage | `NFR-1.1` scenario + step traceability in `Petition059Steps.java` | Resolved |
| NFR-5 performance coverage | `NFR-5.1` scenario + step traceability in `Petition059Steps.java` | Resolved |
| Ordered API histories | `FR-1.2` now asserts sorted `afbrydelseHistory` by `eventDate` and `tillaegsfristHistory` by `appliedDate` | Resolved |
| FR-6 DTO response shape | `FR-6.2` and `FR-6.3` now assert updated `ForaeldelseStatusDto` plus key fields | Resolved |
| Remaining unknown-ID mutation coverage | `FR-5.3`, `FR-6.1c`, and `FR-6.2b` cover 404 on remaining public mutation endpoints | Resolved |

### Coverage Matrix
| Requirement | Petition / outcome / architecture anchor | Generated BDD anchor | Spec anchor | Coverage verdict | Notes |
|---|---|---|---|---|---|
| FR-1 Forældelsesfrist tracking | Petition FR-1; Outcome FR-1; AC 1-2; Architecture 6.2 FR-1 | Debt `FR-1.1`, `FR-1.2`, `FR-1.2b`, `FR-1.3`, `FR-1.3b`, `FR-1.4` | Debt-service acceptance: full observable contract incl. propagated metadata and ordered histories | **COVERED** | `FR-1.2` now explicitly covers ordered histories. |
| FR-2 Udskydelse | Petition FR-2; Outcome FR-2; AC 3-5; Architecture 6.2 FR-2 | Debt `FR-2.1`..`FR-2.4`; Portal `FR-7.1` | Debt-service acceptance: threshold-day semantics explicit | **COVERED** | Boundary dates, immutability, threshold flip, and no-rule path covered. |
| FR-3 Afbrydelse registration | Petition FR-3; Outcome FR-3; AC 6-12; Architecture 6.2 FR-3 | Debt `FR-3.1`..`FR-3.12`; `NFR-2.1`; `NFR-2.2`; Portal `FR-7.2` | Debt-service acceptance + wage-garnishment facts acceptance | **COVERED** | All four types, 422/404 paths, legal refs, audit payloads covered. |
| FR-4 Fordringskompleks propagation | Petition FR-4; Outcome FR-4; AC 13-14; Architecture 6.2 FR-4 | Debt `FR-4.1`..`FR-4.5`; Debt `FR-1.2b`; Portal `FR-7.3`; `NFR-2.2` | Debt-service acceptance: create/add/list + metadata + rollback | **COVERED** | Membership operations, propagation metadata, rollback, tomt kompleks interruption covered. |
| FR-5 Tillægsfrister | Petition FR-5; Outcome FR-5; AC 15; Architecture 6.2 FR-5 | Debt `FR-5.1`, `FR-5.2`, `FR-5.3`; Portal `FR-7.3`; `NFR-2.2` | Debt-service acceptance incl. `max(currentFristExpires, appliedDate) + 2 years` and state-changing 404 handling | **COVERED** | Focused retry added unknown-ID mutation coverage. |
| FR-6 Forældelsesindsigelse workflow | Petition FR-6; Outcome FR-6; AC 16-18; Architecture 6.2 FR-6 | Debt `FR-6.1`, `FR-6.1b`, `FR-6.1c`, `FR-6.2`, `FR-6.2b`, `FR-6.3`, `FR-6.3b`; Portal `FR-7.4`..`FR-7.6`; `NFR-2.2` | Debt-service acceptance + case-service workflow acceptance | **COVERED** | Response-shape assertions now present in `FR-6.2` / `FR-6.3`; missing 404 mutation paths are closed. |
| FR-7 Caseworker portal visibility | Petition FR-7; Outcome FR-7; AC 19; Architecture 6.2 FR-7 | Portal `FR-7.1`..`FR-7.7` | Portal acceptance criteria | **COVERED** | Panel data, chronology, complex section, control states, read-only suppression covered. |
| NFR-1 Deterministic date arithmetic | Petition NFR-1; Architecture 6.2 NFR-1 | Debt `FR-2.3b`; Debt `NFR-1.1` | Debt-service petition-specific NFR `P059-NFR-1` | **COVERED** | Focused retry added explicit fixed-Clock / LocalDate scenario. |
| NFR-2 Full audit trail | Petition NFR-2; Outcome AC 20; Architecture 6.2 NFR-2 | Debt `NFR-2.1`, `NFR-2.2`, `FR-4.3`, `FR-5.1`, `FR-6.1`, `FR-6.2`, `FR-6.3` | Debt-service + case-service audit acceptance | **COVERED** | Audit fields and audited event families covered. |
| NFR-3 No PII outside UUID | Petition NFR-3; Outcome AC 21; Architecture 6.2 NFR-3 | Debt `NFR-3.1` | Debt-service petition-specific NFR `P059-NFR-3` | **COVERED** | API and persistence UUID-only assertions present. |
| NFR-4 Transactional consistency | Petition NFR-4; Architecture 6.2 NFR-4 | Debt `FR-4.4` | Debt-service petition-specific NFR `P059-NFR-4` | **COVERED** | Rollback-on-failure scenario covers the transaction rule. |
| NFR-5 Performance | Petition NFR-5; Outcome DoD + success metric; Architecture 6.2 NFR-5 | Debt `NFR-5.1` | Debt-service petition-specific NFR `P059-NFR-5` | **COVERED** | Focused retry added explicit p99 coverage scenario. |

### Flagged Gaps
- None.

### Flagged Overreach
- None.
- The five extra debt-service scenarios are justified by explicit petition/spec requirements and therefore are not invented scope.

### Warnings
- Step-definition classes remain RED/pending stubs; this stage validates traceability and dry-run discovery, not executable green behavior.
- The petition/outcome package still mentions `behave --dry-run`; repo reality is Maven + cucumber-jvm, and the audit used the resolved repo stack.

### Blocking Issues
- 0 blocking coverage gaps remain.
- 0 overreach items remain.
- 0 remaining prior-review blockers remain.
- Note: the previous `petition059-bdd-test-coverage-auditor.yaml` carried 5 DISCARD items; all 5 are resolved by the revised BDD package and this artifact supersedes that state.

### Approval Decision
**PASS / APPROVED**

Reason:
- All previously flagged gaps now have explicit generated BDD coverage.
- No unjustified scenarios remain.
- Targeted dry-run discovery succeeds for both audited modules.
