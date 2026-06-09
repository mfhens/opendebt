package dk.ufst.opendebt.debtservice.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.ufst.opendebt.debtservice.dto.CitizenDebtItemDto;
import dk.ufst.opendebt.debtservice.dto.CitizenDebtSummaryResponse;
import dk.ufst.opendebt.debtservice.dto.CitizenEffectiveInterestRateDto;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.InterestSelectionEmbeddable;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.CitizenDebtService;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Petition026 RED BDD coverage for the debt-service enrichment contract.
 *
 * <p>Traceability: P026-DEBT-001, VAL-P026-003, VAL-P026-005, VAL-P026-013, VAL-P026-014,
 * VAL-P026-015.
 */
public class Petition026Steps {

  @Autowired private CitizenDebtService citizenDebtService;
  @Autowired private DebtRepository debtRepository;

  private final Map<String, UUID> personIds = new HashMap<>();
  private final List<UUID> scopedDebtIds = new ArrayList<>();
  private final List<UUID> outOfScopeDebtIds = new ArrayList<>();
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  private UUID currentPersonId;
  private CitizenDebtSummaryResponse lastResponse;
  private JsonNode responseJson;

  @Before
  public void setUpScenario() {
    debtRepository.deleteAll();
    personIds.clear();
    scopedDebtIds.clear();
    outOfScopeDebtIds.clear();
    currentPersonId = null;
    lastResponse = null;
    responseJson = null;
  }

  // P026-DEBT-001 / SA-026 S2 — mandatory creditorDisplayName and citizenStatus enrichment.
  @Given("petition026 citizen {string} has an in-collection debt that needs enrichment")
  public void citizenHasAnInCollectionDebtThatNeedsEnrichment(String citizenAlias) {
    currentPersonId = personId(citizenAlias);
    createDebt(
        currentPersonId,
        DebtEntity.DebtStatus.ACTIVE,
        ClaimLifecycleState.OVERDRAGET,
        new BigDecimal("4500.00"),
        new BigDecimal("4500.00"),
        new BigDecimal("125.00"),
        BigDecimal.ZERO,
        null,
        false,
        null);
  }

  // P026-DEBT-001 / VAL-P026-013 — paused-interest rows need explicit state and reason codes.
  @Given("petition026 citizen {string} has a paused-interest debt with reason code {string}")
  public void citizenHasAPausedInterestDebtWithReasonCode(String citizenAlias, String reasonCode) {
    currentPersonId = personId(citizenAlias);
    createDebt(
        currentPersonId,
        DebtEntity.DebtStatus.ACTIVE,
        ClaimLifecycleState.OVERDRAGET,
        new BigDecimal("5100.00"),
        new BigDecimal("5100.00"),
        new BigDecimal("55.00"),
        BigDecimal.ZERO,
        InterestSelectionEmbeddable.builder().interestRule("INDR_STD").build(),
        true,
        reasonCode);
  }

  // P026-DEBT-001 / VAL-P026-014 — interest-bearing rows need rule metadata and current rate data.
  @Given("petition026 citizen {string} has an interest-bearing debt using interest rule {string}")
  public void citizenHasAnInterestBearingDebtUsingInterestRule(
      String citizenAlias, String interestRuleCode) {
    currentPersonId = personId(citizenAlias);
    createDebt(
        currentPersonId,
        DebtEntity.DebtStatus.ACTIVE,
        ClaimLifecycleState.OVERDRAGET,
        new BigDecimal("7300.00"),
        new BigDecimal("7150.00"),
        new BigDecimal("150.00"),
        BigDecimal.ZERO,
        InterestSelectionEmbeddable.builder().interestRule(interestRuleCode).build(),
        false,
        null);
  }

  // P026-DEBT-001 / VAL-P026-015 — written-off rows need a machine-readable write-off code.
  @Given("petition026 citizen {string} has a written-off debt with reason code {string}")
  public void citizenHasAWrittenOffDebtWithReasonCode(String citizenAlias, String reasonCode) {
    currentPersonId = personId(citizenAlias);
    createDebt(
        currentPersonId,
        DebtEntity.DebtStatus.WRITTEN_OFF,
        ClaimLifecycleState.AFSKREVET,
        new BigDecimal("2400.00"),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        null,
        false,
        reasonCode);
  }

