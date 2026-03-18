package dk.ufst.opendebt.debtservice.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClaimValidationResult {

  @Builder.Default private List<ValidationError> errors = new ArrayList<>();

  public boolean isValid() {
    return errors.isEmpty();
  }

  @Data
  @Builder
  public static class ValidationError {
    private String ruleId;
    private String errorCode;
    private String description;
  }
}
