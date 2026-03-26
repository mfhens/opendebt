# Petition 052: Manual NemKonto Payout with 4-Eyes Approval

## Summary

Enable caseworkers to initiate a manual payout of a credit balance (overpayment or surplus) on a
debt case to the debtor's NemKonto account — and exclusively to NemKonto. The payout must pass a
role-differentiated 4-eyes approval flow (4-øjne-princip) whose approval authority escalates based
on amount thresholds and debtor sensitivity flags (PEP/VIP). Thresholds are managed through the
existing time-versioned `business_config` table with its own 4-eyes lifecycle. Every payout event
— initiated, approved, rejected, or cancelled — appears in the unified Tidslinje (petition 050).
Only APPROVED payouts trigger actual NemKonto disbursement via integration-gateway.

## Context and Motivation

Overpayments and credit balances arise when a debtor pays more than the outstanding debt, when a
debt is partially written off, or when a previously disputed amount is resolved in the debtor's
favour. Currently OpenDebt has no internal mechanism to return such surpluses to the debtor: the
caseworker must arrange repayment outside the system, leaving no audit trail within the case.

Introducing a structured payout workflow closes this gap by:
- Providing an auditable, tamper-evident record of every payout decision (ADR-0018, ADR-0029).
- Enforcing proportionate authorisation controls that reflect Danish public-sector risk management
  (4-øjne-princip at three escalation levels).
- Integrating with NemKonto as the legally mandated single disbursement channel for Danish
  residents and companies.
- Respecting GDPR by keeping NemKonto account numbers (PII) exclusively in person-registry and
  referencing debtors only by `person_id` (UUID) throughout payment-service.

### Domain Terms

| Danish | English | Definition |
|--------|---------|------------|
| Udbetaling | Payout / Disbursement | Return of a credit balance to the debtor |
| Kreditsaldo | Credit balance | Positive balance remaining after overpayment or write-down |
| NemKonto | NemKonto | The Danish national payment account; mandatory disbursement channel for public bodies |
| 4-øjne-princip | Four-eyes principle | Workflow requiring a second authorised user to approve an action; creator ≠ approver |
| Sagsbehandler | Caseworker | Internal user who initiates a payout |
| Supervisor | Supervisor | Role authorised to approve large-amount payouts |
| Direktør | Director | New role (`ROLE_DIRECTOR`) authorised to approve PEP/VIP payouts |
| PEP | Politically Exposed Person | Debtor subject to enhanced due diligence; payout requires director approval |
| VIP | VIP | Debtor flagged for sensitivity in person-registry; treated like PEP for payout purposes |
| Udbetalingskø | Payout approval queue | Filtered view of payouts awaiting approval, shown to eligible approvers |
| Godkend | Approve | Second-actor authorisation that advances payout to APPROVED state |
| Afvis | Reject | Second-actor decision that moves payout to REJECTED state |
| Annuller | Cancel | Initiator withdraws a PENDING_APPROVAL payout before it is decided |
| Videresendt til NemKonto | Forwarded to NemKonto | Integration-gateway has dispatched the disbursement instruction |
| business_config | Business configuration | Time-versioned key/value store in debt-service |
| PAYOUT_SMALL_THRESHOLD | Small-amount threshold | Credit amount at or below which any other caseworker may approve |
| PAYOUT_LARGE_THRESHOLD | Large-amount threshold | Credit amount above which only a supervisor may approve (unless PEP/VIP) |

## Functional Requirements

### FR-1: Credit Balance Prerequisite

- **FR-1.1**: The caseworker portal shall display a "Ny udbetaling" button on the case detail
  page **only** when the case has a positive credit balance (kreditsaldo > 0 DKK).
- **FR-1.2**: The credit balance is derived from the double-entry ledger in payment-service
  (`BookkeepingService`). A ledger query returning a net positive balance qualifies.
- **FR-1.3**: When the credit balance is zero or negative, the "Ny udbetaling" button shall be
  absent (not disabled — entirely omitted) to prevent confusion.

### FR-2: Payout Initiation

