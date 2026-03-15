package dk.ufst.opendebt.rules.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class DebtReadinessResultTest {

  @Test
  void addError_setsReadyToFalseAndAddsError() {
    DebtReadinessResult result =
        DebtReadinessResult.builder().debtId(UUID.randomUUID()).ready(true).build();

    result.addError("Missing documentation");

    assertThat(result.isReady()).isFalse();
    assertThat(result.getValidationErrors()).containsExactly("Missing documentation");
  }

  @Test
  void addError_withNullList_initializesAndAdds() {
    DebtReadinessResult result =
        DebtReadinessResult.builder().debtId(UUID.randomUUID()).ready(true).build();
    result.setValidationErrors(null);

    result.addError("Some error");

    assertThat(result.getValidationErrors()).containsExactly("Some error");
    assertThat(result.isReady()).isFalse();
  }

  @Test
  void addWarning_addsWarningToList() {
    DebtReadinessResult result = DebtReadinessResult.builder().debtId(UUID.randomUUID()).build();

    result.addWarning("Low amount");

    assertThat(result.getWarnings()).containsExactly("Low amount");
  }

  @Test
  void addWarning_withNullList_initializesAndAdds() {
    DebtReadinessResult result = DebtReadinessResult.builder().debtId(UUID.randomUUID()).build();
    result.setWarnings(null);

    result.addWarning("Some warning");

    assertThat(result.getWarnings()).containsExactly("Some warning");
  }

  @Test
  void requireManualReview_setsFieldsCorrectly() {
    DebtReadinessResult result =
        DebtReadinessResult.builder()
            .debtId(UUID.randomUUID())
            .status(DebtReadinessResult.ReadinessStatus.READY_FOR_COLLECTION)
            .build();

    result.requireManualReview("Complex case");

    assertThat(result.isRequiresManualReview()).isTrue();
    assertThat(result.getManualReviewReason()).isEqualTo("Complex case");
    assertThat(result.getStatus()).isEqualTo(DebtReadinessResult.ReadinessStatus.PENDING_REVIEW);
  }
}
