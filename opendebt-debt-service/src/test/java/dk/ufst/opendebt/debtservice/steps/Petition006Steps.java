package dk.ufst.opendebt.debtservice.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import dk.ufst.opendebt.debtservice.dto.ObjectionDto;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.ObjectionEntity.ObjectionStatus;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.ObjectionService;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class Petition006Steps {

  @Autowired private ObjectionService objectionService;
  @Autowired private DebtRepository debtRepository;

  private UUID debtId;
  private UUID debtorPersonId;
  private ObjectionDto lastObjection;
  private Exception lastException;

  @Before
  public void setUp() {
    lastObjection = null;
    lastException = null;
  }

  @Given("a debt exists for objection testing")
  public void aDebtExistsForObjectionTesting() {
    debtorPersonId = UUID.randomUUID();
    DebtEntity debt =
        DebtEntity.builder()
            .debtorPersonId(debtorPersonId)
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

  @When("an objection is registered with reason {string}")
  public void anObjectionIsRegistered(String reason) {
    lastObjection = objectionService.registerObjection(debtId, debtorPersonId, reason);
  }

  @Then("the objection status is {string}")
  public void theObjectionStatusIs(String status) {
    assertThat(lastObjection.getStatus()).isEqualTo(status);
  }

  @And("the debt readiness status is {string}")
  public void theDebtReadinessStatusIs(String status) {
    DebtEntity debt = debtRepository.findById(debtId).orElseThrow();
    assertThat(debt.getReadinessStatus().name()).isEqualTo(status);
  }

  @Given("a debt exists with an active objection")
  public void aDebtExistsWithActiveObjection() {
    aDebtExistsForObjectionTesting();
    lastObjection =
        objectionService.registerObjection(debtId, debtorPersonId, "claim amount disputed");
  }

  @When("the system checks for active objections")
  public void theSystemChecksForActiveObjections() {
    // check happens in then step
  }

  @Then("hasActiveObjection returns true")
  public void hasActiveObjectionReturnsTrue() {
    assertThat(objectionService.hasActiveObjection(debtId)).isTrue();
  }

  @When("the objection is resolved as {string} with note {string}")
  public void theObjectionIsResolved(String outcome, String note) {
    ObjectionStatus status = ObjectionStatus.valueOf(outcome);
    lastObjection = objectionService.resolveObjection(lastObjection.getId(), status, note);
  }

  @And("the debt remains under appeal")
  public void theDebtRemainsUnderAppeal() {
    DebtEntity debt = debtRepository.findById(debtId).orElseThrow();
    assertThat(debt.getReadinessStatus()).isEqualTo(DebtEntity.ReadinessStatus.UNDER_APPEAL);
  }

  @When("a second objection is attempted")
  public void aSecondObjectionIsAttempted() {
    try {
      objectionService.registerObjection(debtId, debtorPersonId, "duplicate");
    } catch (Exception e) {
      lastException = e;
    }
  }

  @Then("the objection is rejected with message {string}")
  public void theObjectionIsRejectedWithMessage(String message) {
    assertThat(lastException).isNotNull();
    assertThat(lastException.getMessage()).contains(message);
  }

  @Given("a debt exists with a resolved objection")
  public void aDebtExistsWithResolvedObjection() {
    aDebtExistsWithActiveObjection();
    lastObjection =
        objectionService.resolveObjection(
            lastObjection.getId(), ObjectionStatus.REJECTED, "resolved");
  }

  @When("the objection history is queried")
  public void theObjectionHistoryIsQueried() {
    // query happens in then step
  }

  @Then("the resolved objection appears in the history")
  public void theResolvedObjectionAppearsInHistory() {
    List<ObjectionDto> objections = objectionService.getObjections(debtId);
    assertThat(objections).isNotEmpty();
    assertThat(objections.get(0).getStatus()).isEqualTo("REJECTED");
  }
}
