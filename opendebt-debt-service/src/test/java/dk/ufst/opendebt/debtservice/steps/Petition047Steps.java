package dk.ufst.opendebt.debtservice.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import dk.ufst.opendebt.debtservice.dto.config.ConfigCreationResult;
import dk.ufst.opendebt.debtservice.dto.config.ConfigEntryDto;
import dk.ufst.opendebt.debtservice.dto.config.CreateConfigRequest;
import dk.ufst.opendebt.debtservice.entity.BusinessConfigEntity;
import dk.ufst.opendebt.debtservice.repository.BusinessConfigAuditRepository;
import dk.ufst.opendebt.debtservice.repository.BusinessConfigRepository;
import dk.ufst.opendebt.debtservice.service.BusinessConfigService;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class Petition047Steps {

  @Autowired private BusinessConfigService configService;
  @Autowired private BusinessConfigRepository configRepository;
  @Autowired private BusinessConfigAuditRepository auditRepository;

  private UUID lastCreatedId;
  private ConfigCreationResult lastCreationResult;
  private Map<String, List<ConfigEntryDto>> lastListResult;
  private List<ConfigEntryDto> lastHistoryResult;
  private BusinessConfigService.ConfigValidationException lastValidationException;
  private BigDecimal lastEffectiveValue;

  @Before("@petition047")
  public void setUp() {
    auditRepository.deleteAll();
    configRepository.deleteAll();
    configService.clearCache();
    lastCreatedId = null;
    lastCreationResult = null;
    lastListResult = null;
    lastHistoryResult = null;
    lastValidationException = null;
    lastEffectiveValue = null;
  }

  @Given("the business config table is empty")
  public void theBusinessConfigTableIsEmpty() {
    assertThat(configRepository.count()).isZero();
  }

  @When("I create a config entry with key {string} value {string} valid from tomorrow")
  public void iCreateConfigEntry(String key, String value) {
    CreateConfigRequest req =
        CreateConfigRequest.builder()
            .configKey(key)
            .configValue(value)
            .valueType("DECIMAL")
            .validFrom(LocalDate.now().plusDays(1))
            .description("BDD test entry")
            .legalBasis("§ 5 renteloven")
            .build();
    lastCreationResult = configService.createEntry(req, "bdd-test", false);
    lastCreatedId = lastCreationResult.getCreated().getId();
  }

  @Then("the entry is stored with review status {string}")
  public void theEntryIsStoredWithReviewStatus(String expectedStatus) {
    assertThat(lastCreatedId).isNotNull();
    BusinessConfigEntity entity = configRepository.findById(lastCreatedId).orElseThrow();
    assertThat(entity.getReviewStatus().name()).isEqualTo(expectedStatus);
  }

  @Given("a PENDING_REVIEW config entry for key {string}")
  public void aPendingReviewConfigEntry(String key) {
    iCreateConfigEntry(key, "0.05");
    assertThat(lastCreatedId).isNotNull();
  }

  @When("I approve the entry")
  public void iApproveTheEntry() {
    configService.approveEntry(lastCreatedId, "approver");
  }

  @Then("the entry has review status {string}")
  public void theEntryHasReviewStatus(String expectedStatus) {
    BusinessConfigEntity entity = configRepository.findById(lastCreatedId).orElseThrow();
    assertThat(entity.getReviewStatus().name()).isEqualTo(expectedStatus);
  }

  @When("I reject the entry")
  public void iRejectTheEntry() {
    configService.rejectEntry(lastCreatedId, "rejector");
  }

  @Then("the entry is deleted from the config table")
  public void theEntryIsDeleted() {
    assertThat(configRepository.findById(lastCreatedId)).isEmpty();
  }

  @Given("config entries exist for keys {string}, {string}")
  public void configEntriesExistForKeys(String key1, String key2) {
    saveApprovedEntry(key1, "0.0575", LocalDate.of(2020, 1, 1), null);
    saveApprovedEntry(key2, "200.00", LocalDate.of(2020, 1, 1), null);
  }

  @When("I list all config entries")
  public void iListAllConfigEntries() {
    lastListResult = configService.listAllGrouped();
  }

  @Then("the result contains groups for {string} and {string}")
  public void theResultContainsGroups(String key1, String key2) {
    assertThat(lastListResult).containsKey(key1);
    assertThat(lastListResult).containsKey(key2);
  }

  @Given("an approved config entry for key {string} valid from {string} to {string}")
  public void approvedConfigEntryForKeyValidFromTo(String key, String from, String to) {
    saveApprovedEntry(key, "0.05", LocalDate.parse(from), LocalDate.parse(to));
  }

  @When("I try to create another entry for key {string} valid from {string}")
  public void iTryToCreateAnotherEntry(String key, String validFrom) {
    CreateConfigRequest req =
        CreateConfigRequest.builder()
            .configKey(key)
            .configValue("0.07")
            .valueType("DECIMAL")
            .validFrom(LocalDate.parse(validFrom))
            .description("Overlap test")
            .legalBasis("§ 5 renteloven")
            .seedMigration(true) // bypass past-date guard to test overlap logic
            .build();
    try {
      configService.createEntry(req, "tester", true);
    } catch (BusinessConfigService.ConfigValidationException e) {
      lastValidationException = e;
    }
  }

  @Then("a validation error is returned indicating period overlap")
  public void aValidationErrorIsReturnedIndicatingPeriodOverlap() {
    assertThat(lastValidationException).isNotNull();
    assertThat(lastValidationException.getMessage()).containsIgnoringCase("overlap");
  }

  @Then("derived entries are created for {string}, {string}, {string}")
  public void derivedEntriesAreCreated(String d1, String d2, String d3) {
    assertThat(lastCreationResult.getDerivedEntries()).isNotNull();
    List<String> derivedKeys =
        lastCreationResult.getDerivedEntries().stream().map(ConfigEntryDto::getConfigKey).toList();
    assertThat(derivedKeys).contains(d1, d2, d3);
  }

  @Given("{int} historical config entries exist for key {string}")
  public void historicalConfigEntriesExist(int count, String key) {
    for (int i = 0; i < count; i++) {
      LocalDate from = LocalDate.of(2018 + i, 1, 1);
      LocalDate to = i < count - 1 ? LocalDate.of(2018 + i, 12, 31) : null;
      saveApprovedEntry(key, "0.0" + (5 + i), from, to);
    }
  }

  @When("I request the history for key {string}")
  public void iRequestTheHistoryForKey(String key) {
    lastHistoryResult =
        configService.getHistory(key).stream()
            .map(
                e ->
                    ConfigEntryDto.builder()
                        .id(e.getId())
                        .configKey(e.getConfigKey())
                        .configValue(e.getConfigValue())
                        .validFrom(e.getValidFrom())
                        .validTo(e.getValidTo())
                        .build())
            .toList();
  }

  @Then("{int} entries are returned in the history")
  public void entriesAreReturnedInHistory(int expected) {
    assertThat(lastHistoryResult).hasSize(expected);
  }

  @Given("a config entry for key {string} with value {string} valid from {string} to {string}")
  public void configEntryWithValueValidFromTo(String key, String value, String from, String to) {
    saveApprovedEntry(key, value, LocalDate.parse(from), LocalDate.parse(to));
  }

  @And("a config entry for key {string} with value {string} valid from {string}")
  public void configEntryWithValueValidFrom(String key, String value, String from) {
    saveApprovedEntry(key, value, LocalDate.parse(from), null);
  }

  @When("I request the effective value for key {string} on date {string}")
  public void iRequestEffectiveValueForKeyOnDate(String key, String date) {
    lastEffectiveValue = configService.getDecimalValue(key, LocalDate.parse(date));
  }

  @Then("the returned value is {string}")
  public void theReturnedValueIs(String expected) {
    assertThat(lastEffectiveValue).isEqualByComparingTo(new BigDecimal(expected));
  }

  // ── Helper ─────────────────────────────────────────────────────────────────

  private void saveApprovedEntry(String key, String value, LocalDate from, LocalDate to) {
    configRepository.save(
        BusinessConfigEntity.builder()
            .configKey(key)
            .configValue(value)
            .valueType("DECIMAL")
            .validFrom(from)
            .validTo(to)
            .description("BDD seed entry")
            .legalBasis("§ 5 renteloven")
            .createdBy("bdd-setup")
            .reviewStatus(BusinessConfigEntity.ReviewStatus.APPROVED)
            .build());
  }
}