- **FR-2.1**: Clicking "Ny udbetaling" opens a payout initiation form. The form shall display:
  - The available credit balance (read-only, derived from ledger).
  - An amount field (numeric, DKK) with a maximum equal to the current credit balance.
  - A free-text "Begrundelse" (reason/justification) field (mandatory, max 500 characters).
- **FR-2.2**: The system shall validate that the requested amount is > 0 and ≤ current credit balance.
  A validation error shall prevent submission and display a user-facing message.
- **FR-2.3**: On submission, payment-service creates a `PayoutRequest` record with status
  `PENDING_APPROVAL`, recording: `payoutId` (UUID), `caseId`, `debtId`, `personId` (UUID),
  `amount`, `currency` (always DKK), `requestedBy` (user ID), `requestedAt`, `begrundelse`,
  and the applicable `approvalTier` (computed from FR-3).
- **FR-2.4**: The initiating caseworker shall not be eligible to approve their own payout (enforced
  server-side; `requestedBy` ≠ `approvedBy`).
- **FR-2.5**: Payout records shall never store the NemKonto account number or any other PII beyond
  `personId` (UUID). NemKonto resolution happens at disbursement time via person-registry.

### FR-3: Approval Tier Routing

The system shall determine the required approval tier at initiation time and store it on the
`PayoutRequest`. Tier determination applies the following rules **in priority order**:

| Priority | Condition | Required Approver | Tier Name |
|----------|-----------|-------------------|-----------|
| 1 (highest) | Debtor is flagged PEP or VIP in person-registry (any amount) | `ROLE_DIRECTOR` | `PEP_VIP` |
| 2 | Amount > `PAYOUT_LARGE_THRESHOLD` | `ROLE_SUPERVISOR` | `LARGE` |
| 3 | Amount ≤ `PAYOUT_SMALL_THRESHOLD` | Any `ROLE_CASEWORKER` ≠ initiator | `SMALL` |
| 4 | `PAYOUT_SMALL_THRESHOLD` < Amount ≤ `PAYOUT_LARGE_THRESHOLD` | `ROLE_SUPERVISOR` | `LARGE` |

- **FR-3.1**: PEP/VIP flag is retrieved from person-registry at initiation time using `personId`.
  The flag is not stored on the payout record — it is re-checked at approval time to detect
  flag changes between initiation and approval.
- **FR-3.2**: Threshold values are resolved from the **currently active** `business_config` entries
  for keys `PAYOUT_SMALL_THRESHOLD` and `PAYOUT_LARGE_THRESHOLD` (i.e., the row whose
  `valid_from` ≤ now() < `valid_to` with status `APPROVED`). If no active value exists, the
  system must reject the initiation with a configuration error.
- **FR-3.3**: The resolved `approvalTier` (`SMALL`, `LARGE`, or `PEP_VIP`) is stored on the
  `PayoutRequest` record for audit purposes.

### FR-4: Payout Approval Queue

- **FR-4.1**: The caseworker portal shall provide an "Udbetalingskø" (payout approval queue)
  view showing all `PENDING_APPROVAL` payouts for which the currently authenticated user is
  an eligible approver.
- **FR-4.2**: Visibility filtering in the queue:
  - `ROLE_CASEWORKER` sees payouts with tier `SMALL` that they did not initiate.
  - `ROLE_SUPERVISOR` sees payouts with tier `SMALL` (as fallback) and tier `LARGE`.
  - `ROLE_DIRECTOR` sees payouts with tier `PEP_VIP` (and may see `LARGE` as a fallback if
    no supervisor is available — **out of scope for this petition**).
- **FR-4.3**: Each queue entry shall display: payout ID, case reference, amount (DKK), initiation
  date, initiating caseworker (user ID / display name), tier badge, and a "Gennemgå" (Review)
  action link.
- **FR-4.4**: The queue shall refresh automatically when a payout in the queue is approved,
  rejected, or cancelled (HTMX polling or push — implementation detail).

### FR-5: Payout Review and Decision

- **FR-5.1**: Navigating to a specific payout from the queue opens a review page showing:
  case reference, credit balance at initiation, requested amount (DKK), begrundelse, initiating
  user, initiation timestamp, and approval tier.
