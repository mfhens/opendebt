# ADR 0026: Inter-Service Resilience — Circuit Breakers, Retries, and Timeouts

## Status

Accepted

## Context

OpenDebt uses synchronous REST as the primary inter-service communication pattern (ADR-0019). This creates temporal coupling: if a downstream service is unavailable, the calling service fails immediately and the in-flight request is lost.

Several places in the codebase already note this gap:

```java
// AIDEV-TODO: Add Resilience4j @CircuitBreaker and @Retry on findByOcrLine and writeDown.
// A dead debt-service must not block payment ingestion; consider a fallback queue.
```

ADR-0002 and ADR-0019 both list "circuit breakers and retries on all inter-service REST calls" as a
mitigation for the temporal coupling introduced by the synchronous orchestration model. This ADR
makes that mitigation concrete and binding.

### Failure modes in scope

| Scenario | Without resilience | With resilience |
|---|---|---|
| debt-service slow (high GC, index rebuild) | payment ingestion hangs | timeout + retry frees the thread |
| debt-service down briefly (pod restart) | all matching calls fail | circuit open → fallback; closes when service recovers |
| creditor-service flaky (network blip) | single request fails | retry with back-off succeeds on 2nd attempt |
| debt-service down for extended period | every payment call fails | circuit open, fast-fail; alerts fire before queue fills |

### Relationship to Flowable orchestration

Flowable BPMN already provides coarse-grained retry for workflow steps (job executor re-runs failed
service tasks after configurable back-off). This ADR addresses fine-grained, in-process resilience
at the HTTP client layer — complementary to, not replacing, the Flowable retry mechanism.

Flowable-managed steps retry the entire BPMN activity including all side effects. Resilience4j
retries operate within a single HTTP call and should be limited to **idempotent** operations.
Non-idempotent calls (e.g., write-down, interest recalculate) must use circuit breaker + timeout
only — **no retry**.

## Decision

We adopt **Resilience4j** as the standard resilience library for all inter-service REST clients in
OpenDebt. We apply a consistent set of patterns and configuration values across all services.

### Pattern catalogue

#### Pattern A — Read operations (safe to retry)

Applies to: `GET` calls where the operation has no side effects.

Examples: `findByOcrLine()`, person-registry lookups, creditor master data reads.

```java
@CircuitBreaker(name = "debt-service", fallbackMethod = "findByOcrLineFallback")
@Retry(name = "default-read")
@TimeLimiter(name = "default")
public List<DebtDto> findByOcrLine(String ocrLine) { ... }
```

#### Pattern B — Write operations (must NOT retry automatically)

Applies to: `POST`/`PUT` calls with side effects that are not idempotent at the network level.

Examples: `writeDown()`, `recalculateInterest()`, `transferForCollection()`.

Rationale: if the HTTP response is lost in transit (the call succeeded but the response never
arrived), a retry would double-apply the write-down or create duplicate interest recalculation
entries. Flowable's job executor handles the outer retry when the entire BPMN step fails.

```java
@CircuitBreaker(name = "debt-service", fallbackMethod = "writeDownFallback")
@TimeLimiter(name = "default")
public DebtDto writeDown(UUID debtId, BigDecimal amount) { ... }
```

#### Pattern C — Fire-and-observe calls (low criticality, log only)

Applies to: notification delivery, letter trigger, metric emission.

```java
@CircuitBreaker(name = "letter-service")
@Retry(name = "default-read")
public void triggerLetter(...) { ... }
// Fallback: log warning, return void — do not throw.
```

### Standard configuration values

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        # Open circuit after 50% failure rate over 10 calls
        slidingWindowSize: 10
        failureRateThreshold: 50
        # Wait 30s before attempting a probe request in HALF_OPEN state
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
        # Slow calls (>5s) count as failures
        slowCallDurationThreshold: 5s
        slowCallRateThreshold: 50
        # Only count these as failures (not 4xx which are business errors)
        recordExceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
          - org.springframework.web.client.ResourceAccessException
        ignoreExceptions:
          - org.springframework.web.client.HttpClientErrorException
    instances:
      debt-service:
        baseConfig: default
      payment-service:
        baseConfig: default
      case-service:
        baseConfig: default
      creditor-service:
        baseConfig: default
      person-registry:
        baseConfig: default
      letter-service:
        baseConfig: default
        # Letters are lower criticality — allow more failures before opening
        failureRateThreshold: 70

  retry:
    configs:
      default-read:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        # Max wait 4s (500 → 1000 → 2000)
        retryExceptions:
          - java.io.IOException
          - org.springframework.web.client.ResourceAccessException
        ignoreExceptions:
          - org.springframework.web.client.HttpClientErrorException
          - org.springframework.web.client.HttpServerErrorException.ServiceUnavailable

  timelimiter:
    configs:
      default:
        timeoutDuration: 5s
        cancelRunningFuture: true
