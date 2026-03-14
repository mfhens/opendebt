package dk.ufst.opendebt.common.dto.fordring;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of fordring (claim action) validation.
 *
 * <p>Contains the overall validation outcome and a list of individual validation errors. The result
 * is valid if and only if there are no errors.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FordringValidationResult {

  /** Whether the fordring action passed all validation rules. */
  @Builder.Default private boolean valid = true;

  /** List of validation errors found during rule evaluation. */
  @Builder.Default private List<FordringValidationError> errors = new ArrayList<>();

  /**
   * Adds a validation error and marks the result as invalid.
   *
   * @param error the validation error
   */
  public void addError(FordringValidationError error) {
    if (errors == null) {
      errors = new ArrayList<>();
    }
    errors.add(error);
    valid = false;
  }

  /**
   * Convenience method to add an error from a FordringErrorCode.
   *
   * @param errorCode the error code
   */
  public void addError(FordringErrorCode errorCode) {
    addError(FordringValidationError.of(errorCode));
  }

  /**
   * Convenience method to add an error from a FordringErrorCode with a specific field.
   *
   * @param errorCode the error code
   * @param field the field that caused the error
   */
  public void addError(FordringErrorCode errorCode, String field) {
    addError(FordringValidationError.of(errorCode, field));
  }

  /** Returns true if there are no validation errors. */
  public boolean isValid() {
    return errors == null || errors.isEmpty();
  }
}
