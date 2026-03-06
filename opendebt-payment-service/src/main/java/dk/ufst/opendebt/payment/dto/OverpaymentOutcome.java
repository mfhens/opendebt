package dk.ufst.opendebt.payment.dto;

/**
 * High-level outcomes for excess amounts after an auto-matched payment exceeds the debt balance.
 */
public enum OverpaymentOutcome {

  /** Excess amount is paid out to the debtor. */
  PAYOUT,

  /** Excess amount is used to cover other debt posts for the same debtor. */
  COVER_OTHER_DEBTS
}