- **FR-5.2**: The review page shall display a PEP/VIP indicator if applicable (re-checked live
  from person-registry at review time).
- **FR-5.3**: The eligible approver may:
  - **Approve**: Sets payout status to `APPROVED`; triggers NemKonto disbursement (FR-7).
  - **Reject**: Sets payout status to `REJECTED`; requires a mandatory rejection reason
    (free text, max 500 characters). The credit balance remains unchanged.
- **FR-5.4**: Server-side enforcement: the approver's role must match the required tier at the
  moment of approval (re-validated, not just at initiation). If the PEP/VIP flag was set after
  initiation, the system shall re-route to `PEP_VIP` tier and reject the approval attempt with
  an informative error.
- **FR-5.5**: A caseworker who initiated the payout must not see an "Approve" or "Reject"
  button on the review page for their own payouts (server-side enforcement + UI suppression).

### FR-6: Payout Cancellation

- **FR-6.1**: The initiating caseworker may cancel a payout while it is in `PENDING_APPROVAL`
  status. This sets status to `CANCELLED`.
- **FR-6.2**: Cancellation requires a mandatory reason field (free text, max 500 characters).
- **FR-6.3**: Once a payout is in `APPROVED`, `REJECTED`, or `CANCELLED` state, it cannot be
  cancelled or modified.

### FR-7: NemKonto Disbursement

- **FR-7.1**: Only payouts in `APPROVED` status trigger disbursement. Disbursement is initiated
  synchronously upon approval (within the same request) or via a short-lived async task
  (implementation choice), but must complete or fail within the same logical business transaction.
- **FR-7.2**: Disbursement is routed exclusively through integration-gateway. The payout channel
  is always NemKonto; no other channel (bank transfer by IBAN, cheque, etc.) is permitted.
- **FR-7.3**: Integration-gateway resolves the debtor's NemKonto account number by calling
  person-registry with the `personId`. The account number must not be logged or stored outside
  person-registry.
- **FR-7.4**: On successful disbursement, payout status transitions to `DISBURSED` and the
  disbursement reference (integration-gateway transaction ID) is stored on the payout record.
- **FR-7.5**: On disbursement failure (NemKonto unreachable, account not found, etc.), payout
  remains `APPROVED` and a `DISBURSEMENT_FAILED` event is recorded. The caseworker portal shall
  display the failure prominently on the case timeline. Retry logic is out of scope (separate
  petition).
- **FR-7.6**: Disbursement creates a corresponding double-entry ledger posting via
  `BookkeepingService` (ADR-0018) and is dual-written to immudb (ADR-0029).

### FR-8: Bookkeeping Integration

- **FR-8.1**: Every state transition of a `PayoutRequest` (PENDING_APPROVAL → APPROVED → DISBURSED,
  REJECTED, CANCELLED) shall be recorded as a `LedgerEntryEntity` pair (debit + credit) via
  `BookkeepingService` where a financial movement occurs.
- **FR-8.2**: At APPROVED/DISBURSED transition, the credit balance on the debt is reduced by the
  payout amount (debit to the debtor liability account, credit to the NemKonto clearing account).
- **FR-8.3**: At REJECTED or CANCELLED, no financial ledger entries are created (no money moved).
- **FR-8.4**: All ledger entries created for payout activities are dual-written to immudb per
  ADR-0029.

### FR-9: Tidslinje (Timeline) Integration

- **FR-9.1**: Every payout state transition shall emit a timeline event visible in the unified
  Tidslinje (petition 050) on the case detail page.
