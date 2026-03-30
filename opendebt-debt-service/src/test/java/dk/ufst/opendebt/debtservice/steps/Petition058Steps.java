package dk.ufst.opendebt.debtservice.steps;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Failing BDD step definitions for petition 058 — Modregning i udbetalinger fra det offentlige og
 * korrektionspulje (G.A.2.3.3–2.3.4).
 *
 * <p>Every step in this file throws {@link PendingException} — these are intentionally RED tests
 * that must remain failing until the full implementation is delivered.
 *
 * <p>Covered ACs: AC-1, AC-2, AC-3, AC-4, AC-5, AC-6, AC-7, AC-8, AC-9, AC-10, AC-11, AC-12, AC-13,
 * AC-14, AC-15, AC-16, AC-17
 *
 * <p>Mandatory NFR coverage (via BDD scenarios):
 *
 * <ul>
 *   <li>NFR-2 (Auditability): CLS audit log contains gilParagraf + modregningEventId (AC-4, AC-6)
 *   <li>NFR-3 (GDPR): debtorPersonId (UUID) is used, never CPR (Background step)
 *   <li>AC-14: renteGodtgoerelseNonTaxable = true on every ModregningEvent
 * </ul>
 *
 * <p>Spec reference: SPEC-058, petition058-modregning-korrektionspulje-specs.md
 *
 * <p>NOTE: This class does NOT carry {@code @CucumberContextConfiguration} — that annotation lives
 * on {@code Petition002Steps}. Spring context is shared across all step definition classes in the
 * {@code dk.ufst.opendebt.debtservice.steps} glue package.
 */
public class Petition058Steps {

  // ── Per-scenario state ──────────────────────────────────────────────────────

  /** Maps external debtor references (e.g. "SKY-5801") to their internal UUID. */
  private final Map<String, UUID> debtorIndex = new HashMap<>();

  /** Maps external fordring references (e.g. "FDR-58011") to their internal UUID. */
  private final Map<String, UUID> fordringIndex = new HashMap<>();

  /** Maps nemkontoReferenceId to the resulting ModregningEvent UUID once processed. */
  private final Map<String, UUID> modregningEventIndex = new HashMap<>();

  /** Maps ModregningEvent external keys (e.g. "EVT-5814-001") to their UUID. */
  private final Map<String, UUID> eventKeyIndex = new HashMap<>();

  /** HTTP status captured from the last controller invocation. */
  private int lastHttpStatus;

  /** Caseworker ID for the current scenario (if applicable). */
  private UUID activeCaseworkerId;

  /** Whether the last call produced a waiver outcome. */
  private boolean waiverOutcome;

  // ── Lifecycle ───────────────────────────────────────────────────────────────

  /**
   * Reset per-scenario state before each P058 scenario. The {@code @petition058} tag is declared on
   * the Feature and applies to all scenarios in the file.
   */
  @Before("@petition058")
  public void resetScenarioState() {
    debtorIndex.clear();
    fordringIndex.clear();
    modregningEventIndex.clear();
    eventKeyIndex.clear();
    lastHttpStatus = 0;
    activeCaseworkerId = null;
    waiverOutcome = false;
  }

  // ── Background steps ────────────────────────────────────────────────────────

  /** Background: Asserts the modregning workflow is active (application context health check). */
  @Given("the debt-service modregning workflow is active")
  public void theDebtServiceModregningWorkflowIsActive() {
    throw new PendingException(
        "Not implemented: ModregningService bean must be registered and OffsettingService"
            + " implementation wired. See SPEC-058 §3.1.");
  }

  /** Background: Asserts DaekningsRaekkefoeigenService (P057) is reachable. AC-3. */
  @Given("the payment-service DaekningsRaekkefoeigenService is available")
  public void theDaekningsRaekkefoeigenServiceIsAvailable() {
    throw new PendingException(
        "Not implemented: DaekningsRaekkefoeigenService P057 client must be wired and healthy."
            + " See SPEC-058 §3.2.");
  }

  /** Background: Asserts caseworker portal is running. AC-6, AC-7. */
  @Given("the caseworker portal is running")
  public void theCaseworkerPortalIsRunning() {
    throw new PendingException(
        "Not implemented: caseworker portal (ModregningController) must be deployed."
            + " See SPEC-058 §3.1.");
  }

  /**
   * Background: Seeds a BusinessConfig key with a string value. FR-4.1 — rentelov.refRate used by
   * RenteGodtgoerelseService.computeRate.
   */
  @Given("BusinessConfigService key {string} is set to {string} percent")
  public void businessConfigServiceKeyIsSetToPercent(String key, String valuePercent) {
    throw new PendingException(
        "Not implemented: BusinessConfigService must support key '"
            + key
            + "' = '"
            + valuePercent
            + "'. RenteGodtgoerelseRateEntry seeded from this config."
            + " See SPEC-058 §3.4.");
  }

  /** Background: Configures DanishBankingCalendar for the test year. FR-4.1, AC-12. */
  @Given("the DanishBankingCalendar is configured for the current test year")
  public void theDanishBankingCalendarIsConfigured() {
    throw new PendingException(
        "Not implemented: DanishBankingCalendar must be registered as a Spring bean"
            + " with test-year holiday data. See SPEC-058 §3.4.");
  }

  /**
   * Background: NFR-3 (GDPR) assertion — the service must store debtor identity as a UUID, never as
   * a raw CPR number.
   *
   * <p>Spec reference: SPEC §NFR-3, ADR-0014
   */
  @And("the debtorPersonId for any debtor is stored as a UUID never as CPR")
  public void debtorPersonIdIsUuidNeverCpr() {
    // NFR-3 (GDPR) - SPEC §NFR-3, ADR-0014
    // Verified by: asserting that the ModregningEvent persisted by the service
    // uses debtor_person_id UUID field, never a raw CPR number
    throw new PendingException(
        "NFR-3: Implement GDPR UUID-only verification in ModregningServiceTest.verifyGdprCompliance");
  }

  // ── Given — debtor & fordring setup ────────────────────────────────────────

  /**
   * Seeds tier-1 fordringer (UBO — registered by the paying authority) for a debtor. GIL § 7, stk.
   * 1, nr. 1. AC-2.
   *
   * <p>DataTable columns: fordringId | tilbaestaaendeBeloeb | registreringsdato
   */
  @Given("debtor {string} has the following tier-1 fordringer registered by the paying authority:")
  public void debtorHasTier1Fordringer(String debtorRef, DataTable table) {
    throw new PendingException(
        "Not implemented: seed tier-1 fordringer for debtor '"
            + debtorRef
            + "'."
            + " Requires GET /internal/debtors/{id}/fordringer/active?tier=1 data."
            + " AC-2. See SPEC-058 §4.1.");
  }

  /**
   * Seeds tier-2 fordringer (RIM inddrivelse — UBO + RIM). GIL § 7, stk. 1, nr. 2. AC-1, AC-3.
   *
   * <p>DataTable columns: fordringId | tilbaestaaendeBeloeb | modtagelsesdato
   */
  @Given("debtor {string} has the following tier-2 fordringer under RIM inddrivelse:")
  public void debtorHasTier2Fordringer(String debtorRef, DataTable table) {
    throw new PendingException(
        "Not implemented: seed tier-2 fordringer for debtor '"
            + debtorRef
            + "'."
            + " Requires active fordringer with tier=2 queryable via TB-040."
            + " AC-1, AC-3. See SPEC-058 §4.1.");
  }

  /**
   * Seeds tier-3 fordringer (andre — sorted by registreringsdato ASC). GIL § 7, stk. 1, nr. 3.
   * AC-1.
   *
   * <p>DataTable columns: fordringId | tilbaestaaendeBeloeb | registreringsdato
   */
  @Given("debtor {string} has the following tier-3 fordringer in registration order:")
  public void debtorHasTier3Fordringer(String debtorRef, DataTable table) {
    throw new PendingException(
        "Not implemented: seed tier-3 fordringer for debtor '"
            + debtorRef
            + "'."
            + " Must be queryable via TB-040 tier=3. AC-1. See SPEC-058 §4.1.");
  }

  /** Asserts no tier-1 fordringer exist for the debtor (used in tier-2/tier-3 only paths). */
  @Given("debtor {string} has no tier-1 fordringer")
  public void debtorHasNoTier1Fordringer(String debtorRef) {
    throw new PendingException(
        "Not implemented: ensure no tier-1 fordringer for debtor '"
            + debtorRef
            + "'."
            + " See SPEC-058 §4.1.");
  }

  /** Asserts no tier-2 fordringer exist for the debtor. */
  @Given("debtor {string} has no tier-2 fordringer")
  public void debtorHasNoTier2Fordringer(String debtorRef) {
    throw new PendingException(
        "Not implemented: ensure no tier-2 fordringer for debtor '"
            + debtorRef
            + "'."
            + " See SPEC-058 §4.1.");
  }

