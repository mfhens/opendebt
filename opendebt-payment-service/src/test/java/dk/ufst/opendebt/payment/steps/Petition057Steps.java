package dk.ufst.opendebt.payment.steps;

import static org.assertj.core.api.Assertions.fail;

import java.math.BigDecimal;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Failing BDD step definitions for Petition 057 — Dækningsrækkefølge (GIL § 4 Payment Application
 * Order) — FR-1 through FR-7.
 *
 * <p><strong>Legal basis:</strong> GIL § 4 stk. 1–4, GIL § 6a stk. 1 og stk. 12, GIL § 9 stk. 1 og
 * stk. 3, GIL § 10b, Gæld.bekendtg. § 4 stk. 3, Retsplejelovens § 507, Lov nr. 288/2022.
 *
 * <p><strong>Every step in this file actively fails (RED).</strong> No implementation exists yet.
 * These step definitions are executable contracts. They will remain red until the following
 * components are implemented:
 *
 * <ul>
 *   <li>{@code DaekningsRaekkefoeigenService} — core rule engine ordering and allocation
 *   <li>{@code PrioritetKategori} enum — 5 statutory priority categories (GIL § 4, stk. 1)
 *   <li>{@code RenteKomponent} enum — 6 interest sub-positions (Gæld.bekendtg. § 4, stk. 3)
 *   <li>{@code InddrivelsesindsatsType} enum — payment channel rules (GIL § 4, stk. 3)
 *   <li>{@code DaekningRecord} JPA entity + Liquibase migrations
 *   <li>{@code collection-priority.drl} tvangsbøder category correction (lov nr. 288/2022)
 *   <li>GET /debtors/{debtorId}/daekningsraekkefoelge (AC-14)
 *   <li>POST /debtors/{debtorId}/daekningsraekkefoelge/simulate (AC-15)
 * </ul>
 *
 * <p><strong>W-1 (petition-translator-reviewer / SKY-3017):</strong> {@code applicationTimestamp}
 * must be injected via a deterministic {@code Clock} bean — do not use {@code Instant.now()} in
 * production code. The test must control the clock to make assertions predictable.
 *
 * <p><strong>W-1 (gherkin-minimality-reviewer):</strong> Field names {@code fordringType} (SKY-
 * 3003), {@code sekvensNummer} (SKY-3027), {@code inLoenindeholdelsesIndsats} / {@code
 * inUdlaegForretning} (SKY-3010/SKY-3011) are step-definition concerns handled by {@code DataTable}
 * column mapping inside this file — not Gherkin-level concepts.
 *
 * <p><strong>Note re SKY-3017 gilParagraf:</strong> The feature file uses {@code gilParagraf = "GIL
 * § 4, stk. 2"} which is an acknowledged error in the Gherkin (gherkin-minimality-reviewer W-1).
 * The correct value per SPEC-P057 is the category-level GIL reference. This step accepts the
 * feature-file value; the implementation must map {@code PrioritetKategori} → correct {@code
 * gilParagraf} in {@code DaekningsRaekkefoeigenService}.
 *
 * <p>Spec reference: SPEC-P057 · design/specs-p057-daekningsraekkefoeigen.md
 *
 * <p>Architecture: design/solution-architecture-p057-daekningsraekkefoeigen.md
 */
public class Petition057Steps {

  // ─────────────────────────────────────────────────────────────────────────────
  // Per-scenario mutable state
  // ─────────────────────────────────────────────────────────────────────────────

  /** Active debtor identifier for the current scenario. */
  private String currentDebtorId;

  /** Payment amount set by the most recent payment-triggering step. */
  private BigDecimal currentPaymentAmount;

  /** Betalingstidspunkt set by the most recent payment-triggering step. */
  private String currentBetalingstidspunkt;

  /** HTTP status of the most recent API call. */
  private int lastHttpStatus;

  /** Response body of the most recent API call (raw JSON string). */
  private String lastResponseBody;

  @Before("@petition057")
  public void resetScenarioState() {
    currentDebtorId = null;
    currentPaymentAmount = null;
    currentBetalingstidspunkt = null;
    lastHttpStatus = 0;
    lastResponseBody = null;
  }

  // =========================================================================
  // BACKGROUND — no-op context steps
  // =========================================================================

  @Given("the payment-service rule engine is active")
  public void thePaymentServiceRuleEngineIsActive() {
    // No-op: Spring context bootstrapped by CucumberContextConfiguration (Petition001Steps).
    // Implementation gate: DaekningsRaekkefoeigenService must be registered as a @Service bean.
  }

  @Given("the sagsbehandler portal is running")
  public void theSagsbehandlerPortalIsRunning() {
    // No-op for payment-service tests.
    // FR-8 portal verification is in opendebt-caseworker-portal Petition057PortalSteps.
  }

  // =========================================================================
  // FR-1 — Priority categories (GIL § 4, stk. 1)
  // AC-1: category ordering RIMELIGE_OMKOSTNINGER → ANDRE_FORDRINGER
  // AC-2: tvangsbøder classified as BOEDER_TVANGSBOEEDER_TILBAGEBETALING (lov nr. 288/2022)
  // Component: DaekningsRaekkefoeigenService, PrioritetKategori enum, collection-priority.drl
  // =========================================================================

  /**
   * Seeds the fordringer for a debtor in the test context.
   *
   * <p>DataTable columns (all scenarios): {@code fordringId, kategori, tilbaestaaendeBeloeb,
   * modtagelsesdato}. Optional columns that may appear in specific scenarios (W-1 step-definition
   * concern): {@code legacyModtagelsesdato} (SKY-3006), {@code sekvensNummer} (SKY-3027), {@code
   * opskrivningAfFordringId} (SKY-3013/14/15/29), {@code inLoenindeholdelsesIndsats} (SKY-3010),
   * {@code inUdlaegForretning} (SKY-3011).
   *
   * <p>FR-1: AC-1, AC-2 | FR-4: AC-7, AC-8 | FR-5: AC-9, AC-10, AC-11 | FR-6: AC-12, AC-13
   */
  @Given("^debtor \"([^\"]+)\" has the following active fordringer:$")
  public void debtorHasFollowingActiveFordringer(String debtorId, DataTable table) {
    // FR-1: AC-1, AC-2 — seed fordringer via DaekningFordringRepository
    // W-1: map all DataTable columns including optional ones (fordringType handled separately)
    fail(
        "Not implemented: FR-1 — seed active fordringer for debtor "
            + debtorId
            + " via DaekningFordringRepository. "
            + "Implement DaekningFordringRepository and DaekningFordringEntity first.");
  }

  /**
   * Triggers a payment with an explicit betalingstidspunkt timestamp.
   *
   * <p>FR-1: AC-1 | FR-6: AC-12
   */
  @When(
      "^a payment of (\\d+(?:\\.\\d+)?) DKK is received for debtor \"([^\"]+)\" with"
          + " betalingstidspunkt \"([^\"]+)\"$")
  public void aPaymentIsReceivedWithBetalingstidspunkt(
      BigDecimal beloeb, String debtorId, String betalingstidspunkt) {
    // FR-1: AC-1 | FR-6: AC-12
    // Component: DaekningsRaekkefoeigenService.applyPayment(debtorId, beloeb, betalingstidspunkt)
    fail(
        "Not implemented: FR-1/FR-6 — receive payment "
            + beloeb
            + " DKK for debtor "
            + debtorId
            + " at betalingstidspunkt "
            + betalingstidspunkt);
  }

  /**
   * Triggers a payment without an explicit betalingstidspunkt (uses system time).
   *
   * <p>FR-1: AC-1, AC-2
   */
  @When("^a payment of (\\d+(?:\\.\\d+)?) DKK is received for debtor \"([^\"]+)\"$")
  public void aPaymentIsReceivedForDebtor(BigDecimal beloeb, String debtorId) {
    // FR-1: AC-1, AC-2
    // Component: DaekningsRaekkefoeigenService.applyPayment(debtorId, beloeb, Instant.now())
    fail(
        "Not implemented: FR-1 — receive payment "
            + beloeb
            + " DKK for debtor "
            + debtorId
            + " (system-time betalingstidspunkt)");
  }

  /**
   * Executes the dækningsrækkefølge rule engine against the current scenario's payment.
   *
   * <p>FR-1: AC-1, AC-2 | FR-2: AC-3, AC-4 | FR-3: AC-5, AC-6 | FR-4: AC-7, AC-8
   */
  @When("the dækningsrækkefølge rule engine applies the payment")
  public void theRuleEngineAppliesThePayment() {
    // FR-1..FR-4 — execute DaekningsRaekkefoeigenService.applyPayment()
    // Drools rules in collection-priority.drl must run during this step.
    fail(
        "Not implemented: FR-1 — DaekningsRaekkefoeigenService.applyPayment() execution. "
            + "Implement service, DRL rules, and DaekningRecord persistence.");
  }

  /**
   * Asserts that a fordring is fully covered (tilbaestaaendeBeloeb == 0) with the expected amount.
   * The parenthetical label in the step text is descriptive only and not captured.
   *
   * <p>FR-1: AC-1 | FR-3: AC-5 | FR-4: AC-7, AC-8 | FR-5: AC-9
   */
  @Then("^fordring \"([^\"]+)\" \\(.*?\\) is fully covered with (\\d+(?:\\.\\d+)?) DKK$")
  public void fordringIsFullyCovered(String fordringId, BigDecimal expectedBeloeb) {
    // FR-1: AC-1 — DaekningRecord.daekningBeloeb == expectedBeloeb AND tilbaestaaendeBeloeb == 0
    fail(
        "Not implemented: FR-1 — assert fordring "
            + fordringId
            + " fully covered with "
            + expectedBeloeb
            + " DKK (DaekningRecord + DaekningFordringEntity.tilbaestaaendeBeloeb == 0)");
  }

  /**
   * Asserts that a fordring is covered with a specific amount (partial or exact). Used when "fully"
   * or "partially" qualifier is absent in the step text.
   *
   * <p>FR-1: AC-1
   */
  @Then("^fordring \"([^\"]+)\" \\(.*?\\) is covered with (\\d+(?:\\.\\d+)?) DKK$")
  public void fordringIsCoveredWith(String fordringId, BigDecimal expectedBeloeb) {
    // FR-1: AC-1 — DaekningRecord.daekningBeloeb == expectedBeloeb
    fail(
        "Not implemented: FR-1 — assert fordring "
            + fordringId
            + " covered with "
            + expectedBeloeb
            + " DKK");
  }

  /**
   * Asserts that a fordring receives no dækning (no DaekningRecord created). Matches both plain
   * "receives no dækning" and "receives no dækning from this payment".
   *
   * <p>FR-1: AC-1 | FR-4: AC-7, AC-8
   */
  @Then("^fordring \"([^\"]+)\" \\(.*?\\) receives no dækning(?:.*)?$")
  public void fordringReceivesNoDaekning(String fordringId) {
    // FR-1: AC-1 — assert no DaekningRecord exists for fordringId in this payment's records
    fail(
        "Not implemented: FR-1 — assert no DaekningRecord for fordring "
            + fordringId
            + " (expected zero dækning)");
  }

  /**
   * Asserts that a fordring is partially covered (tilbaestaaendeBeloeb > 0).
   *
   * <p>FR-1: AC-1 | FR-2: AC-3, AC-4 | FR-3: AC-5
   */
  @Then("^fordring \"([^\"]+)\" \\(.*?\\) is partially covered with (\\d+(?:\\.\\d+)?) DKK$")
  public void fordringIsPartiallyCovered(String fordringId, BigDecimal expectedBeloeb) {
    // DaekningRecord.daekningBeloeb == expectedBeloeb AND tilbaestaaendeBeloeb > 0
    fail(
        "Not implemented: FR-1 — assert fordring "
            + fordringId
            + " partially covered with "
            + expectedBeloeb
            + " DKK (remaining balance > 0)");
  }

  /**
   * Asserts the gilParagraf field on the DaekningRecord for the specified fordring.
   *
   * <p>FR-1: AC-1 | FR-4: AC-7, AC-8 | FR-6: AC-13
   *
   * <p><strong>Note re SKY-3017:</strong> The feature file hard-codes {@code "GIL § 4, stk. 2"} as
   * gilParagraf. This is an acknowledged error (gherkin-minimality-reviewer W-1). The correct value
   * per SPEC-P057 is the category-level GIL reference (e.g. "GIL § 4, stk. 1, nr. 2" for
   * BOEDER_TVANGSBOEEDER_TILBAGEBETALING). This step accepts the feature-file value verbatim.
   */
  @Then("^the dækning record for \"([^\"]+)\" carries gilParagraf \"([^\"]+)\"$")
  public void daekningRecordCarriesGilParagraf(String fordringId, String gilParagraf) {
    // FR-1: AC-1 | FR-4: AC-7, AC-8 | FR-6: AC-13
    // Assert DaekningRecord.gilParagraf == gilParagraf for the DaekningRecord of fordringId
    fail(
        "Not implemented: FR-1 — assert DaekningRecord.gilParagraf == \""
            + gilParagraf
            + "\" for fordring "
            + fordringId);
  }

  /**
   * Asserts that multiple fordringer (plural) receive no dækning.
   *
   * <p>FR-1: AC-1
   */
  @Then("^fordringer \"([^\"]+)\", \"([^\"]+)\", and \"([^\"]+)\" receive no dækning$")
  public void fordringerReceiveNoDaekning(String id1, String id2, String id3) {
    // FR-1: AC-1 — assert no DaekningRecord for any of the three fordringer
    fail(
        "Not implemented: FR-1 — assert no DaekningRecord for fordringer "
            + id1
            + ", "
            + id2
            + ", "
            + id3);
  }

  /**
   * Sets the fordringType on a specific fordring. Used only in SKY-3003 (tvangsbøde test).
   *
   * <p>W-1 (gherkin-minimality-reviewer): fordringType is a step-definition concern — resolved here
   * by updating the fordring entity's type field so that collection-priority.drl can assign the
   * correct PrioritetKategori.
   *
   * <p>FR-1: AC-2
   */
  @Given("fordring {string} has fordringType {string}")
  public void fordringHasFordringType(String fordringId, String fordringType) {
    // FR-1: AC-2 — set fordringType on DaekningFordringEntity
    // collection-priority.drl rule: Fordring(type == "TVANGSBOEDE") →
    // BOEDER_TVANGSBOEEDER_TILBAGEBETALING
    fail(
        "Not implemented: FR-1/AC-2 — set fordringType \""
            + fordringType
            + "\" on fordring "
            + fordringId
            + ". Implement DRL rule: TVANGSBOEDE → BOEDER_TVANGSBOEEDER_TILBAGEBETALING (lov nr. 288/2022).");
  }

  /**
   * Asserts the prioritetKategori field on the DaekningRecord for the specified fordring.
   *
   * <p>FR-1: AC-2
   */
  @Then("^the dækning record for \"([^\"]+)\" carries prioritetKategori \"([^\"]+)\"$")
  public void daekningRecordCarriesPrioritetKategori(String fordringId, String prioritetKategori) {
    // FR-1: AC-2 — DaekningRecord.prioritetKategori == PrioritetKategori.valueOf(prioritetKategori)
    fail(
        "Not implemented: FR-1/AC-2 — assert DaekningRecord.prioritetKategori == "
            + prioritetKategori
            + " for fordring "
            + fordringId);
  }

  // =========================================================================
  // FR-2 — Within-category FIFO ordering (GIL § 4, stk. 2)
  // AC-3: FIFO by modtagelsesdato ascending within same PrioritetKategori
  // AC-4: legacyModtagelsesdato supersedes overdragelse date for pre-2013 fordringer;
  //       FIFO tie-break by sekvensNummer ascending
  // Component: DaekningsRaekkefoeigenService.buildOrderedList
  // =========================================================================

  /**
   * Seeds fordringer all sharing the same priority category for FIFO ordering tests.
   *
   * <p>W-1: DataTable may include an optional {@code sekvensNummer} column (SKY-3027 tie-break).
   * The step implementation must map it to the entity's {@code sekvensNummer} field.
   *
   * <p>FR-2: AC-3, AC-4
   */
  @Given("^debtor \"([^\"]+)\" has the following active fordringer in the same priority category:$")
  public void debtorHasFordringerInSamePriorityCategory(String debtorId, DataTable table) {
    // FR-2: AC-3, AC-4 — seed fordringer sharing the same PrioritetKategori
    // W-1: map sekvensNummer column (SKY-3027) to entity field for FIFO tie-breaking
    fail(
        "Not implemented: FR-2 — seed same-category fordringer for debtor "
            + debtorId
            + ". Columns may include optional sekvensNummer (SKY-3027 tie-break).");
  }

  /**
   * Sets a legacyModtagelsesdato on a fordring to simulate a pre-2013 migration record. The
   * parenthetical "(before 1 September 2013)" is part of the step text.
   *
   * <p>FR-2: AC-4
   */
  @Given(
      "^fordring \"([^\"]+)\" has a legacyModtagelsesdato of \"([^\"]+)\""
          + " \\(before 1 September 2013\\)$")
  public void fordringHasLegacyModtagelsesdato(String fordringId, String legacyDate) {
    // FR-2: AC-4 — migration cutoff: 2013-09-01
    // legacyModtagelsesdato supersedes the overdragelse modtagelsesdato as the FIFO sort key
    fail(
        "Not implemented: FR-2/AC-4 — set legacyModtagelsesdato \""
            + legacyDate
            + "\" on fordring "
            + fordringId
            + " (pre-2013 migration cutoff: 2013-09-01)");
  }

  /**
   * Asserts that the pre-2013 legacy-keyed fordring is covered first (lowest FIFO sort key).
   *
   * <p>FR-2: AC-4
   */
  @Then("fordring {string} is covered first using legacyModtagelsesdato {string} as the sort key")
  public void fordringIsCoveredFirstUsingLegacyModtagelsesdato(
      String fordringId, String legacyDate) {
    // FR-2: AC-4 — DaekningRecord created for fordringId with fifoSortKey == legacyDate
    fail(
        "Not implemented: FR-2/AC-4 — assert fordring "
            + fordringId
            + " covered first using legacyModtagelsesdato "
            + legacyDate
            + " as FIFO sort key");
  }

  /**
   * Asserts that the post-2013 fordring (using overdragelse modtagelsesdato) is covered second.
   *
   * <p>FR-2: AC-4
   */
  @Then("fordring {string} is covered second using its overdragelse modtagelsesdato")
  public void fordringIsCoveredSecondUsingOverdragesleModtagelsesdato(String fordringId) {
    // FR-2: AC-4 — DaekningRecord for fordringId created after the legacy-keyed fordring
    fail(
        "Not implemented: FR-2/AC-4 — assert fordring "
            + fordringId
            + " covered second (overdragelse modtagelsesdato as FIFO sort key)");
  }

  /**
   * Asserts the fifoSortKey field returned by the GET /daekningsraekkefoelge API.
   *
   * <p>FR-2: AC-4 | FR-7: AC-14
   */
  @Then("the API response for {string} contains fifoSortKey {string}")
  public void apiResponseContainsFifoSortKey(String fordringId, String expectedKey) {
    // FR-2: AC-4 — GET /daekningsraekkefoelge: position.fifoSortKey == expectedKey for fordringId
    fail(
        "Not implemented: FR-2/AC-4 — assert API response position.fifoSortKey == \""
            + expectedKey
            + "\" for fordring "
            + fordringId);
  }

  // =========================================================================
  // FR-3 — Interest ordering within each fordring (Gæld.bekendtg. § 4, stk. 3)
  // AC-5: sub-position order: OPKRAEVNINGSRENTER(1) → ... → HOOFDFORDRING(6)
  // AC-6: all six sub-positions covered in strict ascending ordinal order
  // Component: DaekningsRaekkefoeigenService, RenteKomponent enum
  // =========================================================================

  /**
   * Seeds the outstanding cost components for a specific fordring.
   *
   * <p>DataTable columns: {@code komponent, beloeb}. Used in SKY-3007 and SKY-3009.
   *
   * <p>FR-3: AC-5, AC-6
   */
  @Given(
      "^debtor \"([^\"]+)\" has fordring \"([^\"]+)\" with the following outstanding components:$")
  public void debtorHasFordringWithOutstandingComponents(
      String debtorId, String fordringId, DataTable table) {
    // FR-3: AC-5, AC-6 — seed RenteKomponent sub-positions with outstanding amounts
    // RenteKomponent ordinals: OPKRAEVNINGSRENTER=0, INDDRIVELSESRENTER_FORDRINGSHAVER_STK3=1,
    //   INDDRIVELSESRENTER_FOER_TILBAGEFOERSEL=2, INDDRIVELSESRENTER_STK1=3,
    //   OEVRIGE_RENTER_PSRM=4, HOOFDFORDRING=5
    fail(
        "Not implemented: FR-3 — seed outstanding RenteKomponent sub-positions for fordring "
            + fordringId
            + " under debtor "
            + debtorId);
  }

  /**
   * Applies a payment directly to a specific fordring (within-fordring sub-position allocation).
   *
   * <p>FR-3: AC-5, AC-6
   */
  @When("^a payment of (\\d+(?:\\.\\d+)?) DKK is applied to fordring \"([^\"]+)\"$")
  public void aPaymentIsAppliedToFordring(BigDecimal beloeb, String fordringId) {
    // FR-3: AC-5, AC-6 — DaekningsRaekkefoeigenService.applyWithinFordring(fordringId, beloeb)
    fail(
        "Not implemented: FR-3 — apply payment "
            + beloeb
            + " DKK to fordring "
            + fordringId
            + " (within-fordring sub-position allocation)");
  }

  /**
   * Asserts OPKRAEVNINGSRENTER (sub-position 1) is fully covered first. The parenthetical amount in
   * the step text is captured but informational only.
   *
   * <p>FR-3: AC-5
   */
  @Then("^opkrævningsrenter \\((\\d+(?:\\.\\d+)?)\\) are fully covered first$")
  public void opkraevningsrenterAreFullyCoveredFirst(BigDecimal expectedBeloeb) {
    // FR-3: AC-5 — DaekningRecord for RenteKomponent.OPKRAEVNINGSRENTER has daekningBeloeb ==
    // expectedBeloeb
    // OPKRAEVNINGSRENTER is sub-position 1 (ordinal 0) — lowest ordinal → covered first
    fail(
        "Not implemented: FR-3/AC-5 — assert OPKRAEVNINGSRENTER (sub-pos 1) fully covered with "
            + expectedBeloeb
            + " DKK first");
  }

  /**
   * Asserts INDDRIVELSESRENTER_STK1 (sub-position 4) is fully covered second after
   * OPKRAEVNINGSRENTER.
   *
   * <p>FR-3: AC-5
   */
  @Then("^inddrivelsesrenter_stk1 \\((\\d+(?:\\.\\d+)?)\\) are fully covered second$")
  public void inddrivelsesrenterStk1AreFullyCoveredSecond(BigDecimal expectedBeloeb) {
    // FR-3: AC-5 — DaekningRecord for RenteKomponent.INDDRIVELSESRENTER_STK1 (ordinal 3)
    fail(
        "Not implemented: FR-3/AC-5 — assert INDDRIVELSESRENTER_STK1 (sub-pos 4) covered second with "
            + expectedBeloeb
            + " DKK");
  }

  /**
   * Asserts the HOOFDFORDRING component (sub-position 6) received no dækning.
   *
   * <p>FR-3: AC-5
   */
  @Then("Hoofdfordring receives no dækning")
  public void hoofdfordringReceivesNoDaekning() {
    // FR-3: AC-5 — no DaekningRecord for RenteKomponent.HOOFDFORDRING (ordinal 5, last
    // sub-position)
    fail("Not implemented: FR-3/AC-5 — assert no DaekningRecord for RenteKomponent.HOOFDFORDRING");
  }

  /**
   * Asserts no DaekningRecord exists for the named komponent with a positive amount.
   *
   * <p>FR-3: AC-5
   */
  @Then("no dækning record has komponent {string} with beloeb > 0")
  public void noDaekningRecordHasKomponentWithPositiveBeloeb(String komponent) {
    // FR-3: AC-5 — assert DaekningRecord where komponent == RenteKomponent.valueOf(komponent)
    //              has daekningBeloeb == 0 or does not exist
    fail(
        "Not implemented: FR-3/AC-5 — assert no DaekningRecord with komponent \""
            + komponent
            + "\" and daekningBeloeb > 0");
  }

  /**
   * Seeds all six cost components for a fordring (full sub-position coverage test SKY-3008).
   *
   * <p>DataTable columns: {@code sub-position, komponent, beloeb}
   *
   * <p>FR-3: AC-6
   */
  @Given(
      "^debtor \"([^\"]+)\" has fordring \"([^\"]+)\" with all six cost components outstanding:$")
  public void debtorHasFordringWithAllSixCostComponents(
      String debtorId, String fordringId, DataTable table) {
    // FR-3: AC-6 — seed all 6 RenteKomponent sub-positions for full-sequence test
    // Expected ordinal order: OPKRAEVNINGSRENTER(1) → INDDRIVELSESRENTER_FORDRINGSHAVER_STK3(2)
    //   → INDDRIVELSESRENTER_FOER_TILBAGEFOERSEL(3) → INDDRIVELSESRENTER_STK1(4)
    //   → OEVRIGE_RENTER_PSRM(5) → HOOFDFORDRING(6)
    fail(
        "Not implemented: FR-3/AC-6 — seed all six RenteKomponent sub-positions for fordring "
            + fordringId);
  }

  /**
   * Asserts that sub-positions 1–5 (all interest components) are fully covered in ascending order.
   *
   * <p>FR-3: AC-6
   */
  @Then(
      "^sub-positions 1 through 5 are fully covered in ascending order \\(total (\\d+(?:\\.\\d+)?) DKK\\)$")
  public void subPositions1Through5AreFullyCovered(BigDecimal total) {
    // FR-3: AC-6 — sum of daekningBeloeb for sub-positions 1..5 == total
    //              each sub-position DaekningRecord has daekningBeloeb == its outstanding amount
    fail(
        "Not implemented: FR-3/AC-6 — assert sub-positions 1–5 fully covered in ascending order,"
            + " total "
            + total
            + " DKK");
  }

  /**
   * Asserts the HOOFDFORDRING receives the specified residual dækning after renter are covered.
   *
   * <p>FR-3: AC-6
   */
  @Then(
      "^the Hoofdfordring receives (\\d+(?:\\.\\d+)?) DKK dækning \\(the remaining amount after renter\\)$")
  public void hoofdfordringReceivesRemainingDaekning(BigDecimal expectedBeloeb) {
    // FR-3: AC-6 — DaekningRecord for HOOFDFORDRING (sub-pos 6) has daekningBeloeb ==
    // expectedBeloeb
    fail(
        "Not implemented: FR-3/AC-6 — assert HOOFDFORDRING receives "
            + expectedBeloeb
            + " DKK (remaining after sub-positions 1–5)");
  }

  /**
   * Asserts that the line-item allocation records are ordered by sub-position 1 through 6.
   *
   * <p>FR-3: AC-6
   */
  @Then("^the line-item allocation records are ordered by sub-position 1 → 2 → 3 → 4 → 5 → 6$")
  public void lineItemAllocationRecordsAreOrdered() {
    // FR-3: AC-6 — DaekningRecord list is sorted ascending by RenteKomponent.ordinal()
    fail(
        "Not implemented: FR-3/AC-6 — assert DaekningRecord list ordered by RenteKomponent ordinal (1→2→3→4→5→6)");
  }

  /**
   * Seeds a fordring with two INDDRIVELSESRENTER_STK1 interest periods for chronological
   * within-sub-position ordering (SKY-3009).
   *
   * <p>DataTable columns: {@code periode, beloeb}
   *
   * <p>FR-3: AC-5
   */
  @Given(
      "^debtor \"([^\"]+)\" has fordring \"([^\"]+)\" with two INDDRIVELSESRENTER_STK1 periods:$")
  public void debtorHasFordringWithTwoInddrivelsesrenterPeriods(
      String debtorId, String fordringId, DataTable table) {
    // FR-3: AC-5 — within the same sub-position, earliest period covered first (chronological)
    fail(
        "Not implemented: FR-3 — seed two INDDRIVELSESRENTER_STK1 periods for fordring "
            + fordringId
            + " under debtor "
            + debtorId);
  }

  /**
   * Applies a residual payment to a fordring, assuming opkrævningsrenter have already been consumed
   * by prior steps in this scenario.
   *
   * <p>FR-3: AC-5
   */
  @When(
      "^a payment of (\\d+(?:\\.\\d+)?) DKK reaches fordring \"([^\"]+)\" after opkrævningsrenter are covered$")
  public void aPaymentReachesFordringAfterOpkraevningsrenter(BigDecimal beloeb, String fordringId) {
    // FR-3: AC-5 — apply residual payment (after OPKRAEVNINGSRENTER consumed) to
    // INDDRIVELSESRENTER_STK1
    fail(
        "Not implemented: FR-3 — apply residual "
            + beloeb
            + " DKK to INDDRIVELSESRENTER_STK1 on fordring "
            + fordringId
            + " (after opkrævningsrenter covered)");
  }

  /**
   * Asserts the 2023-Q1 interest period is fully covered first.
   *
   * <p>FR-3: AC-5
   */
  @Then("^the 2023-Q1 inddrivelsesrente period \\((\\d+(?:\\.\\d+)?)\\) is fully covered first$")
  public void period2023Q1IsFullyCoveredFirst(BigDecimal expectedBeloeb) {
    // FR-3: AC-5 — earliest interest period (2023-Q1) DaekningRecord.daekningBeloeb ==
    // expectedBeloeb
    fail(
        "Not implemented: FR-3/AC-5 — assert 2023-Q1 INDDRIVELSESRENTER_STK1 period fully covered first with "
            + expectedBeloeb
            + " DKK");
  }

  /**
   * Asserts the 2023-Q2 interest period is partially covered with the specified amount.
   *
   * <p>FR-3: AC-5
   */
  @Then("the 2023-Q2 period is partially covered with {double} DKK")
  public void period2023Q2IsPartiallyCovered(Double expectedBeloeb) {
    // FR-3: AC-5 — later period (2023-Q2) DaekningRecord.daekningBeloeb == expectedBeloeb (partial)
    fail(
        "Not implemented: FR-3/AC-5 — assert 2023-Q2 INDDRIVELSESRENTER_STK1 period partially covered with "
            + expectedBeloeb
            + " DKK");
  }

  // =========================================================================
  // FR-4 — Inddrivelsesindsats rule (GIL § 4, stk. 3)
  // AC-7: LOENINDEHOLDELSE — indsats-fordringer covered first; surplus to eligible fordringer
  // AC-8: UDLAEG — surplus stays isolated; does NOT flow to non-udlaeg fordringer
  // Component: DaekningsRaekkefoeigenService, InddrivelsesindsatsType enum
  // =========================================================================

  /**
   * Triggers a lønindeholdelse payment.
   *
   * <p>W-1: {@code inLoenindeholdelsesIndsats} column in the Given DataTable maps to the fordring
   * entity flag that marks which fordringer belong to the current lønindeholdelse indsats.
   *
   * <p>FR-4: AC-7
   */
  @When(
      "^a lønindeholdelse payment of (\\d+(?:\\.\\d+)?) DKK is received with inddrivelsesindsatsType \"([^\"]+)\"$")
  public void aLoenindeholdelsePaymentIsReceived(BigDecimal beloeb, String indsatsType) {
    // FR-4: AC-7 — DaekningsRaekkefoeigenService.applyPayment with
    // InddrivelsesindsatsType.LOENINDEHOLDELSE
    // W-1: inLoenindeholdelsesIndsats=true fordringer are covered first; surplus → eligible others
    fail(
        "Not implemented: FR-4/AC-7 — receive "
            + indsatsType
            + " payment of "
            + beloeb
            + " DKK. Implement InddrivelsesindsatsType.LOENINDEHOLDELSE routing in DaekningsRaekkefoeigenService.");
  }

  /**
   * Asserts that an indsats-fordring (inLoenindeholdelsesIndsats=true) is covered first.
   *
   * <p>FR-4: AC-7
   */
  @Then(
      "^fordring \"([^\"]+)\" \\(indsats-fordring\\) is fully covered with (\\d+(?:\\.\\d+)?) DKK first$")
  public void indsatsFordringIsFullyCoveredFirst(String fordringId, BigDecimal expectedBeloeb) {
    // FR-4: AC-7 — fordring with inLoenindeholdelsesIndsats=true receives dækning before others
    fail(
        "Not implemented: FR-4/AC-7 — assert indsats-fordring "
            + fordringId
            + " fully covered with "
            + expectedBeloeb
            + " DKK (covered before surplus distribution)");
  }

  /**
   * Asserts that the LOENINDEHOLDELSE surplus is applied to an eligible non-indsats fordring.
   *
   * <p>FR-4: AC-7
   */
  @Then(
      "^surplus (\\d+(?:\\.\\d+)?) DKK is applied to fordring \"([^\"]+)\" \\(same-type-eligible\\)$")
  public void surplusIsAppliedToEligibleFordring(BigDecimal surplus, String fordringId) {
    // FR-4: AC-7 — DaekningRecord for fordringId has daekningBeloeb == surplus
    // Eligible: fordringer in the same inddrivelsesindsats type (LOENINDEHOLDELSE)
    fail(
        "Not implemented: FR-4/AC-7 — assert surplus "
            + surplus
            + " DKK applied to eligible fordring "
            + fordringId);
  }

  /**
   * Asserts that all dækning records for this payment carry the specified gilParagraf.
   *
   * <p>FR-4: AC-7, AC-8
   */
  @Then("^each dækning record carries gilParagraf \"([^\"]+)\"$")
  public void eachDaekningRecordCarriesGilParagraf(String expectedGilParagraf) {
    // FR-4: AC-7, AC-8 — all DaekningRecords created in this payment have gilParagraf == "GIL § 4,
    // stk. 3"
    fail(
        "Not implemented: FR-4 — assert all DaekningRecords.gilParagraf == \""
            + expectedGilParagraf
            + "\" (should be GIL § 4, stk. 3 for inddrivelsesindsats payments)");
  }

  /**
   * Triggers a udlæg payment.
   *
   * <p>W-1: {@code inUdlaegForretning} column in the Given DataTable marks udlæg-fordringer.
   *
   * <p>FR-4: AC-8
   */
  @When(
      "^an udlæg payment of (\\d+(?:\\.\\d+)?) DKK is received with inddrivelsesindsatsType \"([^\"]+)\"$")
  public void anUdlaegPaymentIsReceived(BigDecimal beloeb, String indsatsType) {
    // FR-4: AC-8 — DaekningsRaekkefoeigenService.applyPayment with InddrivelsesindsatsType.UDLAEG
    // Surplus from UDLAEG must NOT flow to non-udlaeg fordringer (Retsplejelovens § 507)
    fail(
        "Not implemented: FR-4/AC-8 — receive "
            + indsatsType
            + " payment of "
            + beloeb
            + " DKK. Implement UDLAEG surplus isolation in DaekningsRaekkefoeigenService.");
  }

  /**
   * Asserts that the UDLAEG surplus is flagged and isolated (udlaegSurplus = true).
   *
   * <p>FR-4: AC-8
   */
  @Then("^the remaining (\\d+(?:\\.\\d+)?) DKK surplus is flagged as udlaegSurplus = true$")
  public void remainingSurplusIsFlaggedAsUdlaegSurplus(BigDecimal surplus) {
    // FR-4: AC-8 — DaekningResult.udlaegSurplus == true and udlaegSurplusAmount == surplus
    // Surplus must NOT be applied to fordringer outside the udlæg forretning
    fail(
        "Not implemented: FR-4/AC-8 — assert "
            + surplus
            + " DKK surplus flagged as udlaegSurplus=true in DaekningResult");
  }

  /**
   * Asserts no DaekningRecord exists for a specific fordring (used for UDLAEG isolation in FR-4).
   *
   * <p>FR-4: AC-8
   */
  @Then("no dækning record exists for fordring {string}")
  public void noDaekningRecordExistsForFordring(String fordringId) {
    // FR-4: AC-8 — UDLAEG surplus is NOT applied to non-udlaeg fordringer
    // assert DaekningRecord repository has no entry for fordringId in this payment
    fail(
        "Not implemented: FR-4/AC-8 — assert no DaekningRecord exists for fordring "
            + fordringId
            + " (UDLAEG surplus isolation)");
  }

  // =========================================================================
  // FR-5 — Opskrivningsfordring positioning
  // AC-9: opskrivningsfordring placed immediately after its stamfordring in the ordered list
  // AC-10: opskrivningsfordring inherits parent's FIFO sort key when parent is fully covered
  // AC-11: multiple opskrivningsfordringer for same parent ordered among themselves by FIFO
  // Component: DaekningsRaekkefoeigenService.buildOrderedList, opskrivningAfFordringId link
  // =========================================================================

  /**
   * Triggers the GET /daekningsraekkefoelge endpoint and stores the result for Then assertions.
   *
   * <p>FR-5: AC-9, AC-10, AC-11 | FR-7: AC-14
   */
  @When("the dækningsrækkefølge ordered list is retrieved for debtor {string}")
  public void orderedListIsRetrievedForDebtor(String debtorId) {
    // FR-5: AC-9, AC-10, AC-11 — DaekningsRaekkefoeigenService.buildOrderedList(debtorId)
    // Also exercised by FR-7 GET /debtors/{debtorId}/daekningsraekkefoelge
    fail(
        "Not implemented: FR-5 — retrieve ordered list for debtor "
            + debtorId
            + " via DaekningsRaekkefoeigenService.buildOrderedList()");
  }

  /**
   * Asserts the full ordered list matches the expected rank/fordringId table.
   *
   * <p>FR-5: AC-9, AC-11
   */
  @Then("^the ordered list is:$")
  public void theOrderedListIs(DataTable expectedTable) {
    // FR-5: AC-9, AC-11 — assert ordered list positions match expected rank and fordringId sequence
    fail("Not implemented: FR-5 — assert ordered list matches expected rank/fordringId/note table");
  }

  /**
   * Asserts the opskrivningAfFordringId field on a position in the API response.
   *
   * <p>FR-5: AC-9
   */
  @Then("^the entry for \"([^\"]+)\" carries opskrivningAfFordringId \"([^\"]+)\"$")
  public void entryCarriesOpskrivningAfFordringId(String fordringId, String expectedParentId) {
    // FR-5: AC-9 — GET response: position.opskrivningAfFordringId == expectedParentId
    fail(
        "Not implemented: FR-5/AC-9 — assert position for "
            + fordringId
            + " has opskrivningAfFordringId == \""
            + expectedParentId
            + "\"");
  }

  /**
   * Seeds fordringer including those with opskrivningAfFordringId linkage.
   *
   * <p>DataTable columns: {@code fordringId, kategori, tilbaestaaendeBeloeb, modtagelsesdato,
   * opskrivningAfFordringId}. Empty string {@code ""} means not an opskrivningsfordring.
   *
   * <p>FR-5: AC-9, AC-10, AC-11
   */
  @Given("^debtor \"([^\"]+)\" has the following fordringer:$")
  public void debtorHasFollowingFordringer(String debtorId, DataTable table) {
    // FR-5: AC-9, AC-10, AC-11 — general fordring setup; opskrivningAfFordringId="" means
    // stamfordring
    fail(
        "Not implemented: FR-5 — seed fordringer with opskrivningAfFordringId linkage for debtor "
            + debtorId
            + ". Empty string opskrivningAfFordringId = stamfordring (not an opskrivningsfordring).");
  }

  /**
   * Asserts the opskrivningsfordring (whose parent is fully covered) appears at rank 1, inheriting
   * the parent's FIFO sort key.
   *
   * <p>FR-5: AC-10
   */
  @Then(
      "^the ordered list includes \"([^\"]+)\" at rank 1 \\(inheriting parent's FIFO sort key (\\d{4}-\\d{2}-\\d{2})\\)$")
  public void orderedListIncludesAtRank1InheritingParentFifoSortKey(
      String fordringId, String parentFifoKey) {
    // FR-5: AC-10 — opskrivningsfordring inherits stamfordring FIFO sort key when parent
    // tilbaestaaendeBeloeb == 0
    fail(
        "Not implemented: FR-5/AC-10 — assert "
            + fordringId
            + " at rank 1 with inherited parent FIFO sort key "
            + parentFifoKey);
  }

  /**
   * Asserts a fordring is at the specified rank with a given FIFO sort key date.
   *
   * <p>FR-5: AC-10
   */
  @Then("^\"([^\"]+)\" is at rank (\\d+) \\(FIFO (\\d{4}-\\d{2}-\\d{2})\\)$")
  public void fordringIsAtRankWithFifoKey(String fordringId, int rank, String fifoDate) {
    // FR-5: AC-10 — assert ordered list position[rank-1].fordringId == fordringId
    //                             and position[rank-1].fifoSortKey == fifoDate
    fail(
        "Not implemented: FR-5/AC-10 — assert "
            + fordringId
            + " at rank "
            + rank
            + " with FIFO sort key "
            + fifoDate);
  }

  /**
   * Asserts a fully-covered fordring (tilbaestaaendeBeloeb == 0) is absent from the ordered list.
   *
   * <p>FR-5: AC-10
   */
  @Then("^\"([^\"]+)\" is not present \\(fully covered, saldo = 0\\)$")
  public void fordringIsNotPresentFullyCovered(String fordringId) {
    // FR-5: AC-10 — fordringer with tilbaestaaendeBeloeb == 0 excluded from ordered list
    fail(
        "Not implemented: FR-5/AC-10 — assert fordring "
            + fordringId
            + " is absent from ordered list (tilbaestaaendeBeloeb == 0)");
  }

  /**
   * Seeds outstanding cost components on a specific fordring (SKY-3029 opskrivningsfordring test).
   * This is the two-argument variant used when only the fordringId is referenced (not the debtor).
   *
   * <p>DataTable columns: {@code komponent, beloeb}
   *
   * <p>FR-5 / SKY-3029
   */
  @Given("^fordring \"([^\"]+)\" has the following outstanding components:$")
  public void fordringHasFollowingOutstandingComponents(String fordringId, DataTable table) {
    // FR-5 / SKY-3029 — seed RenteKomponent sub-positions on an opskrivningsfordring
    // FR-3 interest sequence applies within the opskrivningsfordring (INDDRIVELSESRENTER before
    // HOOFDFORDRING)
    fail(
        "Not implemented: FR-5 — seed outstanding cost components for opskrivningsfordring "
            + fordringId);
  }

  /**
   * Applies a payment directly to an opskrivningsfordring (SKY-3029).
   *
   * <p>FR-5 / SKY-3029
   */
  @When("^a payment of (\\d+(?:\\.\\d+)?) DKK is applied to opskrivningsfordring \"([^\"]+)\"$")
  public void aPaymentIsAppliedToOpskrivningsfordring(BigDecimal beloeb, String fordringId) {
    // FR-5 / SKY-3029 — FR-3 interest sequence applies within opskrivningsfordring
    fail(
        "Not implemented: FR-5 — apply "
            + beloeb
            + " DKK to opskrivningsfordring "
            + fordringId
            + " (FR-3 interest ordering applies)");
  }

  /**
   * Asserts INDDRIVELSESRENTER_STK1 on a fordring received the specified dækning.
   *
   * <p>FR-5 / SKY-3029
   */
  @Then(
      "^INDDRIVELSESRENTER_STK1 on fordring \"([^\"]+)\" receives (\\d+(?:\\.\\d+)?) DKK dækning$")
  public void inddrivelsesrenterStk1OnFordringReceivesDaekning(
      String fordringId, BigDecimal expectedBeloeb) {
    // FR-5 / SKY-3029 — DaekningRecord: komponent=INDDRIVELSESRENTER_STK1, fordringId,
    // daekningBeloeb==expectedBeloeb
    fail(
        "Not implemented: FR-5 — assert INDDRIVELSESRENTER_STK1 on fordring "
            + fordringId
            + " receives "
            + expectedBeloeb
            + " DKK dækning");
  }

  /**
   * Asserts the HOOFDFORDRING component on a fordring received no dækning.
   *
   * <p>FR-5 / SKY-3029
   */
  @Then("^HOOFDFORDRING on fordring \"([^\"]+)\" receives no dækning$")
  public void hoofdfordringOnFordringReceivesNoDaekning(String fordringId) {
    // FR-5 / SKY-3029 — no DaekningRecord with komponent=HOOFDFORDRING for fordringId
    fail(
        "Not implemented: FR-5 — assert HOOFDFORDRING on fordring "
            + fordringId
            + " has no DaekningRecord (blocked by INDDRIVELSESRENTER_STK1)");
  }

  // =========================================================================
  // FR-6 — Timing (GIL § 4, stk. 4)
  // AC-12: all fordringer registered before applicationTimestamp are included in the ordering
  // AC-13: DaekningRecord must carry both betalingstidspunkt and applicationTimestamp
  // Component: DaekningsRaekkefoeigenService (deterministic Clock injection — W-1 fix)
  // =========================================================================

  /**
   * Seeds a fordring with a specific registration timestamp for the timing test (SKY-3016).
   *
   * <p>FR-6: AC-12
   */
  @Given("debtor {string} has fordring {string} received at {string}")
  public void debtorHasFordringReceivedAt(String debtorId, String fordringId, String receivedAt) {
    // FR-6: AC-12 — fordring registered at receivedAt (before betalingstidspunkt)
    fail(
        "Not implemented: FR-6/AC-12 — seed fordring "
            + fordringId
            + " received at "
            + receivedAt
            + " for debtor "
            + debtorId);
  }

  /**
   * Records a payment event at a specific betalingstidspunkt (before application).
   *
   * <p>FR-6: AC-12
   */
  @Given("a payment is received for debtor {string} at betalingstidspunkt {string}")
  public void aPaymentIsReceivedAtBetalingstidspunkt(String debtorId, String betalingstidspunkt) {
    // FR-6: AC-12 — payment event timestamp (separate from applicationTimestamp)
    fail(
        "Not implemented: FR-6/AC-12 — record payment for debtor "
            + debtorId
            + " at betalingstidspunkt "
            + betalingstidspunkt);
  }

  /**
   * Registers a late-arriving fordring that arrives after betalingstidspunkt but before
   * applicationTimestamp (SKY-3016).
   *
   * <p>FR-6: AC-12
   */
  @Given(
      "^fordring \"([^\"]+)\" arrives at \"([^\"]+)\""
          + " \\(after betalingstidspunkt but before application\\)$")
  public void fordringArrivesAfterBetalingstidspunktBeforeApplication(
      String fordringId, String arrivalTimestamp) {
    // FR-6: AC-12 — late-arriving fordring: registered after betalingstidspunkt but
    //              before applicationTimestamp → must be included in the ordering snapshot
    fail(
        "Not implemented: FR-6/AC-12 — register late-arriving fordring "
            + fordringId
            + " at "
            + arrivalTimestamp
            + " (after betalingstidspunkt, before applicationTimestamp)");
  }

  /**
   * Applies the payment at a specific applicationTimestamp (injected via deterministic Clock).
   *
   * <p>W-1: Must inject a {@code Clock} bean fixed to {@code timestamp} — do NOT use {@code
   * Instant.now()}.
   *
   * <p>FR-6: AC-12
   */
  @When("the rule engine applies the payment at applicationTimestamp {string}")
  public void ruleEngineAppliesPaymentAtApplicationTimestamp(String timestamp) {
    // FR-6: AC-12 — applicationTimestamp injected via Clock bean fixed to timestamp
    // All fordringer registered on or before timestamp are included in the ordering snapshot
    fail(
        "Not implemented: FR-6/AC-12 — apply payment at applicationTimestamp "
            + timestamp
            + ". W-1: inject deterministic Clock bean (do not use Instant.now()).");
  }

  /**
   * Asserts that both fordringer appear in the ordering (late-arrival included).
   *
   * <p>FR-6: AC-12
   */
  @Then("both {string} and {string} are included in the ordering")
  public void bothFordringerAreIncludedInTheOrdering(String fordring1, String fordring2) {
    // FR-6: AC-12 — DaekningRecord exists for both fordring1 and fordring2
    fail(
        "Not implemented: FR-6/AC-12 — assert both "
            + fordring1
            + " and "
            + fordring2
            + " are included in the ordering (snapshot at applicationTimestamp)");
  }

  /**
   * Asserts all DaekningRecords carry the expected betalingstidspunkt.
   *
   * <p>FR-6: AC-12
   */
  @Then("the dækning records carry betalingstidspunkt {string}")
  public void daekningRecordsCarryBetalingstidspunkt(String expectedTimestamp) {
    // FR-6: AC-12 — all DaekningRecords.betalingstidspunkt == expectedTimestamp
    fail(
        "Not implemented: FR-6/AC-12 — assert all DaekningRecords.betalingstidspunkt == "
            + expectedTimestamp);
  }

  /**
   * Asserts all DaekningRecords carry the expected applicationTimestamp.
   *
   * <p>FR-6: AC-13
   */
  @Then("the dækning records carry applicationTimestamp {string}")
  public void daekningRecordsCarryApplicationTimestamp(String expectedTimestamp) {
    // FR-6: AC-13 — all DaekningRecords.applicationTimestamp == expectedTimestamp
    fail(
        "Not implemented: FR-6/AC-13 — assert all DaekningRecords.applicationTimestamp == "
            + expectedTimestamp);
  }

  /**
   * Seeds a single fordring with a known tilbaestaaendeBeloeb (SKY-3017 audit log test).
   *
   * <p>FR-6: AC-13
   */
  @Given("debtor {string} has fordring {string} with tilbaestaaendeBeloeb {double}")
  public void debtorHasFordringWithTilbaestaaendeBeloeb(
      String debtorId, String fordringId, Double beloeb) {
    // FR-6: AC-13 — single-fordring scenario to verify full DaekningRecord audit log field coverage
    fail(
        "Not implemented: FR-6/AC-13 — seed fordring "
            + fordringId
            + " with tilbaestaaendeBeloeb "
            + beloeb
            + " for debtor "
            + debtorId);
  }

  /**
   * Applies a payment to a debtor at a specific betalingstidspunkt (SKY-3017 When step).
   *
   * <p>FR-6: AC-13
   */
  @When("a payment of {double} DKK is applied to debtor {string} at betalingstidspunkt {string}")
  public void aPaymentIsAppliedToDebtorAtBetalingstidspunkt(
      Double beloeb, String debtorId, String betalingstidspunkt) {
    // FR-6: AC-13 — trigger full payment application to verify DaekningRecord audit log fields
    fail(
        "Not implemented: FR-6/AC-13 — apply payment "
            + beloeb
            + " DKK to debtor "
            + debtorId
            + " at betalingstidspunkt "
            + betalingstidspunkt);
  }

  /**
   * Asserts the DaekningRecord has the specified field values (SKY-3017).
   *
   * <p>DataTable columns: {@code field, value}. Expected fields: {@code daekningBeloeb,
   * betalingstidspunkt, applicationTimestamp, gilParagraf, prioritetKategori, fifoSortKey}.
   *
   * <p><strong>Note (W-1 / SKY-3017):</strong> The feature file uses {@code gilParagraf = "GIL § 4,
   * stk. 2"} which is an acknowledged error. The correct value per SPEC-P057 is the category-level
   * reference. This step accepts the feature-file value; the implementation must derive the correct
   * gilParagraf from PrioritetKategori.
   *
   * <p>FR-6: AC-13
   */
  @Then("^a dækning record is created for fordring \"([^\"]+)\" with:$")
  public void aDaekningRecordIsCreatedForFordringWith(String fordringId, DataTable table) {
    // FR-6: AC-13 — verify all 8 required DaekningRecord fields from the DataTable
    // Required: fordringId, komponent, daekningBeloeb, betalingstidspunkt, applicationTimestamp,
    //           gilParagraf, prioritetKategori, fifoSortKey
    // NOTE: gilParagraf "GIL § 4, stk. 2" in feature file is acknowledged Gherkin error
    // (W-1/SKY-3017).
    fail(
        "Not implemented: FR-6/AC-13 — assert DaekningRecord for "
            + fordringId
            + " has all 8 required audit fields from DataTable. "
            + "W-1/SKY-3017: gilParagraf value in feature file is acknowledged error; "
            + "implementation maps PrioritetKategori → correct GIL reference.");
  }

  /**
   * Asserts the CLS/immudb audit log contains an entry for the fordring with all eight fields.
   *
   * <p>FR-6: AC-13
   */
  @Then(
      "^the CLS audit log contains an entry for fordring \"([^\"]+)\" with all eight required fields:.*$")
  public void clsAuditLogContainsEntryWithEightRequiredFields(String fordringId) {
    // FR-6: AC-13 — immudb/CLS audit log entry must contain:
    //   fordringId, komponent, daekningBeloeb, betalingstidspunkt,
    //   applicationTimestamp, gilParagraf, prioritetKategori, fifoSortKey
    fail(
        "Not implemented: FR-6/AC-13 — assert CLS audit log entry for fordring "
            + fordringId
            + " contains all 8 required fields (ADR-0022, ADR-0029)");
  }

  // =========================================================================
  // FR-7 — Payment application API
  // AC-14: GET /debtors/{debtorId}/daekningsraekkefoelge — ordered position list
  // AC-15: POST /debtors/{debtorId}/daekningsraekkefoelge/simulate — dry-run allocation
  // OAuth2 scope: payment-service:read
  // =========================================================================

  /**
   * Seeds three fordringer in the same priority category with different modtagelsesdatoer
   * (SKY-3018).
   *
   * <p>FR-7: AC-14
   */
  @Given(
      "debtor {string} has three active fordringer in the same priority category with different modtagelsesdatoer")
  public void debtorHasThreeActiveFordringerWithDifferentModtagelsesdatoer(String debtorId) {
    // FR-7: AC-14 — seed 3 fordringer with distinct modtagelsesdatoer for FIFO ordering
    // verification
    fail(
        "Not implemented: FR-7/AC-14 — seed 3 fordringer with different modtagelsesdatoer for debtor "
            + debtorId);
  }

  /**
   * Calls GET /debtors/{debtorId}/daekningsraekkefoelge as an authenticated sagsbehandler with
   * {@code payment-service:read} OAuth2 scope.
   *
   * <p>FR-7: AC-14
   */
  @When("an authenticated sagsbehandler calls GET {string}")
  public void anAuthenticatedSagsbehandlerCallsGet(String path) {
    // FR-7: AC-14 — GET path with payment-service:read OAuth2 scope
    // Implement MockMvc / TestRestTemplate call; set lastHttpStatus and lastResponseBody
    fail(
        "Not implemented: FR-7/AC-14 — GET "
            + path
            + " with payment-service:read OAuth2 scope. "
            + "Implement MockMvc controller test for DaekningsRaekkefoeigenController.");
  }

  /**
   * Asserts the HTTP response status code of the most recent API call.
   *
   * <p>FR-7: AC-14, AC-15
   */
  @Then("the response status is {int}")
  public void theResponseStatusIs(Integer expectedStatus) {
    // FR-7: AC-14, AC-15 — assert lastHttpStatus == expectedStatus
    fail(
        "Not implemented: FR-7 — assert HTTP response status == "
            + expectedStatus
            + " (lastHttpStatus = "
            + lastHttpStatus
            + ")");
  }

  /**
   * Asserts the response body is a JSON array of ordered positions.
   *
   * <p>FR-7: AC-14
   */
  @Then("the response body is an ordered array of positions")
  public void theResponseBodyIsAnOrderedArrayOfPositions() {
    // FR-7: AC-14 — response body is JSON array ordered by prioritetKategori then fifoSortKey
    fail("Not implemented: FR-7/AC-14 — assert response body is ordered JSON array of positions");
  }

  /**
   * Asserts that each position in the response includes the specified fields (open-ended step text,
   * field list is captured but not individually validated here).
   *
   * <p>FR-7: AC-14
   */
  @Then("^each position includes fields:.*$")
  public void eachPositionIncludesFields() {
    // FR-7: AC-14 — each position must include:
    //   fordringId, fordringshaverId, prioritetKategori, komponent,
    //   tilbaestaaendeBeloeb, modtagelsesdato, fifoSortKey, gilParagraf
    fail(
        "Not implemented: FR-7/AC-14 — assert each position in the response includes all required fields: "
            + "fordringId, fordringshaverId, prioritetKategori, komponent, tilbaestaaendeBeloeb, "
            + "modtagelsesdato, fifoSortKey, gilParagraf");
  }

  /**
   * Asserts the response array ordering: prioritetKategori ascending, then fifoSortKey ascending.
   *
   * <p>FR-7: AC-14
   */
  @Then(
      "the array is ordered by prioritetKategori ascending, then by fifoSortKey ascending within each category")
  public void arrayIsOrderedByPrioritetKategoriThenFifoSortKey() {
    // FR-7: AC-14 — PrioritetKategori.ordinal() ascending; within category, fifoSortKey date
    // ascending
    fail(
        "Not implemented: FR-7/AC-14 — assert response array ordered by PrioritetKategori.ordinal() ASC, "
            + "then by fifoSortKey ASC within each category");
  }

  /**
   * Seeds a fordring that was outstanding on a historical date but fully covered since then (for
   * asOf query test SKY-3019).
   *
   * <p>FR-7: AC-14
   */
  @Given(
      "debtor {string} had fordring {string} outstanding on {string} but fully covered before today")
  public void debtorHadFordringOutstandingOnDateButCoveredNow(
      String debtorId, String fordringId, String asOfDate) {
    // FR-7: AC-14 — seed a fordring with historical balance snapshot for asOf query verification
    fail(
        "Not implemented: FR-7/AC-14 — seed historical fordring "
            + fordringId
            + " outstanding on "
            + asOfDate
            + " (fully covered today) for debtor "
            + debtorId);
  }

  /**
   * Asserts the asOf response includes the historical fordring with its historical balance.
   *
   * <p>FR-7: AC-14
   */
  @Then(
      "^the response includes fordring \"([^\"]+)\" with its historical tilbaestaaendeBeloeb as of (\\d{4}-\\d{2}-\\d{2})$")
  public void responseIncludesFordringWithHistoricalBalance(String fordringId, String asOfDate) {
    // FR-7: AC-14 — asOf query: response includes fordringId with historical tilbaestaaendeBeloeb
    //              (not the current zero balance)
    fail(
        "Not implemented: FR-7/AC-14 — assert response includes fordring "
            + fordringId
            + " with historical tilbaestaaendeBeloeb as of "
            + asOfDate);
  }

  /**
   * Seeds fordringer with a known total outstanding balance for simulate endpoint tests.
   *
   * <p>FR-7: AC-15
   */
  @Given("debtor {string} has fordringer with total outstanding {double} DKK")
  public void debtorHasFordringerWithTotalOutstanding(String debtorId, Double total) {
    // FR-7: AC-15 — seed fordringer whose sum of tilbaestaaendeBeloeb == total
    fail(
        "Not implemented: FR-7/AC-15 — seed fordringer with total outstanding "
            + total
            + " DKK for debtor "
            + debtorId);
  }

  /**
   * Calls POST /debtors/{debtorId}/daekningsraekkefoelge/simulate as an authenticated
   * sagsbehandler.
   *
   * <p>FR-7: AC-15
   */
  @When("an authenticated sagsbehandler calls POST {string}")
  public void anAuthenticatedSagsbehandlerCallsPost(String path) {
    // FR-7: AC-15 — POST path with payment-service:read OAuth2 scope
    // Body is set by the subsequent "And with body:" step
    fail(
        "Not implemented: FR-7/AC-15 — POST "
            + path
            + " with payment-service:read OAuth2 scope. "
            + "Implement MockMvc controller test for DaekningsRaekkefoeigenController.simulate().");
  }

  /**
   * Sets the request body for the preceding POST call. The DocString content is the JSON body.
   *
   * <p><strong>Note on "With body:" in canonical feature file:</strong> The canonical petition
   * feature file uses {@code With body:} as the step keyword, which is not valid Gherkin. This
   * module feature file uses {@code And with body:} as the corrected form.
   *
   * <p>FR-7: AC-15
   */
  @And("with body:")
  public void withBody(String requestBody) {
    // FR-7: AC-15 — set request body JSON for the POST /simulate or POST endpoint
    fail(
        "Not implemented: FR-7/AC-15 — set request body: "
            + requestBody.trim()
            + ". Wire this body into the preceding POST call in the step context.");
  }

  /**
   * Asserts each simulate response position includes daekningBeloeb and fullyCovers fields.
   *
   * <p>FR-7: AC-15
   */
  @Then("each position in the response includes daekningBeloeb and fullyCovers")
  public void eachPositionIncludesDaekningBeloebAndFullyCovers() {
    // FR-7: AC-15 — simulate response: each position has daekningBeloeb (BigDecimal) and
    // fullyCovers (boolean)
    fail(
        "Not implemented: FR-7/AC-15 — assert each simulate response position includes daekningBeloeb and fullyCovers");
  }

  /**
   * Asserts no DaekningRecord is persisted to the database (dry-run / simulate).
   *
   * <p>FR-7: AC-15
   */
  @Then("no DaekningRecord is persisted to the database")
  public void noDaekningRecordIsPersistedToDatabase() {
    // FR-7: AC-15 — simulate is a dry-run: DaekningRecord repository must remain empty
    fail(
        "Not implemented: FR-7/AC-15 — assert no DaekningRecord persisted to database (simulate is dry-run)");
  }

  /**
   * Asserts the sum of all daekningBeloeb values in the simulate response equals the payment
   * amount.
   *
   * <p>FR-7: AC-15
   */
  @Then("the total of all daekningBeloeb values equals {double}")
  public void totalOfAllDaekningBeloebValuesEquals(Double expectedTotal) {
    // FR-7: AC-15 — sum(position.daekningBeloeb) == expectedTotal across all positions
    fail(
        "Not implemented: FR-7/AC-15 — assert sum of all position.daekningBeloeb == "
            + expectedTotal);
  }

  /**
   * Seeds a minimal debtor record (no fordringer required — used for HTTP 4xx error tests).
   *
   * <p>FR-7: AC-14, AC-15
   */
  @Given("debtor {string} exists")
  public void debtorExists(String debtorId) {
    // FR-7: AC-14, AC-15 — minimal debtor existence check for 404/403/422 error scenarios
    fail(
        "Not implemented: FR-7 — ensure debtor "
            + debtorId
            + " exists in the system (minimal setup for error scenario)");
  }

  /**
   * Asserts the response body contains an RFC 7807 problem-detail with a validation error.
   *
   * <p>FR-7: AC-15
   */
  @Then("the response body contains a problem-detail with description of the validation failure")
  public void responseBodyContainsProblemDetailWithValidationFailure() {
    // FR-7: AC-15 — HTTP 422 with RFC 7807 problem-detail JSON body
    // Fields expected: type, title, status (422), detail (validation failure description)
    fail(
        "Not implemented: FR-7/AC-15 — assert response body is RFC 7807 problem-detail with validation failure description");
  }

  /**
   * Calls GET without the {@code payment-service:read} OAuth2 scope (expected to receive HTTP 403).
   *
   * <p>FR-7: AC-14
   */
  @When("a caller without payment-service:read scope calls GET {string}")
  public void aCallerWithoutScopeCallsGet(String path) {
    // FR-7: AC-14 — call GET path without payment-service:read scope → expect HTTP 403 Forbidden
    fail(
        "Not implemented: FR-7/AC-14 — call GET "
            + path
            + " without payment-service:read OAuth2 scope (expect 403 Forbidden)");
  }
}
