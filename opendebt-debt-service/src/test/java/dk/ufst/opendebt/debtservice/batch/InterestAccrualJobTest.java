package dk.ufst.opendebt.debtservice.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity;
import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity.BatchStatus;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.InterestJournalEntry;
import dk.ufst.opendebt.debtservice.repository.BatchJobExecutionRepository;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.InterestJournalEntryRepository;

@ExtendWith(MockitoExtension.class)
class InterestAccrualJobTest {

  @Mock private DebtRepository debtRepository;
  @Mock private InterestJournalEntryRepository interestRepository;
  @Mock private BatchJobExecutionRepository batchRepository;

  private InterestAccrualJob job;

  private static final BigDecimal ANNUAL_RATE = new BigDecimal("0.0575");

  @BeforeEach
  void setUp() {
    job = new InterestAccrualJob(debtRepository, interestRepository, batchRepository);
    ReflectionTestUtils.setField(job, "pageSize", 1000);
    ReflectionTestUtils.setField(job, "annualRate", ANNUAL_RATE);
  }

  @Test
  void execute_accruesInterestOnOverdragetDebts() {
    LocalDate accrualDate = LocalDate.of(2026, 3, 19);
    when(batchRepository.existsByJobNameAndExecutionDate(InterestAccrualJob.JOB_NAME, accrualDate))
        .thenReturn(false);
    when(batchRepository.save(any(BatchJobExecutionEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity debt = testDebt(ClaimLifecycleState.OVERDRAGET, new BigDecimal("100000.00"));
    when(debtRepository.findByLifecycleStateAndPositiveBalance(
            eq(ClaimLifecycleState.OVERDRAGET), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(debt)));
    when(interestRepository.existsByDebtIdAndAccrualDate(debt.getId(), accrualDate))
        .thenReturn(false);
    when(interestRepository.save(any(InterestJournalEntry.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    BatchJobExecutionEntity result = job.execute(accrualDate);

    assertThat(result.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(result.getRecordsProcessed()).isEqualTo(1);

    ArgumentCaptor<InterestJournalEntry> captor =
        ArgumentCaptor.forClass(InterestJournalEntry.class);
    verify(interestRepository).save(captor.capture());

    InterestJournalEntry entry = captor.getValue();
    assertThat(entry.getDebtId()).isEqualTo(debt.getId());
    assertThat(entry.getAccrualDate()).isEqualTo(accrualDate);
    assertThat(entry.getBalanceSnapshot()).isEqualByComparingTo(new BigDecimal("100000.00"));
    assertThat(entry.getRate()).isEqualByComparingTo(ANNUAL_RATE);

    BigDecimal expectedInterest =
        new BigDecimal("100000.00")
            .multiply(ANNUAL_RATE)
            .divide(new BigDecimal("365"), 2, RoundingMode.HALF_UP);
    assertThat(entry.getInterestAmount()).isEqualByComparingTo(expectedInterest);
  }

  @Test
  void execute_idempotent_skipsAlreadyExecuted() {
    LocalDate accrualDate = LocalDate.of(2026, 3, 19);
    when(batchRepository.existsByJobNameAndExecutionDate(InterestAccrualJob.JOB_NAME, accrualDate))
        .thenReturn(true);

    BatchJobExecutionEntity result = job.execute(accrualDate);

    assertThat(result).isNull();
    verify(interestRepository, never()).save(any());
  }

  @Test
  void execute_skipsDebtWithExistingEntryForDate() {
    LocalDate accrualDate = LocalDate.of(2026, 3, 19);
    when(batchRepository.existsByJobNameAndExecutionDate(InterestAccrualJob.JOB_NAME, accrualDate))
        .thenReturn(false);
    when(batchRepository.save(any(BatchJobExecutionEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity debt = testDebt(ClaimLifecycleState.OVERDRAGET, new BigDecimal("100000.00"));
    when(debtRepository.findByLifecycleStateAndPositiveBalance(
            eq(ClaimLifecycleState.OVERDRAGET), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(debt)));
    when(interestRepository.existsByDebtIdAndAccrualDate(debt.getId(), accrualDate))
        .thenReturn(true);

    BatchJobExecutionEntity result = job.execute(accrualDate);

    assertThat(result.getRecordsProcessed()).isEqualTo(0);
    verify(interestRepository, never()).save(any());
  }

  @Test
  void execute_interestCalculation_accuracy() {
    LocalDate accrualDate = LocalDate.of(2026, 3, 19);
    when(batchRepository.existsByJobNameAndExecutionDate(InterestAccrualJob.JOB_NAME, accrualDate))
        .thenReturn(false);
    when(batchRepository.save(any(BatchJobExecutionEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity debt = testDebt(ClaimLifecycleState.OVERDRAGET, new BigDecimal("100000.00"));
    when(debtRepository.findByLifecycleStateAndPositiveBalance(
            eq(ClaimLifecycleState.OVERDRAGET), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(debt)));
    when(interestRepository.existsByDebtIdAndAccrualDate(debt.getId(), accrualDate))
        .thenReturn(false);
    when(interestRepository.save(any(InterestJournalEntry.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    job.execute(accrualDate);

    ArgumentCaptor<InterestJournalEntry> captor =
        ArgumentCaptor.forClass(InterestJournalEntry.class);
    verify(interestRepository).save(captor.capture());

    // 100000 * 0.0575 / 365 = 15.75 (rounded HALF_UP)
    assertThat(captor.getValue().getInterestAmount()).isEqualByComparingTo(new BigDecimal("15.75"));
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
