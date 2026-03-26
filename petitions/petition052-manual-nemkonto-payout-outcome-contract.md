# Outcome Contract: Petition 052 — Manual NemKonto Payout with 4-Eyes Approval

## Contract Header

| Field | Value |
|-------|-------|
| Petition ID | 052 |
| Title | Manual NemKonto Payout with 4-Eyes Approval |
| Type | Feature / Workflow / Financial |
| Scope | Payout initiation, tiered approval, NemKonto disbursement, timeline integration, threshold configuration |
| Status | Not Started |
| Created | 2026-06-05 |

## Acceptance Criteria

### Category A: Credit Balance and Initiation

#### AC-A1: "Ny udbetaling" button visibility
**Given** a case has a positive credit balance (kreditsaldo > 0 DKK) in the payment-service ledger
**When** a caseworker views the case detail page
**Then** a "Ny udbetaling" button is visible on the page

#### AC-A2: Button absent when no credit balance
**Given** a case has a credit balance of zero or a debit balance
**When** a caseworker views the case detail page
**Then** no "Ny udbetaling" button is displayed (not disabled — entirely absent)

#### AC-A3: Initiation form displays correct credit balance
**Given** a case has a credit balance of 2 500,00 DKK
**When** the caseworker clicks "Ny udbetaling"
**Then** the form displays the available credit balance as "2.500,00 DKK" (read-only, Danish locale)
**And** the amount field maximum is 2 500,00 DKK

#### AC-A4: Amount validation — zero amount rejected
**Given** the caseworker enters 0,00 DKK in the amount field
**When** the form is submitted
**Then** a validation error "Beløb skal være større end 0 kr." is displayed
**And** no `PayoutRequest` record is created

#### AC-A5: Amount validation — exceeds credit balance rejected
**Given** the case has a credit balance of 1 000,00 DKK
**And** the caseworker enters 1 500,00 DKK in the amount field
**When** the form is submitted
**Then** a validation error "Beløbet overstiger den tilgængelige kreditsaldo." is displayed
**And** no `PayoutRequest` record is created

#### AC-A6: Begrundelse is mandatory
**Given** the caseworker has entered a valid amount
**And** the "Begrundelse" field is empty
**When** the form is submitted
**Then** a validation error "Begrundelse er påkrævet." is displayed
**And** no `PayoutRequest` record is created

#### AC-A7: Successful initiation creates PayoutRequest
**Given** the caseworker enters a valid amount ≤ credit balance and a non-empty begrundelse
**When** the form is submitted
**Then** a `PayoutRequest` record is created in payment-service with status `PENDING_APPROVAL`
**And** the record contains: `payoutId` (UUID), `caseId`, `debtId`, `personId` (UUID), `amount`, `currency = DKK`, `requestedBy`, `requestedAt`, `begrundelse`, and `approvalTier`
**And** no NemKonto account number or other PII is stored on the record

#### AC-A8: Initiator cannot approve their own payout (server-side)
**Given** caseworker Alice creates a payout request
**When** the payment-service receives an approval request from Alice for that same payout
**Then** the approval is rejected with HTTP 403
**And** the payout remains in `PENDING_APPROVAL` status

---

### Category B: Approval Tier Routing

#### AC-B1: Small-amount payout routed to SMALL tier
**Given** `PAYOUT_SMALL_THRESHOLD` is configured as 10 000,00 DKK
**And** the debtor is not flagged as PEP or VIP
**And** the payout amount is 5 000,00 DKK (≤ threshold)
**When** the payout is initiated
**Then** `approvalTier` is set to `SMALL`
**And** any caseworker other than the initiator is eligible to approve

#### AC-B2: Large-amount payout routed to LARGE tier
**Given** `PAYOUT_SMALL_THRESHOLD` is 10 000,00 DKK and `PAYOUT_LARGE_THRESHOLD` is 100 000,00 DKK
**And** the debtor is not flagged as PEP or VIP
**And** the payout amount is 50 000,00 DKK (between thresholds)
**When** the payout is initiated
**Then** `approvalTier` is set to `LARGE`
**And** only a `ROLE_SUPERVISOR` is eligible to approve

#### AC-B3: Amount at exact SMALL threshold boundary routed to SMALL tier
**Given** `PAYOUT_SMALL_THRESHOLD` is 10 000,00 DKK
**And** the payout amount is exactly 10 000,00 DKK
**And** the debtor is not flagged as PEP or VIP
**When** the payout is initiated
**Then** `approvalTier` is set to `SMALL`

