# Petition 020: OpenTelemetry-Based Observability

## Summary

All OpenDebt microservices shall emit structured logs, metrics, and distributed traces using OpenTelemetry. The observability stack shall use open-source, vendor-neutral tooling that runs on-premise or in government cloud, supports both local development and production Kubernetes deployment, and produces actionable dashboards and alerts.

## Context and motivation

OpenDebt is a distributed microservices system with 11+ services communicating via synchronous REST (ADR-0019). In production, operators need to:

- Trace a single request across multiple services (e.g., a creditor fordring submission that flows through integration-gateway → creditor-service → debt-service → rules-engine → person-registry).
- Correlate logs from different services belonging to the same business transaction.
- Monitor JVM health, HTTP latency, error rates, and custom business metrics (e.g., fordringer submitted per hour, payment matching success rate).
- Receive alerts when services degrade or fail.

Without unified observability, diagnosing production issues in a microservices system requires manual log correlation across services, which is slow and error-prone. The Danish public sector (UFST Horizontale Driftsplatform) requires operational transparency for systems handling sensitive debt collection data.

OpenTelemetry is the CNCF standard for vendor-neutral telemetry collection. It provides a single instrumentation layer that can export to multiple backends, avoiding vendor lock-in. Spring Boot 3.3 has native support for OpenTelemetry via Micrometer Tracing and the OpenTelemetry Java agent.

## Functional requirements

1. All OpenDebt services shall produce structured JSON logs that include `traceId` and `spanId` fields when a trace context is present.
2. All OpenDebt services shall propagate W3C Trace Context headers (`traceparent`, `tracestate`) across inter-service REST calls so that a single request can be traced end-to-end.
3. All OpenDebt services shall export distributed traces via OTLP (OpenTelemetry Protocol) to a configurable trace backend.
4. All OpenDebt services shall export metrics via OTLP or Prometheus scrape endpoint, including at minimum:
   - JVM metrics (heap, GC, threads)
   - HTTP server metrics (request count, latency histogram, error rate)
   - Spring Boot Actuator health status
5. The observability stack shall include a trace visualization tool that allows operators to search traces by traceId, service name, or time range.
6. The observability stack shall include a log aggregation tool that allows operators to search and filter logs by traceId, service name, log level, or time range.
7. The observability stack shall include a metrics visualization and alerting tool that supports dashboard definitions and threshold-based alerts.
8. An OpenTelemetry Collector shall be deployed as a central telemetry relay, receiving OTLP from services and forwarding to the appropriate backends.
9. Custom business metrics shall be supported. Services shall be able to register and emit counters, gauges, and histograms for domain-specific measurements (e.g., fordring submission count, payment matching success/failure rate, rule evaluation duration).
10. The solution shall work in two deployment modes:
    - **Local development:** lightweight, single-node setup via Docker Compose additions
    - **Production Kubernetes:** scalable deployment with Kustomize overlays (consistent with ADR-0006)
11. An Architecture Decision Record (ADR) shall be written to document the chosen observability backend stack and the rationale for the selection.

## Non-functional requirements

1. Trace instrumentation overhead shall not increase p99 latency of any service endpoint by more than 5 ms under normal load.
2. The observability stack shall be entirely open-source with no vendor lock-in, consistent with the Fællesoffentlige Arkitekturprincipper (ADR-0010).
3. The solution shall run on-premise or in government cloud; it shall not require any external SaaS dependency.
4. Log output shall not contain PII (personal data). Trace spans and log messages shall reference `person_id` (UUID) only, never CPR, CVR, names, or addresses (consistent with ADR-0014).

## Constraints and assumptions

- Spring Boot 3.3 provides native Micrometer Tracing bridges for OpenTelemetry, which is the preferred instrumentation approach over the OpenTelemetry Java agent.
- The existing Actuator endpoints (health, info, prometheus, metrics) remain available and are complemented, not replaced, by OTLP export.
- The ADR for backend selection (requirement 11) is part of this petition's scope but the specific backend choice is an architectural decision to be documented there. The petition requires only that the selected tools are open-source and vendor-neutral.
- Keycloak and PostgreSQL observability (e.g., database connection pool metrics, Keycloak login latency) are desirable but not mandatory in the first iteration.
- CLS (Common Logging System) audit integration via Filebeat (already configured in opendebt-common) is a separate concern and is not replaced by this observability stack.

## Out of scope

- Application Performance Management (APM) with code-level profiling
- Synthetic monitoring and uptime checks
- Cost estimation for production infrastructure sizing
- Log retention policies and GDPR-specific log purging schedules
- Service mesh (e.g., Istio) sidecar-based telemetry
- CLS audit log replacement (CLS and observability are complementary)