  /** Asserts no tier-3 fordringer exist for the debtor. */
  @Given("debtor {string} has no tier-3 fordringer")
  public void debtorHasNoTier3Fordringer(String debtorRef) {
    throw new PendingException(
        "Not implemented: ensure no tier-3 fordringer for debtor '"
            + debtorRef
            + "'."
            + " See SPEC-058 §4.1.");
  }

  /**
   * Seeds one active tier-2 fordring with the given outstanding balance. Used in FR-4 banking-day
   * and rentegodtgørelse scenarios. AC-12, AC-13, AC-15.
   */
  @Given("debtor {string} has one active tier-2 fordring with tilbaestaaendeBeloeb {double} DKK")
  public void debtorHasOneTier2Fordring(String debtorRef, double beloeb) {
    throw new PendingException(
        "Not implemented: seed single tier-2 fordring ("
            + beloeb
            + " DKK) for debtor '"
            + debtorRef
            + "'. AC-12, AC-13, AC-15. See SPEC-058 §3.4.");
  }

  /**
   * Seeds a tier-2 fordring with a named fordringId reference for the debtor. Used in gendækning
   * and korrektionspulje settlement scenarios. AC-8, AC-10.
   */
  @Given(
      "debtor {string} has an active tier-2 fordring {string} with tilbaestaaendeBeloeb {double} DKK")
  public void debtorHasActiveTier2FordringWithRef(
      String debtorRef, String fordringRef, double beloeb) {
    throw new PendingException(
        "Not implemented: seed tier-2 fordring '"
            + fordringRef
            + "' ("
            + beloeb
            + " DKK)"
            + " for debtor '"
            + debtorRef
            + "'. AC-8, AC-10. See SPEC-058 §3.3.");
  }

  /** Seeds an additional tier-2 fordring by reference (used in gendækning scenarios). AC-8. */
  @Given(
      "debtor {string} has another tier-2 fordring {string} with tilbaestaaendeBeloeb {double} DKK")
  public void debtorHasAnotherTier2Fordring(String debtorRef, String fordringRef, double beloeb) {
    throw new PendingException(
        "Not implemented: seed additional tier-2 fordring '"
            + fordringRef
            + "' ("
            + beloeb
            + " DKK) for debtor '"
            + debtorRef
            + "'. AC-8. See SPEC-058 §3.3.");
  }

  // ── Given — PublicDisbursementEvent setup ───────────────────────────────────

  /**
   * Stages a PublicDisbursementEvent with disbursementAmount and nemkontoReferenceId. Used as a
   * Given-context event that will be processed in a When step. AC-1 through AC-5, AC-15.
   */
  @Given(
      "a PublicDisbursementEvent arrives for debtor {string} with disbursementAmount {double} DKK and nemkontoReferenceId {string}")
  public void publicDisbursementEventArrives(String debtorRef, double amount, String nemkontoRef) {
    throw new PendingException(
        "Not implemented: stage PublicDisbursementEvent for debtor '"
            + debtorRef
            + "', amount="
            + amount
            + ", nemkontoRef='"
            + nemkontoRef
            + "'."
            + " NFR-3: must use debtorPersonId (UUID), no CPR."
            + " AC-1, AC-2, AC-3, AC-4, AC-5, AC-14. See SPEC-058 §3.6.");
  }

  /**
   * Stages a PublicDisbursementEvent with only receiptDate set (decisionDate set separately). Used
   * in FR-4 banking-day exception and rentegodtgørelse scenarios. AC-12, AC-16.
   */
  @Given("a PublicDisbursementEvent arrives for debtor {string} with receiptDate {string}")
  public void publicDisbursementEventArrivesWithReceiptDate(
      String debtorRef, String receiptDateStr) {
    throw new PendingException(
        "Not implemented: stage PublicDisbursementEvent for debtor '"
            + debtorRef
            + "' with receiptDate="
            + receiptDateStr
            + ". AC-12, AC-16. See SPEC-058 §3.4.");
  }

  /**
   * Sets the modregning decisionDate relative to receiptDate. FR-4.1 five-banking-day rule. AC-12.
   */
  @Given(
      "the modregning decision is made on {string} which is {int} banking days after receiptDate")
  public void modregningDecisionIsMadeOn(String decisionDateStr, int bankingDays) {
    throw new PendingException(
        "Not implemented: set modregning decisionDate="
            + decisionDateStr
            + " ("
            + bankingDays
            + " banking days after receiptDate)."
            + " DanishBankingCalendar.bankingDaysBetween must be configured."
            + " AC-12. See SPEC-058 §3.4.");
  }

  /**
   * Stages a PublicDisbursementEvent from a DataTable map (paymentType, indkomstAar, receiptDate,
   * disbursementAmount, nemkontoReferenceId). AC-13 — Kildeskattelov § 62/62A OVERSKYDENDE_SKAT.
   *
   * <p>DataTable layout: | field | value |
   */
  @Given("a PublicDisbursementEvent arrives for debtor {string} with:")
  public void publicDisbursementEventArrivesWithTable(String debtorRef, DataTable table) {
    throw new PendingException(
        "Not implemented: stage PublicDisbursementEvent from DataTable for debtor '"
            + debtorRef
            + "'."
            + " Must parse paymentType, indkomstAar, receiptDate, disbursementAmount,"
            + " nemkontoReferenceId. AC-13. See SPEC-058 §3.4.");
  }

  // ── Given — Idempotency setup ────────────────────────────────────────────────

  /** Seeds an already-processed PublicDisbursementEvent (for idempotency test). AC-5 — NFR-4. */
  @Given(
      "a PublicDisbursementEvent with nemkontoReferenceId {string} has already been processed for debtor {string}")
  public void disbursementEventAlreadyProcessed(String nemkontoRef, String debtorRef) {
    throw new PendingException(
        "Not implemented: seed existing ModregningEvent for nemkontoReferenceId='"
            + nemkontoRef
            + "' and debtor '"
            + debtorRef
            + "'."
            + " AC-5 (idempotency). See SPEC-058 §3.1.");
  }

  /** Asserts a ModregningEvent already exists for the given nemkontoReferenceId. AC-5. */
  @Given("a ModregningEvent exists for {string}")
  public void modregningEventExistsFor(String nemkontoRef) {
    throw new PendingException(
        "Not implemented: verify ModregningEvent exists for nemkontoReferenceId='"
            + nemkontoRef
            + "'. AC-5. See SPEC-058 §3.1.");
  }

  // ── Given — Korrektionspulje / Gendækning setup ──────────────────────────────

  /**
   * Seeds context that debtor had a specific fordring previously offset by modregning. AC-8, AC-11.
   */
  @Given(
      "debtor {string} had fordring {string} with tilbaestaaendeBeloeb {double} DKK previously offset")
  public void debtorHadFordringPreviouslyOffset(
      String debtorRef, String fordringRef, double beloeb) {
    throw new PendingException(
        "Not implemented: seed prior modregning for fordring '"
            + fordringRef
            + "' ("
            + beloeb
            + " DKK) of debtor '"
            + debtorRef
            + "'."
            + " AC-8. See SPEC-058 §3.3.");
  }

  /**
   * Seeds the fact that debtor had a fordring previously offset (without specifying beloeb). AC-11.
   */
  @Given("debtor {string} had a fordring {string} previously offset by modregning")
  public void debtorHadFordringOffsetByModregning(String debtorRef, String fordringRef) {
    throw new PendingException(
        "Not implemented: seed prior modregning context for fordring '"
            + fordringRef
            + "' of debtor '"
            + debtorRef
            + "'."
            + " AC-11. See SPEC-058 §3.3.");
  }

  /**
   * Seeds that the original disbursement carried a specific paymentType. Used for
   * børne-og-ungeydelse restriction propagation. AC-11.
   */
  @Given("the original disbursement had paymentType {string}")
  public void originalDisbursementHadPaymentType(String paymentType) {
    throw new PendingException(
        "Not implemented: record originalPaymentType='"
            + paymentType
            + "' on context."
            + " AC-11 — boerneYdelseRestriction must propagate to KorrektionspuljeEntry."
            + " See SPEC-058 §3.3.");
  }

  /**
   * Seeds an OffsettingReversalEvent triggered by fordring write-down. AC-8, AC-11. surplusAmount
   * goes to the gendækning / korrektionspulje logic.
   */
  @Given(
      "fordring {string} has been written down generating an OffsettingReversalEvent with surplusAmount {double} DKK")
  public void fordringHasBeenWrittenDownWithReversalEvent(String fordringRef, double surplus) {
    throw new PendingException(
        "Not implemented: seed OffsettingReversalEvent for fordring '"
            + fordringRef
            + "', surplusAmount="
            + surplus
            + " DKK."
            + " Triggers OffsettingReversalEventConsumer → KorrektionspuljeService.processReversal."
            + " AC-8, AC-11. See SPEC-058 §3.3 and §3.6.");
  }

