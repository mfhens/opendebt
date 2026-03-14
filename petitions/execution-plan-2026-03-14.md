# Execution Plan — 2026-03-14

## Planning Summary

| Field | Value |
|---|---|
| Plan date | 2026-03-14 |
| Backlog refreshed | Yes — `program-status.yaml` and `execution-backlog.yaml` updated |
| Entrypoint | Path C — all petition artifacts (petition, outcome contract, feature file) exist |
| Planning gate | Applied — next implementation slice confirmed with dependency analysis |

## Current State (Post-Refresh)

### Completed Tickets (11 of 22)

| Ticket | Sprint | Description |
|---|---|---|
| W1-SPEC-01 | sprint-1 | petition008 executable contract |
| W1-SPEC-02 | sprint-1 | API boundaries frozen |
| W1-BOOT-01 | sprint-1 | creditor-service module bootstrapped |
| W1-BOOT-02 | sprint-1 | person-registry org API |
| W1-CRD-01 | sprint-1 | creditor persistence model |
| W1-CRD-02 | sprint-1 | creditor lookup/admin APIs |
| W1-CRD-03 | sprint-1 | status/permission validation |
| W2-SPEC-01 | sprint-3 | petition015 specs |
| W2-SPEC-02 | sprint-3 | petition016 specs |
| W2-SPEC-03 | sprint-4 | petition017 specs |
| W2-SPEC-04 | sprint-4 | petition018 specs |

### Pending Tickets (11 of 22)

| Ticket | Sprint | Status | Blocked By |
|---|---|---|---|
| **W1-CRD-04** | sprint-1 | **UNBLOCKED** | — |
| **W2-BOOT-01** | sprint-3 | **UNBLOCKED** | — |
| W1-ACC-01 | sprint-2 | blocked | W1-CRD-04 |
| W1-ACC-02 | sprint-2 | blocked | W1-ACC-01 |
| W1-ACC-03 | sprint-2 | blocked | W1-ACC-02 |
| W1-ACC-04 | sprint-2 | blocked | W1-ACC-03 |
| W2-RULE-01 | sprint-3 | blocked | W2-BOOT-01 |
| W2-RULE-02 | sprint-3 | blocked | W2-RULE-01 |
| W2-RULE-03 | sprint-4 | blocked | W2-RULE-02 |
| W2-RULE-04 | sprint-4 | blocked | W2-RULE-03 |
| W2-INT-01 | sprint-4 | blocked | W2-RULE-04 |
| W2-ACC-01 | sprint-4 | blocked | W2-INT-01 |

---

## Next Implementation Slice

Two unblocked tickets can execute **in parallel**. They touch independent modules with no shared deliverables.

### Slot 1: W1-CRD-04 — petition009 acceptance tests + architecture tests

| Field | Value |
|---|---|
| Ticket | W1-CRD-04 |
| Sprint | sprint-1 (completion) |
| Petition | petition009 |
| Module | `opendebt-creditor-service` |
| Dependencies | W1-CRD-02 ✅, W1-CRD-03 ✅ |
| Unblocks | W1-ACC-01 → Sprint 2 |

**Current scaffold state:**
- `RunCucumberTest.java` — configured, ready
- `Petition009Steps.java` — 30+ step definitions, all throwing `PendingException`
- `petition009.feature` — 13 scenarios covering all 6 acceptance criteria
- `application-test.yml` — test profile exists

**Deliverables required:**
1. **Implement all 13 Cucumber scenario step definitions** — replace every `PendingException` with working assertions against `CreditorService`, `CreditorRepository`, and `CreditorController`
2. **Add ArchUnit architecture tests** — enforce package layering (controllers → service → repository), no cross-service database access, GDPR data isolation
3. **Configure coverage gate** — JaCoCo minimum 80% line / 70% branch coverage for `opendebt-creditor-service`
4. **Verify all 13 scenarios pass** with `mvn test -pl opendebt-creditor-service`

**Implementation notes:**
- Step definitions must test against a real Spring context with Testcontainers PostgreSQL
- Person-registry organization references should use a mock/stub (WireMock or test double)
- Audit logging assertions need access to the history/audit tables created by the V1 migration
- GDPR isolation scenarios must verify that no CVR/name/address fields exist on `CreditorDto`

### Slot 2: W2-BOOT-01 — Drools rules engine bootstrap for fordring validation

