package dk.ufst.opendebt.debtservice.exception;

public class InvalidReversalAmountException extends RuntimeException {
  public InvalidReversalAmountException(String message) {
    super(message);
  }
}