  // P026-DEBT-001 — pagination must preserve truthful page metadata while still returning enriched
  // rows.
  @Given("petition026 citizen {string} has {int} enriched debts")
  public void citizenHasEnrichedDebts(String citizenAlias, int debtCount) {
    currentPersonId = personId(citizenAlias);
    for (int index = 0; index < debtCount; index++) {
      createDebt(
          currentPersonId,
          DebtEntity.DebtStatus.ACTIVE,
          ClaimLifecycleState.OVERDRAGET,
          new BigDecimal("1000.00").add(BigDecimal.valueOf(index)),
          new BigDecimal("1000.00").add(BigDecimal.valueOf(index)),
          BigDecimal.ZERO,
          BigDecimal.ZERO,
          null,
          false,
          null);
    }
  }

  // VAL-P026-003 / NFR-GDPR-002 — service summaries must be scoped to the requested person_id.
  @Given("petition026 citizen {string} has {int} summary debts totaling {string}")
  public void citizenHasSummaryDebtsTotaling(
      String citizenAlias, int debtCount, String totalOutstandingAmount) {
    currentPersonId = personId(citizenAlias);
    scopedDebtIds.clear();

    BigDecimal totalAmount = new BigDecimal(totalOutstandingAmount);
    BigDecimal defaultDebtAmount =
        totalAmount.divide(BigDecimal.valueOf(debtCount), 2, RoundingMode.HALF_UP);
    BigDecimal assignedAmount = BigDecimal.ZERO;

    for (int index = 0; index < debtCount; index++) {
      BigDecimal debtAmount =
          index == debtCount - 1 ? totalAmount.subtract(assignedAmount) : defaultDebtAmount;
      DebtEntity savedDebt =
          createDebt(
              currentPersonId,
              DebtEntity.DebtStatus.ACTIVE,
              ClaimLifecycleState.OVERDRAGET,
              debtAmount,
              debtAmount,
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              null,
              false,
              null);
      scopedDebtIds.add(savedDebt.getId());
      assignedAmount = assignedAmount.add(debtAmount);
    }
  }

  // VAL-P026-003 — out-of-scope debts must never leak into the requested citizen summary.
  @Given("petition026 another citizen {string} has {int} debts outside the summary scope")
  public void anotherCitizenHasDebtsOutsideTheSummaryScope(String citizenAlias, int debtCount) {
    UUID outOfScopePersonId = personId(citizenAlias);
    outOfScopeDebtIds.clear();

    for (int index = 0; index < debtCount; index++) {
      DebtEntity savedDebt =
          createDebt(
              outOfScopePersonId,
              DebtEntity.DebtStatus.ACTIVE,
              ClaimLifecycleState.OVERDRAGET,
              new BigDecimal("900.00").add(BigDecimal.valueOf(index)),
              new BigDecimal("900.00").add(BigDecimal.valueOf(index)),
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              null,
              false,
              null);
      outOfScopeDebtIds.add(savedDebt.getId());
    }
  }

  @When("petition026 debt-service builds the citizen debt summary for page {int} and size {int}")
  public void debtServiceBuildsTheCitizenDebtSummaryForPageAndSize(int pageNumber, int pageSize) {
    lastResponse =
        citizenDebtService.getDebtSummary(
            currentPersonId, null, PageRequest.of(pageNumber, pageSize));
    responseJson = objectMapper.valueToTree(lastResponse);
  }

  @Then("petition026 each returned debt includes creditorDisplayName")
  public void eachReturnedDebtIncludesCreditorDisplayName() {
    JsonNode debtsNode = debtsNode();
    for (JsonNode debtNode : debtsNode) {
      assertThat(debtNode.hasNonNull("creditorDisplayName"))
          .as("P026-DEBT-001 / VAL-P026-005 requires creditorDisplayName on every visible debt row")
          .isTrue();
    }
  }

  @Then("petition026 the first returned debt exposes citizenStatus {string}")
  public void theFirstReturnedDebtExposesCitizenStatus(String expectedCitizenStatus) {
    assertThat(firstDebtNode().path("citizenStatus").asText(null))
        .as("P026-DEBT-001 requires citizenStatus as the primary citizen-facing status field")
        .isEqualTo(expectedCitizenStatus);
  }

