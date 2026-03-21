package dk.ufst.opendebt.debtservice.batch;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity;
import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity.BatchStatus;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.HoeringEntity;
import dk.ufst.opendebt.debtservice.entity.HoeringStatus;
import dk.ufst.opendebt.debtservice.repository.BatchJobExecutionRepository;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.HoeringRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadlineMonitoringJob {

  static final String JOB_NAME = "DEADLINE_MONITORING";

  private final DebtRepository debtRepository;
  private final HoeringRepository hoeringRepository;
  private final BatchJobExecutionRepository batchRepository;

  @Value("${opendebt.batch.limitation-warning-days:90}")
  private int limitationWarningDays;

  @Scheduled(cron = "${opendebt.batch.deadline-monitoring.cron:0 0 3 * * *}")
  public void run() {
    execute(LocalDate.now());
  }

  public BatchJobExecutionEntity execute(LocalDate checkDate) {
    if (batchRepository.existsByJobNameAndExecutionDate(JOB_NAME, checkDate)) {
      log.info("Deadline monitoring already executed for {}, skipping", checkDate);
      return null;
    }

    BatchJobExecutionEntity execution =
        BatchJobExecutionEntity.builder()
            .jobName(JOB_NAME)
            .executionDate(checkDate)
            .startedAt(Instant.now())
            .status(BatchStatus.RUNNING)
            .build();
    execution = batchRepository.save(execution);

    int flagged = 0;

    LocalDate warningDate = checkDate.plusDays(limitationWarningDays);
    List<DebtEntity> approachingLimitation = debtRepository.findApproachingLimitation(warningDate);
    for (DebtEntity debt : approachingLimitation) {
      log.warn(
          "Approaching limitation: debt={}, limitationDate={}, daysRemaining={}",
          debt.getId(),
          debt.getLimitationDate(),
          java.time.temporal.ChronoUnit.DAYS.between(checkDate, debt.getLimitationDate()));
      flagged++;
    }

    List<HoeringEntity> expiredHoerings =
        hoeringRepository.findByHoeringStatusAndSlaDeadlineBefore(
            HoeringStatus.AFVENTER_FORDRINGSHAVER, checkDate.atStartOfDay());
    for (HoeringEntity hoering : expiredHoerings) {
      log.warn(
          "Expired SLA: hoering={}, debt={}, slaDeadline={}",
          hoering.getId(),
          hoering.getDebtId(),
          hoering.getSlaDeadline());
      flagged++;
    }

    execution.setRecordsProcessed(flagged);
    execution.setRecordsFailed(0);
    execution.setCompletedAt(Instant.now());
    execution.setStatus(BatchStatus.COMPLETED);
    batchRepository.save(execution);

    log.info("Deadline monitoring completed: date={}, flagged={}", checkDate, flagged);

    return execution;
  }
}
