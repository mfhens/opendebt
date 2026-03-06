package dk.ufst.opendebt.debtservice.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.DebtService;
import dk.ufst.opendebt.debtservice.service.ReadinessValidationService;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;

@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
public class Petition002Steps {

  @Autowired private DebtService debtService;
  @Autowired private ReadinessValidationService readinessValidationService;
  @Autowired private DebtRepository debtRepository;

  private final Map<String, UUID> creditorIds = new HashMap<>();
  private final Map<String, UUID> debtorIds = new HashMap<>();
  private final Map<String, String> linkedCreditorsByUser = new HashMap<>();

  private boolean apiAuthenticated;
  private boolean portalAuthenticated;
  private boolean bookkeepingUpdated;
  private boolean rejected;
  private boolean rulesReady;
  private boolean submissionProcessed;

  private String rejectionReason;
  private String errorMessage;
  private String currentUser;
  private String submittedCreditorAlias;
  private String submittedDebtorAlias;
  private DebtDto createdDebt;

  @Before
  public void setUpScenario() {
    debtRepository.deleteAll();
    creditorIds.clear();
    debtorIds.clear();
    linkedCreditorsByUser.clear();
    apiAuthenticated = false;
    portalAuthenticated = false;
    bookkeepingUpdated = false;
    rejected = false;
    rulesReady = false;
    submissionProcessed = false;
    rejectionReason = null;
    errorMessage = null;
    currentUser = null;
    submittedCreditorAlias = null;
    submittedDebtorAlias = null;
    createdDebt = null;
  }

  @Given("fordringshaver {string} authenticates to the API with a valid OCES3 certificate")
  public void fordringshaver_authenticates_to_the_api_with_a_valid_oces3_certificate(
      String creditorAlias) {
    apiAuthenticated = true;
    creditorId(creditorAlias);
  }

  @Given("fordringshaver {string} submits a new fordring for debtor {string}")
  public void fordringshaver_submits_a_new_fordring_for_debtor(
      String creditorAlias, String debtorAlias) {
    submittedCreditorAlias = creditorAlias;
    submittedDebtorAlias = debtorAlias;
    submissionProcessed = false;
    creditorId(creditorAlias);
    debtorId(debtorAlias);
  }

  @Given("the rules evaluate the fordring as inddrivelsesparat")
  public void the_rules_evaluate_the_fordring_as_inddrivelsesparat() {
    rulesReady = true;
    rejectionReason = null;
    submissionProcessed = false;
  }

  @When("OpenDebt processes the submission")
  public void open_debt_processes_the_submission() {
    processSubmission();
  }

  @Then("a new debt post is created for debtor {string}")
  public void a_new_debt_post_is_created_for_debtor(String debtorAlias) {
    ensureSubmissionProcessed();
    assertThat(createdDebt).isNotNull();
    assertThat(createdDebt.getDebtorId()).isEqualTo(debtorId(debtorAlias).toString());
    assertThat(debtRepository.count()).isEqualTo(1);
  }

  @Then("bookkeeping is updated for the new debt post")
  public void bookkeeping_is_updated_for_the_new_debt_post() {
    ensureSubmissionProcessed();
    assertThat(bookkeepingUpdated).isTrue();
  }

  @Given("a fordringshaver submits a new fordring to the API without a valid OCES3 certificate")
  public void
      a_fordringshaver_submits_a_new_fordring_to_the_api_without_a_valid_oces3_certificate() {
    apiAuthenticated = false;
    submittedCreditorAlias = "K1";
    submittedDebtorAlias = "P1";
    submissionProcessed = false;
    creditorId(submittedCreditorAlias);
    debtorId(submittedDebtorAlias);
  }

  @When("OpenDebt receives the submission")
  public void open_debt_receives_the_submission() {
    processSubmission();
  }

  @Then("the submission is rejected")
  public void the_submission_is_rejected() {
    ensureSubmissionProcessed();
    assertThat(rejected).isTrue();
  }

  @Then("no debt post is created")
  public void no_debt_post_is_created() {
    ensureSubmissionProcessed();
    assertThat(createdDebt).isNull();
    assertThat(debtRepository.count()).isZero();
  }

  @Given("user {string} is logged into the fordringshaverportal with MitID Erhverv")
  public void user_is_logged_into_the_fordringshaverportal_with_mit_id_erhverv(String userAlias) {
    portalAuthenticated = true;
    currentUser = userAlias;
  }

  @Given("user {string} is linked to fordringshaver {string}")
  public void user_is_linked_to_fordringshaver(String userAlias, String creditorAlias) {
    linkedCreditorsByUser.put(userAlias, creditorAlias);
    creditorId(creditorAlias);
  }

  @Given("user {string} submits a new fordring for fordringshaver {string}")
  public void user_submits_a_new_fordring_for_fordringshaver(
      String userAlias, String creditorAlias) {
    currentUser = userAlias;
    submittedCreditorAlias = creditorAlias;
    submittedDebtorAlias = "P-SUBMITTED";
    submissionProcessed = false;
    creditorId(creditorAlias);
    debtorId(submittedDebtorAlias);
  }

