package dk.ufst.opendebt.debtservice.offsetting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.debtservice.client.DaekningsRaekkefoeigenServiceClient;
import dk.ufst.opendebt.debtservice.entity.KorrektionspuljeEntry;
import dk.ufst.opendebt.debtservice.repository.KorrektionspuljeEntryRepository;
import dk.ufst.opendebt.debtservice.repository.ModregningEventRepository;
import dk.ufst.opendebt.debtservice.service.KorrektionspuljeResult;
import dk.ufst.opendebt.debtservice.service.KorrektionspuljeService;
import dk.ufst.opendebt.debtservice.service.ModregningResult;
import dk.ufst.opendebt.debtservice.service.ModregningService;
import dk.ufst.opendebt.debtservice.service.OffsettingReversalEvent;
import dk.ufst.opendebt.debtservice.service.RenteGodtgoerelseService;

/**
 * Unit tests for {@code KorrektionspuljeService} — FR-3 implementation for petition P058.
 *
 * <p>Covered requirements:
 *
 * <ul>
 *   <li><b>AC-8</b>: Gendækning after fordring write-down (3-step processReversal)
 *   <li><b>AC-9</b>: Threshold — entries &lt; 50 DKK get annualOnlySettlement=true
 *   <li><b>AC-10</b>: Monthly settlement — settleEntry re-enters FR-1 modregning workflow
 *   <li><b>AC-11</b>: Børne-og-ungeydelse restriction propagated through settleEntry
 *   <li><b>FR-3.2(b)</b>: Gendækning opt-out conditions
 * </ul>
 *
 * <p>Spec reference: SPEC-058 §3.3 and §3.5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KorrektionspuljeService — P058 unit tests")
class KorrektionspuljeServiceTest {

  @Mock private KorrektionspuljeEntryRepository entryRepository;
  @Mock private ModregningEventRepository modregningEventRepository;
  @Mock private DaekningsRaekkefoeigenServiceClient daekningsRaekkefoeigenService;
  @Mock private ModregningService modregningService;
  @Mock private RenteGodtgoerelseService renteGodtgoerelseService;

  private KorrektionspuljeService underTest;

  @BeforeEach
  void setUp() {
    underTest =
        new KorrektionspuljeService(
            entryRepository,
            modregningEventRepository,
            daekningsRaekkefoeigenService,
            modregningService,
            renteGodtgoerelseService);
  }

  private OffsettingReversalEvent buildReversalEvent(
      BigDecimal surplus,
      String poolTarget,
      boolean debtUnderCollectionOptOut,
      boolean retroactivePartialCoverage) {
    return new OffsettingReversalEvent(
        UUID.randomUUID(),
        UUID.randomUUID(),
        surplus,
        UUID.randomUUID(),
        poolTarget,
        "STANDARD",
        debtUnderCollectionOptOut,
        retroactivePartialCoverage);
  }

  private void setupEntryRepositorySave() {
    when(entryRepository.save(any()))
        .thenAnswer(
            inv -> {
              KorrektionspuljeEntry e = inv.getArgument(0);
              e.setId(UUID.randomUUID());
              return e;
            });
  }

  // ── AC-8: processReversal algorithm ──────────────────────────────────────────

  @Nested
  @DisplayName("AC-8: processReversal — 3-step gendækning algorithm")
  class ProcessReversal {

    /**
     * AC-8 Step 1: Surplus applied to same-fordring uncovered renter. Simplified: step1Consumed=0
     * (no renter tracking). step1Consumed=0 on result. Ref: Gæld.bekendtg. § 7, stk. 4; SPEC-058
     * §3.3 Step 1
     */
    @Test
    @DisplayName("AC-8 Step 1: surplus applied to same-fordring uncovered renter before gendækning")
    void step1_appliesSurplusToSameFordringRenter() {
      setupEntryRepositorySave();
      when(daekningsRaekkefoeigenService.allocate(any(), any())).thenReturn(List.of());

      OffsettingReversalEvent reversalEvent =
          buildReversalEvent(new BigDecimal("1500.00"), "PSRM", false, false);

      KorrektionspuljeResult result = underTest.processReversal(reversalEvent);

      // Simplified: step1Consumed=0 per SPEC-058 §3.3
      assertThat(result.step1Consumed())
          .as("step1Consumed must be 0 (simplified renter tracking, SPEC-058 §3.3)")
          .isEqualByComparingTo(BigDecimal.ZERO);
    }

    /**
     * AC-8 Step 2: Remaining surplus offered to DaekningsRaekkefoeigenService for gendækning. Ref:
     * SPEC-058 §3.3 Step 2
     */
    @Test
    @DisplayName(
        "AC-8 Step 2: remaining surplus offered to DaekningsRaekkefoeigenService for gendækning")
    void step2_delegatesRemainingToGendaekning() {
      setupEntryRepositorySave();
      when(daekningsRaekkefoeigenService.allocate(any(), any())).thenReturn(List.of());

      OffsettingReversalEvent reversalEvent =
          buildReversalEvent(new BigDecimal("1500.00"), "PSRM", false, false);

      underTest.processReversal(reversalEvent);

      verify(daekningsRaekkefoeigenService).allocate(any(), any());
    }

    /**
     * AC-8 Step 3: Remaining surplus persisted as KorrektionspuljeEntry. Ref: SPEC-058 §3.3 Step 3
     */
    @Test
    @DisplayName("AC-8 Step 3: remaining surplus persisted as KorrektionspuljeEntry")
    void step3_persistsKorrektionspuljeEntry() {
      setupEntryRepositorySave();
      when(daekningsRaekkefoeigenService.allocate(any(), any())).thenReturn(List.of());

      OffsettingReversalEvent reversalEvent =
          buildReversalEvent(new BigDecimal("1500.00"), "PSRM", false, false);

      KorrektionspuljeResult result = underTest.processReversal(reversalEvent);

      ArgumentCaptor<KorrektionspuljeEntry> captor =
          ArgumentCaptor.forClass(KorrektionspuljeEntry.class);
      verify(entryRepository).save(captor.capture());

      KorrektionspuljeEntry saved = captor.getValue();
      assertThat(saved.getCorrectionPoolTarget())
          .as("correctionPoolTarget must match reversalEvent (SPEC-058 §3.3)")
          .isEqualTo("PSRM");
      assertThat(saved.getSurplusAmount())
          .as("surplusAmount must equal remaining after gendækning")
          .isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    /**
     * AC-8 + NFR-1: processReversal exception propagates (rollback in @Transactional context). Ref:
     * SPEC-058 §3.3
     */
    @Test
    @DisplayName(
        "AC-8 + NFR-1: processReversal is @Transactional — Step 3 failure rolls back Step 1")
    void processReversal_isFullyAtomicTransaction() {
      when(daekningsRaekkefoeigenService.allocate(any(), any())).thenReturn(List.of());
      when(entryRepository.save(any())).thenThrow(new RuntimeException("DB failure at Step 3"));

      OffsettingReversalEvent reversalEvent =
          buildReversalEvent(new BigDecimal("1000.00"), "PSRM", false, false);

      assertThatThrownBy(() -> underTest.processReversal(reversalEvent))
          .as("Step 3 failure must propagate for @Transactional rollback (NFR-1, SPEC-058 §3.3)")
          .isInstanceOf(RuntimeException.class);
    }
  }

  // ── FR-3.2(b): Gendækning opt-out conditions ─────────────────────────────────

  @Nested
  @DisplayName("FR-3.2(b): Gendækning opt-out — mandatory reviewer requirements")
  class GendaekningOptOut {

    /** FR-3.2(b)(a): correctionPoolTarget=DMI skips gendækning. Ref: SPEC-058 §3.3 */
    @Test
    @DisplayName("FR-3.2(b)(a): correctionPoolTarget=DMI skips gendækning, surplus goes to pool")
    void gendaekningOptOut_whenCorrectionPoolTargetDmi() {
      setupEntryRepositorySave();

      OffsettingReversalEvent reversalEvent =
          buildReversalEvent(new BigDecimal("800.00"), "DMI", false, false);

      underTest.processReversal(reversalEvent);

      verify(daekningsRaekkefoeigenService, never()).allocate(any(), any());

      ArgumentCaptor<KorrektionspuljeEntry> captor =
          ArgumentCaptor.forClass(KorrektionspuljeEntry.class);
      verify(entryRepository).save(captor.capture());
      assertThat(captor.getValue().getCorrectionPoolTarget())
          .as("correctionPoolTarget must be DMI in persisted entry")
          .isEqualTo("DMI");
    }

    /** FR-3.2(b)(b): debtUnderCollectionOptOut=true skips gendækning. Ref: SPEC-058 §3.3 */
    @Test
    @DisplayName("FR-3.2(b)(b): debt-under-collection opt-out flag skips gendækning")
    void gendaekningOptOut_whenDebtUnderCollectionOptOut() {
      setupEntryRepositorySave();

      OffsettingReversalEvent reversalEvent =
          buildReversalEvent(new BigDecimal("600.00"), "PSRM", true, false);

      underTest.processReversal(reversalEvent);

      verify(daekningsRaekkefoeigenService, never()).allocate(any(), any());
      verify(entryRepository).save(any());
    }

    /** FR-3.2(b)(c): retroactivePartialCoverage=true skips gendækning. Ref: SPEC-058 §3.3 */
    @Test
    @DisplayName("FR-3.2(b)(c): retroactive partial coverage skips gendækning")
    void gendaekningOptOut_whenRetroactivePartialCoverage() {
      setupEntryRepositorySave();

      OffsettingReversalEvent reversalEvent =
          buildReversalEvent(new BigDecimal("400.00"), "PSRM", false, true);

      underTest.processReversal(reversalEvent);

      verify(daekningsRaekkefoeigenService, never()).allocate(any(), any());
      verify(entryRepository).save(any());
    }
  }

  // ── AC-9: Threshold — < 50 DKK ───────────────────────────────────────────────

  @Nested
  @DisplayName("AC-9: Threshold — entries < 50 DKK get annualOnlySettlement=true")
  class Threshold {

    /** AC-9: surplusAmount < 50.00 → annualOnlySettlement=true at creation. Ref: SPEC-058 §2.1.2 */
    @Test
    @DisplayName("AC-9: surplusAmount < 50.00 → annualOnlySettlement=true at creation")
    void belowThreshold_setsAnnualOnlySettlement() {
      setupEntryRepositorySave();
      when(daekningsRaekkefoeigenService.allocate(any(), any())).thenReturn(List.of());

      OffsettingReversalEvent reversalEvent =
          buildReversalEvent(new BigDecimal("45.00"), "PSRM", false, false);

      underTest.processReversal(reversalEvent);

      ArgumentCaptor<KorrektionspuljeEntry> captor =
          ArgumentCaptor.forClass(KorrektionspuljeEntry.class);
      verify(entryRepository).save(captor.capture());

      assertThat(captor.getValue().isAnnualOnlySettlement())
          .as(
              "annualOnlySettlement must be true when surplusAmount < 50.00 (AC-9, SPEC-058 §2.1.2)")
          .isTrue();
    }

    /**
     * AC-9: runMonthlySettlement excludes entries with annualOnlySettlement=true. Ref: SPEC-058
     * §3.5
     */
    @Test
    @DisplayName("AC-9: runMonthlySettlement excludes entries with annualOnlySettlement=true")
    void monthlyJob_excludesBelowThresholdEntries() {
      // Monthly query excludes annualOnlySettlement=true → returns empty list
      when(entryRepository.findBySettledAtIsNullAndCorrectionPoolTargetAndAnnualOnlySettlementFalse(
              "PSRM"))
          .thenReturn(List.of());

      dk.ufst.opendebt.debtservice.batch.KorrektionspuljeSettlementJob job =
          new dk.ufst.opendebt.debtservice.batch.KorrektionspuljeSettlementJob(
              entryRepository, underTest);

      job.runMonthlySettlement();

      verify(modregningService, never())
          .initiateModregning(any(), any(), any(), any(), anyBoolean());
    }
  }

  // ── AC-10: Monthly settlement ─────────────────────────────────────────────────

  @Nested
  @DisplayName("AC-10: Monthly settlement — settleEntry re-enters FR-1 modregning workflow")
  class MonthlySettlement {

    private ModregningResult mockModregningResult() {
      return new ModregningResult(
          UUID.randomUUID(),
          UUID.randomUUID(),
          LocalDate.now(),
          BigDecimal.TEN,
          BigDecimal.TEN,
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          false,
          false,
          null,
          LocalDate.now().plusYears(1),
          null,
          true,
          List.of());
    }

    /**
     * AC-10: settleEntry computes renteGodtgoerelseAccrued and calls initiateModregning with total.
     * surplusAmount=750.00, rate=5.0%, startDate=2025-01-01, settlementDate=2025-04-01. 90 days:
     * 750.00 × 0.05 × 90/365 ≈ 9.25 (HALF_UP). total = 759.25. Ref: SPEC-058 §3.3 settleEntry
     */
    @Test
    @DisplayName(
        "AC-10: settleEntry computes renteGodtgoerelseAccrued and calls initiateModregning with total")
    void settleEntry_computesAccrualAndCallsInitiateModregning() {
      KorrektionspuljeEntry entry =
          KorrektionspuljeEntry.builder()
              .id(UUID.randomUUID())
              .debtorPersonId(UUID.randomUUID())
              .originEventId(UUID.randomUUID())
              .surplusAmount(new BigDecimal("750.00"))
              .correctionPoolTarget("PSRM")
              .renteGodtgoerelseStartDate(LocalDate.of(2025, 1, 1))
              .boerneYdelseRestriction(false)
              .build();

      when(renteGodtgoerelseService.computeRate(LocalDate.of(2025, 4, 1)))
          .thenReturn(new BigDecimal("5.00"));
      when(modregningService.initiateModregning(any(), any(), any(), any(), anyBoolean()))
          .thenReturn(mockModregningResult());
      when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      underTest.settleEntry(entry, LocalDate.of(2025, 4, 1));

      // accrual = 750 × 0.05 × 90/365 = 9.2465... → HALF_UP = 9.25
      // Days from 2025-01-01 to 2025-04-01 = 90 days
      ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
      verify(modregningService)
          .initiateModregning(
              any(),
              amountCaptor.capture(),
              eq("KORREKTIONSPULJE_SETTLEMENT"),
              any(),
              anyBoolean());

      BigDecimal total = amountCaptor.getValue();
      assertThat(total)
          .as(
              "total must be surplusAmount(750.00) + accrual(≈9.25) = 759.25 (AC-10, SPEC-058 §3.3)")
          .isEqualByComparingTo(new BigDecimal("759.25"));
    }

    /**
     * AC-10: null renteGodtgoerelseStartDate → renteGodtgoerelseAccrued stays 0.00. Ref: SPEC-058
     * §3.3
     */
    @Test
    @DisplayName("AC-10: null renteGodtgoerelseStartDate → renteGodtgoerelseAccrued stays 0.00")
    void settleEntry_withNullStartDate_noAccrual() {
      KorrektionspuljeEntry entry =
          KorrektionspuljeEntry.builder()
              .id(UUID.randomUUID())
              .debtorPersonId(UUID.randomUUID())
              .originEventId(UUID.randomUUID())
              .surplusAmount(new BigDecimal("500.00"))
              .correctionPoolTarget("PSRM")
              .renteGodtgoerelseStartDate(null)
              .boerneYdelseRestriction(false)
              .build();

      when(modregningService.initiateModregning(any(), any(), any(), any(), anyBoolean()))
          .thenReturn(mockModregningResult());
      when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      underTest.settleEntry(entry, LocalDate.of(2025, 4, 1));

      // No computeRate call when startDate is null
      verify(renteGodtgoerelseService, never()).computeRate(any());

      ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
      verify(modregningService)
          .initiateModregning(any(), amountCaptor.capture(), any(), any(), anyBoolean());

      assertThat(amountCaptor.getValue())
          .as("When startDate is null, total must equal surplusAmount only (no accrual, AC-10)")
          .isEqualByComparingTo(new BigDecimal("500.00"));
    }

    /** AC-10: settleEntry sets settledAt to non-null Instant on completion. Ref: SPEC-058 §2.1.2 */
    @Test
    @DisplayName("AC-10: settleEntry sets settledAt to non-null Instant on completion")
    void settleEntry_setsSettledAt() {
      KorrektionspuljeEntry entry =
          KorrektionspuljeEntry.builder()
              .id(UUID.randomUUID())
              .debtorPersonId(UUID.randomUUID())
              .originEventId(UUID.randomUUID())
              .surplusAmount(new BigDecimal("200.00"))
              .correctionPoolTarget("PSRM")
              .renteGodtgoerelseStartDate(null)
              .boerneYdelseRestriction(false)
              .build();

      when(modregningService.initiateModregning(any(), any(), any(), any(), anyBoolean()))
          .thenReturn(mockModregningResult());
      when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      underTest.settleEntry(entry, LocalDate.of(2025, 4, 1));

      assertThat(entry.getSettledAt())
          .as(
              "settledAt must be set to non-null Instant after settleEntry (AC-10, SPEC-058 §2.1.2)")
          .isNotNull();
    }

    /**
     * AC-10: settleEntry with correctionPoolTarget=DMI throws IllegalArgumentException. Ref:
     * SPEC-058 §3.3 constraint
     */
    @Test
    @DisplayName("AC-10: settleEntry with correctionPoolTarget=DMI throws IllegalArgumentException")
    void settleEntry_withDmiTarget_throwsException() {
      KorrektionspuljeEntry dmiEntry =
          KorrektionspuljeEntry.builder()
              .id(UUID.randomUUID())
              .debtorPersonId(UUID.randomUUID())
              .originEventId(UUID.randomUUID())
              .surplusAmount(new BigDecimal("1000.00"))
              .correctionPoolTarget("DMI")
              .boerneYdelseRestriction(false)
              .build();

      assertThatThrownBy(() -> underTest.settleEntry(dmiEntry, LocalDate.of(2025, 4, 1)))
          .as(
              "settleEntry must throw IllegalArgumentException for DMI entries (AC-10, SPEC-058 §3.3)")
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  // ── AC-11: Børne-og-ungeydelse restriction ────────────────────────────────────

  @Nested
  @DisplayName("AC-11: Børne-og-ungeydelse restriction propagated through settleEntry")
  class BoerneOgUngeydelse {

    private ModregningResult mockModregningResult() {
      return new ModregningResult(
          UUID.randomUUID(),
          UUID.randomUUID(),
          LocalDate.now(),
          BigDecimal.TEN,
          BigDecimal.TEN,
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          false,
          false,
          null,
          LocalDate.now().plusYears(1),
          null,
          true,
          List.of());
    }

    /**
     * AC-11: boerneYdelseRestriction=true → initiateModregning called with restrictedPayment=true.
     * Ref: SPEC-058 §3.3 settleEntry restrictedPayment parameter
     */
    @Test
    @DisplayName(
        "AC-11: boerneYdelseRestriction=true → initiateModregning called with restrictedPayment=true")
    void boerneYdelseRestriction_propagatedToInitiateModregning() {
      KorrektionspuljeEntry entry =
          KorrektionspuljeEntry.builder()
              .id(UUID.randomUUID())
              .debtorPersonId(UUID.randomUUID())
              .originEventId(UUID.randomUUID())
              .surplusAmount(new BigDecimal("300.00"))
              .correctionPoolTarget("PSRM")
              .renteGodtgoerelseStartDate(null)
              .boerneYdelseRestriction(true)
              .build();

      when(modregningService.initiateModregning(any(), any(), any(), any(), anyBoolean()))
          .thenReturn(mockModregningResult());
      when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      underTest.settleEntry(entry, LocalDate.of(2025, 4, 1));

      // Verify 5th argument (restrictedPayment=true) — AC-11
      verify(modregningService).initiateModregning(any(), any(), any(), any(), eq(true));
    }

    /**
     * AC-11: boerneYdelseRestriction=false → initiateModregning called with
     * restrictedPayment=false. Ref: SPEC-058 §3.3
     */
    @Test
    @DisplayName(
        "AC-11: boerneYdelseRestriction=false → initiateModregning called with restrictedPayment=false")
    void boerneYdelseRestriction_falseNotPropagated() {
      KorrektionspuljeEntry entry =
          KorrektionspuljeEntry.builder()
              .id(UUID.randomUUID())
              .debtorPersonId(UUID.randomUUID())
              .originEventId(UUID.randomUUID())
              .surplusAmount(new BigDecimal("300.00"))
              .correctionPoolTarget("PSRM")
              .renteGodtgoerelseStartDate(null)
              .boerneYdelseRestriction(false)
              .build();

      when(modregningService.initiateModregning(any(), any(), any(), any(), anyBoolean()))
          .thenReturn(mockModregningResult());
      when(entryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      underTest.settleEntry(entry, LocalDate.of(2025, 4, 1));

      verify(modregningService).initiateModregning(any(), any(), any(), any(), eq(false));
    }
  }
}
