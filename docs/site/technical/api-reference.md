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

### debt-service (`:8082`) — v1.5.0

#### Debts

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/api/v1/debts` | List debts (filter by creditorId, debtorId, ids, status, readinessStatus) |
| `GET` | `/api/v1/debts/{id}` | Get debt by ID |
| `POST` | `/api/v1/debts` | Register a new debt |
| `PUT` | `/api/v1/debts/{id}` | Update a debt |
| `DELETE` | `/api/v1/debts/{id}` | Delete a debt (retraction before collection) |
| `POST` | `/api/v1/debts/submit` | Batch claim submission |
| `GET` | `/api/v1/debts/by-ocr` | Look up debt by OCR/payment ID |
| `GET` | `/api/v1/debts/debtor/{debtorId}` | List debts for a debtor (UUID) |
| `POST` | `/api/v1/debts/{id}/evaluate-state` | Trigger lifecycle state evaluation |
| `POST` | `/api/v1/debts/{id}/transfer-for-collection` | Transfer to PSRM for collection |
| `POST` | `/api/v1/debts/{id}/write-down` | Write down claim balance |
| `POST` | `/api/v1/debts/{id}/interest/recalculate` | Recalculate interest from a date (rate boundary-aware) |

#### Adjustments (petition 053)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/api/v1/debts/{id}/adjustments` | Submit write-up or write-down adjustment; enforces all G.A.1.4.3/G.A.1.4.4/Gæld.bekendtg. § 7 rules independently of the portal (FR-9). Returns `201` with `ClaimAdjustmentResponseDto` or `422` with RFC 7807 `ProblemDetail`. |

#### Readiness

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/api/v1/debts/{id}/validate-readiness` | Validate indrivelsesparathed |
| `POST` | `/api/v1/debts/{id}/approve-readiness` | Manually approve readiness |
| `POST` | `/api/v1/debts/{id}/reject-readiness` | Manually reject readiness |

#### Collection Measures

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/api/v1/debts/{debtId}/collection-measures` | List collection measures |
| `POST` | `/api/v1/debts/{debtId}/collection-measures` | Register a collection measure |
| `POST` | `/api/v1/debts/{debtId}/collection-measures/{measureId}/complete` | Mark complete |
| `POST` | `/api/v1/debts/{debtId}/collection-measures/{measureId}/cancel` | Cancel measure |

#### Liabilities

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/api/v1/debts/{debtId}/liabilities` | List liabilities |
| `POST` | `/api/v1/debts/{debtId}/liabilities` | Add debtor liability |
| `DELETE` | `/api/v1/debts/{debtId}/liabilities/{liabilityId}` | Remove liability |

#### Objections

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/api/v1/debts/{debtId}/objections` | List objections |
| `POST` | `/api/v1/debts/{debtId}/objections` | Register objection (indsigelse) |
| `PUT` | `/api/v1/debts/{debtId}/objections/{objectionId}/resolve` | Resolve objection |

#### Notifications

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/api/v1/debts/{debtId}/notifications` | List notifications |
| `POST` | `/api/v1/debts/{debtId}/demand-for-payment` | Issue påkrav |
| `POST` | `/api/v1/debts/{debtId}/reminder` | Issue rykker |

#### Citizen

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/api/v1/citizen/debts` | Citizen's own debts (CITIZEN role; no PII exposed) |

#### Business Configuration (petition 046/047)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/api/v1/config` | List all config entries grouped by key |
| `POST` | `/api/v1/config` | Create a new versioned config entry |
| `GET` | `/api/v1/config/{key}` | Get effective config value for a key and date |
| `GET` | `/api/v1/config/{key}/history` | Version history for a key |
| `GET` | `/api/v1/config/{key}/audit` | Audit trail for a key |
| `GET` | `/api/v1/config/{key}/preview` | Preview derived rates for an NB rate |
| `PUT` | `/api/v1/config/{id}` | Update a pending or future entry |
| `DELETE` | `/api/v1/config/{id}` | Delete a future entry |
| `PUT` | `/api/v1/config/{id}/approve` | Approve a PENDING_REVIEW entry |
| `PUT` | `/api/v1/config/{id}/reject` | Reject a PENDING_REVIEW entry |