- **FR-9.2**: Timeline event types for payouts:

  | Status | Event Type | Category | Visible to |
  |--------|------------|----------|------------|
  | `PENDING_APPROVAL` created | `PAYOUT_INITIATED` | `FINANCIAL` | CASEWORKER, SUPERVISOR, DIRECTOR |
  | `APPROVED` | `PAYOUT_APPROVED` | `FINANCIAL` | CASEWORKER, SUPERVISOR, DIRECTOR |
  | `REJECTED` | `PAYOUT_REJECTED` | `FINANCIAL` | CASEWORKER, SUPERVISOR, DIRECTOR |
  | `CANCELLED` | `PAYOUT_CANCELLED` | `FINANCIAL` | CASEWORKER, SUPERVISOR, DIRECTOR |
  | `DISBURSED` | `PAYOUT_DISBURSED` | `FINANCIAL` | CASEWORKER, SUPERVISOR, DIRECTOR, CITIZEN |
  | `DISBURSEMENT_FAILED` | `PAYOUT_DISBURSEMENT_FAILED` | `FINANCIAL` | CASEWORKER, SUPERVISOR, DIRECTOR |

- **FR-9.3**: Citizen (debtor) can see only the `PAYOUT_DISBURSED` event in their portal timeline.
  All other payout events are internal only.
- **FR-9.4**: Each timeline entry for a payout shall display: amount, tier badge, and status badge.
  The `begrundelse` (reason) is shown for internal roles only; rejection/cancellation reason is
  shown for internal roles only.

### FR-10: Payout Status Badges in UI

- **FR-10.1**: The case detail page and payout queue shall display colour-coded status badges:

  | Status | Danish Label | Colour |
  |--------|-------------|--------|
  | `PENDING_APPROVAL` | Afventer godkendelse | Amber |
  | `APPROVED` | Godkendt | Blue |
  | `REJECTED` | Afvist | Red |
  | `CANCELLED` | Annulleret | Grey |
  | `DISBURSED` | Udbetalt | Green |
  | `DISBURSEMENT_FAILED` | Udbetalingsfejl | Red (with warning icon) |

### FR-11: Threshold Configuration in business_config

- **FR-11.1**: Two new configuration keys shall be introduced in the `business_config` table
  (owned by debt-service):
  - `PAYOUT_SMALL_THRESHOLD` — decimal value in DKK.
  - `PAYOUT_LARGE_THRESHOLD` — decimal value in DKK.
- **FR-11.2**: Both keys follow the identical time-versioned, 4-øjne-princip lifecycle already
  implemented for `RATE_NB_UDLAAN`, `RATE_INDR_STD`, etc.:
  `PENDING_REVIEW` → `APPROVED` (or `REJECTED`); creator ≠ approver.
- **FR-11.3**: A `CONFIGURATION_MANAGER` creates a new threshold value; a second
  `CONFIGURATION_MANAGER` or `SUPERVISOR` approves it. The new value is effective from `valid_from`.
- **FR-11.4**: If `PAYOUT_SMALL_THRESHOLD` ≥ `PAYOUT_LARGE_THRESHOLD`, the system shall reject
  the configuration approval with a validation error.
- **FR-11.5**: The Configuration Administration UI (petition 047) shall expose these two new keys
  in the same configuration management screens already used for rate keys.

### FR-12: ROLE_DIRECTOR

- **FR-12.1**: A new Keycloak role `ROLE_DIRECTOR` shall be added to the dev and demo realm
  configurations.
- **FR-12.2**: `ROLE_DIRECTOR` grants access to:
  - The payout approval queue (PEP_VIP tier only).
  - The payout review page for PEP_VIP tier payouts.
  - Read-only access to case detail pages (same as SUPERVISOR for viewing; no write actions
    beyond payout approval).
- **FR-12.3**: `ROLE_DIRECTOR` does not grant access to case creation, debt management,
  configuration administration, or the immudb audit UI.
- **FR-12.4**: A demo director user (`director-user` / `director-password`) shall be available
  in dev and demo environments.

### FR-13: GDPR Compliance

- **FR-13.1**: `PayoutRequest` records shall store only `personId` (UUID). No name, CPR, address,
  or NemKonto account number shall be persisted in payment-service.
- **FR-13.2**: NemKonto resolution (UUID → account number) occurs exclusively within
  integration-gateway at disbursement time. The resolved account number must not appear in any
  log, event, or response from payment-service.
- **FR-13.3**: PEP/VIP flag is looked up at initiation and approval time but not stored on the
  payout record. The `approvalTier` field (`SMALL`, `LARGE`, `PEP_VIP`) is stored as the
  derived outcome — not the flag itself.
