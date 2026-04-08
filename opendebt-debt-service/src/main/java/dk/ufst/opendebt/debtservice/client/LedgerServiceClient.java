package dk.ufst.opendebt.debtservice.client;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Stub HTTP client for posting double-entry ledger entries to payment-service — ADR-0018.
 *
 * <p>Calls POST /internal/ledger/double-entry on paymentService. Real HTTP integration to be wired
 * in infra ticket.
 */
@Component
public class LedgerServiceClient {

  /**
   * Posts a double-entry ledger entry for a modregning allocation.
   *
   * @param debtorPersonId UUID of the debtor
   * @param fordringId UUID of the fordring being covered
   * @param amount amount covered
   * @param modregningEventId the originating modregning event
   */
  public void postLedgerEntry(
      UUID debtorPersonId, UUID fordringId, BigDecimal amount, UUID modregningEventId) {
    // Stub — real implementation calls POST /internal/ledger/double-entry on payment-service
  }

  /**
   * Reverses a previously posted ledger entry (used by tier-2 waiver).
   *
   * @param debtorPersonId UUID of the debtor
   * @param fordringId UUID of the fordring
   * @param amount amount to reverse
   * @param modregningEventId the originating modregning event
   */
  public void reverseLedgerEntry(
      UUID debtorPersonId, UUID fordringId, BigDecimal amount, UUID modregningEventId) {
    // Stub — real implementation calls POST /internal/ledger/double-entry (reversal) on
    // payment-service
  }
}