Known config keys:

| Key | Description |
|-----|-------------|
| `RATE_NB_UDLAAN` | Nationalbankens udlånsrente (base rate, triggers derived rate computation) |
| `RATE_INDR_STD` | Standard inddrivelsesrente (NB + 4 pp, §5 renteloven) |
| `RATE_INDR_TOLD` | Told inddrivelsesrente (NB + 2 pp, §7 renteloven) |
| `RATE_INDR_TOLD_AFD` | Told afdragsrente (NB + 1 pp, §8 renteloven) |
| `FEE_STANDARDGEBYR` | Standard gebyr (kr) |
| `THRESHOLD_MIN_INTEREST` | Minimumsrentegrænse |

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

#### REST endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/api/v1/creditor-m2m/claims/submit` | M2M claim submission via DUPLA |
| `POST` | `/api/v1/skb/cremul/parse` | Parse SKB CREMUL payment file |
| `POST` | `/api/v1/skb/debmul/generate` | Generate SKB DEBMUL file |

#### SOAP endpoints (petition019 — legacy OIO/SKAT protocols)

SOAP endpoints are served by a dedicated `MessageDispatcherServlet` at `/soap/*`, separate from the REST dispatcher at `/api`. Authentication uses OCES3 mTLS client certificates, not OAuth2 bearer tokens.

**OIO protocol** (`urn:oio:skat:efi:ws:1.0.1`)

| Method | Endpoint | Operation | Purpose |
|--------|----------|-----------|---------|
| `POST` | `/soap/oio` | `MFFordringIndberet_I` | Submit OIO claims |
| `POST` | `/soap/oio` | `MFKvitteringHent_I` | Retrieve OIO receipts |
| `POST` | `/soap/oio` | `MFUnderretSamlingHent_I` | Retrieve OIO notification collections |
| `GET` | `/soap/oio?wsdl` | — | OIO WSDL (dual SOAP 1.1/1.2 binding) |

**SKAT protocol** (`http://skat.dk/begrebsmodel/2009/01/15/`)

| Method | Endpoint | Operation | Purpose |
|--------|----------|-----------|---------|
| `POST` | `/soap/skat` | `FordringIndberet` | Submit SKAT claims |
| `POST` | `/soap/skat` | `KvitteringHent` | Retrieve SKAT receipts |
| `POST` | `/soap/skat` | `UnderretSamlingHent` | Retrieve SKAT notification collections |
| `GET` | `/soap/skat?wsdl` | — | SKAT WSDL (dual SOAP 1.1/1.2 binding) |

**SOAP fault codes**

| Exception | HTTP | SOAP 1.1 `faultcode` | SOAP 1.2 `Code/Value` |
|-----------|------|----------------------|-----------------------|
| Malformed XML / SAAJ parse error | 400 | `env:Client` | `env:Sender` |
| `FordringValidationException` | 422 | `env:Client` | `env:Sender` |
| `Oces3AuthenticationException` | 401 | `env:Client` | `env:Sender` |
| `Oces3AuthorizationException` | 403 | `env:Client` | `env:Sender` |
| `ServiceException` (generic) | 500 | `env:Server` | `env:Receiver` |

## Authentication

REST API endpoints require OAuth2 bearer tokens issued by Keycloak. SOAP endpoints at `/soap/*` use OCES3 mTLS client certificates (see ADR-0030).

**OAuth2 roles (REST):**

| Role | Access |
|------|--------|
| `CREDITOR` | Claim creation and update |
| `CASEWORKER` | Case management, claim validation, read-only config view |
| `SUPERVISOR` | Readiness approval |
| `CONFIGURATION_MANAGER` | Business config create, approve, reject |
| `ADMIN` | Full access including all config operations |
| `SYSTEM` | Service-to-service (M2M) |
| `SERVICE` | Internal service calls |
| `CITIZEN` | Own debt summary only (citizen portal) |