#### AC-B4: Amount above LARGE threshold routed to LARGE tier
**Given** `PAYOUT_LARGE_THRESHOLD` is 100 000,00 DKK
**And** the payout amount is 150 000,00 DKK
**And** the debtor is not flagged as PEP or VIP
**When** the payout is initiated
**Then** `approvalTier` is set to `LARGE`
**And** only a `ROLE_SUPERVISOR` is eligible to approve

#### AC-B5: PEP debtor routed to PEP_VIP tier regardless of amount
**Given** the debtor is flagged as PEP in person-registry
**And** the payout amount is 500,00 DKK (below `PAYOUT_SMALL_THRESHOLD`)
**When** the payout is initiated
**Then** `approvalTier` is set to `PEP_VIP`
**And** only a `ROLE_DIRECTOR` is eligible to approve

#### AC-B6: VIP debtor routed to PEP_VIP tier regardless of amount
**Given** the debtor is flagged as VIP in person-registry
**And** the payout amount is 200 000,00 DKK (above `PAYOUT_LARGE_THRESHOLD`)
**When** the payout is initiated
**Then** `approvalTier` is set to `PEP_VIP`
**And** only a `ROLE_DIRECTOR` is eligible to approve

#### AC-B7: PEP/VIP flag re-checked at approval time
**Given** a payout was initiated with tier `SMALL` (debtor was not PEP at initiation time)
**And** the debtor has since been flagged as PEP in person-registry
**When** a caseworker attempts to approve the payout
**Then** the approval is rejected with an error "Skyldner er nu markeret som PEP/VIP — godkendelse kræver direktørrolle"
**And** the payout `approvalTier` is updated to `PEP_VIP`

#### AC-B8: Missing threshold configuration blocks initiation
**Given** no active `PAYOUT_SMALL_THRESHOLD` entry exists in `business_config`
**When** a caseworker attempts to initiate a payout
**Then** the initiation fails with error "Udbetalingsgrænser er ikke konfigureret — kontakt en konfigurationsansvarlig"
**And** no `PayoutRequest` record is created

---

### Category C: Approval Queue

#### AC-C1: Caseworker sees only SMALL-tier payouts in queue
**Given** the queue contains payouts of tier SMALL, LARGE, and PEP_VIP
**And** the current user has role `ROLE_CASEWORKER`
**When** the caseworker opens the "Udbetalingskø"
**Then** only `SMALL`-tier payouts that the caseworker did not initiate are displayed
**And** no `LARGE` or `PEP_VIP` tier payouts appear

#### AC-C2: Supervisor sees SMALL and LARGE tier payouts in queue
**Given** the queue contains payouts of tier SMALL, LARGE, and PEP_VIP
**And** the current user has role `ROLE_SUPERVISOR`
**When** the supervisor opens the "Udbetalingskø"
**Then** payouts of tier `SMALL` and `LARGE` are displayed
**And** no `PEP_VIP` tier payouts appear

#### AC-C3: Director sees only PEP_VIP tier payouts in queue
**Given** the queue contains payouts of tier SMALL, LARGE, and PEP_VIP
**And** the current user has role `ROLE_DIRECTOR`
**When** the director opens the "Udbetalingskø"
**Then** only `PEP_VIP`-tier payouts are displayed

#### AC-C4: Queue entry displays required fields
**Given** a payout in PENDING_APPROVAL status is in the queue
**When** the approver views the queue
**Then** each entry displays: payout ID, case reference, amount (DKK), initiation date, initiating user, tier badge, and a "Gennemgå" action link

#### AC-C5: Initiator's own payouts excluded from their queue view
**Given** caseworker Alice has initiated a `SMALL`-tier payout
**When** Alice opens the "Udbetalingskø"
**Then** her own payout is not displayed in her queue

---

### Category D: Approval and Rejection Flows

#### DC-D1: Supervisor approves a LARGE-tier payout
**Given** a `LARGE`-tier payout is in `PENDING_APPROVAL` status
**And** the current user has role `ROLE_SUPERVISOR`
**When** the supervisor clicks "Godkend" on the review page
**Then** the payout status changes to `APPROVED`
**And** NemKonto disbursement is triggered

#### AC-D2: Caseworker approves a SMALL-tier payout
**Given** a `SMALL`-tier payout is in `PENDING_APPROVAL` status
**And** the approving caseworker is not the initiator
**When** the caseworker clicks "Godkend" on the review page
**Then** the payout status changes to `APPROVED`
**And** NemKonto disbursement is triggered

