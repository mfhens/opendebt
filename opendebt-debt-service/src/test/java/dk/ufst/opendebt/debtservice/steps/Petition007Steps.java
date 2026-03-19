package dk.ufst.opendebt.debtservice.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import dk.ufst.opendebt.debtservice.dto.CollectionMeasureDto;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.CollectionMeasureEntity.MeasureType;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.CollectionMeasureService;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class Petition007Steps {

  @Autowired private CollectionMeasureService collectionMeasureService;
  @Autowired private DebtRepository debtRepository;

  private UUID debtId;
  private CollectionMeasureDto lastMeasure;
  private Exception lastException;

  @Before
  public void setUp() {
    lastMeasure = null;
    lastException = null;
  }

  private DebtEntity createDebt(ClaimLifecycleState state) {
    DebtEntity debt =
        DebtEntity.builder()
            .debtorPersonId(UUID.randomUUID())
            .creditorOrgId(UUID.randomUUID())
            .debtTypeCode("600")
            .principalAmount(new BigDecimal("50000"))
            .outstandingBalance(new BigDecimal("50000"))
            .dueDate(LocalDate.now().minusMonths(3))
            .status(DebtEntity.DebtStatus.IN_COLLECTION)
            .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
            .lifecycleState(state)
            .receivedAt(LocalDateTime.now().minusDays(30))
            .build();
    return debtRepository.save(debt);
  }

  @Given("a debt in OVERDRAGET state")
  public void aDebtInOverdragetState() {
    DebtEntity debt = createDebt(ClaimLifecycleState.OVERDRAGET);
    debtId = debt.getId();
  }

  @Given("a debt in REGISTERED state")
  public void aDebtInRegisteredState() {
    DebtEntity debt = createDebt(ClaimLifecycleState.REGISTERED);
    debtId = debt.getId();
  }

  @When("a SET_OFF measure is initiated with amount {int}")
  public void aSetOffMeasureIsInitiated(int amount) {
    lastMeasure =
        collectionMeasureService.initiateMeasure(
            debtId, MeasureType.SET_OFF, new BigDecimal(amount), null);
  }

  @When("a WAGE_GARNISHMENT measure is initiated")
  public void aWageGarnishmentMeasureIsInitiated() {
    lastMeasure =
        collectionMeasureService.initiateMeasure(debtId, MeasureType.WAGE_GARNISHMENT, null, null);
  }

  @When("an ATTACHMENT measure is initiated")
  public void anAttachmentMeasureIsInitiated() {
    lastMeasure =
        collectionMeasureService.initiateMeasure(debtId, MeasureType.ATTACHMENT, null, null);
  }

  @When("a SET_OFF measure is attempted")
  public void aSetOffMeasureIsAttempted() {
    try {
      collectionMeasureService.initiateMeasure(
          debtId, MeasureType.SET_OFF, new BigDecimal("5000"), null);
    } catch (Exception e) {
      lastException = e;
    }
  }

  @Then("a collection measure record is created with type {string}")
  public void aCollectionMeasureRecordIsCreated(String type) {
    assertThat(lastMeasure).isNotNull();
    assertThat(lastMeasure.getMeasureType()).isEqualTo(type);
  }

  @And("the measure status is {string}")
  public void theMeasureStatusIs(String status) {
    assertThat(lastMeasure.getStatus()).isEqualTo(status);
  }

  @Then("the initiation is rejected with {string}")
  public void theInitiationIsRejectedWith(String message) {
    assertThat(lastException).isNotNull();
    assertThat(lastException.getMessage()).contains(message);
  }

  @Given("an existing INITIATED collection measure")
  public void anExistingInitiatedMeasure() {
    aDebtInOverdragetState();
    lastMeasure =
        collectionMeasureService.initiateMeasure(
            debtId, MeasureType.SET_OFF, new BigDecimal("5000"), null);
  }

  @Given("an existing COMPLETED collection measure")
  public void anExistingCompletedMeasure() {
    anExistingInitiatedMeasure();
    lastMeasure = collectionMeasureService.completeMeasure(lastMeasure.getId());
  }

  @When("the measure is completed")
  public void theMeasureIsCompleted() {
    lastMeasure = collectionMeasureService.completeMeasure(lastMeasure.getId());
  }

  @And("a completion timestamp is recorded")
  public void aCompletionTimestampIsRecorded() {
    assertThat(lastMeasure.getCompletedAt()).isNotNull();
  }

  @When("the measure is cancelled with reason {string}")
  public void theMeasureIsCancelled(String reason) {
    lastMeasure = collectionMeasureService.cancelMeasure(lastMeasure.getId(), reason);
  }

  @When("cancellation is attempted")
  public void cancellationIsAttempted() {
    try {
      collectionMeasureService.cancelMeasure(lastMeasure.getId(), "should fail");
    } catch (Exception e) {
      lastException = e;
    }
  }

  @Then("the system rejects the cancellation")
  public void theSystemRejectsCancellation() {
    assertThat(lastException).isNotNull();
  }
}