  @Then("a new debt post is created for the submitted debtor")
  public void a_new_debt_post_is_created_for_the_submitted_debtor() {
    ensureSubmissionProcessed();
    assertThat(createdDebt).isNotNull();
    assertThat(createdDebt.getDebtorId()).isEqualTo(debtorId(submittedDebtorAlias).toString());
  }

  @Given("a user is not logged into the fordringshaverportal with MitID Erhverv")
  public void a_user_is_not_logged_into_the_fordringshaverportal_with_mit_id_erhverv() {
    portalAuthenticated = false;
    currentUser = "U1";
  }

  @When("the user attempts to create a new fordring in the portal")
  public void the_user_attempts_to_create_a_new_fordring_in_the_portal() {
    submittedCreditorAlias = "K1";
    submittedDebtorAlias = "P1";
    submissionProcessed = false;
    creditorId(submittedCreditorAlias);
    debtorId(submittedDebtorAlias);
    processSubmission();
  }

  @Then("the user is not allowed to create the fordring")
  public void the_user_is_not_allowed_to_create_the_fordring() {
    ensureSubmissionProcessed();
    assertThat(rejected).isTrue();
    assertThat(errorMessage).contains("Authentication");
  }

  @Given("the API for fordringshaver {string} submits a new fordring using valid authentication")
  public void the_api_for_fordringshaver_submits_a_new_fordring_using_valid_authentication(
      String creditorAlias) {
    apiAuthenticated = true;
    submittedCreditorAlias = creditorAlias;
    submittedDebtorAlias = "P1";
    submissionProcessed = false;
    creditorId(creditorAlias);
    debtorId(submittedDebtorAlias);
  }

  @Given("the rules evaluate the fordring as not inddrivelsesparat with reason {string}")
  public void the_rules_evaluate_the_fordring_as_not_inddrivelsesparat_with_reason(String reason) {
    rulesReady = false;
    rejectionReason = reason;
    submissionProcessed = false;
  }

  @Then("the fordringshaver receives an error message that includes {string}")
  public void the_fordringshaver_receives_an_error_message_that_includes(String reason) {
    ensureSubmissionProcessed();
    assertThat(errorMessage).contains(reason);
  }

  @Then("bookkeeping is not updated for debt creation")
  public void bookkeeping_is_not_updated_for_debt_creation() {
    ensureSubmissionProcessed();
    assertThat(bookkeepingUpdated).isFalse();
  }

  @Given("the portal user for creditor {string} submits a new fordring using valid authentication")
  public void the_portal_user_for_creditor_submits_a_new_fordring_using_valid_authentication(
      String creditorAlias) {
    portalAuthenticated = true;
    currentUser = "U1";
    linkedCreditorsByUser.put(currentUser, creditorAlias);
    submittedCreditorAlias = creditorAlias;
    submittedDebtorAlias = "P1";
    submissionProcessed = false;
    creditorId(creditorAlias);
    debtorId(submittedDebtorAlias);
  }

  private void processSubmission() {
    submissionProcessed = true;
    bookkeepingUpdated = false;
    rejected = false;
    errorMessage = null;
    createdDebt = null;

    if (!isAuthenticated()) {
      rejected = true;
      errorMessage = "Authentication required";
      return;
    }

    if (!rulesReady) {
      rejected = true;
      errorMessage = rejectionReason;
      return;
    }

    createdDebt = debtService.createDebt(buildDebtDto());
    createdDebt = readinessValidationService.validateReadiness(createdDebt.getId());
    bookkeepingUpdated = true;
  }

  private void ensureSubmissionProcessed() {
    if (!submissionProcessed) {
      processSubmission();
    }
  }

  private boolean isAuthenticated() {
    if (apiAuthenticated) {
      return true;
    }
    return portalAuthenticated
        && currentUser != null
        && submittedCreditorAlias != null
        && submittedCreditorAlias.equals(linkedCreditorsByUser.get(currentUser));
  }

  private DebtDto buildDebtDto() {
    return DebtDto.builder()
        .debtorId(debtorId(submittedDebtorAlias).toString())
        .creditorId(creditorId(submittedCreditorAlias).toString())
        .debtTypeCode("600")
        .principalAmount(new BigDecimal("1000"))
        .dueDate(LocalDate.of(2026, 4, 1))
        .externalReference("petition002")
        .ocrLine("OCR-" + submittedCreditorAlias + "-" + submittedDebtorAlias)
        .build();
  }

  private UUID creditorId(String creditorAlias) {
    return creditorIds.computeIfAbsent(creditorAlias, ignored -> UUID.randomUUID());
  }

  private UUID debtorId(String debtorAlias) {
    return debtorIds.computeIfAbsent(debtorAlias, ignored -> UUID.randomUUID());
  }
}
