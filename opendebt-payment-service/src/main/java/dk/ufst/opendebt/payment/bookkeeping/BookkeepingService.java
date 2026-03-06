package dk.ufst.opendebt.payment.bookkeeping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Service for bi-temporal double-entry bookkeeping of all financial transactions in debt
 * collection. Each operation records both an effective date (when the event economically applies)
 * and a posting date (when it is recorded in the system).
 */
public interface BookkeepingService {

  /** Records a new debt as a receivable. Debit: Fordringer, Credit: Indrivelsesindtaegter. */
  void recordDebtRegistered(
      UUID debtId, BigDecimal principalAmount, LocalDate effectiveDate, String reference);

  /** Records a payment received via CREMUL. Debit: SKB Bank, Credit: Fordringer. */
  void recordPaymentReceived(
      UUID debtId, BigDecimal amount, LocalDate effectiveDate, String cremulReference);

  /** Records accrued interest. Debit: Renter tilgodehavende, Credit: Renteindtaegter. */
  void recordInterestAccrued(
      UUID debtId, BigDecimal interestAmount, LocalDate effectiveDate, String reference);

  /** Records an offsetting (modregning) transaction. Debit: Modregning, Credit: Fordringer. */
  void recordOffsetting(UUID debtId, BigDecimal amount, LocalDate effectiveDate, String reference);

  /** Records a debt write-off. Debit: Tab paa fordringer, Credit: Fordringer. */
  void recordWriteOff(UUID debtId, BigDecimal amount, LocalDate effectiveDate, String reference);

  /** Records a refund via DEBMUL. Debit: Fordringer, Credit: SKB Bank. */
  void recordRefund(
      UUID debtId, BigDecimal amount, LocalDate effectiveDate, String debmulReference);
}