  @Then("petition026 the first returned debt omits conditional fields when they do not apply")
  public void theFirstReturnedDebtOmitsConditionalFieldsWhenTheyDoNotApply() {
    JsonNode debtNode = firstDebtNode();
    assertThat(debtNode.has("statusReasonCode"))
        .as("P026-DEBT-001 statusReasonCode must be omitted when no extra explanation is required")
        .isFalse();
    assertThat(debtNode.has("interestPauseReasonCode"))
        .as(
            "P026-DEBT-001 interestPauseReasonCode must be omitted unless interestAccrualState is PAUSED")
        .isFalse();
    assertThat(debtNode.has("currentInterestRate"))
        .as(
            "P026-DEBT-001 currentInterestRate must be omitted when no effective recovery-interest rate applies")
        .isFalse();
    assertThat(debtNode.has("writtenOffReasonCode"))
        .as(
            "P026-DEBT-001 writtenOffReasonCode must be omitted unless citizenStatus is WRITTEN_OFF")
        .isFalse();
  }

  @Then("petition026 the first returned debt exposes interestAccrualState {string}")
  public void theFirstReturnedDebtExposesInterestAccrualState(String expectedAccrualState) {
    assertThat(firstDebtNode().path("interestAccrualState").asText(null))
        .as("P026-DEBT-001 / VAL-P026-013 requires a structured interestAccrualState field")
        .isEqualTo(expectedAccrualState);
  }

  @Then("petition026 the first returned debt exposes interestPauseReasonCode {string}")
  public void theFirstReturnedDebtExposesInterestPauseReasonCode(String expectedReasonCode) {
    assertThat(firstDebtNode().path("interestPauseReasonCode").asText(null))
        .as("P026-DEBT-001 / VAL-P026-013 requires a machine-readable interestPauseReasonCode")
        .isEqualTo(expectedReasonCode);
  }

  @Then("petition026 the first returned debt omits writtenOffReasonCode")
  public void theFirstReturnedDebtOmitsWrittenOffReasonCode() {
    assertThat(firstDebtNode().has("writtenOffReasonCode"))
        .as("P026-DEBT-001 writtenOffReasonCode must be omitted for non-written-off debts")
        .isFalse();
  }

  @Then("petition026 the first returned debt exposes interestRuleCode {string}")
  public void theFirstReturnedDebtExposesInterestRuleCode(String expectedInterestRuleCode) {
    assertThat(firstDebtNode().path("interestRuleCode").asText(null))
        .as("P026-DEBT-001 / VAL-P026-014 requires row-level interestRuleCode metadata")
        .isEqualTo(expectedInterestRuleCode);
  }

  @Then("petition026 the first returned debt exposes currentInterestRate")
  public void theFirstReturnedDebtExposesCurrentInterestRate() {
    assertThat(firstDebtNode().hasNonNull("currentInterestRate"))
        .as("P026-DEBT-001 / VAL-P026-014 requires currentInterestRate for interest-bearing debts")
        .isTrue();
  }

  @Then("petition026 the response exposes effectiveInterestRates metadata for rule {string}")
  public void theResponseExposesEffectiveInterestRatesMetadataForRule(String interestRuleCode) {
    CitizenEffectiveInterestRateDto matchingRate = findEffectiveInterestRate(interestRuleCode);
    assertThat(matchingRate)
        .as(
            "P026-DEBT-001 / VAL-P026-014 requires page-level effectiveInterestRates metadata for %s",
            interestRuleCode)
        .isNotNull();
    assertThat(matchingRate.getAnnualRate())
        .as("P026-DEBT-001 effectiveInterestRates entries must expose annualRate")
        .isNotNull();
    assertThat(matchingRate.getValidFrom())
        .as("P026-DEBT-001 effectiveInterestRates entries must expose validFrom")
        .isNotNull();
  }

  @Then("petition026 the first returned debt exposes writtenOffReasonCode {string}")
  public void theFirstReturnedDebtExposesWrittenOffReasonCode(String expectedReasonCode) {
    assertThat(firstDebtNode().path("writtenOffReasonCode").asText(null))
        .as("P026-DEBT-001 / VAL-P026-015 requires writtenOffReasonCode for WRITTEN_OFF debts")
        .isEqualTo(expectedReasonCode);
  }

  @Then("petition026 the response echoes pageNumber {int} and pageSize {int}")
  public void theResponseEchoesPageNumberAndPageSize(int expectedPageNumber, int expectedPageSize) {
    assertThat(responseJson.path("pageNumber").asInt(-1))
        .as("P026-DEBT-001 pageNumber must echo the applied request value")
        .isEqualTo(expectedPageNumber);
    assertThat(responseJson.path("pageSize").asInt(-1))
        .as("P026-DEBT-001 pageSize must echo the applied request value")
        .isEqualTo(expectedPageSize);
  }

