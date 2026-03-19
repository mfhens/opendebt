package dk.ufst.opendebt.gateway.creditor.client;

import java.util.List;
import java.util.UUID;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimSubmissionResult {

  private Outcome outcome;
  private UUID claimId;
  private UUID caseId;
  private List<ValidationError> errors;

  public enum Outcome {
    UDFOERT,
    AFVIST,
    HOERING
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ValidationError {
    private String ruleCode;
    private String field;
    private String message;
  }
}
