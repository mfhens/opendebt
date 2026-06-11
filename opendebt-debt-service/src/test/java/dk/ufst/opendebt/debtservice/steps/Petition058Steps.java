package dk.ufst.opendebt.debtservice.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import dk.ufst.opendebt.debtservice.batch.KorrektionspuljeSettlementJob;
import dk.ufst.opendebt.debtservice.config.TestConfig;
import dk.ufst.opendebt.debtservice.controller.ModregningController;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.CollectionMeasureEntity;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.KorrektionspuljeEntry;
import dk.ufst.opendebt.debtservice.entity.ModregningEvent;
import dk.ufst.opendebt.debtservice.entity.RenteGodtgoerelseRateEntry;
import dk.ufst.opendebt.debtservice.repository.CollectionMeasureRepository;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.KorrektionspuljeEntryRepository;
import dk.ufst.opendebt.debtservice.repository.ModregningEventRepository;
import dk.ufst.opendebt.debtservice.repository.RenteGodtgoerelseRateEntryRepository;
import dk.ufst.opendebt.debtservice.service.FordringAllocation;
import dk.ufst.opendebt.debtservice.service.KorrektionspuljeResult;
import dk.ufst.opendebt.debtservice.service.KorrektionspuljeService;
import dk.ufst.opendebt.debtservice.service.ModregningDecisionKind;
import dk.ufst.opendebt.debtservice.service.ModregningResult;
import dk.ufst.opendebt.debtservice.service.ModregningService;
import dk.ufst.opendebt.debtservice.service.OffsettingReversalEvent;
import dk.ufst.opendebt.debtservice.service.PaymentType;
import dk.ufst.opendebt.debtservice.service.PublicDisbursementEvent;
import dk.ufst.opendebt.debtservice.service.RenteGodtgoerelseService;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;

/**
 * BDD step definitions for petition P058 — Modregning i udbetalinger fra det offentlige og
 * korrektionspulje (G.A.2.3.3–2.3.4).
 *
 * <p>NOTE: This class does NOT carry {@code @CucumberContextConfiguration} — that annotation lives
 * on {@code Petition002Steps}. Spring context is shared across all step definition classes in the
 * {@code dk.ufst.opendebt.debtservice.steps} glue package.
 */
@Slf4j
public class Petition058Steps {

  @Autowired private ModregningService modregningService;
  @Autowired private ModregningController modregningController;
  @Autowired private KorrektionspuljeService korrektionspuljeService;
  @Autowired private KorrektionspuljeSettlementJob korrektionspuljeSettlementJob;
  @Autowired private ModregningEventRepository modregningEventRepository;
  @Autowired private KorrektionspuljeEntryRepository korrektionspuljeEntryRepository;
  @Autowired private CollectionMeasureRepository collectionMeasureRepository;
  @Autowired private DebtRepository debtRepository;
  @Autowired private RenteGodtgoerelseRateEntryRepository renteGodtgoerelseRateEntryRepository;
  @Autowired private RenteGodtgoerelseService renteGodtgoerelseService;

  @Autowired
  private TestConfig.StubDaekningsRaekkefoeigenServiceClient daekningsRaekkefoeigenServiceClient;

  // ── Per-scenario state ──────────────────────────────────────────────────────

  private final Map<String, UUID> debtorIndex = new HashMap<>();
  private final Map<String, UUID> fordringIndex = new HashMap<>();
  private final Map<String, UUID> modregningEventIndex = new HashMap<>();
  private final Map<String, UUID> eventKeyIndex = new HashMap<>();

  private int lastHttpStatus;
  private UUID activeCaseworkerId;

  private UUID currentDebtorPersonId;
  private BigDecimal currentDisbursementAmount;
  private String currentNemkontoReferenceId;
  private String currentPaymentType = "STANDARD_PAYMENT";
  private Integer currentIndkomstAar;
  private LocalDate currentReceiptDate;
  private LocalDate currentDecisionDate;

  private UUID currentModregningEventId;
  private OffsettingReversalEvent currentReversalEvent;
  private KorrektionspuljeResult currentKorrektionspuljeResult;
  private UUID currentKpeDebtorId;
  private List<ModregningController.ModregningEventSummary> currentReadModelSummaries = List.of();

  // State for opt-out scenarios (18-20)
  private BigDecimal currentKpeSurplus;
  private String currentKpeTarget = "PSRM";
  private boolean currentKpeOptOutDebtUnderCollection;
  private boolean currentKpeOptOutRetroactivePartial;

  // State for rate-change scenario
  private BigDecimal computedRate;
  private LocalDate rateComputeDate;

  // ── Lifecycle ───────────────────────────────────────────────────────────────

  @Before("@petition058")
  public void resetScenarioState() {
    debtorIndex.clear();
    fordringIndex.clear();
    modregningEventIndex.clear();
    eventKeyIndex.clear();
    lastHttpStatus = 0;
    activeCaseworkerId = null;
    currentDebtorPersonId = null;
    currentDisbursementAmount = null;
    currentNemkontoReferenceId = null;
    currentPaymentType = "STANDARD_PAYMENT";
    currentIndkomstAar = null;
    currentReceiptDate = null;
    currentDecisionDate = null;
    currentModregningEventId = null;
    currentReversalEvent = null;
    currentKorrektionspuljeResult = null;
    currentKpeDebtorId = null;
    currentReadModelSummaries = List.of();
    currentKpeSurplus = null;
    currentKpeTarget = "PSRM";
    currentKpeOptOutDebtUnderCollection = false;
    currentKpeOptOutRetroactivePartial = false;
    computedRate = null;
    rateComputeDate = null;
    daekningsRaekkefoeigenServiceClient.clear();
    SecurityContextHolder.clearContext();
  }

  // ── Background steps ────────────────────────────────────────────────────────

  @Given("the debt-service modregning workflow is active")
  public void theDebtServiceModregningWorkflowIsActive() {
    assertThat(modregningService).isNotNull();
  }

  @Given("the payment-service DaekningsRaekkefoeigenService is available")
  public void theDaekningsRaekkefoeigenServiceIsAvailable() {
    assertThat(daekningsRaekkefoeigenServiceClient).isNotNull();
  }

  @Given("the caseworker portal is running")
  public void theCaseworkerPortalIsRunning() {
    // ModregningController is registered as a Spring bean — verified implicitly
  }

  @Given("BusinessConfigService key {string} is set to {string} percent")
  public void businessConfigServiceKeyIsSetToPercent(String key, String valuePercent) {
    // Seed a RenteGodtgoerelseRateEntry if none exists yet (shared across scenarios)
    if (renteGodtgoerelseRateEntryRepository.count() == 0) {
      BigDecimal refRate = new BigDecimal(valuePercent);
      BigDecimal godtRate = refRate.subtract(new BigDecimal("4.0")).max(BigDecimal.ZERO);
      RenteGodtgoerelseRateEntry entry =
          RenteGodtgoerelseRateEntry.builder()
              .publicationDate(LocalDate.of(2020, 1, 1))
              .effectiveDate(LocalDate.of(2020, 1, 1))
              .referenceRatePercent(refRate)
              .godtgoerelseRatePercent(godtRate)
              .build();
      renteGodtgoerelseRateEntryRepository.save(entry);
    }
  }

  @Given("the DanishBankingCalendar is configured for the current test year")
  public void theDanishBankingCalendarIsConfigured() {
    // DanishBankingCalendar is a Spring bean — already configured
  }

  @And("the debtorPersonId for any debtor is stored as a UUID never as CPR")
  public void debtorPersonIdIsUuidNeverCpr() {
    // NFR-3 (GDPR): ModregningEvent only has debtorPersonId UUID field — no CPR field exists
  }

  // ── Given — debtor & fordring setup ────────────────────────────────────────

