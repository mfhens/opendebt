@petition054
Feature: Catala Compliance Spike — G.A.1.4.3 and G.A.1.4.4 Encoding (P054)

  # Type: Research spike — no production code
  # Deliverables: Catala source files, test suite, spike report
  # Verification: file-system and content assertions on the spike deliverables;
  #               no application behaviour is asserted.
  # Legal basis: G.A.1.4.3, G.A.1.4.4, Gæld.bekendtg. § 7 stk. 1–2, GIL § 18 k
  # G.A. snapshot version: v3.16 (2026-01-30)
  # Out of scope: runtime integration, CI pipeline, full G.A. chapter encoding.

  # --- FR-1: Catala encoding of G.A.1.4.3 modtagelsestidspunkt rules ---

  Scenario: ga_1_4_3_opskrivning.catala_da exists and encodes all four modtagelsestidspunkt sub-rules
    Given the spike has been completed within the 2-working-day time box
    Then the file "catala/ga_1_4_3_opskrivning.catala_da" exists in the repository
    And the file declares the Danish Catala dialect "catala_da"
    And the file contains a Catala rule block for the default receipt rule anchored to "Gæld.bekendtg. § 7, stk. 1, 3. pkt."
    And the file contains a Catala rule block for the høring exception anchored to "Gæld.bekendtg. § 7, stk. 1, 4. pkt."
    And the file contains a Catala rule block for the same-system annulleret nedskrivning exception anchored to "Gæld.bekendtg. § 7, stk. 1, 5. pkt."
    And the file contains a Catala rule block for the cross-system annulleret nedskrivning exception anchored to "Gæld.bekendtg. § 7, stk. 1, 6. pkt."

  # --- FR-2: Catala encoding of G.A.1.4.4 nedskrivning rules ---

  Scenario: ga_1_4_4_nedskrivning.catala_da exists and encodes nedskrivningsgrunde, virkningsdato and GIL § 18 k
    Given the spike has been completed within the 2-working-day time box
    Then the file "catala/ga_1_4_4_nedskrivning.catala_da" exists in the repository
    And the file declares the Danish Catala dialect "catala_da"
    And the file contains Catala rule blocks for all three nedskrivningsgrunde anchored to "Gæld.bekendtg. § 7, stk. 2, nr. 1", "Gæld.bekendtg. § 7, stk. 2, nr. 2", and "Gæld.bekendtg. § 7, stk. 2, nr. 3"
    And the file contains a Catala rule block determining retroactivity when "virkningsdato < fordring.receivedAt"
    And the file contains a Catala rule block for the GIL § 18 k suspension flag anchored to "GIL § 18 k"
    And the file contains a Catala validation rule rejecting a nedskrivning submitted without a valid grund

  # --- FR-3: Catala test suite ---

  Scenario: ga_opskrivning_nedskrivning_tests.catala_da exists and contains at least 8 tests
    Given the spike has been completed within the 2-working-day time box
    Then the file "catala/tests/ga_opskrivning_nedskrivning_tests.catala_da" exists in the repository
    And the file contains at least 8 test cases expressed using Catala's built-in Test module
    And the test cases cover all four FR-1 modtagelsestidspunkt sub-rules
    And the test cases cover all three FR-2 nedskrivningsgrunde
    And the test cases cover the GIL § 18 k suspension flag for both true and false outcomes
    And the test cases include at least one boundary-date assertion for the virkningsdato retroactivity rule

  # --- FR-4: Comparison report against P053 Gherkin scenarios ---

  Scenario: SPIKE-REPORT.md exists and contains a P053 coverage comparison table and effort estimate
    Given the spike has been completed within the 2-working-day time box
    Then the file "catala/SPIKE-REPORT.md" exists in the repository
    And the report contains a table mapping each P053 FR-1 and FR-2 Gherkin scenario to a coverage status of "Covered", "Not covered", or "Discrepancy found"
    And every scenario from "petitions/petition053-fordringshaverportal-opskrivning-nedskrivning-fuld-spec.feature" that is in scope for FR-1 or FR-2 has a row in the table
    And the report contains a "Gaps" section listing rule branches encoded in Catala but not covered by any P053 scenario, or stating "None found"
    And the report contains a "Discrepancies" section listing cases where a P053 scenario contradicts the G.A. text, or stating "None found"
    And the report contains an "Effort estimate" section with a person-day estimate for encoding the full G.A. Inddrivelse chapter and a rationale

  # --- FR-5: Go/No-Go recommendation ---

  Scenario: SPIKE-REPORT.md contains an explicit Go/No-Go recommendation with evidence for each criterion
    Given the spike has been completed within the 2-working-day time box
    Then the file "catala/SPIKE-REPORT.md" contains a "Go/No-Go" section with an unambiguous verdict of "Go" or "No-Go"
    And the section provides evidence for whether all 4 modtagelsestidspunkt sub-rules encoded without ambiguity
    And the section provides evidence for whether at least 1 gap or discrepancy was found relative to P053 Gherkin
    And the section provides evidence for whether Catala test compilation succeeded without errors
    And the section provides evidence for whether OCaml or Python extraction produced runnable code
    And the section provides evidence for each No-Go trigger: temporal rule workarounds, legal ambiguities blocking encoding, and encoding effort per G.A. section exceeding 4 person-days

  # --- NFR-1: Catala CLI compilation exits 0 ---

  Scenario: Both Catala source files compile without errors
    Given the Catala CLI is available on the execution PATH
    When "catala ocaml ga_1_4_3_opskrivning.catala_da" is executed from the "catala/" directory
    Then the command exits with code 0
    When "catala ocaml ga_1_4_4_nedskrivning.catala_da" is executed from the "catala/" directory
    Then the command exits with code 0

  # --- NFR-4: No production artefacts modified ---

  Scenario: No Java source files, database migrations, or API specifications are modified by the spike
    Given the spike has been completed within the 2-working-day time box
    Then no Java source files have been created or modified under any "src/main/java" path
    And no database migration scripts have been added under any "db/migration" or "resources/db" path
    And no OpenAPI or Swagger specification files have been modified
    And no Spring Boot module configuration files have been created or modified
