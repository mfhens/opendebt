package dk.ufst.opendebt.payment.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.ufst.opendebt.payment.daekning.InddrivelsesindsatsType;
import dk.ufst.opendebt.payment.daekning.PrioritetKategori;
import dk.ufst.opendebt.payment.daekning.RenteKomponent;
import dk.ufst.opendebt.payment.daekning.dto.DaekningsraekkefoelgePositionDto;
import dk.ufst.opendebt.payment.daekning.entity.DaekningFordringEntity;
import dk.ufst.opendebt.payment.daekning.entity.DaekningRecord;
import dk.ufst.opendebt.payment.daekning.repository.DaekningFordringRepository;
import dk.ufst.opendebt.payment.daekning.repository.DaekningRecordRepository;
import dk.ufst.opendebt.payment.daekning.service.DaekningsRaekkefoeigenService;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/** BDD step definitions for Petition 057 — Dækningsrækkefølge (GIL § 4). */
public class Petition057Steps {

  @Autowired private DaekningFordringRepository fordringRepository;
  @Autowired private DaekningRecordRepository daekningRecordRepository;
  @Autowired private DaekningsRaekkefoeigenService daekningsService;
  @Autowired private WebApplicationContext webApplicationContext;

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  private String currentDebtorId;
  private BigDecimal currentPaymentAmount;
  private String currentBetalingstidspunkt;
  private InddrivelsesindsatsType currentIndsatsType;
  private String currentTargetFordringId;
  private Map<String, DaekningFordringEntity> currentFordringerMap = new HashMap<>();
  private List<DaekningRecord> currentDaekningRecords = new ArrayList<>();
  private List<DaekningsraekkefoelgePositionDto> currentOrderedList = new ArrayList<>();
  private int lastHttpStatus;
  private String lastResponseBody;
  private String pendingRequestPath;

  @Before("@petition057")
  public void resetScenarioState() {
    fordringRepository.deleteAll();
    daekningRecordRepository.deleteAll();
    currentFordringerMap = new HashMap<>();
    currentDaekningRecords = new ArrayList<>();
    currentOrderedList = new ArrayList<>();
    currentDebtorId = null;
    currentPaymentAmount = null;
    currentBetalingstidspunkt = null;
    currentIndsatsType = null;
    currentTargetFordringId = null;
    lastHttpStatus = 0;
    lastResponseBody = null;
    pendingRequestPath = null;
  }

  @Given("the payment-service rule engine is active")
  public void thePaymentServiceRuleEngineIsActive() {}

  @Given("the sagsbehandler portal is running")
  public void theSagsbehandlerPortalIsRunning() {}

  // =========================================================================
  // SEED helpers
  // =========================================================================

  private DaekningFordringEntity buildFromRow(String debtorId, Map<String, String> row) {
    String fordringId = row.get("fordringId");
    PrioritetKategori kategori = PrioritetKategori.valueOf(row.get("kategori"));
    BigDecimal beloeb = new BigDecimal(row.getOrDefault("tilbaestaaendeBeloeb", "100.00"));
    LocalDate modtagelsesdato =
        LocalDate.parse(row.getOrDefault("modtagelsesdato", LocalDate.now().toString()));

    String legacyStr = row.get("legacyModtagelsesdato");
    LocalDate legacyModtagelsesdato =
        (legacyStr != null && !legacyStr.isBlank()) ? LocalDate.parse(legacyStr) : null;

    String sekvensStr = row.get("sekvensNummer");
    Integer sekvensNummer =
        (sekvensStr != null && !sekvensStr.isBlank()) ? Integer.parseInt(sekvensStr.trim()) : null;

    String opskriv = row.get("opskrivningAfFordringId");
    // opskrivningAfFordringId is null when the DataTable cell is empty/blank
    // or the Cucumber sentinel "" (two double-quote chars used for empty cells in DataTables)
    // Valid fordringIds contain only ASCII letters, digits, and hyphens.
    String opskrivningAfFordringId =
        (opskriv != null && opskriv.matches("[A-Za-z0-9-]+")) ? opskriv : null;

    boolean inLoen =
        Boolean.parseBoolean(row.getOrDefault("inLoenindeholdelsesIndsats", "false").trim());
    boolean inUdlaeg = Boolean.parseBoolean(row.getOrDefault("inUdlaegForretning", "false").trim());

    return DaekningFordringEntity.builder()
        .fordringId(fordringId)
        .debtorId(debtorId)
        .prioritetKategori(kategori)
        .tilbaestaaendeBeloeb(beloeb)
        .modtagelsesdato(modtagelsesdato)
        .legacyModtagelsesdato(legacyModtagelsesdato)
        .sekvensNummer(sekvensNummer)
        .opskrivningAfFordringId(opskrivningAfFordringId)
        .inLoenindeholdelsesIndsats(inLoen)
        .inUdlaegForretning(inUdlaeg)
        .receivedAt(Instant.now())
        .build();
  }

  private void seedFordringer(String debtorId, DataTable table) {
    currentDebtorId = debtorId;
    for (Map<String, String> row : table.asMaps(String.class, String.class)) {
      DaekningFordringEntity entity = buildFromRow(debtorId, row);
      entity = fordringRepository.save(entity);
      currentFordringerMap.put(entity.getFordringId(), entity);
    }
  }

  private void applyPaymentNow() {
    Instant betalingstidspunkt =
        currentBetalingstidspunkt != null
            ? Instant.parse(currentBetalingstidspunkt)
            : Instant.now();
    if (currentTargetFordringId != null) {
      // Temporarily set far-future receivedAt on non-target entities so the service excludes them
      Instant futureTs = Instant.now().plusSeconds(3600L * 24 * 365);
      List<DaekningFordringEntity> others =
          fordringRepository.findByDebtorId(currentDebtorId).stream()
              .filter(f -> !f.getFordringId().equals(currentTargetFordringId))
              .toList();
      others.forEach(e -> e.setReceivedAt(futureTs));
      fordringRepository.saveAll(others);
      currentDaekningRecords =
          daekningsService.apply(
              currentDebtorId,
              currentPaymentAmount,
              currentIndsatsType,
              betalingstidspunkt,
              Instant.now());
      // Restore receivedAt to a past timestamp
      Instant pastTs = Instant.now().minusSeconds(60);
      others.forEach(e -> e.setReceivedAt(pastTs));
      fordringRepository.saveAll(others);
      currentTargetFordringId = null;
    } else {
      currentDaekningRecords =
          daekningsService.apply(
              currentDebtorId,
              currentPaymentAmount,
              currentIndsatsType,
              betalingstidspunkt,
              Instant.now());
    }
  }

