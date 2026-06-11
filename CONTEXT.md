# OpenDebt

OpenDebt is a public debt-collection domain for Danish public institutions. This glossary captures stable business terms so petitions and services do not drift into conflicting language.

## Set-off

**Set-off decision**:
A legal decision to apply a public disbursement against one or more debts in the statutory priority order.
_Avoid_: Modregning event, offset event

**Tier-2 waiver**:
A caseworker decision under GIL § 4, stk. 11 to skip tier-2 claims for a specific set-off decision without reopening tier-1.
_Avoid_: Re-run flag, waiver mutation

**Superseding decision**:
A new decision that replaces the operative effect of an earlier decision while leaving the earlier decision intact as history.
_Avoid_: In-place rewrite, overwrite

**Superseded decision**:
A historical set-off decision that no longer has operative effect but retains its own notice and appeal metadata.
_Avoid_: Deleted decision, erased decision

**Notice**:
The debtor communication that informs the debtor of a specific set-off decision or superseding decision.
_Avoid_: Async notification, side effect

**Lineage disclosure**:
The practice of identifying the predecessor or origin of a debtor-facing set-off decision in its notice.
_Avoid_: Orphan notice, unexplained replacement

**Appeal deadline**:
The last date on which a specific set-off decision may be appealed.
_Avoid_: Global case deadline, reused deadline

**Appeal target**:
The specific decision reference that an appeal is lodged against.
_Avoid_: Whole lineage, transport reference

**Gendækning**:
A redistribution of surplus to debts after reversal; it may stay internal while later settlement remains possible, but a material reallocation that becomes the operative outcome must surface as a debtor-facing decision.
_Avoid_: Always invisible adjustment, automatic non-decision

**Settlement decision**:
A new debtor-facing set-off decision created when a correction-pool settlement re-enters set-off, normally without tier-1 priority unless the law explicitly restores it, and otherwise following tier-2 then tier-3 ordering.
_Avoid_: Internal-only accounting step, pooled adjustment

**Decision lineage**:
The explicit predecessor-and-successor relationship between related set-off decisions.
_Avoid_: Inferred history, reconstructed chain

**Business idempotency key**:
A stable domain key that identifies one legal decision regardless of retries or transport-level duplicates.
_Avoid_: Random reference, transport UUID

**Decision reference**:
A domain-level reference used to identify a set-off decision in notices, appeals, and caseworker support.
_Avoid_: Raw UUID, transport reference

**Lineage reference**:
A stable domain-level reference that groups the related decisions derived from one classified external disbursement and its internal correction flow.
_Avoid_: Per-decision identifier, transport reference

**Root decision**:
The single external disbursement decision that anchors a decision lineage.
_Avoid_: Multiple roots, rootless correction flow

**Decision kind**:
The canonical category describing why a set-off decision exists.
_Avoid_: Inferred type, heuristic classification

**Identity fields**:
The immutable fields that define a set-off decision's legal identity.
_Avoid_: Mutable legal identity, rewritten decision core

**External disbursement decision**:
A set-off decision created from a classified public disbursement.
_Avoid_: Generic decision

**Superseding waiver decision**:
A set-off decision created when a tier-2 waiver supersedes an earlier decision.
_Avoid_: Updated original decision

**Correction-pool settlement decision**:
A set-off decision created when pooled funds are settled and re-enter set-off, using settlement-time timing while preserving the original payment category.
_Avoid_: Internal pool entry

**Gendækning reallocation decision**:
A debtor-facing set-off decision created when material gendækning changes the operative allocation outcome without waiting for a later settlement decision, carrying full notice and appeal semantics, superseding the prior operative decision, and using the original receipt date with a new decision date as its timing basis.
_Avoid_: Hidden reallocation, reused settlement decision

**Operative decision**:
The currently effective set-off decision in a decision lineage, regardless of whether notice delivery ultimately succeeds.
_Avoid_: Latest row, whichever decision was stored last

**Decision history**:
The ordered lineage of superseded and operative decisions for the same set-off matter.
_Avoid_: Flat event list, inferred timeline

**Timing basis**:
The receipt-date and decision-date basis used to evaluate timing-sensitive consequences for a specific set-off decision.
_Avoid_: Inherited timestamp by default, frozen timing

**Lifecycle metadata**:
Post-decision status information that may evolve without changing a decision's legal identity or substance.
_Avoid_: Rewritten allocation, rewritten category, rewritten kind

## Disbursements

**Payment category**:
A statutory category of public disbursement that determines which set-off rules and restrictions apply.
_Avoid_: Config key, runtime option, STANDARD_PAYMENT

**Eligible payment type**:
A payment category that the law permits RIM to intercept for set-off.
_Avoid_: YAML switch, deployment setting

**Correction-pool settlement**:
An internal settlement origin that re-enters set-off using pooled funds while preserving the original statutory payment category separately.
_Avoid_: Payment category, statutory disbursement type

**Correction-pool entry**:
An internal accounting state representing pooled surplus pending settlement.
_Avoid_: Debtor-facing decision, appeal target

**Original payment category**:
The statutory payment category from the originating disbursement that remains authoritative and immutable across the entire decision lineage.
_Avoid_: Remembered boolean, fallback category

**Derived restriction**:
A restriction inferred from a payment category and carried into later decisioning.
_Avoid_: Ad hoc flag, one-off exception

**Unclassified disbursement**:
A disbursement whose statutory payment category has not yet been resolved and therefore cannot be used for a legal set-off decision.
_Avoid_: Standard payment, generic payment

**Quarantined disbursement**:
An unclassified disbursement that is preserved for explicit human resolution and kept out of legal set-off processing until classified.
_Avoid_: Soft reject, best-effort decision

**Classification decision**:
An auditable human decision that resolves a quarantined disbursement into a statutory payment category before any set-off decision is created.
_Avoid_: Hidden release, debtor-facing set-off decision
