# Petition066 Test Generation Run Evidence

- Run id: `d0a94d69-d0c9-4b17-b833-6cae274957ce`
- Stage: `test-generation`
- Invocation: `1/3`
- Date: `2026-06-12`

## Canonical artifacts reopened in this run

- `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.md`
- `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow-outcome-contract.md`
- `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature`
- `petitions/specs/petition066-specs.yaml`
- `petitions/validation/petition066/validation-contract.md`

## Repository repairs performed in this run

Created dry-run-discoverable petition066 executable coverage stubs bound to approved requirements:

- `opendebt-debt-service/src/test/resources/features/petition066.feature`
- `opendebt-debt-service/src/test/java/dk/ufst/opendebt/debtservice/steps/Petition066Steps.java`
- `opendebt-integration-gateway/src/test/resources/features/petition066.feature`
- `opendebt-integration-gateway/src/test/java/dk/ufst/opendebt/gateway/steps/Petition066Steps.java`

## Coverage intent

### Debt-service dry-run seams

The generated debt-service BDD pack marks petition066 coverage pending implementation until these seams exist:

- `POST /api/internal/v1/debtors/{debtorId}/attachment-workflows`
- `POST /api/internal/v1/debtors/{debtorId}/attachment-workflows/{workflowId}/dispatch`
- `POST /api/internal/v1/debtors/{debtorId}/attachment-workflows/{workflowId}/withdraw`
- `POST /api/internal/v1/debtors/{debtorId}/attachment-workflows/callbacks`
- `GET /api/internal/v1/debtors/{debtorId}/attachment-workflows`
- `GET /api/internal/v1/debtors/{debtorId}/attachment-workflows/{workflowId}`
- package `dk.ufst.opendebt.debtservice.attachment`
- types `AttachmentWorkflowApi`, `AttachmentWorkflowApplicationService`, `AttachmentCallbackValidator`, `AttachmentInterruptionBridge`

Trace summary encoded in the generated steps binds the pack to `AC-01` through `AC-14`, `workflowReference`, and petition059 interruption type `UDLAEG`.

### Integration-gateway dry-run seams

The generated gateway BDD pack marks petition066 coverage pending implementation until these seams exist:

- `POST /api/external/v1/fogedret/attachment-callbacks`
- `POST /api/external/v1/fogedret/attachment-dispatch`
- package `dk.ufst.opendebt.gateway.fogedret`
- types `FogedretCallbackController`, `FogedretReplayGuard`, `AttachmentGatewayClient`

Trace summary encoded in the generated steps binds the pack to `AC-15` through `AC-17`, `workflowReference`, replay protection, and `OCES3 mTLS` ingress constraints.

## Execution evidence from this run

Attempted to execute the generated pack with Maven:

1. `mvn -pl opendebt-debt-service,opendebt-integration-gateway -Dtest=RunCucumberTest test`
   - blocked before test execution by repository/toolchain issue in Spotless:
   - `java.lang.NoSuchMethodError: 'java.util.Queue com.sun.tools.javac.util.Log$DeferredDiagnosticHandler.getDiagnostics()'`
2. `mvn -pl opendebt-debt-service,opendebt-integration-gateway -Dspotless.check.skip=true -Dtest=RunCucumberTest test`
   - blocked during compilation by Java toolchain incompatibility:
   - `Fatal error compiling: java.lang.ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN`

## Result interpretation

This run truthfully produced repairable test artifacts and bound them to approved petition066 requirements. The remaining blocker is environmental/toolchain-level execution, not missing test coverage surfaces in the repository.
