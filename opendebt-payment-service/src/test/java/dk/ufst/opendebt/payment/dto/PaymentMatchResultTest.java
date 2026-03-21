package dk.ufst.opendebt.payment.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PaymentMatchResultTest {

  @Test
  void builderAndToStringExposeValues() {
    UUID paymentId = UUID.randomUUID();
    UUID debtId = UUID.randomUUID();
    PaymentMatchResult result =
        PaymentMatchResult.builder()
            .paymentId(paymentId)
            .autoMatched(true)
            .matchedDebtId(debtId)
            .writeDownAmount(new BigDecimal("100"))
            .excessAmount(new BigDecimal("25"))
            .excessOutcome(OverpaymentOutcome.PAYOUT)
            .routedToManualMatching(false)
            .build();

    assertThat(result.getPaymentId()).isEqualTo(paymentId);
    assertThat(result.isAutoMatched()).isTrue();
    assertThat(result.getMatchedDebtId()).isEqualTo(debtId);
    assertThat(result.getWriteDownAmount()).isEqualByComparingTo("100");
    assertThat(result.getExcessAmount()).isEqualByComparingTo("25");
    assertThat(result.getExcessOutcome()).isEqualTo(OverpaymentOutcome.PAYOUT);
    assertThat(result.isRoutedToManualMatching()).isFalse();
    assertThat(result.toString()).contains(paymentId.toString());
  }

  @Test
  void constructorsAndSettersWork() {
    PaymentMatchResult result = new PaymentMatchResult();
    UUID paymentId = UUID.randomUUID();
    UUID debtId = UUID.randomUUID();
    result.setPaymentId(paymentId);
    result.setAutoMatched(false);
    result.setMatchedDebtId(debtId);
    result.setWriteDownAmount(new BigDecimal("50"));
    result.setExcessAmount(BigDecimal.ZERO);
    result.setExcessOutcome(OverpaymentOutcome.COVER_OTHER_DEBTS);
    result.setRoutedToManualMatching(true);

    PaymentMatchResult copied =
        new PaymentMatchResult(
            result.getPaymentId(),
            result.isAutoMatched(),
            result.getMatchedDebtId(),
            result.getWriteDownAmount(),
            result.getExcessAmount(),
            result.getExcessOutcome(),
            result.isRoutedToManualMatching(),
            false,
            null);

    assertThat(copied.getPaymentId()).isEqualTo(paymentId);
    assertThat(copied.getMatchedDebtId()).isEqualTo(debtId);
    assertThat(copied.isRoutedToManualMatching()).isTrue();
  }

  @Test
  void equalsAndHashCodeConsiderAllFields() {
    UUID paymentId = UUID.randomUUID();
    UUID debtId = UUID.randomUUID();
    PaymentMatchResult base =
        new PaymentMatchResult(
            paymentId,
            true,
            debtId,
            new BigDecimal("100"),
            new BigDecimal("25"),
            OverpaymentOutcome.PAYOUT,
            false,
            false,
            null);
    PaymentMatchResult same =
        new PaymentMatchResult(
            paymentId,
            true,
            debtId,
            new BigDecimal("100"),
            new BigDecimal("25"),
            OverpaymentOutcome.PAYOUT,
            false,
            false,
            null);

    assertThat(base).isEqualTo(base);
    assertThat(base).isEqualTo(same);
    assertThat(base.hashCode()).isEqualTo(same.hashCode());
    assertThat(base).isNotEqualTo(null);
    assertThat(base).isNotEqualTo("other");
    assertThat(base)
        .isNotEqualTo(
            new PaymentMatchResult(
                UUID.randomUUID(),
                true,
                debtId,
                new BigDecimal("100"),
                new BigDecimal("25"),
                OverpaymentOutcome.PAYOUT,
                false,
                false,
                null));
    assertThat(base)
        .isNotEqualTo(
            new PaymentMatchResult(
                paymentId,
                false,
                debtId,
                new BigDecimal("100"),
                new BigDecimal("25"),
                OverpaymentOutcome.PAYOUT,
                false,
                false,
                null));
    assertThat(base)
        .isNotEqualTo(
            new PaymentMatchResult(
                paymentId,
                true,
                UUID.randomUUID(),
                new BigDecimal("100"),
                new BigDecimal("25"),
                OverpaymentOutcome.PAYOUT,
                false,
                false,
                null));
    assertThat(base)
        .isNotEqualTo(
            new PaymentMatchResult(
                paymentId,
                true,
                debtId,
                new BigDecimal("101"),
                new BigDecimal("25"),
                OverpaymentOutcome.PAYOUT,
                false,
                false,
                null));
    assertThat(base)
        .isNotEqualTo(
            new PaymentMatchResult(
                paymentId,
                true,
                debtId,
                new BigDecimal("100"),
                new BigDecimal("30"),
                OverpaymentOutcome.PAYOUT,
                false,
                false,
                null));
    assertThat(base)
        .isNotEqualTo(
            new PaymentMatchResult(
                paymentId,
                true,
                debtId,
                new BigDecimal("100"),
                new BigDecimal("25"),
                OverpaymentOutcome.COVER_OTHER_DEBTS,
                false,
                false,
                null));
    assertThat(base)
        .isNotEqualTo(
            new PaymentMatchResult(
                paymentId,
                true,
                debtId,
                new BigDecimal("100"),
                new BigDecimal("25"),
                OverpaymentOutcome.PAYOUT,
                true,
                false,
                null));
  }

  @Test
  void equalsAndHashCodeHandleNullFieldsAndSubclass() {
    PaymentMatchResult base =
        new PaymentMatchResult(null, false, null, null, null, null, false, false, null);
    PaymentMatchResult same =
        new PaymentMatchResult(null, false, null, null, null, null, false, false, null);
    PaymentMatchResult withManualRoute =
        new PaymentMatchResult(null, false, null, null, null, null, true, false, null);
    SpecialPaymentMatchResult subclass = new SpecialPaymentMatchResult();

    assertThat(base).isEqualTo(same);
    assertThat(base.hashCode()).isEqualTo(same.hashCode());
    assertThat(base).isNotEqualTo(withManualRoute);
    assertThat(base).isEqualTo(subclass);
    assertThat(subclass).isEqualTo(base);
  }

  private static final class SpecialPaymentMatchResult extends PaymentMatchResult {}
}