#### AC-D3: Director approves a PEP/VIP payout
**Given** a `PEP_VIP`-tier payout is in `PENDING_APPROVAL` status
**And** the current user has role `ROLE_DIRECTOR`
**When** the director clicks "Godkend" on the review page
**Then** the payout status changes to `APPROVED`
**And** NemKonto disbursement is triggered

#### AC-D4: Rejection requires a mandatory reason
**Given** a payout is in `PENDING_APPROVAL` status
**And** an eligible approver views the review page
**When** the approver clicks "Afvis" without entering a rejection reason
**Then** a validation error "Afvisningsårsag er påkrævet." is displayed
**And** the payout remains in `PENDING_APPROVAL` status

#### AC-D5: Rejection with reason sets status to REJECTED
**Given** a payout is in `PENDING_APPROVAL` status
**And** an eligible approver enters a rejection reason
**When** the approver clicks "Afvis"
**Then** the payout status changes to `REJECTED`
**And** no ledger entries are created
**And** the credit balance on the debt is unchanged

#### AC-D6: Role mismatch blocks approval
**Given** a `LARGE`-tier payout is in `PENDING_APPROVAL` status
**And** the current user has only `ROLE_CASEWORKER`
**When** the caseworker attempts to approve via the API
**Then** the approval is rejected with HTTP 403
**And** the payout remains in `PENDING_APPROVAL` status

#### AC-D7: Review page suppresses Approve/Reject for the initiator
**Given** caseworker Alice initiated a payout
**When** Alice navigates to the review page for her own payout
**Then** no "Godkend" or "Afvis" button is displayed
**And** a message indicates she is the initiator and cannot approve

#### AC-D8: Optimistic locking prevents duplicate approval
**Given** two eligible approvers simultaneously open the same `PENDING_APPROVAL` payout
**When** both attempt to approve within the same millisecond
**Then** exactly one approval succeeds
**And** the second attempt receives an error "Udbetalingen er allerede behandlet af en anden bruger"
**And** the payout is not double-disbursed

---

### Category E: Cancellation

#### AC-E1: Initiator can cancel a PENDING_APPROVAL payout
**Given** caseworker Alice initiated a payout now in `PENDING_APPROVAL` status
**When** Alice cancels the payout with a reason
**Then** the payout status changes to `CANCELLED`
**And** the credit balance is unchanged

#### AC-E2: Cancellation requires a mandatory reason
**Given** a payout is in `PENDING_APPROVAL` status
**When** the initiator attempts to cancel without providing a reason
**Then** a validation error "Annulleringsårsag er påkrævet." is displayed
**And** the payout remains in `PENDING_APPROVAL` status

#### AC-E3: Non-initiators cannot cancel
**Given** a payout is in `PENDING_APPROVAL` status initiated by Alice
**When** a different caseworker (Bob) attempts to cancel via the API
**Then** the cancellation is rejected with HTTP 403
**And** the payout remains in `PENDING_APPROVAL` status

#### AC-E4: APPROVED payouts cannot be cancelled
**Given** a payout is in `APPROVED` status
**When** any user attempts to cancel
**Then** the cancellation is rejected with an error "Godkendte udbetalinger kan ikke annulleres"
**And** the payout status remains `APPROVED`

---

### Category F: NemKonto Disbursement

#### AC-F1: APPROVED payout triggers NemKonto disbursement
**Given** a payout transitions to `APPROVED` status
**When** the disbursement call to integration-gateway is successful
**Then** payout status changes to `DISBURSED`
**And** the disbursement reference (integration-gateway transaction ID) is stored on the record

#### AC-F2: No disbursement for REJECTED or CANCELLED payouts
**Given** a payout is in `REJECTED` or `CANCELLED` status
**Then** no call to integration-gateway is made
**And** no NemKonto disbursement occurs

#### AC-F3: NemKonto account number not stored in payment-service
**Given** a payout is successfully disbursed
**When** the `PayoutRequest` record and all related ledger entries are inspected
**Then** no NemKonto account number appears in any field

#### AC-F4: NemKonto account number not logged
**Given** integration-gateway resolves the NemKonto account for disbursement
**Then** the account number does not appear in any application log in payment-service or caseworker-portal

#### AC-F5: Disbursement failure sets DISBURSEMENT_FAILED
**Given** integration-gateway returns an error for the disbursement call
**When** payment-service receives the error
**Then** payout status remains `APPROVED` (not rolled back)
**And** a `DISBURSEMENT_FAILED` event is recorded
**And** a `PAYOUT_DISBURSEMENT_FAILED` timeline entry appears in the Tidslinje

#### AC-F6: Disbursement triggers double-entry bookkeeping
**Given** a payout is successfully disbursed
**Then** a double-entry ledger pair is created via `BookkeepingService`:
  a debit to the debtor liability account and a credit to the NemKonto clearing account
