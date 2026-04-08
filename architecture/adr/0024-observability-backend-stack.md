# ADR-0024: Observability Backend Stack (OpenTelemetry)

## Status

Accepted

## Date

2026-03-15

## Context

OpenDebt is a distributed microservices system with 11+ services communicating via synchronous REST (ADR-0019). Petition 020 mandates unified observability across all services: structured JSON logs with trace correlation, distributed traces, JVM/HTTP metrics, and custom business metrics. The observability stack must be entirely open-source, vendor-neutral, run on-premise or in government cloud, and work in both local development (Docker Compose) and production (Kubernetes with Kustomize).

### Requirements driving this decision

1. **Three pillars of observability** — traces, logs, and metrics must be collected, stored, and queryable through visualization UIs.
2. **OpenTelemetry Protocol (OTLP)** as the primary ingestion protocol, with an OTel Collector as the central telemetry relay.
3. **Spring Boot 3.3 native Micrometer Tracing** bridges preferred over the OTel Java agent (petition constraint).
4. **Existing Actuator prometheus/metrics endpoints** remain; the stack must support Prometheus scrape in addition to OTLP push.
5. **No PII in telemetry** (ADR-0014) — all person references use `person_id` UUID only.
6. **CLS audit integration via Filebeat** (ADR-0022) is a separate concern and must not be replaced.
7. **Resource footprint matters** — the local dev stack runs alongside 11+ application services, PostgreSQL 16, and Keycloak 24 on a single developer machine.
8. **Fællesoffentlige Arkitekturprincipper** (ADR-0010) — openness, no vendor lock-in, interoperability, standard protocols.

### Evaluation criteria

| Criterion | Weight | Description |
|-----------|--------|-------------|
| OTLP-native ingestion | High | Direct OTLP support without protocol translation layers |
| Resource footprint (dev) | High | RAM/CPU on a single developer machine running Docker Compose |
| Operational complexity | High | Number of components to deploy, configure, and maintain |
| Single-pane-of-glass | Medium | Ability to correlate traces, logs, and metrics in one UI |
| Spring Boot integration maturity | Medium | Micrometer/OTel exporter support, documentation quality |
| Community adoption and longevity | Medium | CNCF status, GitHub stars, release cadence, support community |
| Danish public-sector precedent | Low | Proven deployments in Danish government infrastructure |
| Kubernetes-native operations | Medium | Helm charts, operators, Kustomize compatibility |

## Options Evaluated

### Option A: Grafana Stack (Grafana + Prometheus + Loki + Tempo)

**Components:**
- **Grafana** — unified dashboard and alerting UI for all three pillars
- **Prometheus** — metrics storage and querying (PromQL)
- **Loki** — log aggregation (LogQL), label-indexed, low-footprint
- **Tempo** — distributed trace storage (TraceQL), OTLP-native

**Architecture:**

```
Services → OTLP → OTel Collector → Tempo   (traces)
                                 → Loki    (logs via OTLP or Loki exporter)
                                 → Prometheus (metrics via remote-write or scrape)
                        Grafana ← queries all three backends
```

**Strengths:**
- Grafana provides genuine single-pane-of-glass: traces, logs, and metrics in one UI with cross-linking (click from trace span → correlated logs, click from metric → exemplar traces).
- All components are CNCF or Grafana Labs open-source (Apache 2.0 / AGPL-3.0 for Grafana OSS).
- Tempo is OTLP-native — no protocol translation needed for trace ingestion.
- Loki is lightweight compared to Elasticsearch — it indexes labels only, not full text, resulting in 10-20× less storage than ELK for equivalent log volumes.
- Prometheus is the de-facto standard for Kubernetes metrics; Spring Boot Actuator has first-class `prometheus` endpoint support via Micrometer.
- Mature Helm charts and Kubernetes operators for production deployment.
- Grafana has the largest dashboard ecosystem (18,000+ community dashboards) including Spring Boot, JVM, and Kubernetes dashboards.
- Very active community: Grafana (65k+ GitHub stars), Prometheus (CNCF graduated), Loki (24k+ stars), Tempo (4k+ stars).

