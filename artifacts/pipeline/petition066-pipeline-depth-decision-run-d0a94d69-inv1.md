# Pipeline Depth Decision — petition066

- Run ID: d0a94d69-d0c9-4b17-b833-6cae274957ce
- Stage: pipeline-depth-decision
- Invocation: 1/1
- Decision mode: direct conductor fallback
- Timestamp (UTC): 2026-06-12T12:19:08Z

## Canonical artifacts reopened in this run
- `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.md`
- `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow-outcome-contract.md`
- `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature`

## Fresh repo evidence considered
- Petition frontmatter declares `delivery_track: governed`.
- Petition requires a dedicated debt-service aggregate with a multi-state workflow (`REQUESTED`, `IN_COURT_PROCESS`, `COMPLETED`, `UNSUCCESSFUL`, `WITHDRAWN`).
- Petition spans multiple bounded contexts/services: integration-gateway external boundary, debt-service write ownership, case-service read-model consumption, and petition059 interruption handling.
- Petition includes compliance/security obligations: OCES3 mTLS and replay protection on callback ingress.
- Petition includes legal/policy coupling: policy-derived legal references and prescription interruption registration for terminal court outcomes.
- Feature and outcome contract preserve that same architecture-heavy intent rather than a local or isolated change.

## Decision
- RESOLVE FULL: keep the run on the full architecture path.

## Rationale
A lean path is not appropriate because the petition is governed, cross-service, stateful, externally integrated, security-sensitive, and legally coupled. Those characteristics require full architecture treatment and validation depth.
