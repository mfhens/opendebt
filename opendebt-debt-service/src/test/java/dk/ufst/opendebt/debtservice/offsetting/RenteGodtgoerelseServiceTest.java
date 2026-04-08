package dk.ufst.opendebt.debtservice.offsetting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.debtservice.entity.RenteGodtgoerelseRateEntry;
import dk.ufst.opendebt.debtservice.exception.NoRenteGodtgoerelseRateException;
import dk.ufst.opendebt.debtservice.repository.RenteGodtgoerelseRateEntryRepository;
import dk.ufst.opendebt.debtservice.service.DanishBankingCalendar;
import dk.ufst.opendebt.debtservice.service.PaymentType;
import dk.ufst.opendebt.debtservice.service.RenteGodtgoerelseDecision;
import dk.ufst.opendebt.debtservice.service.RenteGodtgoerelseService;

/**
 * Unit tests for {@code RenteGodtgoerelseService} — FR-4 implementation for petition P058.
 *
 * <p>Covered requirements:
 *
 * <ul>
 *   <li><b>FR-4.1</b>: Rate-change effective-date — new rate takes effect 5 banking days after
 *       publication
 *   <li><b>AC-12</b>: 5-banking-day exception — no rentegodtgørelse when decision ≤ 5 banking days
 *       after receipt
 *   <li><b>AC-13</b>: Kildeskattelov § 62/62A — OVERSKYDENDE_SKAT startDate override
 *   <li><b>Standard case</b>: decision > 5 banking days, non-OVERSKYDENDE_SKAT
 * </ul>
 *
 * <p>Spec reference: SPEC-058 §3.4
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RenteGodtgoerelseService — P058 unit tests")
class RenteGodtgoerelseServiceTest {

  @Mock private RenteGodtgoerelseRateEntryRepository rateEntryRepository;
  @Mock private DanishBankingCalendar bankingCalendar;

  private RenteGodtgoerelseService underTest;

  @BeforeEach
  void setUp() {
    underTest = new RenteGodtgoerelseService(rateEntryRepository, bankingCalendar);
  }

  // ── FR-4.1: Rate-change effective-date ──────────────────────────────────────

  @Nested
  @DisplayName(
      "FR-4.1: Rate-change effective-date — new rate takes effect 5 banking days after publication")
  class RateChangeEffectiveDate {

    /**
     * FR-4.1: computeRate must return the rate whose effectiveDate ≤ referenceDate. A rate
     * published but not yet effective must NOT be returned. Ref: SPEC-058 §3.4
     */
    @Test
    @DisplayName(
        "FR-4.1: computeRate returns previous rate when referenceDate < effectiveDate of new rate")
    void computeRate_returnsOldRateWhenNewRateNotYetEffective() {
      // Old rate: effectiveDate=2024-07-08, godtgoerelseRate=3.0%
      RenteGodtgoerelseRateEntry oldRate =
          RenteGodtgoerelseRateEntry.builder()
              .publicationDate(LocalDate.of(2024, 7, 1))
              .effectiveDate(LocalDate.of(2024, 7, 8))
              .referenceRatePercent(new BigDecimal("7.00"))
              .godtgoerelseRatePercent(new BigDecimal("3.00"))
              .build();

      // New rate: effectiveDate=2025-01-08, godtgoerelseRate=5.0%
      RenteGodtgoerelseRateEntry newRate =
          RenteGodtgoerelseRateEntry.builder()
              .publicationDate(LocalDate.of(2025, 1, 1))
              .effectiveDate(LocalDate.of(2025, 1, 8))
              .referenceRatePercent(new BigDecimal("9.00"))
              .godtgoerelseRatePercent(new BigDecimal("5.00"))
              .build();

      LocalDate beforeNewRate = LocalDate.of(2025, 1, 5);
      LocalDate onNewRate = LocalDate.of(2025, 1, 8);

      when(rateEntryRepository.findFirstByEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
              beforeNewRate))
          .thenReturn(Optional.of(oldRate));
      when(rateEntryRepository.findFirstByEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
              onNewRate))
          .thenReturn(Optional.of(newRate));

      assertThat(underTest.computeRate(beforeNewRate))
          .as("computeRate(2025-01-05) must return old rate 3.0% (new rate not yet effective)")
          .isEqualByComparingTo(new BigDecimal("3.00"));
      assertThat(underTest.computeRate(onNewRate))
          .as("computeRate(2025-01-08) must return new rate 5.0% (effective from this date)")
          .isEqualByComparingTo(new BigDecimal("5.00"));
    }

    /**
     * FR-4.1: computeRate throws NoRenteGodtgoerelseRateException when no entry covers
     * referenceDate. Ref: SPEC-058 §3.4 computeRate error contract
     */
    @Test
    @DisplayName(
        "FR-4.1: computeRate throws NoRenteGodtgoerelseRateException when no rate covers referenceDate")
    void computeRate_throwsWhenNoRateCoverReferenceDate() {
      when(rateEntryRepository.findFirstByEffectiveDateLessThanEqualOrderByEffectiveDateDesc(any()))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> underTest.computeRate(LocalDate.of(2025, 1, 1)))
          .as(
              "computeRate must throw NoRenteGodtgoerelseRateException when no entry covers the date")
          .isInstanceOf(NoRenteGodtgoerelseRateException.class);
    }
  }

  // ── AC-12: 5-banking-day exception ──────────────────────────────────────────

  @Nested
  @DisplayName("AC-12: 5-banking-day exception — no rentegodtgørelse when decision within 5 days")
  class FiveBankingDayException {

    /**
     * AC-12: ≤ 5 banking days → startDate=null, exceptionApplied=FIVE_BANKING_DAY. Ref: SPEC-058
     * §3.4
     */
    @Test
    @DisplayName("AC-12: ≤ 5 banking days → startDate=null, exceptionApplied=FIVE_BANKING_DAY")
    void withinFiveBankingDays_returnsNullStartDate() {
      when(bankingCalendar.bankingDaysBetween(LocalDate.of(2025, 3, 10), LocalDate.of(2025, 3, 13)))
          .thenReturn(3);

      RenteGodtgoerelseDecision decision =
          underTest.computeDecision(
              LocalDate.of(2025, 3, 10),
              LocalDate.of(2025, 3, 13),
              PaymentType.OVERSKYDENDE_SKAT,
              null);

      assertThat(decision.startDate())
          .as("startDate must be null when banking days ≤ 5 (SPEC-058 §3.4)")
          .isNull();
      assertThat(decision.exceptionApplied())
          .as("exceptionApplied must be FIVE_BANKING_DAY")
          .isEqualTo(RenteGodtgoerelseDecision.ExceptionType.FIVE_BANKING_DAY);
    }

    /**
     * AC-12: Exactly 5 banking days still triggers exception (boundary value test). Ref: SPEC-058
     * §3.4 — condition is "≤ 5"
     */
    @Test
    @DisplayName("AC-12: exactly 5 banking days → still triggers exception (boundary)")
    void exactlyFiveBankingDays_stillTriggersException() {
      when(bankingCalendar.bankingDaysBetween(any(LocalDate.class), any(LocalDate.class)))
          .thenReturn(5);

      RenteGodtgoerelseDecision decision =
          underTest.computeDecision(
              LocalDate.of(2025, 3, 10),
              LocalDate.of(2025, 3, 17),
              PaymentType.STANDARD_PAYMENT,
              null);

      assertThat(decision.startDate())
          .as("Exactly 5 banking days must still trigger 5-banking-day exception (boundary)")
          .isNull();
      assertThat(decision.exceptionApplied())
          .isEqualTo(RenteGodtgoerelseDecision.ExceptionType.FIVE_BANKING_DAY);
    }

    /** AC-12: > 5 banking days → standard start date applies (no exception). Ref: SPEC-058 §3.4 */
    @Test
    @DisplayName("AC-12: > 5 banking days → standard start date applies (no exception)")
    void moreThanFiveBankingDays_standardStartDateApplies() {
      when(bankingCalendar.bankingDaysBetween(LocalDate.of(2025, 3, 10), LocalDate.of(2025, 3, 24)))
          .thenReturn(10);

      RenteGodtgoerelseDecision decision =
          underTest.computeDecision(
              LocalDate.of(2025, 3, 10),
              LocalDate.of(2025, 3, 24),
              PaymentType.STANDARD_PAYMENT,
              null);

      // standard = receiptDate.plusMonths(1).withDayOfMonth(1) = 2025-04-01
      assertThat(decision.startDate())
          .as("startDate must be 2025-04-01 (1st of month after receiptDate)")
          .isEqualTo(LocalDate.of(2025, 4, 1));
      assertThat(decision.exceptionApplied())
          .isEqualTo(RenteGodtgoerelseDecision.ExceptionType.NONE);
    }
  }

  // ── AC-13: Kildeskattelov § 62/62A ───────────────────────────────────────────

  @Nested
  @DisplayName("AC-13: Kildeskattelov § 62/62A — OVERSKYDENDE_SKAT start date override")
  class KildeskattelovException {

    /**
     * AC-13: OVERSKYDENDE_SKAT → startDate = MAX(standard, 1 Sep after indkomstAar).
     * indkomstAar=2024 → candidate=2025-09-01. receiptDate=2025-04-01 → standard=2025-05-01. MAX =
     * 2025-09-01. Ref: SPEC-058 §3.4
     */
    @Test
    @DisplayName("AC-13: OVERSKYDENDE_SKAT → startDate = MAX(standard, 1 Sep after indkomstAar)")
    void overskydendeSkat_startDateIsMaxOfCandidateAndStandard() {
      when(bankingCalendar.bankingDaysBetween(any(LocalDate.class), any(LocalDate.class)))
          .thenReturn(10);

      RenteGodtgoerelseDecision decision =
          underTest.computeDecision(
              LocalDate.of(2025, 4, 1),
              LocalDate.of(2025, 4, 20),
              PaymentType.OVERSKYDENDE_SKAT,
              2024);

      // candidate=2025-09-01 > standard=2025-05-01 → KILDESKATTELOV exception
      assertThat(decision.startDate())
          .as("startDate must be 2025-09-01 (candidate wins over standard 2025-05-01)")
          .isEqualTo(LocalDate.of(2025, 9, 1));
      assertThat(decision.exceptionApplied())
          .as("exceptionApplied must be KILDESKATTELOV when candidate > standard")
          .isEqualTo(RenteGodtgoerelseDecision.ExceptionType.KILDESKATTELOV);
    }

    /**
     * AC-13: When standard date > 1 Sep candidate, standard date wins (MAX returns standard).
     * receiptDate=2026-10-01 → standard=2026-11-01, candidate=2026-09-01. startDate=2026-11-01.
     * Ref: SPEC-058 §3.4
     */
    @Test
    @DisplayName("AC-13: when standard date > 1 Sep candidate, standard date wins")
    void overskydendeSkat_standardDateWinsWhenLater() {
      when(bankingCalendar.bankingDaysBetween(any(LocalDate.class), any(LocalDate.class)))
          .thenReturn(10);

      RenteGodtgoerelseDecision decision =
          underTest.computeDecision(
              LocalDate.of(2026, 10, 1),
              LocalDate.of(2026, 10, 20),
              PaymentType.OVERSKYDENDE_SKAT,
              2025);

      // candidate=2026-09-01 < standard=2026-11-01 → standard wins, no KILDESKATTELOV
      assertThat(decision.startDate())
          .as("startDate must be 2026-11-01 (standard wins over candidate 2026-09-01)")
          .isEqualTo(LocalDate.of(2026, 11, 1));
      assertThat(decision.exceptionApplied())
          .as("exceptionApplied must be NONE when standard wins")
          .isEqualTo(RenteGodtgoerelseDecision.ExceptionType.NONE);
    }

    /**
     * AC-13: Non-OVERSKYDENDE_SKAT payment types use the standard start date. Ref: SPEC-058 §3.4
     */
    @Test
    @DisplayName("AC-13: non-OVERSKYDENDE_SKAT paymentType uses standard start date")
    void nonOverskydendeSkat_usesStandardStartDate() {
      when(bankingCalendar.bankingDaysBetween(any(LocalDate.class), any(LocalDate.class)))
          .thenReturn(10);

      RenteGodtgoerelseDecision decision =
          underTest.computeDecision(
              LocalDate.of(2025, 3, 10),
              LocalDate.of(2025, 4, 1),
              PaymentType.BOERNE_OG_UNGEYDELSE,
              null);

      assertThat(decision.startDate())
          .as("Non-OVERSKYDENDE_SKAT must use standard date 2025-04-01")
          .isEqualTo(LocalDate.of(2025, 4, 1));
      assertThat(decision.exceptionApplied())
          .isEqualTo(RenteGodtgoerelseDecision.ExceptionType.NONE);
    }
  }

  // ── Standard case ─────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("Standard case: decision > 5 banking days, non-OVERSKYDENDE_SKAT")
  class StandardCase {

    /**
     * Standard case: startDate = receiptDate.plusMonths(1).withDayOfMonth(1).
     * receiptDate=2025-03-10 → standard=2025-04-01. Ref: SPEC-058 §3.4
     */
    @Test
    @DisplayName("Standard: startDate = 1st of month after receiptDate")
    void standardCase_startDateIsFirstOfMonthAfterReceipt() {
      when(bankingCalendar.bankingDaysBetween(LocalDate.of(2025, 3, 10), LocalDate.of(2025, 3, 24)))
          .thenReturn(10);

      RenteGodtgoerelseDecision decision =
          underTest.computeDecision(
              LocalDate.of(2025, 3, 10),
              LocalDate.of(2025, 3, 24),
              PaymentType.STANDARD_PAYMENT,
              null);

      assertThat(decision.startDate())
          .as("Standard startDate must be 2025-04-01 (1st of month after 2025-03-10)")
          .isEqualTo(LocalDate.of(2025, 4, 1));
      assertThat(decision.exceptionApplied())
          .isEqualTo(RenteGodtgoerelseDecision.ExceptionType.NONE);
    }

    /**
     * Standard case boundary: receiptDate on last day of March. 2025-03-31.plusMonths(1) =
     * 2025-04-30 → withDayOfMonth(1) = 2025-04-01. Ref: SPEC-058 §3.4
     * plusMonths(1).withDayOfMonth(1)
     */
    @Test
    @DisplayName("Standard: receiptDate=last day of month → startDate = 1st of month after")
    void standardCase_receiptOnLastDayOfMonth_startDateIsCorrect() {
      when(bankingCalendar.bankingDaysBetween(LocalDate.of(2025, 3, 31), LocalDate.of(2025, 4, 20)))
          .thenReturn(10);

      RenteGodtgoerelseDecision decision =
          underTest.computeDecision(
              LocalDate.of(2025, 3, 31),
              LocalDate.of(2025, 4, 20),
              PaymentType.STANDARD_PAYMENT,
              null);

      // 2025-03-31.plusMonths(1) = 2025-04-30 → .withDayOfMonth(1) = 2025-04-01
      assertThat(decision.startDate())
          .as("2025-03-31.plusMonths(1).withDayOfMonth(1) = 2025-04-01")
          .isEqualTo(LocalDate.of(2025, 4, 1));
    }
  }
}
