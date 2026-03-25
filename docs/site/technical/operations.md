# Operations Guide

## Deployment

OpenDebt is deployed on Kubernetes (ADR-0006). Manifests are in the `k8s/` directory.

### Prerequisites

- Kubernetes cluster (1.28+)
- PostgreSQL 16 (managed or self-hosted)
- Keycloak 24+ for authentication
- Grafana stack for observability

### Configuration

Each service is configured via environment variables and Spring profiles:

| Variable | Purpose | Example |
|----------|---------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod` |
| `KEYCLOAK_ISSUER_URI` | Keycloak realm URL | `https://auth.opendebt.dk/realms/opendebt` |
| `DEBT_SERVICE_URL` | Debt service base URL | `http://debt-service:8082` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OpenTelemetry collector | `http://tempo:4318` |

Service URLs follow the pattern `{SERVICE_NAME}_URL` (e.g., `CREDITOR_SERVICE_URL`, `CASE_SERVICE_URL`).

## Observability (ADR-0024)

### Stack

| Component | Purpose | Port |
|-----------|---------|------|
| Grafana | Dashboards and visualization | 3000 |
| Prometheus | Metrics collection | 9090 |
| Loki | Log aggregation | 3100 |
| Tempo | Distributed tracing | 4318 |

### Health checks

Every service exposes Spring Actuator endpoints:

```
GET /actuator/health          # Health status
GET /actuator/prometheus      # Prometheus metrics
GET /actuator/info            # Build info
```

### Log analysis

All services emit structured JSON logs via Logstash Logback encoder. Query logs in Grafana via Loki:

```logql
{service="opendebt-debt-service"} |= "ERROR"
```

### Distributed tracing

Services propagate W3C `traceparent` headers via injected `WebClient.Builder` (ADR-0024). View traces in Grafana via Tempo.

### RBAC authorization dashboard

Grafana provisions an RBAC-specific dashboard from `config/grafana/provisioning/dashboards/opendebt-rbac-authorization.json` when the observability stack starts.

The dashboard is built around these signals:

- `authorization_denied_total{role,resource_type}` for denial-rate and unauthorized-attempt panels
- `authorization_check_seconds_bucket{role,resource_type}` for p50, p95, and p99 authorization latency
- `resilience4j_circuitbreaker_state{name="person-registry",state="open"}` for person-registry lookup resilience

Alert templates are provisioned from `config/grafana/provisioning/alerting/opendebt-rbac-authorization-alerts.yaml`:

- `HighAuthorizationDenialRate`
- `PersonRegistryCircuitBreakerOpen`

If you run Grafana outside the provided Compose stack, import the dashboard JSON manually and ensure the Prometheus datasource uses UID `prometheus`.

## Database management

Each service owns its own PostgreSQL database. Schema migrations use Flyway and are applied automatically on startup.

| Service | Database |
|---------|----------|
| debt-service | `opendebt_debt` |
| case-service | `opendebt_case` |
| payment-service | `opendebt_payment` |
| creditor-service | `opendebt_creditor` |
| person-registry | `opendebt_person` |

## Incident response

1. Check Grafana dashboards for service health
2. Query Loki for error logs: `{level="ERROR"}`
3. Trace request flow in Tempo using correlation ID
4. Check PostgreSQL connection pool metrics in Prometheus
5. Review Keycloak token issuance if authentication failures spike

## Disaster Recovery (ADR-0028)

**Targets: RTO 4 hours / RPO 4 hours**

| Failure class | Recovery mechanism | Expected time |
|--------------|-------------------|---------------|
| Pod / node failure | Kubernetes self-healing | 2–5 min |
| DB primary failure (standby available) | Standby promotion | 15–30 min |
| DB data loss / corruption | pgBackRest PITR restore | 90–150 min |
| Catastrophic cluster loss | Re-provision + pgBackRest restore | 2–4 hours |

### Backup components

- **pgBackRest**: Continuous WAL archiving (~5-min segments) to Azure Blob Storage + weekly full backup + daily differential
- **Streaming replication**: Hot standby on a second PostgreSQL instance
- **Encryption key**: `ENCRYPTION_KEY` stored in Azure Key Vault — never in git or env vars
- **Keycloak**: Nightly realm export + Keycloak DB backed up via pgBackRest

### Backup monitoring alerts

- `PostgreSQLBackupStale` — fires if backup is >24 hours old (critical)
- `WALArchivingLagging` — fires if WAL archiving lags >15 minutes (warning)
- `EncryptionKeyVaultUnreachable` — fires if key vault is unreachable (critical)

For the step-by-step DR execution procedure see `docs/dr-runbook.md` in the repository.
