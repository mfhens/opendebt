package dk.ufst.opendebt.common.dto.fordring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single validation error from fordring rule evaluation.
 *
 * <p>Each error maps to a specific fordring error code with a Danish description, and optionally
 * identifies the field that caused the validation failure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FordringValidationError {

  /** Numeric error code matching the fordring integration API specification. */
  private int errorCode;

  /** Typed error code enum for programmatic handling. */
  private FordringErrorCode errorCodeEnum;

  /** Danish description of the validation error. */
  private String message;

  /** The field or structure that caused the validation failure, if applicable. */
  private String field;

  /**
   * Creates a validation error from a FordringErrorCode.
   *
   * @param errorCodeEnum the error code
   * @return a new FordringValidationError
   */
  public static FordringValidationError of(FordringErrorCode errorCodeEnum) {
    return FordringValidationError.builder()
        .errorCode(errorCodeEnum.getCode())
        .errorCodeEnum(errorCodeEnum)
        .message(errorCodeEnum.getDanishDescription())
        .build();
  }

  /**
   * Creates a validation error from a FordringErrorCode with a specific field.
   *
   * @param errorCodeEnum the error code
   * @param field the field that caused the error
   * @return a new FordringValidationError
   */
  public static FordringValidationError of(FordringErrorCode errorCodeEnum, String field) {
    return FordringValidationError.builder()
        .errorCode(errorCodeEnum.getCode())
        .errorCodeEnum(errorCodeEnum)
        .message(errorCodeEnum.getDanishDescription())
        .field(field)
        .build();
  }
}