**Weaknesses:**
- Four separate components (Grafana, Prometheus, Loki, Tempo) plus the OTel Collector — more moving parts than an all-in-one solution.
- Loki's label-only indexing means full-text search across log content is slower than Elasticsearch for ad-hoc queries.
- Grafana AGPL-3.0 license requires that modifications to Grafana itself are open-sourced (this does not affect OpenDebt since we use Grafana as-is, not as an embedded library).

**Resource footprint (Docker Compose):**
- Grafana: ~100 MB RAM
- Prometheus: ~200 MB RAM (small metric volume)
- Loki: ~100 MB RAM (single binary mode)
- Tempo: ~100 MB RAM (single binary mode)
- OTel Collector: ~50 MB RAM
- **Total: ~550 MB additional RAM**

### Option B: Jaeger + ELK (Elasticsearch + Logstash + Kibana) + Prometheus

**Components:**
- **Jaeger** — distributed tracing UI and storage (CNCF graduated)
- **Elasticsearch** — log storage and full-text search
- **Logstash** or **Filebeat** — log collection and processing
- **Kibana** — log visualization and dashboards
- **Prometheus** — metrics storage and querying
- **Grafana** (optional) — metrics dashboards (Prometheus alone has no dashboard UI)

**Architecture:**

```
Services → OTLP → OTel Collector → Jaeger         (traces)
                                 → Logstash → ES  (logs)
                                 → Prometheus      (metrics)
                        Kibana ← ES (logs)
                        Jaeger UI ← traces
                        Grafana ← Prometheus (metrics)
```

**Strengths:**
- Jaeger is CNCF graduated with strong tracing-specific features (dependency graphs, compare traces).
- Elasticsearch provides powerful full-text log search with complex query DSL.
- CLS (UFST Common Logging System) is an ELK stack, so there is organizational familiarity with the ELK toolchain.
- Jaeger supports OTLP ingestion natively since v1.35.
- Most widely deployed tracing solution in CNCF surveys.

