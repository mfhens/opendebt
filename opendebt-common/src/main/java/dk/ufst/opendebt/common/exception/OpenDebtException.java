package dk.ufst.opendebt.common.exception;

import lombok.Getter;

@Getter
public class OpenDebtException extends RuntimeException {

  private final String errorCode;
  private final ErrorSeverity severity;

  public OpenDebtException(String message, String errorCode) {
    super(message);
    this.errorCode = errorCode;
    this.severity = ErrorSeverity.ERROR;
  }

  public OpenDebtException(String message, String errorCode, ErrorSeverity severity) {
    super(message);
    this.errorCode = errorCode;
    this.severity = severity;
  }

  public OpenDebtException(String message, String errorCode, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    this.severity = ErrorSeverity.ERROR;
  }

  public enum ErrorSeverity {
    WARNING,
    ERROR,
    CRITICAL
  }
}
