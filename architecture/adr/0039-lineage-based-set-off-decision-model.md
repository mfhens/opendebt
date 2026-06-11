# Lineage-based set-off decision model

## Status

Accepted

P058 models debtor-facing set-off outcomes as explicit lineage-linked decisions rather than in-place mutation of a single `ModregningEvent`. Each lineage has exactly one root external disbursement decision and carries a stable `lineageReference`; each debtor-facing decision within that lineage carries its own immutable `decisionReference`, `decisionKind`, notice, appeal window, and stable business idempotency key. Appeals target the specific `decisionReference`, while debtor-facing and default read surfaces expose `decisionReference` and `lineageReference` rather than transport identifiers such as `nemkontoReferenceId`.

External disbursement decisions, superseding waiver decisions, gendækning reallocation decisions, and correction-pool settlement decisions are the debtor-facing decision kinds in scope. Internal accounting states such as correction-pool entries remain non-debtor-facing, and classification of an unresolved disbursement remains a pre-decision audit artifact rather than part of the debtor-facing lineage. The original payment category is immutable across the lineage; superseding and gendækning decisions reuse the original receipt date with a new decision date, while correction-pool settlement decisions use settlement-time timing and skip tier-1 unless statute explicitly restores it.
