package dk.ufst.opendebt.debtservice.offsetting;

import java.math.BigDecimal;
import java.util.UUID;

import dk.ufst.opendebt.debtservice.service.PaymentType;

/**
 * Domain service for offsetting (modregning) — matching incoming payments eligible for government
 * set-off against registered debts.
 *
 * <p>Offsetting logic lives in the debt service (ADR-0027) because:
 *
 * <ul>
 *   <li>Balance write-downs must be atomic with the {@code SET_OFF} CollectionMeasure record.
 *   <li>No external system integration is required (cf. wage-garnishment-service / EINDKOMST).
 * </ul>
 *
 * <p>Priority order and eligible payment types are configured under {@code opendebt.offsetting} in
 * {@code application.yml}.
 */
public interface OffsettingService {

  /**
   * Initiate an offsetting cycle for a given debtor. Matches the available offset amount against
   * ranked debts and records a {@code SET_OFF} CollectionMeasure for each debt that is fully or
   * partially offset.
   *
   * @param debtorPersonId the UUID identifying the debtor in the Person Registry
   * @param availableAmount the gross amount available for offsetting (before priority allocation)
   * @param paymentType the type of incoming payment (must be in eligible-payment-types)
   * @return a summary of the offsetting result
   */
  OffsettingResult initiateOffsetting(
      UUID debtorPersonId, BigDecimal availableAmount, PaymentType paymentType);
}