  /**
   * Seeds an OffsettingReversalEvent with surplusAmount; fordring still has uncovered renter. AC-8
   * — Step 1 of processReversal algorithm.
   */
  @Given(
      "fordring {string} is written down generating an OffsettingReversalEvent with surplusAmount {double} DKK")
  public void fordringIsWrittenDownWithReversalEvent(String fordringRef, double surplus) {
    throw new PendingException(
        "Not implemented: seed OffsettingReversalEvent for fordring '"
            + fordringRef
            + "', surplusAmount="
            + surplus
            + " DKK."
            + " AC-8. See SPEC-058 §3.3.");
  }

  /**
   * Seeds an uncovered renter portion on the same fordring after write-down. AC-8 Step 1
   * (Gæld.bekendtg. § 7, stk. 4).
   */
  @Given("fordring {string} still has an uncovered portion of {double} DKK \\(renter\\)")
  public void fordringHasUncoveredRenterPortion(String fordringRef, double amount) {
    throw new PendingException(
        "Not implemented: set uncoveredPortion="
            + amount
            + " DKK on fordring '"
            + fordringRef
            + "' (renter sub-position)."
            + " AC-8 Step 1 (Gæld.bekendtg. § 7, stk. 4). See SPEC-058 §3.3.");
  }

  /**
   * Seeds: gendækning exhausted 0.00 DKK (no eligible fordringer — opt-out or empty pool). AC-8,
   * AC-11 — surplus goes straight to KorrektionspuljeEntry.
   */
  @Given("gendækning exhausts {double} DKK of the surplus \\(no eligible fordringer exist\\)")
  public void gendaekningExhausts(double amount) {
    throw new PendingException(
        "Not implemented: configure DaekningsRaekkefoeigenService to return empty allocation"
            + " (0.00 DKK gendækning). AC-11. See SPEC-058 §3.3.");
  }

  /** Seeds an existing KorrektionspuljeEntry with boerneYdelseRestriction true. AC-11. */
  @Given(
      "a KorrektionspuljeEntry exists with surplusAmount {double} DKK and boerneYdelseRestriction true")
  public void korrektionspuljeEntryExistsWith(double surplusAmount) {
    throw new PendingException(
        "Not implemented: seed KorrektionspuljeEntry, surplusAmount="
            + surplusAmount
            + " DKK, boerneYdelseRestriction=true."
            + " AC-11. See SPEC-058 §2.1.2.");
  }

  /**
   * Seeds a KorrektionspuljeEntry from a DataTable map. AC-9, AC-10.
   *
   * <p>DataTable layout: | field | value | (surplusAmount, correctionPoolTarget,
   * boerneYdelseRestriction, renteGodtgoerelseAccrued, renteGodtgoerelseStartDate,
   * annualOnlySettlement)
   */
  @Given("debtor {string} has a KorrektionspuljeEntry with:")
  public void debtorHasKorrektionspuljeEntry(String debtorRef, DataTable table) {
    throw new PendingException(
        "Not implemented: seed KorrektionspuljeEntry from DataTable for debtor '"
            + debtorRef
            + "'."
            + " AC-9, AC-10. See SPEC-058 §2.1.2.");
  }

  // ── Given — Waiver setup ────────────────────────────────────────────────────

  /**
   * Seeds an existing ModregningEvent with a named key and tier2WaiverApplied=false. AC-6, AC-7.
   */
  @Given("a ModregningEvent {string} exists for debtor {string} with tier2WaiverApplied false")
  public void modregningEventExistsWithWaiverFalse(String eventKey, String debtorRef) {
    throw new PendingException(
        "Not implemented: seed ModregningEvent '"
            + eventKey
            + "' for debtor '"
            + debtorRef
            + "' with tier2WaiverApplied=false."
            + " AC-6, AC-7. See SPEC-058 §3.1.");
  }

  /** Seeds an existing ModregningEvent with a named key (waiver state not specified). AC-7. */
  @Given("a ModregningEvent {string} exists for debtor {string}")
  public void modregningEventExistsForDebtor(String eventKey, String debtorRef) {
    throw new PendingException(
        "Not implemented: seed ModregningEvent '"
            + eventKey
            + "' for debtor '"
            + debtorRef
            + "'. AC-7. See SPEC-058 §3.1.");
  }

  /** Seeds the caseworker's OAuth2 scope for the waiver authorisation check. AC-6. */
  @Given("caseworker {string} holds OAuth2 scope {string}")
  public void caseworkerHoldsOAuthScope(String caseworkerRef, String scope) {
    throw new PendingException(
        "Not implemented: configure OAuth2 security context for caseworker '"
            + caseworkerRef
            + "' with scope '"
            + scope
            + "'."
            + " AC-6 (GIL § 4, stk. 11). See SPEC-058 §3.1.");
  }

  /** Seeds a caller that does NOT hold the required OAuth2 scope. AC-7. */
  @Given("caller {string} does NOT hold OAuth2 scope {string}")
  public void callerDoesNotHoldOAuthScope(String callerRef, String scope) {
    throw new PendingException(
        "Not implemented: configure OAuth2 security context for caller '"
            + callerRef
            + "' WITHOUT scope '"
            + scope
            + "'."
            + " AC-7 (403 Forbidden). See SPEC-058 §3.1.");
  }

  // ── When — Event processing ─────────────────────────────────────────────────

  /**
   * Triggers ModregningService to process the staged PublicDisbursementEvent. This is the primary
   * action for FR-1 scenarios. AC-1 through AC-5, AC-12, AC-13, AC-14, AC-15.
   *
   * <p>NFR-1 (Atomicity): entire modregning runs in a single @Transactional boundary. NFR-2
   * (Auditability): audit log written within the same transaction.
   */
  @When("the ModregningService processes the event")
  public void modregningServiceProcessesTheEvent() {
    throw new PendingException(
        "Not implemented: call ModregningService.initiateModregning(...)."
            + " Must be @Transactional (NFR-1). Must write CLS audit (NFR-2)."
            + " Must not contain CPR/PII in ModregningEvent (NFR-3)."
            + " AC-1, AC-2, AC-3, AC-4, AC-5, AC-12, AC-13, AC-14. See SPEC-058 §3.1.");
  }

  /** Re-submits the same PublicDisbursementEvent to test idempotency guard. AC-5 (NFR-4). */
  @When("the same PublicDisbursementEvent with nemkontoReferenceId {string} arrives again")
  public void sameDisbursementEventArrivesAgain(String nemkontoRef) {
    throw new PendingException(
        "Not implemented: re-trigger PublicDisbursementEventConsumer with nemkontoReferenceId='"
            + nemkontoRef
            + "'. Idempotency guard must prevent second processing."
            + " AC-5 (NFR-4). See SPEC-058 §3.1.");
  }

  /**
   * Triggers OffsettingReversalEventConsumer to process the seeded reversal event. AC-8. Invokes
   * KorrektionspuljeService.processReversal (3-step algorithm).
   */
  @When("the OffsettingReversalEventConsumer processes the reversal event")
  public void offsettingReversalEventConsumerProcessesReversalEvent() {
    throw new PendingException(
        "Not implemented: call OffsettingReversalEventConsumer → KorrektionspuljeService"
            + ".processReversal(). AC-8. Steps: (1) renter coverage, (2) gendækning,"
            + " (3) pool entry creation. See SPEC-058 §3.3.");
  }

  /** Runs the monthly korrektionspulje settlement job for a specific debtor. AC-9, AC-10, AC-11. */
  @When("the KorrektionspuljeSettlementJob runs its monthly settlement for debtor {string}")
  public void korrektionspuljeJobRunsMonthlyForDebtor(String debtorRef) {
    throw new PendingException(
        "Not implemented: invoke KorrektionspuljeSettlementJob.runMonthlySettlement() for debtor '"
            + debtorRef
            + "'. AC-9, AC-10, AC-11. See SPEC-058 §3.5.");
  }

  /** Runs the monthly korrektionspulje settlement job (all debtors). AC-9, AC-10. */
  @When("the KorrektionspuljeSettlementJob runs its monthly settlement")
  public void korrektionspuljeJobRunsMonthly() {
    throw new PendingException(
        "Not implemented: invoke KorrektionspuljeSettlementJob.runMonthlySettlement()."
            + " Threshold: entries with surplusAmount < 50.00 must have annualOnlySettlement=true"
            + " and be excluded. AC-9, AC-10. See SPEC-058 §3.5.");
  }

  /**
   * Processes a PublicDisbursementEvent directly from a When step (klage scenario). AC-15, AC-16.
   */
  @When(
      "a PublicDisbursementEvent with disbursementAmount {double} DKK is processed for debtor {string} with nemkontoReferenceId {string}")
  public void disbursementEventIsProcessedForDebtor(
      double amount, String debtorRef, String nemkontoRef) {
    throw new PendingException(
        "Not implemented: process PublicDisbursementEvent for debtor '"
            + debtorRef
            + "', amount="
            + amount
            + ", nemkontoRef='"
            + nemkontoRef
            + "'."
            + " AC-15, AC-16. See SPEC-058 §3.1.");
  }

