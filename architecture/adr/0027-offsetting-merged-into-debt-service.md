# ADR 0027: Merge Offsetting (Modregning) into the Debt Service

## Status

Accepted

## Context

OpenDebt initially scaffolded offsetting (modregning) as a standalone microservice
(`opendebt-offsetting-service`, port 8087). The rationale was that each collection
mechanism would have its own service to allow independent deployment and scaling.

When evaluated against the actual domain model and operational requirements, the
offsetting service turns out not to justify a separate process boundary:

- **Skeleton only.** The service contained only a Spring Boot main class, an
  architecture test, and `application.yml` configuration. No entities, repositories,
  controllers, or business logic had been implemented.

- **No unique external integration.** Offsetting operates entirely within the
  OpenDebt domain — it reads debt balances, matches them against incoming payments,
  and updates debt records. It does not communicate with any external system outside
  of the OpenDebt service mesh.

- **Transactional consistency requirement.** The core offsetting operation —
  reducing an outstanding debt balance by the offset amount and recording the
  offsetting event — must be atomic. A `SET_OFF` collection measure is already
  tracked in `CollectionMeasureEntity` within the debt service. Executing the
  balance write-down across a service boundary requires either a distributed saga or
  a two-phase approach; both add complexity without architectural benefit.

- **Identical dependency set.** The offsetting service's planned dependencies
  (debt-service, case-service, payment-service, letter-service) were a superset of
  the debt service's existing dependencies. The debt service already owns the
  `CollectionMeasureEntity` with a `SET_OFF` type — the natural home for offsetting
  execution is alongside the debt lifecycle it operates on.

- **High inter-service call volume.** An offsetting operation in a separate service
  would make multiple synchronous calls back to the debt service for balance reads
  and write-downs per offsetting cycle. This chatty coupling is a known microservices
  anti-pattern when services share the same bounded context.

Contrast with `opendebt-wage-garnishment-service`, which is deliberately kept
separate because it integrates with EINDKOMST and A-melding — Danish government
payroll registries that impose their own compliance, security, and deployment
requirements. That external boundary justifies the operational overhead of a separate
service.

## Decision

We merge the offsetting domain into `opendebt-debt-service`.

- The `opendebt-offsetting-service` Maven module is removed from the parent POM and
  its source directory deleted.
- Offsetting business logic will be implemented in a dedicated package:
  `dk.ufst.opendebt.debtservice.offsetting`.
- The offsetting domain configuration (eligible payment types, priority rules,
  notification lead time) is moved into the debt service's `application.yml` under
  `opendebt.offsetting`.
- The Prometheus scrape target for `offsetting-service` is removed; offsetting
  metrics are covered by the existing `debt-service` scrape job.
- The `SET_OFF` collection measure type already present in `CollectionMeasureEntity`
  becomes the integration point between the debt lifecycle and the offsetting logic.

## Consequences

### Positive

- Offsetting balance write-downs are transactional: a single JPA `@Transactional`
  boundary covers the debt balance update and the `CollectionMeasureEntity` record.
  No saga or compensating transaction is needed.
- One fewer service to build, deploy, monitor, and maintain. Operational overhead
  reduction is meaningful for a small team.
- No network hops between offsetting logic and the debt repository it reads from and
  writes to.
- Consistent with how interest accrual and readiness validation already live in the
  debt service despite being separate business processes.

### Negative

- The debt service grows in scope. Future team expansion may want to extract
  offsetting again if the service becomes a deployment bottleneck. This ADR should be
  revisited when offsetting is fully implemented and team topology is known.
- Offsetting batch jobs will share the debt service's scheduler thread pool and
  deployment lifecycle with the core debt API. A noisy offsetting batch cannot be
  scaled independently.

### Mitigations

- The `dk.ufst.opendebt.debtservice.offsetting` package provides a clean internal
  module boundary. If extraction is needed later, the package maps directly to a new
  module with minimal refactoring.
- The existing `ArchUnit` test (`SharedArchRules.noAccessToOtherServiceRepositories`)
  is adapted to cover the offsetting package within the debt service boundary.
- The debt service's batch job configuration (`spring.task.scheduling`) should be
  reviewed when offsetting batch jobs are added, to ensure fair thread allocation.

## Alternatives considered

| Option | Reason not chosen |
|---|---|
| **Keep as separate service** | Requires a distributed saga for atomic balance write-down. No external integration justifies the boundary. High inter-service coupling for what is operationally the same bounded context. |
| **Merge into payment-service** | Payment service ingests incoming payments; offsetting is a debt lifecycle action triggered by those payments. The debt service already owns the debt lifecycle, making it the correct target. |
| **Merge into case-service** | Case service orchestrates collection workflows (ADR-0019) but does not own debt balances. Offsetting execution needs direct access to the debt repository. |
| **Keep both and add anti-corruption layer** | Adds indirection without solving the transactional consistency problem. |
