# ADR 0035: Embed Drools as Shared In-Process Library (`ufst-rules-lib`)

## Status

Accepted — supersedes ADR-0015 (Drools Rules Engine)

## Context

ADR-0015 adopted Drools as a **standalone HTTP service** (`opendebt-rules-engine`, port 8091).
That decision rested on three assumptions that have since been falsified:

| ADR-0015 Assumption | Actual Situation |
|---------------------|-----------------|
| Business analysts maintain rules via Excel decision tables | Interest rates are stored in the `business_config` table and resolved by `BusinessConfigService`. No Excel or UI rule editing is needed or planned. |
| Rules can be updated without code deployment (hot reload) | DRL files are classpath resources, not DB-stored. Any DRL change already requires a build and redeploy. |
| Rules engine handles high-volume evaluation centrally | The batch job (`InterestAccrualJobHelper`) bypasses the service with inline calculation because a per-debt HTTP call at scale is unacceptable. |

### Evidence from implementation audit (2026-04)

1. **No production HTTP calls to the rules engine exist.** The only Java reference is
   `OverpaymentRulesServiceImpl`, which is an explicit placeholder with an `AIDEV-TODO` comment
   blocked on an unmodelled sagstype field. `InterestAccrualJobHelper` and
   `InterestRecalculationServiceImpl` both calculate interest inline.

2. **Three diverging implementations of interest logic** have emerged as a consequence:
   the batch path, the retroactive recalculation path, and the DRL path. CR-001 (2026-04)
   fixed only the DRL; the batch and recalculation paths remain independent.

3. **The standalone service database is unused.** `opendebt-rules-engine` has a full
   PostgreSQL + Flyway setup that was presumably intended for rule persistence/versioning, but
   DRL files are classpath resources. The DB schema is empty beyond Flyway bookkeeping.

4. **The rate-resolution contract is caller-side anyway.** CR-001 established that callers
   must resolve `annualRate` from `BusinessConfigService` before constructing an
   `InterestCalculationRequest`. The rules engine does not and cannot know about rate configuration.
   This makes the "centralized rule governance" value hollow: the rules engine is merely a
   routing / salience layer, not a source of truth for rates.

5. **ADR-0033 (`ufst-bookkeeping-core`) provides the proven pattern.** Cross-cutting financial
   logic is packaged as a shared Maven library with no Spring dependency, versioned releases,
   and in-process evaluation. The same pattern applies to rules.

### Why the standalone service model is wrong for this domain

```
Current state (three paths, diverging):

  InterestAccrualJobHelper      ──► inline calculation
  InterestRecalculationService  ──► inline calculation
  RulesEngine HTTP API          ──► DRL evaluation (unused by callers)

Target state (one path, one library):

  InterestAccrualJobHelper      ──┐
  InterestRecalculationService  ──┤── ufst-rules-lib (in-process KieContainer)
  OverpaymentRulesService       ──┘
```

## Decision

We supersede the standalone service architecture from ADR-0015 and adopt the following:

**Drools rule models, DRL files, and the `KieContainerFactory` are extracted into a shared
Maven library `ufst-rules-lib`. All consumers evaluate rules in-process. The
`opendebt-rules-engine` standalone service is retired.**

### Library coordinates

```xml
<groupId>dk.ufst</groupId>
<artifactId>ufst-rules-lib</artifactId>
```

The module lives inside the OpenDebt repository (`ufst-rules-lib/`) as a sibling Maven module,
following the same pattern as `ufst-bookkeeping-core`. It is extractable to a dedicated UFST
platform repository when needed.

### What goes into `ufst-rules-lib`

| Component | Included | Rationale |
|-----------|----------|-----------|
| Rule model classes (`InterestCalculationRequest`, `InterestCalculationResult`, etc.) | ✅ | Shared contract between callers and DRL |
| All `.drl` files | ✅ | Version-locked with the model classes that the rules reference |
| `KieContainerFactory` | ✅ | Spring-agnostic; each consumer wires it as a `@Bean` |
| `RulesEngineService` facade | ✅ | Thin wrapper: build session, insert facts, fire, return result |
| `RulesTestHarness` (test-jar) | ✅ | Helps consumers write rules integration tests without KIE boilerplate |
| `BusinessConfigService` rate resolution | ❌ | Per-service concern; callers pass resolved rates into request objects |
| Spring Boot / web layer | ❌ | Consumers own their Spring wiring |
| JPA / database | ❌ | Not needed; no rule persistence required |