  /**
   * Simulates Digital Post notice delivery success for a given event. AC-15 (notice delivered →
   * klageFristDato = decisionDate + 3 months).
   */
  @When("the Digital Post notice for {string} is delivered on {string}")
  public void digitalPostNoticeIsDeliveredOn(String nemkontoRef, String deliveryDateStr) {
    throw new PendingException(
        "Not implemented: record noticeDelivered=true, noticeDeliveryDate='"
            + deliveryDateStr
            + "' on ModregningEvent for '"
            + nemkontoRef
            + "'."
            + " klageFristDato = noticeDeliveryDate + 3 months."
            + " AC-15. See SPEC-058 §2.1.1.");
  }

  /**
   * Processes a second PublicDisbursementEvent with decisionDate override. AC-16 (notice not
   * delivered → klageFristDato = decisionDate + 12 months).
   */
  @When(
      "a second PublicDisbursementEvent is processed for debtor {string} with nemkontoReferenceId {string} and decisionDate {string}")
  public void secondDisbursementEventIsProcessedWithDecisionDate(
      String debtorRef, String nemkontoRef, String decisionDateStr) {
    throw new PendingException(
        "Not implemented: process second PublicDisbursementEvent for debtor '"
            + debtorRef
            + "', nemkontoRef='"
            + nemkontoRef
            + "', decisionDate='"
            + decisionDateStr
            + "'."
            + " AC-16. See SPEC-058 §3.1.");
  }

  /**
   * Simulates Digital Post notice delivery failure. AC-16 (klageFristDato = decisionDate + 12
   * months when notice cannot be delivered).
   */
  @When("the Digital Post notice for {string} cannot be delivered")
  public void digitalPostNoticeCannotBeDelivered(String nemkontoRef) {
    throw new PendingException(
        "Not implemented: record noticeDelivered=false on ModregningEvent for '"
            + nemkontoRef
            + "'. klageFristDato = decisionDate + 12 months (GIL § 9a)."
            + " AC-16. See SPEC-058 §2.1.1.");
  }

  /**
   * Caseworker calls GET /debtors/{debtorId}/modregning-events read-model endpoint. AC-17.
   *
   * @param debtorId the path variable (non-quoted in Gherkin, captured via regex)
   */
  @When("^a caseworker calls GET /debtors/([^/]+)/modregning-events$")
  public void caseworkerCallsGetModregningEvents(String debtorId) {
    throw new PendingException(
        "Not implemented: GET /debtors/"
            + debtorId
            + "/modregning-events."
            + " Must return ModregningEventReadModel list with klageFristDato."
            + " AC-17. See SPEC-058 §3.1.");
  }

  /**
   * Caseworker with waiver scope calls POST tier2-waiver endpoint. AC-6 (GIL § 4, stk. 11).
   *
   * @param caseworkerRef caseworker identifier (quoted in Gherkin)
   * @param debtorId path variable — debtor (non-quoted in Gherkin)
   * @param eventId path variable — modregning event (non-quoted in Gherkin)
   * @param waiverReason waiver reason string (quoted in Gherkin)
   */
  @When(
      "^caseworker \"([^\"]+)\" calls POST /debtors/([^/]+)/modregning-events/([^/]+)/tier2-waiver with waiverReason \"([^\"]+)\"$")
  public void caseworkerCallsPostTier2Waiver(
      String caseworkerRef, String debtorId, String eventId, String waiverReason) {
    throw new PendingException(
        "Not implemented: POST /debtors/"
            + debtorId
            + "/modregning-events/"
            + eventId
            + "/tier2-waiver by caseworker '"
            + caseworkerRef
            + "'."
            + " Must call ModregningService.applyTier2Waiver(...)."
            + " AC-6 (GIL § 4, stk. 11). See SPEC-058 §3.1.");
  }

  /**
   * Non-authorised caller calls POST tier2-waiver endpoint → must return 403. AC-7.
   *
   * @param callerRef the caller (quoted in Gherkin)
   * @param debtorId path variable
   * @param eventId path variable
   */
  @When("^\"([^\"]+)\" calls POST /debtors/([^/]+)/modregning-events/([^/]+)/tier2-waiver$")
  public void unauthorisedCallerCallsPostTier2Waiver(
      String callerRef, String debtorId, String eventId) {
    throw new PendingException(
        "Not implemented: POST /debtors/"
            + debtorId
            + "/modregning-events/"
            + eventId
            + "/tier2-waiver by '"
            + callerRef
            + "' without required scope."
            + " Must return HTTP 403."
            + " AC-7. See SPEC-058 §3.1.");
  }

  // ── Then — Tier allocation assertions ──────────────────────────────────────

  /**
   * Asserts a fordring is fully covered with the given amount from the specified tier. AC-1, AC-2.
   * Parameterised on tier number so this single definition handles tier-1, tier-2, and tier-3.
   *
   * <p>Example Gherkin: {@code fordring "FDR-58011" is fully covered with 1200.00 DKK from tier-1}
   */
  @Then("fordring {string} is fully covered with {double} DKK from tier-{int}")
  public void fordringIsFullyCoveredWithFromTier(String fordringRef, double amount, int tier) {
    throw new PendingException(
        "Not implemented: assert fordring '"
            + fordringRef
            + "' fully covered with "
            + amount
            + " DKK from tier-"
            + tier
            + "."
            + " Verify CollectionMeasureEntity SET_OFF record and ledger entry."
            + " AC-1, AC-2. See SPEC-058 §3.1.");
  }

  /**
   * Asserts a fordring is partially covered with the given amount from the specified tier. AC-1,
   * AC-3. Covers tier-2 (P057 ordered) and tier-3 (registration order) partial allocations.
   *
   * <p>Example Gherkin: {@code fordring "FDR-58043" is partially covered with 200.00 DKK from
   * tier-3}
   */
  @Then("fordring {string} is partially covered with {double} DKK from tier-{int}")
  public void fordringIsPartiallyCoveredWithFromTier(String fordringRef, double amount, int tier) {
    throw new PendingException(
        "Not implemented: assert fordring '"
            + fordringRef
            + "' partially covered with "
            + amount
            + " DKK from tier-"
            + tier
            + "."
            + " AC-1, AC-3. See SPEC-058 §3.2 (partial tier-2 via P057).");
  }

  /**
   * Asserts fordring received no dækning (no allocation at all). AC-2 (tier-1 short-circuits
   * tier-2).
   */
  @Then("fordring {string} receives no dækning")
  public void fordringReceivesNoDaekning(String fordringRef) {
    throw new PendingException(
        "Not implemented: assert fordring '"
            + fordringRef
            + "' has zero allocation."
            + " AC-2. See SPEC-058 §3.1.");
  }

  /** Asserts fordring received no dækning in this specific event (tier exhausted). AC-3. */
  @Then("fordring {string} receives no dækning in this event")
  public void fordringReceivesNoDaekningInThisEvent(String fordringRef) {
    throw new PendingException(
        "Not implemented: assert fordring '"
            + fordringRef
            + "' has no allocation in the current event."
            + " AC-3. See SPEC-058 §3.2.");
  }

  /**
   * Asserts fordring received no dækning in the waiver re-processed event. AC-6 (tier-2 skipped).
   */
  @Then("fordring {string} receives no dækning in the re-processed event")
  public void fordringReceivesNoDaekningInReprocessedEvent(String fordringRef) {
    throw new PendingException(
        "Not implemented: assert fordring '"
            + fordringRef
            + "' skipped in tier-2 waiver re-run."
            + " AC-6. See SPEC-058 §3.1 applyTier2Waiver step 3.");
  }

  /**
   * Asserts a specific tier-2 fordring is partially covered per P057 ordering (oldest
   * modtagelsesdato first). AC-3.
   *
   * <p>The date in the Gherkin step ({@code 2024-02-01}) is unquoted, so it is matched as three
   * separate {@code {int}} parameters (year, month, day) rather than a {@code {string}}.
   */
  @Then(
      "fordring {string} \\(oldest modtagelsesdato {int}-{int}-{int}\\) is partially covered with {double} DKK per P057 ordering")
  public void fordringIsPartiallyCoveredPerP057(
      String fordringRef, int year, int month, int day, double amount) {
    throw new PendingException(
        "Not implemented: assert tier-2 fordring '"
            + fordringRef
            + "' (modtagelsesdato="
            + year
            + "-"
            + month
            + "-"
            + day
            + ") is partially covered with "
            + amount
            + " DKK."
            + " DaekningsRaekkefoeigenService must be called exactly once."
            + " AC-3. See SPEC-058 §3.2 and §4.2.");
  }

  // ── Then — ModregningEvent amount assertions ────────────────────────────────

  /** Asserts the tier1Amount on a ModregningEvent referenced by nemkontoReferenceId. AC-2. */
  @Then("the ModregningEvent for {string} has tier1Amount {double} DKK")
  public void modregningEventHasTier1Amount(String nemkontoRef, double amount) {
    throw new PendingException(
        "Not implemented: assert ModregningEvent["
            + nemkontoRef
            + "].tier1Amount = "
            + amount
            + " DKK. AC-2. See SPEC-058 §2.1.1.");
  }

