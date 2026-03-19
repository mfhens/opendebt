package dk.ufst.opendebt.debtservice.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleEvent;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.ClaimLifecycleEventRepository;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.ClaimLifecycleService;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class Petition003Steps {

  @Autowired private ClaimLifecycleService claimLifecycleService;
  @Autowired private DebtRepository debtRepository;
  @Autowired private ClaimLifecycleEventRepository claimLifecycleEventRepository;

  private final Map<String, UUID> claimIds = new HashMap<>();
  private final Map<String, UUID> creditorIds = new HashMap<>();
  private final Map<String, UUID> recipientIds = new HashMap<>();

  private boolean transferRejected;
  private long eventCountBeforeTransfer;

  @Before
  public void setUp() {
    claimLifecycleEventRepository.deleteAll();
    debtRepository.deleteAll();
    claimIds.clear();
    creditorIds.clear();
    recipientIds.clear();
    transferRejected = false;
    eventCountBeforeTransfer = 0;
  }

  // =========================================================================
  // Given
  // =========================================================================

  @Given("a fordring {string} has a betalingsfrist on {int}-{int}-{int}")
  public void a_fordring_has_a_betalingsfrist_on(String alias, int year, int month, int day) {
    LocalDate deadline = LocalDate.of(year, month, day);
    UUID creditorId = UUID.randomUUID();
    DebtEntity debt =
        DebtEntity.builder()
            .debtorPersonId(UUID.randomUUID())
            .creditorOrgId(creditorId)
            .debtTypeCode("600")
            .principalAmount(new BigDecimal("1000"))
            .dueDate(deadline)
            .lastPaymentDate(deadline)
            .outstandingBalance(new BigDecimal("1000"))
            .status(DebtEntity.DebtStatus.ACTIVE)
            .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
            .lifecycleState(ClaimLifecycleState.REGISTERED)
            .build();
    DebtEntity saved = debtRepository.save(debt);
    claimIds.put(alias, saved.getId());
    creditorIds.put(alias, creditorId);
  }

  @And("fordring {string} is not fully paid by {int}-{int}-{int}")
  public void fordring_is_not_fully_paid_by(String alias, int year, int month, int day) {
    // outstanding balance already > 0 from creation
  }

  @And("fordring {string} is fully paid by {int}-{int}-{int}")
  public void fordring_is_fully_paid_by(String alias, int year, int month, int day) {
    UUID debtId = claimIds.get(alias);
    DebtEntity debt = debtRepository.findById(debtId).orElseThrow();
    debt.setOutstandingBalance(BigDecimal.ZERO);
    debtRepository.save(debt);
  }

  @Given("a fordring {string} is not classified as a restance")
  public void a_fordring_is_not_classified_as_a_restance(String alias) {
    UUID creditorId = UUID.randomUUID();
    DebtEntity debt =
        DebtEntity.builder()
            .debtorPersonId(UUID.randomUUID())
            .creditorOrgId(creditorId)
            .debtTypeCode("600")
            .principalAmount(new BigDecimal("1000"))
            .dueDate(LocalDate.of(2026, 6, 1))
            .lastPaymentDate(LocalDate.of(2026, 6, 1))
            .outstandingBalance(new BigDecimal("1000"))
            .status(DebtEntity.DebtStatus.ACTIVE)
            .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
            .lifecycleState(ClaimLifecycleState.REGISTERED)
            .build();
    DebtEntity saved = debtRepository.save(debt);
    claimIds.put(alias, saved.getId());
    creditorIds.put("K1", creditorId);
  }

  @Given("restance {string} belongs to fordringshaver {string}")
  public void restance_belongs_to_fordringshaver(String alias, String creditorAlias) {
    UUID creditorId = UUID.randomUUID();
    creditorIds.put(creditorAlias, creditorId);
    DebtEntity debt =
        DebtEntity.builder()
            .debtorPersonId(UUID.randomUUID())
            .creditorOrgId(creditorId)
            .debtTypeCode("600")
            .principalAmount(new BigDecimal("5000"))
            .dueDate(LocalDate.of(2025, 12, 1))
            .lastPaymentDate(LocalDate.of(2025, 12, 1))
            .outstandingBalance(new BigDecimal("5000"))
            .status(DebtEntity.DebtStatus.ACTIVE)
            .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
            .lifecycleState(ClaimLifecycleState.RESTANCE)
            .build();
    DebtEntity saved = debtRepository.save(debt);
    claimIds.put(alias, saved.getId());
  }

  @And("restance {string} is eligible for transfer to collection")
  public void restance_is_eligible_for_transfer_to_collection(String alias) {
    // Already RESTANCE with expired deadline and outstanding balance > 0
  }

  // =========================================================================
  // When
  // =========================================================================

  @When("OpenDebt evaluates the claim state on {int}-{int}-{int}")
  public void open_debt_evaluates_the_claim_state_on(int year, int month, int day) {
    LocalDate evaluationDate = LocalDate.of(year, month, day);
    for (UUID debtId : claimIds.values()) {
      claimLifecycleService.evaluateClaimState(debtId, evaluationDate);
    }
  }

  @When("fordringshaver {string} attempts overdragelse til inddrivelse for fordring {string}")
  public void fordringshaver_attempts_overdragelse_for_fordring(
      String creditorAlias, String claimAlias) {
    UUID debtId = claimIds.get(claimAlias);
    UUID recipientId = UUID.randomUUID();
    recipientIds.put("default", recipientId);
    eventCountBeforeTransfer = claimLifecycleEventRepository.count();
    try {
      claimLifecycleService.transferForCollection(debtId, recipientId);
      transferRejected = false;
    } catch (OpenDebtException e) {
      transferRejected = true;
    }
  }

  @When(
      "fordringshaver {string} transfers restance {string} to restanceinddrivelsesmyndighed {string}")
  public void fordringshaver_transfers_restance_to_rim(
      String creditorAlias, String claimAlias, String rimName) {
    UUID debtId = claimIds.get(claimAlias);
    UUID recipientId = UUID.randomUUID();
    recipientIds.put(rimName, recipientId);
    eventCountBeforeTransfer = claimLifecycleEventRepository.count();
    claimLifecycleService.transferForCollection(debtId, recipientId);
    transferRejected = false;
  }

  // =========================================================================
  // Then
  // =========================================================================

  @Then("fordring {string} is classified as a restance")
  public void fordring_is_classified_as_a_restance(String alias) {
    UUID debtId = claimIds.get(alias);
    DebtEntity debt = debtRepository.findById(debtId).orElseThrow();
    assertThat(debt.getLifecycleState()).isEqualTo(ClaimLifecycleState.RESTANCE);
  }

  @Then("fordring {string} is not classified as a restance")
  public void fordring_is_not_classified_as_a_restance(String alias) {
    UUID debtId = claimIds.get(alias);
    DebtEntity debt = debtRepository.findById(debtId).orElseThrow();
    assertThat(debt.getLifecycleState()).isNotEqualTo(ClaimLifecycleState.RESTANCE);
  }

  @Then("the transfer is rejected")
  public void the_transfer_is_rejected() {
    assertThat(transferRejected).isTrue();
  }

  @And("no transfer record is created")
  public void no_transfer_record_is_created() {
    long eventCountAfter = claimLifecycleEventRepository.count();
    assertThat(eventCountAfter).isEqualTo(eventCountBeforeTransfer);
  }

  @Then("a transfer record is created for restance {string}")
  public void a_transfer_record_is_created_for_restance(String alias) {
    UUID debtId = claimIds.get(alias);
    List<ClaimLifecycleEvent> events =
        claimLifecycleEventRepository.findByDebtIdOrderByOccurredAtDesc(debtId);
    assertThat(events).isNotEmpty();
    ClaimLifecycleEvent latest = events.get(0);
    assertThat(latest.getNewState()).isEqualTo("OVERDRAGET");
  }

  @And("the transfer record identifies fordringshaver {string}")
  public void the_transfer_record_identifies_fordringshaver(String creditorAlias) {
    UUID creditorId = creditorIds.get(creditorAlias);
    boolean found =
        claimLifecycleEventRepository.findAll().stream()
            .filter(e -> "OVERDRAGET".equals(e.getNewState()))
            .anyMatch(e -> creditorId.equals(e.getCreditorId()));
    assertThat(found).isTrue();
  }

  @And("the transfer record identifies restanceinddrivelsesmyndighed {string}")
  public void the_transfer_record_identifies_rim(String rimName) {
    UUID recipientId = recipientIds.get(rimName);
    boolean found =
        claimLifecycleEventRepository.findAll().stream()
            .filter(e -> "OVERDRAGET".equals(e.getNewState()))
            .anyMatch(e -> recipientId.equals(e.getRecipientId()));
    assertThat(found).isTrue();
  }

  @And("the transfer record contains the transfer timestamp")
  public void the_transfer_record_contains_the_transfer_timestamp() {
    boolean found =
        claimLifecycleEventRepository.findAll().stream()
            .filter(e -> "OVERDRAGET".equals(e.getNewState()))
            .anyMatch(e -> e.getOccurredAt() != null);
    assertThat(found).isTrue();
  }

  @And("restance {string} becomes eligible for further collection handling")
  public void restance_becomes_eligible_for_further_collection_handling(String alias) {
    UUID debtId = claimIds.get(alias);
    DebtEntity debt = debtRepository.findById(debtId).orElseThrow();
    assertThat(debt.getLifecycleState()).isEqualTo(ClaimLifecycleState.OVERDRAGET);
    assertThat(debt.getReceivedAt()).isNotNull();
  }
}