**Weaknesses:**
- **Three separate UIs** — Jaeger UI for traces, Kibana for logs, Grafana for metrics. No single-pane-of-glass; operators must context-switch between tools to correlate signals.
- **Elasticsearch is resource-heavy** — minimum 1-2 GB RAM for a single node, typically 4+ GB for production. On a developer machine already running 11+ services + PostgreSQL + Keycloak, this is a significant burden.
- **Six+ components** to deploy, configure, and maintain (Jaeger, Elasticsearch, Logstash/Filebeat, Kibana, Prometheus, Grafana).
- Elasticsearch licensing changed from Apache 2.0 to SSPL/Elastic License in 2021 (versions > 7.10). OpenSearch (Amazon fork, Apache 2.0) is the open-source alternative but adds community fragmentation.
- Jaeger is [moving its backend to OTel Collector architecture](https://www.jaegertracing.io/docs/next-architecture/) — the standalone backend is being deprecated in favor of `jaeger-v2` which is an OTel Collector distribution. This increases future migration risk for the standalone deployment model.
- Logstash adds another JVM process (1+ GB RAM) for log processing; the lighter Filebeat alternative needs pipeline configuration for OTLP → Elasticsearch routing.

**Resource footprint (Docker Compose):**
- Elasticsearch: ~1,500 MB RAM (minimum viable)
- Logstash: ~1,000 MB RAM (JVM)
- Kibana: ~500 MB RAM
- Jaeger (all-in-one): ~200 MB RAM
- Prometheus: ~200 MB RAM
- Grafana: ~100 MB RAM
- OTel Collector: ~50 MB RAM
- **Total: ~3,550 MB additional RAM**

### Option C: SigNoz (All-in-One OpenTelemetry-Native)

**Components:**
- **SigNoz** — single platform covering traces, logs, and metrics with unified UI
- Uses ClickHouse as the storage backend

**Architecture:**

```
Services → OTLP → OTel Collector → SigNoz (OTel Collector + ClickHouse + Query Service + Frontend)
```

**Strengths:**
- Purpose-built for OpenTelemetry — OTLP is the primary and only ingestion protocol.
- Single UI for traces, logs, and metrics with native correlation.
- Fewer conceptual components to understand than the Grafana stack or ELK.
- Apache 2.0 licensed (fully open source).
- Built-in alerting.

**Weaknesses:**
- **Smaller community** — ~20k GitHub stars, significantly less adoption than Grafana/Prometheus/Jaeger in CNCF ecosystem surveys.
- **ClickHouse dependency** — ClickHouse is a columnar OLAP database optimized for analytics workloads. It requires 2+ GB RAM minimum and has operational characteristics (merge trees, background compaction) that are non-trivial to operate.
- **No Danish public-sector precedent** — no known deployments in Danish government infrastructure.
- **Spring Boot ecosystem integration** — fewer pre-built dashboards and community resources compared to the Grafana+Prometheus ecosystem which has extensive Spring Boot and JVM dashboard libraries.
- **Vendor concentration risk** — SigNoz Inc. is a VC-funded startup. While the code is Apache 2.0, the project's roadmap and maintenance depend on a single company. Grafana Labs and the Prometheus/CNCF community are more diversified.
- **Kubernetes operator maturity** — SigNoz Helm charts exist but are less battle-tested than the Grafana/Prometheus operator ecosystem.
- **Local dev footprint** — ClickHouse + SigNoz services require ~2 GB RAM minimum, more than the Grafana stack's single-binary mode.

**Resource footprint (Docker Compose):**
- ClickHouse: ~1,500 MB RAM
- SigNoz OTel Collector: ~100 MB RAM
- SigNoz Query Service: ~200 MB RAM
- SigNoz Frontend: ~100 MB RAM
- **Total: ~1,900 MB additional RAM**

## Decision Matrix

| Criterion | Weight | Option A: Grafana Stack | Option B: Jaeger+ELK | Option C: SigNoz |
|-----------|--------|------------------------|----------------------|-------------------|
| OTLP-native ingestion | High | ★★★★☆ Tempo OTLP-native; Loki/Prom need exporter config | ★★★☆☆ Jaeger OTLP ok; ES needs Logstash pipeline | ★★★★★ Built for OTLP |
| Resource footprint (dev) | High | ★★★★★ ~550 MB (single-binary modes) | ★★☆☆☆ ~3,550 MB (ES+Logstash heavy) | ★★★☆☆ ~1,900 MB (ClickHouse) |
| Operational complexity | High | ★★★★☆ 4 components, well-documented | ★★☆☆☆ 6+ components, 3 UIs | ★★★★☆ Fewer components, less docs |
| Single-pane-of-glass | Medium | ★★★★★ Grafana correlates all three | ★★☆☆☆ Three separate UIs | ★★★★★ Native unified UI |
| Spring Boot integration | Medium | ★★★★★ Micrometer + Prometheus = first-class | ★★★★☆ Good but fragmented | ★★★☆☆ Works but fewer resources |
| Community & longevity | Medium | ★★★★★ CNCF graduated (Prometheus), massive community | ★★★★☆ CNCF graduated (Jaeger), but ES licensing issues | ★★★☆☆ Smaller, single-company risk |
| Danish public-sector precedent | Low | ★★★★☆ Prometheus/Grafana widely used in Nordic public infra | ★★★★★ CLS is ELK; Jaeger used in some agencies | ★☆☆☆☆ No known precedent |
| K8s-native operations | Medium | ★★★★★ Mature operators, Helm, kube-prometheus-stack | ★★★★☆ Good Helm charts for each component | ★★★☆☆ Helm charts exist, less mature |

## Decision

**We adopt Option A: Grafana Stack (Grafana + Prometheus + Loki + Tempo)** as the observability backend for OpenDebt.

### Rationale

1. **Lowest resource footprint for local development.** At ~550 MB additional RAM in single-binary mode, the Grafana stack is the only option that does not significantly burden a developer machine already running 11+ services, PostgreSQL, and Keycloak. Option B (ELK) requires ~3.5 GB and Option C (SigNoz/ClickHouse) requires ~1.9 GB.

2. **True single-pane-of-glass.** Grafana's data source model allows correlating traces (Tempo), logs (Loki), and metrics (Prometheus) in a single UI. An operator can click from a Prometheus alert → exemplar trace in Tempo → correlated logs in Loki without leaving the dashboard. This directly addresses the petition's requirement for actionable observability.

3. **Best Spring Boot ecosystem integration.** Spring Boot 3.3's Micrometer already exports a Prometheus-compatible `/actuator/prometheus` endpoint. The `micrometer-tracing-bridge-otel` sends traces to Tempo via OTLP. Grafana has hundreds of community dashboards for Spring Boot and JVM metrics. This is the most natural fit for the existing technology stack.

4. **Strongest community and longevity.** Prometheus is CNCF graduated. Grafana, Loki, and Tempo are backed by Grafana Labs (publicly funded, profitable company) with large open-source communities. The risk of project abandonment is minimal.

5. **CLS coexistence.** CLS (ADR-0022) uses ELK for audit logs shipped via Filebeat. The Grafana stack operates alongside CLS without conflict — CLS handles audit compliance, while Loki/Grafana handles operational log search and trace correlation. These are complementary, not competing concerns.

6. **Fællesoffentlige Arkitekturprincipper compliance** (ADR-0010). All components are open-source with permissive or copyleft licenses. The stack uses CNCF standard protocols (OTLP, PromQL). There is no vendor lock-in — each component can be replaced independently (e.g., swap Tempo for Jaeger, swap Loki for Elasticsearch) because the OTel Collector acts as a routing layer.

### Why not Option B (Jaeger + ELK + Prometheus)?

While Jaeger is CNCF graduated and CLS familiarity with ELK exists, the combined resource footprint (~3.5 GB) is prohibitive for local development. Three separate UIs (Jaeger, Kibana, Grafana) fragment the operator experience. Elasticsearch's licensing situation (SSPL/Elastic License post-7.10) conflicts with the openness principle. Jaeger's own roadmap is converging on OTel Collector as its backend, reducing the value of a standalone Jaeger deployment.

### Why not Option C (SigNoz)?

SigNoz is architecturally elegant but carries vendor concentration risk (single VC-funded company), has no Danish public-sector precedent, and its ClickHouse dependency adds operational complexity and ~1.9 GB RAM for local development. The smaller community means fewer pre-built dashboards and less Spring Boot-specific documentation.

## Consequences

### Positive

1. **Unified observability** — operators can trace requests end-to-end across all OpenDebt services, correlate with logs, and monitor metrics from a single Grafana dashboard.
2. **Low barrier to local development** — the lightweight stack does not overwhelm developer machines.
3. **Standards-based** — OTLP, PromQL, LogQL, and TraceQL are open standards. Instrumentation is portable to any OTLP-compatible backend if the organization's needs change.
4. **Existing Actuator endpoints preserved** — Prometheus scrapes the existing `/actuator/prometheus` endpoint; no breaking changes to service configuration.
5. **Complementary to CLS** — CLS audit logging via Filebeat continues unchanged alongside operational observability.

### Negative

1. **Four backend components** — Grafana, Prometheus, Loki, and Tempo must be deployed and maintained in addition to the OTel Collector. This is more operational surface than a single-binary solution.
2. **Loki full-text search limitations** — Loki indexes labels (service name, trace ID, log level) but not full log content. Complex text searches across log bodies are slower than Elasticsearch. This is acceptable for trace-correlated log lookup but may require adjustment if ad-hoc log forensics become a primary use case.
3. **Additional Kubernetes manifests** — production deployment requires Kustomize overlays for five new components (OTel Collector, Grafana, Prometheus, Loki, Tempo).

### Mitigations

- Use Grafana stack **single-binary modes** (monolithic) for local development to minimize container count and RAM.
- Use Grafana stack **microservices/distributed modes** for production if scale demands it.
- If full-text log search becomes critical, Loki can be complemented with or replaced by an Elasticsearch target on the OTel Collector without changing service instrumentation.

## Related ADRs

- ADR-0002: Microservices Architecture
- ADR-0003: Java/Spring Boot Technology Stack
- ADR-0006: Kubernetes Deployment
- ADR-0010: Fællesoffentlige Arkitekturprincipper Compliance
- ADR-0014: GDPR Data Isolation — Person Registry
- ADR-0019: Orchestration over Event-Driven Architecture
- ADR-0022: Shared Audit Infrastructure

## References

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Grafana OSS](https://grafana.com/oss/)
- [Prometheus — CNCF Graduated](https://prometheus.io/)
- [Grafana Loki](https://grafana.com/oss/loki/)
- [Grafana Tempo](https://grafana.com/oss/tempo/)
- [Spring Boot Observability with Micrometer](https://docs.spring.io/spring-boot/reference/actuator/observability.html)
- [Petition 020: OpenTelemetry-Based Observability](../petitions/petition020-opentelemetry-observability.md)