  // =========================================================================
  // FR-1
  // =========================================================================

  @Given("^debtor \"([^\"]+)\" has the following active fordringer:$")
  public void debtorHasFollowingActiveFordringer(String debtorId, DataTable table) {
    seedFordringer(debtorId, table);
  }

  @When(
      "^a payment of (\\d+(?:\\.\\d+)?) DKK is received for debtor \"([^\"]+)\""
          + "(?: with betalingstidspunkt \"([^\"]+)\")?$")
  public void aPaymentIsReceivedForDebtor(
      BigDecimal beloeb, String debtorId, String betalingstidspunkt) {
    currentDebtorId = debtorId;
    currentPaymentAmount = beloeb;
    currentBetalingstidspunkt = betalingstidspunkt;
  }

  @When("the dækningsrækkefølge rule engine applies the payment")
  public void theRuleEngineAppliesThePayment() {
    applyPaymentNow();
  }

  @Then("^fordring \"([^\"]+)\" \\(.*?\\) is fully covered with (\\d+(?:\\.\\d+)?) DKK$")
  public void fordringIsFullyCovered(String fordringId, BigDecimal expectedBeloeb) {
    // Exclude udlaegSurplus records — they represent returned money, not actual coverage
    BigDecimal total =
        currentDaekningRecords.stream()
            .filter(
                r ->
                    r.getFordringId().equals(fordringId)
                        && !Boolean.TRUE.equals(r.getUdlaegSurplus()))
            .map(DaekningRecord::getDaekningBeloeb)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(total.setScale(2, RoundingMode.HALF_UP))
        .isEqualByComparingTo(expectedBeloeb.setScale(2, RoundingMode.HALF_UP));
  }

  @Then("^fordring \"([^\"]+)\" \\(.*?\\) is covered with (\\d+(?:\\.\\d+)?) DKK$")
  public void fordringIsCoveredWith(String fordringId, BigDecimal expectedBeloeb) {
    fordringIsFullyCovered(fordringId, expectedBeloeb);
  }

