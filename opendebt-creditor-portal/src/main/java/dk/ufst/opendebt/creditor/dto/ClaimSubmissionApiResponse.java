package dk.ufst.opendebt.creditor.dto;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON shape of {@code ClaimSubmissionResponse} from debt-service {@code POST /debts/submit}. Kept
 * in the portal module so the BFF can deserialize without depending on debt-service.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClaimSubmissionApiResponse {

  private Outcome outcome;
  private UUID claimId;
  private UUID caseId;
  private List<ApiValidationError> errors;

  public enum Outcome {
    UDFOERT,
    AFVIST,
    HOERING
  }

  @Data
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ApiValidationError {
    private String ruleId;
    private String errorCode;
    private String description;
  }
}
