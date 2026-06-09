# Test Coverage Audit Report
## Petition: petition060
## Language: java (Java 21)
## BDD Framework: cucumber-jvm via Maven/JUnit Platform
## Date: 2026-05-27T05:51:57.2404091+02:00
## Status: PASS

### Scope
- Petition: `petitions\petition060-retskraftvurdering.md`
- Outcome contract: `petitions\petition060-retskraftvurdering-outcome-contract.md`
- Canonical feature: `petitions\petition060-retskraftvurdering.feature`
- Specs: `petitions\specs\petition060-specs.yaml`
- Architecture: `design\solution-architecture-p060-retskraftvurdering.md`
- Audited generated artefacts:
  - `opendebt-debt-service\src\test\resources\features\petition060.feature`
  - `opendebt-debt-service\src\test\java\dk\ufst\opendebt\debtservice\steps\Petition060Steps.java`

### Validation evidence
- Resolved stack from `.factory\project.yaml`: Java 21, Maven, `cucumber-jvm`, dry-run `mvn test -Dcucumber.execution.dry-run=true`.
- Targeted dry-run passed:
  - `mvn -q --no-transfer-progress "-Dtest=RunCucumberTest" test "-Dcucumber.execution.dry-run=true" "-Dcucumber.filter.tags=@petition060" "-DfailIfNoTests=false"` in `opendebt-debt-service`
- Discovery result:
  - Canonical petition feature: 10 scenarios
  - Generated debt-service feature: 11 scenarios
  - Generated debt-service step annotations: 53
  - All 10 canonical scenarios are present in the generated debt-service feature.
  - 1 focused extra debt-service scenario is explicitly justified by petition/spec requirement `NFR-01`:
    - `Identical input produces the same ordering without manual override`
- Prior review scan:
  - `petition060-component-mapping-reviewer.yaml`, `petition060-gherkin-minimality-reviewer.yaml`, `petition060-petition-translator-reviewer.yaml`, and `petition060-specs-minimality-reviewer.yaml` all report `discard_count: 0` and `escalate_count: 0`.
  - No prior petition060 review file reported a positive discard or escalate count.

### Coverage Matrix
| Requirement | Petition / outcome / architecture anchor | Generated BDD anchor | Spec anchor | Coverage verdict | Notes |
|---|---|---|---|---|---|
| FR-01 Default ordering | Petition FR-01; Outcome AC-01; Architecture S1 section-50 owner | Scenario `Default ordering without suspected data error` | Debt-service AC-01 | **COVERED** | Default fine/private-maintenance/other ranking is asserted directly. |
| FR-02 Override with legal basis | Petition FR-02; Outcome AC-02; Architecture S1 + S3 | Scenario `Special circumstances override records why default order was changed` | Debt-service AC-02; Caseworker-portal AC-02 | **COVERED** | Ranking change, override reason, and legal basis are all present. |
| FR-03 Data-error discretionary ordering | Petition FR-03; Outcome AC-03; Architecture S1 | Scenario `Suspected data error uses discretionary ordering instead of default ranking` | Debt-service AC-03 | **COVERED** | Scenario asserts discretionary path and prioritisation factors. |
| FR-04 Principal before accessory | Petition FR-04; Outcome AC-04; Architecture S1 | Scenarios `Accessory amounts stay behind...` and `Disproportionate accessory evaluation removes...` | Debt-service AC-04 | **COVERED** | Both defer-after-principal and disproportionate write-off exclusion are covered. |
| FR-05 Voluntary-payment surplus window | Petition FR-05; Outcome AC-05; Architecture S1 + S2 | Scenario `Voluntary payment surplus limits which doubtful claims are selected for evaluation` | Debt-service AC-05; Payment-service AC-05 | **COVERED** | Remaining window, section-4 principal ordering reuse, and accessory-last behavior are covered. |
| FR-06 Expedited surplus deviation | Petition FR-06; Outcome AC-06; Architecture S1 + S3 | Scenario `Expedited voluntary-payment deviation is logged when normal ordering would delay coverage` | Debt-service AC-06; Payment-service AC-06; Caseworker-portal AC-09 | **COVERED** | Scenario covers expedited deviation marker plus visible reason. |
| FR-07 Modregning windowing | Petition FR-07; Outcome AC-07; Architecture S1 | Scenario `Modregning uses confirmed claims first and evaluates doubtful claims within the remaining amount window` | Debt-service AC-07 | **COVERED** | Confirmed claims first, remaining amount window, and accessory-last are all asserted. |
| FR-08 Partial/no modregning visibility | Petition FR-08; Outcome AC-08; Architecture S1 + S3 | Scenario `Partial or no modregning can be chosen for operational reasons and must be visible` | Debt-service AC-08; Caseworker-portal AC-08 | **COVERED** | No-modregning decision visibility and operational reason are both covered. |
| FR-09 Worklist inspection surface | Petition FR-09; Outcome AC-09; Architecture S3 | Scenario `Caseworkers can inspect the rule path and audit details for an ordering decision` | Debt-service AC-09; Caseworker-portal AC-09 | **COVERED** | Ordering mode, legal reference, and visible worklist basis are covered. |
| FR-10 Traceable ordering log | Petition FR-10; Outcome AC-10; Architecture S1 + S4 | Scenario `Caseworkers can inspect the rule path and audit details for an ordering decision` | Debt-service AC-10; Caseworker-portal AC-10 | **COVERED** | Timestamp, origin, rule-path, and technical-ID-only inspection are covered. |
| NFR-01 Deterministic ordering on identical input | Petition NFR-01; Architecture 3.1 deterministic section-50 owner | Scenario `Identical input produces the same ordering without manual override` | Debt-service petition-specific NFR `P060-NFR-01` | **COVERED** | The focused extra scenario asserts repeat evaluation on identical input without override and requires the same ranked order plus the same default rule path. |
| NFR-02 Auditable override/discretionary decisions | Petition NFR-02; Outcome AC-08/AC-10; Architecture S1 + S4 | Scenarios `Special circumstances override...`, `Suspected data error...`, `Expedited voluntary-payment deviation...`, `Partial or no modregning...`, `Caseworkers can inspect...` | Debt-service `P060-NFR-02` | **COVERED** | Audit-visible legal reference, timestamp/origin, and recorded reasons are all exercised. |
| NFR-03 Technical IDs only / no PII | Petition NFR-03; Outcome AC-10; Architecture S3 + GDPR boundary | Scenario `Caseworkers can inspect the rule path and audit details for an ordering decision` | Debt-service `P060-NFR-03` | **COVERED** | The inspection scenario explicitly asserts technical identifiers only. |

### Flagged Gaps
- None.

### Flagged Overreach
- None.
- The one extra debt-service scenario is justified by explicit petition/spec requirement `NFR-01` and is therefore not invented scope.

### Warnings
- The generated package is still a RED package of failing step definitions; this audit only validates traceability and dry-run discoverability.
- Portal-oriented observable behavior is currently represented inside the debt-service BDD package rather than a dedicated `opendebt-caseworker-portal` BDD artefact. That is traceable enough for this audit, but the eventual green implementation may need module-local portal tests.

### Blocking Issues
- 0 blocking coverage gaps remain.
- 0 overreach items were found.
- 0 remaining prior-review discard/escalate blockers were found.

### Approval Decision
**PASS / APPROVED**

Reason:
- All 10 canonical functional scenarios remain covered.
- The missing `NFR-01` deterministic-ordering gap is now closed by an explicit focused extra scenario.
- Targeted dry-run discovery succeeds for the updated debt-service package.