**And** both entries are dual-written to immudb

---

### Category G: Tidslinje (Timeline) Integration

#### AC-G1: PAYOUT_INITIATED event appears in Tidslinje
**Given** a caseworker initiates a payout
**When** the case Tidslinje is viewed by a caseworker or supervisor
**Then** a `PAYOUT_INITIATED` timeline entry is visible
**And** it displays the amount, tier badge, and "Afventer godkendelse" status badge

#### AC-G2: PAYOUT_APPROVED event appears in Tidslinje
**Given** an eligible approver approves a payout
**When** the Tidslinje is viewed by an internal user
**Then** a `PAYOUT_APPROVED` entry is visible with the approval timestamp and approver identity

#### AC-G3: PAYOUT_REJECTED event appears in Tidslinje
**Given** an eligible approver rejects a payout
**When** the Tidslinje is viewed by an internal user
**Then** a `PAYOUT_REJECTED` entry is visible with the rejection reason (internal roles only)

#### AC-G4: PAYOUT_CANCELLED event appears in Tidslinje
**Given** the initiator cancels a payout
**When** the Tidslinje is viewed by an internal user
**Then** a `PAYOUT_CANCELLED` entry is visible with the cancellation reason (internal roles only)

#### AC-G5: PAYOUT_DISBURSED event visible to citizen
**Given** a payout is successfully disbursed
**When** the citizen views their Tidslinje
**Then** a `PAYOUT_DISBURSED` entry is visible showing the amount and "Udbetalt" status
**And** no NemKonto account number, rejection reason, begrundelse, or internal metadata is visible

#### AC-G6: Internal payout events hidden from citizen
**Given** a case has `PAYOUT_INITIATED`, `PAYOUT_APPROVED`, `PAYOUT_REJECTED`, and `PAYOUT_CANCELLED` events
**When** the citizen views the Tidslinje
**Then** none of the above events are displayed
**And** only `PAYOUT_DISBURSED` events (if any) are visible to the citizen

#### AC-G7: Status badges on timeline entries
**Given** payout events of various statuses are in the Tidslinje
**Then** each entry displays a colour-coded status badge matching the status table in FR-10.1

---

### Category H: Threshold Configuration

#### AC-H1: PAYOUT_SMALL_THRESHOLD key supported in business_config
**Given** a `CONFIGURATION_MANAGER` creates a new `PAYOUT_SMALL_THRESHOLD` entry
**Then** the entry is created with status `PENDING_REVIEW`
**And** it appears in the Configuration Administration UI with the key `PAYOUT_SMALL_THRESHOLD`

#### AC-H2: Threshold value requires 4-eyes approval
**Given** a `CONFIGURATION_MANAGER` created a `PAYOUT_SMALL_THRESHOLD` entry
**When** the same user attempts to approve it
**Then** the approval is rejected with "Du kan ikke godkende din egen konfiguration"

#### AC-H3: Second CONFIGURATION_MANAGER can approve threshold
**Given** a `PAYOUT_SMALL_THRESHOLD` entry is in `PENDING_REVIEW` status
**And** a different `CONFIGURATION_MANAGER` reviews it
**When** they approve the entry
**Then** the entry transitions to `APPROVED` status
**And** it becomes the active threshold for future payout initiations from `valid_from`

#### AC-H4: LARGE threshold must be greater than SMALL threshold
**Given** `PAYOUT_SMALL_THRESHOLD` is active at 10 000,00 DKK
**When** a `CONFIGURATION_MANAGER` attempts to approve a `PAYOUT_LARGE_THRESHOLD` value of 8 000,00 DKK
**Then** the approval fails with validation error "PAYOUT_LARGE_THRESHOLD skal være større end PAYOUT_SMALL_THRESHOLD"

#### AC-H5: Threshold keys visible in Configuration Administration UI
**Given** the Configuration Administration UI (petition 047)
**When** a `CONFIGURATION_MANAGER` navigates to the configuration list
**Then** `PAYOUT_SMALL_THRESHOLD` and `PAYOUT_LARGE_THRESHOLD` are displayed with their current values and lifecycle status

---

### Category I: ROLE_DIRECTOR

#### AC-I1: ROLE_DIRECTOR exists in Keycloak dev and demo realms
**Given** the dev and demo Keycloak realm configurations
**Then** `ROLE_DIRECTOR` is defined as a valid realm role

#### AC-I2: director-user can authenticate and obtain ROLE_DIRECTOR token
**Given** the demo environment
**When** `director-user` authenticates with `director-password`
**Then** the JWT access token contains `ROLE_DIRECTOR` in the roles claim

