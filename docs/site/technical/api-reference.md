# API Reference

Each OpenDebt service exposes a REST API documented with OpenAPI 3.1. Swagger UI is available at `http://localhost:{port}/{context-path}/swagger-ui.html` when running locally.

## OpenAPI specifications

| Service | Spec file | Swagger UI |
|---------|-----------|------------|
| debt-service | `api-specs/openapi-debt-service.yaml` | `:8082/debt-service/swagger-ui.html` |
| case-service | `api-specs/openapi-case-service.yaml` | `:8081/case-service/swagger-ui.html` |
| creditor-service | `api-specs/openapi-creditor-service.yaml` | `:8092/creditor-service/swagger-ui.html` |
| person-registry | `api-specs/openapi-person-registry-internal.yaml` | `:8090/person-registry/swagger-ui.html` |
| integration-gateway | Auto-generated | `:8089/integration-gateway/swagger-ui.html` |
| payment-service | Auto-generated | `:8083/payment-service/swagger-ui.html` |

## Key endpoints by service

### debt-service (`:8082`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/api/v1/debts` | List debts with filters |
| `GET` | `/api/v1/debts/{id}` | Get debt by ID |
| `POST` | `/api/v1/debts` | Register a new debt |
| `POST` | `/api/v1/debts/submit` | Submit claim for collection (validates + creates case) |
| `POST` | `/api/v1/debts/{id}/evaluate-state` | Evaluate lifecycle state transition |
| `POST` | `/api/v1/debts/{id}/transfer-for-collection` | Transfer to collection authority |
| `POST` | `/api/v1/debts/{id}/write-down` | Write down a claim |

### creditor-service (`:8092`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/api/v1/creditors` | List active creditors |
| `GET` | `/api/v1/creditors/{creditorOrgId}` | Get creditor by org ID |
| `POST` | `/api/v1/creditors/access/resolve` | Resolve channel access for M2M/portal |
| `POST` | `/api/v1/creditors/{id}/validate-action` | Validate creditor permission for action |

### case-service (`:8081`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/api/v1/cases` | List cases with filters |
| `GET` | `/api/v1/cases/{id}` | Get case by ID |
| `POST` | `/api/v1/cases/{id}/assign-debt` | Assign a debt to a case |

### integration-gateway (`:8089`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/api/v1/creditor-m2m/claims/submit` | M2M claim submission via DUPLA |
| `POST` | `/api/v1/skb/cremul/parse` | Parse SKB CREMUL payment file |
| `POST` | `/api/v1/skb/debmul/generate` | Generate SKB DEBMUL file |

## Authentication

All API endpoints require OAuth2 bearer tokens issued by Keycloak. Roles:

| Role | Access |
|------|--------|
| `CREDITOR` | Claim creation and update |
| `CASEWORKER` | Case management, claim validation |
| `SUPERVISOR` | Readiness approval |
| `ADMIN` | Full access |
| `SYSTEM` | Service-to-service (M2M) |
| `SERVICE` | Internal service calls |
