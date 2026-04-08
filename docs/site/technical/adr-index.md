# Architecture Decision Records

| ADR | Title | Summary |
|-----|-------|---------|
| 0001 | Record architecture decisions | Use ADRs to capture significant architectural decisions |
| 0002 | Microservices architecture | Decompose the system into independently deployable services |
| 0003 | Java/Spring Boot technology stack | Java 21 and Spring Boot 3.5 as the primary stack |
| 0004 | API-first design with OpenAPI | Define APIs with OpenAPI 3.1 specs before implementation |
| 0005 | Keycloak authentication | Keycloak for OAuth2/OIDC authentication and authorization |
| 0006 | Kubernetes deployment | Deploy all services on Kubernetes |
| 0007 | No cross-service database connections | Each service owns its database; cross-service access is via APIs |
| 0008 | Letter management strategy | Centralized letter-service for Digital Post and physical mail |
| 0009 | DUPLA integration | External APIs exposed through DUPLA API gateway |
| 0010 | Architecture principles compliance | Align with Danish public sector architecture principles |
| 0011 | PostgreSQL database | PostgreSQL 16 as the database for all services |
| 0012 | Debtor identification model | CPR/CVR with role-based identification in person-registry |
| 0013 | Enterprise PostgreSQL with audit | Audit trail and history tracking in PostgreSQL |
| 0014 | GDPR data isolation | All PII isolated in person-registry service |
| 0015 | Drools rules engine | Drools for business rule evaluation |
| 0016 | Flowable workflow engine | Flowable BPMN for case management workflows |
| 0017 | Smooks EDIFACT CREMUL/DEBMUL | Smooks for parsing SKB payment files |
| 0018 | Double-entry bookkeeping | All financial effects post to payment-service ledger (amendment #3); local journals supplementary |
| 0019 | Orchestration over event-driven | Prefer orchestration (REST calls) over async events |
| 0020 | Creditor channel and master data | Channel binding and access resolution architecture |
| 0021 | UI accessibility compliance | WCAG 2.1 AA and webtilgaengelighed.dk compliance |
| 0022 | Shared audit infrastructure | Common audit entity and CLS integration |
| 0023 | Creditor portal frontend | Thymeleaf + HTMX for server-rendered portals |
| 0024 | Observability backend stack | Grafana + Prometheus + Loki + Tempo for observability |
| 0025 | Maven build tool | Maven as the build and dependency management tool |
| 0026 | Inter-service resilience | Resilience4j circuit breaker and retry for all inter-service REST clients |
| 0027 | Offsetting merged into debt-service | Modregning domain consolidated into debt-service |
| 0028 | Backup and disaster recovery | pgBackRest WAL archiving + streaming replication; RTO 4h / RPO 4h |
| 0029 | immudb for financial ledger integrity | immudb 1.10 + immudb4j 1.0.1 as cryptographic tamper-evidence KV store for double-entry ledger entries; dual-write pattern from `BookkeepingService`; conditionally accepted pending UFST HDP platform validation (TB-028-a) |
| 0030 | SOAP legacy gateway | Spring-WS SOAP 1.1/1.2 on `/soap/*` in `integration-gateway`; OCES3 mTLS auth for OIO/SKAT protocols |
| 0031 | Statutory codes as enums | Values defined by statute (e.g., `WriteDownReasonCode`) are Java enums, not DB configuration; confirmed compatible with Catala's enumeration model (P054 spike) |

ADR source files are in `architecture/adr/` in the repository.
