package dk.ufst.opendebt.citizen.exception;

public class DebtOverviewServiceUnavailableException extends RuntimeException {

  public DebtOverviewServiceUnavailableException(String message) {
    super(message);
  }

  public DebtOverviewServiceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
