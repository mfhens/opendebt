package dk.ufst.opendebt.payment.steps;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;

@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
public class Petition001Steps {

  @Given("incoming payments are received by OpenDebt from SKB as CREMUL payment entries")
  public void incoming_payments_are_received_by_open_debt_from_skb_as_cremul_payment_entries() {
    // Write code here that turns the phrase above into concrete actions
  }

  @Given("an issued påkrav contains Betalingsservice OCR-linje {string}")
  public void an_issued_påkrav_contains_betalingsservice_ocr_linje(String string) {
    // Write code here that turns the phrase above into concrete actions
  }

  @Given("OCR-linje {string} uniquely identifies debt {string}")
  public void ocr_linje_uniquely_identifies_debt(String string, String string2) {
    // Write code here that turns the phrase above into concrete actions
  }

  @Given("debt {string} has an outstanding balance of {int} DKK")
  public void debt_has_an_outstanding_balance_of_dkk(String string, Integer int1) {
    // Write code here that turns the phrase above into concrete actions
  }

  @Given("an incoming payment references OCR-linje {string} with amount {int} DKK")
  public void an_incoming_payment_references_ocr_linje_with_amount_dkk(
      String string, Integer int1) {
    // Write code here that turns the phrase above into concrete actions
  }

  @When("the payment is processed")
  public void the_payment_is_processed() {
    // Write code here that turns the phrase above into concrete actions
  }

  @Then("the payment is auto-matched to debt {string}")
  public void the_payment_is_auto_matched_to_debt(String string) {
    // Write code here that turns the phrase above into concrete actions
  }

  @Then("the payment is not routed to manual matching on the case")
  public void the_payment_is_not_routed_to_manual_matching_on_the_case() {
    // Write code here that turns the phrase above into concrete actions
  }

  @Then("debt {string} is written down by {int} DKK")
  public void debt_is_written_down_by_dkk(String string, Integer int1) {
    // Write code here that turns the phrase above into concrete actions
  }

  @Then("debt {string} has {int} DKK remaining")
  public void debt_has_dkk_remaining(String string, Integer int1) {
    // Write code here that turns the phrase above into concrete actions
  }

  @Given("an incoming payment does not contain an OCR-linje that uniquely identifies a debt")
  public void an_incoming_payment_does_not_contain_an_ocr_linje_that_uniquely_identifies_a_debt() {
    // Write code here that turns the phrase above into concrete actions
  }

  @Then("the payment is not auto-matched")
  public void the_payment_is_not_auto_matched() {
    // Write code here that turns the phrase above into concrete actions
  }

  @Then("the payment is routed to manual matching on the case")
  public void the_payment_is_routed_to_manual_matching_on_the_case() {
    // Write code here that turns the phrase above into concrete actions
  }

  @Given(
      "rules for sagstype and frivillig indbetaling resolve the excess amount outcome to {string}")
  public void rules_for_sagstype_and_frivillig_indbetaling_resolve_the_excess_amount_outcome_to(
      String string) {
    // Write code here that turns the phrase above into concrete actions
  }

  @Then(
      "debt {string} is written down by the actual paid amount according to the applicable payment rules")
  public void
      debt_is_written_down_by_the_actual_paid_amount_according_to_the_applicable_payment_rules(
          String string) {
    // Write code here that turns the phrase above into concrete actions
  }

  @Then("the excess amount outcome is {string}")
  public void the_excess_amount_outcome_is(String string) {
    // Write code here that turns the phrase above into concrete actions
  }
}
