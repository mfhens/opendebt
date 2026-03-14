package dk.ufst.opendebt.debtservice.exception;

import dk.ufst.opendebt.common.exception.OpenDebtException;

public class CreditorValidationException extends OpenDebtException {

  public CreditorValidationException(String message, String reasonCode) {
    super(message, reasonCode, ErrorSeverity.WARNING);
  }

  public CreditorValidationException(String message, String reasonCode, ErrorSeverity severity) {
    super(message, reasonCode, severity);
  }
}