  /** Asserts the tier2Amount on a ModregningEvent. AC-1, AC-3. */
  @Then("the ModregningEvent for {string} has tier2Amount {double} DKK")
  public void modregningEventHasTier2Amount(String nemkontoRef, double amount) {
    throw new PendingException(
        "Not implemented: assert ModregningEvent["
            + nemkontoRef
            + "].tier2Amount = "
            + amount
            + " DKK. AC-1, AC-3. See SPEC-058 §2.1.1.");
  }

  /** Asserts the tier3Amount on a ModregningEvent. AC-1. */
  @Then("the ModregningEvent for {string} has tier3Amount {double} DKK")
  public void modregningEventHasTier3Amount(String nemkontoRef, double amount) {
    throw new PendingException(
        "Not implemented: assert ModregningEvent["
            + nemkontoRef
            + "].tier3Amount = "
            + amount
            + " DKK. AC-1. See SPEC-058 §2.1.1.");
  }

  /** Asserts the residualPayoutAmount on a ModregningEvent. AC-1. */
  @Then("the ModregningEvent for {string} has residualPayoutAmount {double} DKK")
  public void modregningEventHasResidualPayoutAmount(String nemkontoRef, double amount) {
    throw new PendingException(
        "Not implemented: assert ModregningEvent["
            + nemkontoRef
            + "].residualPayoutAmount = "
            + amount
            + " DKK. Invariant: tier1+tier2+tier3+residual = disbursementAmount."
            + " AC-1. See SPEC-058 §2.1.1.");
  }

  // ── Then — Service call assertions ──────────────────────────────────────────

  /** Asserts DaekningsRaekkefoeigenService was NOT called (tier-1 short-circuit). AC-2. */
  @Then("DaekningsRaekkefoeigenService is not called for this event")
  public void daekningsRaekkefoeigenServiceIsNotCalled() {
    throw new PendingException(
        "Not implemented: verify DaekningsRaekkefoeigenService was NOT invoked."
            + " Tier-1 full coverage short-circuits tier-2 (AC-2). See SPEC-058 §3.2.");
  }

  /**
   * Asserts DaekningsRaekkefoeigenService was called exactly once with the given residual and
   * debtorPersonId. AC-3 — partial tier-2 must delegate to P057 exactly once.
   */
  @Then(
      "DaekningsRaekkefoeigenService is called once with residualAmount {double} DKK and debtorPersonId for {string}")
  public void daekningsRaekkefoeigenServiceIsCalledOnce(double residualAmount, String debtorRef) {
    throw new PendingException(
        "Not implemented: verify DaekningsRaekkefoeigenService called exactly once with"
            + " residualAmount="
            + residualAmount
            + " DKK for debtor '"
            + debtorRef
            + "'."
            + " AC-3. See SPEC-058 §3.2.");
  }

  // ── Then — CollectionMeasure assertions ─────────────────────────────────────

  /**
   * Asserts a SET_OFF CollectionMeasureEntity exists for the fordring, referencing the current
   * ModregningEvent. AC-4.
   */
  @Then("a SET_OFF CollectionMeasureEntity referencing this ModregningEvent exists for {string}")
  public void setOffCollectionMeasureExistsForFordring(String fordringRef) {
    throw new PendingException(
        "Not implemented: assert CollectionMeasureEntity exists with measureType=SET_OFF"
            + " and modregningEventId set, for fordring '"
            + fordringRef
            + "'."
            + " NFR-2 (Auditability). AC-4. See SPEC-058 §2.1.4.");
  }

  /**
   * Asserts a SET_OFF CollectionMeasureEntity with explicit measureType and modregningEventId
   * exists for the fordring. AC-4 (NFR-2 auditability requirement).
   */
  @Then(
      "a SET_OFF CollectionMeasureEntity with measureType {string} and modregningEventId {string} exists for fordring {string}")
  public void setOffCollectionMeasureWithEventId(
      String measureType, String eventKey, String fordringRef) {
    throw new PendingException(
        "Not implemented: assert CollectionMeasureEntity with measureType='"
            + measureType
            + "' and modregningEventId='"
            + eventKey
            + "' exists for fordring '"
            + fordringRef
            + "'. NFR-2 (Auditability). AC-4. See SPEC-058 §2.1.4.");
  }

  // ── Then — Rentegodtgørelse assertions ──────────────────────────────────────

  /**
   * Asserts renteGodtgoerelseNonTaxable = true on the current ModregningEvent. AC-14. GIL § 8b,
   * stk. 2, 3. pkt. — this MUST be true on every persisted row.
   */
  @Then("the ModregningEvent has renteGodtgoerelseNonTaxable set to true")
  public void modregningEventHasRenteGodtgoerelseNonTaxableTrue() {
    throw new PendingException(
        "Not implemented: assert ModregningEvent.renteGodtgoerelseNonTaxable = true."
            + " Must be true on EVERY persisted ModregningEvent (GIL § 8b, stk. 2, 3. pkt.)."
            + " AC-14. See SPEC-058 §2.1.1.");
  }

  /**
   * Asserts renteGodtgoerelseAccrued amount on current ModregningEvent. AC-12 (5-banking-day
   * exception → 0.00 DKK accrued).
   */
  @Then("the ModregningEvent has renteGodtgoerelseAccrued {double} DKK")
  public void modregningEventHasRenteGodtgoerelseAccrued(double amount) {
    throw new PendingException(
        "Not implemented: assert ModregningEvent.renteGodtgoerelseAccrued = "
            + amount
            + " DKK."
            + " AC-12 (5-banking-day exception → 0.00). See SPEC-058 §3.4.");
  }

  /** Asserts no renteGodtgoerelseStartDate is recorded (5-banking-day exception applies). AC-12. */
  @Then("the ModregningEvent has no renteGodtgoerelseStartDate recorded")
  public void modregningEventHasNoRenteGodtgoerelseStartDate() {
    throw new PendingException(
        "Not implemented: assert ModregningEvent.renteGodtgoerelseStartDate IS NULL."
            + " 5-banking-day exception applied (GIL § 8b). AC-12. See SPEC-058 §3.4.");
  }

  /** Asserts renteGodtgoerelseStartDate on ModregningEvent referenced by nemkontoRef. AC-13. */
  @Then("the ModregningEvent for {string} has renteGodtgoerelseStartDate {string}")
  public void modregningEventHasRenteGodtgoerelseStartDate(
      String nemkontoRef, String expectedDate) {
    throw new PendingException(
        "Not implemented: assert ModregningEvent["
            + nemkontoRef
            + "].renteGodtgoerelseStartDate = '"
            + expectedDate
            + "'."
            + " AC-13 (Kildeskattelov § 62/62A OVERSKYDENDE_SKAT). See SPEC-058 §3.4.");
  }

  /**
   * Asserts renteGodtgoerelseStartDate on the current ModregningEvent (unnamed). AC-16 standard
   * case — 1st of month after receiptDate.
   */
  @Then("the ModregningEvent has renteGodtgoerelseStartDate {string}")
  public void modregningEventHasRenteStartDate(String expectedDate) {
    throw new PendingException(
        "Not implemented: assert ModregningEvent.renteGodtgoerelseStartDate = '"
            + expectedDate
            + "'. Standard case: 1st of month after receiptDate."
            + " AC-16 / FR-4. See SPEC-058 §3.4.");
  }

  /**
   * Asserts the standard receiptDate + 1 month start date was NOT used (OVERSKYDENDE_SKAT exception
   * overrides it). AC-13.
   */
  @Then("the standard start date {string} \\(1st of month after receipt\\) is not used")
  public void standardStartDateIsNotUsed(String standardDate) {
    throw new PendingException(
        "Not implemented: assert renteGodtgoerelseStartDate != '"
            + standardDate
            + "'."
            + " Kildeskattelov § 62/62A start date must override standard calculation."
            + " AC-13. See SPEC-058 §3.4.");
  }

  /**
   * Asserts the rentegodtgørelse rate percentage. FR-4.1 — rate = MAX(0, refRate - 4.0). The
   * BusinessConfig key rentelov.refRate=9.0 → godtgoerelseRate = 5.0.
   */
  @Then("the renteGodtgørelse rate is {double} percent")
  public void renteGodtgoerelseRateIs(double ratePercent) {
    throw new PendingException(
        "Not implemented: assert godtgoerelseRatePercent = "
            + ratePercent
            + "%."
            + " Formula: MAX(0, rentelov.refRate - 4.0). refRate=9.0 → rate=5.0."
            + " FR-4.1. See SPEC-058 §2.1.3 and §3.4.");
  }

  // ── Then — Idempotency assertions ───────────────────────────────────────────

  /**
   * Asserts no new ModregningEvent was created for an already-processed nemkontoReferenceId. AC-5.
   */
  @Then("no new ModregningEvent is created for {string}")
  public void noNewModregningEventIsCreatedFor(String nemkontoRef) {
    throw new PendingException(
        "Not implemented: assert exactly ONE ModregningEvent for nemkontoReferenceId='"
            + nemkontoRef
            + "'. Idempotency guard (NFR-4). AC-5. See SPEC-058 §3.1.");
  }