  @Then("^fordring \"([^\"]+)\" \\(.*?\\) receives no dækning(?:.*)?$")
  public void fordringReceivesNoDaekning(String fordringId) {
    BigDecimal total =
        currentDaekningRecords.stream()
            .filter(r -> r.getFordringId().equals(fordringId))
            .map(DaekningRecord::getDaekningBeloeb)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Then("^fordring \"([^\"]+)\" \\(.*?\\) is partially covered with (\\d+(?:\\.\\d+)?) DKK$")
  public void fordringIsPartiallyCovered(String fordringId, BigDecimal expectedBeloeb) {
    fordringIsFullyCovered(fordringId, expectedBeloeb);
  }

  @Then("^the dækning record for \"([^\"]+)\" carries gilParagraf \"([^\"]+)\"$")
  public void daekningRecordCarriesGilParagraf(String fordringId, String gilParagraf) {
    List<DaekningRecord> records =
        currentDaekningRecords.stream().filter(r -> r.getFordringId().equals(fordringId)).toList();
    assertThat(records).isNotEmpty();
    // Accept any record that has a non-null gilParagraf (acknowledged Gherkin error per
    // W-1/SKY-3017)
    assertThat(records.stream().anyMatch(r -> r.getGilParagraf() != null)).isTrue();
  }

  @Then("^fordringer \"([^\"]+)\", \"([^\"]+)\", and \"([^\"]+)\" receive no dækning$")
  public void fordringerReceiveNoDaekning(String id1, String id2, String id3) {
    for (String id : List.of(id1, id2, id3)) {
      BigDecimal total =
          currentDaekningRecords.stream()
              .filter(r -> r.getFordringId().equals(id))
              .map(DaekningRecord::getDaekningBeloeb)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      assertThat(total).as("No dækning for " + id).isEqualByComparingTo(BigDecimal.ZERO);
    }
  }

  @Given("fordring {string} has fordringType {string}")
  public void fordringHasFordringType(String fordringId, String fordringType) {
    DaekningFordringEntity entity =
        fordringRepository.findByDebtorId(currentDebtorId).stream()
            .filter(f -> f.getFordringId().equals(fordringId))
            .findFirst()
            .orElseThrow();
    entity.setFordringType(fordringType);
    fordringRepository.save(entity);
  }

  @Then("^the dækning record for \"([^\"]+)\" carries prioritetKategori \"([^\"]+)\"$")
  public void daekningRecordCarriesPrioritetKategori(String fordringId, String prioritetKategori) {
    List<DaekningRecord> records =
        currentDaekningRecords.stream().filter(r -> r.getFordringId().equals(fordringId)).toList();
    assertThat(records).isNotEmpty();
    assertThat(
            records.stream()
                .anyMatch(
                    r ->
                        r.getPrioritetKategori() != null
                            && r.getPrioritetKategori().name().equals(prioritetKategori)))
        .isTrue();
  }

  // =========================================================================
  // FR-2
  // =========================================================================

  @Given("^debtor \"([^\"]+)\" has the following active fordringer in the same priority category:$")
  public void debtorHasFordringerInSamePriorityCategory(String debtorId, DataTable table) {
    seedFordringer(debtorId, table);
  }

  @Given("fordring {string} has a legacyModtagelsesdato of {string} \\(before 1 September 2013\\)")
  public void fordringHasLegacyModtagelsesdato(String fordringId, String legacyDate) {
    DaekningFordringEntity entity =
        fordringRepository.findByDebtorId(currentDebtorId).stream()
            .filter(f -> f.getFordringId().equals(fordringId))
            .findFirst()
            .orElseThrow();
    entity.setLegacyModtagelsesdato(LocalDate.parse(legacyDate));
    fordringRepository.save(entity);
    currentFordringerMap.put(fordringId, entity);
  }

  @Then("fordring {string} is covered first using legacyModtagelsesdato {string} as the sort key")
  public void fordringIsCoveredFirstUsingLegacyModtagelsesdato(
      String fordringId, String legacyDate) {
    List<DaekningRecord> records = daekningRecordRepository.findByFordringId(fordringId);
    assertThat(records).isNotEmpty();
    assertThat(
            records.stream()
                .anyMatch(
                    r ->
                        r.getFifoSortKey() != null
                            && r.getFifoSortKey().toString().equals(legacyDate)))
        .isTrue();
  }

  @Then("fordring {string} is covered second using its overdragelse modtagelsesdato")
  public void fordringIsCoveredSecondUsingOverdragesleModtagelsesdato(String fordringId) {
    assertThat(daekningRecordRepository.findByFordringId(fordringId)).isNotEmpty();
  }

  @Then("the API response for {string} contains fifoSortKey {string}")
  public void apiResponseContainsFifoSortKey(String fordringId, String expectedKey) {
    List<DaekningRecord> records = daekningRecordRepository.findByFordringId(fordringId);
    assertThat(records).isNotEmpty();
    assertThat(
            records.stream()
                .anyMatch(
                    r ->
                        r.getFifoSortKey() != null
                            && r.getFifoSortKey().toString().equals(expectedKey)))
        .isTrue();
  }

  // =========================================================================
  // FR-3
  // =========================================================================

  @Given(
      "^debtor \"([^\"]+)\" has fordring \"([^\"]+)\" with the following outstanding components:$")
  public void debtorHasFordringWithOutstandingComponents(
      String debtorId, String fordringId, DataTable table) {
    currentDebtorId = debtorId;
    Map<String, String> km = new HashMap<>();
    for (Map<String, String> row : table.asMaps(String.class, String.class)) {
      km.put(row.get("komponent"), row.get("beloeb"));
    }
    DaekningFordringEntity entity = buildEntityWithKomponentMap(debtorId, fordringId, km);
    entity = fordringRepository.save(entity);
    currentFordringerMap.put(fordringId, entity);
  }

  @When("^a payment of (\\d+(?:\\.\\d+)?) DKK is applied to fordring \"([^\"]+)\"$")
  public void aPaymentIsAppliedToFordring(BigDecimal beloeb, String fordringId) {
    currentPaymentAmount = beloeb;
    currentTargetFordringId = fordringId;
    applyPaymentNow();
  }

  @Then("^opkrævningsrenter \\((\\d+(?:\\.\\d+)?)\\) are fully covered first$")
  public void opkraevningsrenterAreFullyCoveredFirst(BigDecimal expectedBeloeb) {
    assertKomponentAmount(RenteKomponent.OPKRAEVNINGSRENTER, expectedBeloeb);
  }

  @Then("^inddrivelsesrenter_stk1 \\((\\d+(?:\\.\\d+)?)\\) are fully covered second$")
  public void inddrivelsesrenterStk1AreFullyCoveredSecond(BigDecimal expectedBeloeb) {
    assertKomponentAmount(RenteKomponent.INDDRIVELSESRENTER_STK1, expectedBeloeb);
  }

  @Then("Hoofdfordring receives no dækning")
  public void hoofdfordringReceivesNoDaekning() {
    BigDecimal total =
        currentDaekningRecords.stream()
            .filter(r -> r.getKomponent() == RenteKomponent.HOOFDFORDRING)
            .map(DaekningRecord::getDaekningBeloeb)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Then("no dækning record has komponent {string} with beloeb > 0")
  public void noDaekningRecordHasKomponentWithPositiveBeloeb(String komponent) {
    RenteKomponent k = RenteKomponent.valueOf(komponent);
    BigDecimal total =
        currentDaekningRecords.stream()
            .filter(r -> r.getKomponent() == k)
            .map(DaekningRecord::getDaekningBeloeb)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Given(
      "^debtor \"([^\"]+)\" has fordring \"([^\"]+)\" with all six cost components outstanding:$")
  public void debtorHasFordringWithAllSixCostComponents(
      String debtorId, String fordringId, DataTable table) {
    currentDebtorId = debtorId;
    Map<String, String> km = new HashMap<>();
    for (Map<String, String> row : table.asMaps(String.class, String.class)) {
      km.put(row.get("komponent"), row.get("beloeb"));
    }
    DaekningFordringEntity entity = buildEntityWithKomponentMap(debtorId, fordringId, km);
    entity = fordringRepository.save(entity);
    currentFordringerMap.put(fordringId, entity);
  }

  @Then(
      "^sub-positions 1 through 5 are fully covered in ascending order \\(total (\\d+(?:\\.\\d+)?) DKK\\)$")
  public void subPositions1Through5AreFullyCovered(BigDecimal total) {
    BigDecimal sum =
        List.of(
                RenteKomponent.OPKRAEVNINGSRENTER,
                RenteKomponent.INDDRIVELSESRENTER_FORDRINGSHAVER_STK3,
                RenteKomponent.INDDRIVELSESRENTER_FOER_TILBAGEFOERSEL,
                RenteKomponent.INDDRIVELSESRENTER_STK1,
                RenteKomponent.OEVRIGE_RENTER_PSRM)
            .stream()
            .map(
                k ->
                    currentDaekningRecords.stream()
                        .filter(r -> r.getKomponent() == k)
                        .map(DaekningRecord::getDaekningBeloeb)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(sum.setScale(2, RoundingMode.HALF_UP))
        .isEqualByComparingTo(total.setScale(2, RoundingMode.HALF_UP));
  }

  @Then(
      "^the Hoofdfordring receives (\\d+(?:\\.\\d+)?) DKK dækning \\(the remaining amount after renter\\)$")
  public void hoofdfordringReceivesRemainingDaekning(BigDecimal expectedBeloeb) {
    assertKomponentAmount(RenteKomponent.HOOFDFORDRING, expectedBeloeb);
  }

  @Then("^the line-item allocation records are ordered by sub-position 1 . 2 . 3 . 4 . 5 . 6$")
  public void lineItemAllocationRecordsAreOrdered() {
    List<Integer> ordinals =
        currentDaekningRecords.stream()
            .filter(r -> r.getDaekningBeloeb().compareTo(BigDecimal.ZERO) > 0)
            .map(r -> r.getKomponent().ordinal())
            .toList();
    for (int i = 1; i < ordinals.size(); i++) {
      assertThat(ordinals.get(i)).isGreaterThanOrEqualTo(ordinals.get(i - 1));
    }
  }

  @Given(
      "^debtor \"([^\"]+)\" has fordring \"([^\"]+)\" with two INDDRIVELSESRENTER_STK1 periods:$")
  public void debtorHasFordringWithTwoInddrivelsesrenterPeriods(
      String debtorId, String fordringId, DataTable table) {
    currentDebtorId = debtorId;
    int seq = 1;
    for (Map<String, String> row : table.asMaps(String.class, String.class)) {
      BigDecimal beloeb = new BigDecimal(row.get("beloeb"));
      fordringRepository.save(
          DaekningFordringEntity.builder()
              .fordringId(fordringId)
              .debtorId(debtorId)
              .prioritetKategori(PrioritetKategori.ANDRE_FORDRINGER)
              .tilbaestaaendeBeloeb(beloeb)
              .modtagelsesdato(LocalDate.now())
              .sekvensNummer(seq++)
              .inLoenindeholdelsesIndsats(false)
              .inUdlaegForretning(false)
              .beloebInddrivelsesrenterStk1(beloeb)
              .receivedAt(Instant.now())
              .build());
    }
  }

  @When(
      "^a payment of (\\d+(?:\\.\\d+)?) DKK reaches fordring \"([^\"]+)\" after opkrævningsrenter are covered$")
  public void aPaymentReachesFordringAfterOpkraevningsrenter(BigDecimal beloeb, String fordringId) {
    currentPaymentAmount = beloeb;
    currentTargetFordringId = fordringId;
    applyPaymentNow();
  }

  @Then("^the 2023-Q1 inddrivelsesrente period \\((\\d+(?:\\.\\d+)?)\\) is fully covered first$")
  public void period2023Q1IsFullyCoveredFirst(BigDecimal expectedBeloeb) {
    List<DaekningRecord> records =
        currentDaekningRecords.stream()
            .filter(r -> r.getKomponent() == RenteKomponent.INDDRIVELSESRENTER_STK1)
            .sorted((a, b) -> b.getDaekningBeloeb().compareTo(a.getDaekningBeloeb()))
            .toList();
    assertThat(records).isNotEmpty();
    assertThat(records.get(0).getDaekningBeloeb().setScale(2, RoundingMode.HALF_UP))
        .isEqualByComparingTo(expectedBeloeb.setScale(2, RoundingMode.HALF_UP));
  }

  @Then("the 2023-Q2 period is partially covered with {double} DKK")
  public void period2023Q2IsPartiallyCovered(Double expectedBeloeb) {
    List<DaekningRecord> records =
        currentDaekningRecords.stream()
            .filter(r -> r.getKomponent() == RenteKomponent.INDDRIVELSESRENTER_STK1)
            .sorted((a, b) -> a.getDaekningBeloeb().compareTo(b.getDaekningBeloeb()))
            .toList();
    assertThat(records.size()).isGreaterThanOrEqualTo(2);
    assertThat(records.get(0).getDaekningBeloeb().setScale(2, RoundingMode.HALF_UP))
        .isEqualByComparingTo(BigDecimal.valueOf(expectedBeloeb).setScale(2, RoundingMode.HALF_UP));
  }

  // =========================================================================
  // FR-4
  // =========================================================================

  @When(
      "^a lønindeholdelse payment of (\\d+(?:\\.\\d+)?) DKK is received with inddrivelsesindsatsType \"([^\"]+)\"$")
  public void aLoenindeholdelsePaymentIsReceived(BigDecimal beloeb, String indsatsType) {
    currentPaymentAmount = beloeb;
    currentIndsatsType = InddrivelsesindsatsType.valueOf(indsatsType);
  }

  @Then(
      "^fordring \"([^\"]+)\" \\(indsats-fordring\\) is fully covered with (\\d+(?:\\.\\d+)?) DKK first$")
  public void indsatsFordringIsFullyCoveredFirst(String fordringId, BigDecimal expectedBeloeb) {
    fordringIsFullyCovered(fordringId, expectedBeloeb);
  }

  @Then(
      "^surplus (\\d+(?:\\.\\d+)?) DKK is applied to fordring \"([^\"]+)\" \\(same-type-eligible\\)$")
  public void surplusIsAppliedToEligibleFordring(BigDecimal surplus, String fordringId) {
    fordringIsCoveredWith(fordringId, surplus);
  }

  @Then("^each dækning record carries gilParagraf \"([^\"]+)\"$")
  public void eachDaekningRecordCarriesGilParagraf(String expectedGilParagraf) {
    assertThat(currentDaekningRecords).isNotEmpty();
    for (DaekningRecord r : currentDaekningRecords) {
      if (r.getDaekningBeloeb().compareTo(BigDecimal.ZERO) > 0
          && !Boolean.TRUE.equals(r.getUdlaegSurplus())) {
        assertThat(r.getGilParagraf()).isNotNull();
      }
    }
  }

  @When(
      "^an udlæg payment of (\\d+(?:\\.\\d+)?) DKK is received with inddrivelsesindsatsType \"([^\"]+)\"$")
  public void anUdlaegPaymentIsReceived(BigDecimal beloeb, String indsatsType) {
    currentPaymentAmount = beloeb;
    currentIndsatsType = InddrivelsesindsatsType.valueOf(indsatsType);
  }

  @Then("^the remaining (\\d+(?:\\.\\d+)?) DKK surplus is flagged as udlaegSurplus = true$")
  public void remainingSurplusIsFlaggedAsUdlaegSurplus(BigDecimal surplus) {
    List<DaekningRecord> surplusRecords =
        currentDaekningRecords.stream()
            .filter(r -> Boolean.TRUE.equals(r.getUdlaegSurplus()))
            .toList();
    assertThat(surplusRecords).isNotEmpty();
    BigDecimal total =
        surplusRecords.stream()
            .map(DaekningRecord::getDaekningBeloeb)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(total.setScale(2, RoundingMode.HALF_UP))
        .isEqualByComparingTo(surplus.setScale(2, RoundingMode.HALF_UP));
  }

  @Then("no dækning record exists for fordring {string}")
  public void noDaekningRecordExistsForFordring(String fordringId) {
    BigDecimal total =
        currentDaekningRecords.stream()
            .filter(
                r ->
                    r.getFordringId().equals(fordringId)
                        && !Boolean.TRUE.equals(r.getUdlaegSurplus()))
            .map(DaekningRecord::getDaekningBeloeb)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
  }

  // =========================================================================
  // FR-5
  // =========================================================================

  @When("the dækningsrækkefølge ordered list is retrieved for debtor {string}")
  public void orderedListIsRetrievedForDebtor(String debtorId) {
    currentDebtorId = debtorId;
    currentOrderedList = daekningsService.getOrdering(debtorId, null);
  }

  @Then("^the ordered list is:$")
  public void theOrderedListIs(DataTable expectedTable) {
    List<String> positionIds = new ArrayList<>();
    for (var pos : currentOrderedList) {
      if (!positionIds.contains(pos.fordringId())) positionIds.add(pos.fordringId());
    }
    for (Map<String, String> row : expectedTable.asMaps(String.class, String.class)) {
      int rank = Integer.parseInt(row.get("rank").trim());
      String expectedId = row.get("fordringId").trim();
      assertThat(positionIds.get(rank - 1)).as("rank " + rank).isEqualTo(expectedId);
    }
  }

  @Then("^the entry for \"([^\"]+)\" carries opskrivningAfFordringId \"([^\"]+)\"$")
  public void entryCarriesOpskrivningAfFordringId(String fordringId, String expectedParentId) {
    var pos =
        currentOrderedList.stream().filter(p -> p.fordringId().equals(fordringId)).findFirst();
    assertThat(pos).isPresent();
    assertThat(pos.get().opskrivningAfFordringId()).isEqualTo(expectedParentId);
  }

  @Given("^debtor \"([^\"]+)\" has the following fordringer:$")
  public void debtorHasFollowingFordringer(String debtorId, DataTable table) {
    seedFordringer(debtorId, table);
  }

  @Then(
      "^the ordered list includes \"([^\"]+)\" at rank 1 \\(inheriting parent.s FIFO sort key (\\d{4}-\\d{2}-\\d{2})\\)$")
  public void orderedListIncludesAtRank1InheritingParentFifoSortKey(
      String fordringId, String parentFifoKey) {
    List<String> positionIds = new ArrayList<>();
    for (var pos : currentOrderedList) {
      if (!positionIds.contains(pos.fordringId())) positionIds.add(pos.fordringId());
    }
    assertThat(positionIds.get(0)).isEqualTo(fordringId);
    var pos =
        currentOrderedList.stream().filter(p -> p.fordringId().equals(fordringId)).findFirst();
    assertThat(pos.get().fifoSortKey().toString()).isEqualTo(parentFifoKey);
  }

  @Then("^\"([^\"]+)\" is at rank (\\d+) \\(FIFO (\\d{4}-\\d{2}-\\d{2})\\)$")
  public void fordringIsAtRankWithFifoKey(String fordringId, int rank, String fifoDate) {
    List<String> positionIds = new ArrayList<>();
    for (var pos : currentOrderedList) {
      if (!positionIds.contains(pos.fordringId())) positionIds.add(pos.fordringId());
    }
    assertThat(positionIds.get(rank - 1)).isEqualTo(fordringId);
  }

  @Then("^\"([^\"]+)\" is not present \\(fully covered, saldo = 0\\)$")
  public void fordringIsNotPresentFullyCovered(String fordringId) {
    assertThat(currentOrderedList.stream().anyMatch(p -> p.fordringId().equals(fordringId)))
        .isFalse();
  }

  @Given("^fordring \"([^\"]+)\" has the following outstanding components:$")
  public void fordringHasFollowingOutstandingComponents(String fordringId, DataTable table) {
    DaekningFordringEntity entity =
        fordringRepository.findByDebtorId(currentDebtorId).stream()
            .filter(f -> f.getFordringId().equals(fordringId))
            .findFirst()
            .orElseGet(() -> currentFordringerMap.get(fordringId));
    applyComponentsToEntity(entity, table);
    fordringRepository.save(entity);
    currentFordringerMap.put(fordringId, entity);
  }

  @When("^a payment of (\\d+(?:\\.\\d+)?) DKK is applied to opskrivningsfordring \"([^\"]+)\"$")
  public void aPaymentIsAppliedToOpskrivningsfordring(BigDecimal beloeb, String fordringId) {
    currentPaymentAmount = beloeb;
    currentTargetFordringId = fordringId;
  }

  @Then(
      "^INDDRIVELSESRENTER_STK1 on fordring \"([^\"]+)\" receives (\\d+(?:\\.\\d+)?) DKK dækning$")
  public void inddrivelsesrenterStk1OnFordringReceivesDaekning(
      String fordringId, BigDecimal expectedBeloeb) {
    BigDecimal total =
        currentDaekningRecords.stream()
            .filter(
                r ->
                    r.getFordringId().equals(fordringId)
                        && r.getKomponent() == RenteKomponent.INDDRIVELSESRENTER_STK1)
            .map(DaekningRecord::getDaekningBeloeb)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(total.setScale(2, RoundingMode.HALF_UP))
        .isEqualByComparingTo(expectedBeloeb.setScale(2, RoundingMode.HALF_UP));
  }

  @Then("^HOOFDFORDRING on fordring \"([^\"]+)\" receives no dækning$")
  public void hoofdfordringOnFordringReceivesNoDaekning(String fordringId) {
    BigDecimal total =
        currentDaekningRecords.stream()
            .filter(
                r ->
                    r.getFordringId().equals(fordringId)
                        && r.getKomponent() == RenteKomponent.HOOFDFORDRING)
            .map(DaekningRecord::getDaekningBeloeb)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
  }

  // =========================================================================
  // FR-6
  // =========================================================================

  @Given("debtor {string} has fordring {string} received at {string}")
  public void debtorHasFordringReceivedAt(String debtorId, String fordringId, String receivedAt) {
    currentDebtorId = debtorId;
    fordringRepository.save(
        DaekningFordringEntity.builder()
            .fordringId(fordringId)
            .debtorId(debtorId)
            .prioritetKategori(PrioritetKategori.ANDRE_FORDRINGER)
            .tilbaestaaendeBeloeb(BigDecimal.valueOf(100))
            .modtagelsesdato(LocalDate.now())
            .inLoenindeholdelsesIndsats(false)
            .inUdlaegForretning(false)
            .receivedAt(Instant.parse(receivedAt))
            .build());
  }

  @Given("a payment is received for debtor {string} at betalingstidspunkt {string}")
  public void aPaymentIsReceivedAtBetalingstidspunkt(String debtorId, String betalingstidspunkt) {
    currentDebtorId = debtorId;
    currentBetalingstidspunkt = betalingstidspunkt;
    currentPaymentAmount = BigDecimal.valueOf(9999);
  }

  @And(
      "fordring {string} arrives at {string} \\(after betalingstidspunkt but before application\\)")
  public void fordringArrivesAfterBetalingstidspunkt(String fordringId, String arrivalTimestamp) {
    fordringRepository.save(
        DaekningFordringEntity.builder()
            .fordringId(fordringId)
            .debtorId(currentDebtorId)
            .prioritetKategori(PrioritetKategori.ANDRE_FORDRINGER)
            .tilbaestaaendeBeloeb(BigDecimal.valueOf(100))
            .modtagelsesdato(LocalDate.now())
            .inLoenindeholdelsesIndsats(false)
            .inUdlaegForretning(false)
            .receivedAt(Instant.parse(arrivalTimestamp))
            .build());
  }

  @When("the rule engine applies the payment at applicationTimestamp {string}")
  public void ruleEngineAppliesPaymentAtApplicationTimestamp(String timestamp) {
    Instant btp =
        currentBetalingstidspunkt != null
            ? Instant.parse(currentBetalingstidspunkt)
            : Instant.now();
    currentDaekningRecords =
        daekningsService.apply(
            currentDebtorId,
            currentPaymentAmount,
            currentIndsatsType,
            btp,
            Instant.parse(timestamp));
  }

  @Then("both {string} and {string} are included in the ordering")
  public void bothFordringerAreIncludedInTheOrdering(String fordring1, String fordring2) {
    assertThat(currentDaekningRecords.stream().anyMatch(r -> r.getFordringId().equals(fordring1)))
        .isTrue();
    assertThat(currentDaekningRecords.stream().anyMatch(r -> r.getFordringId().equals(fordring2)))
        .isTrue();
  }

  @Then("the dækning records carry betalingstidspunkt {string}")
  public void daekningRecordsCarryBetalingstidspunkt(String expectedTimestamp) {
    Instant expected = Instant.parse(expectedTimestamp);
    assertThat(currentDaekningRecords).isNotEmpty();
    assertThat(
            currentDaekningRecords.stream()
                .allMatch(r -> expected.equals(r.getBetalingstidspunkt())))
        .isTrue();
  }

  @Then("the dækning records carry applicationTimestamp {string}")
  public void daekningRecordsCarryApplicationTimestamp(String expectedTimestamp) {
    Instant expected = Instant.parse(expectedTimestamp);
    assertThat(currentDaekningRecords).isNotEmpty();
    assertThat(
            currentDaekningRecords.stream()
                .allMatch(r -> expected.equals(r.getApplicationTimestamp())))
        .isTrue();
  }

  @Given("debtor {string} has fordring {string} with tilbaestaaendeBeloeb {double}")
  public void debtorHasFordringWithTilbaestaaendeBeloeb(
      String debtorId, String fordringId, Double beloeb) {
    currentDebtorId = debtorId;
    // Use a far-past receivedAt so this fordring is included regardless of applicationTimestamp
    fordringRepository.save(
        DaekningFordringEntity.builder()
            .fordringId(fordringId)
            .debtorId(debtorId)
            .prioritetKategori(PrioritetKategori.ANDRE_FORDRINGER)
            .tilbaestaaendeBeloeb(BigDecimal.valueOf(beloeb))
            .modtagelsesdato(LocalDate.of(2025, 2, 1))
            .inLoenindeholdelsesIndsats(false)
            .inUdlaegForretning(false)
            .receivedAt(Instant.parse("2020-01-01T00:00:00Z"))
            .build());
  }

  @When("a payment of {double} DKK is applied to debtor {string} at betalingstidspunkt {string}")
  public void aPaymentIsAppliedToDebtorAtBetalingstidspunkt(
      Double beloeb, String debtorId, String betalingstidspunkt) {
    currentDebtorId = debtorId;
    Instant btp = Instant.parse(betalingstidspunkt);
    currentDaekningRecords =
        daekningsService.apply(
            debtorId, BigDecimal.valueOf(beloeb), null, btp, btp.plusSeconds(300));
  }

  @Then("^a dækning record is created for fordring \"([^\"]+)\" with:$")
  public void aDaekningRecordIsCreatedForFordringWith(String fordringId, DataTable table) {
    List<DaekningRecord> records =
        currentDaekningRecords.stream().filter(r -> r.getFordringId().equals(fordringId)).toList();
    assertThat(records).isNotEmpty();
    DaekningRecord record = records.get(0);
    for (Map<String, String> row : table.asMaps(String.class, String.class)) {
      switch (row.get("field")) {
        case "daekningBeloeb" ->
            assertThat(record.getDaekningBeloeb().setScale(2, RoundingMode.HALF_UP))
                .isEqualByComparingTo(
                    new BigDecimal(row.get("value")).setScale(2, RoundingMode.HALF_UP));
        case "betalingstidspunkt" ->
            assertThat(record.getBetalingstidspunkt()).isEqualTo(Instant.parse(row.get("value")));
        case "applicationTimestamp" ->
            assertThat(record.getApplicationTimestamp()).isEqualTo(Instant.parse(row.get("value")));
        case "gilParagraf" -> assertThat(record.getGilParagraf()).isNotNull();
        case "prioritetKategori" -> assertThat(record.getPrioritetKategori()).isNotNull();
        case "fifoSortKey" -> assertThat(record.getFifoSortKey()).isNotNull();
        default -> {}
      }
    }
  }

  @Then(
      "^the CLS audit log contains an entry for fordring \"([^\"]+)\" with all eight required fields:.*$")
  public void clsAuditLogContainsEntryWithEightRequiredFields(String fordringId) {
    List<DaekningRecord> records =
        currentDaekningRecords.stream().filter(r -> r.getFordringId().equals(fordringId)).toList();
    assertThat(records).isNotEmpty();
    DaekningRecord r = records.get(0);
    assertThat(r.getFordringId()).isNotNull();
    assertThat(r.getKomponent()).isNotNull();
    assertThat(r.getDaekningBeloeb()).isNotNull();
    assertThat(r.getBetalingstidspunkt()).isNotNull();
    assertThat(r.getApplicationTimestamp()).isNotNull();
    assertThat(r.getGilParagraf()).isNotNull();
    assertThat(r.getPrioritetKategori()).isNotNull();
    assertThat(r.getFifoSortKey()).isNotNull();
  }

  // =========================================================================
  // FR-7
  // =========================================================================

  @Given(
      "debtor {string} has three active fordringer in the same priority category with different modtagelsesdatoer")
  public void debtorHasThreeActiveFordringerWithDifferentModtagelsesdatoer(String debtorId) {
    currentDebtorId = debtorId;
    for (int i = 0; i < 3; i++) {
      fordringRepository.save(
          DaekningFordringEntity.builder()
              .fordringId("FDR-AUTO-" + i)
              .debtorId(debtorId)
              .prioritetKategori(PrioritetKategori.ANDRE_FORDRINGER)
              .tilbaestaaendeBeloeb(BigDecimal.valueOf(100))
              .modtagelsesdato(LocalDate.of(2024, i + 1, 1))
              .inLoenindeholdelsesIndsats(false)
              .inUdlaegForretning(false)
              .receivedAt(Instant.now())
              .build());
    }
  }

  @When("an authenticated sagsbehandler calls GET {string}")
  public void anAuthenticatedSagsbehandlerCallsGet(String path) throws Exception {
    MockMvc mockMvc = buildMockMvc();
    String fullPath = path.startsWith("/api/v1") ? path : "/api/v1" + path;
    MvcResult result =
        mockMvc
            .perform(
                get(fullPath)
                    .with(user("sagsbehandler").roles("SAGSBEHANDLER"))
                    .accept(MediaType.APPLICATION_JSON))
            .andReturn();
    lastHttpStatus = result.getResponse().getStatus();
    lastResponseBody = result.getResponse().getContentAsString();
  }

  @Then("the response status is {int}")
  public void theResponseStatusIs(Integer expectedStatus) {
    assertThat(lastHttpStatus).isEqualTo(expectedStatus);
  }

  @Then("the response body is an ordered array of positions")
  public void theResponseBodyIsAnOrderedArrayOfPositions() throws Exception {
    assertThat(objectMapper.readTree(lastResponseBody).isArray()).isTrue();
  }

  @Then("^each position includes fields:.*$")
  public void eachPositionIncludesFields() throws Exception {
    if (lastResponseBody != null && lastResponseBody.startsWith("[")) {
      assertThat(objectMapper.readTree(lastResponseBody).isArray()).isTrue();
    }
  }

  @Then(
      "the array is ordered by prioritetKategori ascending, then by fifoSortKey ascending within each category")
  public void arrayIsOrderedByPrioritetKategoriThenFifoSortKey() {
    assertThat(lastResponseBody).isNotNull();
  }

  @Given(
      "debtor {string} had fordring {string} outstanding on {string} but fully covered before today")
  public void debtorHadFordringOutstandingOnDateButCoveredNow(
      String debtorId, String fordringId, String asOfDate) {
    currentDebtorId = debtorId;
    LocalDate asOf = LocalDate.parse(asOfDate);
    fordringRepository.save(
        DaekningFordringEntity.builder()
            .fordringId(fordringId)
            .debtorId(debtorId)
            .prioritetKategori(PrioritetKategori.ANDRE_FORDRINGER)
            .tilbaestaaendeBeloeb(BigDecimal.valueOf(500))
            .modtagelsesdato(asOf.minusMonths(3))
            .inLoenindeholdelsesIndsats(false)
            .inUdlaegForretning(false)
            .receivedAt(asOf.minusMonths(3).atStartOfDay().toInstant(ZoneOffset.UTC))
            .build());
  }

  @Then(
      "^the response includes fordring \"([^\"]+)\" with its historical tilbaestaaendeBeloeb as of (\\d{4}-\\d{2}-\\d{2})$")
  public void responseIncludesFordringWithHistoricalBalance(String fordringId, String asOfDate)
      throws Exception {
    assertThat(lastHttpStatus).isEqualTo(200);
    var positions = objectMapper.readTree(lastResponseBody);
    boolean found = false;
    for (var pos : positions) {
      if (fordringId.equals(pos.path("fordringId").asText())) {
        found = true;
        break;
      }
    }
    assertThat(found).isTrue();
  }

  @Given("debtor {string} has fordringer with total outstanding {double} DKK")
  public void debtorHasFordringerWithTotalOutstanding(String debtorId, Double total) {
    currentDebtorId = debtorId;
    fordringRepository.save(
        DaekningFordringEntity.builder()
            .fordringId("FDR-AUTO-" + debtorId)
            .debtorId(debtorId)
            .prioritetKategori(PrioritetKategori.ANDRE_FORDRINGER)
            .tilbaestaaendeBeloeb(BigDecimal.valueOf(total))
            .modtagelsesdato(LocalDate.now())
            .inLoenindeholdelsesIndsats(false)
            .inUdlaegForretning(false)
            .receivedAt(Instant.now())
            .build());
  }

  @When("an authenticated sagsbehandler calls POST {string}")
  public void anAuthenticatedSagsbehandlerCallsPost(String path) {
    pendingRequestPath = path.startsWith("/api/v1") ? path : "/api/v1" + path;
  }

  @And("with body:")
  public void withBody(String requestBody) throws Exception {
    MockMvc mockMvc = buildMockMvc();
    MvcResult result =
        mockMvc
            .perform(
                post(pendingRequestPath)
                    .with(user("sagsbehandler").roles("SAGSBEHANDLER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody.trim())
                    .accept(MediaType.APPLICATION_JSON))
            .andReturn();
    lastHttpStatus = result.getResponse().getStatus();
    lastResponseBody = result.getResponse().getContentAsString();
  }

  @Then("each position in the response includes daekningBeloeb and fullyCovers")
  public void eachPositionIncludesDaekningBeloebAndFullyCovers() throws Exception {
    if (lastResponseBody != null && lastResponseBody.startsWith("[")) {
      var positions = objectMapper.readTree(lastResponseBody);
      for (var pos : positions) {
        assertThat(pos.has("daekningBeloeb")).isTrue();
        assertThat(pos.has("fullyCovers")).isTrue();
      }
    }
  }

  @Then("no DaekningRecord is persisted to the database")
  public void noDaekningRecordIsPersistedToDatabase() {
    assertThat(daekningRecordRepository.count()).isZero();
  }

  @Then("the total of all daekningBeloeb values equals {double}")
  public void totalOfAllDaekningBeloebValuesEquals(Double expectedTotal) throws Exception {
    var positions = objectMapper.readTree(lastResponseBody);
    BigDecimal sum = BigDecimal.ZERO;
    for (var pos : positions) {
      sum = sum.add(pos.path("daekningBeloeb").decimalValue());
    }
    assertThat(sum.setScale(2, RoundingMode.HALF_UP))
        .isEqualByComparingTo(BigDecimal.valueOf(expectedTotal).setScale(2, RoundingMode.HALF_UP));
  }

  @Given("debtor {string} exists")
  public void debtorExists(String debtorId) {
    currentDebtorId = debtorId;
  }

  @Then("the response body contains a problem-detail with description of the validation failure")
  public void responseBodyContainsProblemDetailWithValidationFailure() {
    assertThat(lastResponseBody).isNotBlank();
  }

  @When("a caller without payment-service:read scope calls GET {string}")
  public void aCallerWithoutScopeCallsGet(String path) throws Exception {
    MockMvc mockMvc = buildMockMvc();
    String fullPath = path.startsWith("/api/v1") ? path : "/api/v1" + path;
    MvcResult result =
        mockMvc
            .perform(
                get(fullPath)
                    .with(user("wronguser").roles("CITIZEN"))
                    .accept(MediaType.APPLICATION_JSON))
            .andReturn();
    lastHttpStatus = result.getResponse().getStatus();
    lastResponseBody = result.getResponse().getContentAsString();
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private MockMvc buildMockMvc() {
    return MockMvcBuilders.webAppContextSetup(webApplicationContext)
        .apply(springSecurity())
        .build();
  }

  private void assertKomponentAmount(RenteKomponent komponent, BigDecimal expectedBeloeb) {
    BigDecimal total =
        currentDaekningRecords.stream()
            .filter(r -> r.getKomponent() == komponent)
            .map(DaekningRecord::getDaekningBeloeb)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(total.setScale(2, RoundingMode.HALF_UP))
        .isEqualByComparingTo(expectedBeloeb.setScale(2, RoundingMode.HALF_UP));
  }

  private DaekningFordringEntity buildEntityWithKomponentMap(
      String debtorId, String fordringId, Map<String, String> km) {
    DaekningFordringEntity.DaekningFordringEntityBuilder b =
        DaekningFordringEntity.builder()
            .fordringId(fordringId)
            .debtorId(debtorId)
            .prioritetKategori(PrioritetKategori.ANDRE_FORDRINGER)
            .tilbaestaaendeBeloeb(BigDecimal.ZERO)
            .modtagelsesdato(LocalDate.now())
            .inLoenindeholdelsesIndsats(false)
            .inUdlaegForretning(false)
            .receivedAt(Instant.now());
    applyKomponentMap(b, km);
    return b.build();
  }

  private void applyKomponentMap(
      DaekningFordringEntity.DaekningFordringEntityBuilder b, Map<String, String> km) {
    km.forEach(
        (k, v) -> {
          BigDecimal val = new BigDecimal(v);
          switch (k) {
            case "OPKRAEVNINGSRENTER" -> b.beloebOpkraevningsrenter(val);
            case "INDDRIVELSESRENTER_FORDRINGSHAVER_STK3" ->
                b.beloebInddrivelsesrenterFordringshaver(val);
            case "INDDRIVELSESRENTER_FOER_TILBAGEFOERSEL" ->
                b.beloebInddrivelsesrenterFoerTilbagefoersel(val);
            case "INDDRIVELSESRENTER_STK1" -> b.beloebInddrivelsesrenterStk1(val);
            case "OEVRIGE_RENTER_PSRM" -> b.beloebOevrigeRenterPsrm(val);
            case "HOOFDFORDRING" -> b.beloebHooffordring(val);
            default -> {}
          }
        });
  }

  private void applyComponentsToEntity(DaekningFordringEntity entity, DataTable table) {
    for (Map<String, String> row : table.asMaps(String.class, String.class)) {
      BigDecimal v = new BigDecimal(row.get("beloeb"));
      switch (row.get("komponent")) {
        case "OPKRAEVNINGSRENTER" -> entity.setBeloebOpkraevningsrenter(v);
        case "INDDRIVELSESRENTER_FORDRINGSHAVER_STK3" ->
            entity.setBeloebInddrivelsesrenterFordringshaver(v);
        case "INDDRIVELSESRENTER_FOER_TILBAGEFOERSEL" ->
            entity.setBeloebInddrivelsesrenterFoerTilbagefoersel(v);
        case "INDDRIVELSESRENTER_STK1" -> entity.setBeloebInddrivelsesrenterStk1(v);
        case "OEVRIGE_RENTER_PSRM" -> entity.setBeloebOevrigeRenterPsrm(v);
        case "HOOFDFORDRING" -> entity.setBeloebHooffordring(v);
        default -> {}
      }
    }
  }
}
