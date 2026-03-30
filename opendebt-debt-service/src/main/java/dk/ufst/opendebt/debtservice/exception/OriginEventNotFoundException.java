package dk.ufst.opendebt.debtservice.exception;

import java.util.UUID;

public class OriginEventNotFoundException extends RuntimeException {
  public OriginEventNotFoundException(UUID id) {
    super("Origin ModregningEvent not found: " + id);
  }
}
