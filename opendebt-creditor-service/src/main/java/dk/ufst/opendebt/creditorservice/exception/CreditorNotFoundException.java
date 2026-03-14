package dk.ufst.opendebt.creditorservice.exception;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.common.exception.OpenDebtException.ErrorSeverity;

public class CreditorNotFoundException extends OpenDebtException {

  public CreditorNotFoundException(String message) {
    super(message, "CREDITOR_NOT_FOUND", ErrorSeverity.WARNING);
  }

  public CreditorNotFoundException(String message, Throwable cause) {
    super(message, "CREDITOR_NOT_FOUND", cause);
  }
}
