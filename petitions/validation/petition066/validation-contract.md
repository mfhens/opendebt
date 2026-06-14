# Validation Contract — petition066

## VAL-P066-001: Create workflow with eligible covered claims

**Source**: `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature` — Scenario: "Create workflow with eligible covered claims"  
**Description**: Creating an attachment workflow for an eligible debtor/claim set stores a new workflow in `REQUESTED` state with a unique `workflowReference`.  
**Pass criteria**:
- The create response is successful.
- The created workflow is visible on the debtor-scoped read surface.
- The visible workflow status is `REQUESTED`.
- The visible workflow has a non-empty `workflowReference`.
**Fail criteria**: Any failed creation, missing workflow, wrong status, or missing `workflowReference` is observed.  
**Required evidence**: network

## VAL-P066-002: Reject creation when any covered claim is ineligible

**Source**: `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature` — Scenario: "Reject creation when any covered claim is ineligible"  
**Description**: If one covered claim is ineligible, the request fails atomically and no workflow is created.  
**Pass criteria**:
- The create response is rejected.
- The response exposes an ineligibility reason for the offending claim.
- No workflow for the rejected input becomes visible on the debtor-scoped read surface.
**Fail criteria**: Any visible created workflow or missing ineligibility reason is observed.  
**Required evidence**: network

## VAL-P066-003: Accepted dispatch moves workflow to IN_COURT_PROCESS

**Source**: `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature` — Scenario: "Accepted dispatch moves workflow to IN_COURT_PROCESS"  
**Description**: Dispatching a `REQUESTED` workflow moves it to `IN_COURT_PROCESS` and stores dispatch metadata.  
**Pass criteria**:
- The dispatch response is successful.
- The visible workflow status becomes `IN_COURT_PROCESS`.
- Dispatch metadata is visible on the workflow read surface.
**Fail criteria**: Any missing transition or missing dispatch metadata is observed.  
**Required evidence**: network

## VAL-P066-004: Repeated dispatch command is idempotent

**Source**: `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature` — Scenario: "Repeated dispatch command is idempotent"  
**Description**: Dispatching an already-dispatched workflow does not create a duplicate outbound dispatch and returns the existing metadata.  
**Pass criteria**:
- The repeated dispatch response indicates idempotent replay or equivalent no-op semantics.
- The returned dispatch metadata matches the original dispatch metadata.
- The visible workflow remains single-dispatched and in `IN_COURT_PROCESS`.
**Fail criteria**: Any duplicate-dispatch evidence or changed dispatch metadata is observed.  
**Required evidence**: network

## VAL-P066-005: Covered claim scope is immutable after REQUESTED

**Source**: `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature` — Scenario: "Covered claim scope is immutable after REQUESTED"  
**Description**: Attempts to mutate covered claims after workflow creation are rejected and instruct callers to withdraw and recreate.  
**Pass criteria**:
- The mutation request is rejected.
- The visible covered claim set remains unchanged.
- The visible error or response instructs withdrawal and recreation.
**Fail criteria**: Any visible scope mutation or missing guidance is observed.  
**Required evidence**: network

## VAL-P066-006: Callback requires both debtor scope and workflow reference match

**Source**: `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature` — Scenario: "Callback requires both debtor scope and workflow reference match"  
**Description**: A callback is rejected when debtor scope and `workflowReference` do not both match the intended workflow.  
**Pass criteria**:
- The callback response is rejected.
- The visible workflow state remains unchanged.
**Fail criteria**: Any accepted mismatch callback or any visible workflow mutation is observed.  
**Required evidence**: network

## VAL-P066-006B: Callback with mismatched workflow reference is rejected

**Source**: `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature` — Scenario: "Callback with mismatched workflow reference is rejected"  
**Description**: A callback is rejected when the debtor matches but the supplied `workflowReference` does not match the target workflow.  
**Pass criteria**:
- The callback response is rejected.
- The visible workflow state for the debtor remains unchanged.
**Fail criteria**: Any accepted mismatched-workflowReference callback or any visible workflow mutation is observed.  
**Required evidence**: network

## VAL-P066-007: COMPLETED callback persists terminal state and policy-derived interruption atomically

**Source**: `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature` — Scenario: "COMPLETED callback persists terminal state and policy-derived interruption atomically"  
**Description**: A `COMPLETED` callback moves the workflow to terminal state and registers a petition059 `UDLAEG` interruption in the same visible outcome using the policy-derived legal reference.  
**Pass criteria**:
- The callback response is successful.
- The visible workflow status becomes `COMPLETED`.
- The visible interruption linkage shows type `UDLAEG`.
- The visible interruption event date matches the court outcome date.
- The visible interruption linkage uses the policy-derived legal reference for `UDLAEG`.
**Fail criteria**: Any missing terminal state, missing interruption linkage, wrong interruption date, or missing policy-derived legal reference is observed.  
**Required evidence**: network

## VAL-P066-008: UNSUCCESSFUL callback requires reason code and registers interruption

**Source**: `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature` — Scenario: "UNSUCCESSFUL callback requires reason code and registers interruption"  
**Description**: An `UNSUCCESSFUL` callback with a canonical reason code stores the terminal status and still registers `UDLAEG` interruption.  
**Pass criteria**:
- The callback response is successful.
- The visible workflow status becomes `UNSUCCESSFUL`.
- The visible unsuccessful reason code matches the submitted canonical value.
- Visible interruption linkage shows type `UDLAEG`.
**Fail criteria**: Any accepted callback without visible reason code persistence or missing interruption linkage is observed.  
**Required evidence**: network