  /**
   * Asserts the system returned a reference to the existing ModregningEvent (idempotency). AC-5.
   */
  @Then("the system returns a reference to the existing ModregningEvent")
  public void systemReturnsReferenceToExistingModregningEvent() {
    throw new PendingException(
        "Not implemented: assert ModregningService returned existing ModregningResult"
            + " without creating a new one. AC-5 (NFR-4). See SPEC-058 §3.1.");
  }

  /** Asserts no additional SET_OFF CollectionMeasureEntity was created on duplicate event. AC-5. */
  @Then("no additional SET_OFF CollectionMeasureEntity is created")
  public void noAdditionalSetOffCollectionMeasureIsCreated() {
    throw new PendingException(
        "Not implemented: assert SET_OFF CollectionMeasure count unchanged after duplicate event."
            + " AC-5. See SPEC-058 §3.1.");
  }

  // ── Then — Korrektionspulje assertions ──────────────────────────────────────

  /** Asserts the korrektionspulje settled amount including rentegodtgørelse accrual. AC-10. */
  @Then(
      "the KorrektionspuljeEntry is settled with total amount {double} DKK \\(surplus + rentegodtgørelse\\)")
  public void korrektionspuljeEntryIsSettledWithTotalAmount(double totalAmount) {
    throw new PendingException(
        "Not implemented: assert KorrektionspuljeEntry settled with total "
            + totalAmount
            + " DKK"
            + " (surplusAmount + renteGodtgoerelseAccrued)."
            + " AC-10. See SPEC-058 §3.3.");
  }

  /**
   * Asserts a new PublicDisbursementEvent equivalent was created for korrektionspulje settlement.
   * AC-10 — settled amount re-enters the FR-1 three-tier modregning workflow.
   */
  @Then(
      "a new PublicDisbursementEvent equivalent is created with disbursementAmount {double} DKK for debtor {string}")
  public void newPublicDisbursementEventEquivalentCreated(double amount, String debtorRef) {
    throw new PendingException(
        "Not implemented: assert ModregningService.initiateModregning called with amount="
            + amount
            + " DKK for debtor '"
            + debtorRef
            + "' from settleEntry."
            + " TODO: paymentType='KORREKTIONSPULJE_SETTLEMENT' (synthetic constant, no column)."
            + " AC-10. See SPEC-058 §3.3.");
  }

  /**
   * Asserts the FR-1 three-tier modregning workflow was invoked for the settled korrektionspulje
   * amount. AC-10.
   */
  @Then("the FR-1 three-tier modregning workflow is invoked for the settled amount")
  public void frOneTierModregningWorkflowInvokedForSettledAmount() {
    throw new PendingException(
        "Not implemented: assert ModregningsRaekkefoeigenEngine.allocate was called for the"
            + " settled KorrektionspuljeEntry. AC-10. See SPEC-058 §3.5.");
  }

  /**
   * Asserts a fordring receives dækning from a settled korrektionspulje without transporter
   * restrictions from the original payment. AC-10.
   */
  @Then(
      "fordring {string} receives dækning from the settled amount without transporter restrictions from the original payment")
  public void fordringReceivesDaekningWithoutTransporterRestrictions(String fordringRef) {
    throw new PendingException(
        "Not implemented: assert fordring '"
            + fordringRef
            + "' receives dækning from"
            + " settled KorrektionspuljeEntry without original transporter restrictions."
            + " AC-10. See SPEC-058 §3.3 settleEntry.");
  }

  /** Asserts the KorrektionspuljeEntry is marked as settled (settledAt IS NOT NULL). AC-10. */
  @Then("the KorrektionspuljeEntry is marked as settled")
  public void korrektionspuljeEntryIsMarkedAsSettled() {
    throw new PendingException(
        "Not implemented: assert KorrektionspuljeEntry.settledAt IS NOT NULL."
            + " AC-10. See SPEC-058 §2.1.2.");
  }

  /**
   * Asserts the KorrektionspuljeEntry for the debtor was NOT settled in the monthly run. AC-9
   * (threshold: surplusAmount < 50 DKK → annualOnlySettlement).
   */
  @Then("the KorrektionspuljeEntry for debtor {string} is NOT settled")
  public void korrektionspuljeEntryForDebtorIsNotSettled(String debtorRef) {
    throw new PendingException(
        "Not implemented: assert KorrektionspuljeEntry for debtor '"
            + debtorRef
            + "' has settledAt = NULL after monthly run."
            + " AC-9. See SPEC-058 §3.5.");
  }

  /** Asserts the KorrektionspuljeEntry was marked for annual-only settlement. AC-9. */
  @Then("the entry is marked for annual-only settlement")
  public void entryIsMarkedForAnnualOnlySettlement() {
    throw new PendingException(
        "Not implemented: assert KorrektionspuljeEntry.annualOnlySettlement = true."
            + " Threshold: surplusAmount < 50.00 DKK. AC-9. See SPEC-058 §2.1.2.");
  }

  /** Asserts no new ModregningEvent was created from the pool entry in the monthly run. AC-9. */
  @Then("no new ModregningEvent is created from this pool entry in the monthly run")
  public void noNewModregningEventCreatedFromPoolEntry() {
    throw new PendingException(
        "Not implemented: assert no ModregningEvent created from KorrektionspuljeEntry"
            + " with annualOnlySettlement=true in monthly run."
            + " AC-9. See SPEC-058 §3.5.");
  }

  // ── Then — Børne-og-ungeydelse restriction assertions ───────────────────────

  /**
   * Asserts the settled børne-og-ungeydelse amount is NOT treated as an unrestricted payment. AC-11
   * — KorrektionspuljeEntry.boerneYdelseRestriction must propagate to settleEntry.
   */
  @Then("the settled amount of {double} DKK is NOT treated as an unrestricted Nemkonto payment")
  public void settledAmountIsNotUnrestricted(double amount) {
    throw new PendingException(
        "Not implemented: assert KorrektionspuljeService.settleEntry passes"
            + " restrictedPayment=true to ModregningService.initiateModregning for amount="
            + amount
            + " DKK. AC-11. See SPEC-058 §3.3.");
  }

  /** Asserts boerneYdelseRestriction flag is true on the settled amount context. AC-11. */
  @Then("the boerneYdelseRestriction flag is true on the settled amount")
  public void boerneYdelseRestrictionFlagIsTrueOnSettledAmount() {
    throw new PendingException(
        "Not implemented: assert KorrektionspuljeEntry.boerneYdelseRestriction = true"
            + " is propagated through settleEntry."
            + " AC-11. See SPEC-058 §2.1.2.");
  }

  /** Asserts børne-og-ungeydelse modregning restrictions apply to the re-applied amount. AC-11. */
  @Then("the børne-og-ungeydelse modregning restrictions apply to the re-applied amount")
  public void boerneOgUngeydelsRestrictionsApply() {
    throw new PendingException(
        "Not implemented: assert restrictedPayment=true parameter propagated to ModregningService"
            + " preventing use of børne-og-ungeydelse funds for ineligible fordringer."
            + " AC-11. See SPEC-058 §3.3 settleEntry.");
  }

  // ── Then — Gendækning assertions ────────────────────────────────────────────

  /**
   * Asserts Step 1 of processReversal: the given amount was applied to the same fordring's
   * uncovered renter portion (Gæld.bekendtg. § 7, stk. 4). AC-8.
   */
  @Then("Step 1: {double} DKK is applied to fordring {string} uncovered renter portion")
  public void step1AmountAppliedToUncoveredRenter(double amount, String fordringRef) {
    throw new PendingException(
        "Not implemented: assert "
            + amount
            + " DKK applied as ledger adjustment to fordring '"
            + fordringRef
            + "' uncovered renter sub-position (Step 1, Gæld.bekendtg. § 7, stk. 4)."
            + " AC-8. See SPEC-058 §3.3 processReversal.");
  }

  /**
   * Asserts Step 2 of processReversal: DaekningsRaekkefoeigenService called with remaining surplus
   * for gendækning. AC-8.
   */
  @Then(
      "Step 2: DaekningsRaekkefoeigenService is called with remaining surplus {double} DKK for gendækning")
  public void step2DaekningsRaekkefoeigenServiceCalledForGendaekning(double remaining) {
    throw new PendingException(
        "Not implemented: assert DaekningsRaekkefoeigenService called with remainingSurplus="
            + remaining
            + " DKK for gendækning (Step 2)."
            + " AC-8. See SPEC-058 §3.3 processReversal.");
  }

  /** Asserts a fordring was gendækket (re-covered) with the given amount. AC-8. */
  @Then("fordring {string} is gendækket with {double} DKK")
  public void fordringIsGendaekketWith(String fordringRef, double amount) {
    throw new PendingException(
        "Not implemented: assert fordring '"
            + fordringRef
            + "' received gendækning of "
            + amount
            + " DKK via DaekningsRaekkefoeigenService."
            + " AC-8. See SPEC-058 §3.3 processReversal Step 2.");
  }

