package dk.ufst.opendebt.debtservice.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import dk.ufst.opendebt.debtservice.section50.Section50ClaimCategory;
import dk.ufst.opendebt.debtservice.section50.Section50ContextType;
import dk.ufst.opendebt.debtservice.section50.Section50ItemType;
import dk.ufst.opendebt.debtservice.section50.Section50ModregningOutcome;
import dk.ufst.opendebt.debtservice.section50.Section50OrderingMode;
import dk.ufst.opendebt.debtservice.section50.client.PaymentCoverageOrderClient;
import dk.ufst.opendebt.debtservice.section50.dto.GenerateSection50WorklistRequest;
import dk.ufst.opendebt.debtservice.section50.dto.Section50ModregningDecisionRequest;
import dk.ufst.opendebt.debtservice.section50.dto.Section50OverrideRequest;
import dk.ufst.opendebt.debtservice.section50.dto.Section50WorklistDto;
import dk.ufst.opendebt.debtservice.section50.dto.Section50WorklistEntryDto;
import dk.ufst.opendebt.debtservice.section50.entity.Section50CandidateItemEntity;
import dk.ufst.opendebt.debtservice.section50.repository.Section50CandidateItemRepository;
import dk.ufst.opendebt.debtservice.section50.repository.Section50DecisionSnapshotRepository;
import dk.ufst.opendebt.debtservice.section50.repository.Section50WorklistEntryRepository;
import dk.ufst.opendebt.debtservice.section50.repository.Section50WorklistRepository;
import dk.ufst.opendebt.debtservice.section50.service.Section50WorklistApplicationService;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class Petition060Steps {

  @Autowired private Section50WorklistApplicationService section50WorklistApplicationService;
  @Autowired private Section50CandidateItemRepository candidateItemRepository;
  @Autowired private Section50WorklistRepository worklistRepository;
  @Autowired private Section50WorklistEntryRepository worklistEntryRepository;
  @Autowired private Section50DecisionSnapshotRepository decisionSnapshotRepository;
  @Autowired private PaymentCoverageOrderClient paymentCoverageOrderClient;

  private final Map<String, UUID> debtorIds = new HashMap<>();
  private final Map<String, UUID> worklistIds = new HashMap<>();
  private final Map<String, UUID> candidateIds = new HashMap<>();

  private UUID activeDebtorId;
  private String activeDebtorAlias;
  private BigDecimal currentAvailableAmount;
  private BigDecimal currentConfirmedAmount;
  private String pendingOverrideReason;
  private String pendingOverrideLegalBasis;
  private String pendingDecisionReason;
  private boolean expeditedRequested;
  private List<String> expectedPrincipalOrder = List.of();
  private Section50WorklistDto lastWorklist;
  private Section50WorklistDto previousWorklist;
  private Section50WorklistDto repeatedWorklist;

  @Before("@petition060")
  public void resetPetition060Scenario() {
    decisionSnapshotRepository.deleteAll();
    worklistEntryRepository.deleteAll();
    worklistRepository.deleteAll();
    candidateItemRepository.deleteAll();
    debtorIds.clear();
    worklistIds.clear();
    candidateIds.clear();
    activeDebtorId = null;
    activeDebtorAlias = null;
    currentAvailableAmount = null;
    currentConfirmedAmount = BigDecimal.ZERO;
    pendingOverrideReason = null;
    pendingOverrideLegalBasis = "Section 50 special circumstances";
    pendingDecisionReason = null;
    expeditedRequested = false;
    expectedPrincipalOrder = List.of();
    lastWorklist = null;
    previousWorklist = null;
    repeatedWorklist = null;
    reset(paymentCoverageOrderClient);
    when(paymentCoverageOrderClient.orderPrincipalClaimIds(any(UUID.class), any(), anyList()))
        .thenAnswer(invocation -> List.copyOf(invocation.getArgument(2)));
  }

  // Trace: P060 FR-01/FR-02/FR-03/NFR-01 | Module: opendebt-debt-service.section50
  @Given("debtor {string} has these doubtful claims under collection:")
  public void debtorHasTheseDoubtfulClaimsUnderCollection(String debtorAlias, DataTable table) {
    setActiveDebtor(debtorAlias);
    seedDoubtfulClaims(table);
  }

  // Trace: P060 FR-01/NFR-01 | Module: opendebt-debt-service.section50
  @When("the system generates the default retskraft evaluation worklist")
  public void theSystemGeneratesTheDefaultRetskraftEvaluationWorklist() {
    generateWorklist(
        new GenerateSection50WorklistRequest(
            Section50ContextType.DEFAULT,
            currentAvailableAmount,
            currentConfirmedAmount,
            null,
            Boolean.TRUE));
  }

  // Trace: P060 AC-01/NFR-01 | Module: opendebt-debt-service.section50
  @Then("the ranked order is:")
  public void theRankedOrderIs(DataTable expectedOrder) {
    assertThat(rankedClaimIds(lastWorklist))
        .containsExactlyElementsOf(columnValues(expectedOrder, "claimId"));
  }

  // Trace: P060 AC-01 | Module: opendebt-debt-service.section50
  @And("the result identifies that the default section-50 ordering path was used")
  public void theResultIdentifiesThatTheDefaultSection50OrderingPathWasUsed() {
    assertThat(lastWorklist.orderingMode()).isEqualTo(Section50OrderingMode.DEFAULT_SECTION_50);
    assertThat(lastWorklist.decisionSnapshot().rulePath()).isEqualTo("DEFAULT_SECTION_50_PATH");
  }

  // Trace: P060 FR-02 | Module: opendebt-caseworker-portal.section50
  @And("a caseworker records override reason {string}")
  public void aCaseworkerRecordsOverrideReason(String reason) {
    pendingOverrideReason = reason;
    pendingOverrideLegalBasis = "Section 50 special circumstances";
    expectedPrincipalOrder =
        candidateItemRepository.findByDebtorPersonId(activeDebtorId).stream()
            .map(Section50CandidateItemEntity::getClaimId)
            .sorted(Comparator.reverseOrder())
            .toList();
  }

  // Trace: P060 FR-02 | Module: opendebt-debt-service.section50
  @When("the system generates the retskraft evaluation worklist with override enabled")
  public void theSystemGeneratesTheRetskraftEvaluationWorklistWithOverrideEnabled() {
    theSystemGeneratesTheDefaultRetskraftEvaluationWorklist();
    lastWorklist =
        section50WorklistApplicationService.applyOverride(
            activeDebtorId,
            lastWorklist.worklistId(),
            new Section50OverrideRequest(
                pendingOverrideReason,
                pendingOverrideLegalBasis,
                Boolean.FALSE,
                expectedPrincipalOrder));
  }

  // Trace: P060 AC-02 | Module: opendebt-debt-service.section50
  @Then("claim {string} may appear ahead of the default section-50 order")
  public void claimMayAppearAheadOfTheDefaultSection50Order(String claimId) {
    assertThat(rankedClaimIds(lastWorklist).get(0)).isEqualTo(claimId);
  }

  // Trace: P060 AC-02 | Module: opendebt-caseworker-portal.section50
  @And("the result includes override reason {string}")
  public void theResultIncludesOverrideReason(String reason) {
    assertThat(lastWorklist.overrideReason()).isEqualTo(reason);
  }

  // Trace: P060 AC-02 | Module: opendebt-caseworker-portal.section50
  @And("the result includes the legal basis for the override")
  public void theResultIncludesTheLegalBasisForTheOverride() {
    assertThat(lastWorklist.overrideLegalBasis()).isEqualTo(pendingOverrideLegalBasis);
    assertThat(lastWorklist.legalReference()).isEqualTo(pendingOverrideLegalBasis);
  }

  // Trace: P060 FR-03 | Module: opendebt-debt-service.section50
  @When("the system generates the retskraft evaluation worklist for suspected data error")
  public void theSystemGeneratesTheRetskraftEvaluationWorklistForSuspectedDataError() {
    generateWorklist(
        new GenerateSection50WorklistRequest(
            Section50ContextType.DATA_ERROR,
            currentAvailableAmount,
            currentConfirmedAmount,
            null,
            Boolean.TRUE));
  }

  // Trace: P060 AC-03 | Module: opendebt-debt-service.section50
  @Then("the result identifies that a discretionary data-error ordering path was used")
  public void theResultIdentifiesThatADiscretionaryDataErrorOrderingPathWasUsed() {
    assertThat(lastWorklist.orderingMode())
        .isEqualTo(Section50OrderingMode.DATA_ERROR_DISCRETIONARY);
    assertThat(lastWorklist.decisionSnapshot().rulePath())
        .isEqualTo("SECTION_50_DATA_ERROR_DISCRETIONARY_PATH");
  }

  // Trace: P060 AC-03 | Module: opendebt-debt-service.section50
  @And("each ranked item includes the factors used for prioritisation")
  public void eachRankedItemIncludesTheFactorsUsedForPrioritisation() {
    assertThat(lastWorklist.entries()).allMatch(entry -> !entry.prioritisationFactors().isEmpty());
  }

  // Trace: P060 AC-03 | Module: opendebt-debt-service.section50
  @And("the result does not claim that the default section-50 order was applied")
  public void theResultDoesNotClaimThatTheDefaultSection50OrderWasApplied() {
    assertThat(lastWorklist.orderingMode()).isNotEqualTo(Section50OrderingMode.DEFAULT_SECTION_50);
  }

  // Trace: P060 FR-04 | Module: opendebt-debt-service.section50
  @Given("principal claim {string} is not yet established as retskraftig")
  public void principalClaimIsNotYetEstablishedAsRetskraftig(String claimId) {
    ensureDefaultDebtor();
    seedCandidate(
        claimId,
        Section50ItemType.PRINCIPAL,
        Section50ClaimCategory.OTHER,
        "100.00",
        false,
        false,
        null,
        false,
        null,
        null,
        null);
  }

  // Trace: P060 FR-04 | Module: opendebt-debt-service.section50
  @And("accessory amount {string} belongs to principal claim {string}")
  public void accessoryAmountBelongsToPrincipalClaim(String accessoryId, String principalClaimId) {
    ensureDefaultDebtor();
    seedCandidate(
        accessoryId,
        Section50ItemType.ACCESSORY,
        Section50ClaimCategory.OTHER,
        "25.00",
        false,
        true,
        principalClaimId,
        false,
        null,
        null,
        null);
  }

  // Trace: P060 FR-04 | Module: opendebt-debt-service.section50
  @When("the system generates the retskraft evaluation worklist")
  public void theSystemGeneratesTheRetskraftEvaluationWorklist() {
    generateWorklist(
        new GenerateSection50WorklistRequest(
            Section50ContextType.DEFAULT,
            currentAvailableAmount,
            currentConfirmedAmount,
            null,
            Boolean.TRUE));
  }

  // Trace: P060 AC-04 | Module: opendebt-debt-service.section50
  @Then("accessory amount {string} is not ranked ahead of principal claim {string}")
  public void accessoryAmountIsNotRankedAheadOfPrincipalClaim(
      String accessoryId, String principalClaimId) {
    List<String> claimIds = rankedClaimIds(lastWorklist);
    int principalIndex = claimIds.indexOf(principalClaimId);
    int accessoryIndex = claimIds.indexOf(accessoryId);
    assertThat(principalIndex).isGreaterThanOrEqualTo(0);
    assertThat(accessoryIndex == -1 || accessoryIndex > principalIndex).isTrue();
  }

  // Trace: P060 FR-04 | Module: opendebt-debt-service.section50
  @When("principal claim {string} becomes established as retskraftig")
  public void principalClaimBecomesEstablishedAsRetskraftig(String claimId) {
    Section50CandidateItemEntity principal = requireCandidate(claimId);
    principal.setConfirmedRetskraft(true);
    candidateItemRepository.save(principal);
    previousWorklist = lastWorklist;
    theSystemGeneratesTheRetskraftEvaluationWorklist();
  }

  // Trace: P060 AC-04 | Module: opendebt-debt-service.section50
  @Then("accessory amount {string} may be included after principal claim {string}")
  public void accessoryAmountMayBeIncludedAfterPrincipalClaim(
      String accessoryId, String principalClaimId) {
    List<String> claimIds = rankedClaimIds(lastWorklist);
    assertThat(claimIds).contains(principalClaimId, accessoryId);
    assertThat(claimIds.indexOf(accessoryId)).isGreaterThan(claimIds.indexOf(principalClaimId));
    if (previousWorklist != null) {
      assertThat(rankedClaimIds(previousWorklist)).doesNotContain(accessoryId);
    }
  }

  // Trace: P060 FR-04 | Module: opendebt-debt-service.section50
  @Given("principal claim {string} is established as retskraftig")
  public void principalClaimIsEstablishedAsRetskraftig(String claimId) {
    ensureDefaultDebtor();
    seedCandidate(
        claimId,
        Section50ItemType.PRINCIPAL,
        Section50ClaimCategory.OTHER,
        "100.00",
        false,
        true,
        null,
        false,
        null,
        null,
        null);
  }

  // Trace: P060 FR-04 | Module: opendebt-debt-service.section50
  @And("accessory amount {string} is written off because evaluation would be disproportionate")
  public void accessoryAmountIsWrittenOffBecauseEvaluationWouldBeDisproportionate(
      String accessoryId) {
    Section50CandidateItemEntity accessory = requireCandidate(accessoryId);
    accessory.setDisproportionateWriteOff(true);
    candidateItemRepository.save(accessory);
  }

  // Trace: P060 AC-04 | Module: opendebt-debt-service.section50
  @Then("accessory amount {string} is excluded from the worklist")
  public void accessoryAmountIsExcludedFromTheWorklist(String accessoryId) {
    assertThat(rankedClaimIds(lastWorklist)).doesNotContain(accessoryId);
  }

  // Trace: P060 FR-05 | Module: opendebt-debt-service.section50
  @Given(
      "debtor {string} has already covered retskraftige claims for {int} DKK from a {int} DKK payment")
  public void debtorHasAlreadyCoveredRetskraftigeClaimsFromAPayment(
      String debtorAlias, Integer coveredAmount, Integer paymentAmount) {
    setActiveDebtor(debtorAlias);
    currentConfirmedAmount = BigDecimal.valueOf(coveredAmount.longValue());
    currentAvailableAmount = BigDecimal.valueOf(paymentAmount.longValue());
  }

  // Trace: P060 FR-05/FR-07 | Module: opendebt-debt-service.section50
  @And("debtor {string} has these remaining doubtful items:")
  public void debtorHasTheseRemainingDoubtfulItems(String debtorAlias, DataTable table) {
    setActiveDebtor(debtorAlias);
    List<Map<String, String>> rows = table.asMaps(String.class, String.class);
    for (Map<String, String> row : rows) {
      seedCandidate(
          row.get("itemId"),
          Section50ItemType.valueOf(row.get("itemType")),
          row.containsKey("category")
              ? Section50ClaimCategory.valueOf(row.get("category"))
              : Section50ClaimCategory.OTHER,
          row.get("amount"),
          false,
          false,
          row.get("itemType").equals("ACCESSORY")
              ? inferPrincipalFromAccessory(row.get("itemId"), rows)
              : null,
          false,
          null,
          null,
          null);
    }

    List<String> principalIds =
        rows.stream()
            .filter(row -> "PRINCIPAL".equals(row.get("itemType")))
            .map(row -> row.get("itemId"))
            .toList();
    expectedPrincipalOrder = new ArrayList<>(principalIds);
    expectedPrincipalOrder.sort(Comparator.reverseOrder());
    when(paymentCoverageOrderClient.orderPrincipalClaimIds(any(UUID.class), any(), anyList()))
        .thenAnswer(invocation -> List.copyOf(expectedPrincipalOrder));
  }

  // Trace: P060 FR-05 | Module: opendebt-debt-service.section50
  @When("the system generates a retskraft worklist for the remaining voluntary-payment surplus")
  public void theSystemGeneratesARetskraftWorklistForTheRemainingVoluntaryPaymentSurplus() {
    generateWorklist(
        new GenerateSection50WorklistRequest(
            Section50ContextType.VOLUNTARY_PAYMENT_SURPLUS,
            currentAvailableAmount,
            currentConfirmedAmount,
            null,
            Boolean.TRUE));
  }

  // Trace: P060 AC-05/AC-07 | Module: opendebt-debt-service.section50
  @Then("the remaining amount window is {int} DKK")
  public void theRemainingAmountWindowIsDkk(Integer amountWindow) {
    assertThat(lastWorklist.amountWindow())
        .isEqualByComparingTo(BigDecimal.valueOf(amountWindow.longValue()));
  }

  // Trace: P060 AC-05 | Module: opendebt-payment-service.coverage-simulation-internal
  @And("selected principal items follow the applicable GIL section 4 ordering")
  public void selectedPrincipalItemsFollowTheApplicableGilSection4Ordering() {
    List<String> actualPrincipalOrder =
        lastWorklist.entries().stream()
            .filter(
                entry ->
                    entry.itemType() == Section50ItemType.PRINCIPAL && !entry.confirmedRetskraft())
            .map(Section50WorklistEntryDto::claimId)
            .toList();
    assertThat(actualPrincipalOrder).startsWith(expectedPrincipalOrder.toArray(String[]::new));
  }

  // Trace: P060 AC-05/AC-07 | Module: opendebt-debt-service.section50
  @And("accessory item {string} is ranked after principal items")
  public void accessoryItemIsRankedAfterPrincipalItems(String accessoryId) {
    List<String> claimIds = rankedClaimIds(lastWorklist);
    int accessoryIndex = claimIds.indexOf(accessoryId);
    int lastPrincipalIndex =
        Math.max(
            claimIds.indexOf(
                lastWorklist.entries().stream()
                    .filter(entry -> entry.itemType() == Section50ItemType.PRINCIPAL)
                    .reduce((left, right) -> right)
                    .orElseThrow()
                    .claimId()),
            0);
    assertThat(accessoryIndex).isGreaterThan(lastPrincipalIndex);
  }

  // Trace: P060 AC-05 | Module: opendebt-debt-service.section50
  @And("the selected doubtful amount does not exceed {int} DKK")
  public void theSelectedDoubtfulAmountDoesNotExceedDkk(Integer amount) {
    BigDecimal selectedAmount =
        lastWorklist.entries().stream()
            .filter(Section50WorklistEntryDto::withinAmountWindow)
            .map(Section50WorklistEntryDto::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(selectedAmount).isLessThanOrEqualTo(BigDecimal.valueOf(amount.longValue()));
  }

  // Trace: P060 FR-06 | Module: opendebt-debt-service.section50
  @Given("debtor {string} has a voluntary-payment surplus waiting to be applied")
  public void debtorHasAVoluntaryPaymentSurplusWaitingToBeApplied(String debtorAlias) {
    setActiveDebtor(debtorAlias);
    currentAvailableAmount = new BigDecimal("400.00");
    currentConfirmedAmount = BigDecimal.ZERO;
    if (candidateItemRepository.findByDebtorPersonId(activeDebtorId).isEmpty()) {
      seedCandidate(
          "C-06061",
          Section50ItemType.PRINCIPAL,
          Section50ClaimCategory.OTHER,
          "300.00",
          false,
          false,
          null,
          false,
          null,
          null,
          null);
      seedCandidate(
          "C-06062",
          Section50ItemType.PRINCIPAL,
          Section50ClaimCategory.OTHER,
          "100.00",
          false,
          false,
          null,
          false,
          null,
          null,
          null);
      expectedPrincipalOrder = List.of("C-06061", "C-06062");
      when(paymentCoverageOrderClient.orderPrincipalClaimIds(any(UUID.class), any(), anyList()))
          .thenReturn(List.of("C-06061", "C-06062"));
    }
  }

  // Trace: P060 FR-06 | Module: opendebt-debt-service.section50
  @And("the normal section-50 ordering would delay application of that surplus")
  public void theNormalSection50OrderingWouldDelayApplicationOfThatSurplus() {
    expeditedRequested = true;
    pendingOverrideReason = "Quicker-to-apply claims were prioritised";
    pendingOverrideLegalBasis = "Section 50 subsection 4";
  }

  // Trace: P060 FR-06 | Module: opendebt-debt-service.section50
  @When("the system generates the expedited retskraft worklist for that payment context")
  public void theSystemGeneratesTheExpeditedRetskraftWorklistForThatPaymentContext() {
    theSystemGeneratesARetskraftWorklistForTheRemainingVoluntaryPaymentSurplus();
    lastWorklist =
        section50WorklistApplicationService.applyOverride(
            activeDebtorId,
            lastWorklist.worklistId(),
            new Section50OverrideRequest(
                pendingOverrideReason,
                pendingOverrideLegalBasis,
                expeditedRequested,
                expectedPrincipalOrder));
  }

  // Trace: P060 AC-06 | Module: opendebt-caseworker-portal.section50
  @Then("the worklist records that an expedited deviation was used")
  public void theWorklistRecordsThatAnExpeditedDeviationWasUsed() {
    assertThat(lastWorklist.orderingMode()).isEqualTo(Section50OrderingMode.EXPEDITED_SURPLUS);
  }

  // Trace: P060 AC-06 | Module: opendebt-caseworker-portal.section50
  @And("the result explains why quicker-to-apply claims were prioritised")
  public void theResultExplainsWhyQuickerToApplyClaimsWerePrioritised() {
    assertThat(lastWorklist.deviationReason()).contains("Quicker-to-apply claims");
  }

  // Trace: P060 FR-07 | Module: opendebt-debt-service.section50
  @Given("debtor {string} has an overskydende beloeb of {int} DKK for modregning")
  public void debtorHasAnOverskydendeBeloebOfDkkForModregning(String debtorAlias, Integer amount) {
    setActiveDebtor(debtorAlias);
    currentAvailableAmount = BigDecimal.valueOf(amount.longValue());
  }

  // Trace: P060 FR-07 | Module: opendebt-debt-service.section50
  @And(
      "debtor {string} has confirmed retskraftige claims without suspected data error totalling {int} DKK")
  public void debtorHasConfirmedRetskraftigeClaimsWithoutSuspectedDataErrorTotallingDkk(
      String debtorAlias, Integer amount) {
    setActiveDebtor(debtorAlias);
    currentConfirmedAmount = BigDecimal.valueOf(amount.longValue());
    seedCandidate(
        "CONFIRMED-" + debtorAlias,
        Section50ItemType.PRINCIPAL,
        Section50ClaimCategory.OTHER,
        amount.toString(),
        false,
        true,
        null,
        false,
        null,
        null,
        null);
  }

  // Trace: P060 FR-07 | Module: opendebt-debt-service.section50
  @When("the system generates the modregning retskraft worklist")
  public void theSystemGeneratesTheModregningRetskraftWorklist() {
    generateWorklist(
        new GenerateSection50WorklistRequest(
            Section50ContextType.MODREGNING,
            currentAvailableAmount,
            currentConfirmedAmount,
            null,
            Boolean.TRUE));
  }

  // Trace: P060 AC-07 | Module: opendebt-debt-service.section50
  @Then("confirmed retskraftige claims are used before doubtful items")
  public void confirmedRetskraftigeClaimsAreUsedBeforeDoubtfulItems() {
    assertThat(lastWorklist.entries().get(0).confirmedRetskraft()).isTrue();
  }

  // Trace: P060 AC-07 | Module: opendebt-debt-service.section50
  @And("the remaining amount window for doubtful items is {int} DKK")
  public void theRemainingAmountWindowForDoubtfulItemsIsDkk(Integer amountWindow) {
    assertThat(lastWorklist.amountWindow())
        .isEqualByComparingTo(BigDecimal.valueOf(amountWindow.longValue()));
  }

  // Trace: P060 FR-08 | Module: opendebt-caseworker-portal.section50
  @Given("debtor {string} is in a modregning context with payout deadline pressure")
  public void debtorIsInAModregningContextWithPayoutDeadlinePressure(String debtorAlias) {
    setActiveDebtor(debtorAlias);
    currentAvailableAmount = new BigDecimal("250.00");
    currentConfirmedAmount = BigDecimal.ZERO;
    seedCandidate(
        "C-06081",
        Section50ItemType.PRINCIPAL,
        Section50ClaimCategory.OTHER,
        "250.00",
        true,
        false,
        null,
        false,
        "DATA_MISMATCH",
        "high",
        "low");
  }

  // Trace: P060 FR-08 | Module: opendebt-caseworker-portal.section50
  @And("the required investigations are too complex to complete before the payout deadline")
  public void theRequiredInvestigationsAreTooComplexToCompleteBeforeThePayoutDeadline() {
    pendingDecisionReason =
        "Timing or complexity constraints prevent safe modregning before payout deadline";
  }

  // Trace: P060 FR-08 | Module: opendebt-debt-service.section50
  @When("the system records a decision to perform no modregning for the current payout")
  public void theSystemRecordsADecisionToPerformNoModregningForTheCurrentPayout() {
    theSystemGeneratesTheModregningRetskraftWorklist();
    lastWorklist =
        section50WorklistApplicationService.recordModregningDecision(
            activeDebtorId,
            lastWorklist.worklistId(),
            new Section50ModregningDecisionRequest(
                Section50ModregningOutcome.NO_MODREGNING, pendingDecisionReason));
  }

  // Trace: P060 AC-08 | Module: opendebt-caseworker-portal.section50
  @Then("the decision is visible to the caseworker")
  public void theDecisionIsVisibleToTheCaseworker() {
    assertThat(lastWorklist.orderingMode()).isEqualTo(Section50OrderingMode.MODREGNING_ABSTAINED);
    assertThat(lastWorklist.modregningOutcome())
        .isEqualTo(Section50ModregningOutcome.NO_MODREGNING);
  }

  // Trace: P060 AC-08 | Module: opendebt-caseworker-portal.section50
  @And("the reason references timing or complexity constraints")
  public void theReasonReferencesTimingOrComplexityConstraints() {
    assertThat(lastWorklist.deviationReason())
        .containsIgnoringCase("timing")
        .containsIgnoringCase("complexity");
  }

  // Trace: P060 FR-09/FR-10 | Module: opendebt-caseworker-portal.section50
  @Given("a retskraft evaluation worklist already exists for debtor {string}")
  public void aRetskraftEvaluationWorklistAlreadyExistsForDebtor(String debtorAlias) {
    setActiveDebtor(debtorAlias);
    seedCandidate(
        "C-06091",
        Section50ItemType.PRINCIPAL,
        Section50ClaimCategory.FINE,
        "200.00",
        false,
        false,
        null,
        false,
        null,
        null,
        null);
    theSystemGeneratesTheDefaultRetskraftEvaluationWorklist();
    worklistIds.put(debtorAlias, lastWorklist.worklistId());
  }

  // Trace: P060 FR-09/FR-10 | Module: opendebt-caseworker-portal.section50
  @When("a caseworker inspects the ranking details")
  public void aCaseworkerInspectsTheRankingDetails() {
    lastWorklist =
        section50WorklistApplicationService.getWorklist(activeDebtorId, lastWorklist.worklistId());
  }

  // Trace: P060 AC-09/AC-10 | Module: opendebt-caseworker-portal.section50
  @Then("the result shows the ordering mode used")
  public void theResultShowsTheOrderingModeUsed() {
    assertThat(lastWorklist.orderingMode()).isNotNull();
  }

  // Trace: P060 AC-10 | Module: opendebt-caseworker-portal.section50
  @And("the result shows the legal reference for the rule path")
  public void theResultShowsTheLegalReferenceForTheRulePath() {
    assertThat(lastWorklist.legalReference()).isNotBlank();
    assertThat(lastWorklist.decisionSnapshot().legalReference())
        .isEqualTo(lastWorklist.legalReference());
  }

  // Trace: P060 AC-10 | Module: opendebt-caseworker-portal.section50
  @And("the result shows the actor or system origin and timestamp")
  public void theResultShowsTheActorOrSystemOriginAndTimestamp() {
    assertThat(lastWorklist.decisionSnapshot().origin()).isEqualTo("SYSTEM");
    assertThat(lastWorklist.generatedAt()).isNotNull();
    assertThat(lastWorklist.decisionSnapshot().occurredAt()).isNotNull();
  }

  // Trace: P060 AC-10 | Module: opendebt-caseworker-portal.section50
  @And("the result uses technical identifiers only")
  public void theResultUsesTechnicalIdentifiersOnly() {
    assertThat(lastWorklist.debtorId()).isNotNull();
    assertThat(lastWorklist.entries())
        .allMatch(
            entry ->
                entry.claimId().startsWith("C-")
                    || entry.claimId().startsWith("A-")
                    || entry.claimId().startsWith("CONFIRMED-"));
  }

  // Trace: P060 NFR-01 | Module: opendebt-debt-service.section50
  @And("the same input is evaluated again without manual override")
  public void theSameInputIsEvaluatedAgainWithoutManualOverride() {
    repeatedWorklist =
        section50WorklistApplicationService.generateWorklist(
            activeDebtorId,
            new GenerateSection50WorklistRequest(
                Section50ContextType.DEFAULT,
                currentAvailableAmount,
                currentConfirmedAmount,
                null,
                Boolean.TRUE));
  }

  // Trace: P060 NFR-01 | Module: opendebt-debt-service.section50
  @Then("both generated worklists use the same ranked order:")
  public void bothGeneratedWorklistsUseTheSameRankedOrder(DataTable expectedOrder) {
    List<String> expectedClaimIds = columnValues(expectedOrder, "claimId");
    assertThat(rankedClaimIds(lastWorklist)).containsExactlyElementsOf(expectedClaimIds);
    assertThat(rankedClaimIds(repeatedWorklist)).containsExactlyElementsOf(expectedClaimIds);
    assertThat(rankedClaimIds(repeatedWorklist))
        .containsExactlyElementsOf(rankedClaimIds(lastWorklist));
  }

  // Trace: P060 NFR-01 | Module: opendebt-debt-service.section50
  @And("both results identify that the default section-50 ordering path was used")
  public void bothResultsIdentifyThatTheDefaultSection50OrderingPathWasUsed() {
    assertThat(lastWorklist.orderingMode()).isEqualTo(Section50OrderingMode.DEFAULT_SECTION_50);
    assertThat(repeatedWorklist.orderingMode()).isEqualTo(Section50OrderingMode.DEFAULT_SECTION_50);
    assertThat(lastWorklist.decisionSnapshot().rulePath()).isEqualTo("DEFAULT_SECTION_50_PATH");
    assertThat(repeatedWorklist.decisionSnapshot().rulePath()).isEqualTo("DEFAULT_SECTION_50_PATH");
  }

  private void seedDoubtfulClaims(DataTable table) {
    List<Map<String, String>> rows = table.asMaps(String.class, String.class);
    for (int index = 0; index < rows.size(); index++) {
      Map<String, String> row = rows.get(index);
      String amount = row.getOrDefault("amount", String.valueOf(100 + (index * 10)));
      seedCandidate(
          row.get("claimId"),
          Section50ItemType.PRINCIPAL,
          Section50ClaimCategory.valueOf(row.get("category")),
          amount,
          Boolean.parseBoolean(row.getOrDefault("suspectedDataError", "false")),
          false,
          null,
          false,
          row.get("errorType"),
          row.get("complexity"),
          row.get("paymentOpportunity"));
    }
  }

  private void generateWorklist(GenerateSection50WorklistRequest request) {
    previousWorklist = lastWorklist;
    lastWorklist = section50WorklistApplicationService.generateWorklist(activeDebtorId, request);
  }

  private void setActiveDebtor(String debtorAlias) {
    activeDebtorAlias = debtorAlias;
    activeDebtorId =
        debtorIds.computeIfAbsent(
            debtorAlias,
            alias ->
                UUID.nameUUIDFromBytes(("petition060-" + alias).getBytes(StandardCharsets.UTF_8)));
  }

  private void ensureDefaultDebtor() {
    if (activeDebtorId == null) {
      setActiveDebtor("petition060-default");
    }
  }

  private void seedCandidate(
      String claimId,
      Section50ItemType itemType,
      Section50ClaimCategory claimCategory,
      String amount,
      boolean suspectedDataError,
      boolean confirmedRetskraft,
      String accessoryOfClaimId,
      boolean disproportionateWriteOff,
      String errorType,
      String complexity,
      String paymentOpportunity) {
    Section50CandidateItemEntity entity =
        Section50CandidateItemEntity.builder()
            .debtorPersonId(activeDebtorId)
            .claimId(claimId)
            .itemType(itemType)
            .claimCategory(claimCategory)
            .amount(new BigDecimal(amount))
            .suspectedDataError(suspectedDataError)
            .confirmedRetskraft(confirmedRetskraft)
            .accessoryOfClaimId(accessoryOfClaimId)
            .disproportionateWriteOff(disproportionateWriteOff)
            .errorType(errorType)
            .complexity(complexity)
            .paymentOpportunity(paymentOpportunity)
            .build();
    entity = candidateItemRepository.save(entity);
    candidateIds.put(claimId, entity.getId());
  }

  private Section50CandidateItemEntity requireCandidate(String claimId) {
    return candidateItemRepository.findById(candidateIds.get(claimId)).orElseThrow();
  }

  private List<String> rankedClaimIds(Section50WorklistDto worklist) {
    return worklist.entries().stream().map(Section50WorklistEntryDto::claimId).toList();
  }

  private List<String> columnValues(DataTable table, String columnName) {
    return table.asMaps(String.class, String.class).stream()
        .map(row -> row.get(columnName))
        .toList();
  }

  private String inferPrincipalFromAccessory(String accessoryId, List<Map<String, String>> rows) {
    return rows.stream()
        .filter(row -> "PRINCIPAL".equals(row.get("itemType")))
        .map(row -> row.get("itemId"))
        .findFirst()
        .orElse(accessoryId.replace("A-", "C-"));
  }
}
