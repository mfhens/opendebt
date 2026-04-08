package dk.ufst.opendebt.debtservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.debtservice.client.DaekningsRaekkefoeigenServiceClient;
import dk.ufst.opendebt.debtservice.entity.KorrektionspuljeEntry;
import dk.ufst.opendebt.debtservice.repository.KorrektionspuljeEntryRepository;
import dk.ufst.opendebt.debtservice.repository.ModregningEventRepository;

/** Service implementing the FR-3 Korrektionspulje workflow per SPEC-058 §3.3 and §3.5. */
@Service
@Transactional
public class KorrektionspuljeService {

  private static final BigDecimal ANNUAL_ONLY_THRESHOLD = new BigDecimal("50.00");

  private final KorrektionspuljeEntryRepository entryRepository;
  private final ModregningEventRepository modregningEventRepository;
  private final DaekningsRaekkefoeigenServiceClient daekningsRaekkefoeigenServiceClient;
  private final ModregningService modregningService;
  private final RenteGodtgoerelseService renteGodtgoerelseService;
  private final FordringQueryPort fordringQueryPort;

  public KorrektionspuljeService(
      KorrektionspuljeEntryRepository entryRepository,
      ModregningEventRepository modregningEventRepository,
      DaekningsRaekkefoeigenServiceClient daekningsRaekkefoeigenServiceClient,
      ModregningService modregningService,
      RenteGodtgoerelseService renteGodtgoerelseService,
      FordringQueryPort fordringQueryPort) {
    this.entryRepository = entryRepository;
    this.modregningEventRepository = modregningEventRepository;
    this.daekningsRaekkefoeigenServiceClient = daekningsRaekkefoeigenServiceClient;
    this.modregningService = modregningService;
    this.renteGodtgoerelseService = renteGodtgoerelseService;
    this.fordringQueryPort = fordringQueryPort;
  }

  /**
   * Processes an offsetting reversal event via the 3-step gendækning algorithm (SPEC-058 §3.3).
   *
   * <p>Step 1: Apply surplus to same-fordring uncovered renter via {@link FordringQueryPort}
   * (Gæld.bekendtg. § 7, stk. 4). Step 2: If not opted out, delegate remaining to
   * DaekningsRaekkefoeigenService. Step 3: Persist any remaining surplus as KorrektionspuljeEntry.
   *
   * @return KorrektionspuljeResult with consumed amounts and pool entry details
   */
  @Transactional
  public KorrektionspuljeResult processReversal(OffsettingReversalEvent reversalEvent) {
    BigDecimal surplus = reversalEvent.surplusAmount();

    // Step 1: Apply to same-fordring uncovered renter (Gæld.bekendtg. § 7, stk. 4; SPEC-058 §3.3)
    // Query the outstanding balance of the reversed fordring to cover its uncovered renter first.
    BigDecimal uncoveredRenter =
        fordringQueryPort.getOutstandingAmount(reversalEvent.reversedFordringId());
    BigDecimal step1Consumed = surplus.min(uncoveredRenter).max(BigDecimal.ZERO);
    BigDecimal remaining = surplus.subtract(step1Consumed);

    // Step 2: Gendækning via DaekningsRaekkefoeigenService (unless opted out)
    BigDecimal gendaekketAmount = BigDecimal.ZERO;
    boolean isDmi = "DMI".equals(reversalEvent.correctionPoolTarget());
    boolean optOut =
        isDmi
            || reversalEvent.debtUnderCollectionOptOut()
            || reversalEvent.retroactivePartialCoverage();

    if (!optOut) {
      List<FordringAllocation> gendaekningAllocations =
          daekningsRaekkefoeigenServiceClient.allocate(reversalEvent.debtorPersonId(), remaining);
      gendaekketAmount =
          gendaekningAllocations.stream()
              .map(FordringAllocation::amountCovered)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      remaining = remaining.subtract(gendaekketAmount);
    }

    // BUG-3: Look up origin ModregningEvent to get renteGodtgoerelseStartDate
    LocalDate renteGodtgoerelseStartDate = null;
    if (reversalEvent.originModregningEventId() != null) {
      renteGodtgoerelseStartDate =
          modregningEventRepository
              .findById(reversalEvent.originModregningEventId())
              .map(originEvent -> originEvent.getDecisionDate().plusDays(1))
              .orElse(null);
    }

    // OTHER: Set boerneYdelseRestriction from originalPaymentType
    boolean boerneYdelseRestriction =
        PaymentType.BOERNE_OG_UNGEYDELSE.name().equals(reversalEvent.originalPaymentType());

    // Step 3: Persist remaining as KorrektionspuljeEntry
    boolean annualOnly = remaining.compareTo(ANNUAL_ONLY_THRESHOLD) < 0;
    KorrektionspuljeEntry entry =
        KorrektionspuljeEntry.builder()
            .debtorPersonId(reversalEvent.debtorPersonId())
            .originEventId(reversalEvent.originModregningEventId())
            .surplusAmount(remaining)
            .correctionPoolTarget(reversalEvent.correctionPoolTarget())
            .annualOnlySettlement(annualOnly)
            .renteGodtgoerelseStartDate(renteGodtgoerelseStartDate)
            .boerneYdelseRestriction(boerneYdelseRestriction)
            .build();
    entry = entryRepository.save(entry);

    return new KorrektionspuljeResult(step1Consumed, gendaekketAmount, entry.getId(), remaining);
  }

  /**
   * Settles a KorrektionspuljeEntry by computing accrued rentegodtgørelse and re-entering the
   * modregning workflow (SPEC-058 §3.3 settleEntry).
   *
   * @throws IllegalArgumentException if correctionPoolTarget = DMI (DMI entries are never settled
   *     here)
   */
  @Transactional
  public void settleEntry(KorrektionspuljeEntry entry, LocalDate settlementDate) {
    if ("DMI".equals(entry.getCorrectionPoolTarget())) {
      throw new IllegalArgumentException(
          "Cannot settle DMI entries via PSRM settlement job. Entry: " + entry.getId());
    }

    // Compute accrual
    BigDecimal accrual = BigDecimal.ZERO;
    if (entry.getRenteGodtgoerelseStartDate() != null) {
      BigDecimal rate = renteGodtgoerelseService.computeRate(settlementDate);
      long days = ChronoUnit.DAYS.between(entry.getRenteGodtgoerelseStartDate(), settlementDate);
      accrual =
          entry
              .getSurplusAmount()
              .multiply(rate.divide(BigDecimal.valueOf(100)))
              .multiply(BigDecimal.valueOf(days))
              .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);
    }
    entry.setRenteGodtgoerelseAccrued(accrual);
    BigDecimal total = entry.getSurplusAmount().add(accrual);

    // Re-enter modregning workflow
    modregningService.initiateModregning(
        entry.getDebtorPersonId(),
        total,
        PaymentType.KORREKTIONSPULJE_SETTLEMENT,
        null,
        entry.isBoerneYdelseRestriction());

    entry.setSettledAt(Instant.now());
    entryRepository.save(entry);
  }
}