```

### Fallback strategy

Fallback methods must follow this priority order:

1. **Return a safe empty value** — for reads where absence is acceptable (e.g., empty list for OCR lookup → route to manual matching).
2. **Throw a typed domain exception** — for writes where the caller (Flowable) must know the step failed so it can retry the BPMN activity.
3. **Log and return void** — for notifications/letters where failure is not blocking.

```java
// Pattern A fallback — safe empty value
private List<DebtDto> findByOcrLineFallback(String ocrLine, Exception ex) {
    log.warn("debt-service unavailable for OCR lookup {}: {}", ocrLine, ex.getMessage());
    return List.of(); // Routes payment to manual matching
}

// Pattern B fallback — fail loudly so Flowable retries the BPMN step
private DebtDto writeDownFallback(UUID debtId, BigDecimal amount, Exception ex) {
    throw new ServiceUnavailableException(
        "debt-service unavailable for write-down of " + debtId, ex);
}
```

### Where to apply

Every class annotated with `@Component` or `@Service` that makes outbound HTTP calls to another
OpenDebt service must apply these patterns. The responsibility is on the **calling** service, not
the called service.

| Client class | Service called | Pattern(s) to apply |
|---|---|---|
| `DebtServiceClient` (payment) | debt-service | A (findByOcrLine), B (writeDown, recalculateInterest) |
| `CaseServiceClient` (payment) | case-service | A |
| `DebtServiceClient` (debt) | creditor-service | A |
| `PersonRegistryClient` (all portals) | person-registry | A |
| `CreditorServiceClient` (debt) | creditor-service | A |
| `LetterServiceClient` (case) | letter-service | C |

### Observability

Resilience4j exposes Micrometer metrics out of the box. The following dashboards are required
(added to the existing Grafana stack per ADR-0024):

- Circuit breaker state per service pair (CLOSED / OPEN / HALF_OPEN)
- Retry attempt count per client method
- Time limiter timeout count per client method
- Call success rate per service pair

Alerts must fire when any circuit breaker enters OPEN state. Use the existing Prometheus/Alertmanager
stack (ADR-0024). Suggested alert name: `OpenDebtCircuitBreakerOpen`.

### Implementation steps

1. Add `resilience4j-spring-boot3` to the parent `pom.xml` dependency management.
2. Add `spring-boot-starter-aop` to each service module that uses the annotations (required by
   Resilience4j's Spring AOP proxy).
3. Apply patterns A/B/C to all client classes listed above.
4. Add the standard `resilience4j` config block to each service's `application.yml`.
5. Add the circuit breaker Grafana dashboard and Prometheus alert to the observability stack.
6. Update integration tests to verify fallback behaviour using WireMock stubs that simulate
   downstream unavailability.

## Consequences

### Positive

- Payment ingestion continues (routes to manual matching) when debt-service is temporarily down.
- Flowable BPMN steps fail fast on write operations and the job executor retries at activity level,
  preserving exactly-once semantics for financial mutations.
- Circuit breaker state is visible in Grafana before a complete outage is noticed by users.
- Consistent pattern across all services — no per-engineer ad-hoc retry logic.
- No new infrastructure: Resilience4j is a library, not a sidecar or service mesh.

### Negative

- Annotations on client methods create an AOP proxy — must ensure client beans are Spring-managed
  (they are: all annotated `@Component`).
- `@TimeLimiter` requires `CompletableFuture` return types in some configurations; can be avoided
  by using `@CircuitBreaker` + `RestClient` connect/read timeout instead of `@TimeLimiter` for
  simpler cases.
- Fallback methods must have the same signature as the primary method plus an `Exception` parameter;
  this adds boilerplate.

### Mitigations

- Use `RestClient` built-in connect and read timeouts as the primary timeout mechanism (no AOP
  needed); reserve `@TimeLimiter` for cases where a configurable, observable timeout is needed.
- A base `AbstractServiceClient` class can share the fallback boilerplate.

## Alternatives considered

| Option | Reason not chosen |
|---|---|
| **Service mesh (Istio/Linkerd)** | Adds significant infrastructure complexity; circuit breaking at the proxy layer loses Spring context needed for typed fallbacks and Flowable re-throw. Revisit if Kubernetes HDP adds mesh support. |
| **Spring Retry** | Less feature-rich than Resilience4j (no circuit breaker, no built-in Micrometer integration). Resilience4j is already the Spring Boot 3 recommended library. |
| **Rely on Flowable retry only** | Flowable retries the entire BPMN activity including multiple REST calls. A flaky single downstream service would cause all activities to retry unnecessarily. Fine-grained client-level retry is more efficient. |
| **Custom RestClient interceptor** | Re-inventing the wheel; Resilience4j is battle-tested and Spring Boot 3 auto-configures it. |
