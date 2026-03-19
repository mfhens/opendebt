package dk.ufst.opendebt.debtservice.batch;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity;
import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity.BatchStatus;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.BatchJobExecutionRepository;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.ClaimLifecycleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestanceTransitionJob {

  static final String JOB_NAME = "RESTANCE_TRANSITION";

  private final DebtRepository debtRepository;
  private final ClaimLifecycleService claimLifecycleService;
  private final BatchJobExecutionRepository batchRepository;

  @Value("${opendebt.batch.page-size:1000}")
  private int pageSize;

  @Scheduled(cron = "${opendebt.batch.restance-transition.cron:0 0 2 * * *}")
  public void run() {
    execute(LocalDate.now());
  }

  @Transactional
  public BatchJobExecutionEntity execute(LocalDate evaluationDate) {
    if (batchRepository.existsByJobNameAndExecutionDate(JOB_NAME, evaluationDate)) {
      log.info("RESTANCE transition already executed for {}, skipping", evaluationDate);
      return null;
    }

    BatchJobExecutionEntity execution =
        BatchJobExecutionEntity.builder()
            .jobName(JOB_NAME)
            .executionDate(evaluationDate)
            .startedAt(Instant.now())
            .status(BatchStatus.RUNNING)
            .build();
    execution = batchRepository.save(execution);

    int processed = 0;
    int failed = 0;
    int page = 0;

    Page<DebtEntity> debts;
    do {
      debts =
          debtRepository.findEligibleForRestanceTransition(
              ClaimLifecycleState.REGISTERED, evaluationDate, PageRequest.of(page, pageSize));

      for (DebtEntity debt : debts.getContent()) {
        try {
          claimLifecycleService.evaluateClaimState(debt.getId(), evaluationDate);
          processed++;
        } catch (Exception e) {
          log.warn("Failed to transition debt={}: {}", debt.getId(), e.getMessage());
          failed++;
        }
      }
      page++;
    } while (debts.hasNext());

    execution.setRecordsProcessed(processed);
    execution.setRecordsFailed(failed);
    execution.setCompletedAt(Instant.now());
    execution.setStatus(failed > 0 ? BatchStatus.FAILED : BatchStatus.COMPLETED);
    batchRepository.save(execution);

    log.info(
        "RESTANCE transition completed: date={}, processed={}, failed={}",
        evaluationDate,
        processed,
        failed);

    return execution;
  }
}
