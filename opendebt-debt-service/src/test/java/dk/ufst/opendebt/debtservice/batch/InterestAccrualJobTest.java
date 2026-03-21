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
import dk.ufst.opendebt.debtservice.repository.DebtRepository;

@ExtendWith(MockitoExtension.class)
class InterestAccrualJobTest {

  @Mock private DebtRepository debtRepository;
  @Mock private InterestAccrualJobHelper helper;

  private InterestAccrualJob job;

  private static final BigDecimal ANNUAL_RATE = new BigDecimal("0.0575");

  @BeforeEach
  void setUp() {
    job = new InterestAccrualJob(debtRepository, helper);
    ReflectionTestUtils.setField(job, "pageSize", 1000);
    ReflectionTestUtils.setField(job, "annualRate", ANNUAL_RATE);
  }

  @Test
  void execute_accruesInterestOnOverdragetDebts() {
    LocalDate accrualDate = LocalDate.of(2026, 3, 19);
    BatchJobExecutionEntity execution = executionEntity(BatchStatus.RUNNING);
    BatchJobExecutionEntity completed = executionEntity(BatchStatus.COMPLETED);
    completed.setRecordsProcessed(1);

    when(helper.alreadyExecuted(InterestAccrualJob.JOB_NAME, accrualDate)).thenReturn(false);
    when(helper.createExecution(InterestAccrualJob.JOB_NAME, accrualDate)).thenReturn(execution);
    when(helper.processPage(any(), eq(accrualDate), eq(ANNUAL_RATE))).thenReturn(new int[] {1, 0});
    when(helper.finalizeExecution(execution, 1, 0)).thenReturn(completed);

    DebtEntity debt = testDebt(ClaimLifecycleState.OVERDRAGET, new BigDecimal("100000.00"));
    when(debtRepository.findByLifecycleStateAndPositiveBalance(
            eq(ClaimLifecycleState.OVERDRAGET), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(debt)));

    BatchJobExecutionEntity result = job.execute(accrualDate);

    assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(result.getRecordsProcessed()).isEqualTo(1);
    verify(helper).processPage(List.of(debt), accrualDate, ANNUAL_RATE);
  }

  @Test
  void execute_idempotent_skipsAlreadyExecuted() {
    LocalDate accrualDate = LocalDate.of(2026, 3, 19);
    when(helper.alreadyExecuted(InterestAccrualJob.JOB_NAME, accrualDate)).thenReturn(true);

    BatchJobExecutionEntity result = job.execute(accrualDate);

    assertThat(result).isNull();
    verify(helper, never()).createExecution(any(), any());
    verify(debtRepository, never()).findByLifecycleStateAndPositiveBalance(any(), any());
  }

  @Test
  void execute_noEligibleDebts_completesWithZero() {
    LocalDate accrualDate = LocalDate.of(2026, 3, 19);
    BatchJobExecutionEntity execution = executionEntity(BatchStatus.RUNNING);
    BatchJobExecutionEntity completed = executionEntity(BatchStatus.COMPLETED);

    when(helper.alreadyExecuted(InterestAccrualJob.JOB_NAME, accrualDate)).thenReturn(false);
    when(helper.createExecution(InterestAccrualJob.JOB_NAME, accrualDate)).thenReturn(execution);
    when(helper.processPage(any(), eq(accrualDate), eq(ANNUAL_RATE))).thenReturn(new int[] {0, 0});
    when(helper.finalizeExecution(execution, 0, 0)).thenReturn(completed);
    when(debtRepository.findByLifecycleStateAndPositiveBalance(
            eq(ClaimLifecycleState.OVERDRAGET), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    BatchJobExecutionEntity result = job.execute(accrualDate);

    assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    verify(helper).processPage(List.of(), accrualDate, ANNUAL_RATE);
  }

  private BatchJobExecutionEntity executionEntity(BatchStatus status) {
    return BatchJobExecutionEntity.builder()
        .id(UUID.randomUUID())
        .jobName(InterestAccrualJob.JOB_NAME)
        .executionDate(LocalDate.of(2026, 3, 19))
        .status(status)
        .build();
  }

  private DebtEntity testDebt(ClaimLifecycleState state, BigDecimal balance) {
    return DebtEntity.builder()
        .id(UUID.randomUUID())
        .debtorPersonId(UUID.randomUUID())
        .creditorOrgId(UUID.randomUUID())
        .debtTypeCode("600")
        .principalAmount(balance)
        .outstandingBalance(balance)
        .dueDate(LocalDate.now().minusMonths(3))
        .status(DebtEntity.DebtStatus.IN_COLLECTION)
        .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
        .lifecycleState(state)
        .build();
  }
}
