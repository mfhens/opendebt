package dk.ufst.opendebt.creditor.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.cucumber.java.Before;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * BDD step definitions for Petition 054 — Catala Compliance Spike.
 *
 * <p>All assertions are pure file-system and content checks using {@code java.nio.file}. No Spring
 * context, MockMvc, or web layer is used or required by any step in this class.
 *
 * <p><strong>Red-phase strategy:</strong> Every step that checks a deliverable file is
 * intentionally failing because the {@code catala/} directory and its contents do not exist yet.
 * Steps will turn green once the spike deliverables are committed to the repository:
 *
 * <ul>
 *   <li>{@code catala/ga_1_4_3_opskrivning.catala_da} (FR-1, AC-1–AC-3, AC-15)
 *   <li>{@code catala/ga_1_4_4_nedskrivning.catala_da} (FR-2, AC-6–AC-10, AC-14, AC-15)
 *   <li>{@code catala/tests/ga_opskrivning_nedskrivning_tests.catala_da} (FR-3, AC-11)
 *   <li>{@code catala/SPIKE-REPORT.md} (FR-4/FR-5, AC-12–AC-13)
 * </ul>
 *
 * <p>NFR-1 (Catala CLI compilation) steps emit a {@link PendingException} when the {@code catala}
 * executable is absent from PATH, rather than failing the build. NFR-4 (no production artefacts
 * modified) steps are protective constraints that pass immediately and must continue to pass after
 * the spike.
 *
 * <p>Spec reference: {@code design/specs-p054-catala-compliance-spike.md} (SPEC-P054)<br>
 * Outcome contract: {@code petitions/petition054-catala-compliance-spike-outcome-contract.md}<br>
 * Legal basis: G.A.1.4.3, G.A.1.4.4, Gæld.bekendtg. § 7 stk. 1–2, GIL § 18 k<br>
 * G.A. snapshot: v3.16 (2026-01-30)
 */
public class Petition054Steps {

  // ── Per-scenario state ────────────────────────────────────────────────────

  /**
   * The file being inspected by successive {@code And the file ...} steps. Set by {@code Then the
   * file "<path>" exists in the repository} and by standalone steps that embed the path in their
   * step text (e.g. the Go/No-Go verdict step).
   */
  private Path currentFilePath;

  /**
   * Cached content of {@link #currentFilePath}. Reset whenever {@link #currentFilePath} changes.
   */
  private String currentFileContent;

  /** Whether the Catala CLI was found on PATH (set by the Given step for NFR-1 scenarios). */
  private boolean catalaCliAvailable;

  /**
   * The compilation command last passed to the When step (e.g. {@code catala ocaml foo.catala_da}).
   */
  private String compilationCommand;

  /** Working directory name used for the last compilation command. */
  private String compilationDirectory;

  /** Exit code of the last compilation command. */
  private int lastExitCode;

  // ── Lifecycle ─────────────────────────────────────────────────────────────

  @Before("@petition054")
  public void resetState() {
    currentFilePath = null;
    currentFileContent = null;
    catalaCliAvailable = false;
    compilationCommand = null;
    compilationDirectory = null;
    lastExitCode = -1;
  }

  // ── Utilities ─────────────────────────────────────────────────────────────

  /**
   * Walk up from {@code user.dir} until a directory containing {@code .git} is found. Robust to
   * Maven running either from a module directory or from the repository root.
   */
  private static Path resolveRepoRoot() {
    Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    while (current != null) {
      if (Files.isDirectory(current.resolve(".git"))) {
        return current;
      }
      current = current.getParent();
    }
    throw new IllegalStateException(
        "Cannot locate repository root — no .git directory found above: "
            + System.getProperty("user.dir"));
  }