  /** Asserts a KorrektionspuljeEntry was created with the given surplus amount. AC-8. */
  @Then("a KorrektionspuljeEntry is created with surplusAmount {double} DKK")
  public void korrektionspuljeEntryCreatedWithSurplus(double surplusAmount) {
    throw new PendingException(
        "Not implemented: assert KorrektionspuljeEntry persisted with surplusAmount="
            + surplusAmount
            + " DKK."
            + " AC-8. See SPEC-058 §3.3 processReversal Step 3.");
  }

  /** Asserts no Digital Post notice was sent for gendækning operations. AC-8. */
  @Then("no Digital Post notice is sent for the gendækning")
  public void noDigitalPostNoticeForGendaekning() {
    throw new PendingException(
        "Not implemented: assert no Digital Post outbox message created for gendækning."
            + " AC-8. See SPEC-058 §3.3 processReversal.");
  }

  // ── Then — Klage deadline assertions ────────────────────────────────────────

  /**
   * Asserts a ModregningEvent with a named key was persisted for the given nemkontoReferenceId.
   * AC-4.
   */
  @Then("a ModregningEvent {string} is persisted for {string}")
  public void modregningEventIsPersisted(String eventKey, String nemkontoRef) {
    throw new PendingException(
        "Not implemented: assert ModregningEvent '"
            + eventKey
            + "' exists for"
            + " nemkontoReferenceId='"
            + nemkontoRef
            + "'."
            + " AC-4. See SPEC-058 §2.1.1.");
  }

  /** Asserts noticeDelivered = true on the ModregningEvent. AC-15. */
  @Then("the ModregningEvent for {string} has noticeDelivered true")
  public void modregningEventHasNoticeDeliveredTrue(String nemkontoRef) {
    throw new PendingException(
        "Not implemented: assert ModregningEvent["
            + nemkontoRef
            + "].noticeDelivered = true."
            + " AC-15. See SPEC-058 §2.1.1.");
  }

  /**
   * Asserts klageFristDato on the ModregningEvent. AC-15 (notice delivered → +3 months), AC-16
   * (notice failed → +12 months).
   */
  @Then("the ModregningEvent for {string} has klageFristDato {string}")
  public void modregningEventHasKlageFristDato(String nemkontoRef, String expectedDate) {
    throw new PendingException(
        "Not implemented: assert ModregningEvent["
            + nemkontoRef
            + "].klageFristDato = '"
            + expectedDate
            + "'. AC-15 (noticeDelivered) / AC-16 (notice failed)."
            + " See SPEC-058 §2.1.1.");
  }

  /** Asserts noticeDelivered = false on the ModregningEvent (Digital Post failure). AC-16. */
  @Then("the ModregningEvent for {string} has noticeDelivered false")
  public void modregningEventHasNoticeDeliveredFalse(String nemkontoRef) {
    throw new PendingException(
        "Not implemented: assert ModregningEvent["
            + nemkontoRef
            + "].noticeDelivered = false."
            + " AC-16. klageFristDato = decisionDate + 12 months. See SPEC-058 §2.1.1.");
  }

  /** Asserts the read-model response contains both ModregningEvents with klageFristDato. AC-17. */
  @Then("the response contains both modregning events with their klageFristDato values")
  public void responseContainsBothModregningEventsWithKlageFristDato() {
    throw new PendingException(
        "Not implemented: assert GET response contains 2 ModregningEvent read-model entries"
            + " each with klageFristDato set."
            + " AC-17. See SPEC-058 §3.1.");
  }

  /** Asserts each read-model event includes the required fields. AC-17. */
  @Then(
      "each event in the response includes fields: eventId, decisionDate, totalOffsetAmount, tier1Amount, tier2Amount, tier3Amount, residualPayoutAmount, klageFristDato, noticeDelivered, tier2WaiverApplied")
  public void eachEventIncludesRequiredFields() {
    throw new PendingException(
        "Not implemented: assert ModregningEvent read-model DTO contains all required fields:"
            + " eventId, decisionDate, totalOffsetAmount, tier1Amount, tier2Amount, tier3Amount,"
            + " residualPayoutAmount, klageFristDato, noticeDelivered, tier2WaiverApplied."
            + " AC-17. See SPEC-058 §3.1.");
  }

  /**
   * Asserts the caseworker portal shows an amber indicator when klageFristDato is within 14 days.
   * AC-17.
   */
  @Then(
      "the caseworker portal displays the event for {string} with an amber indicator if klageFristDato is within 14 days")
  public void caseworkerPortalDisplaysAmberIndicator(String nemkontoRef) {
    throw new PendingException(
        "Not implemented: assert portal read-model includes a flag/field for 'klageFristDato"
            + " within 14 days' for event '"
            + nemkontoRef
            + "'."
            + " AC-17. See SPEC-058 §3.1.");
  }

  // ── Then — Waiver assertions ─────────────────────────────────────────────────

  /** Asserts tier2WaiverApplied = true on the named ModregningEvent. AC-6. */
  @Then("the ModregningEvent {string} has tier2WaiverApplied set to true")
  public void modregningEventHasTier2WaiverAppliedTrue(String eventKey) {
    throw new PendingException(
        "Not implemented: assert ModregningEvent['"
            + eventKey
            + "'].tier2WaiverApplied = true."
            + " AC-6 (GIL § 4, stk. 11). See SPEC-058 §3.1 applyTier2Waiver.");
  }

  /** Asserts the three-tier engine re-ran with skipTier2=true for the event. AC-6. */
  @Then("the three-tier ordering engine re-runs for {string} skipping tier-2")
  public void threeTierEngineReRunsSkippingTier2(String eventKey) {
    throw new PendingException(
        "Not implemented: assert ModregningsRaekkefoeigenEngine.allocate called with"
            + " skipTier2=true for event '"
            + eventKey
            + "'."
            + " AC-6. See SPEC-058 §3.1 applyTier2Waiver step 3.");
  }

  /** Asserts each SET_OFF CollectionMeasureEntity for the event has waiverApplied = true. AC-6. */
  @Then("each SET_OFF CollectionMeasureEntity for this event has waiverApplied set to true")
  public void eachSetOffMeasureHasWaiverApplied() {
    throw new PendingException(
        "Not implemented: assert all CollectionMeasureEntity rows for the waiver event have"
            + " waiverApplied=true and caseworkerId set."
            + " AC-6. See SPEC-058 §2.1.4.");
  }

  /**
   * Asserts the CLS audit log contains an entry with the specified fields. AC-6 (GIL § 4, stk. 11
   * waiver audit), AC-4 (per-tier audit). NFR-2 (Auditability).
   *
   * <p>DataTable layout: | field | value |
   */
  @Then("the CLS audit log contains an entry with:")
  public void clsAuditLogContainsEntryWith(DataTable table) {
    throw new PendingException(
        "Not implemented: assert CLS audit log contains entry matching DataTable fields."
            + " NFR-2 (Auditability): must include gilParagraf, modregningEventId, debtorPersonId"
            + " (as UUID — no CPR per NFR-3), fordringId."
            + " AC-4, AC-6. See SPEC-058 §3.1.");
  }

  /**
   * Asserts the CLS audit log contains entries for two specific fordringer with the given
   * gilParagraf reference. AC-4 (NFR-2 Auditability).
   */
  @Then("the CLS audit log contains entries for {string} and {string} with gilParagraf {string}")
  public void clsAuditLogContainsEntriesForTwo(
      String fordringRef1, String fordringRef2, String gilParagraf) {
    throw new PendingException(
        "Not implemented: assert CLS audit log contains entries for fordringer '"
            + fordringRef1
            + "' and '"
            + fordringRef2
            + "' each with gilParagraf='"
            + gilParagraf
            + "'."
            + " NFR-2 (Auditability). AC-4. See SPEC-058 §3.1.");
  }

  /** Asserts HTTP response status code. AC-6 (200 OK), AC-7 (403 Forbidden). */
  @Then("the HTTP response status is {int}")
  public void httpResponseStatusIs(int expectedStatus) {
    throw new PendingException(
        "Not implemented: assert HTTP response status = "
            + expectedStatus
            + "."
            + " AC-6 (200), AC-7 (403). See SPEC-058 §3.1.");
  }

  /** Asserts tier2WaiverApplied remains false (no change on 403 rejection). AC-7. */
  @Then("the ModregningEvent {string} has tier2WaiverApplied unchanged as false")
  public void modregningEventHasTier2WaiverUnchangedFalse(String eventKey) {
    throw new PendingException(
        "Not implemented: assert ModregningEvent['"
            + eventKey
            + "'].tier2WaiverApplied"
            + " remains false after unauthorised waiver attempt."
            + " AC-7. See SPEC-058 §3.1.");
  }

