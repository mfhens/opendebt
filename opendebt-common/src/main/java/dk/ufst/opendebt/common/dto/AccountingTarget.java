package dk.ufst.opendebt.common.dto;

/**
 * Identifies who receives the money in accounting/bookkeeping entries.
 *
 * <p>Per gældsinddrivelsesloven: renter on the original debt (hovedstol) belong to the creditor
 * (fordringshaver), while fees (gebyrer) and renter on fees belong to the state (staten).
 */
public enum AccountingTarget {
  /** Renter (interest on principal) — tilfalder fordringshaver. */
  FORDRINGSHAVER,
  /** Gebyrer and renter on gebyrer — tilfalder staten. */
  STATEN
}
