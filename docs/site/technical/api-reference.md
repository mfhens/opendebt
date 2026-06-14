# API Reference

Each OpenDebt service exposes a REST API documented with OpenAPI 3.1. Swagger UI is available at `http://localhost:{port}/{context-path}/swagger-ui.html` when running locally.

## OpenAPI specifications

| Service | Spec file | Swagger UI |
|---------|-----------|------------|
| debt-service | `api-specs/openapi-debt-service.yaml` | `:8082/debt-service/swagger-ui.html` |
| case-service | `api-specs/openapi-case-service.yaml` | `:8081/case-service/swagger-ui.html` |
| debt-service (limitation surface, petition059) | `api-specs/openapi-debt-service-limitation.yaml` | `:8082/debt-service/swagger-ui.html` |
| case-service (limitation internal workflow) | `api-specs/openapi-case-service-limitation-internal.yaml` | Internal cluster contract |
| wage-garnishment-service (limitation facts) | `api-specs/openapi-wage-garnishment-service-internal.yaml` | Internal cluster contract |
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
| `POST` | `/api/v1/debts` | Internal-only debt persistence endpoint after validation |
| `PUT` | `/api/v1/debts/{id}` | Update a debt |
| `DELETE` | `/api/v1/debts/{id}` | Delete a debt (retraction before collection) |
| `POST` | `/api/v1/debts/submit` | Validated claim submission via shared fordring Drools rules |
| `GET` | `/api/v1/debts/by-ocr` | Look up debt by OCR/payment ID |
| `GET` | `/api/v1/debts/debtor/{debtorId}` | List debts for a debtor (UUID) |
| `POST` | `/api/v1/debts/{id}/evaluate-state` | Trigger lifecycle state evaluation |
| `POST` | `/api/v1/debts/{id}/transfer-for-collection` | Transfer to PSRM for collection |
| `POST` | `/api/v1/debts/{id}/write-down` | Write down claim balance |
| `POST` | `/api/v1/debts/{id}/interest/recalculate` | Recalculate interest from a date (rate boundary-aware) |

`POST /api/v1/debts/submit` accepts an optional `X-OpenDebt-Claim-Ingress-Path` header with
`PORTAL` or `SYSTEM_TO_SYSTEM`. The creditor portal and integration gateway set this header so the
same Drools validation contract applies across all claim-ingestion paths.

#### Adjustments (petition 053)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/api/v1/debts/{id}/adjustments` | Submit write-up or write-down adjustment; enforces all G.A.1.4.3/G.A.1.4.4/Gæld.bekendtg. § 7 rules independently of the portal (FR-9). Returns `201` with `ClaimAdjustmentResponseDto` or `422` with RFC 7807 `ProblemDetail`. |

#### Limitation / prescription (petition 059)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/api/v1/foraeldelse/{fordringId}` | Get limitation status, expiry date, histories, and claim-complex context for a claim. |
| `POST` | `/api/v1/foraeldelse/{fordringId}/afbrydelse` | Register a legally effective interruption and recalculate expiry. |
| `POST` | `/api/v1/foraeldelse/{fordringId}/tillaegsfrist` | Register a supplementary period (for example `INTERN_OPSKRIVNING`). |
| `POST` | `/api/v1/fordringskompleks` | Create a claim complex with one or more initial members. |
| `POST` | `/api/v1/fordringskompleks/{kompleksId}/members/{fordringId}` | Add one member to an existing claim complex. |
| `GET` | `/api/v1/fordringskompleks/{kompleksId}/members` | List the current members of a claim complex. |
| `POST` | `/api/v1/foraeldelse/{fordringId}/indsigelse` | Register a limitation objection on the petition-aligned limitation surface; debt-service delegates workflow ownership to case-service. |
| `PUT` | `/api/v1/foraeldelse/{fordringId}/indsigelse/{indsigelsesId}` | Evaluate a limitation objection on the petition-aligned limitation surface. |

Internal companion specs for petition059:

- `api-specs/openapi-case-service-limitation-internal.yaml` — debt-service to case-service workflow delegation
- `api-specs/openapi-wage-garnishment-service-internal.yaml` — debt-service to wage-garnishment limitation fact reads

Public FR-6 objection commands reject caller-supplied `registeredBy`, `decidedBy`, and
`debtorPersonId`; those values are derived server-side from authenticated context and
authoritative claim state.

