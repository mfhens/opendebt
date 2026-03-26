package dk.ufst.opendebt.payment.immudb;

import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity;

/**
 * Appends financial ledger entries to the immudb tamper-evidence layer (ADR-0029).
 *
 * <p>Implementations are fire-and-forget: any failure must be logged and retried independently. The
 * PostgreSQL transaction boundary must never be affected by immudb availability.
 *
 * <p>Two implementations exist:
 *
 * <ul>
 *   <li>{@link ImmuLedgerClient} — active when {@code opendebt.immudb.enabled=true}
 *   <li>{@link NoOpImmuLedgerAppender} — default no-op; used in tests and local dev
 * </ul>
 */
public interface ImmuLedgerAppender {

  /**
   * Appends one or more ledger entries to immudb asynchronously.
   *
   * <p>Called after PostgreSQL save, outside the transaction boundary. Failures must not propagate
   * to the caller.
   *
   * @param entries the debit and credit entries for a double-entry posting
   */
  void appendAsync(LedgerEntryEntity... entries);
}
