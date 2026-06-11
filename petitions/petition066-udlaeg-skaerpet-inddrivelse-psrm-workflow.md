---
id: petition066
title: "Implement PSRM-side udlaeg workflow"
delivery_track: governed
status: draft
created: 2026-06-11
author: "Copilot CLI (Alice)"
---

## Context

OpenDebt currently models attachment only as a generic collection measure (`ATTACHMENT`) and lacks the dedicated G.A.3.2 PSRM workflow needed for court dispatch, callback handling, and legally correct interruption coupling to petition059.

## Problem Statement

PSRM has no single authoritative aggregate for attachment workflow state, correlation, and legal outcomes, creating risk of duplicate dispatch, invalid callback transitions, and incorrect prescription interruption events.

## Functional Requirements

- FR-01: The system must persist attachment workflows in a dedicated debt-service aggregate (`attachment_workflow`) as single writer source of truth.
- FR-02: The system must create workflows on debtor scope with explicit `coveredFordringIds` and initial status `REQUESTED`.
- FR-03: Workflow creation must be atomic on eligibility: if any covered claim is ineligible, creation is rejected with per-claim reasons.
- FR-04: Covered claim scope must be immutable from `REQUESTED`; scope changes require `WITHDRAWN` and new workflow creation.
- FR-05: The system must process dispatch idempotently per workflow; repeated dispatch commands must not create duplicate outbound court submissions.
- FR-06: Accepted dispatch must transition workflow to `IN_COURT_PROCESS` and store dispatch metadata including OpenDebt `workflowReference` (primary correlation key) and optional external case number.
- FR-07: Callback processing must require both matching debtor scope and matching `workflowReference`; mismatches or illegal transitions must be rejected without state change.
- FR-08: The workflow state machine must support `REQUESTED`, `IN_COURT_PROCESS`, `COMPLETED`, `UNSUCCESSFUL`, and `WITHDRAWN`.
- FR-09: `UNSUCCESSFUL` transitions must require standardized reason code enum value (`NO_ATTACHABLE_ASSETS`, `INSOLVENCY_DECLARED`, `LEGAL_OR_PROCEDURAL_DEFECT`, `THIRD_PARTY_RIGHT_BLOCK`, `COURT_REJECTION`) and may include optional free-text detail.
- FR-10: `COMPLETED` and `UNSUCCESSFUL` terminal transitions must atomically persist terminal state and register petition059 interruption using `type=UDLAEG`, legal reference from policy engine, and required court outcome date as event date.
- FR-11: Interruption emission must occur once per covered complex group (or standalone claim), relying on petition059 fordringskompleks propagation for member expansion.
- FR-12: `WITHDRAWN` transitions must require caseworker reason and must not emit interruption events.
- FR-13: Terminal callbacks must be idempotent per workflow and terminal status; exact duplicates are no-op.
- FR-14: The system must expose debtor-scoped read APIs with current workflow status, full status history, outcome qualifier, and interruption linkage metadata.
- FR-15: External fogedret dispatch/callback traffic must terminate at integration-gateway, which invokes internal debtor-scoped debt-service APIs.
- FR-16: Callback ingress must enforce OCES3 mTLS and replay protection on callback identity fields.

## Constraints and Assumptions

- Scope is PSRM-side orchestration only; court procedure internals are reference-only (Tier C).
- Petition066 depends on petition062 (phase dependency) and petition059 (prescription event model).
- Debt-service owns writes; case-service consumes projection/read-model updates.
- Callback mode is asynchronous dispatch/callback; synchronous court finalization is not in scope.
- Integration-gateway is mandatory external boundary for court-facing traffic.
- All person references must use technical IDs (`person_id` UUID); no CPR/PII storage outside Person Registry.
- Cross-service integration must use service APIs/events; no cross-service database access.
- Legal interruption references are policy-derived; workflow payload must not override legal reference text.

## Out of Scope

- Detailed judicial execution rules for specific asset classes (fast ejendom, loesoere, virksomhedspant).
- Court hearing/adjudication logic inside fogedretten.
- Tvangsauktion business handling beyond returned workflow outcome and metadata storage.
- New legal interpretation outside scoped G.A.3.2 PSRM workflow surface.
- Runtime-configurable unsuccessful reason code catalogs.
- Direct external callback exposure of debt-service endpoints.
