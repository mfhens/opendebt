# Execution Plan — 2026-03-15

## Wave 1 reconciliation summary

This plan supersedes the Wave 1 execution slice from `execution-plan-2026-03-14.md`.

| Field | Value |
|---|---|
| Scope | Wave 1 only (`petition008`, `petition009`, `petition010`) |
| Basis | Reconciled against `execution-backlog.yaml`, `program-status.yaml`, and the current repo implementation |
| Outcome | `petition008` implemented, `petition009` validated, `petition010` still in progress |

## Reconciled ticket state

### Completed

| Ticket | Status | Notes |
|---|---|---|
| W1-SPEC-01 | done | Petition008 executable contract exists |
| W1-SPEC-02 | done | Internal API boundaries frozen in OpenAPI |
| W1-BOOT-01 | done | `opendebt-creditor-service` module bootstrapped |
| W1-BOOT-02 | done | `person-registry` organization reference API exposed |
| W1-CRD-01 | done | Creditor persistence model, audit/history, and hierarchy support implemented |
| W1-CRD-02 | done | Reconciled to creditor lookup APIs and service layer actually required by petition009 |
| W1-CRD-03 | done | `validate-action` wired into `debt-service` |
| W1-CRD-04 | done | Petition009 acceptance tests and architecture tests implemented |
| W1-ACC-01 | done | Channel binding model and resolution service logic implemented |

### In progress

| Ticket | Status | Remaining work |
|---|---|---|
| W1-ACC-02 | in_progress | Expose `/api/v1/creditors/access/resolve` in a controller and add executable petition010 BDD coverage |

### Pending

| Ticket | Status | Blocked by |
|---|---|---|
| W1-ACC-03 | pending | W1-ACC-02 |
| W1-ACC-04 | pending | W1-ACC-03 |

## Mismatches resolved

1. `petition009` is no longer tracked as waiting on W1-CRD-04; the Cucumber steps and architecture tests already exist in `opendebt-creditor-service`.
2. W1-CRD-02 is aligned to the petition009 outcome contract: lookup APIs are implemented, while separate admin CRUD endpoints are not required for petition completion.
3. `petition010` remains in progress because the access-resolution service logic exists, but the REST endpoint and BDD coverage are still missing.
4. Petition009 acceptance coverage is tracked as **12 scenarios**, matching `opendebt-creditor-service/src/test/resources/features/petition009.feature`.

## Next implementation slice

### W1-ACC-02 — expose access resolution publicly inside creditor-service

| Field | Value |
|---|---|
| Ticket | W1-ACC-02 |
| Petition | petition010 |
| Modules | `opendebt-creditor-service`, `api-specs` |
| Objective | Complete the creditor-service backend capability for shared access resolution |
| Deliverables | REST controller endpoint, security annotation, request/response mapping reuse, petition010 BDD coverage |
| Unblocks | W1-ACC-03, then W1-ACC-04 |

## Recommended execution order

1. Finish W1-ACC-02 in `opendebt-creditor-service`.
2. Wire shared access resolution into `integration-gateway`, `creditor-portal`, and `debt-service` under W1-ACC-03.
3. Converge local/CI wiring and smoke coverage under W1-ACC-04.