#### Retskraft evaluation worklists (petition 060)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/api/v1/debtors/{debtorId}/retskraft-worklists` | Generate a section-50 ranking snapshot for the debtor in default, data-error, voluntary-payment-surplus, or modregning context. |
| `GET` | `/api/v1/debtors/{debtorId}/retskraft-worklists/{worklistId}` | Inspect ranked entries, legal reference, decision origin, timestamp, and input hash for an existing petition060 worklist. |
| `POST` | `/api/v1/debtors/{debtorId}/retskraft-worklists/{worklistId}/override` | Persist a caseworker-documented override or expedited deviation on the existing worklist. |
| `POST` | `/api/v1/debtors/{debtorId}/retskraft-worklists/{worklistId}/modregning-decision` | Record a partial/no-modregning outcome for the current payout context with visible rationale. |

#### Attachment workflows (petition 066)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/internal/debtors/{debtorId}/attachment-workflows` | Create a debtor-scoped attachment workflow with immutable `coveredFordringIds`; rejects atomically if any covered claim is ineligible. |
| `POST` | `/internal/debtors/{debtorId}/attachment-workflows/{workflowId}/dispatch` | Idempotently dispatch a requested workflow to fogedret and persist `workflowReference` plus optional external case metadata. |
| `POST` | `/internal/debtors/{debtorId}/attachment-workflows/{workflowId}/withdraw` | Withdraw a non-terminal workflow with mandatory caseworker reason and no petition059 interruption emission. |
| `POST` | `/internal/debtors/{debtorId}/attachment-workflows/callbacks` | Apply a fogedret callback correlated by debtor scope and `workflowReference`; legal terminal transitions trigger atomic petition059 `UDLAEG` interruption handling. |
| `GET` | `/internal/debtors/{debtorId}/attachment-workflows` | List debtor-scoped attachment workflows with current status, history, outcome qualifier, and interruption linkage metadata. |
| `GET` | `/internal/debtors/{debtorId}/attachment-workflows/{workflowId}` | Read one debtor-scoped attachment workflow by workflow ID. |

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
| `GET` | `/api/v1/citizen/debts` | Citizen's own debts (`bearerAuth`; `pageNumber`/`pageSize`; no PII exposed) |

`GET /api/v1/citizen/debts` is the petition026 citizen projection. The debt-service resolves
`person_id` from the JWT and tolerates an `X-Person-Id` bridge header for the citizen-portal. The
response includes `creditorDisplayName`, `citizenStatus`, optional pause/write-off reason codes,
row-level rate metadata (`interestRuleCode`, `currentInterestRate`), and page-level
`effectiveInterestRates`.

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
| `GET` | `/api/v1/creditors/{creditorOrgId}` | Get creditor by org ID, including citizen-safe `displayName` |
| `POST` | `/api/v1/creditors/access/resolve` | Resolve channel access for M2M/portal |
| `POST` | `/api/v1/creditors/{id}/validate-action` | Validate creditor permission for action |

### payment-service (`:8083`)

#### Payment matching

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/api/v1/payments/incoming` | Process incoming CREMUL payment (OCR-based matching, write-down, overpayment rules) |

#### Timeline (petition 050)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/api/v1/events/case/{caseId}` | Debt events by case for BFF timeline aggregation (roles: CASEWORKER, CREDITOR, CITIZEN, SERVICE) |

#### Dækningsrækkefølge — GIL § 4 payment application order (petition 057)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `GET` | `/api/v1/debtors/{debtorId}/daekningsraekkefoelge` | Get GIL § 4 payment application order for a debtor — returns ordered list of `DaekningsraekkefoelgePositionDto` (fordring_id, komponent, daekning_beloeb, prioritet_kategori) |
| `POST` | `/api/v1/debtors/{debtorId}/daekningsraekkefoelge/simulate` | Simulate payment application against a debtor's fordringer — dry-run only, no `DaekningRecord` written; request body is `SimulateRequestDto` (amount + debtor context) |

The 8-step GIL § 4 algorithm sorts fordringer by `PrioritetKategori` (INDDRIVELSESRENTER → OPKRAEVNINGSRENTER → GEBYRER → AFDRAG → ANDRE) and by FIFO sort key within each category. The `RenteKomponent` sub-position resolves the 6-tier interest allocation for inddrivelsesrenter (GIL § 4 stk. 1–4). Actual application writes an immutable `daekning_record` row per component. The simulate endpoint returns the same ordering without persisting any records.

Legal basis: GIL § 4 stk. 1–4, GIL § 10b, Gæld.bekendtg. § 4 stk. 3, Retsplejelovens § 507, Lov nr. 288/2022.

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
| `POST` | `/api/external/v1/fogedret/attachment-dispatch` | Petition066 external dispatch ingress/egress boundary for fogedret attachment traffic. |
| `POST` | `/api/external/v1/fogedret/attachment-callbacks` | Petition066 external callback ingress protected by OCES3 mTLS and replay detection before forwarding to debt-service. |

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