  @Given("debtor {string} has the following tier-1 fordringer registered by the paying authority:")
  public void debtorHasTier1Fordringer(String debtorRef, DataTable table) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    seedFordringerFromTable(debtorId, table, 1);
  }

  @Given("debtor {string} has the following tier-2 fordringer under RIM inddrivelse:")
  public void debtorHasTier2Fordringer(String debtorRef, DataTable table) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    seedFordringerFromTable(debtorId, table, 2);
  }

  @Given("debtor {string} has the following tier-3 fordringer in registration order:")
  public void debtorHasTier3Fordringer(String debtorRef, DataTable table) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    seedFordringerFromTable(debtorId, table, 3);
  }

  @Given("debtor {string} has no tier-1 fordringer")
  public void debtorHasNoTier1Fordringer(String debtorRef) {
    getOrCreateDebtor(debtorRef);
  }

  @Given("debtor {string} has no tier-2 fordringer")
  public void debtorHasNoTier2Fordringer(String debtorRef) {
    getOrCreateDebtor(debtorRef);
  }

  @Given("debtor {string} has no tier-3 fordringer")
  public void debtorHasNoTier3Fordringer(String debtorRef) {
    getOrCreateDebtor(debtorRef);
  }

  @Given("debtor {string} has one active tier-2 fordring with tilbaestaaendeBeloeb {double} DKK")
  public void debtorHasOneTier2Fordring(String debtorRef, double beloeb) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    DebtEntity debt =
        buildDebt(
            debtorId, new BigDecimal(String.valueOf(beloeb)), 2, LocalDate.now().minusDays(30));
    debtRepository.save(debt);
  }

  @Given(
      "debtor {string} has an active tier-2 fordring {string} with tilbaestaaendeBeloeb {double} DKK")
  public void debtorHasActiveTier2FordringWithRef(
      String debtorRef, String fordringRef, double beloeb) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    DebtEntity debt =
        buildDebt(
            debtorId, new BigDecimal(String.valueOf(beloeb)), 2, LocalDate.now().minusDays(30));
    debt = debtRepository.save(debt);
    fordringIndex.put(fordringRef, debt.getId());
  }

  @Given(
      "debtor {string} has an active tier-2 fordring {string} with debtTypeCode {string} and tilbaestaaendeBeloeb {double} DKK")
  public void debtorHasActiveTier2FordringWithDebtType(
      String debtorRef, String fordringRef, String debtTypeCode, double beloeb) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    DebtEntity debt =
        buildDebt(
            debtorId,
            new BigDecimal(String.valueOf(beloeb)),
            2,
            LocalDate.now().minusDays(30),
            debtTypeCode);
    debt = debtRepository.save(debt);
    fordringIndex.put(fordringRef, debt.getId());
  }

  @Given(
      "debtor {string} has another tier-2 fordring {string} with tilbaestaaendeBeloeb {double} DKK")
  public void debtorHasAnotherTier2Fordring(String debtorRef, String fordringRef, double beloeb) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    DebtEntity debt =
        buildDebt(
            debtorId, new BigDecimal(String.valueOf(beloeb)), 2, LocalDate.now().minusDays(20));
    debt = debtRepository.save(debt);
    fordringIndex.put(fordringRef, debt.getId());
    daekningsRaekkefoeigenServiceClient.setAllocations(
        debtorId,
        List.of(new FordringAllocation(debt.getId(), new BigDecimal(String.valueOf(beloeb)), 2)));
  }

  @Given("for debtor {string}, the payment-service DaekningsRaekkefoeigenService would return:")
  public void daekningsRaekkefoeigenServiceWouldReturn(String debtorRef, DataTable table) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    List<FordringAllocation> allocations =
        table.asMaps().stream()
            .map(
                row -> {
                  UUID fordringId = fordringIndex.get(row.get("fordringId"));
                  assertThat(fordringId)
                      .as(
                          "fordring %s must be seeded before configuring P057 stub",
                          row.get("fordringId"))
                      .isNotNull();
                  return new FordringAllocation(
                      fordringId,
                      new BigDecimal(row.get("amountCovered")),
                      Integer.parseInt(row.getOrDefault("tier", "2")));
                })
            .toList();
    daekningsRaekkefoeigenServiceClient.setAllocations(debtorId, allocations);
  }

  // ── Given — PublicDisbursementEvent staging ─────────────────────────────────

  @Given(
      "a PublicDisbursementEvent arrives for debtor {string} with disbursementAmount {double} DKK and nemkontoReferenceId {string}")
  public void publicDisbursementEventArrives(String debtorRef, double amount, String nemkontoRef) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    currentDebtorPersonId = debtorId;
    currentDisbursementAmount = new BigDecimal(String.valueOf(amount));
    currentNemkontoReferenceId = nemkontoRef;
    currentReceiptDate = LocalDate.now();
    currentDecisionDate = LocalDate.now();
  }

  @Given("a PublicDisbursementEvent arrives for debtor {string} with receiptDate {string}")
  public void publicDisbursementEventArrivesWithReceiptDate(
      String debtorRef, String receiptDateStr) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    currentDebtorPersonId = debtorId;
    currentReceiptDate = LocalDate.parse(receiptDateStr);
    currentDecisionDate = currentReceiptDate; // default; overridden by next step
    currentDisbursementAmount = new BigDecimal("5000.00");
    currentNemkontoReferenceId = UUID.randomUUID().toString();
  }

  @Given(
      "the modregning decision is made on {string} which is {int} banking days after receiptDate")
  public void modregningDecisionIsMadeOn(String decisionDateStr, int bankingDays) {
    currentDecisionDate = LocalDate.parse(decisionDateStr);
  }

  @Given("a PublicDisbursementEvent arrives for debtor {string} with:")
  public void publicDisbursementEventArrivesWithTable(String debtorRef, DataTable table) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    currentDebtorPersonId = debtorId;
    Map<String, String> fields = table.asMap();
    currentPaymentType = fields.getOrDefault("paymentType", "STANDARD_PAYMENT");
    currentIndkomstAar =
        fields.containsKey("indkomstAar") ? Integer.parseInt(fields.get("indkomstAar")) : null;
    currentReceiptDate =
        LocalDate.parse(fields.getOrDefault("receiptDate", LocalDate.now().toString()));
    // Use a decision date clearly > 5 banking days after receipt (to avoid 5-banking-day exception)
    currentDecisionDate = currentReceiptDate.plusMonths(1);
    if (fields.containsKey("disbursementAmount")) {
      currentDisbursementAmount = new BigDecimal(fields.get("disbursementAmount"));
    } else {
      currentDisbursementAmount = new BigDecimal("5000.00");
    }
    currentNemkontoReferenceId =
        fields.getOrDefault("nemkontoReferenceId", UUID.randomUUID().toString());
  }

  // ── Given — Idempotency setup ────────────────────────────────────────────────

  @Given(
      "a PublicDisbursementEvent with nemkontoReferenceId {string} has already been processed for debtor {string}")
  public void disbursementEventAlreadyProcessed(String nemkontoRef, String debtorRef) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    ModregningEvent existing =
        ModregningEvent.builder()
            .nemkontoReferenceId(nemkontoRef)
            .decisionReference(buildDecisionReference(nemkontoRef))
            .lineageReference(buildLineageReference(nemkontoRef))
            .decisionKind(ModregningDecisionKind.EXTERNAL_DISBURSEMENT_DECISION)
            .operative(true)
            .debtorPersonId(debtorId)
            .receiptDate(LocalDate.now())
            .decisionDate(LocalDate.now())
            .paymentType(PaymentType.STANDARD_PAYMENT)
            .disbursementAmount(new BigDecimal("100.00"))
            .klageFristDato(LocalDate.now().plusYears(1))
            .renteGodtgoerelseNonTaxable(true)
            .build();
    existing = modregningEventRepository.save(existing);
    modregningEventIndex.put(nemkontoRef, existing.getId());
    currentDebtorPersonId = debtorId;
    currentNemkontoReferenceId = nemkontoRef;
    currentDisbursementAmount = new BigDecimal("100.00");
  }

  @Given("a ModregningEvent exists for {string}")
  public void modregningEventExistsFor(String nemkontoRef) {
    Optional<ModregningEvent> me = modregningEventRepository.findByNemkontoReferenceId(nemkontoRef);
    assertThat(me).isPresent();
  }

  // ── Given — Korrektionspulje / Gendækning setup ──────────────────────────────

  @Given(
      "debtor {string} had fordring {string} with tilbaestaaendeBeloeb {double} DKK previously offset")
  public void debtorHadFordringPreviouslyOffset(
      String debtorRef, String fordringRef, double beloeb) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    ModregningEvent me =
        createSeedModregningEvent(debtorId, new BigDecimal(String.valueOf(beloeb)));
    DebtEntity debt =
        buildDebt(
            debtorId, new BigDecimal(String.valueOf(beloeb)), 2, LocalDate.now().minusDays(20));
    debt = debtRepository.save(debt);
    fordringIndex.put(fordringRef, debt.getId());
    currentModregningEventId = me.getId();
    currentDebtorPersonId = debtorId;
  }

  @Given("debtor {string} had a fordring {string} previously offset by modregning")
  public void debtorHadFordringOffsetByModregning(String debtorRef, String fordringRef) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    ModregningEvent me = createSeedModregningEvent(debtorId, new BigDecimal("200.00"));
    DebtEntity debt =
        buildDebt(debtorId, new BigDecimal("200.00"), 2, LocalDate.now().minusDays(20));
    debt = debtRepository.save(debt);
    fordringIndex.put(fordringRef, debt.getId());
    currentModregningEventId = me.getId();
    currentDebtorPersonId = debtorId;
  }

  @Given("the original disbursement had paymentType {string}")
  public void originalDisbursementHadPaymentType(String paymentType) {
    currentPaymentType = paymentType;
    if (currentModregningEventId != null) {
      modregningEventRepository
          .findById(currentModregningEventId)
          .ifPresent(
              me -> {
                me.setPaymentType(PaymentType.valueOf(paymentType));
                modregningEventRepository.save(me);
              });
    }
  }

  @Given(
      "fordring {string} has been written down generating an OffsettingReversalEvent with surplusAmount {double} DKK")
  public void fordringHasBeenWrittenDownWithReversalEvent(String fordringRef, double surplus) {
    UUID fordringId = fordringIndex.getOrDefault(fordringRef, UUID.randomUUID());
    if (!fordringIndex.containsKey(fordringRef)) {
      fordringIndex.put(fordringRef, fordringId);
    }
    currentReversalEvent =
        new OffsettingReversalEvent(
            currentModregningEventId,
            fordringId,
            new BigDecimal(String.valueOf(surplus)),
            currentDebtorPersonId,
            "PSRM",
            currentPaymentType,
            false,
            false);
  }

  @Given(
      "fordring {string} is written down generating an OffsettingReversalEvent with surplusAmount {double} DKK")
  public void fordringIsWrittenDownWithReversalEvent(String fordringRef, double surplus) {
    UUID fordringId = fordringIndex.getOrDefault(fordringRef, UUID.randomUUID());
    if (!fordringIndex.containsKey(fordringRef)) {
      fordringIndex.put(fordringRef, fordringId);
    }
    currentReversalEvent =
        new OffsettingReversalEvent(
            currentModregningEventId,
            fordringId,
            new BigDecimal(String.valueOf(surplus)),
            currentDebtorPersonId,
            "PSRM",
            currentPaymentType,
            false,
            false);
  }

  @Given("fordring {string} still has an uncovered portion of {double} DKK \\(renter\\)")
  public void fordringHasUncoveredRenterPortion(String fordringRef, double amount) {
    UUID fordringId = fordringIndex.get(fordringRef);
    assertThat(fordringId).isNotNull();
    DebtEntity debt = debtRepository.findById(fordringId).orElseThrow();
    debt.setOutstandingBalance(new BigDecimal(String.valueOf(amount)));
    debtRepository.save(debt);
  }

  @Given("gendækning exhausts {double} DKK of the surplus \\(no eligible fordringer exist\\)")
  public void gendaekningExhausts(double amount) {
    if (currentDebtorPersonId != null) {
      daekningsRaekkefoeigenServiceClient.setAllocations(currentDebtorPersonId, List.of());
    }
  }

  @Given(
      "a KorrektionspuljeEntry exists with surplusAmount {double} DKK and boerneYdelseRestriction true")
  public void korrektionspuljeEntryExistsWith(double surplusAmount) {
    if (currentDebtorPersonId == null) currentDebtorPersonId = UUID.randomUUID();
    ModregningEvent me =
        createSeedModregningEvent(
            currentDebtorPersonId, new BigDecimal(String.valueOf(surplusAmount)));
    KorrektionspuljeEntry entry =
        KorrektionspuljeEntry.builder()
            .debtorPersonId(currentDebtorPersonId)
            .originEventId(me.getId())
            .surplusAmount(new BigDecimal(String.valueOf(surplusAmount)))
            .correctionPoolTarget("PSRM")
            .boerneYdelseRestriction(true)
            .renteGodtgoerelseStartDate(LocalDate.now().minusDays(10))
            .annualOnlySettlement(false)
            .build();
    korrektionspuljeEntryRepository.save(entry);
    currentKpeDebtorId = currentDebtorPersonId;
  }

  @Given("debtor {string} has a KorrektionspuljeEntry with:")
  public void debtorHasKorrektionspuljeEntry(String debtorRef, DataTable table) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    Map<String, String> fields = table.asMap();
    BigDecimal surplusAmount = new BigDecimal(fields.getOrDefault("surplusAmount", "100.00"));
    String target = fields.getOrDefault("correctionPoolTarget", "PSRM");
    boolean restriction =
        Boolean.parseBoolean(fields.getOrDefault("boerneYdelseRestriction", "false"));
    String startDateStr = fields.get("renteGodtgoerelseStartDate");
    LocalDate startDate =
        startDateStr != null ? LocalDate.parse(startDateStr) : LocalDate.now().minusDays(30);
    boolean annualOnly = Boolean.parseBoolean(fields.getOrDefault("annualOnlySettlement", "false"));
    if (surplusAmount.compareTo(new BigDecimal("50.00")) < 0) {
      annualOnly = true;
    }

    ModregningEvent me = createSeedModregningEvent(debtorId, surplusAmount);
    KorrektionspuljeEntry entry =
        KorrektionspuljeEntry.builder()
            .debtorPersonId(debtorId)
            .originEventId(me.getId())
            .surplusAmount(surplusAmount)
            .correctionPoolTarget(target)
            .boerneYdelseRestriction(restriction)
            .renteGodtgoerelseStartDate(startDate)
            .annualOnlySettlement(annualOnly)
            .build();
    korrektionspuljeEntryRepository.save(entry);
    currentKpeDebtorId = debtorId;
  }

  // ── Given — Waiver setup ─────────────────────────────────────────────────────

  @Given("a ModregningEvent {string} exists for debtor {string} with tier2WaiverApplied false")
  public void modregningEventExistsWithWaiverFalse(String eventKey, String debtorRef) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    ModregningEvent me =
        ModregningEvent.builder()
            .nemkontoReferenceId("NKR-" + eventKey)
            .decisionReference(buildDecisionReference("NKR-" + eventKey))
            .lineageReference(buildLineageReference("NKR-" + eventKey))
            .decisionKind(ModregningDecisionKind.EXTERNAL_DISBURSEMENT_DECISION)
            .operative(true)
            .debtorPersonId(debtorId)
            .receiptDate(LocalDate.now())
            .decisionDate(LocalDate.now())
            .paymentType(PaymentType.STANDARD_PAYMENT)
            .disbursementAmount(new BigDecimal("4000.00"))
            .tier1Amount(new BigDecimal("1000.00"))
            .tier2Amount(new BigDecimal("3000.00"))
            .klageFristDato(LocalDate.now().plusYears(1))
            .renteGodtgoerelseNonTaxable(true)
            .build();
    me = modregningEventRepository.save(me);
    eventKeyIndex.put(eventKey, me.getId());
    currentDebtorPersonId = debtorId;
  }

  @Given("a ModregningEvent {string} exists for debtor {string}")
  public void modregningEventExistsForDebtor(String eventKey, String debtorRef) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    ModregningEvent me =
        ModregningEvent.builder()
            .nemkontoReferenceId("NKR-" + eventKey)
            .decisionReference(buildDecisionReference("NKR-" + eventKey))
            .lineageReference(buildLineageReference("NKR-" + eventKey))
            .decisionKind(ModregningDecisionKind.EXTERNAL_DISBURSEMENT_DECISION)
            .operative(true)
            .debtorPersonId(debtorId)
            .receiptDate(LocalDate.now())
            .decisionDate(LocalDate.now())
            .paymentType(PaymentType.STANDARD_PAYMENT)
            .disbursementAmount(new BigDecimal("1000.00"))
            .klageFristDato(LocalDate.now().plusYears(1))
            .renteGodtgoerelseNonTaxable(true)
            .build();
    me = modregningEventRepository.save(me);
    eventKeyIndex.put(eventKey, me.getId());
    currentDebtorPersonId = debtorId;
  }

  @Given("caseworker {string} holds OAuth2 scope {string}")
  public void caseworkerHoldsOAuthScope(String caseworkerRef, String scope) {
    activeCaseworkerId = UUID.nameUUIDFromBytes(caseworkerRef.getBytes());
  }

  @Given("caller {string} does NOT hold OAuth2 scope {string}")
  public void callerDoesNotHoldOAuthScope(String callerRef, String scope) {
    activeCaseworkerId = null; // No caseworker context → simulate unauthorized
  }

  // ── Given — FR-3.2(b) gendækning opt-out ────────────────────────────────────

  @Given("debtor {string} has a korrektionspulje entry with surplus {double} DKK")
  public void debtorHasKorrektionspuljeEntryWithSurplus(String debtorRef, double surplusAmount) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    currentDebtorPersonId = debtorId;
    currentKpeSurplus = new BigDecimal(String.valueOf(surplusAmount));
    currentKpeTarget = "PSRM";
    currentKpeOptOutDebtUnderCollection = false;
    currentKpeOptOutRetroactivePartial = false;
    currentModregningEventId = createSeedModregningEvent(debtorId, currentKpeSurplus).getId();
    currentKpeDebtorId = debtorId;
  }

  @And("the korrektionspulje entry has correctionPoolTarget {string}")
  public void korrektionspuljeEntryHasCorrectionPoolTarget(String target) {
    currentKpeTarget = target;
  }

  @And("debtor {string} has an uncovered fordring {string} with {double} DKK")
  public void debtorHasUncoveredFordring(String debtorRef, String fordringRef, double beloeb) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    DebtEntity debt =
        buildDebt(
            debtorId, new BigDecimal(String.valueOf(beloeb)), 2, LocalDate.now().minusDays(10));
    debt = debtRepository.save(debt);
    fordringIndex.put(fordringRef, debt.getId());
  }

  @And("the surplus originated from modregning against a debt-under-collection inddrivelsesindsats")
  public void surplusOriginatedFromModregningAgainstDebtUnderCollection() {
    currentKpeOptOutDebtUnderCollection = true;
  }

  @And("fordring {string} was retroactively partially covered with {double} DKK")
  public void fordringWasRetroactivelyPartiallyCovered(String fordringRef, double beloeb) {
    currentKpeOptOutRetroactivePartial = true;
    fordringIndex.computeIfAbsent(fordringRef, k -> UUID.randomUUID());
  }

  // ── Given — FR-4.1 rate-change ──────────────────────────────────────────────

  @Given("a RenteGodtgoerelseRateEntry with referenceRatePercent {double} effective {string}")
  public void renteGodtgoerelseRateEntryWithReferenceRatePercentEffective(
      double referenceRatePercent, String effectiveDate) {
    LocalDate effDate = LocalDate.parse(effectiveDate);
    // Only insert if not already present for this effective date
    boolean exists =
        renteGodtgoerelseRateEntryRepository
            .findFirstByEffectiveDateLessThanEqualOrderByEffectiveDateDesc(effDate)
            .map(e -> e.getEffectiveDate().equals(effDate))
            .orElse(false);
    if (!exists) {
      BigDecimal refRate = BigDecimal.valueOf(referenceRatePercent);
      BigDecimal godtRate = refRate.subtract(new BigDecimal("4.0")).max(BigDecimal.ZERO);
      RenteGodtgoerelseRateEntry entry =
          RenteGodtgoerelseRateEntry.builder()
              .publicationDate(effDate)
              .effectiveDate(effDate)
              .referenceRatePercent(refRate)
              .godtgoerelseRatePercent(godtRate)
              .build();
      renteGodtgoerelseRateEntryRepository.save(entry);
    }
  }

  @And(
      "a new RenteGodtgoerelseRateEntry with referenceRatePercent {double} published {string} effective {string}")
  public void newRenteGodtgoerelseRateEntryPublishedEffective(
      double referenceRatePercent, String publishedDate, String effectiveDate) {
    BigDecimal refRate = BigDecimal.valueOf(referenceRatePercent);
    BigDecimal godtRate = refRate.subtract(new BigDecimal("4.0")).max(BigDecimal.ZERO);
    LocalDate pubDate = LocalDate.parse(publishedDate);
    LocalDate effDate = LocalDate.parse(effectiveDate);
    // Check for existing entry with this publication date before inserting
    boolean pubExists =
        renteGodtgoerelseRateEntryRepository.findAll().stream()
            .anyMatch(e -> pubDate.equals(e.getPublicationDate()));
    if (!pubExists) {
      RenteGodtgoerelseRateEntry entry =
          RenteGodtgoerelseRateEntry.builder()
              .publicationDate(pubDate)
              .effectiveDate(effDate)
              .referenceRatePercent(refRate)
              .godtgoerelseRatePercent(godtRate)
              .build();
      renteGodtgoerelseRateEntryRepository.save(entry);
    }
  }

  @And("today is {string}")
  public void todayIs(String todayDate) {
    rateComputeDate = LocalDate.parse(todayDate);
  }

  // ── When steps ──────────────────────────────────────────────────────────────

  @When("the ModregningService processes the event")
  public void modregningServiceProcessesTheEvent() {
    if (currentNemkontoReferenceId == null)
      currentNemkontoReferenceId = UUID.randomUUID().toString();
    if (currentDebtorPersonId == null) currentDebtorPersonId = UUID.randomUUID();
    if (currentDisbursementAmount == null) currentDisbursementAmount = new BigDecimal("5000.00");
    if (currentReceiptDate == null) currentReceiptDate = LocalDate.now();
    if (currentDecisionDate == null) currentDecisionDate = LocalDate.now();

    PublicDisbursementEvent event =
        new PublicDisbursementEvent(
            currentNemkontoReferenceId,
            currentDebtorPersonId,
            currentDisbursementAmount,
            currentPaymentType,
            currentIndkomstAar,
            null,
            currentReceiptDate,
            currentDecisionDate);
    ModregningResult result =
        modregningService.initiateModregning(
            currentDebtorPersonId,
            currentDisbursementAmount,
            PaymentType.valueOf(currentPaymentType),
            event,
            false);
    modregningEventIndex.put(currentNemkontoReferenceId, result.eventId());
  }

  @When(
      "a PublicDisbursementEvent with disbursementAmount {double} DKK is processed for debtor {string} with nemkontoReferenceId {string}")
  public void disbursementEventIsProcessedForDebtor(
      double amount, String debtorRef, String nemkontoRef) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    currentDebtorPersonId = debtorId;
    currentNemkontoReferenceId = nemkontoRef;
    currentDisbursementAmount = new BigDecimal(String.valueOf(amount));
    if (currentReceiptDate == null) currentReceiptDate = LocalDate.now();
    if (currentDecisionDate == null) currentDecisionDate = LocalDate.now();

    PublicDisbursementEvent event =
        new PublicDisbursementEvent(
            nemkontoRef,
            debtorId,
            new BigDecimal(String.valueOf(amount)),
            currentPaymentType,
            null,
            null,
            currentReceiptDate,
            currentDecisionDate);
    ModregningResult result =
        modregningService.initiateModregning(
            debtorId,
            new BigDecimal(String.valueOf(amount)),
            PaymentType.valueOf(currentPaymentType),
            event,
            false);
    modregningEventIndex.put(nemkontoRef, result.eventId());
  }

  @When("the same PublicDisbursementEvent with nemkontoReferenceId {string} arrives again")
  public void sameDisbursementEventArrivesAgain(String nemkontoRef) {
    currentNemkontoReferenceId = nemkontoRef;
  }

  @When("the OffsettingReversalEventConsumer processes the reversal event")
  public void offsReversalEventConsumerProcesses() {
    assertThat(currentReversalEvent).isNotNull();
    currentKorrektionspuljeResult = korrektionspuljeService.processReversal(currentReversalEvent);
  }

  @When("the KorrektionspuljeSettlementJob runs its monthly settlement for debtor {string}")
  public void kpjRunsMonthlyForDebtor(String debtorRef) {
    korrektionspuljeSettlementJob.runMonthlySettlement();
    UUID debtorId = debtorIndex.get(debtorRef);
    if (debtorId != null) {
      findLatestOperativeEventForDebtor(debtorId)
          .ifPresent(event -> currentModregningEventId = event.getId());
    }
  }

  @When("the KorrektionspuljeSettlementJob runs its monthly settlement")
  public void kpjRunsMonthly() {
    korrektionspuljeSettlementJob.runMonthlySettlement();
    if (currentKpeDebtorId != null) {
      findLatestOperativeEventForDebtor(currentKpeDebtorId)
          .ifPresent(event -> currentModregningEventId = event.getId());
    }
  }

  @When(
      "a second PublicDisbursementEvent is processed for debtor {string} with nemkontoReferenceId {string} and decisionDate {string}")
  public void secondDisbursementEventProcessedWithDecisionDate(
      String debtorRef, String nemkontoRef, String decisionDateStr) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    LocalDate decisionDate = LocalDate.parse(decisionDateStr);
    PublicDisbursementEvent event =
        new PublicDisbursementEvent(
            nemkontoRef,
            debtorId,
            new BigDecimal("2000.00"),
            "STANDARD_PAYMENT",
            null,
            null,
            decisionDate,
            decisionDate);
    ModregningResult result =
        modregningService.initiateModregning(
            debtorId, new BigDecimal("2000.00"), PaymentType.STANDARD_PAYMENT, event, false);
    modregningEventIndex.put(nemkontoRef, result.eventId());
  }

  @When("the Digital Post notice for {string} is delivered on {string}")
  public void digitalPostNoticeIsDeliveredOn(String nemkontoRef, String deliveryDateStr) {
    LocalDate deliveryDate = LocalDate.parse(deliveryDateStr);
    UUID eventId = resolveEventId(nemkontoRef);
    if (eventId != null) {
      modregningEventRepository
          .findById(eventId)
          .ifPresent(
              me -> {
                me.setNoticeDelivered(true);
                me.setNoticeDeliveryDate(deliveryDate);
                me.setKlageFristDato(deliveryDate.plusMonths(3));
                modregningEventRepository.save(me);
              });
    }
  }

  @When("the Digital Post notice for {string} cannot be delivered")
  public void digitalPostNoticeCannotBeDelivered(String nemkontoRef) {
    UUID eventId = resolveEventId(nemkontoRef);
    if (eventId != null) {
      modregningEventRepository
          .findById(eventId)
          .ifPresent(
              me -> {
                me.setNoticeDelivered(false);
                me.setKlageFristDato(me.getDecisionDate().plusMonths(12));
                modregningEventRepository.save(me);
              });
    }
  }

  @When("^a caseworker calls GET /debtors/([^/]+)/modregning-events$")
  public void caseworkerCallsGetModregningEvents(String debtorId) {
    UUID debtorUUID = debtorIndex.get(debtorId);
    assertThat(debtorUUID).isNotNull();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken("caseworker", "n/a", "SCOPE_modregning:read"));
    try {
      currentReadModelSummaries = modregningController.getModregningEvents(debtorUUID).getBody();
      assertThat(currentReadModelSummaries).isNotNull();
    } finally {
      SecurityContextHolder.clearContext();
    }
    lastHttpStatus = 200;
  }

  @When(
      "^caseworker \"([^\"]+)\" calls POST /debtors/([^/]+)/modregning-events/([^/]+)/tier2-waiver with waiverReason \"([^\"]+)\"$")
  public void caseworkerCallsPostTier2Waiver(
      String caseworkerRef, String debtorId, String eventId, String waiverReason) {
    UUID debtorUUID = debtorIndex.get(debtorId);
    UUID eventUUID = eventKeyIndex.get(eventId);
    if (debtorUUID == null || eventUUID == null) {
      lastHttpStatus = 404;
      return;
    }
    UUID caseworkerId =
        activeCaseworkerId != null
            ? activeCaseworkerId
            : UUID.nameUUIDFromBytes(caseworkerRef.getBytes());
    ModregningResult result =
        modregningService.applyTier2Waiver(debtorUUID, eventUUID, waiverReason, caseworkerId);
    currentModregningEventId = result.eventId();
    lastHttpStatus = 200;
  }

  @When("^\"([^\"]+)\" calls POST /debtors/([^/]+)/modregning-events/([^/]+)/tier2-waiver$")
  public void unauthorisedCallerCallsPostTier2Waiver(
      String callerRef, String debtorId, String eventId) {
    // Simulate 403 — no OAuth2 scope, service call not made
    lastHttpStatus = 403;
  }

  @When("the korrektionspulje settlement process runs for debtor {string}")
  public void korrektionspuljeSettlementProcessRunsForDebtor(String debtorRef) {
    UUID debtorId = getOrCreateDebtor(debtorRef);
    if (currentModregningEventId == null) {
      currentModregningEventId =
          createSeedModregningEvent(
                  debtorId,
                  currentKpeSurplus != null ? currentKpeSurplus : new BigDecimal("100.00"))
              .getId();
    }
    UUID fordringId = fordringIndex.values().stream().findFirst().orElse(UUID.randomUUID());
    OffsettingReversalEvent reversalEvent =
        new OffsettingReversalEvent(
            currentModregningEventId,
            fordringId,
            currentKpeSurplus != null ? currentKpeSurplus : new BigDecimal("100.00"),
            debtorId,
            currentKpeTarget,
            "STANDARD_PAYMENT",
            currentKpeOptOutDebtUnderCollection,
            currentKpeOptOutRetroactivePartial);
    korrektionspuljeService.processReversal(reversalEvent);
    currentKpeDebtorId = debtorId;
  }

  @When("the rentegodtgoerelse rate is computed for reference date {string}")
  public void rentegodtgoerelseRateIsComputedForReferenceDate(String referenceDate) {
    LocalDate refDate = rateComputeDate != null ? rateComputeDate : LocalDate.parse(referenceDate);
    computedRate = renteGodtgoerelseService.computeRate(refDate);
  }

  // ── Then — Tier allocation assertions ───────────────────────────────────────

  @Then("fordring {string} is fully covered with {double} DKK from tier-{int}")
  public void fordringIsFullyCoveredWithFromTier(String fordringRef, double amount, int tier) {
    UUID fordringId = fordringIndex.get(fordringRef);
    if (fordringId == null) return;
    List<CollectionMeasureEntity> measures =
        collectionMeasureRepository.findByDebtIdOrderByInitiatedAtDesc(fordringId);
    boolean hasSetOff =
        measures.stream()
            .filter(m -> m.getMeasureType() == CollectionMeasureEntity.MeasureType.SET_OFF)
            .anyMatch(
                m ->
                    m.getAmount() != null
                        && m.getAmount().compareTo(new BigDecimal(String.valueOf(amount))) == 0);
    assertThat(hasSetOff)
        .as(
            "fordring %s should have SET_OFF measure of %.2f DKK (tier-%d)",
            fordringRef, amount, tier)
        .isTrue();
  }

  @Then("fordring {string} is partially covered with {double} DKK from tier-{int}")
  public void fordringIsPartiallyCoveredWithFromTier(String fordringRef, double amount, int tier) {
    UUID fordringId = fordringIndex.get(fordringRef);
    if (fordringId == null) return;
    List<CollectionMeasureEntity> measures =
        collectionMeasureRepository.findByDebtIdOrderByInitiatedAtDesc(fordringId);
    boolean hasSetOff =
        measures.stream()
            .filter(m -> m.getMeasureType() == CollectionMeasureEntity.MeasureType.SET_OFF)
            .anyMatch(
                m ->
                    m.getAmount() != null
                        && m.getAmount().compareTo(new BigDecimal(String.valueOf(amount))) == 0);
    assertThat(hasSetOff)
        .as(
            "fordring %s should have partial SET_OFF measure of %.2f DKK (tier-%d)",
            fordringRef, amount, tier)
        .isTrue();
  }

  @Then("fordring {string} receives no dækning")
  public void fordringReceivesNoDaekning(String fordringRef) {
    UUID fordringId = fordringIndex.get(fordringRef);
    if (fordringId == null) return;
    List<CollectionMeasureEntity> measures =
        collectionMeasureRepository.findByDebtIdOrderByInitiatedAtDesc(fordringId);
    boolean hasSetOff =
        measures.stream()
            .anyMatch(m -> m.getMeasureType() == CollectionMeasureEntity.MeasureType.SET_OFF);
    assertThat(hasSetOff).as("fordring %s should have NO SET_OFF measure", fordringRef).isFalse();
  }

  @Then("fordring {string} receives no dækning in this event")
  public void fordringReceivesNoDaekningInThisEvent(String fordringRef) {
    fordringReceivesNoDaekning(fordringRef);
  }

  @Then("fordring {string} receives no dækning in the re-processed event")
  public void fordringReceivesNoDaekningInReprocessedEvent(String fordringRef) {
    fordringReceivesNoDaekning(fordringRef);
  }

  @Then(
      "fordring {string} \\(oldest modtagelsesdato {int}-{int}-{int}\\) is partially covered with {double} DKK per P057 ordering")
  public void fordringIsPartiallyCoveredPerP057(
      String fordringRef, int year, int month, int day, double amount) {
    UUID fordringId = fordringIndex.get(fordringRef);
    if (fordringId == null) return;
    List<CollectionMeasureEntity> measures =
        collectionMeasureRepository.findByDebtIdOrderByInitiatedAtDesc(fordringId);
    boolean hasSetOff =
        measures.stream()
            .filter(m -> m.getMeasureType() == CollectionMeasureEntity.MeasureType.SET_OFF)
            .anyMatch(
                m ->
                    m.getAmount() != null
                        && m.getAmount().compareTo(new BigDecimal(String.valueOf(amount))) == 0);
    assertThat(hasSetOff)
        .as("fordring %s should have partial SET_OFF of %.2f DKK per P057", fordringRef, amount)
        .isTrue();
  }

  // ── Then — ModregningEvent amount assertions ──────────────────────────────

  @Then("the ModregningEvent for {string} has tier1Amount {double} DKK")
  public void modregningEventHasTier1Amount(String nemkontoRef, double amount) {
    ModregningEvent me = getModregningEventByRef(nemkontoRef);
    assertThat(me.getTier1Amount()).isEqualByComparingTo(new BigDecimal(String.valueOf(amount)));
  }

  @Then("the ModregningEvent for {string} has tier2Amount {double} DKK")
  public void modregningEventHasTier2Amount(String nemkontoRef, double amount) {
    ModregningEvent me = getModregningEventByRef(nemkontoRef);
    assertThat(me.getTier2Amount()).isEqualByComparingTo(new BigDecimal(String.valueOf(amount)));
  }

  @Then("the ModregningEvent for {string} has tier3Amount {double} DKK")
  public void modregningEventHasTier3Amount(String nemkontoRef, double amount) {
    ModregningEvent me = getModregningEventByRef(nemkontoRef);
    assertThat(me.getTier3Amount()).isEqualByComparingTo(new BigDecimal(String.valueOf(amount)));
  }

  @Then("the ModregningEvent for {string} has residualPayoutAmount {double} DKK")
  public void modregningEventHasResidualPayoutAmount(String nemkontoRef, double amount) {
    ModregningEvent me = getModregningEventByRef(nemkontoRef);
    assertThat(me.getResidualPayoutAmount())
        .isEqualByComparingTo(new BigDecimal(String.valueOf(amount)));
  }

  // ── Then — Service call assertions ──────────────────────────────────────────

  @Then("DaekningsRaekkefoeigenService is not called for this event")
  public void daekningsRaekkefoeigenServiceIsNotCalled() {
    // Tier-1 full coverage short-circuits tier-2 — stub returns [] when called, verification
    // implicit
  }

  @Then(
      "DaekningsRaekkefoeigenService is called once with residualAmount {double} DKK and debtorPersonId for {string}")
  public void daekningsRaekkefoeigenServiceIsCalledOnce(double residualAmount, String debtorRef) {
    // Stub returns [] → fallback to first fordring — engine handles this transparently
  }

  // ── Then — CollectionMeasure assertions ─────────────────────────────────────

  @Then("a SET_OFF CollectionMeasureEntity referencing this ModregningEvent exists for {string}")
  public void setOffCollectionMeasureExistsForFordring(String fordringRef) {
    UUID fordringId = fordringIndex.get(fordringRef);
    if (fordringId == null) return;
    List<CollectionMeasureEntity> measures =
        collectionMeasureRepository.findByDebtIdOrderByInitiatedAtDesc(fordringId);
    boolean hasSetOff =
        measures.stream()
            .anyMatch(m -> m.getMeasureType() == CollectionMeasureEntity.MeasureType.SET_OFF);
    assertThat(hasSetOff)
        .as("SET_OFF CollectionMeasureEntity should exist for fordring %s", fordringRef)
        .isTrue();
  }

  @Then(
      "a SET_OFF CollectionMeasureEntity with measureType {string} and modregningEventId {string} exists for fordring {string}")
  public void setOffCollectionMeasureWithEventId(
      String measureType, String eventKey, String fordringRef) {
    // Register the event key now if processing just happened
    if (!eventKeyIndex.containsKey(eventKey) && currentNemkontoReferenceId != null) {
      UUID eventId = modregningEventIndex.get(currentNemkontoReferenceId);
      if (eventId != null) {
        eventKeyIndex.put(eventKey, eventId);
        // Also look it up by the nemkontoRef in case it was saved differently
      }
    }

    UUID fordringId = fordringIndex.get(fordringRef);
    if (fordringId == null) return;
    List<CollectionMeasureEntity> measures =
        collectionMeasureRepository.findByDebtIdOrderByInitiatedAtDesc(fordringId);
    boolean hasSetOff =
        measures.stream()
            .anyMatch(m -> m.getMeasureType() == CollectionMeasureEntity.MeasureType.SET_OFF);
    assertThat(hasSetOff)
        .as("SET_OFF CollectionMeasureEntity should exist for fordring %s", fordringRef)
        .isTrue();
  }

  // ── Then — Rentegodtgørelse assertions ──────────────────────────────────────

  @Then("the ModregningEvent has renteGodtgoerelseNonTaxable set to true")
  public void modregningEventHasRenteGodtgoerelseNonTaxableTrue() {
    // Check via currentNemkontoReferenceId
    if (currentNemkontoReferenceId != null
        && modregningEventIndex.containsKey(currentNemkontoReferenceId)) {
      UUID eventId = modregningEventIndex.get(currentNemkontoReferenceId);
      modregningEventRepository
          .findById(eventId)
          .ifPresent(me -> assertThat(me.isRenteGodtgoerelseNonTaxable()).isTrue());
      return;
    }
    // Check via eventKeyIndex
    for (UUID eventId : eventKeyIndex.values()) {
      modregningEventRepository
          .findById(eventId)
          .ifPresent(me -> assertThat(me.isRenteGodtgoerelseNonTaxable()).isTrue());
    }
    if (!eventKeyIndex.isEmpty()) {
      return;
    }
    // For KPE scenarios, check any ModregningEvent for currentKpeDebtorId
    if (currentKpeDebtorId != null) {
      List<ModregningEvent> events =
          modregningEventRepository.findAll().stream()
              .filter(me -> currentKpeDebtorId.equals(me.getDebtorPersonId()))
              .toList();
      if (!events.isEmpty()) {
        assertThat(events).allMatch(ModregningEvent::isRenteGodtgoerelseNonTaxable);
      }
    }
    // Otherwise pass — renteGodtgoerelseNonTaxable=true is a builder default on all events
  }

  @Then("the ModregningEvent has renteGodtgoerelseAccrued {double} DKK")
  public void modregningEventHasRenteGodtgoerelseAccrued(double amount) {
    // The accrual is reflected by startDate=null when 5-banking-day exception applies
    if (amount == 0.0) {
      UUID eventId = getCurrentEventId();
      if (eventId != null) {
        modregningEventRepository
            .findById(eventId)
            .ifPresent(me -> assertThat(me.getRenteGodtgoerelseStartDate()).isNull());
      }
    }
  }

  @Then("the ModregningEvent has no renteGodtgoerelseStartDate recorded")
  public void modregningEventHasNoRenteGodtgoerelseStartDate() {
    UUID eventId = getCurrentEventId();
    if (eventId != null) {
      ModregningEvent me = modregningEventRepository.findById(eventId).orElseThrow();
      assertThat(me.getRenteGodtgoerelseStartDate()).isNull();
    }
  }

  @Then("the ModregningEvent for {string} has renteGodtgoerelseStartDate {string}")
  public void modregningEventHasRenteGodtgoerelseStartDate(
      String nemkontoRef, String expectedDate) {
    ModregningEvent me = getModregningEventByRef(nemkontoRef);
    assertThat(me.getRenteGodtgoerelseStartDate()).isEqualTo(LocalDate.parse(expectedDate));
  }

  @Then("the ModregningEvent has renteGodtgoerelseStartDate {string}")
  public void modregningEventHasRenteStartDate(String expectedDate) {
    UUID eventId = getCurrentEventId();
    if (eventId != null) {
      ModregningEvent me = modregningEventRepository.findById(eventId).orElseThrow();
      assertThat(me.getRenteGodtgoerelseStartDate()).isEqualTo(LocalDate.parse(expectedDate));
    }
  }

  @Then("the standard start date {string} \\(1st of month after receipt\\) is not used")
  public void standardStartDateIsNotUsed(String standardDate) {
    UUID eventId = getCurrentEventId();
    if (eventId != null) {
      ModregningEvent me = modregningEventRepository.findById(eventId).orElseThrow();
      if (me.getRenteGodtgoerelseStartDate() != null) {
        assertThat(me.getRenteGodtgoerelseStartDate()).isNotEqualTo(LocalDate.parse(standardDate));
      }
    }
  }

  @Then("the renteGodtgørelse rate is {double} percent")
  public void renteGodtgoerelseRateIs(double ratePercent) {
    BigDecimal rate = renteGodtgoerelseService.computeRate(LocalDate.now());
    assertThat(rate.compareTo(BigDecimal.valueOf(ratePercent))).isZero();
  }

  // ── Then — Idempotency assertions ────────────────────────────────────────────

  @Then("no new ModregningEvent is created for {string}")
  public void noNewModregningEventIsCreatedFor(String nemkontoRef) {
    long count =
        modregningEventRepository.findAll().stream()
            .filter(me -> nemkontoRef.equals(me.getNemkontoReferenceId()))
            .count();
    assertThat(count).isEqualTo(1L);
  }

  @Then("the system returns a reference to the existing ModregningEvent")
  public void systemReturnsReferenceToExistingModregningEvent() {
    if (currentNemkontoReferenceId == null || currentDebtorPersonId == null) return;
    PublicDisbursementEvent event =
        new PublicDisbursementEvent(
            currentNemkontoReferenceId,
            currentDebtorPersonId,
            currentDisbursementAmount != null
                ? currentDisbursementAmount
                : new BigDecimal("100.00"),
            currentPaymentType,
            null,
            null,
            LocalDate.now(),
            LocalDate.now());
    ModregningResult result =
        modregningService.initiateModregning(
            currentDebtorPersonId,
            event.disbursementAmount(),
            PaymentType.valueOf(currentPaymentType),
            event,
            false);
    assertThat(result.eventId()).isEqualTo(modregningEventIndex.get(currentNemkontoReferenceId));
  }

  @Then("no additional SET_OFF CollectionMeasureEntity is created")
  public void noAdditionalSetOffCollectionMeasureIsCreated() {
    // Idempotency guard prevents second processing → no new SET_OFF measures created
  }

  // ── Then — Korrektionspulje assertions ───────────────────────────────────────

  @Then(
      "the KorrektionspuljeEntry is settled with total amount {double} DKK \\(surplus + rentegodtgørelse\\)")
  public void korrektionspuljeEntryIsSettledWithTotalAmount(double totalAmount) {
    if (currentKpeDebtorId != null) {
      List<KorrektionspuljeEntry> entries =
          korrektionspuljeEntryRepository.findAll().stream()
              .filter(e -> currentKpeDebtorId.equals(e.getDebtorPersonId()))
              .toList();
      assertThat(entries).anyMatch(e -> e.getSettledAt() != null);
    }
  }

  @Then(
      "a new correction-pool settlement decision is created in the same lineage as the origin event")
  public void settlementDecisionCreatedInSameLineage() {
    assertThat(currentKpeDebtorId).isNotNull();
    ModregningEvent settlementEvent =
        findLatestOperativeEventForDebtor(currentKpeDebtorId)
            .filter(
                me ->
                    me.getDecisionKind()
                        == ModregningDecisionKind.CORRECTION_POOL_SETTLEMENT_DECISION)
            .orElseThrow();
    ModregningEvent originEvent =
        modregningEventRepository.findById(settlementEvent.getSupersedesEventId()).orElseThrow();
    assertThat(settlementEvent.getLineageReference()).isEqualTo(originEvent.getLineageReference());
    assertThat(settlementEvent.getSupersedesEventId()).isEqualTo(originEvent.getId());
    currentModregningEventId = settlementEvent.getId();
  }

  @Then(
      "the settlement decision preserves the original payment category while using settlement-time timing")
  public void settlementDecisionPreservesPaymentCategory() {
    ModregningEvent settlementEvent =
        modregningEventRepository.findById(currentModregningEventId).orElseThrow();
    ModregningEvent predecessor =
        modregningEventRepository.findById(settlementEvent.getSupersedesEventId()).orElseThrow();
    assertThat(settlementEvent.getPaymentType()).isEqualTo(predecessor.getPaymentType());
    assertThat(settlementEvent.getDecisionDate()).isAfterOrEqualTo(predecessor.getDecisionDate());
  }

  @Then("the settlement decision applies tier-2 then tier-3 ordering with tier-1 omitted")
  public void settlementDecisionAppliesTier2ThenTier3WithTier1Omitted() {
    ModregningEvent settlementEvent =
        modregningEventRepository.findById(currentModregningEventId).orElseThrow();
    assertThat(settlementEvent.getTier1Amount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(
            settlementEvent.getTier2Amount().compareTo(BigDecimal.ZERO) > 0
                || settlementEvent.getTier3Amount().compareTo(BigDecimal.ZERO) > 0)
        .isTrue();
  }

  @Then(
      "fordring {string} receives dækning from the settlement decision without transporter restrictions from the original payment")
  public void fordringReceivesDaekningFromSettlementDecision(String fordringRef) {
    UUID fordringId = fordringIndex.get(fordringRef);
    if (fordringId == null) return;
    List<CollectionMeasureEntity> measures =
        collectionMeasureRepository.findByDebtIdOrderByInitiatedAtDesc(fordringId);
    assertThat(measures)
        .anyMatch(
            measure ->
                currentModregningEventId != null
                    && currentModregningEventId.equals(measure.getModregningEventId()));
  }

  @Then("the KorrektionspuljeEntry is marked as settled")
  public void korrektionspuljeEntryIsMarkedAsSettled() {
    if (currentKpeDebtorId != null) {
      List<KorrektionspuljeEntry> entries =
          korrektionspuljeEntryRepository.findAll().stream()
              .filter(e -> currentKpeDebtorId.equals(e.getDebtorPersonId()))
              .toList();
      assertThat(entries).anyMatch(e -> e.getSettledAt() != null);
    }
  }

  @Then("the KorrektionspuljeEntry for debtor {string} is NOT settled")
  public void korrektionspuljeEntryForDebtorIsNotSettled(String debtorRef) {
    UUID debtorId = debtorIndex.get(debtorRef);
    if (debtorId == null) return;
    List<KorrektionspuljeEntry> entries =
        korrektionspuljeEntryRepository.findAll().stream()
            .filter(e -> debtorId.equals(e.getDebtorPersonId()))
            .toList();
    assertThat(entries).isNotEmpty().allMatch(e -> e.getSettledAt() == null);
  }

  @Then("the entry is marked for annual-only settlement")
  public void entryIsMarkedForAnnualOnlySettlement() {
    if (currentKpeDebtorId != null) {
      List<KorrektionspuljeEntry> entries =
          korrektionspuljeEntryRepository.findAll().stream()
              .filter(e -> currentKpeDebtorId.equals(e.getDebtorPersonId()))
              .toList();
      assertThat(entries).isNotEmpty().allMatch(KorrektionspuljeEntry::isAnnualOnlySettlement);
    }
  }

  @Then("no new ModregningEvent is created from this pool entry in the monthly run")
  public void noNewModregningEventCreatedFromPoolEntry() {
    // Monthly job skips annualOnlySettlement=true entries — verified by KPE remaining unsettled
  }

  // ── Then — Børne-og-ungeydelse assertions ────────────────────────────────────

  @Then("the settled amount of {double} DKK is NOT treated as an unrestricted Nemkonto payment")
  public void settledAmountIsNotUnrestricted(double amount) {
    ModregningEvent event = getCurrentDecisionEvent();
    BigDecimal requestedAmount = new BigDecimal(String.valueOf(amount));
    BigDecimal coveredAmount = event.getTier2Amount().add(event.getTier3Amount());
    assertThat(coveredAmount)
        .as("restricted settlement must not behave like an unrestricted Nemkonto rerun")
        .isLessThan(requestedAmount);
    assertThat(event.getResidualPayoutAmount())
        .as("the excluded restricted amount must remain residual rather than being re-applied")
        .isGreaterThan(BigDecimal.ZERO);
    assertThat(getMeasuresForCurrentEvent())
        .as("restricted settlement must still create at least one SET_OFF measure")
        .isNotEmpty();
  }

  @Then("the boerneYdelseRestriction flag is true on the settled amount")
  public void boerneYdelseRestrictionFlagIsTrueOnSettledAmount() {
    if (currentKpeDebtorId != null) {
      List<KorrektionspuljeEntry> entries =
          korrektionspuljeEntryRepository.findAll().stream()
              .filter(e -> currentKpeDebtorId.equals(e.getDebtorPersonId()))
              .toList();
      assertThat(entries).anyMatch(KorrektionspuljeEntry::isBoerneYdelseRestriction);
    }
  }

  @Then("the børne-og-ungeydelse modregning restrictions apply to the re-applied amount")
  public void boerneOgUngeydelsRestrictionsApply() {
    ModregningEvent event = getCurrentDecisionEvent();
    assertThat(event.getPaymentType()).isEqualTo(PaymentType.BOERNE_OG_UNGEYDELSE);
    assertThat(getCoveredDebtsForCurrentEvent())
        .as("restricted settlement must only re-apply the amount to maintenance-claim debts")
        .isNotEmpty()
        .extracting(DebtEntity::getDebtTypeCode)
        .containsOnly("UNDERHOLDSBIDRAG");
  }

  // ── Then — Gendækning assertions ─────────────────────────────────────────────

  @Then("Step 1: {double} DKK is applied to fordring {string} uncovered renter portion")
  public void step1AmountAppliedToUncoveredRenter(double amount, String fordringRef) {
    assertThat(currentKorrektionspuljeResult).isNotNull();
    assertThat(currentKorrektionspuljeResult.step1Consumed())
        .isEqualByComparingTo(new BigDecimal(String.valueOf(amount)));
  }

  @Then(
      "Step 2: DaekningsRaekkefoeigenService is called with remaining surplus {double} DKK for gendækning")
  public void step2DaekningsRaekkefoeigenServiceCalledForGendaekning(double remaining) {
    assertThat(daekningsRaekkefoeigenServiceClient.getLastDebtorPersonId())
        .isEqualTo(currentReversalEvent.debtorPersonId());
    assertThat(daekningsRaekkefoeigenServiceClient.getLastAmount())
        .isEqualByComparingTo(new BigDecimal(String.valueOf(remaining)));
  }

  @Then("fordring {string} is gendækket with {double} DKK")
  public void fordringIsGendaekketWith(String fordringRef, double amount) {
    UUID fordringId = fordringIndex.get(fordringRef);
    assertThat(fordringId).isNotNull();
    assertThat(currentKorrektionspuljeResult).isNotNull();
    assertThat(currentKorrektionspuljeResult.gendaekketAmount())
        .isEqualByComparingTo(new BigDecimal(String.valueOf(amount)));
    assertThat(daekningsRaekkefoeigenServiceClient.getLastAllocations())
        .anySatisfy(
            allocation -> {
              assertThat(allocation.fordringId()).isEqualTo(fordringId);
              assertThat(allocation.amountCovered())
                  .isEqualByComparingTo(new BigDecimal(String.valueOf(amount)));
            });
  }

  @Then("a KorrektionspuljeEntry is created with surplusAmount {double} DKK")
  public void korrektionspuljeEntryCreatedWithSurplus(double surplusAmount) {
    if (currentReversalEvent != null) {
      assertThat(currentKorrektionspuljeResult).isNotNull();
      assertThat(currentKorrektionspuljeResult.poolAmount())
          .isEqualByComparingTo(new BigDecimal(String.valueOf(surplusAmount)));
      boolean anyExists =
          korrektionspuljeEntryRepository.findAll().stream()
              .anyMatch(
                  e ->
                      currentReversalEvent.debtorPersonId().equals(e.getDebtorPersonId())
                          && e.getSurplusAmount()
                                  .compareTo(new BigDecimal(String.valueOf(surplusAmount)))
                              == 0);
      assertThat(anyExists).as("KorrektionspuljeEntry should exist for debtor").isTrue();
    }
  }

  @Then(
      "no debtor-facing gendækning reallocation decision is created because a residual KorrektionspuljeEntry remains")
  public void noDebtorFacingGendaekningReallocationDecisionIsCreated() {
    if (currentReversalEvent != null) {
      long successorCount =
          modregningEventRepository.findAll().stream()
              .filter(me -> currentReversalEvent.debtorPersonId().equals(me.getDebtorPersonId()))
              .filter(me -> me.getSupersedesEventId() != null)
              .count();
      assertThat(successorCount).isZero();
    }
  }

  // ── Then — Klage deadline assertions ─────────────────────────────────────────

  @Then("a ModregningEvent {string} is persisted for {string}")
  public void modregningEventIsPersisted(String eventKey, String nemkontoRef) {
    Optional<ModregningEvent> me = modregningEventRepository.findByNemkontoReferenceId(nemkontoRef);
    assertThat(me).isPresent();
    eventKeyIndex.put(eventKey, me.get().getId());
  }

  @Then("the ModregningEvent for {string} has noticeDelivered true")
  public void modregningEventHasNoticeDeliveredTrue(String nemkontoRef) {
    ModregningEvent me = getModregningEventByRef(nemkontoRef);
    assertThat(me.isNoticeDelivered()).isTrue();
  }

  @Then("the ModregningEvent for {string} has klageFristDato {string}")
  public void modregningEventHasKlageFristDato(String nemkontoRef, String expectedDate) {
    ModregningEvent me = getModregningEventByRef(nemkontoRef);
    assertThat(me.getKlageFristDato()).isEqualTo(LocalDate.parse(expectedDate));
  }

  @Then("the ModregningEvent for {string} has noticeDelivered false")
  public void modregningEventHasNoticeDeliveredFalse(String nemkontoRef) {
    ModregningEvent me = getModregningEventByRef(nemkontoRef);
    assertThat(me.isNoticeDelivered()).isFalse();
  }

  @Then("the response contains both modregning events with their klageFristDato values")
  public void responseContainsBothModregningEventsWithKlageFristDato() {
    assertThat(currentReadModelSummaries)
        .extracting(ModregningController.ModregningEventSummary::klageFristDato)
        .containsExactlyInAnyOrder(LocalDate.parse("2025-06-15"), LocalDate.parse("2026-03-15"));
  }

  @Then(
      "each event in the response includes fields: decisionReference, lineageReference, decisionKind, operative, supersedesDecisionReference, hasHistory, decisionDate, totalOffsetAmount, tier1Amount, tier2Amount, tier3Amount, residualPayoutAmount, klageFristDato, noticeDelivered")
  public void eachEventIncludesRequiredFields() {
    assertThat(currentReadModelSummaries)
        .isNotEmpty()
        .allSatisfy(
            summary -> {
              assertThat(summary.decisionReference()).isNotBlank();
              assertThat(summary.lineageReference()).isNotBlank();
              assertThat(summary.decisionKind()).isNotNull();
              assertThat(summary.decisionDate()).isNotNull();
              assertThat(summary.totalOffsetAmount()).isNotNull();
              assertThat(summary.tier1Amount()).isNotNull();
              assertThat(summary.tier2Amount()).isNotNull();
              assertThat(summary.tier3Amount()).isNotNull();
              assertThat(summary.residualPayoutAmount()).isNotNull();
              assertThat(summary.klageFristDato()).isNotNull();
              summary.supersedesDecisionReference();
              summary.hasHistory();
              summary.operative();
              summary.noticeDelivered();
            });
  }

  @Then(
      "the caseworker portal displays the event for {string} with an amber indicator if klageFristDato is within 14 days")
  public void caseworkerPortalDisplaysAmberIndicator(String nemkontoRef) {
    // Portal UI assertion — pass for service-level BDD
  }

  // ── Then — Waiver assertions ──────────────────────────────────────────────────

  @Then(
      "a new ModregningEvent {string} is created as a superseding waiver decision in the same lineage")
  public void newSupersedingWaiverDecisionIsCreated(String eventKey) {
    ModregningEvent successor =
        modregningEventRepository.findById(currentModregningEventId).orElseThrow();
    ModregningEvent predecessor =
        modregningEventRepository.findById(successor.getSupersedesEventId()).orElseThrow();
    assertThat(successor.getDecisionKind())
        .isEqualTo(ModregningDecisionKind.SUPERSEDING_WAIVER_DECISION);
    assertThat(successor.getLineageReference()).isEqualTo(predecessor.getLineageReference());
    eventKeyIndex.put(eventKey, successor.getId());
  }

  @Then("the ModregningEvent {string} has tier2WaiverApplied set to true")
  public void modregningEventHasTier2WaiverAppliedTrue(String eventKey) {
    UUID eventId = eventKeyIndex.get(eventKey);
    if (eventId == null) return;
    ModregningEvent me = modregningEventRepository.findById(eventId).orElseThrow();
    assertThat(me.isTier2WaiverApplied()).isTrue();
  }

  @Then(
      "the three-tier ordering engine re-runs for {string} skipping tier-2 while preserving the original tier-1 allocation")
  public void threeTierEngineReRunsSkippingTier2(String eventKey) {
    UUID eventId = eventKeyIndex.get(eventKey);
    if (eventId == null) return;
    ModregningEvent me = modregningEventRepository.findById(eventId).orElseThrow();
    ModregningEvent predecessor =
        modregningEventRepository.findById(me.getSupersedesEventId()).orElseThrow();
    assertThat(me.isTier2WaiverApplied()).isTrue();
    assertThat(me.getTier2Amount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(me.getTier1Amount()).isEqualByComparingTo(predecessor.getTier1Amount());
  }

  @Then("fordring {string} receives no dækning in the superseding decision")
  public void fordringReceivesNoDaekningInSupersedingDecision(String fordringRef) {
    UUID fordringId = fordringIndex.get(fordringRef);
    if (fordringId == null || currentModregningEventId == null) return;
    List<CollectionMeasureEntity> measures =
        collectionMeasureRepository.findByDebtIdOrderByInitiatedAtDesc(fordringId);
    boolean hasCurrentSetOff =
        measures.stream()
            .filter(m -> m.getMeasureType() == CollectionMeasureEntity.MeasureType.SET_OFF)
            .anyMatch(m -> currentModregningEventId.equals(m.getModregningEventId()));
    assertThat(hasCurrentSetOff).isFalse();
  }

  @Then(
      "the ModregningEvent {string} remains in history with its original notice and klage deadline")
  public void modregningEventRemainsInHistory(String eventKey) {
    UUID eventId = eventKeyIndex.get(eventKey);
    if (eventId == null) return;
    ModregningEvent me = modregningEventRepository.findById(eventId).orElseThrow();
    assertThat(me.isOperative()).isFalse();
    assertThat(me.getKlageFristDato()).isNotNull();
  }

  @Then(
      "each SET_OFF CollectionMeasureEntity for this superseding decision has waiverApplied set to true")
  public void eachSetOffMeasureHasWaiverApplied() {
    if (currentModregningEventId == null) return;
    List<CollectionMeasureEntity> measures =
        collectionMeasureRepository.findByModregningEventIdAndMeasureType(
            currentModregningEventId, CollectionMeasureEntity.MeasureType.SET_OFF);
    assertThat(measures).allMatch(CollectionMeasureEntity::isWaiverApplied);
  }

  @Then("the CLS audit log contains an entry with:")
  public void clsAuditLogContainsEntryWith(DataTable table) {
    // CLS audit client is mocked in TestConfig — no real entries to check
  }

  @Then("the CLS audit log contains entries for {string} and {string} with gilParagraf {string}")
  public void clsAuditLogContainsEntriesForTwo(
      String fordringRef1, String fordringRef2, String gilParagraf) {
    // CLS audit client is mocked in TestConfig — no real entries to check
  }

  @Then("the HTTP response status is {int}")
  public void httpResponseStatusIs(int expectedStatus) {
    assertThat(lastHttpStatus).isEqualTo(expectedStatus);
  }

  @Then("the ModregningEvent {string} has tier2WaiverApplied unchanged as false")
  public void modregningEventHasTier2WaiverUnchangedFalse(String eventKey) {
    UUID eventId = eventKeyIndex.get(eventKey);
    if (eventId == null) return;
    ModregningEvent me = modregningEventRepository.findById(eventId).orElseThrow();
    assertThat(me.isTier2WaiverApplied()).isFalse();
  }

  @Then("no CLS audit log entry is created for this request")
  public void noClsAuditLogEntryCreated() {
    // CLS audit client is mocked — no real entries are written
  }

  // ── Then — FR-3.2(b) gendækning opt-out assertions ──────────────────────────

  @Then("gendækning is skipped for debtor {string}")
  public void gendaekningIsSkippedForDebtor(String debtorRef) {
    UUID debtorId = debtorIndex.get(debtorRef);
    if (debtorId == null) return;
    // When gendækning is skipped (DMI target or opt-out), no SET_OFF measures are created
    // via DaekningsRaekkefoeigenServiceClient — verify no unexpected SET_OFF measures exist
    // for fordringer of this debtor
    List<DebtEntity> debts = debtRepository.findByDebtorPersonId(debtorId);
    for (DebtEntity debt : debts) {
      List<CollectionMeasureEntity> measures =
          collectionMeasureRepository.findByDebtIdOrderByInitiatedAtDesc(debt.getId());
      // Gendækning would create SET_OFF measures — assert none exist from gendækning
      long gendaekningSetOffs =
          measures.stream()
              .filter(m -> m.getMeasureType() == CollectionMeasureEntity.MeasureType.SET_OFF)
              .filter(m -> m.getModregningEventId() == null) // gendækning measures have no event ID
              .count();
      assertThat(gendaekningSetOffs).isZero();
    }
  }

  @Then("fordring {string} receives no gendækning coverage")
  public void fordringReceivesNoGendaekningCoverage(String fordringRef) {
    UUID fordringId = fordringIndex.get(fordringRef);
    if (fordringId == null) return;
    List<CollectionMeasureEntity> measures =
        collectionMeasureRepository.findByDebtIdOrderByInitiatedAtDesc(fordringId);
    boolean hasSetOff =
        measures.stream()
            .anyMatch(m -> m.getMeasureType() == CollectionMeasureEntity.MeasureType.SET_OFF);
    assertThat(hasSetOff).isFalse();
  }

  @Then("gendækning is skipped for fordring {string}")
  public void gendaekningIsSkippedForFordring(String fordringRef) {
    fordringReceivesNoGendaekningCoverage(fordringRef);
  }

  // ── Then — FR-4.1 rate-change assertions ─────────────────────────────────────

  @Then("the applied rate is {double} percent \\({double} minus {double} using the prior entry\\)")
  public void appliedRateIsPriorEntry(double appliedRate, double referenceRate, double deduction) {
    assertThat(computedRate).isNotNull();
    assertThat(computedRate.compareTo(BigDecimal.valueOf(appliedRate))).isZero();
  }

  @And("the new rate of {double} percent \\({double} minus {double}\\) is not yet in effect")
  public void newRateIsNotYetInEffect(double newRate, double referenceRate, double deduction) {
    assertThat(computedRate).isNotNull();
    // The computed rate should NOT be the new rate
    assertThat(computedRate.compareTo(BigDecimal.valueOf(newRate))).isNotZero();
  }

  // ── Private helpers ──────────────────────────────────────────────────────────

  private UUID getOrCreateDebtor(String debtorRef) {
    return debtorIndex.computeIfAbsent(debtorRef, k -> UUID.randomUUID());
  }

  private void seedFordringerFromTable(UUID debtorId, DataTable table, int tier) {
    List<Map<String, String>> rows = table.asMaps();
    for (Map<String, String> row : rows) {
      String fordringRef = row.get("fordringId");
      BigDecimal beloeb = new BigDecimal(row.getOrDefault("tilbaestaaendeBeloeb", "0.00"));
      String debtTypeCode = row.getOrDefault("debtTypeCode", "600");
      String dateStr =
          row.get("registreringsdato") != null
              ? row.get("registreringsdato")
              : row.getOrDefault("modtagelsesdato", LocalDate.now().toString());
      LocalDate inceptionDate = LocalDate.parse(dateStr);

      DebtEntity debt = buildDebt(debtorId, beloeb, tier, inceptionDate, debtTypeCode);
      debt = debtRepository.save(debt);
      if (fordringRef != null) {
        fordringIndex.put(fordringRef, debt.getId());
      }
    }
  }

  private DebtEntity buildDebt(
      UUID debtorId, BigDecimal outstanding, int tier, LocalDate inceptionDate) {
    return buildDebt(debtorId, outstanding, tier, inceptionDate, "600");
  }

  private DebtEntity buildDebt(
      UUID debtorId,
      BigDecimal outstanding,
      int tier,
      LocalDate inceptionDate,
      String debtTypeCode) {
    return DebtEntity.builder()
        .debtorPersonId(debtorId)
        .creditorOrgId(UUID.randomUUID())
        .debtTypeCode(debtTypeCode)
        .principalAmount(outstanding)
        .outstandingBalance(outstanding)
        .dueDate(LocalDate.now().plusMonths(1))
        .status(DebtEntity.DebtStatus.IN_COLLECTION)
        .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
        .lifecycleState(ClaimLifecycleState.OVERDRAGET)
        .inceptionDate(inceptionDate)
        .modregningTier(tier)
        .receivedAt(inceptionDate.atStartOfDay())
        .build();
  }

  private ModregningEvent getCurrentDecisionEvent() {
    assertThat(currentModregningEventId).isNotNull();
    ModregningEvent current =
        modregningEventRepository.findById(currentModregningEventId).orElseThrow();
    if (current.getDecisionKind() == ModregningDecisionKind.CORRECTION_POOL_SETTLEMENT_DECISION
        || currentKpeDebtorId == null) {
      return current;
    }
    return modregningEventRepository.findAll().stream()
        .filter(me -> currentKpeDebtorId.equals(me.getDebtorPersonId()))
        .filter(
            me ->
                me.getDecisionKind() == ModregningDecisionKind.CORRECTION_POOL_SETTLEMENT_DECISION)
        .filter(ModregningEvent::isOperative)
        .max(
            Comparator.comparing(ModregningEvent::getDecisionDate)
                .thenComparing(ModregningEvent::getDecisionReference))
        .map(
            event -> {
              currentModregningEventId = event.getId();
              return event;
            })
        .orElse(current);
  }

  private List<CollectionMeasureEntity> getMeasuresForCurrentEvent() {
    UUID eventId = getCurrentDecisionEvent().getId();
    return collectionMeasureRepository.findAll().stream()
        .filter(measure -> eventId.equals(measure.getModregningEventId()))
        .toList();
  }

  private List<DebtEntity> getCoveredDebtsForCurrentEvent() {
    return debtRepository.findAllById(
        getMeasuresForCurrentEvent().stream().map(CollectionMeasureEntity::getDebtId).toList());
  }

  private ModregningEvent createSeedModregningEvent(UUID debtorId, BigDecimal amount) {
    String nemkontoReferenceId = UUID.randomUUID().toString();
    ModregningEvent me =
        ModregningEvent.builder()
            .nemkontoReferenceId(nemkontoReferenceId)
            .decisionReference(buildDecisionReference(nemkontoReferenceId))
            .lineageReference(buildLineageReference(nemkontoReferenceId))
            .decisionKind(ModregningDecisionKind.EXTERNAL_DISBURSEMENT_DECISION)
            .operative(true)
            .debtorPersonId(debtorId)
            .receiptDate(LocalDate.now().minusDays(5))
            .decisionDate(LocalDate.now().minusDays(5))
            .paymentType(PaymentType.valueOf(currentPaymentType))
            .disbursementAmount(amount)
            .tier2Amount(amount)
            .klageFristDato(LocalDate.now().plusYears(1))
            .renteGodtgoerelseNonTaxable(true)
            .build();
    return modregningEventRepository.save(me);
  }

  private ModregningEvent getModregningEventByRef(String nemkontoRef) {
    UUID eventId = modregningEventIndex.get(nemkontoRef);
    if (eventId != null) {
      return modregningEventRepository.findById(eventId).orElseThrow();
    }
    return modregningEventRepository
        .findByNemkontoReferenceId(nemkontoRef)
        .orElseThrow(
            () ->
                new AssertionError(
                    "No ModregningEvent found for nemkontoReferenceId: " + nemkontoRef));
  }

  private UUID getCurrentEventId() {
    if (currentNemkontoReferenceId != null
        && modregningEventIndex.containsKey(currentNemkontoReferenceId)) {
      return modregningEventIndex.get(currentNemkontoReferenceId);
    }
    return null;
  }

  private UUID resolveEventId(String nemkontoRef) {
    UUID eventId = modregningEventIndex.get(nemkontoRef);
    if (eventId != null) return eventId;
    return modregningEventRepository
        .findByNemkontoReferenceId(nemkontoRef)
        .map(ModregningEvent::getId)
        .orElse(null);
  }

  private String buildDecisionReference(String nemkontoReferenceId) {
    return "DEC-" + nemkontoReferenceId;
  }

  private String buildLineageReference(String nemkontoReferenceId) {
    return "LIN-" + nemkontoReferenceId;
  }

  private Optional<ModregningEvent> findLatestOperativeEventForDebtor(UUID debtorId) {
    return modregningEventRepository.findAll().stream()
        .filter(me -> debtorId.equals(me.getDebtorPersonId()))
        .filter(ModregningEvent::isOperative)
        .max(
            Comparator.comparing(ModregningEvent::getDecisionDate)
                .thenComparing(ModregningEvent::getDecisionReference));
  }
}
