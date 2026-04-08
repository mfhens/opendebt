package dk.ufst.opendebt.rules.collection;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import dk.ufst.opendebt.rules.model.CollectionPriorityRequest;
import dk.ufst.opendebt.rules.model.CollectionPriorityResult;

/**
 * Integration tests for collection-priority.drl.
 *
 * <p>Verifies the GIL § 4 inter-claim daekningsraekkefoeige rules as formalised in the Catala
 * source file {@code ga_2_3_2_1_daekningsraekkefoeigen.catala_da} (G.A. v3.16, 2026-03-28).
 *
 * <p>TB-034: corrects wrong category order (was: boernebidrag #1, skat #2, boeder #3).
 *
 * <p>Correct order per GIL § 4, stk. 1 / Catala FR-1.1 - FR-1.4:
 *
 * <ol>
 *   <li>RIMELIGE_OMKOSTNINGER (rank 1) — GIL § 6a, stk. 1
 *   <li>BOEDER_TVANGSBOEEDER_TILBAGEBETALING (rank 2) — GIL § 10b
 *   <li>UNDERHOLDSBIDRAG privatretlig (rank 3, ordning 1) — GIL § 4, stk. 1, nr. 2
 *   <li>UNDERHOLDSBIDRAG offentlig (rank 3, ordning 2) — GIL § 4, stk. 1, nr. 2
 *   <li>ANDRE_FORDRINGER (rank 4) — GIL § 4, stk. 1, nr. 3 (incl. tax debts)
 * </ol>
 */
class CollectionPriorityRulesIntegrationTest {

  private static final String RESULT_GLOBAL = "result";
  private static final String RULES_PATH = "rules/";

  private static KieContainer kieContainer;

  private KieSession kieSession;
  private CollectionPriorityResult result;

