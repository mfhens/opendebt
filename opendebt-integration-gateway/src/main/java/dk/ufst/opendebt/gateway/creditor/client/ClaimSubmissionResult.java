package dk.ufst.opendebt.gateway.creditor.client;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ValidationError {
    @JsonAlias({"ruleId"})
    private String ruleCode;

    @JsonAlias({"field"})
    private String field;

    @JsonAlias({"description"})
    private String message;

    @JsonAlias({"errorCode"})
    private String errorCode;
  }
}
