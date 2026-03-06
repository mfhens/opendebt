package dk.ufst.opendebt.gateway.skb.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class DebitAdviceTest {

  @Test
  void builderAndAccessorsWork() {
    DebitAdvice advice =
        DebitAdvice.builder()
            .messageReference("MSG-1")
            .accountNumber("12345678")
            .amount(new BigDecimal("100.50"))
            .currency("DKK")
            .valueDate(LocalDate.of(2026, 3, 6))
            .creditorReference("CRED-1")
            .paymentReference("PAY-1")
            .remittanceInfo("info")
            .build();

    assertThat(advice.getMessageReference()).isEqualTo("MSG-1");
    assertThat(advice.getAccountNumber()).isEqualTo("12345678");
    assertThat(advice.getAmount()).isEqualByComparingTo("100.50");
    assertThat(advice.getCurrency()).isEqualTo("DKK");
    assertThat(advice.getValueDate()).isEqualTo(LocalDate.of(2026, 3, 6));
    assertThat(advice.getCreditorReference()).isEqualTo("CRED-1");
    assertThat(advice.getPaymentReference()).isEqualTo("PAY-1");
    assertThat(advice.getRemittanceInfo()).isEqualTo("info");
  }

  @Test
  void constructorsAndSettersWork() {
    DebitAdvice advice = new DebitAdvice();
    advice.setMessageReference("MSG-2");
    advice.setAccountNumber("87654321");
    advice.setAmount(new BigDecimal("200"));
    advice.setCurrency("EUR");
    advice.setValueDate(LocalDate.of(2026, 4, 1));
    advice.setCreditorReference("CRED-2");
    advice.setPaymentReference("PAY-2");
    advice.setRemittanceInfo("other");

    DebitAdvice copied =
        new DebitAdvice(
            advice.getMessageReference(),
            advice.getAccountNumber(),
            advice.getAmount(),
            advice.getCurrency(),
            advice.getValueDate(),
            advice.getCreditorReference(),
            advice.getPaymentReference(),
            advice.getRemittanceInfo());

    assertThat(copied.getMessageReference()).isEqualTo("MSG-2");
    assertThat(copied.getCurrency()).isEqualTo("EUR");
    assertThat(copied.getRemittanceInfo()).isEqualTo("other");
  }
}
