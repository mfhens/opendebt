package dk.ufst.opendebt.debtservice.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import dk.ufst.opendebt.debtservice.dto.LiabilityDto;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.LiabilityEntity.LiabilityType;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.LiabilityService;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class Petition005Steps {

  @Autowired private LiabilityService liabilityService;
  @Autowired private DebtRepository debtRepository;

  private UUID debtId;
  private LiabilityDto lastLiability;
  private Exception lastException;
  private final UUID debtor1 = UUID.randomUUID();
  private final UUID debtor2 = UUID.randomUUID();

  @Before
  public void setUp() {
    lastLiability = null;
    lastException = null;
  }

  @Given("a debt exists in the system")
  public void aDebtExistsInTheSystem() {
    DebtEntity debt =
        DebtEntity.builder()
            .debtorPersonId(UUID.randomUUID())
            .creditorOrgId(UUID.randomUUID())
            .debtTypeCode("600")
            .principalAmount(new BigDecimal("10000"))
            .outstandingBalance(new BigDecimal("10000"))
            .dueDate(LocalDate.now().minusMonths(1))
            .status(DebtEntity.DebtStatus.ACTIVE)
            .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
            .build();
    debt = debtRepository.save(debt);
    debtId = debt.getId();
  }

  @When("a SOLE liability is added for one debtor")
  public void aSoleLiabilityIsAdded() {
    lastLiability = liabilityService.addLiability(debtId, debtor1, LiabilityType.SOLE, null);
  }

  @Then("the liability is created with type {string}")
  public void theLiabilityIsCreatedWithType(String type) {
    assertThat(lastLiability.getLiabilityType()).isEqualTo(type);
  }

  @And("the liability is active")
  public void theLiabilityIsActive() {
    assertThat(lastLiability.isActive()).isTrue();
  }

  @When("a JOINT_AND_SEVERAL liability is added for debtor {int}")
  public void aJointLiabilityIsAddedForDebtor(int debtorNum) {
    UUID debtorId = debtorNum == 1 ? debtor1 : debtor2;
    lastLiability =
        liabilityService.addLiability(debtId, debtorId, LiabilityType.JOINT_AND_SEVERAL, null);
  }

  @Then("{int} active liabilities exist for the debt")
  public void activeLiabilitiesExist(int count) {
    List<LiabilityDto> liabilities = liabilityService.getLiabilities(debtId);
    long activeCount = liabilities.stream().filter(LiabilityDto::isActive).count();
    assertThat(activeCount).isEqualTo(count);
  }

  @When("a PROPORTIONAL liability is added for debtor {int} with {int} percent")
  public void aProportionalLiabilityIsAdded(int debtorNum, int percent) {
    UUID debtorId = debtorNum == 1 ? debtor1 : debtor2;
    lastLiability =
        liabilityService.addLiability(
            debtId, debtorId, LiabilityType.PROPORTIONAL, new BigDecimal(percent));
  }

  @And("the shares sum to {int} percent")
  public void theSharesSum(int total) {
    List<LiabilityDto> liabilities = liabilityService.getLiabilities(debtId);
    BigDecimal sum =
        liabilities.stream()
            .map(LiabilityDto::getSharePercentage)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(sum.intValue()).isEqualTo(total);
  }

  @Given("a debt exists with a SOLE liability")
  public void aDebtExistsWithSoleLiability() {
    aDebtExistsInTheSystem();
    lastLiability = liabilityService.addLiability(debtId, debtor1, LiabilityType.SOLE, null);
  }

  @When("a second SOLE liability is attempted")
  public void aSecondSoleLiabilityIsAttempted() {
    try {
      liabilityService.addLiability(debtId, debtor2, LiabilityType.SOLE, null);
    } catch (Exception e) {
      lastException = e;
    }
  }

  @Then("the system rejects with {string}")
  public void theSystemRejectsWith(String message) {
    assertThat(lastException).isNotNull();
    assertThat(lastException.getMessage()).contains(message);
  }

  @Given("a debt exists with a PROPORTIONAL liability at {int} percent")
  public void aDebtExistsWithProportionalLiability(int percent) {
    aDebtExistsInTheSystem();
    liabilityService.addLiability(
        debtId, debtor1, LiabilityType.PROPORTIONAL, new BigDecimal(percent));
  }

  @When("a PROPORTIONAL liability of {int} percent is attempted")
  public void aProportionalLiabilityIsAttempted(int percent) {
    try {
      liabilityService.addLiability(
          debtId, debtor2, LiabilityType.PROPORTIONAL, new BigDecimal(percent));
    } catch (Exception e) {
      lastException = e;
    }
  }

  @Given("a debt exists with a JOINT_AND_SEVERAL liability")
  public void aDebtExistsWithJointLiability() {
    aDebtExistsInTheSystem();
    liabilityService.addLiability(debtId, debtor1, LiabilityType.JOINT_AND_SEVERAL, null);
  }

  @When("a PROPORTIONAL liability is attempted on the same debt")
  public void aProportionalLiabilityIsAttemptedOnSameDebt() {
    try {
      liabilityService.addLiability(
          debtId, debtor2, LiabilityType.PROPORTIONAL, new BigDecimal("50"));
    } catch (Exception e) {
      lastException = e;
    }
  }

  @When("the liability is removed")
  public void theLiabilityIsRemoved() {
    liabilityService.removeLiability(lastLiability.getId());
  }

  @Then("the liability is no longer active")
  public void theLiabilityIsNoLongerActive() {
    List<LiabilityDto> liabilities = liabilityService.getLiabilities(debtId);
    assertThat(liabilities).allMatch(l -> !l.isActive());
  }

  @Given("a debt exists with liabilities for {int} debtors")
  public void aDebtExistsWithLiabilitiesForDebtors(int count) {
    aDebtExistsInTheSystem();
    liabilityService.addLiability(debtId, debtor1, LiabilityType.JOINT_AND_SEVERAL, null);
    if (count > 1) {
      liabilityService.addLiability(debtId, debtor2, LiabilityType.JOINT_AND_SEVERAL, null);
    }
  }

  @When("liabilities are queried for the debt")
  public void liabilitiesAreQueried() {
    // query happens in then step
  }

  @Then("both debtors are returned as liable parties")
  public void bothDebtorsReturned() {
    List<LiabilityDto> liabilities = liabilityService.getLiabilities(debtId);
    assertThat(liabilities).hasSize(2);
  }
}
