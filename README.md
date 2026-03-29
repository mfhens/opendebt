# OpenDebt

Open source system for Danish public debt collection (Offentlig Gældsinddrivelse).

[![CI](https://github.com/ufst/opendebt/actions/workflows/ci.yml/badge.svg)](https://github.com/ufst/opendebt/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Overview

OpenDebt is a modern, microservices-based debt collection system designed for Danish public institutions. It handles:

- **~600 debt types** from **~1,200 public institutions**
- Debt registration and readiness validation (indrivelsesparathed)
- Case management and workflow
- Payment processing
- Offsetting (modregning)
- Wage garnishment (loenindeholdelse)
- Letter management via Digital Post

## Architecture

```
  Creditor Portal   Caseworker Portal   Citizen Portal       DUPLA / M2M
  (Fordringshaver)   (Sagsbehandler)      (Borger)            Gateway
        |                  |                  |                   |
        +------------------+------------------+-------------------+
                                              |
                                 +------------+------------+
                                 |     Keycloak (JWT)      |
                                 +------------+------------+
                                              |
    +----------------+----------------+-------+---------+----------------+
    |                |                |                  |                |
+---+---+      +-----+-----+    +----+------+    +------+----+    +------+----+
| Case  |      |   Debt    |    | Payment   |    |  Rules    |    | Creditor  |
|Service|      |  Service  |    |  Service  |    |  Engine   |    |  Service  |
+-------+      +-----------+    +-----+-----+    +-----------+    +-----------+
                                      |
                               +------+------+
                               |   immudb    |
                               | (tamper-    |
                               |  evidence)  |
                               +-------------+
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| **person-registry** | 8090 | **GDPR data store** — single source of truth for all PII (CPR/CVR encrypted) |
| **rules-engine** | 8091 | **Business rules** — Drools-based rule evaluation |
| case-service | 8081 | Case management and workflow (Flowable BPMN) |
| debt-service | 8082 | Debt registration, lifecycle management, readiness validation |
| payment-service | 8083 | Payment processing, double-entry bookkeeping, tamper-evidence ledger |
| letter-service | 8084 | Letter generation, Digital Post |
| creditor-portal | 8085 | Portal for fordringshavere |
| citizen-portal | 8086 | Portal for borgere |
| offsetting-service | 8087 | Modregning (set-off) processing |
| wage-garnishment-service | 8088 | Loenindeholdelse processing |
| integration-gateway | 8089 | DUPLA, SKB CREMUL/DEBMUL, legacy SOAP (OIO/SKAT) |
| creditor-service | 8092 | Creditor master data, channel binding, access resolution |
| caseworker-portal | 8093 | Portal for sagsbehandlere |
| **immudb** | 3322 (gRPC) | **Tamper-evidence ledger** — cryptographic integrity for financial postings (ADR-0029) |

### GDPR Architecture

All personal data (CPR, CVR, names, addresses) is isolated in the **Person Registry**. Other services store only technical UUIDs:

```
Person Registry (PII)          Other Services (NO PII)
+------------------+           +------------------+
| person_id (UUID) | <-------- | debtor_person_id |
| CPR (encrypted)  |           | (UUID only)      |
| Name (encrypted) |           |                  |
| Address (enc.)   |           |                  |
+------------------+           +------------------+
```

## Technology Stack

- **Runtime**: Java 21, Spring Boot 3.5
- **Database**: PostgreSQL 16 (enterprise grade with audit/history)
- **Tamper-evidence ledger**: immudb 1.10 + immudb4j 1.0.1 (cryptographic integrity, ADR-0029)
- **Rules Engine**: Drools 9.x (business rules, decision tables)
- **Workflow**: Flowable 7.x (BPMN 2.0 case management)
- **Build**: Maven with Spotless, JaCoCo, OWASP
- **API**: OpenAPI 3.1, REST
- **Auth**: OAuth2/OIDC via Keycloak
- **Deployment**: Kubernetes (Horizontale Driftsplatform)

## Getting Started

### Prerequisites

- Java 21 or later
- Maven 3.9+
- Docker (for local development)

### Build

```bash
# Build all modules
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Format code
mvn spotless:apply
```

### Run Locally

```bash
# Start infrastructure (PostgreSQL + Keycloak)
docker compose up -d postgres keycloak

# Run a service
cd opendebt-case-service
mvn spring-boot:run
```

### Run All Services

```bash
docker compose up -d
```

### Demo Startup

```powershell
# Fast demo (no auth on portals/backends, immudb tamper-evidence enabled)
.\start-demo.ps1

# Security demo (Keycloak login + role-based access)
.\start-demo.ps1 -SecurityDemo

# Only caseworker flow with security
.\start-demo.ps1 -SecurityDemo -Only caseworker
```

Seeded demo users:

- `caseworker` / `caseworker123` (role: `CASEWORKER`)
- `creditor` / `creditor123` (role: `CREDITOR`)
- `admin` / `admin123` (role: `ADMIN`)

Keycloak admin console: `http://localhost:8080/admin/` (`admin` / `admin`)

When the demo starts, `payment-service` automatically seeds **14 ledger pairs (28 immudb entries)** to demonstrate the tamper-evidence layer. Inspect the ledger using:

```powershell
cd docs/spike
python immudb-view.py   # generates immudb-report.html and opens it in browser
```

The immudb web console is accessible at `http://localhost:8094` (shows Document Store; KV ledger is accessed via the viewer script above).

### Unified Compose Script (App + Observability)

Use the PowerShell helper to run one stack (app or observability) or both with one command.

```powershell
# Start app + observability
.\compose-stack.ps1

# Start app only
.\compose-stack.ps1 -Stack app

# Start observability only
.\compose-stack.ps1 -Stack obs

# Start infra only (postgres + keycloak + observability)
.\compose-stack.ps1 -Stack infra

# Stop both stacks
.\compose-stack.ps1 -Action down

# Stop infra-only containers (keeps app containers untouched)
.\compose-stack.ps1 -Action down -Stack infra

# Follow logs for one service
.\compose-stack.ps1 -Action logs -Service postgres
```

When the observability stack is running, Grafana provisions both the baseline overview dashboard and the RBAC authorization dashboard from `config/grafana/provisioning/dashboards/`. RBAC alert templates are provisioned from `config/grafana/provisioning/alerting/`.

### Demo Startup (With and Without Security)

```powershell
# Fast demo mode (no auth on portals/backends)
.\start-demo.ps1

# Security demo mode (Keycloak login + role-based access)
.\start-demo.ps1 -SecurityDemo

# Only creditor flow with Keycloak security
.\start-demo.ps1 -SecurityDemo -Only creditor
```

In security demo mode, use these seeded users:

- `caseworker` / `caseworker123` (role: `CASEWORKER`)
- `creditor` / `creditor123` (role: `CREDITOR`)
- `admin` / `admin123` (role: `ADMIN`)

Keycloak admin console: `http://localhost:8080/admin/` with `admin` / `admin`.

## API Documentation

Each service exposes OpenAPI documentation:

- Case Service: http://localhost:8081/case-service/swagger-ui.html
- Debt Service: http://localhost:8082/debt-service/swagger-ui.html
- Payment Service: http://localhost:8083/payment-service/swagger-ui.html

## Architecture Decision Records

Key architectural decisions are documented in [docs/adr/](docs/adr/):

- [ADR-0002: Microservices Architecture](docs/adr/0002-microservices-architecture.md)
- [ADR-0003: Java/Spring Boot Stack](docs/adr/0003-java-spring-boot-technology-stack.md)
- [ADR-0007: No Direct Database Connections](docs/adr/0007-no-direct-database-connections.md)
- [ADR-0010: Faellesoffentlige Arkitekturprincipper](docs/adr/0010-faellesoffentlige-arkitekturprincipper-compliance.md)
- [ADR-0018: Double-Entry Bookkeeping](docs/adr/0018-double-entry-bookkeeping-for-payment-service.md)
- [ADR-0029: immudb for Financial Ledger Integrity](docs/adr/0029-immudb-for-financial-ledger-integrity.md)
- [ADR-0032: Catala Formal Compliance Verification Layer](docs/adr/0032-catala-formal-compliance-layer.md)

## Compliance

OpenDebt complies with:

- **Faellesoffentlige Arkitekturprincipper** - Danish public sector architecture principles
- **GDPR** - Privacy by design
- **DUPLA** - UFST data exchange platform standards

### Formal Compliance Verification (Catala)

High-risk G.A. Inddrivelse rules are formally encoded in [Catala](https://catala-lang.org/)
before implementation. Catala source files under `catala/` act as an executable oracle that
validates Gherkin scenarios against the juridisk vejledning text. See
[ADR-0032](docs/adr/0032-catala-formal-compliance-layer.md) for the full rationale and tier
classification.

Completed spikes:

| Spike | Legal section | Report |
|-------|--------------|--------|
| P054 | G.A.1.4.3–1.4.4 — Opskrivning/nedskrivning | [`catala/SPIKE-REPORT.md`](catala/SPIKE-REPORT.md) |
| P069 | G.A.2.3.2.1 — Dækningsrækkefølge (GIL § 4) | [`catala/SPIKE-REPORT-069.md`](catala/SPIKE-REPORT-069.md) |

## Contributing

See [agents.md](agents.md) for development guidelines.

## License

Apache License 2.0 - see [LICENSE](LICENSE) for details
