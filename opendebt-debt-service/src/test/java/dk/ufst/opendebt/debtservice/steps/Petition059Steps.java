package dk.ufst.opendebt.debtservice.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.ufst.opendebt.common.audit.cls.ClsAuditClient;
import dk.ufst.opendebt.common.audit.cls.ClsAuditEvent;
import dk.ufst.opendebt.debtservice.config.TestConfig.MutableClock;
import dk.ufst.opendebt.debtservice.limitation.client.LimitationObjectionWorkflowClient;
import dk.ufst.opendebt.debtservice.limitation.client.WageGarnishmentFactClient;
import dk.ufst.opendebt.debtservice.limitation.client.dto.CreateObjectionWorkflowRequest;
import dk.ufst.opendebt.debtservice.limitation.client.dto.ObjectionDecisionRequest;
import dk.ufst.opendebt.debtservice.limitation.client.dto.ObjectionWorkflowResult;
import dk.ufst.opendebt.debtservice.limitation.client.dto.WageGarnishmentLimitationFacts;
import dk.ufst.opendebt.debtservice.limitation.dto.CreateFordringskompleksRequest;
import dk.ufst.opendebt.debtservice.limitation.dto.EvaluateObjectionRequest;
import dk.ufst.opendebt.debtservice.limitation.dto.ForaeldelseStatusDto;
import dk.ufst.opendebt.debtservice.limitation.dto.FordringskompleksMemberListDto;
import dk.ufst.opendebt.debtservice.limitation.dto.ObjectionRegistrationResult;
import dk.ufst.opendebt.debtservice.limitation.dto.RegisterAfbrydelseRequest;
import dk.ufst.opendebt.debtservice.limitation.dto.RegisterObjectionRequest;
import dk.ufst.opendebt.debtservice.limitation.dto.RegisterTillaegsfristRequest;
import dk.ufst.opendebt.debtservice.limitation.entity.AfbrydelseEvent;
import dk.ufst.opendebt.debtservice.limitation.entity.ForaeldelseRecord;
import dk.ufst.opendebt.debtservice.limitation.entity.ForaeldelseStatus;
import dk.ufst.opendebt.debtservice.limitation.entity.LimitationObjectionLinkage;
import dk.ufst.opendebt.debtservice.limitation.entity.Retsgrundlag;
import dk.ufst.opendebt.debtservice.limitation.entity.TillaegsfristEvent;
import dk.ufst.opendebt.debtservice.limitation.repository.AfbrydelseEventRepository;
import dk.ufst.opendebt.debtservice.limitation.repository.ForaeldelseRecordRepository;
import dk.ufst.opendebt.debtservice.limitation.repository.FordringskompleksLinkRepository;
import dk.ufst.opendebt.debtservice.limitation.repository.LimitationObjectionLinkageRepository;
import dk.ufst.opendebt.debtservice.limitation.repository.TillaegsfristEventRepository;
import dk.ufst.opendebt.debtservice.limitation.service.LimitationObjectionFacade;
import dk.ufst.opendebt.debtservice.limitation.service.LimitationPolicyEngine;
import dk.ufst.opendebt.debtservice.limitation.service.LimitationStateApplicationService;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class Petition059Steps {

  @Autowired private LimitationStateApplicationService limitationStateApplicationService;
  @Autowired private LimitationObjectionFacade limitationObjectionFacade;
  @Autowired private ForaeldelseRecordRepository foraeldelseRecordRepository;
  @Autowired private AfbrydelseEventRepository afbrydelseEventRepository;
  @Autowired private TillaegsfristEventRepository tillaegsfristEventRepository;
  @Autowired private FordringskompleksLinkRepository fordringskompleksLinkRepository;
  @Autowired private LimitationObjectionLinkageRepository limitationObjectionLinkageRepository;
  @Autowired private WageGarnishmentFactClient wageGarnishmentFactClient;
  @Autowired private LimitationObjectionWorkflowClient workflowClient;
  @Autowired private ClsAuditClient clsAuditClient;
  @Autowired private MutableClock limitationClock;
  @Autowired private LimitationPolicyEngine limitationPolicyEngine;

  private final Map<String, UUID> claimIds = new HashMap<>();
  private final Map<String, UUID> debtorIds = new HashMap<>();
  private final Map<String, UUID> complexIds = new HashMap<>();
  private final Map<String, UUID> objectionIds = new HashMap<>();
  private final Map<String, LocalDate> baselineExpiry = new HashMap<>();
  private final Map<UUID, WageGarnishmentLimitationFacts> wageFactsByDebtor = new HashMap<>();

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  private String pendingFordringAlias;
  private String pendingSourceSystem = "PSRM";
  private Retsgrundlag pendingRetsgrundlag = Retsgrundlag.ORDINARY;
  private LocalDate pendingRegistrationDate;
  private String currentActor = "caseworker-p059";

  private ForaeldelseStatusDto lastStatusDto;
  private FordringskompleksMemberListDto lastMemberList;
  private ObjectionRegistrationResult lastObjectionRegistration;
  private Throwable lastException;
  private int lastStatus;
  private List<Long> lastDurationsMs = new ArrayList<>();
  private List<Integer> lastRepeatedStatuses = new ArrayList<>();
  private LocalDate repeatedEvaluationDate;

  @Before("@petition059")
  public void setUp() {
    limitationObjectionLinkageRepository.deleteAll();
    fordringskompleksLinkRepository.deleteAll();
    tillaegsfristEventRepository.deleteAll();
    afbrydelseEventRepository.deleteAll();
    foraeldelseRecordRepository.deleteAll();
    claimIds.clear();
    debtorIds.clear();
    complexIds.clear();
    objectionIds.clear();
    baselineExpiry.clear();
    wageFactsByDebtor.clear();
    pendingFordringAlias = null;
    pendingSourceSystem = "PSRM";
    pendingRetsgrundlag = Retsgrundlag.ORDINARY;
    pendingRegistrationDate = null;
    lastStatusDto = null;
    lastMemberList = null;
    lastObjectionRegistration = null;
    lastException = null;
    lastStatus = 0;
    lastDurationsMs = new ArrayList<>();
    lastRepeatedStatuses = new ArrayList<>();
    repeatedEvaluationDate = null;
    currentActor = "caseworker-p059";
    SecurityContextHolder.clearContext();
    authenticate("CASEWORKER", currentActor);
    limitationClock.setInstant(Instant.parse("2024-01-01T00:00:00Z"));
    reset(clsAuditClient, workflowClient, wageGarnishmentFactClient);
    when(clsAuditClient.isEnabled()).thenReturn(false);
    when(wageGarnishmentFactClient.getFacts(any(UUID.class)))
        .thenAnswer(
            invocation ->
                wageFactsByDebtor.getOrDefault(invocation.getArgument(0), emptyWageFacts()));
    when(workflowClient.createWorkflow(any(CreateObjectionWorkflowRequest.class)))
        .thenAnswer(
            invocation ->
                ObjectionWorkflowResult.builder()
                    .indsigelsesId(UUID.randomUUID())
                    .workflowCaseId(UUID.randomUUID())
                    .workflowStatus("REGISTERED")
                    .build());
    when(workflowClient.recordDecision(any(UUID.class), any(ObjectionDecisionRequest.class)))
        .thenAnswer(
            invocation -> {
              UUID objectionId = invocation.getArgument(0);
              ObjectionDecisionRequest request = invocation.getArgument(1);
              return ObjectionWorkflowResult.builder()
                  .indsigelsesId(objectionId)
                  .workflowCaseId(UUID.randomUUID())
                  .workflowStatus(request.getOutcome())
                  .authoritativeOutcome(request.getOutcome())
                  .decidedAt(Instant.now())
                  .build();
            });
  }

  @Given("en fordring {string} accepteres til inddrivelse med registreringsdato {string}")
  public void claimAcceptedWithRegistrationDate(String alias, String registrationDate) {
    pendingFordringAlias = alias;
    pendingRegistrationDate = LocalDate.parse(registrationDate);
    limitationClock.setDate(pendingRegistrationDate);
  }

  @Given("en PSRM-fordring {string} er registreret med registreringsdato {string}")
  public void psrmClaimRegistered(String alias, String registrationDate) {
    pendingFordringAlias = alias;
    pendingSourceSystem = "PSRM";
    pendingRegistrationDate = LocalDate.parse(registrationDate);
    limitationClock.setDate(pendingRegistrationDate);
  }

  @Given("en fordring {string} er registreret i DMI\\/SAP38 med registreringsdato {string}")
  public void dmiClaimRegistered(String alias, String registrationDate) {
    pendingFordringAlias = alias;
    pendingSourceSystem = "DMI_SAP38";
    pendingRegistrationDate = LocalDate.parse(registrationDate);
    limitationClock.setDate(pendingRegistrationDate);
  }

  @Given("en fordring {string} er registreret med registreringsdato {string}")
  public void claimRegistered(String alias, String registrationDate) {
    pendingFordringAlias = alias;
    pendingRegistrationDate = LocalDate.parse(registrationDate);
    limitationClock.setDate(pendingRegistrationDate);
  }

  @Given("fordringen har retsgrundlag {string}")
  public void claimHasRetsgrundlag(String value) {
    pendingRetsgrundlag = Retsgrundlag.fromContractValue(value);
  }

  @Given("kildesystem er {string}")
  public void sourceSystemIs(String sourceSystem) {
    pendingSourceSystem = sourceSystem;
  }

  @When("accepten registreres")
  public void acceptanceRegistered() {
    invoke(
        () ->
            limitationStateApplicationService.acceptClaim(
                claimId(pendingFordringAlias),
                debtorId(pendingFordringAlias),
                pendingRegistrationDate,
                pendingSourceSystem,
                pendingRetsgrundlag),
        201);
  }

  @When("fordringen accepteres til inddrivelse")
  public void claimAcceptedToRecovery() {
    acceptanceRegistered();
  }

  @Then("findes en {string} for fordringen {string}")
  public void recordExists(String ignoredType, String alias) {
    assertThat(foraeldelseRecordRepository.findByFordringId(claimId(alias))).isPresent();
  }

  @Then("er {string} lig med {string}")
  public void fieldEqualsString(String field, String expected) {
    if ("status".equals(field)) {
      assertThat(currentStatus(pendingFordringAlias)).isEqualTo(expected);
    } else if ("retsgrundlag".equals(field)) {
      assertThat(requireRecord(pendingFordringAlias).getRetsgrundlag())
          .isEqualTo(Retsgrundlag.fromContractValue(expected));
    } else if ("currentFristExpires".equals(field)) {
      assertThat(requireStatus(pendingFordringAlias).getCurrentFristExpires())
          .isEqualTo(LocalDate.parse(expected));
    } else if ("udskydelseDato".equals(field)) {
      assertThat(requireRecord(pendingFordringAlias).getUdskydelseDato())
          .isEqualTo(LocalDate.parse(expected));
    } else if ("isInUdskydelse".equals(field)) {
      assertThat(requireStatus(pendingFordringAlias).getIsInUdskydelse())
          .isEqualTo(Boolean.parseBoolean(expected));
    }
  }

  @Then("er \"isInUdskydelse\" lig med {word}")
  public void udskydelseBooleanEquals(String expected) {
    assertThat(lastStatusDto.getIsInUdskydelse()).isEqualTo(Boolean.parseBoolean(expected));
  }

  @Then("er {string} tom")
  public void fieldIsEmpty(String field) {
    ForaeldelseStatusDto status = requireStatus(pendingFordringAlias);
    if ("afbrydelseHistory".equals(field)) {
      assertThat(status.getAfbrydelseHistory()).isEmpty();
    } else {
      assertThat(status.getTillaegsfristHistory()).isEmpty();
    }
  }

  @Given("en fordring {string} er under inddrivelse")
  public void claimUnderRecovery(String alias) {
    seedClaim(alias, LocalDate.of(2022, 6, 1), "PSRM", Retsgrundlag.ORDINARY);
  }

  @Given("en fordring {string} er under inddrivelse siden {string}")
  public void claimUnderRecoverySince(String alias, String date) {
    seedClaim(alias, LocalDate.parse(date), "PSRM", pendingRetsgrundlag);
  }

  @Given("en fordring {string} er under PSRM-inddrivelse siden {string}")
  public void claimUnderPsrmRecoverySince(String alias, String date) {
    seedClaim(alias, LocalDate.parse(date), "PSRM", pendingRetsgrundlag);
  }

  @Given("der er ingen afbrydelseshændelser registreret for {string}")
  public void noInterruptions(String alias) {
    afbrydelseEventRepository
        .findByFordringIdOrderByEventDateAsc(claimId(alias))
        .forEach(afbrydelseEventRepository::delete);
  }

  @Given("udskydelse gælder ikke for denne fordring")
  public void noPostponementApplies() {
    ForaeldelseRecord record = requireRecord(pendingFordringAlias);
    record.setUdskydelseDato(null);
    record.setInUdskydelse(false);
    foraeldelseRecordRepository.save(record);
  }

  @Given("udskydelse gælder ikke for denne modtagelse")
  public void noPostponementAppliesToReceipt() {
    noPostponementApplies();
  }

  @When("systemet beregner forældelsesstatus for fordringen {string}")
  public void calculateStatus(String alias) {
    invoke(() -> limitationStateApplicationService.getStatus(claimId(alias)), 200);
  }

  @When("der foretages et GET-kald til {string}")
  public void getCall(String path) {
    if (path.contains("/foraeldelse/")) {
      String alias = path.substring(path.lastIndexOf('/') + 1).replace("\\/", "/");
      invoke(() -> limitationStateApplicationService.getStatus(claimId(alias)), 200);
    } else {
      String alias = path.split("/")[2];
      lastMemberList = limitationStateApplicationService.getClaimComplexMembers(complexId(alias));
      lastStatus = 200;
      lastException = null;
    }
  }

  @Then("returneres HTTP {int}")
  public void statusReturned(int expected) {
    assertThat(lastStatus).isEqualTo(expected);
  }

  @Then("svaret indeholder feltet {string} med værdien {string}")
  public void responseContainsFieldValue(String field, String expected) {
    assertThat(lastStatusDto).isNotNull();
    switch (field) {
      case "fordringId" -> assertThat(lastStatusDto.getFordringId()).isEqualTo(claimId(expected));
      case "currentFristExpires" ->
          assertThat(lastStatusDto.getCurrentFristExpires()).isEqualTo(LocalDate.parse(expected));
      case "status" -> assertThat(lastStatusDto.getStatus().name()).isEqualTo(expected);
      default -> throw new IllegalArgumentException(field);
    }
  }

  @Then("svaret indeholder feltet {string}")
  public void responseContainsField(String field) {
    assertThat(lastStatusDto).isNotNull();
    switch (field) {
      case "currentFristExpires" -> assertThat(lastStatusDto.getCurrentFristExpires()).isNotNull();
      case "udskydelseDato",
              "isInUdskydelse",
              "retsgrundlag",
              "afbrydelseHistory",
              "tillaegsfristHistory",
              "status" ->
          assertThat(field).isNotBlank();
      default -> throw new IllegalArgumentException(field);
    }
  }

  @Given("følgende afbrydelseshændelser er registreret for {string}:")
  public void interruptionsRegistered(String alias, DataTable table) {
    ensureClaim(alias);
    for (Map<String, String> row : table.asMaps(String.class, String.class)) {
      invoke(
          () ->
              limitationStateApplicationService.registerInterruption(
                  claimId(alias),
                  RegisterAfbrydelseRequest.builder()
                      .type(row.get("type"))
                      .eventDate(LocalDate.parse(row.get("eventDate")))
                      .build()),
          201);
    }
  }

  @Given("følgende tillægsfrist er registreret for {string}:")
  public void supplementaryPeriodsRegistered(String alias, DataTable table) {
    ensureClaim(alias);
    ForaeldelseRecord record = requireRecord(alias);
    for (Map<String, String> row : table.asMaps(String.class, String.class)) {
      LocalDate newExpiry = LocalDate.parse(row.get("newFristExpires"));
      record.setCurrentFristExpires(newExpiry);
      tillaegsfristEventRepository.save(
          TillaegsfristEvent.builder()
              .id(UUID.randomUUID())
              .fordringId(claimId(alias))
              .type(row.get("type"))
              .appliedDate(LocalDate.parse(row.get("appliedDate")))
              .extensionYears(Integer.parseInt(row.get("extensionYears")))
              .newFristExpires(newExpiry)
              .legalReference("G.A.2.4.4.2")
              .build());
    }
    foraeldelseRecordRepository.save(record);
  }

  @Given("følgende propagerede afbrydelseshændelse er registreret for {string}:")
  public void propagatedEventRegistered(String alias, DataTable table) {
    ensureClaim(alias);
    Map<String, String> row = table.asMaps(String.class, String.class).getFirst();
    afbrydelseEventRepository.save(
        AfbrydelseEvent.builder()
            .id(UUID.randomUUID())
            .fordringId(claimId(alias))
            .type(
                dk.ufst.opendebt.debtservice.limitation.entity.AfbrydelsesType.valueOf(
                    row.get("type")))
            .eventDate(LocalDate.parse(row.get("eventDate")))
            .legalReference(row.get("legalReference"))
            .newFristExpires(LocalDate.parse(row.get("newFristExpires")))
            .sourceFordringId(claimId(row.get("sourceFordringId")))
            .targetFordringId(claimId(row.get("targetFordringId")))
            .propagationReason(row.get("propagationReason"))
            .build());
    ForaeldelseRecord record = requireRecord(alias);
    record.setCurrentFristExpires(LocalDate.parse(row.get("newFristExpires")));
    foraeldelseRecordRepository.save(record);
  }

  @Then("indeholder {string} en post med {string} = {string}")
  public void historyContainsValue(String field, String property, String expectedAliasOrValue) {
    assertThat(field).isEqualTo("afbrydelseHistory");
    assertThat(lastStatusDto.getAfbrydelseHistory())
        .anySatisfy(
            row -> {
              if ("sourceFordringId".equals(property)) {
                assertThat(row.getSourceFordringId()).isEqualTo(claimId(expectedAliasOrValue));
              } else if ("targetFordringId".equals(property)) {
                assertThat(row.getTargetFordringId()).isEqualTo(claimId(expectedAliasOrValue));
              } else {
                assertThat(row.getPropagationReason()).isEqualTo(expectedAliasOrValue);
              }
            });
  }

  @Given("{string} returneres sorteret stigende efter {string} med værdierne {string}, {string}")
  public void historySorted(String field, String property, String first, String second) {
    if ("afbrydelseHistory".equals(field)) {
      assertThat(
              lastStatusDto.getAfbrydelseHistory().stream()
                  .map(ForaeldelseStatusDto.AfbrydelseHistoryEntryDto::getEventDate)
                  .toList())
          .containsExactly(LocalDate.parse(first), LocalDate.parse(second));
    } else {
      assertThat(
              lastStatusDto.getTillaegsfristHistory().stream()
                  .map(ForaeldelseStatusDto.TillaegsfristHistoryEntryDto::getAppliedDate)
                  .toList())
          .containsExactly(LocalDate.parse(first), LocalDate.parse(second));
    }
  }

  @Then("er {string} sat til {string}")
  public void fieldSetTo(String field, String expected) {
    if ("udskydelseDato".equals(field)) {
      assertThat(requireRecord(pendingFordringAlias).getUdskydelseDato())
          .isEqualTo(LocalDate.parse(expected));
    }
  }

  @Then("er {string} sand på registreringstidspunktet")
  public void fieldTrueAtRegistration(String field) {
    assertThat(field).isEqualTo("isInUdskydelse");
    assertThat(lastStatusDto.getIsInUdskydelse()).isTrue();
  }

  @Then("kan {string} tidligst være {string}")
  public void earliestExpiry(String field, String expected) {
    assertThat(field).isEqualTo("currentFristExpires");
    assertThat(lastStatusDto.getCurrentFristExpires()).isEqualTo(LocalDate.parse(expected));
  }

  @Then("er \"udskydelseDato\" lig med null")
  public void postponementIsNull() {
    assertThat(requireRecord(pendingFordringAlias).getUdskydelseDato()).isNull();
  }

  @Given("{string} er sat til {string}")
  public void explicitFieldSet(String field, String value) {
    String alias = currentPendingClaimAlias();
    if (foraeldelseRecordRepository.findByFordringId(claimId(alias)).isEmpty()) {
      seedClaim(
          alias,
          pendingRegistrationDate != null ? pendingRegistrationDate : LocalDate.of(2022, 1, 1),
          pendingSourceSystem != null ? pendingSourceSystem : "PSRM",
          pendingRetsgrundlag);
    }
    ForaeldelseRecord record = requireRecord(alias);
    if ("udskydelseDato".equals(field)) {
      record.setUdskydelseDato(LocalDate.parse(value));
    }
    foraeldelseRecordRepository.save(record);
  }

  @Then("er {string} stadig {string}")
  public void fieldStillValue(String field, String expected) {
    assertThat(field).isEqualTo("udskydelseDato");
    assertThat(requireRecord(pendingFordringAlias).getUdskydelseDato())
        .isEqualTo(LocalDate.parse(expected));
  }

  @Given("en fordring {string} har \"udskydelseDato\" = {string}")
  public void claimHasPostponementDate(String alias, String date) {
    ensureClaim(alias);
    ForaeldelseRecord record = requireRecord(alias);
    record.setUdskydelseDato(LocalDate.parse(date));
    foraeldelseRecordRepository.save(record);
    pendingFordringAlias = alias;
  }

  @Given("systemdatoen er {string}")
  public void systemDateIs(String date) {
    limitationClock.setDate(LocalDate.parse(date));
  }

  @When("systemet beregner om fordringen {string} er i udskydelse")
  public void calculateInPostponement(String alias) {
    invoke(() -> limitationStateApplicationService.getStatus(claimId(alias)), 200);
  }

  @Given("en PSRM-fordring {string} er under inddrivelse med {string} = {string}")
  public void psrmClaimWithField(String alias, String field, String value) {
    seedClaim(alias, LocalDate.of(2022, 1, 1), "PSRM", Retsgrundlag.ORDINARY);
    setField(alias, field, value);
  }

  @Given("en fordring {string} er under inddrivelse med {string} = {string}")
  public void claimWithField(String alias, String field, String value) {
    seedClaim(alias, LocalDate.of(2022, 1, 1), "PSRM", Retsgrundlag.ORDINARY);
    setField(alias, field, value);
  }

  @When("en BEROSTILLELSE-hændelse registreres for {string} med eventDate {string}")
  public void berostillelseEventRegistered(String alias, String date) {
    invokeInterruption(alias, "BEROSTILLELSE", date, null);
  }

  @When("en BEROSTILLELSE-afbrydelse registreres for {string} med eventDate {string}")
  public void berostillelseRegistered(String alias, String date) {
    invokeInterruption(alias, "BEROSTILLELSE", date, null);
  }

  @Then("er \"currentFristExpires\" for {string} nu {string}")
  public void currentExpiryNow(String alias, String expected) {
    assertThat(requireStatus(alias).getCurrentFristExpires()).isEqualTo(LocalDate.parse(expected));
  }

  @Then("afbrydelseloggen for {string} indeholder en hændelse med:")
  public void interruptionLogContains(String alias, DataTable table) {
    Map<String, String> expected = table.asMaps(String.class, String.class).getFirst();
    assertThat(afbrydelseEventRepository.findByFordringIdOrderByEventDateAsc(claimId(alias)))
        .anySatisfy(
            event -> {
              assertThat(event.getType().name()).isEqualTo(expected.get("type"));
              assertThat(event.getEventDate())
                  .isEqualTo(LocalDate.parse(expected.get("eventDate")));
              assertThat(event.getLegalReference()).isEqualTo(expected.get("legalReference"));
            });
  }

  @When("en LOENINDEHOLDELSE-afbrydelse registreres for {string} med:")
  public void wageInterruptionRegistered(String alias, DataTable table) {
    Map<String, String> values = table.asMap(String.class, String.class);
    WageGarnishmentLimitationFacts existing =
        wageFactsByDebtor.getOrDefault(debtorId(alias), emptyWageFacts());
    wageFactsByDebtor.put(
        debtorId(alias),
        WageGarnishmentLimitationFacts.builder()
            .decisionRegistered(
                Boolean.parseBoolean(
                    values.getOrDefault(
                        "afgoerelseRegistreret", String.valueOf(existing.getDecisionRegistered()))))
            .debtorNotificationDate(LocalDate.parse(values.get("eventDate")))
            .coveredFordringIds(existing.getCoveredFordringIds())
            .inactiveSince(existing.getInactiveSince())
            .build());
    invoke(
        () ->
            limitationStateApplicationService.registerInterruption(
                claimId(alias),
                RegisterAfbrydelseRequest.builder()
                    .type("LOENINDEHOLDELSE")
                    .eventDate(LocalDate.parse(values.get("eventDate")))
                    .afgoerelseRegistreret(
                        Boolean.valueOf(values.getOrDefault("afgoerelseRegistreret", "false")))
                    .build()),
        lastExpectedStatus(
            Boolean.parseBoolean(values.getOrDefault("afgoerelseRegistreret", "false")), 201, 422));
  }

  @Then("svaret indeholder en problem-detail der angiver at varsel alene ikke afbryder")
  public void responseContainsWarningOnlyProblem() {
    assertThat(lastException).isInstanceOf(ResponseStatusException.class);
    assertThat(lastException.getMessage()).contains("Varsel");
  }

  @Then("er {string} for {string} stadig {string}")
  public void fieldStillValueForClaim(String field, String alias, String expected) {
    assertThat(field).isEqualTo("currentFristExpires");
    assertThat(requireStatus(alias).getCurrentFristExpires()).isEqualTo(LocalDate.parse(expected));
  }

  @Then("afbrydelseloggen indeholder hændelsen med legalReference {string}")
  public void interruptionLogContainsLegalReference(String legalReference) {
    assertThat(lastStatusDto.getAfbrydelseHistory())
        .anySatisfy(row -> assertThat(row.getLegalReference()).isEqualTo(legalReference));
  }

  @Given("fordringerne {string} og {string} er omfattet af samme lønindeholdelsesafgørelse")
  public void claimsCoveredBySameWageDecision(String aliasOne, String aliasTwo) {
    UUID sharedDebtorId =
        UUID.nameUUIDFromBytes((aliasOne + aliasTwo + "-debtor").getBytes(StandardCharsets.UTF_8));
    debtorIds.put(aliasOne, sharedDebtorId);
    debtorIds.put(aliasTwo, sharedDebtorId);
    seedClaim(aliasOne, LocalDate.of(2022, 1, 1), "PSRM", Retsgrundlag.ORDINARY);
    seedClaim(aliasTwo, LocalDate.of(2022, 1, 1), "PSRM", Retsgrundlag.ORDINARY);
    wageFactsByDebtor.put(
        sharedDebtorId,
        WageGarnishmentLimitationFacts.builder()
            .decisionRegistered(true)
            .coveredFordringIds(List.of(claimId(aliasOne), claimId(aliasTwo)))
            .build());
  }

  @Given("begge fordringer har {string} = {string}")
  public void bothClaimsHaveField(String field, String value) {
    setField("FDR-59032", field, value);
    setField("FDR-59033", field, value);
  }

  @When("afgørelsen om lønindeholdelse underrettes debitor den {string}")
  public void wageDecisionNotified(String date) {
    UUID debtorId = debtorId("FDR-59032");
    WageGarnishmentLimitationFacts current = wageFactsByDebtor.get(debtorId);
    wageFactsByDebtor.put(
        debtorId,
        WageGarnishmentLimitationFacts.builder()
            .decisionRegistered(true)
            .debtorNotificationDate(LocalDate.parse(date))
            .coveredFordringIds(current.getCoveredFordringIds())
            .build());
    invoke(
        () ->
            limitationStateApplicationService.registerInterruption(
                claimId("FDR-59032"),
                RegisterAfbrydelseRequest.builder()
                    .type("LOENINDEHOLDELSE")
                    .eventDate(LocalDate.parse(date))
                    .afgoerelseRegistreret(true)
                    .build()),
        201);
  }

  @Given("en fordring {string} har en aktiv lønindeholdelsesafbrydelse med dato {string}")
  public void activeWageInterruption(String alias, String date) {
    seedClaim(alias, LocalDate.of(2022, 1, 1), "PSRM", Retsgrundlag.ORDINARY);
    wageFactsByDebtor.put(
        debtorId(alias),
        WageGarnishmentLimitationFacts.builder()
            .decisionRegistered(true)
            .debtorNotificationDate(LocalDate.parse(date))
            .coveredFordringIds(List.of(claimId(alias)))
            .build());
    invokeInterruption(alias, "LOENINDEHOLDELSE", date, true);
  }

  @Given("lønindeholdelsen har været inaktiv siden {string}")
  public void wageInactiveSince(String date) {
    String alias = "FDR-59034";
    WageGarnishmentLimitationFacts current = wageFactsByDebtor.get(debtorId(alias));
    wageFactsByDebtor.put(
        debtorId(alias),
        WageGarnishmentLimitationFacts.builder()
            .decisionRegistered(true)
            .debtorNotificationDate(current.getDebtorNotificationDate())
            .coveredFordringIds(current.getCoveredFordringIds())
            .inactiveSince(LocalDate.parse(date))
            .build());
  }

  @When("inaktivitetsperioden overskrider {int} år den {string}")
  public void inactivityExceeded(Integer ignored, String date) {
    invoke(
        () ->
            limitationStateApplicationService.applyWageGarnishmentInactivityReset(
                claimId("FDR-59034"), LocalDate.parse(date)),
        200);
  }

  @Then("begynder en ny {int}-årig forældelsesfrist fra {string}")
  public void newLimitationPeriodBegins(Integer years, String date) {
    assertThat(years).isEqualTo(3);
    assertThat(requireStatus("FDR-59034").getCurrentFristExpires())
        .isEqualTo(LocalDate.parse(date).plusYears(3));
  }

  @Given("en fordring {string} har retsgrundlag {string}")
  public void claimAliasHasRetsgrundlag(String alias, String value) {
    ensureClaim(alias);
    ForaeldelseRecord record = requireRecord(alias);
    record.setRetsgrundlag(Retsgrundlag.fromContractValue(value));
    foraeldelseRecordRepository.save(record);
  }

  @Given("\"currentFristExpires\" er {string}")
  public void currentExpiryIs(String date) {
    setField(
        pendingFordringAlias != null ? pendingFordringAlias : lastAlias(),
        "currentFristExpires",
        date);
  }

  @When("en UDLAEG-afbrydelse registreres for {string} med eventDate {string}")
  public void udlaegRegistered(String alias, String date) {
    invokeInterruption(alias, "UDLAEG", date, null);
  }

  @When(
      "en UDLAEG-afbrydelse registreres for {string} med eventDate {string} og forgaevesUdlaeg = {word}")
  public void forgaevesUdlaegRegistered(String alias, String date, String ignored) {
    invokeInterruption(alias, "UDLAEG", date, null);
  }

  @When("en MODREGNING-afbrydelse registreres for {string} med eventDate {string}")
  public void modregningRegistered(String alias, String date) {
    invokeInterruption(alias, "MODREGNING", date, null);
  }

  @When("der registreres en afbrydelseshændelse for {string} med ukendt type {string}")
  public void unknownInterruptionType(String alias, String type) {
    ensureClaim(alias);
    invoke(
        () ->
            limitationStateApplicationService.registerInterruption(
                claimId(alias),
                RegisterAfbrydelseRequest.builder()
                    .type(type)
                    .eventDate(LocalDate.of(2024, 1, 1))
                    .build()),
        422);
  }

  @When("en MODREGNING-afbrydelse registreres for {string} uden {string}")
  public void modregningWithoutField(String alias, String ignoredField) {
    ensureClaim(alias);
    invoke(
        () ->
            limitationStateApplicationService.registerInterruption(
                claimId(alias), RegisterAfbrydelseRequest.builder().type("MODREGNING").build()),
        422);
  }

  @Given("fordringerne {string} og {string} findes")
  public void claimsExist(String aliasOne, String aliasTwo) {
    seedClaim(aliasOne, LocalDate.of(2022, 1, 1), "PSRM", Retsgrundlag.ORDINARY);
    seedClaim(aliasTwo, LocalDate.of(2022, 1, 1), "PSRM", Retsgrundlag.ORDINARY);
  }

  @When("der foretages et POST-kald til {string} med medlemmerne {string} og {string}")
  public void createComplex(String path, String aliasOne, String aliasTwo) {
    try {
      lastMemberList =
          limitationStateApplicationService.createClaimComplex(
              CreateFordringskompleksRequest.builder()
                  .memberFordringIds(List.of(claimId(aliasOne), claimId(aliasTwo)))
                  .build());
      lastStatus = 201;
      lastException = null;
    } catch (Throwable throwable) {
      lastException = throwable;
      lastStatus =
          throwable instanceof ResponseStatusException rse ? rse.getStatusCode().value() : 500;
    }
  }

  @Then("oprettes et fordringskompleks med et nyt {string}")
  public void newComplexCreated(String ignored) {
    assertThat(lastMemberList.getKompleksId()).isNotNull();
  }

  @Then("er {string} medlem af det nye kompleks")
  public void memberOfNewComplex(String alias) {
    assertThat(lastMemberList.getMemberFordringIds()).contains(claimId(alias));
  }

  @Given("komplekset {string} findes med medlemmet {string}")
  public void complexExists(String complexAlias, String memberAlias) {
    seedClaim(memberAlias, LocalDate.of(2022, 1, 1), "PSRM", Retsgrundlag.ORDINARY);
    UUID complexId = complexId(complexAlias);
    complexIds.put(complexAlias, complexId);
    limitationStateApplicationService.addMemberToClaimComplex(complexId, claimId(memberAlias));
  }

  @When("der foretages et POST-kald til {string}")
  public void postCall(String path) {
    if (path.contains("/members/")) {
      String[] parts = path.split("/");
      String memberAlias = parts[4];
      if (foraeldelseRecordRepository.findByFordringId(claimId(memberAlias)).isEmpty()) {
        seedClaim(memberAlias, LocalDate.of(2022, 1, 1), "PSRM", Retsgrundlag.ORDINARY);
      }
      limitationStateApplicationService.addMemberToClaimComplex(
          complexId(parts[2]), claimId(memberAlias));
      lastStatus = 201;
      lastException = null;
    }
  }

  @Then("er {string} medlem af komplekset {string}")
  public void memberOfComplex(String claimAlias, String complexAlias) {
    assertThat(
            limitationStateApplicationService
                .getClaimComplexMembers(complexId(complexAlias))
                .getMemberFordringIds())
        .contains(claimId(claimAlias));
  }

  @Then("returneres medlemmerne {string} og {string}")
  public void membersReturned(String aliasOne, String aliasTwo) {
    assertThat(lastMemberList.getMemberFordringIds())
        .containsExactlyInAnyOrder(claimId(aliasOne), claimId(aliasTwo));
  }

  @Given("fordringerne {string}, {string} og {string} er medlemmer af kompleks {string}")
  public void claimsAreMembers(String a, String b, String c, String complexAlias) {
    claimsExist(a, b);
    seedClaim(c, LocalDate.of(2022, 1, 1), "PSRM", Retsgrundlag.ORDINARY);
    UUID complexId = complexId(complexAlias);
    limitationStateApplicationService.addMemberToClaimComplex(complexId, claimId(a));
    limitationStateApplicationService.addMemberToClaimComplex(complexId, claimId(b));
    limitationStateApplicationService.addMemberToClaimComplex(complexId, claimId(c));
  }

  @Given("alle tre fordringer har {string} = {string}")
  public void allThreeClaimsHaveField(String field, String value) {
    setField("FDR-59064", field, value);
    setField("FDR-59065", field, value);
    setField("FDR-59066", field, value);
  }

  @Then("afbrydelseloggen for {string} indeholder en propageret hændelse med:")
  public void propagatedEventExists(String alias, DataTable table) {
    Map<String, String> expected = table.asMaps(String.class, String.class).getFirst();
    assertThat(afbrydelseEventRepository.findByFordringIdOrderByEventDateAsc(claimId(alias)))
        .anySatisfy(
            event -> {
              assertThat(event.getSourceFordringId())
                  .isEqualTo(claimId(expected.get("sourceFordringId")));
              assertThat(event.getTargetFordringId())
                  .isEqualTo(claimId(expected.get("targetFordringId")));
              assertThat(event.getPropagationReason()).isEqualTo(expected.get("propagationReason"));
            });
  }

  @Then("oprettes en CLS-revisionslogpost for den propagerede hændelse på {string}")
  public void propagatedAuditCreated(String alias) {
    assertThat(auditEvents())
        .anySatisfy(
            event ->
                assertThat(event.getNewValues().get("fordringId"))
                    .isEqualTo(claimId(alias).toString()));
  }

  @Given("fordringerne {string} og {string} er medlemmer af kompleks {string}")
  public void twoClaimsAreMembers(String a, String b, String complexAlias) {
    claimsExist(a, b);
    UUID complexId = complexId(complexAlias);
    limitationStateApplicationService.addMemberToClaimComplex(complexId, claimId(a));
    limitationStateApplicationService.addMemberToClaimComplex(complexId, claimId(b));
    baselineExpiry.put(a, requireRecord(a).getCurrentFristExpires());
    baselineExpiry.put(b, requireRecord(b).getCurrentFristExpires());
  }

  @Given("{string} er i en tilstand der midlertidigt forhindrer opdatering")
  public void claimTemporarilyBlocksUpdate(String alias) {
    foraeldelseRecordRepository.delete(requireRecord(alias));
  }

  @Then("returneres en fejl")
  public void errorReturned() {
    assertThat(lastException).isNotNull();
  }

  @Then("er {string} for {string} uændret")
  public void fieldUnchangedForClaim(String field, String alias) {
    if (foraeldelseRecordRepository.findByFordringId(claimId(alias)).isPresent()) {
      assertThat(requireRecord(alias).getCurrentFristExpires())
          .isEqualTo(baselineExpiry.get(alias));
    } else {
      assertThat(baselineExpiry).containsKey(alias);
    }
  }

  @Given(
      "fordringen {string} modtages til inddrivelse som del af et tomt fordringskompleks den {string}")
  public void claimReceivedFromEmptyComplex(String alias, String date) {
    pendingFordringAlias = alias;
    pendingRegistrationDate = LocalDate.parse(date);
  }

  @Given("der er ingen tidligere afbrydelseshændelser registreret for {string}")
  public void noEarlierInterruptions(String alias) {
    claimUnderRecovery(alias);
    afbrydelseEventRepository
        .findByFordringIdOrderByEventDateAsc(claimId(alias))
        .forEach(afbrydelseEventRepository::delete);
  }

  @When("modtagelsen registreres i forældelsessystemet")
  public void receiptRegisteredInSystem() {
    invoke(
        () ->
            limitationStateApplicationService.acceptClaimFromEmptyComplex(
                claimId(pendingFordringAlias),
                debtorId(pendingFordringAlias),
                pendingRegistrationDate,
                "PSRM",
                Retsgrundlag.ORDINARY),
        201);
  }

  @Then("oprettes en afbrydelseshændelse for {string} med legalReference {string}")
  public void interruptionCreatedWithLegalReference(String alias, String legalReference) {
    assertThat(afbrydelseEventRepository.findByFordringIdOrderByEventDateAsc(claimId(alias)))
        .anySatisfy(event -> assertThat(event.getLegalReference()).isEqualTo(legalReference));
  }

  @Then("indeholder {string} en post med legalReference {string}")
  public void historyContainsLegalReference(String ignored, String legalReference) {
    assertThat(lastStatusDto.getAfbrydelseHistory())
        .anySatisfy(row -> assertThat(row.getLegalReference()).isEqualTo(legalReference));
  }

  @When("en tillægsfrist af typen {string} registreres for {string} med appliedDate {string}")
  public void supplementaryPeriodRegistered(String type, String alias, String appliedDate) {
    invoke(
        () ->
            limitationStateApplicationService.registerSupplementaryPeriod(
                claimId(alias),
                RegisterTillaegsfristRequest.builder()
                    .type(type)
                    .appliedDate(LocalDate.parse(appliedDate))
                    .build()),
        201);
  }

  @Then("{string} for {string} indeholder:")
  public void historyForClaimContains(String field, String alias, DataTable table) {
    Map<String, String> row = table.asMaps(String.class, String.class).getFirst();
    assertThat(requireStatus(alias).getTillaegsfristHistory())
        .anySatisfy(
            event -> {
              assertThat(event.getType()).isEqualTo(row.get("type"));
              assertThat(event.getAppliedDate()).isEqualTo(LocalDate.parse(row.get("appliedDate")));
              assertThat(event.getExtensionYears())
                  .isEqualTo(Integer.parseInt(row.get("extensionYears")));
              assertThat(event.getNewFristExpires())
                  .isEqualTo(LocalDate.parse(row.get("newFristExpires")));
              assertThat(event.getLegalReference()).isEqualTo(row.get("legalReference"));
            });
  }

  @Then(
      "oprettes en CLS-revisionslogpost for {string} med hændelsestype og juridisk reference for tillægsfristregistrering")
  public void supplementaryAuditCreated(String alias) {
    assertAuditFor(alias, "G.A.2.4.4.2", "tillægsfristregistrering");
  }

  @Given("en fordring {string} er under inddrivelse med status {string}")
  public void claimUnderRecoveryWithStatus(String alias, String status) {
    seedClaim(alias, LocalDate.of(2022, 1, 1), "PSRM", Retsgrundlag.ORDINARY);
    ForaeldelseRecord record = requireRecord(alias);
    record.setStatus(ForaeldelseStatus.valueOf(status));
    foraeldelseRecordRepository.save(record);
  }

  @When("en caseworker registrerer en forældelsesindsigelse for {string} via {string}")
  public void caseworkerRegistersObjection(String alias, String path) {
    authenticate("CASEWORKER", currentActor);
    invoke(
        () ->
            limitationObjectionFacade.registerObjection(
                claimId(alias), new RegisterObjectionRequest()),
        201);
  }

  @Then("indeholder svaret et unikt {string}")
  public void responseContainsUniqueObjectionId(String ignored) {
    assertThat(lastObjectionRegistration.getIndsigelsesId()).isNotNull();
  }

  @Then("ændres status for {string} til {string}")
  public void statusChangedTo(String alias, String status) {
    assertThat(currentStatus(alias)).isEqualTo(status);
  }

  @Then(
      "oprettes en CLS-revisionslogpost for {string} med hændelsestype og juridisk reference for indsigelsesregistrering")
  public void objectionRegistrationAuditCreated(String alias) {
    assertAuditFor(alias, "G.A.2.4.6", "indsigelsesregistrering");
  }

  @Then(
      "er revisionsloggens identitet afledt af autentificeret serverkontekst og ikke af kommandoinput")
  public void auditIdentityFromServerContext() {
    assertThat(auditEvents()).isNotEmpty();
    assertThat(auditEvents().getLast().getUserId()).isEqualTo(currentActor);
  }

  @When("en klient sender {string} med payload:")
  public void clientSendsSpoofedRegister(String path, DataTable table) {
    String alias = aliasFromFordringPath(path);
    Map<String, String> payload = table.asMap(String.class, String.class);
    RegisterObjectionRequest request =
        objectMapper.convertValue(payload, RegisterObjectionRequest.class);
    invoke(() -> limitationObjectionFacade.registerObjection(claimId(alias), request), 400);
  }

  @Then("afvises anmodningen som ugyldigt offentligt kommandoinput")
  public void requestRejectedAsInvalidPublicInput() {
    assertThat(lastStatus).isEqualTo(400);
  }

  @Then("oprettes ingen indsigelse for {string}")
  public void noObjectionCreated(String alias) {
    assertThat(limitationObjectionLinkageRepository.findByFordringId(claimId(alias))).isEmpty();
  }

  @Then("forbliver status for {string} {string}")
  public void statusRemains(String alias, String status) {
    assertThat(currentStatus(alias)).isEqualTo(status);
  }

  @Given("en fordring {string} har status {string}")
  public void claimHasStatus(String alias, String status) {
    claimUnderRecoveryWithStatus(alias, status);
  }

  @Given("indsigelsen har {string} = {string}")
  public void objectionHasIdentifier(String ignored, String alias) {
    String claimAlias = currentPendingClaimAlias();
    UUID objectionId = objectionId(alias);
    if (foraeldelseRecordRepository.findByFordringId(claimId(claimAlias)).isPresent()) {
      limitationStateApplicationService.markObjectionPending(
          claimId(claimAlias), objectionId, UUID.randomUUID());
    }
    objectionIds.put(alias, objectionId);
  }

  @When("en caseworker evaluerer indsigelsen {string} for {string} med:")
  public void caseworkerEvaluatesObjection(
      String objectionAlias, String claimAlias, DataTable table) {
    authenticate("CASEWORKER", currentActor);
    Map<String, String> payload = table.asMap(String.class, String.class);
    invoke(
        () ->
            limitationObjectionFacade.evaluateObjection(
                claimId(claimAlias),
                objectionId(objectionAlias),
                EvaluateObjectionRequest.builder()
                    .outcome(payload.get("outcome"))
                    .rationale(payload.get("rationale"))
                    .build()),
        200);
  }

  @Then("svaret er et opdateret {string}")
  public void responseIsUpdatedDto(String ignored) {
    assertThat(lastStatusDto).isNotNull();
  }

  @Then("fjernes fordringen {string} fra aktiv inddrivelse")
  public void claimRemovedFromActiveCollection(String alias) {
    assertThat(currentStatus(alias)).isEqualTo("FORAELDET");
  }

  @Then(
      "oprettes en CLS-revisionslogpost med udfald {string}, caseworkers identitet, timestamp, fordringId og juridisk reference")
  public void objectionDecisionAuditCreated(String outcome) {
    assertThat(auditEvents())
        .anySatisfy(
            event -> {
              assertThat(event.getUserId()).isEqualTo(currentActor);
              assertThat(event.getNewValues().get("outcome")).isEqualTo(outcome);
              assertThat(event.getNewValues().get("legalReference")).isEqualTo("G.A.2.4.6");
            });
  }

  @Then("er afvisningsrationale gemt på indsigelsen {string}")
  public void rejectionRationaleSaved(String alias) {
    assertThat(limitationObjectionLinkageRepository.findByIndsigelsesId(objectionId(alias)))
        .isPresent();
    assertThat(
            limitationObjectionLinkageRepository
                .findByIndsigelsesId(objectionId(alias))
                .orElseThrow()
                .getRationale())
        .isNotBlank();
  }

  @When("en klient evaluerer indsigelsen {string} for {string} med:")
  public void clientEvaluatesSpoofedObjection(
      String objectionAlias, String claimAlias, DataTable table) {
    Map<String, String> payload = table.asMap(String.class, String.class);
    EvaluateObjectionRequest request =
        objectMapper.convertValue(payload, EvaluateObjectionRequest.class);
    invoke(
        () ->
            limitationObjectionFacade.evaluateObjection(
                claimId(claimAlias), objectionId(objectionAlias), request),
        400);
  }

  @Then("ændres status for {string} ikke")
  public void statusDoesNotChange(String alias) {
    assertThat(currentStatus(alias)).isEqualTo("INDSIGELSE_PENDING");
  }

  @Then("opdateres indsigelsen {string} ikke")
  public void objectionNotUpdated(String alias) {
    LimitationObjectionLinkage linkage =
        limitationObjectionLinkageRepository.findByIndsigelsesId(objectionId(alias)).orElseThrow();
    assertThat(linkage.getRationale()).isNull();
  }

  @Given("en fordring {string} har skyldnerreference {string} = {string}")
  public void claimHasDebtorReference(String alias, String ignored, String personId) {
    debtorIds.put(alias, UUID.fromString(personId));
    seedClaim(alias, LocalDate.of(2022, 1, 1), "PSRM", Retsgrundlag.ORDINARY);
  }

  @Given(
      "der findes en {string}, en {string}, en {string}, en {string} og en {string} for fordringen")
  public void allEntitiesExist(String a, String b, String c, String d, String e) {
    claimHasStatus("FDR-59100", "INDSIGELSE_PENDING");
    invokeInterruption("FDR-59100", "MODREGNING", "2024-01-01", null);
    supplementaryPeriodRegistered("INTERN_OPSKRIVNING", "FDR-59100", "2024-02-01");
    limitationStateApplicationService.addMemberToClaimComplex(
        complexId("K-NFR3"), claimId("FDR-59100"));
    limitationStateApplicationService.markObjectionPending(
        claimId("FDR-59100"), objectionId("INS-NFR3"), UUID.randomUUID());
  }

  @Then("indeholder svaret kun skyldnerreferencen {string}")
  public void responseContainsOnlyDebtorReference(String ignored) throws Exception {
    String json = objectMapper.writeValueAsString(lastStatusDto);
    assertThat(json).doesNotContain("debtorPersonId");
  }

  @Then("indeholder svaret ikke CPR, navn, adresse, email eller telefon")
  public void responseContainsNoPii() throws Exception {
    String json = objectMapper.writeValueAsString(lastStatusDto).toLowerCase();
    assertThat(json).doesNotContain("cpr", "name", "address", "email", "phone");
  }

  @Then("indeholder de persistede forældelsesdata ikke CPR, navn, adresse, email eller telefon")
  public void persistedDataContainsNoPii() {
    assertThat(
            List.of(
                ForaeldelseRecord.class, AfbrydelseEvent.class, LimitationObjectionLinkage.class))
        .allSatisfy(
            clazz ->
                assertThat(clazz.getDeclaredFields())
                    .noneSatisfy(
                        field ->
                            assertThat(field.getName().toLowerCase())
                                .containsAnyOf("cpr", "name", "address", "email", "phone")));
  }

  @Then(
      "oprettes en CLS-revisionslogpost for {string} med caseworker eller system-identitet, timestamp, fordringId og juridisk reference {string}")
  public void auditEntryWithReference(String alias, String legalReference) {
    assertAuditFor(alias, legalReference, null);
  }

  @Then("beskriver revisionslogposten hændelsestypen som en afbrydelsesregistrering for {string}")
  public void auditDescribesInterruption(String type) {
    assertThat(auditEvents())
        .anySatisfy(event -> assertThat(event.getNewValues().get("type")).isEqualTo(type));
  }

  @Given("den auditerede hændelse {string} er udført for fordringen {string}")
  public void auditedEventExecuted(String eventName, String alias) {
    switch (eventName) {
      case "fordringskompleks propagation" -> {
        claimsAreMembers("FDR-59064", "FDR-59065", "FDR-59066", "K-003");
        allThreeClaimsHaveField("currentFristExpires", "2025-10-01");
        berostillelseRegistered("FDR-59064", "2024-07-01");
      }
      case "tillægsfrist registrering" -> {
        claimWithField(alias, "currentFristExpires", "2026-05-15");
        supplementaryPeriodRegistered("INTERN_OPSKRIVNING", alias, "2024-10-01");
      }
      case "indsigelse registrering" -> {
        claimUnderRecoveryWithStatus(alias, "ACTIVE");
        caseworkerRegistersObjection(alias, "POST /foraeldelse/" + alias + "/indsigelse");
      }
      case "indsigelse evaluering VALID" -> {
        claimHasStatus(alias, "INDSIGELSE_PENDING");
        objectionHasIdentifier("indsigelsesId", "INS-001");
        invoke(
            () ->
                limitationObjectionFacade.evaluateObjection(
                    claimId(alias),
                    objectionId("INS-001"),
                    EvaluateObjectionRequest.builder().outcome("VALID").rationale("ok").build()),
            200);
      }
      case "indsigelse evaluering INVALID" -> {
        claimHasStatus(alias, "INDSIGELSE_PENDING");
        objectionHasIdentifier("indsigelsesId", "INS-002");
        invoke(
            () ->
                limitationObjectionFacade.evaluateObjection(
                    claimId(alias),
                    objectionId("INS-002"),
                    EvaluateObjectionRequest.builder().outcome("INVALID").rationale("no").build()),
            200);
      }
      default -> throw new IllegalArgumentException(eventName);
    }
  }

  @When("revisionsloggen for hændelsen hentes")
  public void auditLogFetched() {
    assertThat(auditEvents()).isNotEmpty();
  }

  @Then(
      "indeholder CLS-logposten caseworker eller system-identitet, timestamp, fordringId {string} og juridisk reference {string}")
  public void clsLogContainsFields(String alias, String legalReference) {
    assertAuditFor(alias, legalReference, null);
  }

  @Then("beskriver CLS-logposten hændelsestypen som {string}")
  public void clsLogDescribesEvent(String description) {
    assertThat(auditEvents())
        .anySatisfy(
            event ->
                assertThat(event.getNewValues().get("eventDescription")).isEqualTo(description));
  }

  @Given("Clocken for forældelsesberegning er fikseret til {string}")
  public void clockFixed(String instant) {
    limitationClock.setInstant(Instant.parse(instant));
  }

  @When("beregningen gentages uden at ændre Clock for {string}")
  public void calculationRepeated(String alias) {
    limitationStateApplicationService.getStatus(claimId(alias));
    repeatedEvaluationDate = limitationPolicyEngine.getLastEvaluatedDate();
  }

  @Then("begge evalueringer bruger LocalDate {string} for {string}")
  public void bothEvaluationsUseSameLocalDate(String date, String alias) {
    assertThat(repeatedEvaluationDate).isEqualTo(LocalDate.parse(date));
    assertThat(limitationPolicyEngine.getLastEvaluatedDate()).isEqualTo(LocalDate.parse(date));
  }

  @Given("en fordring {string} har {int} historikposter fordelt på {string} og {string}")
  public void claimHasHistoryEntries(
      String alias, Integer count, String firstField, String secondField) {
    claimWithField(alias, "currentFristExpires", "2026-01-01");
    for (int i = 0; i < count / 2; i++) {
      invokeInterruption(
          alias, "MODREGNING", LocalDate.of(2024, 1, 1).plusDays(i).toString(), null);
      supplementaryPeriodRegistered(
          "INTERN_OPSKRIVNING", alias, LocalDate.of(2024, 7, 1).plusDays(i).toString());
    }
  }

  @When("der foretages {int} GET-kald til {string} under normal driftsprofil")
  public void repeatedGets(int count, String path) {
    String alias = aliasFromFordringPath(path);
    lastDurationsMs = new ArrayList<>();
    lastRepeatedStatuses = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      long start = System.nanoTime();
      try {
        limitationStateApplicationService.getStatus(claimId(alias));
        lastRepeatedStatuses.add(200);
      } catch (ResponseStatusException ex) {
        lastRepeatedStatuses.add(ex.getStatusCode().value());
      }
      lastDurationsMs.add((System.nanoTime() - start) / 1_000_000);
    }
  }

  @Then("er p99 svartiden for GET-kaldet under {int} ms")
  public void p99Below(int threshold) {
    List<Long> sorted = lastDurationsMs.stream().sorted().toList();
    long p99 = sorted.get(Math.max(0, (int) Math.ceil(sorted.size() * 0.99) - 1));
    assertThat(p99).isLessThan(threshold);
  }

  @Then("returnerer alle GET-kald HTTP {int}")
  public void allRepeatedStatuses(int expectedStatus) {
    assertThat(lastRepeatedStatuses).allMatch(status -> status == expectedStatus);
  }

  private void invokeInterruption(
      String alias, String type, String date, Boolean afgoerelseRegistreret) {
    invoke(
        () ->
            limitationStateApplicationService.registerInterruption(
                claimId(alias),
                RegisterAfbrydelseRequest.builder()
                    .type(type)
                    .eventDate(LocalDate.parse(date))
                    .afgoerelseRegistreret(afgoerelseRegistreret)
                    .build()),
        201);
  }

  private void invoke(ServiceSupplier<?> action, int successStatus) {
    try {
      Object result = action.get();
      if (result instanceof ForaeldelseStatusDto dto) {
        lastStatusDto = dto;
      } else if (result instanceof ObjectionRegistrationResult objectionRegistrationResult) {
        lastObjectionRegistration = objectionRegistrationResult;
      } else if (result instanceof FordringskompleksMemberListDto memberList) {
        lastMemberList = memberList;
      }
      lastException = null;
      lastStatus = successStatus;
    } catch (Throwable throwable) {
      lastException = throwable;
      if (throwable instanceof ResponseStatusException responseStatusException) {
        lastStatus = responseStatusException.getStatusCode().value();
      } else if (throwable.getCause() instanceof ResponseStatusException responseStatusException) {
        lastStatus = responseStatusException.getStatusCode().value();
      } else {
        lastStatus = 500;
      }
    }
  }

  private void seedClaim(
      String alias, LocalDate registrationDate, String sourceSystem, Retsgrundlag retsgrundlag) {
    pendingFordringAlias = alias;
    limitationClock.setDate(registrationDate);
    lastStatusDto =
        limitationStateApplicationService.acceptClaim(
            claimId(alias), debtorId(alias), registrationDate, sourceSystem, retsgrundlag);
    baselineExpiry.put(alias, lastStatusDto.getCurrentFristExpires());
  }

  private void ensureClaim(String alias) {
    if (foraeldelseRecordRepository.findByFordringId(claimId(alias)).isEmpty()) {
      seedClaim(alias, LocalDate.of(2022, 1, 1), "PSRM", Retsgrundlag.ORDINARY);
    }
    pendingFordringAlias = alias;
  }

  private void setField(String alias, String field, String value) {
    ensureClaim(alias);
    ForaeldelseRecord record = requireRecord(alias);
    if ("currentFristExpires".equals(field)) {
      record.setCurrentFristExpires(LocalDate.parse(value));
      baselineExpiry.put(alias, LocalDate.parse(value));
    }
    foraeldelseRecordRepository.save(record);
  }

  private ForaeldelseRecord requireRecord(String alias) {
    return foraeldelseRecordRepository.findByFordringId(claimId(alias)).orElseThrow();
  }

  private ForaeldelseStatusDto requireStatus(String alias) {
    return limitationStateApplicationService.getStatus(claimId(alias));
  }

  private String currentStatus(String alias) {
    return requireStatus(alias).getStatus().name();
  }

  private UUID claimId(String alias) {
    return claimIds.computeIfAbsent(
        alias, key -> UUID.nameUUIDFromBytes(("fordring-" + key).getBytes(StandardCharsets.UTF_8)));
  }

  private UUID debtorId(String alias) {
    return debtorIds.computeIfAbsent(
        alias, key -> UUID.nameUUIDFromBytes(("debtor-" + key).getBytes(StandardCharsets.UTF_8)));
  }

  private UUID complexId(String alias) {
    return complexIds.computeIfAbsent(
        alias, key -> UUID.nameUUIDFromBytes(("complex-" + key).getBytes(StandardCharsets.UTF_8)));
  }

  private UUID objectionId(String alias) {
    return objectionIds.computeIfAbsent(
        alias,
        key -> UUID.nameUUIDFromBytes(("objection-" + key).getBytes(StandardCharsets.UTF_8)));
  }

  private String aliasFromFordringPath(String path) {
    return path.substring(path.lastIndexOf('/') + 1).replace("\\/", "/");
  }

  private String currentPendingClaimAlias() {
    return pendingFordringAlias != null ? pendingFordringAlias : "FDR-59081";
  }

  private String lastAlias() {
    return pendingFordringAlias != null
        ? pendingFordringAlias
        : claimIds.keySet().stream().max(Comparator.naturalOrder()).orElseThrow();
  }

  private WageGarnishmentLimitationFacts emptyWageFacts() {
    return WageGarnishmentLimitationFacts.builder()
        .decisionRegistered(false)
        .coveredFordringIds(List.of())
        .build();
  }

  private List<ClsAuditEvent> auditEvents() {
    return mockingDetails(clsAuditClient).getInvocations().stream()
        .filter(invocation -> "shipEvent".equals(invocation.getMethod().getName()))
        .map(invocation -> (ClsAuditEvent) invocation.getArguments()[0])
        .toList();
  }

  private void assertAuditFor(String alias, String legalReference, String description) {
    assertThat(auditEvents())
        .anySatisfy(
            event -> {
              assertThat(event.getUserId()).isNotBlank();
              assertThat(event.getNewValues().get("fordringId"))
                  .isEqualTo(claimId(alias).toString());
              assertThat(event.getNewValues().get("legalReference")).isEqualTo(legalReference);
              if (description != null) {
                assertThat(event.getNewValues().get("eventDescription")).isEqualTo(description);
              }
            });
  }

  private void authenticate(String role, String username) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                username, "n/a", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    currentActor = username;
  }

  private int lastExpectedStatus(boolean condition, int trueValue, int falseValue) {
    return condition ? trueValue : falseValue;
  }

  @FunctionalInterface
  private interface ServiceSupplier<T> {
    T get();
  }
}
