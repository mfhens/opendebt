package dk.ufst.opendebt.debtservice.dto;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClaimSubmissionResponse {

  private Outcome outcome;
  private UUID claimId;
  private UUID caseId;
  private List<ClaimValidationResult.ValidationError> errors;

  public enum Outcome {
    UDFOERT,
    AFVIST,
    HOERING
  }
}
