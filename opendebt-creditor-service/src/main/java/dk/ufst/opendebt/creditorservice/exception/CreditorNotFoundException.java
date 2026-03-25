package dk.ufst.opendebt.creditorservice.exception;

import dk.ufst.opendebt.common.exception.OpenDebtException;

public class CreditorNotFoundException extends OpenDebtException {

  public CreditorNotFoundException(String message) {
    super(message, "CREDITOR_NOT_FOUND", OpenDebtException.ErrorSeverity.WARNING);
  }

  public CreditorNotFoundException(String message, Throwable cause) {
    super(message, "CREDITOR_NOT_FOUND", cause);
  }
}
