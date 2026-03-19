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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity;
import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity.BatchStatus;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.BatchJobExecutionRepository;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.ClaimLifecycleService;

@ExtendWith(MockitoExtension.class)
class RestanceTransitionJobTest {

  @Mock private DebtRepository debtRepository;
  @Mock private ClaimLifecycleService claimLifecycleService;
  @Mock private BatchJobExecutionRepository batchRepository;

  private RestanceTransitionJob job;

  @BeforeEach
  void setUp() {
    job = new RestanceTransitionJob(debtRepository, claimLifecycleService, batchRepository);
    ReflectionTestUtils.setField(job, "pageSize", 1000);
  }

  @Test
  void execute_transitionsEligibleDebts() {
    LocalDate today = LocalDate.now();
    when(batchRepository.existsByJobNameAndExecutionDate(RestanceTransitionJob.JOB_NAME, today))
        .thenReturn(false);
    when(batchRepository.save(any(BatchJobExecutionEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity d1 = testDebt(ClaimLifecycleState.REGISTERED);
    DebtEntity d2 = testDebt(ClaimLifecycleState.REGISTERED);
    when(debtRepository.findEligibleForRestanceTransition(
            eq(ClaimLifecycleState.REGISTERED), eq(today), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(d1, d2)));
    when(claimLifecycleService.evaluateClaimState(any(UUID.class), eq(today))).thenReturn(d1);

    BatchJobExecutionEntity result = job.execute(today);

    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(result.getRecordsProcessed()).isEqualTo(2);
    assertThat(result.getRecordsFailed()).isEqualTo(0);
    verify(claimLifecycleService, times(2)).evaluateClaimState(any(UUID.class), eq(today));
  }

  @Test
  void execute_idempotent_skipsAlreadyExecuted() {
    LocalDate today = LocalDate.now();
    when(batchRepository.existsByJobNameAndExecutionDate(RestanceTransitionJob.JOB_NAME, today))
        .thenReturn(true);

    BatchJobExecutionEntity result = job.execute(today);

    assertThat(result).isNull();
    verify(claimLifecycleService, never()).evaluateClaimState(any(), any());
  }

  @Test
  void execute_recordsFailuresWithoutStopping() {
    LocalDate today = LocalDate.now();
    when(batchRepository.existsByJobNameAndExecutionDate(RestanceTransitionJob.JOB_NAME, today))
        .thenReturn(false);
    when(batchRepository.save(any(BatchJobExecutionEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity d1 = testDebt(ClaimLifecycleState.REGISTERED);
    DebtEntity d2 = testDebt(ClaimLifecycleState.REGISTERED);
    when(debtRepository.findEligibleForRestanceTransition(
            eq(ClaimLifecycleState.REGISTERED), eq(today), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(d1, d2)));
    when(claimLifecycleService.evaluateClaimState(eq(d1.getId()), eq(today)))
        .thenThrow(new RuntimeException("DB error"));
    when(claimLifecycleService.evaluateClaimState(eq(d2.getId()), eq(today))).thenReturn(d2);

    BatchJobExecutionEntity result = job.execute(today);

    assertThat(result.getStatus()).isEqualTo(BatchStatus.FAILED);
    assertThat(result.getRecordsProcessed()).isEqualTo(1);
    assertThat(result.getRecordsFailed()).isEqualTo(1);
  }

  @Test
  void execute_noEligibleDebts_completesWithZero() {
    LocalDate today = LocalDate.now();
    when(batchRepository.existsByJobNameAndExecutionDate(RestanceTransitionJob.JOB_NAME, today))
        .thenReturn(false);
    when(batchRepository.save(any(BatchJobExecutionEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(debtRepository.findEligibleForRestanceTransition(
            eq(ClaimLifecycleState.REGISTERED), eq(today), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    BatchJobExecutionEntity result = job.execute(today);

    assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(result.getRecordsProcessed()).isEqualTo(0);
  }

  private DebtEntity testDebt(ClaimLifecycleState state) {
    return DebtEntity.builder()
        .id(UUID.randomUUID())
        .debtorPersonId(UUID.randomUUID())
        .creditorOrgId(UUID.randomUUID())
        .debtTypeCode("600")
        .principalAmount(new BigDecimal("10000"))
        .outstandingBalance(new BigDecimal("10000"))
        .dueDate(LocalDate.now().minusMonths(1))
        .paymentDeadline(LocalDate.now().minusDays(5))
        .status(DebtEntity.DebtStatus.ACTIVE)
        .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
        .lifecycleState(state)
        .build();
  }
}
