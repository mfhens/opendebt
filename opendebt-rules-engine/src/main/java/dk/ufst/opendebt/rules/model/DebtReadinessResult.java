package dk.ufst.opendebt.rules.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/** Result of debt readiness evaluation. */
@Data
@Builder
public class DebtReadinessResult {

  private UUID debtId;
  private boolean ready;
  private ReadinessStatus status;

  @Builder.Default private List<String> validationErrors = new ArrayList<>();

  @Builder.Default private List<String> warnings = new ArrayList<>();

  private boolean requiresManualReview;
  private String manualReviewReason;

  public enum ReadinessStatus {
    READY_FOR_COLLECTION,
    NOT_READY,
    PENDING_REVIEW,
    BLOCKED
  }

  public void addError(String error) {
    if (validationErrors == null) {
      validationErrors = new ArrayList<>();
    }
    validationErrors.add(error);
    ready = false;
  }

  public void addWarning(String warning) {
    if (warnings == null) {
      warnings = new ArrayList<>();
    }
    warnings.add(warning);
  }

  public void requireManualReview(String reason) {
    requiresManualReview = true;
    manualReviewReason = reason;
    status = ReadinessStatus.PENDING_REVIEW;
  }
}
