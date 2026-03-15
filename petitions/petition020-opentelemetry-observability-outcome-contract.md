# Petition 020 Outcome Contract

## Acceptance criteria

1. Every OpenDebt service produces structured JSON log output that includes `traceId` and `spanId` fields when processing an incoming HTTP request.
2. A REST call that traverses multiple services (e.g., creditor-portal → debt-service → rules-engine) results in a single distributed trace visible in the trace backend, with spans for each service.
3. W3C Trace Context headers (`traceparent`) are propagated on inter-service REST calls made via WebClient or RestTemplate.
4. Every service exports JVM metrics (heap usage, GC count, thread count) and HTTP server metrics (request count, latency, error rate) that are queryable in the metrics backend.
5. An OpenTelemetry Collector is deployed and receives OTLP telemetry from at least two services simultaneously.
6. The trace visualization tool allows an operator to look up a trace by traceId and see the full call chain across services.
7. The log aggregation tool allows an operator to filter logs by traceId and see correlated log entries from all services involved in the request.
8. The metrics tool displays a dashboard with JVM and HTTP metrics for all instrumented services.
9. Custom business metrics can be registered and emitted by a service and appear in the metrics backend (demonstrated with at least one example counter).
10. The local development setup works via `docker-compose` additions without requiring Kubernetes.
11. The production setup is defined as Kustomize overlays consistent with existing k8s manifests.
12. An ADR is published documenting the chosen observability backend and the selection rationale.
13. No PII appears in trace span attributes, log messages, or metric labels.
14. Trace instrumentation does not increase p99 endpoint latency by more than 5 ms under normal load.

## Definition of done

- Structured JSON logging with traceId/spanId is configured for all services.
- Distributed trace propagation is verified across at least one multi-service call chain.
- Metrics export is verified for JVM and HTTP metrics on all instrumented services.
- OpenTelemetry Collector is deployed and operational in Docker Compose and K8s configurations.
- Trace, log, and metrics backends are operational and accessible via their respective UIs.
- At least one custom business metric is demonstrated end-to-end.
- ADR for backend selection is written and referenced in `docs/architecture-overview.md`.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- A multi-service request cannot be correlated end-to-end by a single traceId.
- Log output lacks traceId/spanId when a trace context is present.
- JVM or HTTP metrics are not available in the metrics backend for an instrumented service.
- PII (CPR, CVR, names, addresses) appears in trace spans, logs, or metric labels.
- The observability stack requires a commercial license or an external SaaS dependency.
- The local development setup requires Kubernetes to function.
- No ADR is produced for the backend selection decision.