  /** Asserts no CLS audit log entry was created for an unauthorised waiver attempt. AC-7. */
  @Then("no CLS audit log entry is created for this request")
  public void noClsAuditLogEntryCreated() {
    throw new PendingException(
        "Not implemented: assert no CLS audit event was written for the rejected waiver attempt."
            + " AC-7. See SPEC-058 §3.1.");
  }

  // ── Given — FR-3.2(b) gendækning opt-out ────────────────────────────────────

  /**
   * Seeds a korrektionspulje entry with a specified surplus amount for the debtor. FR-3.2(b) —
   * gendækning opt-out scenarios. SKY-5820, SKY-5821, SKY-5822.
   */
  @Given("debtor {string} has a korrektionspulje entry with surplus {double} DKK")
  public void debtorHasKorrektionspuljeEntryWithSurplus(String debtorRef, double surplusAmount) {
    throw new PendingException(
        "FR-3.2(b): Not implemented: seed KorrektionspuljeEntry with surplus="
            + surplusAmount
            + " DKK for debtor '"
            + debtorRef
            + "'. See SPEC-058 §3.3.");
  }

  /**
   * Sets the correctionPoolTarget on the current korrektionspulje entry context. FR-3.2(b)(a): when
   * target = DMI, gendækning must be skipped.
   */
  @And("the korrektionspulje entry has correctionPoolTarget {string}")
  public void korrektionspuljeEntryHasCorrectionPoolTarget(String target) {
    throw new PendingException(
        "FR-3.2(b)(a): Not implemented: set correctionPoolTarget='"
            + target
            + "' on active KorrektionspuljeEntry."
            + " When target=DMI, gendækning must be skipped. See SPEC-058 §3.3.");
  }

  /** Seeds an uncovered fordring for the debtor (used to verify gendækning opt-out). FR-3.2(b). */
  @And("debtor {string} has an uncovered fordring {string} with {double} DKK")
  public void debtorHasUncoveredFordring(String debtorRef, String fordringRef, double beloeb) {
    throw new PendingException(
        "FR-3.2(b): Not implemented: seed uncovered fordring '"
            + fordringRef
            + "' ("
            + beloeb
            + " DKK) for debtor '"
            + debtorRef
            + "'. See SPEC-058 §3.3.");
  }

  /** Triggers the korrektionspulje settlement process for the given debtor. FR-3.2(b). */
  @When("the korrektionspulje settlement process runs for debtor {string}")
  public void korrektionspuljeSettlementProcessRunsForDebtor(String debtorRef) {
    throw new PendingException(
        "FR-3.2(b): Not implemented: invoke KorrektionspuljeSettlementJob for debtor '"
            + debtorRef
            + "'. See SPEC-058 §3.3.");
  }

  /** Asserts gendækning was skipped for the debtor (no re-coverage applied). FR-3.2(b). */
  @Then("gendækning is skipped for debtor {string}")
  public void gendaekningIsSkippedForDebtor(String debtorRef) {
    throw new PendingException(
        "FR-3.2(b): Not implemented: assert gendækning was NOT applied for debtor '"
            + debtorRef
            + "'. See SPEC-058 §3.3.");
  }

  /** Asserts the named fordring received no gendækning coverage. FR-3.2(b)(a). */
  @And("fordring {string} receives no gendækning coverage")
  public void fordringReceivesNoGendaekningCoverage(String fordringRef) {
    throw new PendingException(
        "FR-3.2(b)(a): Not implemented: assert fordring '"
            + fordringRef
            + "' received no gendækning. See SPEC-058 §3.3.");
  }

  /**
   * Seeds context that the surplus in the active korrektionspulje entry originated from modregning
   * against a debt-under-collection (inddrivelsesindsats). FR-3.2(b)(b).
   */
  @And("the surplus originated from modregning against a debt-under-collection inddrivelsesindsats")
  public void surplusOriginatedFromModregningAgainstDebtUnderCollection() {
    throw new PendingException(
        "FR-3.2(b)(b): Not implemented: mark active KorrektionspuljeEntry surplus origin as"
            + " DEBT_UNDER_COLLECTION inddrivelsesindsats. Gendækning must be skipped."
            + " See SPEC-058 §3.3.");
  }

  /**
   * Seeds that the named fordring was retroactively partially covered with the given amount.
   * FR-3.2(b)(c) — retroactively partially covered fordringer must skip gendækning.
   */
  @And("fordring {string} was retroactively partially covered with {double} DKK")
  public void fordringWasRetroactivelyPartiallyCovered(String fordringRef, double beloeb) {
    throw new PendingException(
        "FR-3.2(b)(c): Not implemented: mark fordring '"
            + fordringRef
            + "' as retroactively partially covered ("
            + beloeb
            + " DKK). Gendækning must be skipped. See SPEC-058 §3.3.");
  }

  /** Asserts gendækning was skipped for the specific named fordring. FR-3.2(b)(c). */
  @Then("gendækning is skipped for fordring {string}")
  public void gendaekningIsSkippedForFordring(String fordringRef) {
    throw new PendingException(
        "FR-3.2(b)(c): Not implemented: assert gendækning was NOT applied for fordring '"
            + fordringRef
            + "'. See SPEC-058 §3.3.");
  }

  // ── Given/When/Then — FR-4.1 rate-change effective-date delay ───────────────

  /**
   * Seeds an initial RenteGodtgoerelseRateEntry with the given referenceRatePercent and effective
   * date. FR-4.1 — rate-change effective-date delay. SKY-5823.
   */
  @Given("a RenteGodtgoerelseRateEntry with referenceRatePercent {double} effective {string}")
  public void renteGodtgoerelseRateEntryWithReferenceRatePercentEffective(
      double referenceRatePercent, String effectiveDate) {
    throw new PendingException(
        "FR-4.1: Not implemented: seed RenteGodtgoerelseRateEntry with referenceRatePercent="
            + referenceRatePercent
            + ", effectiveDate="
            + effectiveDate
            + ". See SPEC-058 §3.4.");
  }

  /**
   * Seeds a new RenteGodtgoerelseRateEntry with the given referenceRatePercent, publication date,
   * and effective date. FR-4.1 — new rate must not be applied until 5 banking days after
   * publication.
   */
  @And(
      "a new RenteGodtgoerelseRateEntry with referenceRatePercent {double} published {string} effective {string}")
  public void newRenteGodtgoerelseRateEntryPublishedEffective(
      double referenceRatePercent, String publishedDate, String effectiveDate) {
    throw new PendingException(
        "FR-4.1: Not implemented: seed new RenteGodtgoerelseRateEntry with referenceRatePercent="
            + referenceRatePercent
            + ", publishedDate="
            + publishedDate
            + ", effectiveDate="
            + effectiveDate
            + ". Rate must not be applied until 5 banking days after publication."
            + " See SPEC-058 §3.4.");
  }

  /** Sets the clock/reference date for the rate computation. FR-4.1. */
  @And("today is {string}")
  public void todayIs(String todayDate) {
    throw new PendingException(
        "FR-4.1: Not implemented: set reference date (today) to '"
            + todayDate
            + "' for RenteGodtgoerelseService.computeRate."
            + " See SPEC-058 §3.4.");
  }

  /** Invokes the rentegodtgørelse rate computation for the given reference date. FR-4.1. */
  @When("the rentegodtgoerelse rate is computed for reference date {string}")
  public void rentegodtgoerelseRateIsComputedForReferenceDate(String referenceDate) {
    throw new PendingException(
        "FR-4.1: Not implemented: invoke RenteGodtgoerelseService.computeRate for referenceDate='"
            + referenceDate
            + "'. See SPEC-058 §3.4.");
  }

  /**
   * Asserts the applied rate matches the prior entry's computation (referenceRate - 4.0). FR-4.1.
   *
   * <p>Example: "the applied rate is 5.0 percent (9.0 minus 4.0 using the prior entry)"
   */
  @Then("the applied rate is {double} percent \\({double} minus {double} using the prior entry\\)")
  public void appliedRateIsPriorEntry(double appliedRate, double referenceRate, double deduction) {
    throw new PendingException(
        "FR-4.1: Not implemented: assert applied rentegodtgørelse rate = "
            + appliedRate
            + "% (referenceRate "
            + referenceRate
            + " - "
            + deduction
            + " = "
            + appliedRate
            + "). Prior entry must still be in effect before 5-banking-day delay elapses."
            + " See SPEC-058 §3.4.");
  }

  /**
   * Asserts the new rate is not yet in effect (5-banking-day delay not elapsed). FR-4.1.
   *
   * <p>Example: "the new rate of 7.0 percent (11.0 minus 4.0) is not yet in effect"
   */
  @And("the new rate of {double} percent \\({double} minus {double}\\) is not yet in effect")
  public void newRateIsNotYetInEffect(double newRate, double referenceRate, double deduction) {
    throw new PendingException(
        "FR-4.1: Not implemented: assert new rentegodtgørelse rate "
            + newRate
            + "% (referenceRate "
            + referenceRate
            + " - "
            + deduction
            + ") is NOT yet applied — 5-banking-day publication delay has not elapsed."
            + " See SPEC-058 §3.4.");
  }
}
