# Specifications Translation — petition066

- Run ID: d0a94d69-d0c9-4b17-b833-6cae274957ce
- Stage: specifications
- Invocation: 1/3
- Translator skill: specs-translator
- Timestamp (UTC): 2026-06-12T12:28:40Z

## Canonical artifacts reopened in this run
- `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.md`
- `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow-outcome-contract.md`
- `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature`

## Specification artifacts reopened and repaired in this run
- `petitions/specs/petition066-specs.yaml`
- `petitions/validation/petition066/validation-contract.md`
- `petitions/reviews/petition066-specs-reviewer.yaml`

## Fix applied in this run
- The specs package was stale relative to the current feature/outcome contract:
  - it omitted the feature scenario `Callback with mismatched workflow reference is rejected`
  - it used the older scenario wording for AC-07 without the explicit policy-derived legal-reference phrasing
  - it omitted the explicit OCES3 mTLS gateway scenario from the module traces
  - it only traced acceptance through AC-16 even though the outcome contract now includes AC-17 replay protection
- Repaired `petitions/specs/petition066-specs.yaml` to restore full traceability across the current petition package.
- Repaired `petitions/validation/petition066/validation-contract.md` to add explicit validation coverage for the mismatched-workflowReference rejection and the OCES3 mTLS gateway rejection, and to align AC-07 wording with the current feature.

## Fresh validation evidence
- The repaired specs file now traces all 18 current feature scenarios.
- The repaired validation contract now traces all 18 current feature scenarios with explicit validation blocks.
- OpenAPI artifact targets remain concrete and unchanged:
  - `api-specs/openapi-debt-service-attachment-workflow-internal.yaml`
  - `api-specs/openapi-integration-gateway-fogedret-attachment.yaml`
- Cross-artifact consistency was revalidated for FR-01..FR-16, AC-01..AC-17, workflowReference handling, gateway boundary, OCES3 mTLS ingress, and replay blocking.

## Decision
- PASS: specifications translation executed successfully in this run after repairing stale spec/validation traces.