### In-process evaluation contract

Callers resolve all external values (rates, rule codes, balances) from their own services
before constructing a request. The library evaluates salience and routing logic only.

```
Caller (e.g. InterestAccrualJobHelper)
  │
  ├── resolveConfigKey(debt)              ← local to caller
  ├── getDecimalValue(key, accrualDate)   ← BusinessConfigService (caller's dependency)
  │
  └── rulesEngineService.evaluate(
        InterestCalculationRequest.builder()
          .interestRule(ruleCode.name())
          .annualRate(resolvedRate)
          .principalAmount(balance)
          .daysCalculated(1)
          .build()
      )
  ──► InterestCalculationResult
```

### KieContainer lifecycle

The `KieContainer` is built **once at application startup** and held as a `@Bean singleton`.
Each evaluation creates and disposes a `KieSession` (stateless, thread-safe pattern).

```java
@Bean
public KieContainer kieContainer() {
    return KieContainerFactory.buildFromClasspath();  // loads all .drl from rules/
}
```

### DRL versioning policy

DRL changes require:
1. A new `ufst-rules-lib` release (semantic versioning; breaking DRL changes → major version bump)
2. Coordinated update of all consuming services to the new library version

This is acceptable: interest rules change at known schedule boundaries (Nationalbanken rate
announcements, legislative changes) — not ad-hoc. Coordinated release is operationally safe.

### Overpayment rules (future)

When sagstype and frivillig indbetaling classification is formally specified, the overpayment
DRL goes into `ufst-rules-lib`, **not** into a new standalone service. The
`OverpaymentRulesServiceImpl` placeholder will be wired to `RulesEngineService` from the library.

### Retirement of `opendebt-rules-engine`

| Artefact | Disposition |
|----------|-------------|
| Spring Boot application, REST controllers | Remove |
| PostgreSQL / Flyway setup | Remove (DB was never used for DRL storage) |
| `k8s/base/configmap.yaml` → `RULES_ENGINE_URL` | Remove |
| Docker-compose entries | Remove |
| Kubernetes manifests for the service | Remove |
| Maven module declaration in root `pom.xml` | Remove after migration |

## Consequences

### Positive

- **Eliminates the three-path divergence.** All interest calculation paths use the same
  in-process `KieContainer` and the same DRL. A rule change is one change in one place.
- **No network hop, no latency concern.** The performance constraint that forced the batch job
  bypass is removed at its root.
- **No SPOF.** Rule evaluation cannot fail due to a remote service being down.
- **Reduced infrastructure.** One less service to deploy, monitor, scale, and maintain in K8s.
- **Consistent with ADR-0033 precedent.** Cross-cutting UFST financial logic lives in shared
  libraries, not in separate services.

### Negative

- **DRL changes require a versioned library release and coordinated consumer redeploy.**
  This replaces the theoretical "hot reload" capability of the standalone service.
  In practice, DRL changes always required a code change already (classpath resources).
- **~40 MB Drools transitive dependency added per consuming service.**
  Acceptable given modern container image layering (layer is shared across pods).
- **Library release discipline required.** The same governance applied to `ufst-bookkeeping-core`
  must be applied here: semantic versioning, no OpenDebt-specific logic leaking into the library.

## Alternatives Considered

| Option | Reason Not Chosen |
|--------|-------------------|
| Keep standalone service | Entrenches the three-path divergence; does not solve the batch bypass; adds network dependency for no benefit |
| DB-backed hot reload (rules stored in PostgreSQL, fetched at startup) | Adds a runtime DB dependency for a use case that changes a few times per year; over-engineered; not materially different from a versioned release |
| Per-service DRL copy | Rule drift guaranteed; same bug fixed in N places; ruled out immediately |
| Rule execution as a sidecar (same pod, Unix socket) | Eliminates network latency but keeps a separate process; adds operational complexity for no benefit over in-process evaluation |

## Related ADRs

- **ADR-0015**: Original Drools decision — superseded by this ADR
- **ADR-0033**: `ufst-bookkeeping-core` shared library — the pattern this ADR follows
- **ADR-0025**: Maven as build tool — governs library release mechanics
- **ADR-0026**: Inter-service resilience — no longer applies to rules evaluation after this change

## Implementation

See **TB-057** in `petitions/program-status.yaml`.
