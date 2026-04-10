# ADR-0036: Security Baseline

**Status**: proposed  
**Date**: 2026-04-10  
**Deciders**: Architecture team

## Context

OpenDebt handles public-sector debt collection data including personal financial
records subject to GDPR, statutory confidentiality requirements, and public-sector
IT security standards (ISO 27001 / NSIS). A consolidated security baseline ADR is
needed as the normative reference for the NFR register entries NFR-SEC-001 through
NFR-SEC-006.

Existing ADRs cover authentication and infrastructure (ADR-0005 Keycloak,
ADR-0006 Kubernetes) but there is no single ADR capturing the full security
baseline across all cross-cutting concerns.

## Decision

The following security baseline applies to all services in the OpenDebt system:

1. **Authentication** — every non-public API endpoint requires a valid JWT issued
   by the Keycloak instance (ADR-0005). Unauthenticated requests return 401 with
   a structured error body.

2. **Authorisation** — state-mutating service methods carry an explicit
   `@PreAuthorize` annotation checked before business logic executes (Spring
   Security). Missing annotations on write endpoints are a blocking code-review
   finding.

3. **Secrets management** — credentials, tokens, and API keys are injected via
   Kubernetes Secrets or a compatible secrets manager. No plaintext secrets in
   source code, committed configuration, or application logs.

4. **Transport security** — all inter-service and external HTTP communication
   uses HTTPS. TLS termination is handled at the Kubernetes ingress layer
   (ADR-0006). Legacy M2M ingress uses mutual TLS.

5. **Input validation** — all service-boundary inputs are validated for type,
   length, format, and range using Jakarta Bean Validation or explicit guards.
   Validation failures return 400 with a structured error body.

6. **Dependency vulnerability scanning** — the CI pipeline includes OWASP
   Dependency-Check. Builds with known HIGH or CRITICAL CVEs fail. Accepted
   risks are tracked in `owasp-suppressions.xml` with a documented justification.

## Consequences

- NFR-SEC-001 through NFR-SEC-006 in `compliance/nfr-register.yaml` reference
  this ADR as their normative source.
- Code reviewers apply this baseline as a checklist on every PR.
- The `code-reviewer-strict` agent enforces points 1–5 as blocking findings.
- Point 6 is enforced by CI and tracked in `owasp-suppressions.xml`.

## Related ADRs

- ADR-0005 Keycloak Authentication
- ADR-0006 Kubernetes Deployment
- ADR-0014 GDPR Data Isolation