#### AC-I3: Director can access PEP_VIP payout queue
**Given** PEP_VIP-tier payouts are in PENDING_APPROVAL status
**When** `director-user` opens the "Udbetalingskø"
**Then** the PEP_VIP payouts are displayed

#### AC-I4: Director cannot access case management actions
**Given** `director-user` is authenticated
**When** the director attempts to create a case, modify a debt, or access the configuration administration UI
**Then** HTTP 403 is returned for all such actions

#### AC-I5: ROLE_DIRECTOR does not grant access to immudb audit UI
**Given** `director-user` is authenticated
**When** the director navigates to `/audit/ledger`
**Then** an "Ingen adgang" page is shown or the user is redirected to the dashboard

---

### Category J: GDPR and Data Isolation

#### AC-J1: PayoutRequest contains no PII beyond personId
**Given** a payout is in any state
**When** the `PayoutRequest` record in payment-service is inspected
**Then** the only person-identifying field is `personId` (UUID)
**And** no CPR number, name, address, or NemKonto account number is present

#### AC-J2: API responses contain no PII beyond personId
**Given** a call to any payment-service payout endpoint is made
**When** the response body is inspected
**Then** no CPR number, name, or NemKonto account number appears

#### AC-J3: PEP/VIP flag not stored on PayoutRequest
**Given** a PEP_VIP-tier payout is created
**When** the `PayoutRequest` record is inspected
**Then** the `approvalTier` field contains `PEP_VIP`
**And** no field named `isPep`, `isVip`, or equivalent is stored on the record

---

### Non-Functional Acceptance Criteria

| NFR | Acceptance Criterion |
|-----|---------------------|
| Performance | Payout initiation form load completes in ≤ 400 ms server-side (p95) |
| Performance | Payout approval queue page load completes in ≤ 500 ms server-side (p95) |
| Atomicity | Payout state transition and corresponding ledger write are atomic; if the ledger write fails, the state transition is rolled back |
| Locking | Concurrent approval of the same payout results in exactly one success and one failure; no double-disbursement |
| Accessibility | All payout UI pages pass axe-core WCAG 2.1 AA automated checks |
| i18n | All user-facing labels are defined in `messages.properties` for locale `da` |
| Test coverage | payment-service JaCoCo line coverage remains ≥ 70 % and branch coverage ≥ 50 % after implementation |

---

## Definition of Done

1. All acceptance criteria above pass in the CI pipeline.
2. Flyway migration(s) for `payout_requests` table (and any supporting tables) reviewed and run successfully on a clean database.
3. `ROLE_DIRECTOR` added to Keycloak dev and demo realm JSON files; demo startup script picks up the change.
4. `director-user` / `director-password` demo credentials available in dev and demo.
5. `PAYOUT_SMALL_THRESHOLD` and `PAYOUT_LARGE_THRESHOLD` seed values committed to the `business_config` migration (for dev/demo environments).
6. PEP/VIP flag confirmed present on `PersonEntity` in person-registry (or its addition is delivered as part of this petition).
7. Integration-gateway NemKonto adapter confirmed operational in dev environment (or payout stops at `APPROVED` with a clear `DISBURSEMENT_FAILED` event if not yet available).
8. `docs/architecture-overview.md` updated: payout domain, `ROLE_DIRECTOR`, NemKonto channel.
9. `agents.md` updated: `payout_requests` table, new event types, `ROLE_DIRECTOR`.
10. Snyk code scan shows no new high or critical issues in changed code.
11. PR reviewed and approved before merge to `main`.

## Risks

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| NemKonto adapter not yet implemented in integration-gateway | Medium | High | Decouple approval from disbursement; payout reaches `APPROVED` and emits `DISBURSEMENT_FAILED` until adapter is ready. |
| PEP/VIP flag not present on PersonEntity | Medium | High | Confirm with person-registry owners before sprint start; add flag as part of this petition if needed. |
| ROLE_DIRECTOR scope too broad | Low | Medium | Define role permissions narrowly in Keycloak; validate with UAT. |
| Concurrent approvals cause double disbursement | Low | High | Enforce optimistic locking (`@Version`) on `PayoutRequest`; integration test for concurrent approval. |
| Threshold misconfiguration (SMALL ≥ LARGE) | Low | Medium | Server-side validation on config approval; display SMALL and LARGE together in configuration UI for easy review. |
| Credit balance changes between form load and submission | Low | Medium | Re-validate balance server-side at submission time; reject if balance has decreased below requested amount. |