  @BeforeAll
  static void initKieContainer() throws IOException {
    KieServices kieServices = KieServices.Factory.get();
    KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

    // Load only collection-priority.drl to avoid global-variable conflicts with other DRL files
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource[] drlResources =
        resolver.getResources("classpath*:" + RULES_PATH + "collection-priority.drl");

    for (Resource resource : drlResources) {
      kieFileSystem.write(
          ResourceFactory.newClassPathResource(RULES_PATH + resource.getFilename(), "UTF-8"));
    }

    KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
    kieBuilder.buildAll();

    assertThat(kieBuilder.getResults().getMessages())
        .as("collection-priority.drl must compile without errors")
        .filteredOn(m -> m.getLevel() == Message.Level.ERROR)
        .isEmpty();

    KieModule kieModule = kieBuilder.getKieModule();
    kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());
  }

  @BeforeEach
  void setUpSession() {
    result =
        CollectionPriorityResult.builder()
            .debtId(UUID.randomUUID())
            .priorityRank(CollectionPriorityResult.PRIORITY_DEFAULT)
            .build();
    kieSession = kieContainer.newKieSession();
    kieSession.setGlobal(RESULT_GLOBAL, result);
  }

  @AfterEach
  void tearDownSession() {
    if (kieSession != null) {
      kieSession.dispose();
    }
  }

  // =========================================================================
  // Helper
  // =========================================================================

  private CollectionPriorityResult fireRules(CollectionPriorityRequest request) {
    kieSession.insert(request);
    kieSession.fireAllRules();
    return result;
  }

  private CollectionPriorityResult fireRulesInNewSession(CollectionPriorityRequest request) {
    CollectionPriorityResult r =
        CollectionPriorityResult.builder()
            .debtId(request.getDebtId())
            .priorityRank(CollectionPriorityResult.PRIORITY_DEFAULT)
            .build();
    KieSession session = kieContainer.newKieSession();
    try {
      session.setGlobal(RESULT_GLOBAL, r);
      session.insert(request);
      session.fireAllRules();
    } finally {
      session.dispose();
    }
    return r;
  }

  // =========================================================================
  // FR-1.1: Rimelige omkostninger — rank 1 (GIL § 6a)
  // Catala: RIMELIGE_OMKOSTNINGER -> prioritetRang = 1
  // =========================================================================

  @Test
  @DisplayName("FR-1.1: Rimelig omkostning -> rank 1 (GIL § 6a, stk. 1)")
  void rimeligOmkostning_shouldBeRank1() {
    CollectionPriorityRequest request =
        CollectionPriorityRequest.builder()
            .debtId(UUID.randomUUID())
            .isRimeligOmkostning(true)
            .build();

    CollectionPriorityResult res = fireRules(request);

    assertThat(res.getPriorityRank())
        .as("Rimelig omkostning must be category 1 (GIL § 6a)")
        .isEqualTo(CollectionPriorityResult.GIL4_RIMELIGE_OMKOSTNINGER);
    assertThat(res.getPriorityCategory()).isEqualTo("RIMELIGE_OMKOSTNINGER");
    assertThat(res.getUnderholdsbidragOrdning()).isZero();
  }

  // =========================================================================
  // FR-1.2: Boeder/tvangsboeder — rank 2 (GIL § 10b) — was incorrectly rank 3
  // Catala: BOEDER_TVANGSBOEEDER_TILBAGEBETALING -> prioritetRang = 2
  // =========================================================================

  @Test
  @DisplayName("FR-1.2: Boede/tvangsboede -> rank 2 (GIL § 10b) — fixed by TB-034")
  void fine_shouldBeRank2_notRank3() {
    CollectionPriorityRequest request =
        CollectionPriorityRequest.builder().debtId(UUID.randomUUID()).isFine(true).build();

    CollectionPriorityResult res = fireRules(request);

    assertThat(res.getPriorityRank())
        .as("Boede must be category 2 (GIL § 10b) — was incorrectly 3 before TB-034")
        .isEqualTo(CollectionPriorityResult.GIL4_BOEDER_TVANGSBOEEDER_TILBAGEBETALING);
    assertThat(res.getPriorityCategory())
        .isEqualTo(CollectionPriorityResult.CATEGORY_BOEDER_TVANGSBOEEDER_TILBAGEBETALING);
    assertThat(res.getUnderholdsbidragOrdning()).isZero();
  }

  // =========================================================================
  // FR-1.3a/c: Underholdsbidrag privatretlig — rank 3, ordning 1 — was incorrectly rank 1
  // Catala: UNDERHOLDSBIDRAG_PRIVATRETLIG -> prioritetRang = 3, underholdsbidragOrdning = 1
  // =========================================================================

  @Test
  @DisplayName("FR-1.3a: Privatretligt underholdsbidrag -> rank 3, ordning 1 — fixed by TB-034")
  void privatChildSupport_shouldBeRank3Ordning1() {
    CollectionPriorityRequest request =
        CollectionPriorityRequest.builder()
            .debtId(UUID.randomUUID())
            .isChildSupport(true)
            .isPrivatUnderholdsbidrag(true)
            .build();

    CollectionPriorityResult res = fireRules(request);

    assertThat(res.getPriorityRank())
        .as("Privatretligt underholdsbidrag must be rank 3 — was incorrectly 1 before TB-034")
        .isEqualTo(CollectionPriorityResult.GIL4_UNDERHOLDSBIDRAG);
    assertThat(res.getPriorityCategory()).isEqualTo("UNDERHOLDSBIDRAG_PRIVATRETLIG");
    assertThat(res.getUnderholdsbidragOrdning())
        .as("Privatretlig = ordning 1 (covered before offentlig)")
        .isEqualTo(1);
  }

  // =========================================================================
  // FR-1.3b/d: Underholdsbidrag offentlig — rank 3, ordning 2 — was incorrectly rank 1
  // Catala: UNDERHOLDSBIDRAG_OFFENTLIG -> prioritetRang = 3, underholdsbidragOrdning = 2
  // =========================================================================

  @Test
  @DisplayName("FR-1.3b: Offentligretligt underholdsbidrag -> rank 3, ordning 2 — fixed by TB-034")
  void offentligChildSupport_shouldBeRank3Ordning2() {
    CollectionPriorityRequest request =
        CollectionPriorityRequest.builder()
            .debtId(UUID.randomUUID())
            .isChildSupport(true)
            .isPrivatUnderholdsbidrag(false)
            .build();

    CollectionPriorityResult res = fireRules(request);

    assertThat(res.getPriorityRank())
        .as("Offentligretligt underholdsbidrag must be rank 3 — was incorrectly 1 before TB-034")
        .isEqualTo(CollectionPriorityResult.GIL4_UNDERHOLDSBIDRAG);
    assertThat(res.getPriorityCategory()).isEqualTo("UNDERHOLDSBIDRAG_OFFENTLIG");
    assertThat(res.getUnderholdsbidragOrdning())
        .as("Offentlig = ordning 2 (covered after privatretlig)")
        .isEqualTo(2);
  }

  // =========================================================================
  // FR-1.4: Andre fordringer (tax, court-ordered) — rank 4 — tax was incorrectly rank 2
  // Catala: ANDRE_FORDRINGER -> prioritetRang = 4
  // =========================================================================

  @Test
  @DisplayName("FR-1.4: Skattegaeld -> rank 4 (andre fordringer) — fixed by TB-034")
  void taxDebt_shouldBeRank4_notRank2() {
    CollectionPriorityRequest request =
        CollectionPriorityRequest.builder().debtId(UUID.randomUUID()).isTaxDebt(true).build();

    CollectionPriorityResult res = fireRules(request);

    assertThat(res.getPriorityRank())
        .as("Skattegaeld must be andre fordringer rank 4 — was incorrectly 2 before TB-034")
        .isEqualTo(CollectionPriorityResult.GIL4_ANDRE_FORDRINGER);
    assertThat(res.getPriorityCategory()).isEqualTo("ANDRE_FORDRINGER");
  }

  @Test
  @DisplayName("FR-1.4: Court-ordered debt -> rank 4 (andre fordringer)")
  void courtOrderedDebt_shouldBeRank4() {
    CollectionPriorityRequest request =
        CollectionPriorityRequest.builder().debtId(UUID.randomUUID()).isCourtOrdered(true).build();

    CollectionPriorityResult res = fireRules(request);

    assertThat(res.getPriorityRank())
        .as("Retsafgoerelse must be andre fordringer rank 4")
        .isEqualTo(CollectionPriorityResult.GIL4_ANDRE_FORDRINGER);
    assertThat(res.getPriorityCategory()).isEqualTo("ANDRE_FORDRINGER");
  }

  @Test
  @DisplayName("FR-1.4: Unspecified/other claim -> rank 4 (andre fordringer)")
  void unknownClaim_shouldBeRank4() {
    CollectionPriorityRequest request =
        CollectionPriorityRequest.builder().debtId(UUID.randomUUID()).debtTypeCode("OTHER").build();

    CollectionPriorityResult res = fireRules(request);

    assertThat(res.getPriorityRank()).isEqualTo(CollectionPriorityResult.GIL4_ANDRE_FORDRINGER);
  }

  // =========================================================================
  // FR-2.1: FIFO sort key = oprettetDato (GIL § 4, stk. 2, 1. pkt.)
  // Catala: FifoSortNoegle — fifoSortKey = modtagelsesdato
  // =========================================================================

  @Test
  @DisplayName("FR-2.1: FIFO sort key = oprettetDato when no legacy date")
  void fifoSortKey_standardDate_shouldBeOprettetDato() {
    LocalDate oprettet = LocalDate.of(2024, 1, 15);
    CollectionPriorityRequest request =
        CollectionPriorityRequest.builder()
            .debtId(UUID.randomUUID())
            .isFine(true)
            .oprettetDato(oprettet)
            .harLegacyOprettetDato(false)
            .build();

    CollectionPriorityResult res = fireRules(request);

    assertThat(res.getFifoSortKey())
        .as("FR-2.1: FIFO key must equal oprettetDato when no pre-2013 legacy date")
        .isEqualTo(oprettet);
  }

  // =========================================================================
  // FR-2.2: Pre-2013 FIFO exception (GIL § 4, stk. 2, 5. pkt.)
  // Catala: harLegacyModtagelsesdato && legacyModtagelsesdato < 2013-09-01 -> use legacy
  // =========================================================================

  @Test
  @DisplayName("FR-2.2: Pre-2013 legacy date -> fifoSortKey = legacyOprettetDato")
  void fifoSortKey_pre2013Legacy_shouldUseLegacyDate() {
    LocalDate oprettet = LocalDate.of(2024, 1, 15);
    LocalDate legacy = LocalDate.of(2012, 8, 15); // before 2013-09-01
    CollectionPriorityRequest request =
        CollectionPriorityRequest.builder()
            .debtId(UUID.randomUUID())
            .isFine(true)
            .oprettetDato(oprettet)
            .harLegacyOprettetDato(true)
            .legacyOprettetDato(legacy)
            .build();

    CollectionPriorityResult res = fireRules(request);

    assertThat(res.getFifoSortKey())
        .as("FR-2.2: Pre-2013 claim must use legacy date as FIFO key")
        .isEqualTo(legacy);
  }

  @Test
  @DisplayName(
      "FR-2.2: Post-2013 legacy date -> fifoSortKey = oprettetDato (exception does NOT apply)")
  void fifoSortKey_post2013LegacyDate_shouldUseOprettetDato() {
    LocalDate oprettet = LocalDate.of(2024, 1, 15);
    LocalDate legacy = LocalDate.of(2014, 6, 1); // AFTER 2013-09-01
    CollectionPriorityRequest request =
        CollectionPriorityRequest.builder()
            .debtId(UUID.randomUUID())
            .isFine(true)
            .oprettetDato(oprettet)
            .harLegacyOprettetDato(true)
            .legacyOprettetDato(legacy)
            .build();

    CollectionPriorityResult res = fireRules(request);

    assertThat(res.getFifoSortKey())
        .as("FR-2.2 exception applies only for dates before 2013-09-01")
        .isEqualTo(oprettet);
  }

  // =========================================================================
  // Inter-claim sort order: correct GIL § 4 ordering across multiple claims
  // =========================================================================

  @Test
  @DisplayName("Inter-claim order: rimelig (1) < boede (2) < skat (4)")
  void sortOrder_multipleClaimsAreOrderedCorrectly() {
    LocalDate date = LocalDate.of(2024, 3, 1);

    CollectionPriorityRequest fine =
        CollectionPriorityRequest.builder()
            .debtId(UUID.randomUUID())
            .isFine(true)
            .oprettetDato(date)
            .build();

    CollectionPriorityRequest taxDebt =
        CollectionPriorityRequest.builder()
            .debtId(UUID.randomUUID())
            .isTaxDebt(true)
            .oprettetDato(date)
            .build();

    CollectionPriorityRequest rimelig =
        CollectionPriorityRequest.builder()
            .debtId(UUID.randomUUID())
            .isRimeligOmkostning(true)
            .oprettetDato(date)
            .build();

    List<CollectionPriorityResult> results =
        List.of(fine, taxDebt, rimelig).stream()
            .map(this::fireRulesInNewSession)
            .sorted(
                java.util.Comparator.comparingInt(CollectionPriorityResult::getPriorityRank)
                    .thenComparingInt(CollectionPriorityResult::getUnderholdsbidragOrdning)
                    .thenComparing(
                        CollectionPriorityResult::getFifoSortKey,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
            .toList();

    assertThat(results.get(0).getPriorityRank())
        .as("Rimelig omkostning (rank 1) must be first")
        .isEqualTo(CollectionPriorityResult.GIL4_RIMELIGE_OMKOSTNINGER);
    assertThat(results.get(1).getPriorityRank())
        .as("Boede (rank 2) must be second")
        .isEqualTo(CollectionPriorityResult.GIL4_BOEDER_TVANGSBOEEDER_TILBAGEBETALING);
    assertThat(results.get(2).getPriorityRank())
        .as("Skattegaeld (rank 4, andre fordringer) must be last")
        .isEqualTo(CollectionPriorityResult.GIL4_ANDRE_FORDRINGER);
  }

  @Test
  @DisplayName("FIFO within same category: older claim covered first (GIL § 4, stk. 2)")
  void fifoWithinSameCategory_olderClaimFirst() {
    LocalDate older = LocalDate.of(2023, 1, 1);
    LocalDate newer = LocalDate.of(2024, 6, 1);

    CollectionPriorityRequest olderFine =
        CollectionPriorityRequest.builder()
            .debtId(UUID.randomUUID())
            .isFine(true)
            .oprettetDato(older)
            .build();

    CollectionPriorityRequest newerFine =
        CollectionPriorityRequest.builder()
            .debtId(UUID.randomUUID())
            .isFine(true)
            .oprettetDato(newer)
            .build();

    UUID olderId = olderFine.getDebtId();

    List<CollectionPriorityResult> results =
        List.of(newerFine, olderFine).stream() // intentionally reversed
            .map(this::fireRulesInNewSession)
            .sorted(
                java.util.Comparator.comparingInt(CollectionPriorityResult::getPriorityRank)
                    .thenComparingInt(CollectionPriorityResult::getUnderholdsbidragOrdning)
                    .thenComparing(
                        CollectionPriorityResult::getFifoSortKey,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
            .toList();

    assertThat(results.get(0).getDebtId())
        .as("Older claim (2023) must come before newer claim (2024) within same category")
        .isEqualTo(olderId);
  }

  @Test
  @DisplayName("Privatretlig underholdsbidrag covered before offentlig within rank 3 (FR-1.3c/d)")
  void privatUnderholdsbidrag_coveredBeforeOffentlig() {
    LocalDate date = LocalDate.of(2024, 1, 1);
    UUID privatId = UUID.randomUUID();
    UUID offentligId = UUID.randomUUID();

    CollectionPriorityRequest privat =
        CollectionPriorityRequest.builder()
            .debtId(privatId)
            .isChildSupport(true)
            .isPrivatUnderholdsbidrag(true)
            .oprettetDato(date)
            .build();

    CollectionPriorityRequest offentlig =
        CollectionPriorityRequest.builder()
            .debtId(offentligId)
            .isChildSupport(true)
            .isPrivatUnderholdsbidrag(false)
            .oprettetDato(date)
            .build();

    List<CollectionPriorityResult> results =
        List.of(offentlig, privat).stream() // intentionally reversed
            .map(this::fireRulesInNewSession)
            .sorted(
                java.util.Comparator.comparingInt(CollectionPriorityResult::getPriorityRank)
                    .thenComparingInt(CollectionPriorityResult::getUnderholdsbidragOrdning)
                    .thenComparing(
                        CollectionPriorityResult::getFifoSortKey,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
            .toList();

    assertThat(results.get(0).getDebtId())
        .as("Privatretlig (ordning 1) must be covered before offentlig (ordning 2) — FR-1.3c")
        .isEqualTo(privatId);
    assertThat(results.get(1).getDebtId()).isEqualTo(offentligId);
  }
}
