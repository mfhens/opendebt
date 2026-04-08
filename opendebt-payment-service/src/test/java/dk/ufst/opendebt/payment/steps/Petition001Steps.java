package dk.ufst.opendebt.payment.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.common.dto.DebtDto.DebtStatus;
import dk.ufst.opendebt.payment.bookkeeping.BookkeepingService;
import dk.ufst.opendebt.payment.client.DebtServiceClient;
import dk.ufst.opendebt.payment.dto.IncomingPaymentDto;
import dk.ufst.opendebt.payment.dto.OverpaymentOutcome;
import dk.ufst.opendebt.payment.dto.PaymentMatchResult;
import dk.ufst.opendebt.payment.repository.PaymentRepository;
import dk.ufst.opendebt.payment.service.PaymentMatchingService;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;

@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
public class Petition001Steps {

  @Autowired private PaymentMatchingService paymentMatchingService;
  @Autowired private PaymentRepository paymentRepository;

  @MockitoBean private DebtServiceClient debtServiceClient;
  @MockitoBean private BookkeepingService bookkeepingService;

  @MockitoBean
  private dk.ufst.opendebt.payment.service.OverpaymentRulesService overpaymentRulesService;

  private final Map<String, DebtDto> debtsByAlias = new HashMap<>();
  private final Map<String, BigDecimal> originalBalancesByAlias = new HashMap<>();
  private final Map<UUID, OverpaymentOutcome> overpaymentOutcomes = new HashMap<>();

  private IncomingPaymentDto incomingPayment;
  private PaymentMatchResult result;

  @Before
  public void setUpScenario() {
    paymentRepository.deleteAll();
    debtsByAlias.clear();
    originalBalancesByAlias.clear();
    overpaymentOutcomes.clear();
    incomingPayment = null;
    result = null;

    reset(debtServiceClient, bookkeepingService, overpaymentRulesService);

    when(debtServiceClient.findByOcrLine(anyString()))
        .thenAnswer(invocation -> findDebtsByOcr(invocation.getArgument(0, String.class)));
    when(debtServiceClient.writeDown(any(UUID.class), any(BigDecimal.class)))
        .thenAnswer(
            invocation ->
                writeDownDebt(
                    invocation.getArgument(0, UUID.class),
                    invocation.getArgument(1, BigDecimal.class)));
    when(overpaymentRulesService.resolveOutcome(any(UUID.class)))
        .thenAnswer(
            invocation -> {
              UUID debtId = invocation.getArgument(0, UUID.class);
              return overpaymentOutcomes.get(debtId);
            });
  }

  @Given("incoming payments are received by OpenDebt from SKB as CREMUL payment entries")
  public void incoming_payments_are_received_by_open_debt_from_skb_as_cremul_payment_entries() {
    // No-op: This step describes a precondition context for the scenario.
    // Actual CREMUL parsing is tested in SkbEdifactServiceImplTest.
  }

  @Given("an issued påkrav contains Betalingsservice OCR-linje {string}")
  public void an_issued_påkrav_contains_betalingsservice_ocr_linje(String ocrLine) {
    assertThat(ocrLine).isNotBlank();
  }

  @Given("OCR-linje {string} uniquely identifies debt {string}")
  public void ocr_linje_uniquely_identifies_debt(String ocrLine, String debtAlias) {
    debtsByAlias.put(
        debtAlias,
        DebtDto.builder()
            .id(UUID.randomUUID())
            .debtorId(UUID.randomUUID().toString())
            .creditorId(UUID.randomUUID().toString())
            .debtTypeCode("600")
            .ocrLine(ocrLine)
            .build());
  }

  @Given("debt {string} has an outstanding balance of {int} DKK")
  public void debt_has_an_outstanding_balance_of_dkk(String debtAlias, Integer amount) {
    DebtDto debt = debtsByAlias.get(debtAlias);
    assertThat(debt).isNotNull();
    BigDecimal balance = BigDecimal.valueOf(amount.longValue());
    debt.setOutstandingBalance(balance);
    debt.setStatus(DebtStatus.ACTIVE);
    originalBalancesByAlias.put(debtAlias, balance);
  }

  @Given("an incoming payment references OCR-linje {string} with amount {int} DKK")
  public void an_incoming_payment_references_ocr_linje_with_amount_dkk(
      String ocrLine, Integer amount) {
    incomingPayment = incomingPayment(ocrLine, amount);
  }

  @When("the payment is processed")
  public void the_payment_is_processed() {
    result = paymentMatchingService.processIncomingPayment(incomingPayment);
  }

  @Then("the payment is auto-matched to debt {string}")
  public void the_payment_is_auto_matched_to_debt(String debtAlias) {
    assertThat(result.isAutoMatched()).isTrue();
    assertThat(result.getMatchedDebtId()).isEqualTo(debtsByAlias.get(debtAlias).getId());
  }