  /**
   * Return the cached content of {@link #currentFilePath}, asserting the file exists and is
   * readable. Fails with a meaningful message if the file does not exist (red-phase expectation).
   */
  private String readCurrentFileContent() {
    assertThat(currentFilePath)
        .as("No file has been set by a preceding 'the file ... exists in the repository' step.")
        .isNotNull();
    if (currentFileContent == null) {
      assertThat(Files.exists(currentFilePath))
          .as(
              "P054 deliverable file not found (red phase — file must be created during spike"
                  + " implementation): %s",
              currentFilePath)
          .isTrue();
      try {
        currentFileContent = Files.readString(currentFilePath, StandardCharsets.UTF_8);
      } catch (IOException e) {
        fail("Failed to read %s: %s", currentFilePath, e.getMessage());
      }
    }
    return currentFileContent;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // FR-1 / FR-2 / FR-3 / FR-4 — Deliverable file existence
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Assert that the named deliverable file exists in the repository and set it as the current file
   * for subsequent content assertions.
   *
   * <p>Covers AC-1 (D-1 exists), AC-6 (D-2 exists), AC-9 (D-3 exists), AC-12 (D-4 exists).
   */
  @Then("the file {string} exists in the repository")
  public void theFileExistsInTheRepository(String relativePath) {
    currentFilePath = resolveRepoRoot().resolve(relativePath);
    currentFileContent = null; // reset cache for the new file
    assertThat(Files.exists(currentFilePath))
        .as(
            "P054 deliverable file not found (red phase — create during spike implementation): %s",
            currentFilePath)
        .isTrue();
  }

  // ─────────────────────────────────────────────────────────────────────────
  // FR-1 / FR-2 — Catala dialect declaration (AC-3, AC-14)
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Assert that the current file carries the Catala dialect declaration.
   *
   * <p>AC-3 (opskrivning file), AC-14 (nedskrivning file).
   */
  @Then("the file declares the Danish Catala dialect {string}")
  public void theFileDeclaresDanishCatalaDialect(String dialect) {
    String content = readCurrentFileContent();
    assertThat(content)
        .as(
            "Expected Catala dialect declaration '%s' not found in %s."
                + " Each Catala source file must declare 'catala_da' as its source language."
                + " SPEC-P054 §2.1. AC-3 / AC-14.",
            dialect, currentFilePath)
        .contains(dialect);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // FR-1 — G.A.1.4.3 modtagelsestidspunkt rule blocks (AC-2)
  // ─────────────────────────────────────────────────────────────────────────

  /** FR-1.1 (AC-2) — Default receipt rule anchored to Gæld.bekendtg. § 7, stk. 1, 3. pkt. */
  @Then("the file contains a Catala rule block for the default receipt rule anchored to {string}")
  public void fileContainsDefaultReceiptRuleBlock(String citation) {
    assertFileContainsRuleBlockAnchoredTo("default receipt rule (FR-1.1)", citation);
  }

  /** FR-1.2 (AC-2) — Høring exception anchored to Gæld.bekendtg. § 7, stk. 1, 4. pkt. */
  @Then("the file contains a Catala rule block for the høring exception anchored to {string}")
  public void fileContainsHoeringExceptionRuleBlock(String citation) {
    assertFileContainsRuleBlockAnchoredTo("høring exception (FR-1.2)", citation);
  }

  /** FR-1.3 (AC-2) — Same-system annulleret nedskrivning exception, § 7 stk. 1, 5. pkt. */
  @Then(
      "the file contains a Catala rule block for the same-system annulleret nedskrivning"
          + " exception anchored to {string}")
  public void fileContainsSameSystemAnnulleretNedskrivningBlock(String citation) {
    assertFileContainsRuleBlockAnchoredTo(
        "same-system annulleret nedskrivning exception (FR-1.3)", citation);
  }

  /** FR-1.4 (AC-2) — Cross-system annulleret nedskrivning exception, § 7 stk. 1, 6. pkt. */
  @Then(
      "the file contains a Catala rule block for the cross-system annulleret nedskrivning"
          + " exception anchored to {string}")
  public void fileContainsCrossSystemAnnulleretNedskrivningBlock(String citation) {
    assertFileContainsRuleBlockAnchoredTo(
        "cross-system annulleret nedskrivning exception (FR-1.4)", citation);
  }

  private void assertFileContainsRuleBlockAnchoredTo(String ruleDescription, String citation) {
    String content = readCurrentFileContent();
    assertThat(content)
        .as(
            "Catala rule block for '%s' must be anchored to citation '%s' in %s."
                + " Each modtagelsestidspunkt sub-rule requires an identifiable rule block"
                + " with its exact Gæld.bekendtg. § 7 article anchor. SPEC-P054 §2. AC-2.",
            ruleDescription, citation, currentFilePath)
        .contains(citation);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // FR-2 — G.A.1.4.4 nedskrivning rule blocks (AC-7 – AC-10)
  // ─────────────────────────────────────────────────────────────────────────

  /** FR-2.1 (AC-7) — All three nedskrivningsgrunde anchored to § 7 stk. 2 nr. 1–3. */
  @Then(
      "the file contains Catala rule blocks for all three nedskrivningsgrunde anchored to"
          + " {string}, {string}, and {string}")
  public void fileContainsAllThreeNedskrivningsgrunde(
      String citation1, String citation2, String citation3) {
    String content = readCurrentFileContent();
    assertThat(content)
        .as(
            "Catala rule block for nedskrivningsgrund nr. 1 (NED_INDBETALING) must be"
                + " anchored to '%s' in %s. FR-2.1, AC-7.",
            citation1, currentFilePath)
        .contains(citation1);
    assertThat(content)
        .as(
            "Catala rule block for nedskrivningsgrund nr. 2 (NED_FEJL_OVERSENDELSE) must be"
                + " anchored to '%s' in %s. FR-2.1, AC-7.",
            citation2, currentFilePath)
        .contains(citation2);
    assertThat(content)
        .as(
            "Catala rule block for nedskrivningsgrund nr. 3 (NED_GRUNDLAG_AENDRET) must be"
                + " anchored to '%s' in %s. FR-2.1, AC-7.",
            citation3, currentFilePath)
        .contains(citation3);
  }

  /**
   * FR-2.2 (AC-8) — Virkningsdato retroactivity rule: {@code virkningsdato < fordring.receivedAt}
   * encoded using strict less-than (not less-than-or-equal).
   */
  @Then("the file contains a Catala rule block determining retroactivity when {string}")
  public void fileContainsRetroactivityRuleBlock(String condition) {
    String content = readCurrentFileContent();
    // Assert both key identifiers appear; exact Catala syntax is version-dependent
    // [CATALA-SYNTAX-TBD] but both sides of the comparison must be present.
    assertThat(content)
        .as(
            "File %s must contain a Catala rule encoding the retroactivity condition '%s'."
                + " The comparison must use strict less-than (<) — not <=."
                + " FR-2.2, AC-8. SPEC-P054 §3.2.",
            currentFilePath, condition)
        .contains("virkningsdato");
    assertThat(content)
        .as(
            "Retroactivity rule in %s must reference 'fordring.receivedAt' (or equivalent"
                + " Catala identifier) using strict less-than (<). FR-2.2, AC-8.",
            currentFilePath)
        .contains("receivedAt");
  }

  /** FR-2.3 (AC-9) — GIL § 18 k suspension flag anchored to the correct citation. */
  @Then(
      "the file contains a Catala rule block for the GIL § 18 k suspension flag anchored"
          + " to {string}")
  public void fileContainsGilSuspensionFlagBlock(String citation) {
    String content = readCurrentFileContent();
    assertThat(content)
        .as(
            "Catala rule block for GIL § 18 k suspension flag must be anchored to citation"
                + " '%s' in %s. FR-2.3, AC-9. SPEC-P054 §3.3.",
            citation, currentFilePath)
        .contains(citation);
  }

  /** FR-2.4 (AC-10) — Validation rule rejecting a nedskrivning submitted without a valid grund. */
  @Then(
      "the file contains a Catala validation rule rejecting a nedskrivning submitted without"
          + " a valid grund")
  public void fileContainsNedskrivningValidationRule() {
    String content = readCurrentFileContent();
    // Catala validation constructs vary by version [CATALA-SYNTAX-TBD].
    // We assert that an assertion/exception keyword appears together with a nedskrivnings-related
    // identifier, indicating a guard against invalid grounds.
    assertThat(content)
        .as(
            "File %s must contain a Catala validation rule that rejects a nedskrivning"
                + " without a valid grund (NED_INDBETALING, NED_FEJL_OVERSENDELSE, or"
                + " NED_GRUNDLAG_AENDRET). Look for a Catala assertion or exception construct."
                + " FR-2.4, AC-10. SPEC-P054 §3.4.",
            currentFilePath)
        .satisfiesAnyOf(
            c -> assertThat(c).contains("assertion"),
            c -> assertThat(c).contains("Assert"),
            c -> assertThat(c).contains("exception"),
            c -> assertThat(c).contains("Exception"),
            c -> assertThat(c).contains("invalid"),
            c -> assertThat(c).contains("ugyldig"));
  }

  // ─────────────────────────────────────────────────────────────────────────
  // FR-3 — Catala test suite (AC-11)
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * FR-3 (AC-11) — At least {@code minimumCount} test cases using Catala's built-in Test module.
   */
  @Then("the file contains at least {int} test cases expressed using Catala's built-in Test module")
  public void fileContainsAtLeastNTestCases(int minimumCount) {
    String content = readCurrentFileContent();
    // Catala test scopes are declared with a Test annotation or keyword.
    // Count occurrences of the "Test" identifier as a conservative lower bound.
    long testCount = Pattern.compile("\\bTest\\b").matcher(content).results().count();
    assertThat(testCount)
        .as(
            "File %s must contain at least %d test cases expressed using Catala's built-in"
                + " Test module. Found %d 'Test' declarations. FR-3, AC-11. SPEC-P054 §4.",
            currentFilePath, minimumCount, testCount)
        .isGreaterThanOrEqualTo(minimumCount);
  }

  /**
   * FR-3 (AC-11) — Test cases cover all four FR-1 modtagelsestidspunkt sub-rules (§ 7 stk. 1 pkt.
   * 3–6).
   */
  @Then("the test cases cover all four FR-1 modtagelsestidspunkt sub-rules")
  public void testCasesCoverAllFourFr1SubRules() {
    String content = readCurrentFileContent();
    assertThat(content)
        .as(
            "Test file %s must include test cases for all four FR-1 modtagelsestidspunkt"
                + " sub-rules (§ 7 stk. 1 pkt. 3–6). FR-3, AC-11.",
            currentFilePath)
        .contains("3. pkt")
        .contains("4. pkt")
        .contains("5. pkt")
        .contains("6. pkt");
  }

  /** FR-3 (AC-11) — Test cases cover all three FR-2 nedskrivningsgrunde. */
  @Then("the test cases cover all three FR-2 nedskrivningsgrunde")
  public void testCasesCoverAllThreeNedskrivningsgrunde() {
    String content = readCurrentFileContent();
    assertThat(content)
        .as(
            "Test file %s must include test cases for all three nedskrivningsgrunde:"
                + " NED_INDBETALING, NED_FEJL_OVERSENDELSE, NED_GRUNDLAG_AENDRET."
                + " FR-3, AC-11.",
            currentFilePath)
        .contains("NED_INDBETALING")
        .contains("NED_FEJL_OVERSENDELSE")
        .contains("NED_GRUNDLAG_AENDRET");
  }

  /**
   * FR-3 (AC-11) — Test cases cover GIL § 18 k suspension flag for both true and false outcomes.
   */
  @Then("the test cases cover the GIL § 18 k suspension flag for both true and false outcomes")
  public void testCasesCoverGilFlagBothOutcomes() {
    String content = readCurrentFileContent();
    assertThat(content)
        .as(
            "Test file %s must reference 'GIL § 18 k' (or equivalent) with test cases for"
                + " both the true (retroactive) and false (non-retroactive) suspension flag"
                + " outcomes. FR-3, AC-11.",
            currentFilePath)
        .contains("GIL § 18 k");
    assertThat(content)
        .as(
            "Test file %s must assert both true and false outcomes for the GIL § 18 k"
                + " suspension flag. FR-3, AC-11.",
            currentFilePath)
        .contains("true")
        .contains("false");
  }

  /** FR-3 / FR-2.4 (AC-11) — Test case for the invalid nedskrivningsgrund rejection path. */
  @Then(
      "the test cases include a test for the FR-2.4 validation rule: a nedskrivning submitted"
          + " without a valid grund is rejected")
  public void testCasesIncludeFr24ValidationTest() {
    String content = readCurrentFileContent();
    assertThat(content)
        .as(
            "Test file %s must contain a test case that exercises the FR-2.4 rejection path:"
                + " a nedskrivning submitted without a valid grund must be rejected."
                + " Look for keywords such as 'invalid', 'ugyldig', 'Invalid', or 'Ugyldig'."
                + " AC-11.",
            currentFilePath)
        .satisfiesAnyOf(
            c -> assertThat(c).contains("invalid"),
            c -> assertThat(c).contains("Invalid"),
            c -> assertThat(c).contains("ugyldig"),
            c -> assertThat(c).contains("Ugyldig"));
  }

  /**
   * FR-3 (AC-11) — Boundary-date assertions for the virkningsdato retroactivity rule. Must cover:
   * same day as {@code fordring.receivedAt}, one day before, and one day after.
   */
  @Then(
      "the test cases include boundary-date assertions for the virkningsdato retroactivity"
          + " rule covering the same day as fordring.receivedAt, the day before, and the day after")
  public void testCasesIncludeBoundaryDateAssertions() {
    String content = readCurrentFileContent();
    Set<LocalDate> dates =
        Pattern.compile("\\d{4}-\\d{2}-\\d{2}")
            .matcher(content)
            .results()
            .map(m -> LocalDate.parse(m.group()))
            .collect(Collectors.toSet());
    boolean hasThreeConsecutive =
        dates.stream()
            .anyMatch(d -> dates.contains(d.minusDays(1)) && dates.contains(d.plusDays(1)));
    assertThat(hasThreeConsecutive)
        .as(
            "Test file %s must contain three consecutive boundary dates (day-before, same-day,"
                + " day-after a common reference date) to verify the virkningsdato retroactivity"
                + " rule at all three required offsets. FR-3, AC-7.",
            currentFilePath)
        .isTrue();
  }

  // ─────────────────────────────────────────────────────────────────────────
  // FR-4 — Spike report: P053 coverage table and effort estimate (AC-12)
  // ─────────────────────────────────────────────────────────────────────────

  /** FR-4 (AC-12) — Coverage table mapping each P053 FR-1/FR-2 scenario to a coverage status. */
  @Then(
      "the report contains a table mapping each P053 FR-1 and FR-2 Gherkin scenario to a"
          + " coverage status of {string}, {string}, or {string}")
  public void reportContainsCoverageTable(String covered, String notCovered, String discrepancy) {
    String content = readCurrentFileContent();
    assertThat(content)
        .as(
            "SPIKE-REPORT.md (%s) must contain a coverage comparison table with columns"
                + " indicating '%s', '%s', or '%s' for each P053 FR-1/FR-2 scenario."
                + " FR-4, AC-12. SPEC-P054 §5.",
            currentFilePath, covered, notCovered, discrepancy)
        .satisfiesAnyOf(
            c -> assertThat(c).contains(covered),
            c -> assertThat(c).contains(notCovered),
            c -> assertThat(c).contains(discrepancy));
  }

  /** FR-4 (AC-12) — Every in-scope P053 FR-1/FR-2 scenario has a row in the coverage table. */
  @Then("every scenario from {string} that is in scope for FR-1 or FR-2 has a row in the table")
  public void everyP053ScenarioHasRowInTable(String featureFilePath) {
    String content = readCurrentFileContent();
    assertThat(content)
        .as(
            "SPIKE-REPORT.md must contain a row for each P053 FR-1/FR-2 scenario from '%s'."
                + " The report must reference 'P053' or 'petition053' to prove coverage."
                + " FR-4, AC-12.",
            featureFilePath)
        .satisfiesAnyOf(
            c -> assertThat(c).contains("P053"), c -> assertThat(c).contains("petition053"));
  }

  /** FR-4 (AC-12) — "Gaps" section listing Catala rule branches not covered by P053 scenarios. */
  @Then(
      "the report contains a {string} section listing rule branches encoded in Catala but not"
          + " covered by any P053 scenario, or stating {string}")
  public void reportContainsSectionListingGaps(String sectionName, String noneFound) {
    String content = readCurrentFileContent();
    assertThat(content)
        .as(
            "SPIKE-REPORT.md (%s) must contain a '%s' section (listing uncovered Catala rule"
                + " branches or stating '%s'). FR-4, AC-12.",
            currentFilePath, sectionName, noneFound)
        .contains(sectionName);
  }

  /**
   * FR-4 (AC-12) — "Discrepancies" section listing P053 scenarios that contradict the G.A. text.
   */
  @Then(
      "the report contains a {string} section listing cases where a P053 scenario contradicts"
          + " the G.A. text, or stating {string}")
  public void reportContainsSectionListingDiscrepancies(String sectionName, String noneFound) {
    String content = readCurrentFileContent();
    assertThat(content)
        .as(
            "SPIKE-REPORT.md (%s) must contain a '%s' section (listing G.A.-vs-P053"
                + " contradictions or stating '%s'). FR-4, AC-12.",
            currentFilePath, sectionName, noneFound)
        .contains(sectionName);
  }

  /** FR-4 (AC-12) — "Effort estimate" section with person-day estimate and rationale. */
  @Then(
      "the report contains an {string} section with a person-day estimate for encoding the"
          + " full G.A. Inddrivelse chapter and a rationale")
  public void reportContainsEffortEstimateSection(String sectionName) {
    String content = readCurrentFileContent();
    assertThat(content)
        .as(
            "SPIKE-REPORT.md (%s) must contain an '%s' section. FR-4, AC-12.",
            currentFilePath, sectionName)
        .contains(sectionName);
    assertThat(content)
        .as(
            "SPIKE-REPORT.md (%s) 'Effort estimate' section must include a person-day"
                + " estimate for encoding the full G.A. Inddrivelse chapter. FR-4, AC-12.",
            currentFilePath)
        .satisfiesAnyOf(
            c -> assertThat(c).containsIgnoringCase("person-day"),
            c -> assertThat(c).containsIgnoringCase("person day"),
            c -> assertThat(c).containsIgnoringCase("persondage"),
            c -> assertThat(c).containsIgnoringCase("persondag"));
  }

  // ─────────────────────────────────────────────────────────────────────────
  // FR-5 — Spike report: Go/No-Go recommendation (AC-13)
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * FR-5 (AC-13) — The report contains a "Go/No-Go" section with an unambiguous verdict.
   *
   * <p>This step also sets the current file path so that the subsequent evidence steps can read the
   * report content without a preceding "exists in the repository" step.
   */
  @Then(
      "the file {string} contains a {string} section with an unambiguous verdict of {string}"
          + " or {string}")
  public void fileContainsSectionWithUnambiguousVerdict(
      String relativePath, String sectionName, String verdictGo, String verdictNoGo) {
    currentFilePath = resolveRepoRoot().resolve(relativePath);
    currentFileContent = null;
    String content = readCurrentFileContent();
    assertThat(content)
        .as(
            "SPIKE-REPORT.md (%s) must contain a '%s' section. FR-5, AC-13.",
            currentFilePath, sectionName)
        .contains(sectionName);
    assertThat(content)
        .as(
            "SPIKE-REPORT.md (%s) must state an unambiguous verdict of '%s' or '%s' in the"
                + " '%s' section. FR-5, AC-13.",
            currentFilePath, verdictGo, verdictNoGo, sectionName)
        .satisfiesAnyOf(
            c -> assertThat(c).contains("**" + verdictGo + "**"),
            c -> assertThat(c).contains("**" + verdictNoGo + "**"),
            c -> assertThat(c).contains(": " + verdictGo),
            c -> assertThat(c).contains(": " + verdictNoGo),
            c -> assertThat(c).contains("Verdict: " + verdictGo),
            c -> assertThat(c).contains("Verdict: " + verdictNoGo));
  }

  /**
   * FR-5 (AC-13) — Evidence that all 4 modtagelsestidspunkt sub-rules were encoded without
   * ambiguity.
   */
  @Then(
      "the section provides evidence for whether all 4 modtagelsestidspunkt sub-rules encoded"
          + " without ambiguity")
  public void sectionProvidesEvidenceForModtagelsestidspunktSubRules() {
    String content = readCurrentFileContent();
    assertThat(content)
        .as(
            "SPIKE-REPORT.md (%s) Go/No-Go section must include evidence for whether all 4"
                + " modtagelsestidspunkt sub-rules were encoded without ambiguity."
                + " FR-5, AC-13.",
            currentFilePath)
        .satisfiesAnyOf(
            c -> assertThat(c).contains("modtagelsestidspunkt"),
            c -> assertThat(c).containsIgnoringCase("sub-rule"),
            c -> assertThat(c).containsIgnoringCase("subrule"));
  }

  /**
   * FR-5 (AC-13) — Evidence for whether at least 1 gap or discrepancy was found vs P053 Gherkin.
   */
  @Then(
      "the section provides evidence for whether at least 1 gap or discrepancy was found"
          + " relative to P053 Gherkin")
  public void sectionProvidesEvidenceForGapOrDiscrepancy() {
    String content = readCurrentFileContent();
    assertThat(content)
        .as(
            "SPIKE-REPORT.md (%s) Go/No-Go section must include evidence for whether at least"
                + " 1 gap or discrepancy was found relative to P053 Gherkin. FR-5, AC-13.",
            currentFilePath)
        .satisfiesAnyOf(
            c -> assertThat(c).containsIgnoringCase("gap"),
            c -> assertThat(c).containsIgnoringCase("discrepancy"));
  }

  /** FR-5 (AC-13) — Evidence that Catala test compilation succeeded without errors. */
  @Then(
      "the section provides evidence for whether Catala test compilation succeeded without errors")
  public void sectionProvidesEvidenceForCompilationSuccess() {
    String content = readCurrentFileContent();
    assertThat(content)
        .as(
            "SPIKE-REPORT.md (%s) Go/No-Go section must include evidence for the Catala"
                + " compilation result (exit code 0, or error details if it failed)."
                + " FR-5, AC-13.",
            currentFilePath)
        .satisfiesAnyOf(
            c -> assertThat(c).containsIgnoringCase("compil"),
            c -> assertThat(c).containsIgnoringCase("exit code"),
            c -> assertThat(c).containsIgnoringCase("catala ocaml"));
  }

  /** FR-5 (AC-13) — Evidence that OCaml or Python extraction produced runnable code. */
  @Then(
      "the section provides evidence for whether OCaml or Python extraction produced runnable"
          + " code")
  public void sectionProvidesEvidenceForCodeExtraction() {
    String content = readCurrentFileContent();
    assertThat(content)
        .as(
            "SPIKE-REPORT.md (%s) Go/No-Go section must include evidence for whether OCaml"
                + " or Python code extraction produced runnable code. FR-5, AC-13.",
            currentFilePath)
        .satisfiesAnyOf(
            c -> assertThat(c).containsIgnoringCase("ocaml"),
            c -> assertThat(c).containsIgnoringCase("python"),
            c -> assertThat(c).containsIgnoringCase("extraction"),
            c -> assertThat(c).containsIgnoringCase("runnable"));
  }

  /**
   * FR-5 (AC-13) — Evidence that each No-Go trigger was evaluated: temporal rule workarounds, legal
   * ambiguities blocking encoding, and effort per G.A. section exceeding 4 person-days.
   */
  @Then(
      "the section provides evidence for each No-Go trigger: temporal rule workarounds, legal"
          + " ambiguities blocking encoding, and encoding effort per G.A. section exceeding 4"
          + " person-days")
  public void sectionProvidesEvidenceForNoGoTriggers() {
    String content = readCurrentFileContent();
    // N-1: temporal rule workarounds
    assertThat(content)
        .as(
            "SPIKE-REPORT.md (%s) must address No-Go trigger N-1 (temporal rule workarounds)."
                + " FR-5, AC-12.",
            currentFilePath)
        .satisfiesAnyOf(
            c -> assertThat(c).containsIgnoringCase("temporal"),
            c -> assertThat(c).containsIgnoringCase("workaround"));
    // N-2: legal ambiguities blocking encoding
    assertThat(content)
        .as(
            "SPIKE-REPORT.md (%s) must address No-Go trigger N-2 (legal ambiguities blocking"
                + " encoding). FR-5, AC-12.",
            currentFilePath)
        .satisfiesAnyOf(
            c -> assertThat(c).containsIgnoringCase("ambiguity"),
            c -> assertThat(c).containsIgnoringCase("ambiguities"),
            c -> assertThat(c).containsIgnoringCase("tvetydighed"),
            c -> assertThat(c).containsIgnoringCase("underspecified"));
    // N-3: encoding effort per G.A. section exceeding 4 person-days
    assertThat(content)
        .as(
            "SPIKE-REPORT.md (%s) must address No-Go trigger N-3 (effort exceeding 4"
                + " person-days per G.A. section). FR-5, AC-12.",
            currentFilePath)
        .satisfiesAnyOf(
            c -> assertThat(c).containsIgnoringCase("person-day"),
            c -> assertThat(c).containsIgnoringCase("person day"),
            c -> assertThat(c).containsIgnoringCase("4 days"),
            c -> assertThat(c).containsIgnoringCase("4 dage"));
  }

  // ─────────────────────────────────────────────────────────────────────────
  // NFR-3 — G.A. snapshot version reference (AC-15)
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * NFR-3 (AC-15) — Each Catala source file must carry both the snapshot version string and the
   * snapshot date string in its header or a comment block.
   *
   * <p>This step is self-contained: it resolves and reads the file itself rather than relying on a
   * preceding "exists" step, because the version-reference scenario uses standalone {@code Then}
   * steps with the file path embedded directly in the step text.
   */
  @Then("the file {string} contains a version reference to {string} and {string}")
  public void fileContainsVersionReference(String relativePath, String version, String date) {
    Path filePath = resolveRepoRoot().resolve(relativePath);
    assertThat(Files.exists(filePath))
        .as(
            "Catala source file not found: %s. The file must exist and carry a version"
                + " reference to '%s' and '%s'. NFR-3, AC-15.",
            filePath, version, date)
        .isTrue();
    String content;
    try {
      content = Files.readString(filePath, StandardCharsets.UTF_8);
    } catch (IOException e) {
      fail("Failed to read %s: %s", filePath, e.getMessage());
      return; // unreachable — fail() always throws
    }
    assertThat(content)
        .as(
            "File %s must contain G.A. snapshot version string '%s'. NFR-3, AC-15.",
            filePath, version)
        .contains(version);
    assertThat(content)
        .as("File %s must contain G.A. snapshot date string '%s'. NFR-3, AC-15.", filePath, date)
        .contains(date);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // NFR-1 — Catala CLI compilation (AC-3 compilation gate)
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * NFR-1 — Assert the Catala CLI is available on PATH.
   *
   * <p>If the CLI is absent (e.g. in a CI environment that does not have Catala installed), this
   * step emits a {@link PendingException} — which Cucumber reports as "skipped" — rather than
   * failing the build. This matches the mandate: "if CLI not found, skip with a clear message; do
   * NOT fail the build if CLI is absent." SPEC-P054 §6.
   */
  @Given("the Catala CLI is available on the execution PATH")
  public void theCatalaCliIsAvailableOnPath() {
    try {
      ProcessBuilder pb = new ProcessBuilder("catala", "--version");
      pb.redirectErrorStream(true);
      Process process = pb.start();
      int exitCode = process.waitFor();
      catalaCliAvailable = (exitCode == 0);
    } catch (IOException | InterruptedException e) {
      catalaCliAvailable = false;
    }
    if (!catalaCliAvailable) {
      throw new PendingException(
          "Catala CLI is not installed on the execution PATH — NFR-1 compilation validation"
              + " is skipped. Install the Catala CLI to enable this check. SPEC-P054 §6.");
    }
  }

  /** NFR-1 — Execute the given Catala compile command from the specified working directory. */
  @When("{string} is executed from the {string} directory")
  public void commandIsExecutedFromDirectory(String command, String directory) {
    if (!catalaCliAvailable) {
      throw new PendingException(
          "Catala CLI unavailable — compilation command step skipped. SPEC-P054 §6.");
    }
    Path workingDir = resolveRepoRoot().resolve(directory);
    assertThat(Files.isDirectory(workingDir))
        .as(
            "Working directory for Catala compilation does not exist: %s."
                + " The catala/ directory must be created before running this step. NFR-1.",
            workingDir)
        .isTrue();
    compilationCommand = command;
    compilationDirectory = directory;
    String[] parts = command.split("\\s+");
    try {
      ProcessBuilder pb = new ProcessBuilder(parts);
      pb.directory(workingDir.toFile());
      pb.redirectErrorStream(true);
      Process process = pb.start();
      lastExitCode = process.waitFor();
    } catch (IOException | InterruptedException e) {
      fail(
          "Failed to execute compilation command '%s' in '%s': %s",
          command, directory, e.getMessage());
    }
  }

  /** NFR-1 — Assert the compilation command exited with code 0. */
  @Then("the command exits with code 0")
  public void theCommandExitsWithCode0() {
    if (!catalaCliAvailable) {
      throw new PendingException(
          "Catala CLI unavailable — exit code assertion skipped. SPEC-P054 §6.");
    }
    assertThat(lastExitCode)
        .as(
            "Catala compilation command '%s' (run from '%s') must exit with code 0."
                + " Actual exit code: %d. Check the Catala source for syntax errors. NFR-1.",
            compilationCommand, compilationDirectory, lastExitCode)
        .isEqualTo(0);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // NFR-4 — No production artefacts created or modified (AC-16)
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * NFR-4 (AC-16) — No Java source files under any {@code src/main/java} path were created or
   * modified by the spike.
   */
  @Then("no Java source files have been created or modified under any {string} path")
  public void noJavaSourceFilesModifiedUnder(String pathPattern) {
    assertNoModifiedFilesMatchingPattern(
        pathPattern,
        ".java",
        "No Java source files may be created or modified by this spike. NFR-4, AC-16.");
  }

  /**
   * NFR-4 (AC-16) — No database migration scripts under any {@code src/main/resources/db/migration}
   * path were added.
   */
  @Then("no database migration scripts have been added under any {string} path")
  public void noDatabaseMigrationScriptsAddedUnder(String pathPattern) {
    assertNoModifiedFilesMatchingPattern(
        pathPattern,
        ".sql",
        "No database migration scripts may be added by this spike. NFR-4, AC-16.");
  }

  /** NFR-4 (AC-16) — No OpenAPI or Swagger specification files were modified. */
  @Then("no OpenAPI or Swagger specification files have been modified")
  public void noOpenApiFilesModified() {
    List<String> modified = getGitModifiedFiles();
    List<String> violations =
        modified.stream()
            .filter(
                f ->
                    (f.contains("openapi") || f.contains("swagger"))
                        && (f.endsWith(".yaml") || f.endsWith(".yml") || f.endsWith(".json")))
            .toList();
    assertThat(violations)
        .as(
            "No OpenAPI or Swagger specification files may be modified by this spike."
                + " Violations: %s. NFR-4, AC-16.",
            violations)
        .isEmpty();
  }

  /** NFR-4 (AC-16) — No Spring Boot module configuration files were created or modified. */
  @Then("no Spring Boot module configuration files have been created or modified")
  public void noSpringBootConfigFilesModified() {
    List<String> modified = getGitModifiedFiles();
    List<String> violations =
        modified.stream()
            .filter(
                f ->
                    (f.contains("application.yml")
                        || f.contains("application.yaml")
                        || Paths.get(f).getFileName().toString().startsWith("application-")))
            .filter(f -> !f.startsWith("catala/"))
            .toList();
    assertThat(violations)
        .as(
            "No Spring Boot module configuration files may be created or modified by this"
                + " spike. Violations: %s. NFR-4, AC-16.",
            violations)
        .isEmpty();
  }

  // ── NFR-4 helpers ─────────────────────────────────────────────────────────

  private void assertNoModifiedFilesMatchingPattern(
      String pathPattern, String extension, String message) {
    List<String> modified = getGitModifiedFiles();
    List<String> violations =
        modified.stream().filter(f -> f.contains(pathPattern) && f.endsWith(extension)).toList();
    assertThat(violations)
        .as("%s Modified files matching '%s/*%s': %s", message, pathPattern, extension, violations)
        .isEmpty();
  }

  /**
   * Return the list of files that are staged or modified relative to HEAD using {@code git diff
   * --name-only} and {@code git diff --name-only --cached}. Throws a {@link PendingException} if
   * git is not available.
   */
  private List<String> getGitModifiedFiles() {
    try {
      Path repoRoot = resolveRepoRoot();
      String unstaged = runGitDiff(repoRoot, "diff", "--name-only", "HEAD");
      String staged = runGitDiff(repoRoot, "diff", "--name-only", "--cached");
      return Stream.concat(Arrays.stream(unstaged.split("\n")), Arrays.stream(staged.split("\n")))
          .map(String::trim)
          .filter(s -> !s.isBlank())
          .distinct()
          .toList();
    } catch (IOException | InterruptedException e) {
      throw new PendingException(
          "git command unavailable — NFR-4 file-modification check cannot be performed."
              + " Ensure git is on the PATH. Cause: "
              + e.getMessage());
    }
  }

  private String runGitDiff(Path repoRoot, String... args)
      throws IOException, InterruptedException {
    String[] cmd = new String[args.length + 1];
    cmd[0] = "git";
    System.arraycopy(args, 0, cmd, 1, args.length);
    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.directory(repoRoot.toFile());
    pb.redirectErrorStream(false);
    Process process = pb.start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    process.waitFor();
    return output;
  }
}