- **FR-13.4**: The Tidslinje events for payouts must not expose the NemKonto account number,
  debtor name, or CPR in any field.

## Non-Functional Requirements

| NFR | Specification |
|-----|---------------|
| Security | All payout state-change endpoints require authentication; role checks enforced server-side |
| Auditability | All state transitions are persisted with actor identity, timestamp, and reason |
| Tamper-evidence | Ledger entries for payouts are dual-written to immudb (ADR-0029) |
| Performance | Payout initiation form load ≤ 400 ms; queue page load ≤ 500 ms (server-side, excluding network) |
| Accessibility | All payout UI pages shall meet WCAG 2.1 AA |
| Internationalisation | All UI labels in `messages.properties`; Danish (`da`) required, English (`en`) optional |
| Atomicity | Payout state transition and corresponding ledger write must be atomic (same `@Transactional` unit) |

## Constraints and Assumptions

- NemKonto is the **only** permitted disbursement channel. No configuration option for alternative
  channels is provided.
- The integration-gateway NemKonto adapter is assumed to exist or will be introduced as an
  implementation task (not a separate petition).
- Retry for failed disbursements (status `DISBURSEMENT_FAILED`) is out of scope for this petition.
- The PEP/VIP flag is a boolean field on the person-registry `PersonEntity`. If this field does
  not yet exist, its introduction is an implementation prerequisite (not a separate petition but
  a dependency surfaced here).
- The Supervisor approval of `SMALL` tier payouts (supervisor as fallback when no other
  caseworker is available) is out of scope for this petition.
- Concurrent approval attempts (two users simultaneously approving the same payout) must be
  handled via optimistic locking (`@Version` on `PayoutRequest`) — implementation detail.

## Dependencies

| Dependency | Petition / Component | Status | Impact |
|------------|---------------------|--------|--------|
| Double-entry bookkeeping | ADR-0018 / petition 040 | Implemented | All payout ledger postings go through `BookkeepingService` |
| Role-based access control | Petition 048 | Implemented | Role checks, `@PreAuthorize`, Keycloak realm configuration |
| Business configuration (versioned) | Petition 046 | Implemented | `PAYOUT_SMALL_THRESHOLD` and `PAYOUT_LARGE_THRESHOLD` keys |
| Configuration Administration UI | Petition 047 | Implemented | UI for managing threshold keys |
| Unified Case Timeline | Petition 050 | Implemented | Payout events appear in Tidslinje |
| immudb tamper-evidence | ADR-0029 / Petition 051 | Implemented | Payout ledger entries dual-written to immudb |
| `ROLE_DIRECTOR` | This petition (FR-12) | **New** | Must be added to Keycloak realms as part of this petition |
| PEP/VIP flag on PersonEntity | person-registry | **Gap** | Flag must exist in person-registry before payout routing can distinguish PEP/VIP |
| NemKonto adapter in integration-gateway | integration-gateway | **Gap** | Adapter must exist for disbursement; if not implemented, payout stops at APPROVED |

## Out of Scope

- Automatic payout of credit balances (batch/scheduled processing).
- Partial payout (initiating multiple payouts against the same credit balance simultaneously);
  a second payout may be initiated after the first is settled.
- Alternative disbursement channels (IBAN transfer, cheque, cash).
- Retry workflow for `DISBURSEMENT_FAILED` payouts.
- Supervisor approval of `SMALL` tier as a fallback when no other caseworker is available.
- `ROLE_DIRECTOR` access to any functionality beyond payout approval (e.g., report generation,
  configuration management).
- NemKonto real-time validation of account status at initiation time.
- Creditor portal or citizen portal payout initiation (caseworker portal only).
- Bulk approval of multiple payouts in a single action.

## Related ADRs

- ADR-0018: Double-Entry Bookkeeping (storno pattern)
- ADR-0029: immudb for Financial Ledger Integrity
- ADR-0022: Shared Audit Infrastructure
- ADR-0013: Enterprise PostgreSQL with Audit and History