  @Then("the payment is not routed to manual matching on the case")
  public void the_payment_is_not_routed_to_manual_matching_on_the_case() {
    assertThat(result.isRoutedToManualMatching()).isFalse();
  }

  @Then("debt {string} is written down by {int} DKK")
  public void debt_is_written_down_by_dkk(String debtAlias, Integer amount) {
    UUID debtId = debtsByAlias.get(debtAlias).getId();
    assertThat(result.getWriteDownAmount())
        .isEqualByComparingTo(BigDecimal.valueOf(amount.longValue()));
    verify(debtServiceClient).writeDown(debtId, BigDecimal.valueOf(amount.longValue()));
  }

  @Then("debt {string} has {int} DKK remaining")
  public void debt_has_dkk_remaining(String debtAlias, Integer amount) {
    assertThat(debtsByAlias.get(debtAlias).getOutstandingBalance())
        .isEqualByComparingTo(BigDecimal.valueOf(amount.longValue()));
  }

  @Given("an incoming payment does not contain an OCR-linje that uniquely identifies a debt")
  public void an_incoming_payment_does_not_contain_an_ocr_linje_that_uniquely_identifies_a_debt() {
    incomingPayment = incomingPayment("OCR-NO-MATCH", 500);
  }

  @Then("the payment is not auto-matched")
  public void the_payment_is_not_auto_matched() {
    assertThat(result.isAutoMatched()).isFalse();
  }

  @Then("the payment is routed to manual matching on the case")
  public void the_payment_is_routed_to_manual_matching_on_the_case() {
    assertThat(result.isRoutedToManualMatching()).isTrue();
    assertThat(paymentRepository.findAll()).hasSize(1);
  }

  @Given(
      "rules for sagstype and frivillig indbetaling resolve the excess amount outcome to {string}")
  public void rules_for_sagstype_and_frivillig_indbetaling_resolve_the_excess_amount_outcome_to(
      String outcome) {
    debtsByAlias
        .values()
        .forEach(debt -> overpaymentOutcomes.put(debt.getId(), parseOutcome(outcome)));
  }

  @Then(
      "debt {string} is written down by the actual paid amount according to the applicable payment rules")
  public void
      debt_is_written_down_by_the_actual_paid_amount_according_to_the_applicable_payment_rules(
          String debtAlias) {
    BigDecimal expected = originalBalancesByAlias.get(debtAlias).min(incomingPayment.getAmount());
    assertThat(result.getWriteDownAmount()).isEqualByComparingTo(expected);
    verify(debtServiceClient).writeDown(debtsByAlias.get(debtAlias).getId(), expected);
  }

  @Then("the excess amount outcome is {string}")
  public void the_excess_amount_outcome_is(String outcome) {
    assertThat(result.getExcessOutcome()).isEqualTo(parseOutcome(outcome));
  }

  private List<DebtDto> findDebtsByOcr(String ocrLine) {
    return debtsByAlias.values().stream()
        .filter(debt -> ocrLine.equals(debt.getOcrLine()))
        .map(this::copyOf)
        .toList();
  }

  private DebtDto writeDownDebt(UUID debtId, BigDecimal amount) {
    DebtDto debt =
        debtsByAlias.values().stream()
            .filter(candidate -> debtId.equals(candidate.getId()))
            .findFirst()
            .orElseThrow();
    BigDecimal newBalance = debt.getOutstandingBalance().subtract(amount);
    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
      newBalance = BigDecimal.ZERO;
    }
    debt.setOutstandingBalance(newBalance);
    debt.setStatus(
        newBalance.compareTo(BigDecimal.ZERO) == 0 ? DebtStatus.PAID : DebtStatus.PARTIALLY_PAID);
    return copyOf(debt);
  }

  private DebtDto copyOf(DebtDto debt) {
    return DebtDto.builder()
        .id(debt.getId())
        .debtorId(debt.getDebtorId())
        .creditorId(debt.getCreditorId())
        .debtTypeCode(debt.getDebtTypeCode())
        .ocrLine(debt.getOcrLine())
        .outstandingBalance(debt.getOutstandingBalance())
        .status(debt.getStatus())
        .build();
  }

  private IncomingPaymentDto incomingPayment(String ocrLine, int amount) {
    return IncomingPaymentDto.builder()
        .ocrLine(ocrLine)
        .amount(BigDecimal.valueOf(amount))
        .valueDate(LocalDate.of(2025, 12, 1))
        .cremulReference("CREMUL-001")
        .build();
  }

  private OverpaymentOutcome parseOutcome(String outcome) {
    if ("payout".equalsIgnoreCase(outcome)) {
      return OverpaymentOutcome.PAYOUT;
    }
    if ("use to cover other debt posts".equalsIgnoreCase(outcome)) {
      return OverpaymentOutcome.COVER_OTHER_DEBTS;
    }
    throw new IllegalArgumentException("Unknown outcome: " + outcome);
  }
}