## VAL-P066-009: UNSUCCESSFUL callback without reason code is rejected

**Source**: `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature` — Scenario: "UNSUCCESSFUL callback without reason code is rejected"  
**Description**: An `UNSUCCESSFUL` callback without canonical reason code fails and leaves the workflow unchanged.  
**Pass criteria**:
- The callback response is rejected.
- The visible workflow remains in `IN_COURT_PROCESS`.
**Fail criteria**: Any accepted callback or any visible terminal-state mutation is observed.  
**Required evidence**: network

## VAL-P066-010: WITHDRAWN requires reason and emits no interruption

**Source**: `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature` — Scenario: "WITHDRAWN requires reason and emits no interruption"  
**Description**: Withdrawing a workflow requires a caseworker reason and must not produce petition059 interruption linkage.  
**Pass criteria**:
- The withdraw response is successful.
- The visible workflow status becomes `WITHDRAWN`.
- The visible workflow records the withdrawal reason.
- No interruption linkage is visible for the withdrawn workflow.
**Fail criteria**: Any missing reason or any visible interruption linkage is observed after withdrawal.  
**Required evidence**: network

## VAL-P066-011: Interruption emission occurs once per covered complex group

**Source**: `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature` — Scenario: "Interruption emission occurs once per covered complex group"  
**Description**: Terminal workflow completion emits one interruption per covered claim complex group or standalone claim, not per claim row inside one complex.  
**Pass criteria**:
- The visible interruption linkage count equals the number of covered complex groups/standalone claims in the scenario.
- No duplicate linkage is visible for claims inside the same claim complex.
**Fail criteria**: Any extra per-claim duplication inside the same complex is observed.  
**Required evidence**: network

## VAL-P066-012: Duplicate terminal callback is idempotent no-op

**Source**: `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature` — Scenario: "Duplicate terminal callback is idempotent no-op"  
**Description**: Replaying the same terminal callback must not duplicate interruption effects and should be visible as idempotent no-op history.  
**Pass criteria**:
- The replayed callback does not create additional interruption linkage entries.
- The visible status history records the replay as idempotent no-op or equivalent audit note.
**Fail criteria**: Any duplicated interruption linkage or silent terminal duplication is observed.  
**Required evidence**: network

## VAL-P066-013: Legal reference is policy-derived and not caller-overridden

**Source**: `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature` — Scenario: "Legal reference is policy-derived and not caller-overridden"  
**Description**: Terminal interruption linkage uses the policy-derived legal reference and ignores any caller-supplied override.  
**Pass criteria**:
- The visible interruption linkage contains the policy-derived legal reference for `UDLAEG`.
- Any caller-provided legal-reference override is absent from the persisted visible result.
**Fail criteria**: Any persisted caller override or missing policy-derived legal reference is observed.  
**Required evidence**: network

## VAL-P066-014: Debtor-scoped read returns status history and interruption linkage

**Source**: `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature` — Scenario: "Debtor-scoped read returns status history and interruption linkage"  
**Description**: Debtor-scoped reads expose current status, chronological history, and interruption linkage metadata for terminal workflows.  
**Pass criteria**:
- The debtor-scoped read returns all workflows for the debtor in the scenario.
- Each returned workflow exposes current status.
- Each returned workflow exposes chronological status history.
- Terminal workflows expose interruption linkage metadata.
**Fail criteria**: Any missing workflow, missing status history, or missing terminal linkage metadata is observed.  
**Required evidence**: network

## VAL-P066-015: Integration-gateway is the external callback boundary

**Source**: `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature` — Scenario: "Integration-gateway is the external callback boundary"  
**Description**: External callback traffic terminates at integration-gateway and is forwarded internally to debt-service rather than directly exposing debt-service externally.  
**Pass criteria**:
- The observable callback target is integration-gateway, not debt-service.
- The forwarded internal call targets the internal debtor-scoped debt-service API.
**Fail criteria**: Any direct external debt-service callback surface is observed.  
**Required evidence**: network

## VAL-P066-016: Callback ingress requires OCES3 mTLS at gateway

**Source**: `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature` — Scenario: "Callback ingress requires OCES3 mTLS at gateway"  
**Description**: An external callback without valid OCES3 mTLS client authentication is rejected at integration-gateway and causes no downstream workflow mutation.  
**Pass criteria**:
- The callback is rejected by integration-gateway for missing or invalid OCES3 mTLS client authentication.
- No downstream workflow state change is visible after the rejection.
**Fail criteria**: Any accepted unauthenticated callback or any downstream state mutation is observed.  
**Required evidence**: network

## VAL-P066-017: Callback replay is blocked at gateway

**Source**: `petitions/petition066-udlaeg-skaerpet-inddrivelse-psrm-workflow.feature` — Scenario: "Callback replay is blocked at gateway"  
**Description**: Reusing the same callback identity tuple is rejected at gateway with no downstream workflow mutation.  
**Pass criteria**:
- The replayed callback is rejected as replay.
- No downstream workflow state change is visible after replay rejection.
**Fail criteria**: Any accepted replay or any downstream state mutation is observed.  
**Required evidence**: network

## UI note

If a petition066 caseworker browser route is introduced later, the validator should additionally capture screenshots for status-history rendering, interruption-linkage rendering, and withdrawal reason visibility. Until that route exists, API/network evidence is the primary validation basis.
