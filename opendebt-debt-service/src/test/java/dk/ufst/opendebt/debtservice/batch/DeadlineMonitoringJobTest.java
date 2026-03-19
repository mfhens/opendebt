package dk.ufst.opendebt.debtservice.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity;
import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity.BatchStatus;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.HoeringEntity;
import dk.ufst.opendebt.debtservice.entity.HoeringStatus;
import dk.ufst.opendebt.debtservice.repository.BatchJobExecutionRepository;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.HoeringRepository;

@ExtendWith(MockitoExtension.class)
class DeadlineMonitoringJobTest {

  @Mock private DebtRepository debtRepository;
  @Mock private HoeringRepository hoeringRepository;
  @Mock private BatchJobExecutionRepository batchRepository;

  private DeadlineMonitoringJob job;

  @BeforeEach
  void setUp() {
    job = new DeadlineMonitoringJob(debtRepository, hoeringRepository, batchRepository);
    ReflectionTestUtils.setField(job, "limitationWarningDays", 90);
  }

  @Test
  void execute_flagsApproachingLimitationAndExpiredHoering() {
    LocalDate today = LocalDate.now();
    when(batchRepository.existsByJobNameAndExecutionDate(DeadlineMonitoringJob.JOB_NAME, today))
        .thenReturn(false);
    when(batchRepository.save(any(BatchJobExecutionEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity d1 = testDebtWithLimitation(today.plusDays(60));
    DebtEntity d2 = testDebtWithLimitation(today.plusDays(89));
    when(debtRepository.findApproachingLimitation(today.plusDays(90))).thenReturn(List.of(d1, d2));

    HoeringEntity h1 = testHoering(today.minusDays(3));
    when(hoeringRepository.findByHoeringStatusAndSlaDeadlineBefore(
            eq(HoeringStatus.AFVENTER_FORDRINGSHAVER), eq(today.atStartOfDay())))
        .thenReturn(List.of(h1));

    BatchJobExecutionEntity result = job.execute(today);

    assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(result.getRecordsProcessed()).isEqualTo(3);
    assertThat(result.getRecordsFailed()).isEqualTo(0);
  }

  @Test
  void execute_idempotent_skipsAlreadyExecuted() {
    LocalDate today = LocalDate.now();
    when(batchRepository.existsByJobNameAndExecutionDate(DeadlineMonitoringJob.JOB_NAME, today))
        .thenReturn(true);

    BatchJobExecutionEntity result = job.execute(today);

    assertThat(result).isNull();
    verify(debtRepository, never()).findApproachingLimitation(any());
  }

  @Test
  void execute_noIssuesFound_completesWithZero() {
    LocalDate today = LocalDate.now();
    when(batchRepository.existsByJobNameAndExecutionDate(DeadlineMonitoringJob.JOB_NAME, today))
        .thenReturn(false);
    when(batchRepository.save(any(BatchJobExecutionEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(debtRepository.findApproachingLimitation(today.plusDays(90))).thenReturn(List.of());
    when(hoeringRepository.findByHoeringStatusAndSlaDeadlineBefore(
            eq(HoeringStatus.AFVENTER_FORDRINGSHAVER), eq(today.atStartOfDay())))
        .thenReturn(List.of());

    BatchJobExecutionEntity result = job.execute(today);

    assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(result.getRecordsProcessed()).isEqualTo(0);
  }

  @Test
  void execute_onlyLimitationFlags() {
    LocalDate today = LocalDate.now();
    when(batchRepository.existsByJobNameAndExecutionDate(DeadlineMonitoringJob.JOB_NAME, today))
        .thenReturn(false);
    when(batchRepository.save(any(BatchJobExecutionEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity d1 = testDebtWithLimitation(today.plusDays(30));
    when(debtRepository.findApproachingLimitation(today.plusDays(90))).thenReturn(List.of(d1));
    when(hoeringRepository.findByHoeringStatusAndSlaDeadlineBefore(
            eq(HoeringStatus.AFVENTER_FORDRINGSHAVER), eq(today.atStartOfDay())))
        .thenReturn(List.of());

    BatchJobExecutionEntity result = job.execute(today);

    assertThat(result.getRecordsProcessed()).isEqualTo(1);
  }

  private DebtEntity testDebtWithLimitation(LocalDate limitationDate) {
    return DebtEntity.builder()
        .id(UUID.randomUUID())
        .debtorPersonId(UUID.randomUUID())
        .creditorOrgId(UUID.randomUUID())
        .debtTypeCode("600")
        .principalAmount(new BigDecimal("10000"))
        .outstandingBalance(new BigDecimal("10000"))
        .dueDate(LocalDate.now().minusYears(2))
        .limitationDate(limitationDate)
        .status(DebtEntity.DebtStatus.IN_COLLECTION)
        .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
        .build();
  }

  private HoeringEntity testHoering(LocalDate slaDate) {
    return HoeringEntity.builder()
        .id(UUID.randomUUID())
        .debtId(UUID.randomUUID())
        .hoeringStatus(HoeringStatus.AFVENTER_FORDRINGSHAVER)
        .deviationDescription("Test deviation")
        .slaDeadline(slaDate.atStartOfDay())
        .build();
  }
}
