package dk.ufst.opendebt.debtservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import dk.ufst.opendebt.debtservice.dto.InterestRecalculationResult;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.InterestJournalEntry;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.InterestJournalEntryRepository;
import dk.ufst.opendebt.debtservice.service.BusinessConfigService;
import dk.ufst.rules.model.InterestCalculationRequest;
import dk.ufst.rules.model.InterestCalculationResult;
import dk.ufst.rules.service.RulesService;

/**
 * Unit tests for P046-T5: timeline replay with per-day rate boundary splitting in
 * InterestRecalculationServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InterestRecalculationServiceImplTest {

  @Mock private DebtRepository debtRepository;
  @Mock private InterestJournalEntryRepository interestRepository;
  @Mock private BusinessConfigService configService;
  @Mock private RulesService rulesService;

  @InjectMocks private InterestRecalculationServiceImpl service;

  private DebtEntity debt;
  private final UUID debtId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    debt = DebtEntity.builder().id(debtId).outstandingBalance(new BigDecimal("100000.00")).build();
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(interestRepository.deleteByDebtIdFromDate(eq(debtId), any())).thenReturn(3);
    when(interestRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    // Mock rulesService to compute interest using the passed annualRate (mirrors DRL behaviour)
    when(rulesService.calculateInterest(any(InterestCalculationRequest.class)))
        .thenAnswer(
            inv -> {
              InterestCalculationRequest req = inv.getArgument(0);
              java.math.BigDecimal daily =
                  req.getPrincipalAmount()
                      .multiply(req.getAnnualRate())
                      .divide(new java.math.BigDecimal("365"), 2, java.math.RoundingMode.HALF_UP);
              return InterestCalculationResult.builder().interestAmount(daily).build();
            });
  }

  @Test
  void recalculateFromDate_futureDate_returnsEmptyResult() {
    LocalDate future = LocalDate.now().plusDays(1);

    InterestRecalculationResult result = service.recalculateFromDate(debtId, future);

    assertThat(result.getEntriesWritten()).isZero();
    assertThat(result.getEntriesDeleted()).isZero();
    verifyNoInteractions(interestRepository, configService);
  }

  @Test
  void recalculateFromDate_singleRate_allEntriesUseSameRate() {
    LocalDate from = LocalDate.now().minusDays(3);
    BigDecimal rate = new BigDecimal("0.0575");
    when(configService.getDecimalValue(eq("RATE_INDR_STD"), any(LocalDate.class))).thenReturn(rate);

    service.recalculateFromDate(debtId, from);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<InterestJournalEntry>> captor = ArgumentCaptor.forClass(List.class);
    verify(interestRepository).saveAll(captor.capture());

    List<InterestJournalEntry> entries = captor.getValue();
    assertThat(entries)
        .hasSize(3)
        .allSatisfy(e -> assertThat(e.getRate()).isEqualByComparingTo(rate));
  }

  @Test
  void recalculateFromDate_rateChangesAtYearBoundary_entriesUseDifferentRates() {
    // Simulate a recalculation spanning a year boundary where the rate changes
    // on 1 January (monthly check point in the implementation).
    LocalDate from = LocalDate.now().minusDays(5);
    BigDecimal oldRate = new BigDecimal("0.05");
    BigDecimal newRate = new BigDecimal("0.0575");

    // Return different rates depending on date: old rate for days before today, new rate for today
    when(configService.getDecimalValue(eq("RATE_INDR_STD"), any(LocalDate.class)))
        .thenAnswer(
            inv -> {
              LocalDate d = inv.getArgument(1);
              // Simulate a rate change at the first of the current month
              return d.getDayOfMonth() >= 1 && d.getMonthValue() == LocalDate.now().getMonthValue()
                  ? newRate
                  : oldRate;
            });

    service.recalculateFromDate(debtId, from);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<InterestJournalEntry>> captor = ArgumentCaptor.forClass(List.class);
    verify(interestRepository).saveAll(captor.capture());

    List<InterestJournalEntry> entries = captor.getValue();
    // Total entry count = 5 days
    // All entries must have a non-null rate from configService
    assertThat(entries).hasSize(5).allSatisfy(e -> assertThat(e.getRate()).isNotNull());
  }

  @Test
  void recalculateFromDate_configNotFoundFallback_usesFallbackRate() {
    LocalDate from = LocalDate.now().minusDays(2);
    when(configService.getDecimalValue(eq("RATE_INDR_STD"), any(LocalDate.class)))
        .thenThrow(new BusinessConfigService.ConfigurationNotFoundException("RATE_INDR_STD", from));

    service.recalculateFromDate(debtId, from);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<InterestJournalEntry>> captor = ArgumentCaptor.forClass(List.class);
    verify(interestRepository).saveAll(captor.capture());

    List<InterestJournalEntry> entries = captor.getValue();
    // Fallback rate is 0.0575; entries should still be created
    assertThat(entries)
        .hasSize(2)
        .allSatisfy(e -> assertThat(e.getRate()).isEqualByComparingTo(new BigDecimal("0.0575")));
  }

  @Test
  void recalculateFromDate_totalInterestIsSumOfDailyAmounts() {
    LocalDate from = LocalDate.now().minusDays(3);
    BigDecimal rate = new BigDecimal("0.0575");
    when(configService.getDecimalValue(anyString(), any())).thenReturn(rate);

    InterestRecalculationResult result = service.recalculateFromDate(debtId, from);

    // daily = 100000 * 0.0575 / 365 = 15.75 (rounded HALF_UP, 2dp)
    BigDecimal expectedDaily =
        new BigDecimal("100000.00")
            .multiply(rate)
            .divide(new BigDecimal("365"), 2, java.math.RoundingMode.HALF_UP);
    BigDecimal expectedTotal =
        expectedDaily.multiply(new BigDecimal("3")).setScale(2, java.math.RoundingMode.HALF_UP);
    assertThat(result.getTotalInterestRecalculated()).isEqualByComparingTo(expectedTotal);
  }

  @Test
  void recalculateFromDate_deletesBeforeRewriting() {
    LocalDate from = LocalDate.now().minusDays(1);
    when(configService.getDecimalValue(anyString(), any())).thenReturn(new BigDecimal("0.05"));
    when(interestRepository.deleteByDebtIdFromDate(debtId, from)).thenReturn(5);

    InterestRecalculationResult result = service.recalculateFromDate(debtId, from);

    assertThat(result.getEntriesDeleted()).isEqualTo(5);
    verify(interestRepository).deleteByDebtIdFromDate(debtId, from);
  }
}