## Petition059 detailed API surfaces

### Limitation State API (`debt-service`, `:8082`)

**Auth:** `CASEWORKER` or `ADMIN`

| Method | Path | Request shape | Response shape |
|--------|------|---------------|----------------|
| `GET` | `/api/v1/foraeldelse/{fordringId}` | Path parameter: `fordringId` (`UUID`) | `ForaeldelseStatusDto` — `fordringId`, `debtorPersonId`, `currentFristExpires`, `udskydelseDato`, `isInUdskydelse`, `retsgrundlag`, `afbrydelseHistory[]`, `tillaegsfristHistory[]`, `status`, `kompleksId`, `memberFordringIds[]`, `objectionRationale` |
| `POST` | `/api/v1/foraeldelse/{fordringId}/afbrydelse` | `RegisterAfbrydelseRequest` — `type`, `eventDate`, `afgoerelseRegistreret`, `forgaevesUdlaeg` | `201 Created` + `ForaeldelseStatusDto` |
| `POST` | `/api/v1/foraeldelse/{fordringId}/tillaegsfrist` | `RegisterTillaegsfristRequest` — `type`, `appliedDate` | `201 Created` + `ForaeldelseStatusDto` |
| `POST` | `/api/v1/fordringskompleks` | `CreateFordringskompleksRequest` — `memberFordringIds[]` | `201 Created` + `FordringskompleksMemberListDto` — `kompleksId`, `memberFordringIds[]` |
| `POST` | `/api/v1/fordringskompleks/{kompleksId}/members/{fordringId}` | Path parameters only: `kompleksId`, `fordringId` | `201 Created` with empty body |
| `GET` | `/api/v1/fordringskompleks/{kompleksId}/members` | Path parameter: `kompleksId` (`UUID`) | `FordringskompleksMemberListDto` — `kompleksId`, `memberFordringIds[]` |
| `POST` | `/api/v1/foraeldelse/{fordringId}/indsigelse` | Optional `RegisterObjectionRequest`; caller-supplied `registeredBy`, `decidedBy`, and `debtorPersonId` are rejected and derived server-side | `201 Created` + `ObjectionRegistrationResult` — `indsigelsesId`, `status` |
| `PUT` | `/api/v1/foraeldelse/{fordringId}/indsigelse/{indsigelsesId}` | `EvaluateObjectionRequest` — `outcome`, `rationale`; caller-supplied `registeredBy`, `decidedBy`, and `debtorPersonId` are rejected | `200 OK` + `ForaeldelseStatusDto` |

### Limitation Objection Workflow Internal API (`case-service`, `:8081`)

**Auth:** `SERVICE`, `ADMIN`, or `CASEWORKER`

| Method | Path | Request shape | Response shape |
|--------|------|---------------|----------------|
| `POST` | `/api/internal/v1/limitation-objections` | `CreateLimitationObjectionWorkflowRequest` — `fordringId`, `debtorPersonId`, `registeredBy` | `201 Created` + `LimitationObjectionWorkflowResult` — `indsigelsesId`, `workflowCaseId`, `status`, `rationale` |
| `PUT` | `/api/internal/v1/limitation-objections/{indsigelsesId}/decision` | `LimitationObjectionDecisionRequest` — `fordringId`, `outcome`, `rationale`, `decidedBy`, `decidedAt` | `200 OK` + `LimitationObjectionWorkflowResult` — `indsigelsesId`, `workflowCaseId`, `status`, `rationale` |

### Wage Garnishment Limitation Facts Internal API (`wage-garnishment-service`, `:8088`)

**Auth:** `SERVICE`, `ADMIN`, or `CASEWORKER`

| Method | Path | Request shape | Response shape |
|--------|------|---------------|----------------|
| `GET` | `/api/internal/v1/limitation-facts/debtors/{debtorPersonId}` | Path parameter: `debtorPersonId` (`UUID`) | `WageGarnishmentLimitationFacts` — `afgoerelseRegistreret`, `underretningsDato`, `coveredFordringIds[]`, `inaktivSiden` |

### Caseworker Portal Limitation Panel (`caseworker-portal`, `:8087`)

| Route | Access | Rendered content |
|-------|--------|------------------|
| `GET /cases/{caseId}/debts/{fordringId}/limitation-panel` | Authenticated caseworker session; write actions are shown for `CASEWORKER` and `ADMIN` | Thymeleaf limitation panel with current status, expiry date, claim-complex members, interruption history, supplementary-period history, and objection actions |

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
