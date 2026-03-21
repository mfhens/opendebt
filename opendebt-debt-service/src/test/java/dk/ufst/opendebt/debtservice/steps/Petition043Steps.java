package dk.ufst.opendebt.debtservice.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import dk.ufst.opendebt.debtservice.batch.DeadlineMonitoringJob;
import dk.ufst.opendebt.debtservice.batch.InterestAccrualJob;
import dk.ufst.opendebt.debtservice.batch.RestanceTransitionJob;
import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity;
import dk.ufst.opendebt.debtservice.entity.BusinessConfigEntity;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.DebtTypeEntity;
import dk.ufst.opendebt.debtservice.entity.InterestJournalEntry;
import dk.ufst.opendebt.debtservice.repository.BatchJobExecutionRepository;
import dk.ufst.opendebt.debtservice.repository.BusinessConfigRepository;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.DebtTypeRepository;
import dk.ufst.opendebt.debtservice.repository.InterestJournalEntryRepository;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class Petition043Steps {

  @Autowired private RestanceTransitionJob restanceTransitionJob;
  @Autowired private InterestAccrualJob interestAccrualJob;
  @Autowired private DeadlineMonitoringJob deadlineMonitoringJob;
  @Autowired private DebtRepository debtRepository;
  @Autowired private DebtTypeRepository debtTypeRepository;
  @Autowired private InterestJournalEntryRepository interestRepository;
  @Autowired private BatchJobExecutionRepository batchRepository;
  @Autowired private BusinessConfigRepository configRepository;

  private final List<UUID> createdDebtIds = new ArrayList<>();
  private BatchJobExecutionEntity lastExecution;
  private final LocalDate executionDate = LocalDate.of(2026, 3, 19);

  @Before
  public void setUp() {
    createdDebtIds.clear();
    lastExecution = null;
    interestRepository.deleteAll();
    batchRepository.deleteAll();
    debtRepository.deleteAll();

    // Seed debt type "600" with interest applicable for BDD tests
    if (debtTypeRepository.findByCode("600").isEmpty()) {
      debtTypeRepository.save(
          DebtTypeEntity.builder()
              .code("600")
              .name("Test debt type")
              .interestApplicable(true)
              .active(true)
              .build());
    }

    // Seed business config rate for test date range
    if (configRepository.findEffective("RATE_INDR_STD", executionDate).isEmpty()) {
      configRepository.save(
          BusinessConfigEntity.builder()
              .configKey("RATE_INDR_STD")
              .configValue("0.0575")
              .valueType("DECIMAL")
              .validFrom(LocalDate.of(2020, 1, 1))
              .createdBy("test")
              .build());
    }
  }

  @Given("{int} claims in state REGISTERED with expired payment deadline and positive balance")
  public void claimsRegisteredExpiredPositiveBalance(int count) {
    for (int i = 0; i < count; i++) {
      DebtEntity debt = createDebt(ClaimLifecycleState.REGISTERED, new BigDecimal("10000"));
      debt.setPaymentDeadline(LocalDate.of(2026, 3, 1));
      debt.setLastPaymentDate(LocalDate.of(2026, 3, 1));
      debtRepository.save(debt);
      createdDebtIds.add(debt.getId());
    }
  }

  @Given("a claim in state REGISTERED with expired payment deadline and zero balance")
  public void claimRegisteredExpiredZeroBalance() {
    DebtEntity debt = createDebt(ClaimLifecycleState.REGISTERED, BigDecimal.ZERO);
    debt.setPaymentDeadline(LocalDate.of(2026, 3, 1));
    debt.setLastPaymentDate(LocalDate.of(2026, 3, 1));
    debtRepository.save(debt);
    createdDebtIds.add(debt.getId());
  }

  @Given("a claim in state OVERDRAGET with outstanding balance {}")
  public void claimOverdraget(BigDecimal balance) {
    DebtEntity debt = createDebt(ClaimLifecycleState.OVERDRAGET, balance);
    debt.setReceivedAt(LocalDateTime.of(2026, 2, 15, 10, 0));
    debtRepository.save(debt);
    createdDebtIds.add(debt.getId());
  }

  @Given("a claim with limitation date within {int} days")
  public void claimWithLimitationDate(int days) {
    DebtEntity debt = createDebt(ClaimLifecycleState.OVERDRAGET, new BigDecimal("5000"));
    debt.setLimitationDate(executionDate.plusDays(days - 10));
    debtRepository.save(debt);
    createdDebtIds.add(debt.getId());
  }

  @Given("{int} claims eligible for RESTANCE transition")
  public void claimsEligibleForRestance(int count) {
    claimsRegisteredExpiredPositiveBalance(count);
  }

  @When("the daily RESTANCE transition batch job runs")
  public void runRestanceTransition() {
    lastExecution = restanceTransitionJob.execute(executionDate);
  }

  @And("the daily RESTANCE transition batch job runs again for the same date")
  public void runRestanceTransitionAgain() {
    lastExecution = restanceTransitionJob.execute(executionDate);
  }

  @When("the daily interest accrual batch job runs")
  public void runInterestAccrual() {
    lastExecution = interestAccrualJob.execute(executionDate);
  }

  @And("the daily interest accrual batch job runs again for the same date")
  public void runInterestAccrualAgain() {
    lastExecution = interestAccrualJob.execute(executionDate);
  }

  @When("the daily deadline monitoring batch job runs")
  public void runDeadlineMonitoring() {
    lastExecution = deadlineMonitoringJob.execute(executionDate);
  }

  @Then("{int} claims are transitioned to RESTANCE")
  public void claimsTransitioned(int count) {
    long restanceCount =
        createdDebtIds.stream()
            .map(id -> debtRepository.findById(id).orElseThrow())
            .filter(d -> d.getLifecycleState() == ClaimLifecycleState.RESTANCE)
            .count();
    assertThat(restanceCount).isEqualTo(count);
  }

  @Then("the claim remains in state REGISTERED")
  public void claimRemainsRegistered() {
    DebtEntity debt = debtRepository.findById(createdDebtIds.get(0)).orElseThrow();
    assertThat(debt.getLifecycleState()).isEqualTo(ClaimLifecycleState.REGISTERED);
  }

  @Then("the batch skips on the second run")
  public void batchSkipsSecondRun() {
    assertThat(lastExecution).isNull();
  }

  @Then("an interest journal entry is created for the claim with amount {}")
  public void interestEntryCreated(BigDecimal expectedAmount) {
    UUID debtId = createdDebtIds.get(0);
    List<InterestJournalEntry> entries =
        interestRepository.findAll().stream().filter(e -> e.getDebtId().equals(debtId)).toList();
    assertThat(entries).hasSize(1);
    assertThat(entries.get(0).getInterestAmount()).isEqualByComparingTo(expectedAmount);
  }

  @Then("the claim has exactly {int} interest journal entry")
  public void claimHasExactlyNEntries(int count) {
    UUID debtId = createdDebtIds.get(0);
    long entryCount =
        interestRepository.findAll().stream().filter(e -> e.getDebtId().equals(debtId)).count();
    assertThat(entryCount).isEqualTo(count);
  }

  @Then("the batch flags {int} approaching deadline")
  public void batchFlagsDeadlines(int count) {
    assertThat(lastExecution).isNotNull();
    assertThat(lastExecution.getRecordsProcessed()).isEqualTo(count);
  }

  @Then("a batch execution record is created with {int} records processed")
  public void batchExecutionRecord(int processed) {
    assertThat(lastExecution).isNotNull();
    assertThat(lastExecution.getRecordsProcessed()).isEqualTo(processed);
    assertThat(lastExecution.getCompletedAt()).isNotNull();
    assertThat(lastExecution.getStartedAt()).isNotNull();
  }

  private DebtEntity createDebt(ClaimLifecycleState state, BigDecimal balance) {
    return DebtEntity.builder()
        .debtorPersonId(UUID.randomUUID())
        .creditorOrgId(UUID.randomUUID())
        .debtTypeCode("600")
        .principalAmount(new BigDecimal("10000"))
        .outstandingBalance(balance)
        .dueDate(LocalDate.of(2025, 12, 1))
        .status(DebtEntity.DebtStatus.ACTIVE)
        .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
        .lifecycleState(state)
        .build();
  }
}
