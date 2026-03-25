package dk.ufst.opendebt.common.dto.soap;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimSubmissionResponse {
  private String claimId;
  private Outcome outcome;
  private List<FieldError> errors;

  public enum Outcome {
    SUCCESS,
    REJECTED,
    ERROR
  }
}
