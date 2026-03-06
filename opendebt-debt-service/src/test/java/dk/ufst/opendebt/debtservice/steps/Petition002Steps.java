package dk.ufst.opendebt.debtservice.steps;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;

@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
public class Petition002Steps {

  @Given("fordringshaver {string} authenticates to the API with a valid OCES3 certificate")
  public void fordringshaver_authenticates_to_the_api_with_a_valid_oces3_certificate(
      String string) {
    // Write code here that turns the phrase above into concrete actions
  }

  @Given("fordringshaver {string} submits a new fordring for debtor {string}")
  public void fordringshaver_submits_a_new_fordring_for_debtor(String string, String string2) {
    // Write code here that turns the phrase above into concrete actions
  }

  @Given("the rules evaluate the fordring as inddrivelsesparat")
  public void the_rules_evaluate_the_fordring_as_inddrivelsesparat() {
    // Write code here that turns the phrase above into concrete actions
  }

  @When("OpenDebt processes the submission")
  public void open_debt_processes_the_submission() {
    // Write code here that turns the phrase above into concrete actions
  }

  @Then("a new debt post is created for debtor {string}")
  public void a_new_debt_post_is_created_for_debtor(String string) {
    // Write code here that turns the phrase above into concrete actions
  }

  @Then("bookkeeping is updated for the new debt post")
  public void bookkeeping_is_updated_for_the_new_debt_post() {
    // Write code here that turns the phrase above into concrete actions
  }

  @Given("a fordringshaver submits a new fordring to the API without a valid OCES3 certificate")
  public void
      a_fordringshaver_submits_a_new_fordring_to_the_api_without_a_valid_oces3_certificate() {
    // Write code here that turns the phrase above into concrete actions
  }

  @When("OpenDebt receives the submission")
  public void open_debt_receives_the_submission() {
    // Write code here that turns the phrase above into concrete actions
  }

  @Then("the submission is rejected")
  public void the_submission_is_rejected() {
    // Write code here that turns the phrase above into concrete actions
  }

  @Then("no debt post is created")
  public void no_debt_post_is_created() {
    // Write code here that turns the phrase above into concrete actions
  }

  @Given("user {string} is logged into the fordringshaverportal with MitID Erhverv")
  public void user_is_logged_into_the_fordringshaverportal_with_mit_id_erhverv(String string) {
    // Write code here that turns the phrase above into concrete actions
  }

  @Given("user {string} is linked to fordringshaver {string}")
  public void user_is_linked_to_fordringshaver(String string, String string2) {
    // Write code here that turns the phrase above into concrete actions
  }

  @Given("user {string} submits a new fordring for fordringshaver {string}")
  public void user_submits_a_new_fordring_for_fordringshaver(String string, String string2) {
    // Write code here that turns the phrase above into concrete actions
  }

  @Then("a new debt post is created for the submitted debtor")
  public void a_new_debt_post_is_created_for_the_submitted_debtor() {
    // Write code here that turns the phrase above into concrete actions
  }

  @Given("a user is not logged into the fordringshaverportal with MitID Erhverv")
  public void a_user_is_not_logged_into_the_fordringshaverportal_with_mit_id_erhverv() {
    // Write code here that turns the phrase above into concrete actions
  }

  @When("the user attempts to create a new fordring in the portal")
  public void the_user_attempts_to_create_a_new_fordring_in_the_portal() {
    // Write code here that turns the phrase above into concrete actions
  }

  @Then("the user is not allowed to create the fordring")
  public void the_user_is_not_allowed_to_create_the_fordring() {
    // Write code here that turns the phrase above into concrete actions
  }

  @Given("the API for fordringshaver {string} submits a new fordring using valid authentication")
  public void the_api_for_fordringshaver_submits_a_new_fordring_using_valid_authentication(
      String string) {
    // Write code here that turns the phrase above into concrete actions
  }

  @Given("the rules evaluate the fordring as not inddrivelsesparat with reason {string}")
  public void the_rules_evaluate_the_fordring_as_not_inddrivelsesparat_with_reason(String string) {
    // Write code here that turns the phrase above into concrete actions
  }

  @Then("the fordringshaver receives an error message that includes {string}")
  public void the_fordringshaver_receives_an_error_message_that_includes(String string) {
    // Write code here that turns the phrase above into concrete actions
  }

  @Then("bookkeeping is not updated for debt creation")
  public void bookkeeping_is_not_updated_for_debt_creation() {
    // Write code here that turns the phrase above into concrete actions
  }

  @Given("the portal user for creditor {string} submits a new fordring using valid authentication")
  public void the_portal_user_for_creditor_submits_a_new_fordring_using_valid_authentication(
      String string) {
    // Write code here that turns the phrase above into concrete actions
  }
}