| Field | Value |
|---|---|
| Ticket | W2-BOOT-01 |
| Sprint | sprint-3 (start) |
| Petitions | petition015, petition016, petition017, petition018 |
| Modules | `opendebt-rules-engine`, `opendebt-common` |
| Dependencies | W2-SPEC-01 ✅ |
| Unblocks | W2-RULE-01 → core validation rules |

**Current infrastructure state:**
- `opendebt-rules-engine` module exists with Drools dependencies (drools-core, drools-compiler, drools-decisiontables, drools-mvel, kie-api)
- `DroolsConfig.java` — KIE container configured, loads `.drl` and `.xlsx` from classpath `rules/` directory
- `RulesService.java` — interface with 4 methods (readiness, interest, collection priority, sort)
- `RulesServiceImpl.java` — implementation for existing rules
- 3 existing `.drl` files: `interest-calculation.drl`, `debt-readiness.drl`, `collection-priority.drl`
- PostgreSQL with Flyway, JPA, OpenAPI docs all configured

**Deliverables required:**
1. **Fordring error code enumeration** — `FordringErrorCode` enum mapping all error codes from petition015-018 outcome contracts to Danish descriptions (error codes: 2, 5, 151, 152, 156, 403, 404, 406, 407, 409, 411, 412, 438, 444, 447, 448, 458, 464, 467, 505, 548, 568, 569, and authorization/lifecycle/content codes from petition016-018)
2. **Validation DTOs** — `FordringValidationRequest` (claim action payload), `FordringValidationResult` (list of errors with codes and descriptions), `FordringValidationError` (individual error)
3. **Fordring validation service interface** — extend or add to `RulesService` with `validateFordring(FordringValidationRequest)` method
4. **KIE session management for fordring rules** — separate rule session or agenda group for fordring validation to isolate from existing debt-readiness/interest/priority rules
5. **Test infrastructure** — base test class for fordring rule evaluation with helper methods to construct test payloads and assert validation results
6. **Placeholder `.drl` file** — `fordring-validation.drl` with package declaration and imports, ready for W2-RULE-01 to populate with actual rules

**Implementation notes:**
- Error codes and descriptions must match the Fordring integration API specification exactly
- Validation DTOs should be placed in `opendebt-common` if they need to be shared with `opendebt-integration-gateway`
- KIE session should support stateless evaluation for validation (no session persistence needed)
- Test infrastructure should support both unit-level rule testing and scenario-based validation
- Performance target: structure validation within 50ms per petition015 outcome contract

---

## Dependency Graph (Next 4 Tickets)

```
W1-CRD-04 ──► W1-ACC-01 ──► W1-ACC-02
                                (Sprint 2)

W2-BOOT-01 ──► W2-RULE-01 ──► W2-RULE-02
                                (Sprint 3)
```

Both chains are fully independent and parallelizable.

## Blockers and Risks

| # | Risk | Mitigation |
|---|---|---|
| 1 | W1-CRD-04 step definitions require Testcontainers PostgreSQL — local Docker availability | Verify `docker compose` works in CI and locally before starting |
| 2 | Person-registry mock needed for GDPR isolation scenarios | Use WireMock or in-memory test double for organization lookup |
| 3 | W2-BOOT-01 error code enumeration is large (40+ codes across 4 petitions) | Start with petition015 codes only, add petition016-018 codes in W2-RULE-02/03/04 |
| 4 | Existing DroolsConfig loads all `.drl` files — new fordring rules may conflict with existing rules | Use separate agenda groups or rule packages to isolate fordring validation |

## Recommended Execution Order

1. **Immediate (parallel):**
   - Start W1-CRD-04 (creditor-service acceptance tests)
   - Start W2-BOOT-01 (Drools fordring bootstrap)

2. **After W1-CRD-04 completes:**
   - Start W1-ACC-01 (channel binding model)

3. **After W2-BOOT-01 completes:**
   - Start W2-RULE-01 (core fordring validation rules — 28 scenarios)

## Pipeline Conductor Delegation

Once this plan is approved, delegate to **pipeline-conductor** with:
- **W1-CRD-04**: petition009 artifacts + creditor-service module as input
- **W2-BOOT-01**: petition015 artifacts + rules-engine module as input
- Both tickets have frozen specifications and complete BDD scenarios
- Both are single-petition-bounded (W1-CRD-04) or infrastructure-bounded (W2-BOOT-01) — no cross-cutting changes needed
