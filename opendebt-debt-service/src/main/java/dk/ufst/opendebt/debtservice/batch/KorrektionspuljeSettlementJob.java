package dk.ufst.opendebt.debtservice.batch;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.ufst.opendebt.debtservice.entity.KorrektionspuljeEntry;
import dk.ufst.opendebt.debtservice.repository.KorrektionspuljeEntryRepository;
import dk.ufst.opendebt.debtservice.service.KorrektionspuljeService;

/**
 * Scheduled batch job for monthly and annual korrektionspulje settlement.
 *
 * <p>Monthly: settles PSRM entries with annualOnlySettlement=false. Annual: settles all PSRM
 * entries including annualOnlySettlement=true.
 */
@Component
public class KorrektionspuljeSettlementJob {

  private static final Logger log = LoggerFactory.getLogger(KorrektionspuljeSettlementJob.class);

  private final KorrektionspuljeEntryRepository entryRepository;
  private final KorrektionspuljeService korrektionspuljeService;

  public KorrektionspuljeSettlementJob(
      KorrektionspuljeEntryRepository entryRepository,
      KorrektionspuljeService korrektionspuljeService) {
    this.entryRepository = entryRepository;
    this.korrektionspuljeService = korrektionspuljeService;
  }

  /** Runs monthly — settles PSRM entries with annualOnlySettlement=false. AC-9. */
  @Scheduled(cron = "0 0 3 1 * ?")
  public void runMonthlySettlement() {
    LocalDate today = LocalDate.now();
    List<KorrektionspuljeEntry> entries =
        entryRepository.findBySettledAtIsNullAndCorrectionPoolTargetAndAnnualOnlySettlementFalse(
            "PSRM");
    log.info("Monthly settlement: processing {} PSRM entries", entries.size());
    for (KorrektionspuljeEntry entry : entries) {
      try {
        korrektionspuljeService.settleEntry(entry, today);
      } catch (Exception e) {
        log.error("Failed to settle entry {}: {}", entry.getId(), e.getMessage(), e);
      }
    }
  }

  /** Runs annually — settles ALL PSRM entries including annualOnlySettlement=true. */
  @Scheduled(cron = "0 0 4 2 1 ?")
  public void runAnnualSettlement() {
    LocalDate today = LocalDate.now();
    List<KorrektionspuljeEntry> entries =
        entryRepository.findBySettledAtIsNullAndCorrectionPoolTarget("PSRM");
    log.info("Annual settlement: processing {} PSRM entries", entries.size());
    for (KorrektionspuljeEntry entry : entries) {
      try {
        korrektionspuljeService.settleEntry(entry, today);
      } catch (Exception e) {
        log.error("Failed to settle entry {}: {}", entry.getId(), e.getMessage(), e);
      }
    }
  }
}