  @Then("petition026 the response reports totalElements {long} and totalPages {int}")
  public void theResponseReportsTotalElementsAndTotalPages(
      long expectedTotalElements, int expectedTotalPages) {
    assertThat(responseJson.path("totalElements").asLong(-1))
        .as("P026-DEBT-001 totalElements must reflect the full dataset size")
        .isEqualTo(expectedTotalElements);
    assertThat(responseJson.path("totalPages").asInt(-1))
        .as("P026-DEBT-001 totalPages must reflect the accessible page count")
        .isEqualTo(expectedTotalPages);
  }

  @Then("petition026 the response contains {int} debts on the current page")
  public void theResponseContainsDebtsOnTheCurrentPage(int expectedDebtCount) {
    assertThat(debtsNode().size())
        .as("P026-DEBT-001 current page must expose the expected number of debt rows")
        .isEqualTo(expectedDebtCount);
  }

  @Then("petition026 the response contains only debts for citizen {string}")
  public void theResponseContainsOnlyDebtsForCitizen(String citizenAlias) {
    assertThat(currentPersonId)
        .as("VAL-P026-003 scenario must execute against the requested person_id")
        .isEqualTo(personId(citizenAlias));
    assertThat(returnedDebtIds())
        .as("VAL-P026-003 requires the response to exclude debts from other citizens")
        .containsExactlyInAnyOrderElementsOf(scopedDebtIds)
        .doesNotContainAnyElementsOf(outOfScopeDebtIds);
  }

  @Then("petition026 the response totalDebtCount is {int}")
  public void theResponseTotalDebtCountIs(int expectedTotalDebtCount) {
    assertThat(lastResponse.getTotalDebtCount())
        .as("P026-DEBT-001 totalDebtCount must reflect only the requested citizen's debts")
        .isEqualTo(expectedTotalDebtCount);
  }

  @Then("petition026 the response totalOutstandingAmount is {string}")
  public void theResponseTotalOutstandingAmountIs(String expectedOutstandingAmount) {
    assertThat(lastResponse.getTotalOutstandingAmount())
        .as("VAL-P026-003 totalOutstandingAmount must exclude debts for other citizens")
        .isEqualByComparingTo(new BigDecimal(expectedOutstandingAmount));
  }

  private JsonNode debtsNode() {
    JsonNode debtsNode = responseJson.path("debts");
    assertThat(debtsNode.isArray())
        .as("P026-DEBT-001 expected a debts array in CitizenDebtSummaryResponse")
        .isTrue();
    return debtsNode;
  }

  private JsonNode firstDebtNode() {
    JsonNode debtsNode = debtsNode();
    assertThat(debtsNode.size())
        .as("P026-DEBT-001 expected at least one debt row in the citizen summary response")
        .isGreaterThan(0);
    return debtsNode.get(0);
  }

  private CitizenEffectiveInterestRateDto findEffectiveInterestRate(String interestRuleCode) {
    return lastResponse.getEffectiveInterestRates().stream()
        .filter(rate -> interestRuleCode.equals(rate.getInterestRuleCode()))
        .findFirst()
        .orElse(null);
  }

  private List<UUID> returnedDebtIds() {
    return lastResponse.getDebts().stream().map(CitizenDebtItemDto::getDebtId).toList();
  }

  private UUID personId(String citizenAlias) {
    return personIds.computeIfAbsent(
        citizenAlias,
        alias -> UUID.nameUUIDFromBytes(("petition026:" + alias).getBytes(StandardCharsets.UTF_8)));
  }

  private DebtEntity createDebt(
      UUID debtorPersonId,
      DebtEntity.DebtStatus status,
      ClaimLifecycleState lifecycleState,
      BigDecimal principalAmount,
      BigDecimal outstandingAmount,
      BigDecimal interestAmount,
      BigDecimal feesAmount,
      InterestSelectionEmbeddable interestSelection,
      boolean pausedInterest,
      String claimNote) {
    DebtEntity debt =
        DebtEntity.builder()
            .debtorPersonId(debtorPersonId)
            .creditorOrgId(UUID.randomUUID())
            .debtTypeCode("RESTSKAT")
            .principalAmount(principalAmount)
            .outstandingBalance(outstandingAmount)
            .interestAmount(interestAmount)
            .feesAmount(feesAmount)
            .dueDate(LocalDate.now().minusDays(30))
            .status(status)
            .lifecycleState(lifecycleState)
            .interestSelection(interestSelection)
            .ikkeinddrivelsesparat(pausedInterest)
            .claimNote(claimNote)
            .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
            .build();
    return debtRepository.save(debt);
  }
}
