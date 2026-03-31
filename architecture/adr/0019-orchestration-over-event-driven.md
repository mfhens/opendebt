# ADR 0019: Explicit Orchestration over Event-Driven Architecture

## Status
Accepted

## Context
OpenDebt is a microservices system where services communicate via synchronous REST calls, orchestrated by the Flowable BPMN engine in case-service. As the system grows (CREMUL/DEBMUL integration, bookkeeping, retroactive corrections), the question arises whether to introduce a message broker (Kafka, RabbitMQ) for asynchronous event-driven communication between services.

Key considerations:
- Debt collection follows strict legal processes with mandated sequencing and deadlines
- All actions must be auditable and traceable for Rigsrevisionen
- The system serves ~5 million citizens via ~1.200 institutions (high-value, moderate-volume)
- Flowable already provides timer events, signal events, and async user task handling
- A bi-temporal event timeline (`debt_events`) exists within payment-service for bookkeeping replay (ADR-0018)

## Decision
We retain **explicit orchestration via Flowable BPMN and synchronous REST** as the primary inter-service communication pattern. We do **not** introduce a message broker or event-driven architecture.

### Current Communication Model
```
                    Flowable BPMN (orchestrator)
                         case-service
                        /     |      \
                 REST  /   REST|    REST\
                      /       |        \
          debt-service  payment-service  letter-service
                              |
                     integration-gateway
                              |
                        SKB (CREMUL/DEBMUL)
```

### Why Orchestration Fits This Domain

1. **Legal sequencing**: Debt collection follows a strict process (assess -> notify -> wait -> check -> escalate -> close) with legal deadlines. BPMN makes this explicit and auditable.

2. **Traceability**: With orchestration, the workflow state shows exactly where a case is, who triggered what, and why. With events, causality must be reconstructed from event streams -- harder to audit.

3. **Error handling**: When a REST call fails, the workflow pauses at that step and can be retried or escalated. Event-driven patterns require dead letter queues, compensating events, idempotency guarantees, and saga patterns.

4. **Flowable is the event processor**: It already handles timers (P30D payment deadline), signals (appeal received), and async handoff (user tasks). A message broker alongside Flowable creates two competing event systems.

5. **Scale**: High-value, moderate-volume workloads do not benefit from the throughput characteristics of event streaming.

### Where Limited Async Patterns Are Used

| Use Case | Pattern | Rationale |
|----------|---------|-----------|
| CREMUL file arrival from SKB | File polling in integration-gateway | File drops are inherently async; polling with configurable interval is sufficient |
| Bookkeeping retroactive correction | Event timeline within payment-service (`debt_events` table, ADR-0018) | Scoped event sourcing for interest recalculation replay, not cross-service |
| Payment notification to case-service | REST callback to workflow signal endpoint | Push instead of poll when needed, no broker required |

### Anti-Pattern Avoided
Running Flowable for workflow + Kafka for inter-service events + `debt_events` for bookkeeping replay would create three event systems with different consistency guarantees and a debugging nightmare.

## Consequences

### Positive
- **Simplicity**: No message broker infrastructure to operate (no Kafka/ZooKeeper/RabbitMQ cluster)
- **Auditability**: Flowable's `ACT_HI_*` tables provide complete workflow execution history
- **Debuggability**: Synchronous calls are easier to trace via logs and correlation IDs
- **Consistency**: No eventual consistency challenges across services
- **Fewer failure modes**: No message ordering, deduplication, or dead letter queue concerns

### Negative
- **Temporal coupling**: Calling service must be available when called (mitigated by retries and circuit breakers)
- **No fire-and-forget**: Every call blocks until response (acceptable for this volume)
- **Scaling ceiling**: If volume increases dramatically, synchronous calls may become a bottleneck

### Mitigations
- Circuit breakers and retries on all inter-service REST calls
- Health checks and readiness probes in Kubernetes for service availability
- If scaling demands change in the future, events can be introduced incrementally for specific high-volume flows without replacing the orchestration model

## Alternatives Considered

| Option | Reason Not Chosen |
|--------|-------------------|
| Kafka for all inter-service communication | Adds operational complexity (broker cluster, schema registry, consumer groups) without solving a current problem. Creates dual orchestration with Flowable. |
| RabbitMQ for async notifications | Lighter than Kafka but still an extra infrastructure component. REST callbacks achieve the same for current notification needs. |
| Full event sourcing (all services) | Powerful but extremely high implementation cost. The scoped event sourcing in payment-service (ADR-0018) covers the main use case (retroactive corrections). |
| CQRS pattern | Adds read/write model separation complexity. Not justified by current query patterns. |
