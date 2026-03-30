package dk.ufst.opendebt.debtservice.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import dk.ufst.opendebt.debtservice.entity.RenteGodtgoerelseRateEntry;
import dk.ufst.opendebt.debtservice.exception.NoRenteGodtgoerelseRateException;
import dk.ufst.opendebt.debtservice.repository.RenteGodtgoerelseRateEntryRepository;

@Service
public class RenteGodtgoerelseService {

  private final RenteGodtgoerelseRateEntryRepository rateEntryRepository;
  private final DanishBankingCalendar bankingCalendar;

  public RenteGodtgoerelseService(
      RenteGodtgoerelseRateEntryRepository rateEntryRepository,
      DanishBankingCalendar bankingCalendar) {
    this.rateEntryRepository = rateEntryRepository;
    this.bankingCalendar = bankingCalendar;
  }

  /**
   * Computes the rentegodtgørelse start date decision per SPEC-058 §3.4.
   *
   * <p>Algorithm: 1. If bankingDaysBetween(receiptDate, decisionDate) <= 5: return {startDate=null,
   * FIVE_BANKING_DAY} 2. If paymentType == "OVERSKYDENDE_SKAT": candidate =
   * LocalDate.of(indkomstAar+1, 9, 1) standard = receiptDate.plusMonths(1).withDayOfMonth(1)
   * startDate = max(candidate, standard) exceptionApplied = if candidate > standard then
   * KILDESKATTELOV else NONE 3. Otherwise: return
   * {startDate=receiptDate.plusMonths(1).withDayOfMonth(1), NONE}
   */
  public RenteGodtgoerelseDecision computeDecision(
      LocalDate receiptDate, LocalDate decisionDate, PaymentType paymentType, Integer indkomstAar) {

    int bankingDays = bankingCalendar.bankingDaysBetween(receiptDate, decisionDate);
    if (bankingDays <= 5) {
      return new RenteGodtgoerelseDecision(
          null, RenteGodtgoerelseDecision.ExceptionType.FIVE_BANKING_DAY);
    }

    LocalDate standard = receiptDate.plusMonths(1).withDayOfMonth(1);

    if (PaymentType.OVERSKYDENDE_SKAT == paymentType && indkomstAar != null) {
      LocalDate candidate = LocalDate.of(indkomstAar + 1, 9, 1);
      if (candidate.isAfter(standard)) {
        return new RenteGodtgoerelseDecision(
            candidate, RenteGodtgoerelseDecision.ExceptionType.KILDESKATTELOV);
      } else {
        return new RenteGodtgoerelseDecision(
            standard, RenteGodtgoerelseDecision.ExceptionType.NONE);
      }
    }

    return new RenteGodtgoerelseDecision(standard, RenteGodtgoerelseDecision.ExceptionType.NONE);
  }

  /**
   * Returns the godtgoerelseRatePercent for the given reference date. Looks up the entry with the
   * latest effectiveDate <= referenceDate.
   *
   * @throws NoRenteGodtgoerelseRateException if no rate covers the referenceDate
   */
  public BigDecimal computeRate(LocalDate referenceDate) {
    return rateEntryRepository
        .findFirstByEffectiveDateLessThanEqualOrderByEffectiveDateDesc(referenceDate)
        .map(RenteGodtgoerelseRateEntry::getGodtgoerelseRatePercent)
        .orElseThrow(() -> new NoRenteGodtgoerelseRateException(referenceDate));
  }
}
