# ADR 0038: Prescription Objection Boundary with Debt-Service Façade

## Status

Accepted

## Date

2026-05-16

## Context

Petition059 introduces both:

1. authoritative limitation state on the claim aggregate, and
2. a human objection workflow for evaluating whether a claim is prescribed.

The approved ownership map for petition059 assigns:

- limitation state and calculation to `opendebt-debt-service`,
- objection workflow lifecycle to `opendebt-case-service`,
- presentation to `opendebt-caseworker-portal`.

The petition and outcome contract still require objection registration and evaluation on the
limitation surface at `POST /foraeldelse/{fordringId}/indsigelse` and
`PUT /foraeldelse/{fordringId}/indsigelse/{indsigelsesId}`. The same outcome contract also
forbids caller-supplied `registeredBy`, `decidedBy`, and `debtorPersonId` on the public
command surface.

A pure case-service external API breaks the petition contract. A pure debt-service objection
workflow breaks the approved ownership split. The boundary therefore must preserve the visible
limitation surface while keeping workflow authority in case-service.

## Decision

1. **Debt-service keeps the external limitation contract.**
   - The observable FR-6 command surface remains on debt-service.
   - Debt-service acts as a contract-preserving façade for objection registration and evaluation.
   - The public façade accepts only petition-visible objection payloads and rejects caller-supplied
     audit identity or debtor-linkage fields.

2. **Case-service keeps objection workflow ownership.**
   - Case-service owns the objection record, Flowable lifecycle, and decision capture.
   - Debt-service calls a case-service internal API to create and evaluate limitation objections.

3. **Debt-service applies claim-state mutation through an application seam.**
   - Debt-service remains the owner of `INDSIGELSE_PENDING`, `FORAELDET`, and `ACTIVE` claim-state transitions.
   - Internal callers target `LimitationStateApplicationService`, not `LimitationApi`, eliminating controller re-entry.
   - Debt-service derives `registeredBy` / `decidedBy` from authenticated server-side context and
     derives `debtorPersonId` from authoritative claim state before invoking case-service.

4. **Coordination remains synchronous.**
   - Debt-service calls case-service over REST inside the request path.
   - No cross-service database access and no message broker are introduced.

5. **API-first artefacts are mandatory for the new seams.**
   - Debt-service limitation surface: `api-specs/openapi-debt-service-limitation.yaml`
   - Case-service internal workflow API: `api-specs/openapi-case-service-limitation-internal.yaml`
   - Wage-garnishment internal fact API: `api-specs/openapi-wage-garnishment-service-internal.yaml`

## Consequences

### Positive

- Preserves the petition/outcome FR-6 surface without silent contract drift.
- Makes the FR-6 audit model enforceable without trusting caller input.
- Preserves approved petition059 ownership without remapping.
- Keeps claim-state logic with the claim owner.
- Keeps human workflow and decision lifecycle with the workflow owner.
- Replaces ambiguous controller re-entry with a clean application/domain seam.

### Negative

- Debt-service now contains a façade responsibility in addition to aggregate ownership.
- FR-6 latency includes one synchronous case-service call.
- The architecture needs explicit internal API specs for workflow and fact reads.

### Mitigations

- Keep the façade narrow: it translates external limitation commands into workflow commands and local state mutations only.
- Keep workflow state authoritative in case-service; debt-service stores only limitation state and references.
- Validate the boundary through OpenAPI artefacts and C4 model updates before implementation begins.

## Related ADRs

- ADR-0004: API-First Design with OpenAPI
- ADR-0007: No Direct Database Connections
- ADR-0016: Flowable for Workflow Engine
- ADR-0019: Explicit Orchestration over Event-Driven Architecture
- ADR-0022: Shared Audit Infrastructure
