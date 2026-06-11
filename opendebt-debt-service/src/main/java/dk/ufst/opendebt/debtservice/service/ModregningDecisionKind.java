package dk.ufst.opendebt.debtservice.service;

/** Debtor-facing decision kinds within a modregning lineage. */
public enum ModregningDecisionKind {
  EXTERNAL_DISBURSEMENT_DECISION,
  SUPERSEDING_WAIVER_DECISION,
  CORRECTION_POOL_SETTLEMENT_DECISION
}
