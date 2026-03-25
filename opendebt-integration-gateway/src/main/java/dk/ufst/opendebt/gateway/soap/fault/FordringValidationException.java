package dk.ufst.opendebt.gateway.soap.fault;

import java.util.List;

import dk.ufst.opendebt.common.dto.soap.FieldError;

public class FordringValidationException extends RuntimeException {
  private final List<FieldError> fieldErrors;

  public FordringValidationException(List<FieldError> fieldErrors) {
    super("Valideringsfejl i fordringen");
    this.fieldErrors = fieldErrors;
  }

  public List<FieldError> getFieldErrors() {
    return fieldErrors;
  }
}
