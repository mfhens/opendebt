# OpenDebt — Kubernetes Deployment Guide

This directory contains Kustomize manifests for deploying OpenDebt to Kubernetes.
The structure follows a base + overlays pattern suitable for staging and production.

```
k8s/
├── base/                   # Environment-agnostic manifests
│   ├── configmap.yaml      # Shared config (service URLs, OTLP endpoint, Java opts)
│   ├── namespace.yaml
│   ├── service-account.yaml
│   ├── case-service/
│   ├── debt-service/
│   ├── payment-service/
│   ├── letter-service/
│   ├── person-registry/
│   ├── creditor-service/
│   ├── creditor-portal/
│   ├── caseworker-portal/
│   ├── citizen-portal/
│   ├── pgbackrest/         # PostgreSQL backup CronJobs
│   └── keycloak-backup/    # Keycloak realm backup CronJob
└── overlays/
    ├── staging/            # 1 replica, staging image tags, staging Keycloak URL
    └── production/         # 3 replicas, higher memory limits, production Keycloak URL
```

## Prerequisites — External Infrastructure

OpenDebt's Kubernetes manifests deploy **only the application tier**.
The following infrastructure components must be provisioned externally before deploying.

### PostgreSQL

Use a managed PostgreSQL 16+ service. Recommended:

| Platform | Service |
|----------|---------|
| Azure | Azure Database for PostgreSQL Flexible Server |
| AWS | Amazon RDS for PostgreSQL |
| On-prem / UFST | CloudNativePG operator |

Each service needs its own database. Create them with:

```sql
CREATE DATABASE opendebt_person;
CREATE DATABASE opendebt_case;
CREATE DATABASE opendebt_debt;
CREATE DATABASE opendebt_creditor;
CREATE DATABASE opendebt_payment;
CREATE DATABASE opendebt_letter;
CREATE DATABASE opendebt_offsetting;
CREATE DATABASE opendebt_wage_garnishment;
```

The pgbackrest CronJobs in `base/pgbackrest/` handle weekly full + daily differential backups
of the PostgreSQL instance. They require a `pgbackrest-secrets` Secret — see below.

### Keycloak

Deploy Keycloak 24+ externally (managed service or standalone cluster). The application
services validate JWTs against `KEYCLOAK_ISSUER_URI`; the portals use OAuth2 client
credentials. Override the issuer URL per environment in the overlay's `configMapGenerator`.

The CronJob in `base/keycloak-backup/` exports the realm configuration for disaster recovery.

### immudb

immudb (tamper-evidence layer, ADR-0029) is **not deployed to Kubernetes** — it is
a spike feature. `payment-service` runs with `OPENDEBT_IMMUDB_ENABLED=false` in the
base manifests. If immudb graduates to production, deploy it as an external managed
instance and enable it via overlay env override.

---

## Prerequisites — Observability

The application services export telemetry via OpenTelemetry Protocol (OTLP) to
`OTEL_EXPORTER_OTLP_ENDPOINT` (set in `base/configmap.yaml`). All pods expose
`/actuator/prometheus` for Prometheus scraping (annotations already set).

Deploy the observability stack separately using Helm. Recommended charts:

| Component | Helm chart | Notes |
|-----------|-----------|-------|
| Prometheus + Grafana | [kube-prometheus-stack](https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack) | Includes AlertManager, Grafana, node-exporter, kube-state-metrics |
| Loki + Promtail | [grafana/loki-stack](https://github.com/grafana/helm-charts/tree/main/charts/loki-stack) | Promtail runs as DaemonSet, reads pod logs from `/var/log/pods` |
| Tempo | [grafana/tempo](https://github.com/grafana/helm-charts/tree/main/charts/tempo) | Distributed tracing backend |
| OTel Collector | [opentelemetry-collector](https://github.com/open-telemetry/opentelemetry-helm-charts) | Receives OTLP from services, fans out to Tempo + Loki + Prometheus |

Quick-start (cluster-internal observability namespace):

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts
helm repo update

helm install kube-prom prometheus-community/kube-prometheus-stack \
  --namespace observability --create-namespace

helm install loki grafana/loki-stack \
  --namespace observability \
  --set promtail.enabled=true

helm install tempo grafana/tempo \
  --namespace observability

helm install otel open-telemetry/opentelemetry-collector \
  --namespace observability \
  --set mode=deployment
```

Then update `OTEL_EXPORTER_OTLP_ENDPOINT` in your overlay's `configMapGenerator`:

```yaml
# overlays/staging/kustomization.yaml
configMapGenerator:
  - name: opendebt-config
    behavior: merge
    literals:
      - OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-opentelemetry-collector.observability:4318
```

For **Azure**, consider:
- Azure Monitor managed Prometheus + Grafana (no Helm required)
- Azure Monitor OpenTelemetry Distro as the collector sidecar

---

## Secrets

All deployments reference an `opendebt-secrets` Kubernetes Secret. Create it before
deploying (never commit secrets to git):

```bash
kubectl create secret generic opendebt-secrets \
  --namespace opendebt \
  --from-literal=DATABASE_USERNAME=opendebt \
  --from-literal=DATABASE_PASSWORD=<password> \
  --from-literal=ENCRYPTION_KEY=<base64-AES-256-key> \
  --from-literal=KEYCLOAK_CREDITOR_PORTAL_CLIENT_SECRET=<secret> \
  --from-literal=KEYCLOAK_CASEWORKER_PORTAL_CLIENT_SECRET=<secret> \
  --from-literal=TASTSELV_ISSUER_URI=https://nemlogin.dk/... \
  --from-literal=TASTSELV_AUTH_URI=https://nemlogin.dk/.../auth \
  --from-literal=TASTSELV_TOKEN_URI=https://nemlogin.dk/.../token \
  --from-literal=TASTSELV_USERINFO_URI=https://nemlogin.dk/.../userinfo \
  --from-literal=TASTSELV_JWK_URI=https://nemlogin.dk/.../certs \
  --from-literal=TASTSELV_CLIENT_SECRET=<secret>
```

> **ENCRYPTION_KEY**: Must be a 32-byte key encoded as Base64. All personal data
> (CPR, CVR, names, addresses) in `person-registry` is AES-256-GCM encrypted with
> this key. **Losing it means losing all PII** — store it in Azure Key Vault or
> equivalent, and rotate via the key-rotation runbook before retiring the old key.

The pgbackrest CronJobs use a separate `pgbackrest-secrets` Secret — see
`base/pgbackrest/configmap.yaml` for the required keys.

---

## Deploying

```bash
# Staging
kubectl apply -k k8s/overlays/staging

# Production
kubectl apply -k k8s/overlays/production

# Dry-run (preview rendered manifests)
kubectl kustomize k8s/overlays/staging | less
```

## Port reference

| Service | Port | Context path |
|---------|------|-------------|
| case-service | 8081 | /case-service |
| debt-service | 8082 | /debt-service |
| payment-service | 8083 | /payment-service |
| letter-service | 8084 | /letter-service |
| creditor-portal | 8085 | /creditor-portal |
| citizen-portal | 8086 | /borger |
| caseworker-portal | 8087 | /caseworker-portal |
| wage-garnishment-service | 8088 | /wage-garnishment-service |
| integration-gateway | 8089 | /integration-gateway |
| person-registry | 8090 | /person-registry |
| rules-engine | 8091 | /rules-engine |
| creditor-service | 8092 | /creditor-service |
