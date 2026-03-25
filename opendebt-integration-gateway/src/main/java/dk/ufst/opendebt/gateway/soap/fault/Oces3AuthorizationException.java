package dk.ufst.opendebt.gateway.soap.fault;

public class Oces3AuthorizationException extends RuntimeException {
  private final String errorCode;

  public Oces3AuthorizationException(String message, String errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
